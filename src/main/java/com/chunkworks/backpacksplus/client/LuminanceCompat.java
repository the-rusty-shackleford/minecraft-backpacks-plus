/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.BackpackItem;
import com.chunkworks.luminance.LuminanceConfig;
import com.chunkworks.luminance.api.Luminance;
import com.chunkworks.luminance.client.Providers;
import com.chunkworks.luminance.domain.Point;
import com.chunkworks.luminance.domain.Source;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * AF: equipped/hand-carried mount items contribute their strongest Luminance light.
 * RI: optional client-only adapter, loaded only with Luminance present; ordinary storage
 * never contributes. Cached dry/wet levels belong to one immutable contents component
 * and are invalidated on resource reload. No stacks or world light levels are mutated.
 * Real-client backend/terrain checks are the gate; a dedicated server cannot draw light.
 */
final class LuminanceCompat {
    private LuminanceCompat() {}
    private record Levels(ItemContainerContents contents,int dry,int wet) {}
    private static final Map<ItemStack,Levels> CACHE=new WeakHashMap<>();

    /** requires: client setup and Luminance 1.1+. effects: registers one player provider. */
    static void register() { Luminance.forEntityInterpolated(EntityType.PLAYER,LuminanceCompat::sources); }
    /** effects: discards brightness derived from the previous resource definitions. */
    static void clear() { CACHE.clear(); }

    private static int mounts(ItemStack bag,boolean wet) {
        if(bag.getCount()!=1 || !(bag.getItem() instanceof BackpackItem item))return 0;
        var contents=bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        if(contents.getSlots()>item.tier().totalSlots())return 0;
        Levels levels=CACHE.get(bag);
        if(levels==null || levels.contents()!=contents) {
            int dry=0,submerged=0;
            for(int mount=0;mount<item.tier().mounts().size();mount++) {
                int cell=item.tier().mountSlot(mount);
                if(cell>=contents.getSlots())continue;
                var stack=contents.getStackInSlot(cell);
                dry=Math.max(dry,Providers.luminanceOf(stack,false));
                submerged=Math.max(submerged,Providers.luminanceOf(stack,true));
            }
            levels=new Levels(contents,dry,submerged);CACHE.put(bag,levels);
        }
        return wet ? levels.wet() : levels.dry();
    }

    private static List<? extends Source> sources(Player player,Float partial) {
        if(player.isSpectator() || !LuminanceConfig.HELD_ITEMS.get())return List.of();
        boolean wet=player.isUnderWater();
        var view=GearClient.view(player.getUUID());
        int light=view==null ? 0 : mounts(view.bag,wet);
        light=Math.max(light,mounts(player.getMainHandItem(),wet));
        light=Math.max(light,mounts(player.getOffhandItem(),wet));
        // The backend already supplies held light at this same point. Avoid a redundant
        // source when it is as bright or brighter; its Field takes maxima, never sums.
        int held=Math.max(Providers.luminanceOf(player.getMainHandItem(),wet),Providers.luminanceOf(player.getOffhandItem(),wet));
        if(light<=held)return List.of();
        return List.of(new Point(Mth.lerp(partial,player.xOld,player.getX()),
                Mth.lerp(partial,player.yOld,player.getY())+player.getEyeHeight()-0.2,
                Mth.lerp(partial,player.zOld,player.getZ()),light));
    }
}
