/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.BackpacksPlus;
import com.chunkworks.backpacksplus.GearProtocol;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Physical-client registration only; server mod entry points never reference this class. */
@EventBusSubscriber(modid=BackpacksPlus.ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class GearClientSetup {
    private GearClientSetup() {}
    static final KeyMapping BROWSE=new KeyMapping("key.backpacksplus.gear",KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,"key.categories.backpacksplus");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(BROWSE); }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) { event.registerAbove(VanillaGuiLayers.AIR_LEVEL,BackpacksPlus.id("gear"),GearHud::render); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> GearProtocol.receive(GearClient::receive,GearClient::receive));
    }
}
