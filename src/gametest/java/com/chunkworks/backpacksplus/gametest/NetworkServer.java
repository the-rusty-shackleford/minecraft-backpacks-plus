/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.quickslot.Slot;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Dedicated test server fixtures. All tested user exchanges still arrive over normal mod packets. */
@EventBusSubscriber(modid="backpacksplus_gametest")
public final class NetworkServer {
    private NetworkServer() {}
    private static int ticks;
    private static int completed=NetworkFiles.initialSequence("server");
    private static String error="";
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!NetworkFiles.ENABLED || ++ticks%5!=0) return;
        var server=event.getServer();
        try {
            var command=NetworkFiles.read("server-command");
            if (NetworkFiles.sequence(command)>completed) {
                completed=NetworkFiles.sequence(command); error=""; String op=command.get("op").getAsString();
                if (op.equals("quit")) { server.halt(false); return; }
                var p=server.getPlayerList().getPlayerByName(command.has("target") ? command.get("target").getAsString() : "QuickDriver");
                if (p==null) throw new IllegalStateException("Target not connected");
                switch (op) {
                    case "release" -> ReleaseNetwork.execute(p,command);
                    case "wheels" -> WheelsNetwork.execute(p,command);
                    case "life" -> LifecycleNetwork.execute(p,command);
                    case "curiosPrepare" -> CuriosNetwork.prepare(p,command);
                    case "curiosControl" -> CuriosNetwork.control(p,command);
                    case "darkRoom" -> LightingScene.setup(p);
                    case "water" -> LightingScene.water(p,command.get("wet").getAsBoolean());
                    case "cell" -> BagInventory.bind(p,BagLocations.worn(p)).setItem(command.get("slot").getAsInt(),new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(command.get("item").getAsString()))));
                    case "pocketBag" -> { var bag=p.getInventory().getItem(38);p.getInventory().setItem(38,ItemStack.EMPTY);p.getInventory().setItem(9,bag); }
                    case "unPocketBag" -> { var bag=p.getInventory().getItem(9);p.getInventory().setItem(9,ItemStack.EMPTY);p.getInventory().setItem(38,bag); }
                    case "craftSetup" -> CraftingNetwork.setup(p,command);
                    case "craftFill" -> CraftingNetwork.fill(p);
                    case "seed" -> {
                        p.closeContainer(); p.stopRiding();
                        if(net.neoforged.fml.ModList.get().isLoaded("curios"))CuriosNetwork.clear(p); p.getInventory().clearContent(); p.getInventory().selected=0;
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                        p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.getFoodData().setFoodLevel(20);
                        ItemStack bag=new ItemStack(BackpackItems.EXPEDITION.get()); p.getInventory().setItem(38,bag);
                        var inventory=BagInventory.bind(p,38);
                        inventory.setItem(36,new ItemStack(Items.DIAMOND_PICKAXE)); inventory.setItem(37,new ItemStack(Items.DIAMOND_SWORD));
                        inventory.setItem(38,new ItemStack(Items.APPLE,12)); inventory.setItem(39,new ItemStack(Items.TORCH,32));
                        inventory.setItem(0,new ItemStack(Items.BREAD,8));
                        Slot.replace(p,new ItemStack(Items.SHEARS)); p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_AXE));
                        p.getInventory().setItem(1,new ItemStack(Items.FURNACE,16)); p.getInventory().setItem(2,new ItemStack(Items.SHULKER_BOX));
                        for (int x=-7;x<=7;x++) for (int z=-7;z<=7;z++) p.serverLevel().setBlock(new BlockPos(x,70,z),Blocks.STONE.defaultBlockState(),3);
                        p.teleportTo(p.serverLevel(),0,71,0,0,0); p.serverLevel().setDayTime(6000);
                        server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                    }
                    case "holdBag" -> { var bag=p.getInventory().getItem(38); p.getInventory().setItem(38,ItemStack.EMPTY); p.getInventory().setItem(0,bag); }
                    case "wearBag" -> { var bag=p.getInventory().getItem(0); p.getInventory().setItem(0,ItemStack.EMPTY); p.getInventory().setItem(38,bag); }
                    case "quick" -> Slot.replace(p,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(command.get("item").getAsString())),command.has("count") ? command.get("count").getAsInt() : 1));
                    case "held" -> p.getInventory().setItem(p.getInventory().selected,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(command.get("item").getAsString())),command.has("count") ? command.get("count").getAsInt() : 1));
                    case "full" -> { var inv=BagInventory.bind(p,BagLocations.worn(p)); for (int i=0;i<BagContents.tier(GearSync.worn(p)).storageSlots();i++) inv.setItem(i,new ItemStack(Items.STONE,64)); }
                    case "mounts" -> {
                        var bag=GearSync.worn(p); var inv=BagInventory.bind(p,BagLocations.worn(p)); var tier=BagContents.tier(bag);
                        var mounts=command.getAsJsonArray("items");
                        if (mounts.size()!=tier.mounts().size()) throw new IllegalArgumentException("Wrong mount count");
                        for (int i=0;i<mounts.size();i++) {
                            var id=ResourceLocation.parse(mounts.get(i).getAsString());
                            if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown item "+id);
                            inv.setItem(tier.mountSlot(i),new ItemStack(BuiltInRegistries.ITEM.get(id)));
                        }
                        Slot.replace(p,ItemStack.EMPTY); p.getInventory().setItem(p.getInventory().selected,ItemStack.EMPTY);
                    }
                    case "position" -> p.teleportTo(p.serverLevel(),command.get("x").getAsDouble(),command.has("y") ? command.get("y").getAsDouble() : 71,command.get("z").getAsDouble(),0,0);
                    case "save" -> server.getPlayerList().saveAll();
                    default -> throw new IllegalArgumentException("Unknown operation "+op);
                }
                p.containerMenu.broadcastChanges(); p.inventoryMenu.broadcastChanges();
            }
        } catch (Exception failure) { error=failure.toString(); LogUtils.getLogger().error("Backpack fixture failed",failure); }
        JsonObject state=new JsonObject(); state.addProperty("seq",completed); state.addProperty("tick",ticks); state.addProperty("error",error);
        JsonObject players=new JsonObject(); for (var p : server.getPlayerList().getPlayers()) players.add(p.getGameProfile().getName(),NetworkFiles.player(p)); state.add("players",players);
        var vehicles=new com.google.gson.JsonArray();for(var entity:server.overworld().getAllEntities()){
            var vehicle=WheelsNetwork.describe(entity);if(vehicle!=null)vehicles.add(vehicle);
        }state.add("vehicles",vehicles);
        var actor=server.getPlayerList().getPlayerByName("QuickDriver");
        if(actor!=null && net.neoforged.fml.ModList.get().isLoaded("curios"))state.add("lifecycle",LifecycleNetwork.observe(actor));
        try { NetworkFiles.write("server-state",state); } catch (Exception failure) { LogUtils.getLogger().error("Backpack evidence failed",failure); }
    }
}
