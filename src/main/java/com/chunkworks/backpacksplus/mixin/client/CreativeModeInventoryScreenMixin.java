/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin.client;

import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagMenu;
import com.chunkworks.backpacksplus.WornBagSlots;
import com.chunkworks.backpacksplus.client.BagPanelArt;
import com.chunkworks.backpacksplus.client.BagPanelScreen;
import com.chunkworks.backpacksplus.client.CreativePanelScreen;
import com.chunkworks.backpacksplus.domain.InventoryPanel;
import com.chunkworks.backpacksplus.mixin.SlotAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The worn bag's panel on the creative screen's inventory tab (D-0031). That tab wraps every
 * slot of the player's inventory menu and places the ones it does not know over the hotbar row,
 * which put the bag's forty cells over the player's own inventory. On the inventory tab, with a
 * bag worn and room for panel and screen side by side ({@link InventoryPanel#creative}), the
 * screen is laid out for a screen the panel's width wider, which stands it half a panel right,
 * the panel is drawn to its left and the wrapped cells stand in it; elsewhere the cells are
 * inactive and the screen is vanilla's. The tabs switch without a new layout, so a switch that
 * changes the widening lays the screen out again. The server takes a creative player's edits of
 * the cells ({@code CreativeBagCells}). */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu>
        implements BagPanelScreen, CreativePanelScreen {
    private CreativeModeInventoryScreenMixin() { super(null, null, null); }
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private boolean hasClickedOutside;
    @Unique private InventoryPanel backpacksplus$panel = InventoryPanel.NONE;
    /** The widening the current layout was made with, and whether a bag was worn then. */
    @Unique private int backpacksplus$laidOut;
    @Unique private boolean backpacksplus$wornAtLayout;

    @Unique private boolean backpacksplus$inventoryTab() { return selectedTab != null && selectedTab.getType() == CreativeModeTab.Type.INVENTORY; }
    @Unique private InventoryPanel backpacksplus$place() { return InventoryPanel.creative(Math.max(1, width), BagPanelArt.worn(), backpacksplus$inventoryTab()); }

    @Override public int backpacksplus$widenForLayout() {
        backpacksplus$wornAtLayout = BagPanelArt.worn();
        backpacksplus$laidOut = backpacksplus$place().widen();
        return backpacksplus$laidOut;
    }

    /** After a tab is chosen: a widening other than the layout's lays the screen out again (which
     * chooses the tab again); otherwise, on the inventory tab, the wrapped cells follow the panel. */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void backpacksplus$tab(CreativeModeTab tab, CallbackInfo ci) {
        backpacksplus$panel = backpacksplus$place();
        if (backpacksplus$panel.widen() != backpacksplus$laidOut) { rebuildWidgets(); return; }
        if (!backpacksplus$inventoryTab() || minecraft == null || minecraft.player == null) return;
        var inventory = minecraft.player.inventoryMenu;
        int first = ((WornBagMenu) inventory).backpacksplus$first();
        if (first < 0 || menu.slots.size() < first + WornBag.SIZE) return;
        WornBagSlots.place(inventory, backpacksplus$panel, 0);
        // The tab wraps the inventory menu's slots in order, so a cell's wrapper has its index.
        for (int k = first; k < first + WornBag.SIZE; k++) {
            var cell = inventory.slots.get(k);
            ((SlotAccessor) menu.slots.get(k)).backpacksplus$setX(cell.x);
            ((SlotAccessor) menu.slots.get(k)).backpacksplus$setY(cell.y);
        }
    }

    /** A bag put on or taken off while the screen is open lays it out again, as a resize would. */
    @Inject(method = "containerTick", at = @At("TAIL"))
    private void backpacksplus$follow(CallbackInfo ci) { if (BagPanelArt.worn() != backpacksplus$wornAtLayout) rebuildWidgets(); }

    @Override public int[] backpacksplus$panel() {
        if (!backpacksplus$inventoryTab() || !backpacksplus$panel.shown() || !BagPanelArt.worn()) return null;
        return new int[] { leftPos + backpacksplus$panel.panelX(), topPos, WornBagSlots.PANEL_WIDTH, BagPanelArt.height() };
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void backpacksplus$drawPanel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        var bounds = backpacksplus$panel();
        if (bounds != null) BagPanelArt.draw(g, font, bounds[0], bounds[1]);
    }

    /** A click on the panel, between its cells too, is not outside the screen. */
    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void backpacksplus$onPanel(double mouseX, double mouseY, int guiLeft, int guiTop, int button, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && BagPanelArt.inside(backpacksplus$panel(), mouseX, mouseY)) {
            hasClickedOutside = false;
            cir.setReturnValue(false);
        }
    }
}
