/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Where the worn bag's cells sit on the player's inventory screen (D-0027): a panel to the
 * left of the vanilla one, {@link #PANEL_WIDTH} wide with a {@link #GAP} between, the four
 * mounts across its top row and the storage cells in up to four rows of nine below. Slot
 * positions are fixed at join; which of them are active follows the worn bag. Their menu
 * indices start where the menu ended when they were added ({@link WornBagMenu#backpacksplus$first}),
 * since other mods add slots of their own. */
public final class WornBagSlots {
    public static final int PANEL_WIDTH = 176, GAP = 4, PANEL_X = -(PANEL_WIDTH + GAP), MOUNTS_Y = 18, STORAGE_Y = 42;
    private WornBagSlots() {}
    /** effects: adds the forty cells to the menu, mounts first across the top, storage below;
     * returns the menu index of the first. */
    public static int add(InventoryMenu menu, WornBag bag) {
        var access = (AbstractContainerMenuAccessor) menu;
        int first = menu.slots.size();
        for (int k = 0; k < WornBag.SIZE; k++) {
            int x, y;
            if (k < WornBag.STORAGE) { x = PANEL_X + 8 + (k % 9) * 18; y = STORAGE_Y + (k / 9) * 18; }
            else { x = PANEL_X + 8 + (k - WornBag.STORAGE) * 18; y = MOUNTS_Y; }
            int cell = k;
            access.backpacksplus$addSlot(new Slot(bag, cell, x, y) {
                @Override public boolean isActive() { return bag.active(cell); }
                @Override public boolean mayPlace(ItemStack stack) { return bag.canPlaceItem(cell, stack); }
                @Override public boolean mayPickup(Player player) { return bag.active(cell); }
            });
        }
        return first;
    }
    /** effects: moves as much of the stack as the worn bag's active cells take, storage before
     * mounts; returns whether any moved. */
    public static boolean moveInto(InventoryMenu menu, ItemStack stack) {
        int first = ((WornBagMenu) menu).backpacksplus$first();
        return first >= 0 && ((AbstractContainerMenuAccessor) menu).backpacksplus$moveItemStackTo(stack, first, first + WornBag.SIZE, false);
    }
    /** effects: the panel's height for a tier with that many storage rows. */
    public static int panelHeight(int storageRows) { return STORAGE_Y + storageRows * 18 + 8; }
}
