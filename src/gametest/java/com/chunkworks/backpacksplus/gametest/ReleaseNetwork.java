/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Real-world release fixtures: poses, native equipment and registry-selected mod items.
 * Only the explicitly enabled disposable network server calls these operations. */
final class ReleaseNetwork {
    private ReleaseNetwork() {}
    static void execute(ServerPlayer player,JsonObject c) {
        var level=player.serverLevel();String action=c.get("action").getAsString();
        switch(action) {
            case "equipment" -> {
                var id=ResourceLocation.parse(c.get("item").getAsString());
                if(!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Missing item "+id);
                player.setItemSlot(EquipmentSlot.valueOf(c.get("slot").getAsString()),new ItemStack(BuiltInRegistries.ITEM.get(id)));
            }
            case "inventory" -> {
                var id=ResourceLocation.parse(c.get("item").getAsString());
                if(!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Missing item "+id);
                player.getInventory().setItem(c.get("slot").getAsInt(),new ItemStack(BuiltInRegistries.ITEM.get(id),c.has("count")?c.get("count").getAsInt():1));
            }
            case "pose" -> {
                for(var entity:level.getAllEntities())if(entity.getTags().contains("backpacksplus_release_pose"))entity.discard();
                player.closeContainer();player.stopRiding();if(player.isSleeping())player.stopSleepInBed(true,true);
                for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
                    level.setBlock(new BlockPos(x,70,z),Blocks.STONE.defaultBlockState(),3);
                    for(int y=71;y<=74;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                }
                player.teleportTo(level,0,71,0,0,0);player.setHealth(20);player.getFoodData().setFoodLevel(20);
                switch(c.get("pose").getAsString()) {
                    case "stand" -> level.setDayTime(6000);
                    case "swim" -> {
                        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=71;y<=73;y++)level.setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),3);
                        player.teleportTo(level,0,71.5,0,0,0);
                    }
                    case "sleep" -> {
                        level.setDayTime(18000);level.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(101,level.getServer());
                        BlockPos head=new BlockPos(0,71,0);
                        var bed=Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
                        level.setBlock(head,bed.setValue(BedBlock.PART,BedPart.HEAD),3);
                        level.setBlock(head.north(),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
                        var result=player.startSleepInBed(head);
                        if(result.left().isPresent())throw new IllegalStateException("Native sleep rejected: "+result.left().get());
                    }
                    case "boat","horse" -> {
                        var type=c.get("pose").getAsString().equals("boat")?EntityType.BOAT:EntityType.HORSE;
                        var mount=type.create(level);if(mount==null)throw new IllegalStateException("No pose mount");
                        mount.moveTo(0,71,0,0,0);mount.addTag("backpacksplus_release_pose");level.addFreshEntity(mount);
                        if(!player.startRiding(mount,true))throw new IllegalStateException("Native riding failed");
                    }
                    default -> throw new IllegalArgumentException("Unknown pose");
                }
            }
            default -> throw new IllegalArgumentException("Unknown release fixture");
        }
    }
}
