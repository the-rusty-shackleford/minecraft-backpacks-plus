/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.InventoryPanel;
import com.chunkworks.backpacksplus.mixin.AbstractContainerMenuAccessor;
import com.chunkworks.backpacksplus.mixin.SlotAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Where the worn bag's cells sit on the player's inventory screen (D-0027): a panel to the
 * left of the vanilla one, {@link #PANEL_WIDTH} wide with a {@link #GAP} between, the four
 * mounts across its top row and the storage cells in up to four rows of nine below. Which
 * cells are active follows the worn bag; where the panel stands follows the screen's width
 * and the recipe book ({@link InventoryPanel}, D-0028), which the client applies with
 * {@link #place}. Their menu indices start where the menu ended when they were added
 * ({@link WornBagMenu#backpacksplus$first}), since other mods add slots of their own. */
public final class WornBagSlots {
    public static final int PANEL_WIDTH = InventoryPanel.WIDTH, GAP = InventoryPanel.GAP, PANEL_X = -(PANEL_WIDTH + GAP), MOUNTS_Y = 18, STORAGE_Y = 42;
    private WornBagSlots() {}

    /** One of the worn bag's cells on the inventory menu. Active while the worn bag has the
     * cell and, on the client, while the panel is shown; its x follows the panel. */
    public static final class Cell extends Slot {
        private final WornBag bag;
        private final int cell;
        private boolean shown = true;
        private Cell(WornBag bag, int cell, int x, int y) { super(bag, cell, x, y); this.bag = bag; this.cell = cell; }
        @Override public boolean isActive() { return shown && bag.active(cell); }
        @Override public boolean mayPlace(ItemStack stack) { return bag.canPlaceItem(cell, stack); }
        @Override public boolean mayPickup(Player player) { return bag.active(cell); }
        /** effects: the cell's column in its row of the panel. */
        int column() { return cell < WornBag.STORAGE ? cell % 9 : cell - WornBag.STORAGE; }
    }

    /** effects: adds the forty cells to the menu, mounts first across the top, storage below;
     * returns the menu index of the first. */
    public static int add(InventoryMenu menu, WornBag bag) {
        var access = (AbstractContainerMenuAccessor) menu;
        int first = menu.slots.size();
        for (int k = 0; k < WornBag.SIZE; k++) {
            int x, y;
            if (k < WornBag.STORAGE) { x = PANEL_X + 8 + (k % 9) * 18; y = STORAGE_Y + (k / 9) * 18; }
            else { x = PANEL_X + 8 + (k - WornBag.STORAGE) * 18; y = MOUNTS_Y; }
            access.backpacksplus$addSlot(new Cell(bag, k, x, y));
        }
        return first;
    }
    /** effects: on the client, stands the cells where the panel stands, or hides them when it
     * is not shown. Slot positions mean nothing to the server, which never calls this. */
    public static void place(InventoryMenu menu, InventoryPanel panel) {
        int first = ((WornBagMenu) menu).backpacksplus$first();
        if (first < 0) return;
        for (int k = 0; k < WornBag.SIZE; k++) {
            var cell = (Cell) menu.slots.get(first + k);
            cell.shown = panel.shown();
            ((SlotAccessor) (Object) cell).backpacksplus$setX((panel.shown() ? panel.panelX() : PANEL_X) + 8 + cell.column() * 18);
        }
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
