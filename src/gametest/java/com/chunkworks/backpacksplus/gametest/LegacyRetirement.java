/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.quickslot.SlotData;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Real offline server restart experiment, enabled only by the legacyServer dev run.
 * Partitions: ordinary inventory, armor, Curios, container, dropped item and mob equipment.
 * Saves actual native NBT with SB installed, then loads the same world without SB/Core.
 * No player network, inventory, Curios or world-storage backend is substituted. */
@EventBusSubscriber(modid="backpacksplus_gametest")
public final class LegacyRetirement {
    private LegacyRetirement() {}
    private static int ticks;
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        String mode=System.getProperty("backpacksplus.legacyRetirement","");
        if(mode.isEmpty())return;
        var server=event.getServer();var level=server.overworld();
        if(++ticks==1)level.setChunkForced(0,0,true);
        if(ticks!=100)return;
        var report=new JsonObject(); report.addProperty("mode",mode);
        try {
            var p=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("6af52504-f8aa-4cbf-9755-314d0b440add"),"LegacyFixture"));
            Path save=Path.of("legacy-player.dat");
            BlockPos chestPos=new BlockPos(3,71,3);
            if(mode.equals("seed")) {
                var id=ResourceLocation.parse("sophisticatedbackpacks:backpack");
                require(BuiltInRegistries.ITEM.containsKey(id),"SB must be installed for seed");
                ItemStack bag=new ItemStack(BuiltInRegistries.ITEM.get(id));
                p.getInventory().clearContent();p.getInventory().setItem(0,bag.copy());p.getInventory().setItem(1,new ItemStack(Items.DIAMOND,17));
                p.setItemSlot(EquipmentSlot.CHEST,bag.copy());
                CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("back",0,bag.copy());
                require(!CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("back").orElseThrow().getStacks().getStackInSlot(0).isEmpty(),"real Curios seed");
                ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(54);sword.set(DataComponents.CUSTOM_NAME,Component.literal("Retained holster"));
                p.setData(SlotData.STACK,sword);
                NbtIo.writeCompressed(p.saveWithoutId(new CompoundTag()),save);
                for(int x=0;x<8;x++)for(int z=0;z<8;z++)level.setBlock(new BlockPos(x,70,z),Blocks.STONE.defaultBlockState(),3);
                level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
                var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);chest.setItem(0,bag.copy());chest.setItem(1,new ItemStack(Items.DIAMOND,32));chest.setChanged();
                var stand=new ArmorStand(level,5,71,5);stand.setItemSlot(EquipmentSlot.CHEST,bag.copy());stand.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));level.addFreshEntity(stand);
                var dropped=new ItemEntity(level,6,71,6,bag.copy());dropped.setUnlimitedLifetime();level.addFreshEntity(dropped);
                report.addProperty("seeded",true);
            } else if(mode.equals("verify")) {
                require(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("sophisticatedbackpacks:backpack")),"SB must be absent for verification");
                p.load(NbtIo.readCompressed(save,NbtAccounter.create(32L*1024*1024)));
                require(p.getInventory().getItem(0).isEmpty(),"inventory bag removed");
                require(p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"armor bag removed");
                require(CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("back").orElseThrow().getStacks().getStackInSlot(0).isEmpty(),"Curios bag removed");
                require(p.getInventory().getItem(1).is(Items.DIAMOND)&&p.getInventory().getItem(1).getCount()==17,"unrelated player stack intact");
                var quick=SlotData.copy(p);require(quick.is(Items.DIAMOND_SWORD)&&quick.getDamageValue()==54&&quick.getHoverName().getString().equals("Retained holster"),"Quick Slot intact");
                var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);require(chest!=null,"same saved chest reloaded");
                require(chest.getItem(0).isEmpty()&&chest.getItem(1).is(Items.DIAMOND)&&chest.getItem(1).getCount()==32,"only legacy container stack removed");
                var stands=level.getEntities(EntityType.ARMOR_STAND,new AABB(0,65,0,8,80,8),e->true);require(stands.size()==1,"same saved armor stand reloaded");
                require(stands.getFirst().getItemBySlot(EquipmentSlot.CHEST).isEmpty()&&stands.getFirst().getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS),"only legacy equipment removed");
                var drops=level.getEntities(EntityType.ITEM,new AABB(0,65,0,8,80,8),e->true);
                var details=new com.google.gson.JsonArray();
                for(var drop:drops){var detail=NetworkFiles.stack(drop.getItem());detail.addProperty("removed",drop.isRemoved());detail.addProperty("position",drop.position().toString());details.add(detail);}
                report.add("drops",details);
                // The entity lookup can still contain a discarded tombstone on this
                // first load. It must hold no item and already be marked removed.
                require(drops.stream().allMatch(drop->drop.isRemoved()&&drop.getItem().isEmpty()),"no live or recoverable legacy dropped item");
                report.addProperty("verified",true);
            } else throw new IllegalArgumentException("Unknown mode "+mode);
        } catch(Exception|AssertionError failure) {report.addProperty("error",failure.toString());throw failure;}
        finally {Files.writeString(Path.of("legacy-"+mode+"-result.json"),report.toString());server.halt(false);}
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
