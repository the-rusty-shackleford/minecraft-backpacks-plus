/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin.client;

import com.chunkworks.backpacksplus.BagLocations;
import com.chunkworks.backpacksplus.BagContents;
import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The worn bag's panel beside the survival inventory screen (D-0027): drawn behind the bag's
 * slots when a bag is worn, sized to its tier; the screen moves right when the panel would run
 * off the left edge. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    private InventoryScreenMixin() { super(null, null, null); }
    @Inject(method = "init", at = @At("TAIL"))
    private void backpacksplus$makeRoom(CallbackInfo ci) {
        if (backpacksplus$worn() && leftPos < WornBagSlots.PANEL_WIDTH + WornBagSlots.GAP + 2) leftPos = WornBagSlots.PANEL_WIDTH + WornBagSlots.GAP + 2;
    }
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void backpacksplus$panel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!backpacksplus$worn()) return;
        var bag = BagLocations.stack(minecraft.player, BagLocations.worn(minecraft.player));
        var tier = BagContents.tier(bag);
        int rows = (tier.storageSlots() + 8) / 9, mounts = tier.totalSlots() - tier.storageSlots();
        int x0 = leftPos + WornBagSlots.PANEL_X, y0 = topPos, w = WornBagSlots.PANEL_WIDTH, h = WornBagSlots.panelHeight(rows);
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF000000);
        g.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, 0xFFC6C6C6);
        g.fill(x0 + 1, y0 + 1, x0 + w - 2, y0 + 2, 0xFFFFFFFF); g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + h - 2, 0xFFFFFFFF);
        g.fill(x0 + 2, y0 + h - 2, x0 + w - 1, y0 + h - 1, 0xFF555555); g.fill(x0 + w - 2, y0 + 2, x0 + w - 1, y0 + h - 1, 0xFF555555);
        g.drawString(font, bag.getHoverName(), x0 + 8, y0 + 6, 0x404040, false);
        for (int i = 0; i < mounts; i++) backpacksplus$cell(g, x0 + 8 + i * 18, y0 + WornBagSlots.MOUNTS_Y);
        for (int k = 0; k < tier.storageSlots(); k++) backpacksplus$cell(g, x0 + 8 + (k % 9) * 18, y0 + WornBagSlots.STORAGE_Y + (k / 9) * 18);
        var label = Component.translatable("backpacksplus.mounts");
        g.drawString(font, label, x0 + 8 + mounts * 18 + 4, y0 + WornBagSlots.MOUNTS_Y + 4, 0x404040, false);
    }
    /** effects: a vanilla-looking slot well at the cell's top-left corner. */
    private void backpacksplus$cell(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
    private boolean backpacksplus$worn() {
        return minecraft != null && minecraft.player != null && BagLocations.isBag(BagLocations.stack(minecraft.player, BagLocations.worn(minecraft.player)));
    }
}
