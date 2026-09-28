/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin.client;

import com.chunkworks.backpacksplus.WornBagMenu;
import com.chunkworks.backpacksplus.WornBagSlots;
import com.chunkworks.backpacksplus.client.BagPanelArt;
import com.chunkworks.backpacksplus.client.BagPanelScreen;
import com.chunkworks.backpacksplus.domain.InventoryPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.CraftingMenu;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The worn bag's panel beside the crafting table's screen (D-0033), laid out by the inventory
 * screen's rule (D-0028) because the two screens are the same size and lay out their recipe book
 * the same way: a worn bag makes the vanilla layout the panel's width wider, the screen and an
 * open book stand half a panel right, the book goes beside the screen only where panel, book and
 * screen all fit and otherwise takes the room while the panel yields. The panel's top is level
 * with the screen's, as on the inventory screen. This mirrors {@link InventoryScreenMixin} at the
 * same places in the crafting screen's code; only the book button's place differs. */
@Mixin(CraftingScreen.class)
abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> implements BagPanelScreen {
    private CraftingScreenMixin() { super(null, null, null); }
    @Shadow private boolean widthTooNarrow;
    @Shadow @Final private RecipeBookComponent recipeBookComponent;
    @Unique private InventoryPanel backpacksplus$panel = InventoryPanel.NONE;
    @Unique private boolean backpacksplus$wornAtLayout;
    @Unique private int backpacksplus$bookWidened;

    private static final String BOOK_INIT = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;ZLnet/minecraft/world/inventory/RecipeBookMenu;)V";
    private static final String SCREEN_POSITION = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;updateScreenPosition(II)I";

    @Unique private boolean backpacksplus$carries() { return ((WornBagMenu) menu).backpacksplus$first() >= 0; }
    @Unique private boolean backpacksplus$worn() { return backpacksplus$carries() && BagPanelArt.worn(); }

    /** effects: the placement for the screen as it is now; the book's open state from the
     * player's recipe book, which the toggle writes at once. */
    @Unique private InventoryPanel backpacksplus$place() {
        boolean open = minecraft != null && minecraft.player != null && minecraft.player.getRecipeBook().isOpen(menu.getRecipeBookType());
        return InventoryPanel.of(Math.max(1, width), backpacksplus$worn(), open);
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/CraftingScreen;widthTooNarrow:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void backpacksplus$narrow(CallbackInfo ci) { widthTooNarrow = backpacksplus$place().overlays(width); }
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = BOOK_INIT), index = 0)
    private int backpacksplus$bookWidth(int width) { return width + backpacksplus$place().widen(); }
    @ModifyArg(method = { "init", "lambda$init$0" }, at = @At(value = "INVOKE", target = SCREEN_POSITION), index = 0)
    private int backpacksplus$screenWidth(int width) { return width + backpacksplus$place().widen(); }
    @Inject(method = "init", at = @At("TAIL"))
    private void backpacksplus$layout(CallbackInfo ci) {
        backpacksplus$wornAtLayout = backpacksplus$worn();
        backpacksplus$panel = backpacksplus$place();
        backpacksplus$bookWidened = backpacksplus$panel.widen();
        WornBagSlots.place(menu, backpacksplus$panel, 0);
    }
    /** After the book's button toggles it: where the widening changed under the book, it is laid
     * out again for the new width, and the button follows the screen (vanilla puts it at
     * leftPos + 5, height / 2 − 49 on this screen). */
    @Inject(method = "lambda$init$0", at = @At("TAIL"))
    private void backpacksplus$toggled(Button button, CallbackInfo ci) {
        backpacksplus$panel = backpacksplus$place();
        if (backpacksplus$panel.widen() != backpacksplus$bookWidened) {
            backpacksplus$bookWidened = backpacksplus$panel.widen();
            widthTooNarrow = backpacksplus$panel.overlays(width);
            recipeBookComponent.init(width + backpacksplus$bookWidened, height, minecraft, widthTooNarrow, menu);
            leftPos = recipeBookComponent.updateScreenPosition(width + backpacksplus$bookWidened, imageWidth);
            button.setPosition(leftPos + 5, height / 2 - 49);
        }
        WornBagSlots.place(menu, backpacksplus$panel, 0);
    }
    @Inject(method = "containerTick", at = @At("TAIL"))
    private void backpacksplus$follow(CallbackInfo ci) { if (backpacksplus$worn() != backpacksplus$wornAtLayout) rebuildWidgets(); }

    @Override public int[] backpacksplus$panel() {
        if (!backpacksplus$panel.shown() || !backpacksplus$worn()) return null;
        return new int[] { leftPos + backpacksplus$panel.panelX(), topPos, WornBagSlots.PANEL_WIDTH, BagPanelArt.height() };
    }
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void backpacksplus$drawPanel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        var bounds = backpacksplus$panel();
        if (bounds != null) BagPanelArt.draw(g, font, bounds[0], bounds[1]);
    }
    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void backpacksplus$onPanel(double mouseX, double mouseY, int guiLeft, int guiTop, int button, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && BagPanelArt.inside(backpacksplus$panel(), mouseX, mouseY)) cir.setReturnValue(false);
    }
}
