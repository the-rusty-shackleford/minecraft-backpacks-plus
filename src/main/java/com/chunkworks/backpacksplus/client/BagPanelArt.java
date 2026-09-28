/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.BagContents;
import com.chunkworks.backpacksplus.BagLocations;
import com.chunkworks.backpacksplus.WornBagSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The worn bag's panel as drawn behind its cells, the same on the inventory screen (D-0027) and
 * beside a chest (D-0030): a vanilla-looking frame sized to the tier, the bag's name, a well per
 * mount across the top with their label, and a well per storage cell below. */
public final class BagPanelArt {
    private BagPanelArt() {}

    /** effects: whether the client's player wears a bag now. */
    public static boolean worn() {
        var mc = Minecraft.getInstance();
        return mc.player != null && BagLocations.isBag(BagLocations.stack(mc.player, BagLocations.worn(mc.player)));
    }
    /** requires: worn(); effects: the worn bag. */
    public static ItemStack bag() {
        var p = Minecraft.getInstance().player;
        return BagLocations.stack(p, BagLocations.worn(p));
    }
    /** requires: worn(); effects: the panel's height for the worn bag's tier. */
    public static int height() {
        return WornBagSlots.panelHeight((BagContents.tier(bag()).storageSlots() + 8) / 9);
    }
    /** requires: worn(); effects: draws the panel with its top-left corner at (x0, y0). */
    public static void draw(GuiGraphics g, Font font, int x0, int y0) {
        var bag = bag();
        var tier = BagContents.tier(bag);
        int mounts = tier.totalSlots() - tier.storageSlots();
        int w = WornBagSlots.PANEL_WIDTH, h = height();
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF000000);
        g.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, 0xFFC6C6C6);
        g.fill(x0 + 1, y0 + 1, x0 + w - 2, y0 + 2, 0xFFFFFFFF); g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + h - 2, 0xFFFFFFFF);
        g.fill(x0 + 2, y0 + h - 2, x0 + w - 1, y0 + h - 1, 0xFF555555); g.fill(x0 + w - 2, y0 + 2, x0 + w - 1, y0 + h - 1, 0xFF555555);
        g.drawString(font, bag.getHoverName(), x0 + 8, y0 + 6, 0x404040, false);
        for (int i = 0; i < mounts; i++) cell(g, x0 + 8 + i * 18, y0 + WornBagSlots.MOUNTS_Y);
        for (int k = 0; k < tier.storageSlots(); k++) cell(g, x0 + 8 + (k % 9) * 18, y0 + WornBagSlots.STORAGE_Y + (k / 9) * 18);
        g.drawString(font, Component.translatable("backpacksplus.mounts"), x0 + 8 + mounts * 18 + 4, y0 + WornBagSlots.MOUNTS_Y + 4, 0x404040, false);
    }
    /** effects: whether (x, y) is inside the panel's {x, y, width, height}, false for null. A
     * click there is on the screen, not outside it, even between cells: outside, with an item on
     * the cursor, would throw the item. */
    public static boolean inside(int[] panel, double x, double y) {
        return panel != null && x >= panel[0] && x < panel[0] + panel[2] && y >= panel[1] && y < panel[1] + panel[3];
    }
    /** effects: a vanilla-looking slot well at the cell's top-left corner. */
    private static void cell(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
}
