/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;

/** Test-only actual Curios fixtures. Equipping in the test uses the real client's menu clicks. */
final class CuriosNetwork {
    private CuriosNetwork() {}
    static void clear(Player p) {
        var handler=CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("back").orElseThrow();
        for(int i=0;i<handler.getSlots();i++){handler.getStacks().setStackInSlot(i,ItemStack.EMPTY);handler.getRenders().set(i,true);}
    }
    static void prepare(ServerPlayer p,JsonObject c) {
        clear(p);p.closeContainer();
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("backpacksplus:"+c.get("tier").getAsString()+"_backpack"));
        var bag=new ItemStack(item);p.getInventory().setItem(0,bag);p.getInventory().selected=0;
        var inv=BagInventory.bind(p,0);var tier=BagContents.tier(bag);
        inv.setItem(0,new ItemStack(Items.BREAD,8));
        inv.setItem(tier.mountSlot(0),new ItemStack(Items.DIAMOND_SWORD));
        inv.setItem(tier.mountSlot(tier.mounts().size()-1),new ItemStack(Items.TORCH));
        p.getInventory().setItem(38,new ItemStack(Items.IRON_CHESTPLATE));
    }
    static void control(ServerPlayer p,JsonObject c) {
        var handler=CuriosApi.getCuriosInventory(p).orElseThrow();
        switch(c.get("mode").getAsString()) {
            case "replace" -> handler.setEquippedCurio("back",0,BagLocations.stack(p,41).copy());
            case "remove" -> handler.setEquippedCurio("back",0,ItemStack.EMPTY);
            case "active" -> handler.setSlotActive("back",0,c.get("active").getAsBoolean());
            case "dual" -> {var bag=new ItemStack(BackpackItems.BASIC.get());BagContents.identify(bag);p.getInventory().setItem(38,bag);}
            case "armor" -> p.getInventory().setItem(38,c.get("enabled").getAsBoolean() ? new ItemStack(Items.IRON_CHESTPLATE) : ItemStack.EMPTY);
            case "elytra" -> p.getInventory().setItem(38,c.get("enabled").getAsBoolean() ? new ItemStack(Items.ELYTRA) : ItemStack.EMPTY);
            default -> throw new IllegalArgumentException("Unknown Curios fixture");
        }
    }
    static JsonObject observe(Player p) {
        var result=new JsonObject();var back=CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("back").orElseThrow();
        result.addProperty("slots",back.getSlots());result.add("back",NetworkFiles.bag(back.getStacks().getStackInSlot(0)));
        result.addProperty("visible",back.getRenders().get(0));result.addProperty("active",back.getActiveStates().get(0));
        result.add("chest",NetworkFiles.bag(p.getInventory().getItem(38)));return result;
    }
}
