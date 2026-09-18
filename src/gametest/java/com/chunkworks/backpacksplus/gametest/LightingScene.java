/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;

/** Bounded dark-room fixture in the isolated world; changes real blocks and fluid states. */
final class LightingScene {
    private LightingScene() {}
    static void setup(ServerPlayer player) {
        var level=player.serverLevel();
        for(int x=54;x<=74;x++)for(int y=70;y<=78;y++)for(int z=-10;z<=10;z++)
            level.setBlock(new BlockPos(x,y,z),(x==54||x==74||y==70||y==78||z==-10||z==10 ? Blocks.STONE : Blocks.AIR).defaultBlockState(),3);
        level.setDayTime(18000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,player.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,player.getServer());
        player.teleportTo(level,64,71,0,0,0);
    }
    static void water(ServerPlayer player,boolean wet) {
        for(int x=55;x<74;x++)for(int y=71;y<=74;y++)for(int z=-9;z<10;z++)
            player.serverLevel().setBlock(new BlockPos(x,y,z),(wet ? Blocks.WATER : Blocks.AIR).defaultBlockState(),3);
        player.setAirSupply(300);
        player.teleportTo(player.serverLevel(),64,71,0,0,0);
    }
}
