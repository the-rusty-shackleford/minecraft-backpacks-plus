/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.vanillawheels.Vehicle;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real independent-client vehicle fixtures. Inputs and movement still use the shipped
 * vehicle/client/server protocol; this adapter only arranges the course and reports poses. */
final class WheelsNetwork {
    private WheelsNetwork() {}
    static void execute(ServerPlayer player,JsonObject c) {
        var level=player.serverLevel();player.stopRiding();
        for(var other:level.players())other.stopRiding();
        for(var entity:level.getAllEntities())if(entity instanceof Vehicle && entity.getTags().contains("release_vehicle"))entity.discard();
        for(int x=12;x<=28;x++)for(int z=-16;z<=100;z++) {
            level.setBlock(new BlockPos(x,70,z),Blocks.STONE.defaultBlockState(),3);
            for(int y=71;y<=75;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        String profile=c.has("profile")?c.get("profile").getAsString():"trailblazer:trailblazer";
        Vehicle car=create(player,profile,20,0);player.setGameMode(GameType.CREATIVE);
        if(!player.startRiding(car,true))throw new IllegalStateException("Driver boarding failed");
        if(c.has("passenger")){
            var passenger=level.getServer().getPlayerList().getPlayerByName(c.get("passenger").getAsString());
            if(passenger==null || !passenger.startRiding(car,true))throw new IllegalStateException("Passenger boarding failed");
        }
        switch(c.get("course").getAsString()) {
            case "tow" -> {
                Vehicle trailer=create(player,"trailer:trailer",20,-5);
                Vec3 delta=car.hitchPoint().subtract(trailer.tongue());trailer.setPos(trailer.position().add(delta));car.hitch(trailer);
            }
            case "contact" -> create(player,profile,20,12);
            case "moving" -> {
                Vehicle target=create(player,profile,20,22,180);
                var other=level.getServer().getPlayerList().getPlayerByName("QuickViewer");other.setGameMode(GameType.CREATIVE);other.startRiding(target,true);
            }
            case "road" -> {for(int z=12;z<20;z++)for(int x=12;x<=28;x++)level.setBlock(new BlockPos(x,71,z),Blocks.STONE_SLAB.defaultBlockState(),3);}
            default -> throw new IllegalArgumentException("Unknown course");
        }
    }
    private static Vehicle create(ServerPlayer p,String id,int x,int z) {
        return create(p,id,x,z,0);
    }
    private static Vehicle create(ServerPlayer p,String id,int x,int z,float yaw) {
        Vehicle car=Vehicle.create(p.serverLevel(),ResourceLocation.parse(id),new Vec3(x,71,z),yaw);
        if(car==null)throw new IllegalStateException("Profile missing "+id);
        car.addTag("release_vehicle");p.serverLevel().addFreshEntity(car);return car;
    }
    static JsonObject describe(Entity entity) {
        if(!(entity instanceof Vehicle car))return null;
        var out=new JsonObject();out.addProperty("uuid",car.getUUID().toString());out.addProperty("id",car.getId());
        out.addProperty("profile",car.profileId().toString());out.addProperty("x",car.getX());out.addProperty("y",car.getY());out.addProperty("z",car.getZ());
        out.addProperty("yaw",car.getYRot());out.addProperty("lights",car.lights().name());out.addProperty("passengers",car.getPassengers().size());
        out.addProperty("trailer",car.trailer()==null?-1:car.trailer().getId());out.addProperty("tower",car.tower()==null?-1:car.tower().getId());
        if(car.trailer()!=null)out.addProperty("gap",car.hitchPoint().distanceTo(car.trailer().tongue()));
        return out;
    }
}
