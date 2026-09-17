/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.client.GearClient;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Test-only input driver and observer. Uses the real key mapping, scroll event, menu clicks and network connection. */
@EventBusSubscriber(modid="backpacksplus_gametest",value=Dist.CLIENT)
public final class NetworkClient {
    private NetworkClient() {}
    private static int ticks;
    private static int completed=NetworkFiles.initialSequence(System.getProperty("backpacksplus.testRole","client"));
    private static boolean connect=true;
    private static String error="";
    private static int frames,frameIndex;
    private static long nextFrame;
    private static String capturePrefix="";
    @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        if (!NetworkFiles.ENABLED || frames<=0 || System.nanoTime()<nextFrame) return;
        Minecraft mc=Minecraft.getInstance(); nextFrame=System.nanoTime()+50_000_000L; frames--;
        Screenshot.grab(mc.gameDirectory,capturePrefix+String.format(java.util.Locale.ROOT,"-%04d.png",frameIndex++),mc.getMainRenderTarget(),message -> {});
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!NetworkFiles.ENABLED || ++ticks%5!=0) return;
        Minecraft mc=Minecraft.getInstance(); String role=System.getProperty("backpacksplus.testRole");
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
        try {
            if (ModList.get().isLoaded("stowed")) throw new IllegalStateException("Stowed conflicts with test");
            var c=NetworkFiles.read(role+"-command");
            if (NetworkFiles.sequence(c)>completed) {
                completed=NetworkFiles.sequence(c);
                switch (c.get("op").getAsString()) {
                    case "focus" -> { GLFW.glfwFocusWindow(mc.getWindow().getWindow()); mc.mouseHandler.grabMouse(); }
                    case "g" -> java.util.Arrays.stream(mc.options.keyMappings).filter(key -> key.getName().equals("key.backpacksplus.gear"))
                            .findFirst().orElseThrow().setDown(c.get("down").getAsBoolean());
                    case "scroll" -> {
                        var scroll=new InputEvent.MouseScrollingEvent(0,c.get("delta").getAsDouble(),false,false,false,0,0);
                        NeoForge.EVENT_BUS.post(scroll);
                        if (!scroll.isCanceled()) mc.player.getInventory().swapPaint(c.get("delta").getAsDouble());
                    }
                    case "use" -> KeyMapping.click(mc.options.keyUse.getKey());
                    case "b" -> java.util.Arrays.stream(mc.options.keyMappings).filter(key -> key.getName().equals("key.backpacksplus.open"))
                            .findFirst().ifPresent(key -> KeyMapping.click(key.getKey()));
                    case "view" -> {
                        mc.options.setCameraType(CameraType.valueOf(c.get("camera").getAsString()));
                        mc.options.fov().set(c.get("fov").getAsInt()); mc.options.hideGui=c.get("hideGui").getAsBoolean();
                        float yaw=c.get("yaw").getAsFloat(), pitch=c.get("pitch").getAsFloat();
                        mc.player.setYRot(yaw); mc.player.yRotO=yaw; mc.player.yBodyRot=yaw; mc.player.yBodyRotO=yaw;
                        mc.player.setXRot(pitch); mc.player.xRotO=pitch;
                    }
                    case "record" -> {
                        capturePrefix=c.get("name").getAsString(); if (!capturePrefix.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("Unsafe frame name");
                        frames=Math.clamp(c.get("frames").getAsInt(),1,600); frameIndex=0; nextFrame=0;
                    }
                    case "h" -> KeyMapping.click(com.chunkworks.quickslot.client.ClientSetup.SWAP.getKey());
                    case "close" -> { if (mc.screen!=null) mc.screen.onClose(); }
                    case "menuClick" -> mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,c.get("slot").getAsInt(),c.get("button").getAsInt(),ClickType.valueOf(c.get("type").getAsString()),mc.player);
                    case "hud" -> {
                        if (c.has("width")) GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),c.get("width").getAsInt(),c.get("height").getAsInt());
                        mc.options.mainHand().set(HumanoidArm.valueOf(c.get("arm").getAsString()));
                        mc.options.attackIndicator().set(AttackIndicatorStatus.valueOf(c.get("attack").getAsString()));
                        mc.options.guiScale().set(c.get("scale").getAsInt()); mc.resizeDisplay(); mc.options.broadcastOptions();
                    }
                    case "capture" -> {
                        String name=c.get("name").getAsString(); if (!name.matches("[a-z0-9_-]+\\.png")) throw new IllegalArgumentException("Unsafe screenshot name");
                        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message -> {});
                    }
                    case "disconnect" -> { connect=false; mc.disconnect(new TitleScreen()); }
                    case "join" -> connect=true;
                    case "quit" -> { connect=false; mc.stop(); }
                    default -> throw new IllegalArgumentException("Unknown client operation");
                }
            }
            if (connect && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                String address="127.0.0.1:"+System.getProperty("backpacksplus.testPort");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Backpacks+ isolated test",address,ServerData.Type.OTHER),false,null);
            }
        } catch (Exception failure) { error=failure.toString(); LogUtils.getLogger().error("Backpack client fixture failed",failure); }
        JsonObject state=new JsonObject(); state.addProperty("seq",completed); state.addProperty("tick",ticks); state.addProperty("error",error);
        state.addProperty("connected",mc.player!=null); state.addProperty("focused",mc.isWindowActive());
        state.addProperty("browsing",GearClient.browsing()); state.addProperty("selection",GearClient.selection());
        state.addProperty("framesRemaining",frames);
        state.addProperty("guiWidth",mc.getWindow().getGuiScaledWidth()); state.addProperty("guiHeight",mc.getWindow().getGuiScaledHeight());
        JsonObject players=new JsonObject();
        if (mc.level!=null) for (var player : mc.level.players()) {
            JsonObject p=NetworkFiles.player(player); var view=GearClient.snapshot(player.getUUID());
            p.addProperty("skinModel",player.getSkin().model().name());
            if (view!=null) p.add("syncedBag",NetworkFiles.bag(view.bag()));
            if (view!=null) p.addProperty("openedAt",view.openedAt());
            var action=GearClient.action(player.getUUID()); if (action!=null) p.addProperty("action",action.kind().name());
            players.add(player.getGameProfile().getName(),p);
        }
        state.add("players",players);
        try { NetworkFiles.write(role+"-state",state); } catch (Exception failure) { LogUtils.getLogger().error("Backpack client evidence failed",failure); }
    }
}
