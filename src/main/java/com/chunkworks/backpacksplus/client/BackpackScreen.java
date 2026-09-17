/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.BackpackMenu;
import com.chunkworks.backpacksplus.BackpackItems;
import com.chunkworks.backpacksplus.BackpacksPlus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only working inventory screen. RI: menu owns all cells and mutations; this class only draws. */
@EventBusSubscriber(modid=BackpacksPlus.ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    /** effects: creates a screen for the server-opened backpack menu. */
    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250; imageHeight = menu.inventoryTop() + 84;
        titleLabelX = 80; titleLabelY = 10;
        inventoryLabelX = 80; inventoryLabelY = menu.inventoryTop() - 12;
    }
    /** effects: registers this screen only on the physical client. */
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) { event.register(BackpackItems.MENU.get(), BackpackScreen::new); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x=leftPos, y=topPos;
        graphics.fill(x, y, x+imageWidth, y+imageHeight, 0xff343027);
        graphics.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xffc6c6c6);
        graphics.fill(x+6, y+7, x+73, y+100, 0xffa29b8b);
        graphics.drawString(font, Component.translatable("backpacksplus.mounts"), x+10, y+12, 0xff38342d, false);
        for (var slot : menu.slots) {
            int sx=x+slot.x, sy=y+slot.y;
            graphics.fill(sx-1, sy-1, sx+17, sy+17, 0xff373737);
            graphics.fill(sx, sy, sx+17, sy+17, 0xffffffff);
            graphics.fill(sx, sy, sx+16, sy+16, 0xff8b8b8b);
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
