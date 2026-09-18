/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.luminance.LuminanceConfig;
import com.chunkworks.luminance.client.Engine;
import com.chunkworks.luminance.client.Providers;
import com.chunkworks.luminance.domain.Source;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

/** Real-client observation of the installed lighting backend. Loaded only with Luminance;
 * no substitute engine/provider or simulated inventory is used. */
final class LightingNetwork {
    private LightingNetwork() {}
    static JsonObject observe(Player player) {
        JsonObject result=new JsonObject();
        result.addProperty("underwater",player.isUnderWater());
        result.addProperty("held",Math.max(Providers.luminanceOf(player.getMainHandItem(),player.isUnderWater()),
                Providers.luminanceOf(player.getOffhandItem(),player.isUnderWater())));
        var sources=new ArrayList<Source>(); Providers.collect(player,sources,1);
        JsonArray levels=new JsonArray(); for(var source:sources) levels.add(source.luminance());
        result.add("sources",levels);
        JsonArray field=new JsonArray();
        var pos=player.blockPosition();
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)field.add(Engine.lightAt(pos.getX()+x,pos.getY()-1,pos.getZ()+z));
        result.add("field",field);
        result.addProperty("worldBlockLight",player.level().getBrightness(LightLayer.BLOCK,pos));
        return result;
    }
    static void configure(JsonObject command) {
        if(command.has("quick"))com.chunkworks.quickslot.ClientRules.DYNAMIC_LIGHT.set(command.get("quick").getAsBoolean());
        if(command.has("enabled"))LuminanceConfig.ENABLED.set(command.get("enabled").getAsBoolean());
        if(command.has("held"))LuminanceConfig.HELD_ITEMS.set(command.get("held").getAsBoolean());
        if(command.has("entities"))LuminanceConfig.ENTITIES.set(command.get("entities").getAsBoolean());
    }
}
