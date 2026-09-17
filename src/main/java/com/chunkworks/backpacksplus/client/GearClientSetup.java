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
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Physical-client registration only; server mod entry points never reference this class. */
@EventBusSubscriber(modid=BackpacksPlus.ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class GearClientSetup {
    private GearClientSetup() {}
    static final KeyMapping BROWSE=new KeyMapping("key.backpacksplus.gear",KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,"key.categories.backpacksplus");
    static final KeyMapping OPEN=new KeyMapping("key.backpacksplus.open",KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_B,"key.categories.backpacksplus");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(BROWSE); event.register(OPEN); }
    @SubscribeEvent public static void bodyLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin:event.getSkins()) {
            PlayerRenderer renderer=event.getSkin(skin);
            if (renderer!=null) renderer.addLayer(new BackpackLayer(renderer));
        }
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) { BackpackLayer.registerModels(event); }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)resources -> BackpackLayer.clear());
    }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) { event.registerAbove(VanillaGuiLayers.AIR_LEVEL,BackpacksPlus.id("gear"),GearHud::render); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> GearProtocol.receive(GearClient::receive,GearClient::receive));
    }
}
