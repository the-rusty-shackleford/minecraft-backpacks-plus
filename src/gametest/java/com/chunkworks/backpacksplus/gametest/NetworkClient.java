/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.client.GearClient;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
    private static final java.util.Map<java.util.UUID,java.util.function.Supplier<net.minecraft.client.resources.PlayerSkin>> CAPES=new java.util.HashMap<>();
    private static final java.lang.reflect.Method IRIS_ACTIVE=irisMethod();
    private static java.lang.reflect.Method irisMethod() {
        if(!ModList.get().isLoaded("iris"))return null;
        try{return Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("isShaderPackInUse");}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot inspect Iris API",failure);}
    }
    private static boolean shaders() {
        if(IRIS_ACTIVE==null)return false;
        try{return (Boolean)IRIS_ACTIVE.invoke(IRIS_ACTIVE.getDeclaringClass().getMethod("getInstance").invoke(null));}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot inspect active shaders",failure);}
    }
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
                completed=NetworkFiles.sequence(c); error="";
                switch (c.get("op").getAsString()) {
                    case "cape" -> {
                        // Offline fixture identities have no paid/account cape. Feed a
                        // native PlayerSkin descriptor to the real CapeLayer; restore its
                        // original lookup when disabled. No production rendering is replaced.
                        var info=mc.getConnection().getPlayerInfo(c.get("target").getAsString());
                        if(info==null)throw new IllegalStateException("Target player unavailable");
                        var field=net.minecraft.client.multiplayer.PlayerInfo.class.getDeclaredField("skinLookup");field.setAccessible(true);
                        if(c.get("enabled").getAsBoolean()) {
                            @SuppressWarnings("unchecked") var original=(java.util.function.Supplier<net.minecraft.client.resources.PlayerSkin>)field.get(info);
                            CAPES.putIfAbsent(info.getProfile().getId(),original);
                            var skin=info.getSkin();
                            var cape=new net.minecraft.client.resources.PlayerSkin(skin.texture(),skin.textureUrl(),
                                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/entity/elytra.png"),skin.elytraTexture(),skin.model(),skin.secure());
                            field.set(info,(java.util.function.Supplier<net.minecraft.client.resources.PlayerSkin>)()->cape);
                        } else if(CAPES.containsKey(info.getProfile().getId()))field.set(info,CAPES.remove(info.getProfile().getId()));
                    }
                    case "curios" -> CuriosClient.open();
                    case "curiosToggle" -> CuriosClient.toggle();
                    case "lightConfig" -> LightingNetwork.configure(c);
                    case "lightData" -> {
                        var folder=mc.gameDirectory.toPath().resolve("resourcepacks/backpacks-light-test");
                        java.nio.file.Files.createDirectories(folder.resolve("assets/backpacksplus/luminance"));
                        java.nio.file.Files.writeString(folder.resolve("pack.mcmeta"),"{\"pack\":{\"pack_format\":34,\"description\":\"Temporary mounted light verification\"}}");
                        java.nio.file.Files.writeString(folder.resolve("assets/backpacksplus/luminance/items.json"),"{\"minecraft:torch\":{\"luminance\":9,\"underwater\":true},\"farmersdelight:apple_cider\":11}");
                        mc.getResourcePackRepository().reload();
                        var packs=new java.util.ArrayList<>(mc.getResourcePackRepository().getSelectedIds());
                        packs.remove("file/backpacks-light-test");
                        if(c.get("enabled").getAsBoolean())packs.add("file/backpacks-light-test");
                        mc.getResourcePackRepository().setSelected(packs);mc.reloadResourcePacks();
                    }
                    case "placementData" -> {
                        var folder=mc.gameDirectory.toPath().resolve("resourcepacks/quickslot-fit-test");
                        java.nio.file.Files.createDirectories(folder.resolve("assets/quickslot/quickslot_placement/minecraft"));
                        java.nio.file.Files.writeString(folder.resolve("pack.mcmeta"),"{\"pack\":{\"pack_format\":34,\"description\":\"Temporary Quick Slot placement verification\"}}");
                        java.nio.file.Files.writeString(folder.resolve("assets/quickslot/quickslot_placement/minecraft/apple.json"),"{\"anchor\":\"lower_back\",\"with_backpack\":{\"offset\":[0,0.10,0],\"rotation\":[0,0,30],\"scale\":1.5}}");
                        mc.getResourcePackRepository().reload();
                        var packs=new java.util.ArrayList<>(mc.getResourcePackRepository().getSelectedIds());
                        packs.remove("file/quickslot-fit-test");if(c.get("enabled").getAsBoolean())packs.add("file/quickslot-fit-test");
                        mc.getResourcePackRepository().setSelected(packs);mc.reloadResourcePacks();
                    }
                    case "movement" -> {
                        mc.options.keyUp.setDown(c.has("forward")&&c.get("forward").getAsBoolean());
                        mc.options.keyLeft.setDown(c.has("left")&&c.get("left").getAsBoolean());
                        mc.options.keyRight.setDown(c.has("right")&&c.get("right").getAsBoolean());
                        mc.options.keyJump.setDown(c.has("jump")&&c.get("jump").getAsBoolean());
                        mc.options.keyShift.setDown(c.has("crouch")&&c.get("crouch").getAsBoolean());
                        mc.options.keySprint.setDown(c.has("sprint")&&c.get("sprint").getAsBoolean());
                    }
                    case "focus" -> { GLFW.glfwFocusWindow(mc.getWindow().getWindow()); mc.mouseHandler.grabMouse(); }
                    case "g" -> java.util.Arrays.stream(mc.options.keyMappings).filter(key -> key.getName().equals("key.backpacksplus.gear"))
                            .findFirst().orElseThrow().setDown(c.get("down").getAsBoolean());
                    case "scroll" -> {
                        var scroll=new InputEvent.MouseScrollingEvent(0,c.get("delta").getAsDouble(),false,false,false,0,0);
                        NeoForge.EVENT_BUS.post(scroll);
                        if (!scroll.isCanceled()) mc.player.getInventory().swapPaint(c.get("delta").getAsDouble());
                    }
                    case "use" -> KeyMapping.click(mc.options.keyUse.getKey());
                    case "inventory" -> KeyMapping.click(mc.options.keyInventory.getKey());
                    case "recipeBook" -> {
                        // The book's own button, through the screen's click path, so EMI's takeover of it runs too.
                        if (!(mc.screen instanceof InventoryScreen screen)) throw new IllegalStateException("Inventory screen not open");
                        double x=screen.getGuiLeft()+104+10, y=mc.getWindow().getGuiScaledHeight()/2-22+9;
                        screen.mouseClicked(x,y,0); screen.mouseReleased(x,y,0);
                    }
                    case "b" -> java.util.Arrays.stream(mc.options.keyMappings).filter(key -> key.getName().equals("key.backpacksplus.open"))
                            .findFirst().ifPresent(key -> KeyMapping.click(key.getKey()));
                    case "orbit" -> {
                        // A detached, unspawned vanilla camera lets one real client
                        // inspect its own networked player from either side.
                        var camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,0,0,0);
                        double x=c.get("x").getAsDouble(), y=c.get("y").getAsDouble()-camera.getEyeHeight(), z=c.get("z").getAsDouble();
                        camera.setPos(x,y,z); camera.xo=x; camera.yo=y; camera.zo=z;
                        camera.xOld=x; camera.yOld=y; camera.zOld=z;
                        camera.setYRot(c.get("yaw").getAsFloat()); camera.yRotO=camera.getYRot();
                        camera.setYHeadRot(camera.getYRot()); camera.yHeadRotO=camera.getYRot();
                        camera.setXRot(c.get("pitch").getAsFloat()); camera.xRotO=camera.getXRot();
                        mc.setCameraEntity(camera); mc.options.setCameraType(CameraType.FIRST_PERSON);
                        mc.options.fov().set(c.get("fov").getAsInt()); mc.options.hideGui=true;
                    }
                    case "view" -> {
                        mc.setCameraEntity(mc.player);
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
                    case "resources" -> {
                        var packs=new java.util.ArrayList<>(mc.getResourcePackRepository().getSelectedIds());
                        String refined="file/Refined Tools 3.0";
                        packs.remove(refined);
                        if (c.get("refined").getAsBoolean()) packs.add(refined);
                        mc.getResourcePackRepository().setSelected(packs);
                        mc.reloadResourcePacks();
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
                    case "respawn" -> mc.player.respawn();
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
        state.addProperty("reloading",mc.getOverlay()!=null);
        if(ModList.get().isLoaded("curios"))CuriosClient.observe(state);
        state.addProperty("curios",ModList.get().isLoaded("curios"));
        state.addProperty("luminance",ModList.get().isLoaded("luminance"));
        state.addProperty("shaders",shaders());
        state.addProperty("guiWidth",mc.getWindow().getGuiScaledWidth()); state.addProperty("guiHeight",mc.getWindow().getGuiScaledHeight());
        state.addProperty("screen",mc.screen==null ? "" : mc.screen.getClass().getSimpleName());
        if (mc.screen instanceof AbstractContainerScreen<?> container) { state.addProperty("guiLeft",container.getGuiLeft()); state.addProperty("guiTop",container.getGuiTop()); }
        if (mc.screen instanceof InventoryScreen inventory) state.addProperty("recipeBook",inventory.getRecipeBookComponent().isVisible());
        JsonObject players=new JsonObject();
        if (mc.level!=null) for (var player : mc.level.players()) {
            JsonObject p=NetworkFiles.player(player); var view=GearClient.snapshot(player.getUUID());
            if(ModList.get().isLoaded("luminance"))p.add("light",LightingNetwork.observe(player));
            p.addProperty("skinModel",player.getSkin().model().name());p.addProperty("cape",player.getSkin().capeTexture()!=null);
            if (view!=null) p.add("syncedBag",NetworkFiles.bag(view.bag()));
            if (view!=null) { p.addProperty("openedAt",view.openedAt()); p.addProperty("wornSource",view.wornSource()); p.addProperty("bagVisible",view.visible()); }
            var action=GearClient.action(player.getUUID()); if (action!=null) p.addProperty("action",action.kind().name());
            players.add(player.getGameProfile().getName(),p);
        }
        state.add("players",players);
        var vehicles=new com.google.gson.JsonArray();if(mc.level!=null&&ModList.get().isLoaded("vanillawheels"))for(var entity:mc.level.entitiesForRendering()){
            var vehicle=WheelsNetwork.describe(entity);if(vehicle!=null)vehicles.add(vehicle);
        }state.add("vehicles",vehicles);
        try { NetworkFiles.write(role+"-state",state); } catch (Exception failure) { LogUtils.getLogger().error("Backpack client evidence failed",failure); }
    }
}
