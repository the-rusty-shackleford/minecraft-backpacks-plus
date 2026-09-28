/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A creative player's edits of the worn bag's cells (D-0031). The creative screen is
 * client-authoritative: it edits the player's inventory menu itself and tells the server each
 * changed slot's new stack, and vanilla takes only slots 1 to 45 of that menu, so an edit of one
 * of the bag's cells (after vanilla's 46, D-0027) would be dropped and the client left showing a
 * stack the server never had. The server takes it here instead, under the cell's own rules. */
public final class CreativeBagCells {
    private CreativeBagCells() {}

    /** effects: when {@code slot} is one of the worn bag's cells on {@code player}'s inventory
     * menu, sets it to {@code stack} if the player is in creative, the stack is enabled and within
     * its stack size, the cell is active, and the cell admits it (the bag's rules: no nested
     * storage, mounts by size); otherwise leaves it and sends the menu back to the client so it
     * shows what the server has. Returns whether the slot was a bag cell, handled either way;
     * false leaves the packet to vanilla. */
    public static boolean apply(ServerPlayer player, int slot, ItemStack stack) {
        var menu = player.inventoryMenu;
        int first = ((WornBagMenu) menu).backpacksplus$first();
        if (first < 0 || slot < first || slot >= first + WornBag.SIZE) return false;
        var cell = menu.getSlot(slot);
        boolean ok = player.gameMode.isCreative()
                && stack.isItemEnabled(player.level().enabledFeatures())
                && stack.getCount() <= stack.getMaxStackSize()
                && cell.isActive()
                && (stack.isEmpty() || cell.mayPlace(stack) && stack.getCount() <= cell.getMaxStackSize(stack));
        if (ok) {
            cell.setByPlayer(stack);
            menu.broadcastChanges();
        } else {
            menu.sendAllDataToRemote();
        }
        return true;
    }
}
