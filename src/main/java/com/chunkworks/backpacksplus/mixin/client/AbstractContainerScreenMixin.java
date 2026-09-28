/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin.client;

import com.chunkworks.backpacksplus.client.CreativePanelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The creative screen lays itself out for a screen the panel's width wider while the panel is
 * shown (D-0031), as the inventory and chest screens do (D-0028, D-0030). It places its tabs'
 * page buttons and search box from leftPos straight after the container screen's own layout,
 * so the widening is applied there, at the end of that layout, for the creative screen only. */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin extends Screen {
    private AbstractContainerScreenMixin() { super(Component.empty()); }
    @Shadow protected int leftPos;
    @Shadow protected int imageWidth;

    @Inject(method = "init", at = @At("TAIL"))
    private void backpacksplus$creativeWiden(CallbackInfo ci) {
        if ((Object) this instanceof CreativePanelScreen creative) {
            int widen = creative.backpacksplus$widenForLayout();
            if (widen > 0) leftPos = (width + widen - imageWidth) / 2;
        }
    }
}
