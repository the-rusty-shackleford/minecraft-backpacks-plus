/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.BackpacksPlus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * AF: the approved gear gesture owns a shared Curios-open key while a bag is worn.
 * RI: bindings/options are never rewritten. Only a matching key/modifier combination
 * is consumed, before Curios' Post tick handler. Menus, no-bag input and rebound Curios
 * keys retain native behavior. Uses vanilla key mappings, with no other-mod mixin/types.
 */
@EventBusSubscriber(modid=BackpacksPlus.ID,value=Dist.CLIENT)
public final class CuriosInputCompat {
    private CuriosInputCompat() {}
    private static final boolean PRESENT=ModList.get().isLoaded("curios");
    private static boolean resolved;
    private static KeyMapping open;
    /** effects: resolves the optional mapping once and gives equipped gear first input priority. */
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void beforeInput(ClientTickEvent.Pre event) {
        if(!PRESENT)return;
        Minecraft mc=Minecraft.getInstance();
        if(!resolved) {
            for(var key:mc.options.keyMappings)if(key.getName().equals("key.curios.open.desc")){open=key;break;}
            resolved=true;
        }
        if(open==null || !GearClient.ownsBrowseKey())return;
        var gear=GearClientSetup.BROWSE;
        if(open.getKey().equals(gear.getKey()) && open.getKeyModifier()==gear.getKeyModifier())
            while(open.consumeClick()) { /* This press belongs to the equipped gear bar. */ }
    }
}
