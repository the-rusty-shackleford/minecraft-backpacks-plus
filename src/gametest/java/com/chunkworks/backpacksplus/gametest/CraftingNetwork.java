/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.quickslot.Slot;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Test-only ingredient fixtures. Real client clicks take the normal crafting result;
 * this class never calls recipe assembly or supplies the output stack. */
final class CraftingNetwork {
    private CraftingNetwork() {}
    static void setup(ServerPlayer p,JsonObject command) {
        p.closeContainer();
        var source=p.getInventory().getItem(38);
        String recipe=command.get("recipe").getAsString();
        if(!recipe.equals("basic") && !(source.getItem() instanceof BackpackItem)) throw new IllegalStateException("Wear source bag first");
        p.getInventory().clearContent();Slot.replace(p,ItemStack.EMPTY);
        p.getInventory().selected=0;
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        BlockPos pos=new BlockPos(3,70,3);p.serverLevel().setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        p.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(p.serverLevel(),pos));
        Item[] items=switch(recipe) {
            case "basic" -> new Item[]{Items.STRING,Items.LEATHER,Items.STRING,Items.LEATHER,Items.WHITE_WOOL,Items.LEATHER,Items.LEATHER,Items.AIR,Items.LEATHER};
            case "reinforced" -> new Item[]{Items.IRON_INGOT,Items.LEATHER,Items.IRON_INGOT,Items.STRING,Items.AIR,Items.STRING,Items.IRON_INGOT,Items.AIR,Items.IRON_INGOT};
            case "expedition" -> new Item[]{Items.HONEYCOMB,Items.LEAD,Items.HONEYCOMB,Items.IRON_BLOCK,Items.AIR,Items.IRON_BLOCK,Items.LEATHER,Items.AIR,Items.LEATHER};
            case "recolor" -> new Item[]{Items.AIR,Items.AIR,Items.AIR,Items.AIR,Items.AIR,Items.AIR,Items.AIR,Items.AIR,Items.AIR};
            default -> throw new IllegalArgumentException("Unknown recipe");
        };
        for(int i=0;i<9;i++)p.containerMenu.getSlot(i+1).set(new ItemStack(items[i]));
        if(!recipe.equals("basic"))p.containerMenu.getSlot(5).set(source);
        if(command.has("dye") && !command.get("dye").getAsString().equals("none")) {
            var color=DyeColor.byName(command.get("dye").getAsString(),null);
            if(color==null)throw new IllegalArgumentException("Unknown dye");
            p.containerMenu.getSlot(8).set(new ItemStack(DyeItem.byColor(color)));
        }
    }
    static void fill(ServerPlayer p) {
        var bag=p.getInventory().getItem(38);var tier=BagContents.tier(bag);var inv=BagInventory.bind(p,38);
        for(int i=0;i<tier.storageSlots();i++)inv.setItem(i,new ItemStack(Items.FURNACE,i+1));
        for(int i=0;i<tier.mounts().size();i++)inv.setItem(tier.mountSlot(i),new ItemStack(tier.mounts().get(i)==BackpackTier.Mount.LONG ? (i==0?Items.DIAMOND_PICKAXE:Items.DIAMOND_SWORD) : Items.APPLE));
        bag.set(DataComponents.CUSTOM_NAME,Component.literal("Field pack"));
    }
}
