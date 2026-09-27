/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin.client;

import com.chunkworks.backpacksplus.WornBagMenu;
import com.chunkworks.backpacksplus.WornBagSlots;
import com.chunkworks.backpacksplus.client.BagPanelArt;
import com.chunkworks.backpacksplus.client.BagPanelScreen;
import com.chunkworks.backpacksplus.domain.InventoryPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The worn bag's panel beside a chest screen (D-0030): left of it, its bottom level with the
 * screen's so it stands beside the player's inventory rows, the pair centered as one the way the
 * inventory screen's is (D-0028; no recipe book here, so {@link InventoryPanel} with the book
 * closed). Under the width panel and screen need side by side the panel is not shown and the
 * cells are inactive. Only for a chest menu that carries the cells. */
@Mixin(ContainerScreen.class)
abstract class ContainerScreenMixin extends AbstractContainerScreen<ChestMenu> implements BagPanelScreen {
    private ContainerScreenMixin() { super(null, null, null); }
    @Unique private InventoryPanel backpacksplus$panel = InventoryPanel.NONE;
    @Unique private boolean backpacksplus$wornAtLayout;
    @Unique private int backpacksplus$heightAtLayout;

    @Unique private boolean backpacksplus$carries() { return ((WornBagMenu) menu).backpacksplus$first() >= 0; }

    /** After vanilla's layout: the screen moves half a panel right when the panel is shown, and
     * the cells follow the panel. */
    @Override protected void init() {
        super.init();
        if (!backpacksplus$carries()) return;
        backpacksplus$wornAtLayout = BagPanelArt.worn();
        backpacksplus$heightAtLayout = backpacksplus$wornAtLayout ? BagPanelArt.height() : 0;
        backpacksplus$panel = InventoryPanel.of(Math.max(1, width), backpacksplus$wornAtLayout, false);
        leftPos += backpacksplus$panel.widen() / 2;
        WornBagSlots.place(menu, backpacksplus$panel, imageHeight - backpacksplus$heightAtLayout);
    }
    /** A bag put on, taken off or swapped for another tier while the chest is open lays the screen
     * out again, as a resize would. */
    @Override protected void containerTick() {
        super.containerTick();
        if (!backpacksplus$carries()) return;
        boolean worn = BagPanelArt.worn();
        if (worn != backpacksplus$wornAtLayout || worn && BagPanelArt.height() != backpacksplus$heightAtLayout) rebuildWidgets();
    }
    private static final String RENDER_BG = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V";
    /** Vanilla draws the chest's texture centered on the screen, not at leftPos; both of its
     * pieces are drawn where the slots are. */
    @ModifyArg(method = RENDER_BG, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"), index = 1)
    private int backpacksplus$textureLeft(int centered) { return backpacksplus$carries() ? leftPos : centered; }

    @Override public int[] backpacksplus$panel() {
        if (!backpacksplus$carries() || !backpacksplus$panel.shown() || !BagPanelArt.worn()) return null;
        int h = BagPanelArt.height();
        return new int[] { leftPos + backpacksplus$panel.panelX(), topPos + imageHeight - h, WornBagSlots.PANEL_WIDTH, h };
    }

    @Inject(method = RENDER_BG, at = @At("TAIL"))
    private void backpacksplus$drawPanel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        var bounds = backpacksplus$panel();
        if (bounds != null) BagPanelArt.draw(g, font, bounds[0], bounds[1]);
    }
}
