/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.quickslot.Slot;
import com.chunkworks.quickslot.SlotData;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.CuriosApi;

/** Real-client lifecycle fixtures. Partitions: chest/Curios; retention/drop/Vanishing;
 * exact component and content preservation; travel, tracking, relog and native respawn.
 * Expected stacks are independent copies; no production inventory or network is replaced. */
final class LifecycleNetwork {
    private LifecycleNetwork() {}
    private static ItemStack expectedBag=ItemStack.EMPTY,expectedQuick=ItemStack.EMPTY;
    static void execute(ServerPlayer p,JsonObject c) {
        var server=p.getServer();
        switch(c.get("action").getAsString()) {
            case "setup" -> {
                p.closeContainer();p.stopRiding();p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.getFoodData().setFoodLevel(20);
                p.teleportTo(server.overworld(),0,71,0,0,0);platform(p);
                p.getInventory().clearContent();CuriosNetwork.clear(p);p.getInventory().selected=0;
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                for(var drop:p.serverLevel().getEntities(EntityType.ITEM,new AABB(-10,60,-10,10,85,10),e->true))drop.discard();
                var bag=new ItemStack(BackpackItems.EXPEDITION.get());BagContents.identify(bag);
                bag.set(DataComponents.CUSTOM_NAME,Component.literal("Lifecycle pack"));bag.set(DataComponents.DYED_COLOR,new DyedItemColor(0x8b3f32,true));
                var cells=BagContents.copy(bag);cells.set(0,new ItemStack(Items.DIAMOND,64));cells.set(35,new ItemStack(Items.BREAD,8));
                var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(63);sword.set(DataComponents.CUSTOM_NAME,Component.literal("Mounted survivor"));
                cells.set(36,sword);cells.set(37,new ItemStack(Items.DIAMOND_PICKAXE));cells.set(38,new ItemStack(Items.TORCH,32));cells.set(39,new ItemStack(Items.APPLE,12));BagContents.store(bag,cells);
                var quick=new ItemStack(Items.SHEARS);quick.setDamageValue(17);quick.set(DataComponents.CUSTOM_NAME,Component.literal("Holstered survivor"));
                if(c.get("curse").getAsBoolean()){
                    var curse=p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE);
                    bag.enchant(curse,1);quick.enchant(curse,1);
                }
                if(c.get("route").getAsString().equals("curios"))CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("back",0,bag);
                else p.getInventory().setItem(38,bag);
                Slot.replace(p,quick);expectedBag=bag.copy();expectedQuick=quick.copy();
                server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(c.get("keep").getAsBoolean(),server);
                server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                com.chunkworks.quickslot.ServerRules.KEEP_ON_DEATH.set(false);
                p.serverLevel().setDayTime(6000);
            }
            case "kill" -> p.hurt(p.damageSources().genericKill(),Float.MAX_VALUE);
            case "travel" -> {
                var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(c.get("dimension").getAsString())));
                if(level==null)throw new IllegalArgumentException("Unknown test dimension");
                p.teleportTo(level,c.has("x")?c.get("x").getAsDouble():0,71,0,0,0);platform(p);
            }
            default -> throw new IllegalArgumentException("Unknown lifecycle operation");
        }
    }
    private static void platform(ServerPlayer p) {
        var origin=p.blockPosition().below();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            p.serverLevel().setBlock(origin.offset(x,0,z),Blocks.STONE.defaultBlockState(),3);
            for(int y=1;y<=3;y++)p.serverLevel().setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        p.setRespawnPosition(p.level().dimension(),origin.above(),0,true,false);
    }
    static JsonObject observe(ServerPlayer p) {
        JsonObject result=new JsonObject();result.addProperty("alive",p.isAlive());
        result.addProperty("bagIntact",!expectedBag.isEmpty()&&ItemStack.matches(expectedBag,GearSync.worn(p)));
        result.addProperty("quickIntact",!expectedQuick.isEmpty()&&ItemStack.matches(expectedQuick,SlotData.copy(p)));
        int bags=0,quick=0,matchingBags=0,matchingQuick=0;
        for(var drop:p.serverLevel().getEntities(EntityType.ITEM,new AABB(-10,60,-10,10,85,10),e->!e.isRemoved())){
            var stack=drop.getItem();if(stack.getItem() instanceof BackpackItem)bags++;
            if(stack.is(Items.SHEARS))quick++;
            if(!expectedBag.isEmpty()&&ItemStack.matches(expectedBag,stack))matchingBags++;
            if(!expectedQuick.isEmpty()&&ItemStack.matches(expectedQuick,stack))matchingQuick++;
        }
        result.addProperty("bagDrops",bags);result.addProperty("quickDrops",quick);
        result.addProperty("intactBagDrops",matchingBags);result.addProperty("intactQuickDrops",matchingQuick);return result;
    }
}
