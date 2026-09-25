/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin.client;

import com.chunkworks.backpacksplus.BagLocations;
import com.chunkworks.backpacksplus.BagContents;
import com.chunkworks.backpacksplus.WornBagSlots;
import com.chunkworks.backpacksplus.client.BagPanelScreen;
import com.chunkworks.backpacksplus.domain.InventoryPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The worn bag's panel beside the survival inventory screen (D-0027, D-0028). A worn bag makes
 * the vanilla layout the panel's width wider: the screen, and the recipe book when open, are
 * laid out as vanilla lays them out for a screen that much wider, which stands them half a
 * panel to the right, and the book goes beside the screen only where panel, book and screen
 * all fit, else over it as vanilla does on a narrow screen. The rule is
 * {@link InventoryPanel}; this mixin feeds vanilla the widened width at the three places it
 * reads it, stands the cells where the panel is after every layout, and draws the panel. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> implements BagPanelScreen {
    private InventoryScreenMixin() { super(null, null, null); }
    @Shadow private boolean widthTooNarrow;
    @Shadow @Final private RecipeBookComponent recipeBookComponent;
    @Unique private InventoryPanel backpacksplus$panel = InventoryPanel.NONE;
    @Unique private boolean backpacksplus$wornAtLayout;
    /** The widening the book was last laid out with. */
    @Unique private int backpacksplus$bookWidened;

    private static final String BOOK_INIT = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;ZLnet/minecraft/world/inventory/RecipeBookMenu;)V";
    private static final String SCREEN_POSITION = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;updateScreenPosition(II)I";

    /** effects: the placement for the screen as it is now. The book's open state is read from the
     * player's recipe book, which the toggle writes at once and the component only copies when it
     * is laid out, so init and toggle see the same state. */
    private InventoryPanel backpacksplus$place() {
        boolean open = minecraft != null && minecraft.player != null && minecraft.player.getRecipeBook().isOpen(menu.getRecipeBookType());
        return InventoryPanel.of(Math.max(1, width), backpacksplus$worn(), open);
    }

    /** The book overlays the screen wherever panel, book and screen do not all fit beside one
     * another: vanilla's own threshold, the panel's width added. Right after vanilla sets its
     * flag and before the book reads it. */
    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;widthTooNarrow:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void backpacksplus$narrow(CallbackInfo ci) { widthTooNarrow = backpacksplus$place().overlays(width); }
    /** The book lays itself out for the widened screen, so beside the screen it stands at the shifted place. */
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = BOOK_INIT), index = 0)
    private int backpacksplus$bookWidth(int width) { return width + backpacksplus$place().widen(); }
    /** The screen too, at init and when the book's button toggles the book, so the button is
     * built where the screen really is. */
    @ModifyArg(method = { "init", "lambda$init$0" }, at = @At(value = "INVOKE", target = SCREEN_POSITION), index = 0)
    private int backpacksplus$screenWidth(int width) { return width + backpacksplus$place().widen(); }
    /** After the layout, the cells follow the panel. */
    @Inject(method = "init", at = @At("TAIL"))
    private void backpacksplus$layout(CallbackInfo ci) {
        backpacksplus$wornAtLayout = backpacksplus$worn();
        backpacksplus$panel = backpacksplus$place();
        backpacksplus$bookWidened = backpacksplus$panel.widen();
        WornBagSlots.place(menu, backpacksplus$panel);
    }
    /** After the book's button toggles it: where the panel yields to the open book or comes back
     * when it closes, the widening changed under the book, which laid itself out at init, so it
     * is laid out again for the new width, as it is on a resize, and the button follows the screen. */
    @Inject(method = "lambda$init$0", at = @At("TAIL"))
    private void backpacksplus$toggled(Button button, CallbackInfo ci) {
        backpacksplus$panel = backpacksplus$place();
        if (backpacksplus$panel.widen() != backpacksplus$bookWidened) {
            backpacksplus$bookWidened = backpacksplus$panel.widen();
            widthTooNarrow = backpacksplus$panel.overlays(width);
            recipeBookComponent.init(width + backpacksplus$bookWidened, height, minecraft, widthTooNarrow, menu);
            leftPos = recipeBookComponent.updateScreenPosition(width + backpacksplus$bookWidened, imageWidth);
            button.setPosition(leftPos + 104, height / 2 - 22);
        }
        WornBagSlots.place(menu, backpacksplus$panel);
    }
    /** A bag put on or taken off while the screen is open lays the screen out again, as a resize would. */
    @Inject(method = "containerTick", at = @At("TAIL"))
    private void backpacksplus$follow(CallbackInfo ci) { if (backpacksplus$worn() != backpacksplus$wornAtLayout) rebuildWidgets(); }

    @Override public int[] backpacksplus$panel() {
        if (!backpacksplus$panel.shown() || !backpacksplus$worn()) return null;
        var tier = BagContents.tier(BagLocations.stack(minecraft.player, BagLocations.worn(minecraft.player)));
        return new int[] { leftPos + backpacksplus$panel.panelX(), topPos, WornBagSlots.PANEL_WIDTH, WornBagSlots.panelHeight((tier.storageSlots() + 8) / 9) };
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void backpacksplus$panel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        var bounds = backpacksplus$panel();
        if (bounds == null) return;
        var bag = BagLocations.stack(minecraft.player, BagLocations.worn(minecraft.player));
        var tier = BagContents.tier(bag);
        int mounts = tier.totalSlots() - tier.storageSlots();
        int x0 = bounds[0], y0 = bounds[1], w = bounds[2], h = bounds[3];
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
