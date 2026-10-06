/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.BackpackItems;
import com.chunkworks.backpacksplus.BagContents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Bobandy_'s loss on the box, 2026-10-06: a Reinforced bag worn in the Curios back slot (the chest
 * slot without Curios) emptied while he edited it on the creative screen. Eight kinds, one stack
 * each, so a count per kind afterwards shows any stack lost or doubled. */
final class CreativeRaceNetwork {
    private CreativeRaceNetwork() {}

    static void setup(ServerPlayer p) {
        p.closeContainer(); p.stopRiding();
        boolean curios=net.neoforged.fml.ModList.get().isLoaded("curios");
        if (curios) CuriosNetwork.clear(p);
        p.getInventory().clearContent();
        var bag=new ItemStack(BackpackItems.REINFORCED.get()); BagContents.identify(bag);
        var cells=NonNullList.withSize(BagContents.tier(bag).totalSlots(),ItemStack.EMPTY);
        ItemStack[] kinds={new ItemStack(Items.SPIDER_EYE,7),new ItemStack(Items.BREAD,8),new ItemStack(Items.COBBLESTONE,64),
                new ItemStack(Items.TORCH,32),new ItemStack(Items.IRON_INGOT,5),new ItemStack(Items.OAK_LOG,12),
                new ItemStack(Items.ARROW,16),new ItemStack(Items.DIRT,33)};
        for (int i=0;i<kinds.length;i++) cells.set(i,kinds[i]);
        BagContents.store(bag,cells);
        if (curios) CuriosNetwork.wear(p,bag); else p.getInventory().setItem(38,bag);
        p.setGameMode(GameType.CREATIVE);
    }
}
