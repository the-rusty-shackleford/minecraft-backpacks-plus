/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * AF: stable source addresses shared by menus, transactions and synchronized poses.
 * RI: -1 means absent; 0..40 are vanilla inventory cells; 41+i is functional Curios
 * back slot i. Cosmetic slots never own storage. All stack resolution is authoritative
 * and bounds checked; no Curios class is loaded when the optional mod is absent.
 */
public final class BagLocations {
    private BagLocations() {}
    public static final int NONE=-1, CHEST=38, CURIOS_BACK=41;
    private static final boolean CURIOS=ModList.get().isLoaded("curios");
    /** effects: returns whether the stack is one usable backpack. */
    public static boolean isBag(ItemStack stack) { return stack.getCount()==1 && stack.getItem() instanceof BackpackItem; }
    /** effects: resolves an address to its current stack, or EMPTY if missing/inactive. */
    public static ItemStack stack(Player player,int source) {
        if(source<0)return ItemStack.EMPTY;
        if(source<CURIOS_BACK)return player.getInventory().getItem(source);
        return CURIOS ? CuriosCompat.stack(player,source-CURIOS_BACK) : ItemStack.EMPTY;
    }
    /** effects: chooses the first active Curios back bag, then the native chest bag. */
    public static int worn(Player player) {
        int back=CURIOS ? CuriosCompat.firstBack(player) : NONE;
        return back>=0 ? CURIOS_BACK+back : isBag(stack(player,CHEST)) ? CHEST : NONE;
    }
    /** effects: reports cosmetic visibility independently of usable storage and lights. */
    public static boolean visible(Player player,int source) {
        return source==CHEST || source>=CURIOS_BACK && CURIOS && CuriosCompat.visible(player,source-CURIOS_BACK);
    }
    /** effects: marks the source inventory dirty after an in-place component edit. */
    public static void changed(Player player,int source) {
        if(source>=CURIOS_BACK && CURIOS)CuriosCompat.changed(player,source-CURIOS_BACK);
        else player.getInventory().setChanged();
    }
}
