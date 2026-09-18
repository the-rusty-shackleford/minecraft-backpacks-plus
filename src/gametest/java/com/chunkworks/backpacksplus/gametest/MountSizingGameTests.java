/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Partitions: actual mod guns, pistol/long guns; all tiers; survival/creative;
 * empty/occupied mounts; G exchange/menu clicks; pre-existing wrong-size contents.
 * compatible pack gear, knives/sidearms/magazines, tagged vanilla tools.
 * Actual registered items and inventories; no synthetic weapon substitutes. */
@PrefixGameTestTemplate(false)
public final class MountSizingGameTests {
    public MountSizingGameTests() {}
    private static final List<String> GUNS=List.of("rifle","scoped_rifle","shotgun","machine_gun","pistol");
    private static ItemStack gun(String name) {
        var id=ResourceLocation.fromNamespaceAndPath("rangedweaponsmod",name);
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalStateException("Missing real test gun: "+id);
        ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Keep "+name));
        stack.setDamageValue(7);
        return stack;
    }
    private static Player player(GameTestHelper h,GameType mode,BackpackTier tier) {
        Player p=h.makeMockPlayer(mode);
        p.getInventory().setItem(38,new ItemStack(switch(tier) {
            case BASIC -> BackpackItems.BASIC.get(); case REINFORCED -> BackpackItems.REINFORCED.get();
            case EXPEDITION -> BackpackItems.EXPEDITION.get();
        }));
        BagContents.identify(bag(p));return p;
    }
    private static ItemStack bag(Player p) {return p.getInventory().getItem(38);}
    private static boolean swap(Player p,int mount) {
        return MountExchange.swap(p,38,bag(p).get(BackpackItems.ID),BagContents.revision(bag(p)),mount,0);
    }
    @GameTest(template="empty",templateNamespace="backpacksplus") public void knownPackGearUsesIntendedMounts(GameTestHelper h) {
        // Optional mods are checked when installed; the five RWM guns are mandatory
        // for this suite, registered only when RWM is loaded by TestMod.
        for(String id:List.of(
                "rangedweaponsmod:rifle",
                "rangedweaponsmod:scoped_rifle",
                "rangedweaponsmod:shotgun",
                "rangedweaponsmod:machine_gun",
                "anothergunmod:rifle",
                "anothergunmod:rifle_spyglass",
                "anothergunmod:shotgun",
                "anothergunmod:auto_gun",
                "anothergunmod:machine_gun",
                "anothergunmod:flame_thrower",
                "anothergunmod:soul_of_mosin",
                "alexsmobs:shield_of_the_deep",
                "mowziesmobs:blowgun",
                "mowziesmobs:sand_rake",
                "create:potato_cannon",
                "create:handheld_worldshaper",
                "minecraft:diamond_sword",
                "minecraft:diamond_pickaxe",
                "minecraft:bow",
                "minecraft:crossbow",
                "minecraft:trident",
                "minecraft:shield",
                "minecraft:mace",
                "minecraft:fishing_rod",
                "mowziesmobs:spear",
                "mowziesmobs:sculptor_staff",
                "alexsmobs:skelewag_sword",
                "alexsmobs:ghostly_pickaxe",
                "block_factorys_bosses:large_sword",
                "block_factorys_bosses:kraken_trident")) checkGear(h,id,true);
        for(String id:List.of(
                "rangedweaponsmod:pistol",
                "anothergunmod:revolver",
                "mowziesmobs:naga_fang_dagger",
                "block_factorys_bosses:dagger",
                "minecraft:shears",
                "minecraft:brush",
                "minecraft:flint_and_steel",
                "minecraft:torch",
                "minecraft:lantern",
                "minecraft:apple",
                "rangedweaponsmod:pistol_magazine",
                "rangedweaponsmod:rifle_magazine",
                "rangedweaponsmod:machine_gun_box",
                "farmersdelight:flint_knife",
                "farmersdelight:iron_knife",
                "farmersdelight:golden_knife",
                "farmersdelight:diamond_knife",
                "farmersdelight:netherite_knife",
                "create:wrench",
                "vanillawheels:wrench")) checkGear(h,id,false);
        h.succeed();
    }
    private static void checkGear(GameTestHelper h,String name,boolean expectedLong) {
        var id=ResourceLocation.parse(name);
        if(!BuiltInRegistries.ITEM.containsKey(id)) return;
        ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
        for(BackpackTier tier:BackpackTier.values()) {
            Player p=player(h,GameType.SURVIVAL,tier);
            BackpackMenu menu=new BackpackMenu(1,p.getInventory(),BagInventory.bind(p,38),tier,38);
            for(int mount=0;mount<tier.mounts().size();mount++) {
                boolean expected=(tier.mounts().get(mount)==BackpackTier.Mount.LONG)==expectedLong;
                menu.setCarried(stack.copy());menu.clicked(tier.mountSlot(mount),0,ClickType.PICKUP,p);
                var mounted=BagContents.copy(bag(p)).get(tier.mountSlot(mount));
                h.assertTrue(expected ? ItemStack.matches(stack,mounted) && menu.getCarried().isEmpty()
                        : mounted.isEmpty() && ItemStack.matches(stack,menu.getCarried()),name+" "+tier+" mount="+mount+" expected="+expected);
            }
        }
    }
    @GameTest(template="empty",templateNamespace="backpacksplus") public void gunsRespectMountSizeThroughExchange(GameTestHelper h) {
        for(String name:GUNS) for(BackpackTier tier:BackpackTier.values())
        for(GameType mode:List.of(GameType.SURVIVAL,GameType.CREATIVE))
        for(int mount=0;mount<tier.mounts().size();mount++) for(boolean occupied:new boolean[]{false,true}) {
            Player p=player(h,mode,tier);boolean large=tier.mounts().get(mount)==BackpackTier.Mount.LONG;
            ItemStack old=occupied ? new ItemStack(large ? Items.DIAMOND_SWORD : Items.APPLE) : ItemStack.EMPTY;
            if(occupied) BagInventory.bind(p,38).setItem(tier.mountSlot(mount),old);
            ItemStack held=gun(name);p.getInventory().setItem(0,held.copy());ItemStack before=bag(p).copy();
            boolean expected=large!=name.equals("pistol");boolean result=swap(p,mount);
            h.assertTrue(result==expected,name+" "+tier+" "+mode+" mount="+mount+" occupied="+occupied+" expected="+expected+" actual="+result);
            if(expected) {
                h.assertTrue(ItemStack.matches(old,p.getMainHandItem()),"exact previous mounted stack drawn");
                h.assertTrue(ItemStack.matches(held,BagContents.copy(bag(p)).get(tier.mountSlot(mount))),"gun components preserved");
            } else {
                h.assertTrue(ItemStack.matches(before,bag(p)) && ItemStack.matches(held,p.getMainHandItem()),"refusal preserves both stacks and revision");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus") public void gunsRespectMountSizeThroughMenu(GameTestHelper h) {
        for(String name:GUNS) for(BackpackTier tier:BackpackTier.values()) for(int mount=0;mount<tier.mounts().size();mount++) {
            Player p=player(h,GameType.SURVIVAL,tier);
            BackpackMenu menu=new BackpackMenu(1,p.getInventory(),BagInventory.bind(p,38),tier,38);
            ItemStack held=gun(name);menu.setCarried(held.copy());menu.clicked(tier.mountSlot(mount),0,ClickType.PICKUP,p);
            boolean expected=(tier.mounts().get(mount)==BackpackTier.Mount.LONG)!=name.equals("pistol");
            ItemStack mounted=BagContents.copy(bag(p)).get(tier.mountSlot(mount));
            h.assertTrue(expected ? ItemStack.matches(held,mounted) && menu.getCarried().isEmpty()
                : mounted.isEmpty() && ItemStack.matches(held,menu.getCarried()),name+" menu mount="+mount+" expected="+expected);
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus") public void formerlySmallGunsRemainRecoverable(GameTestHelper h) {
        for(String name:GUNS) if(!name.equals("pistol")) for(boolean stow:new boolean[]{false,true}) {
            Player p=player(h,GameType.SURVIVAL,BackpackTier.BASIC);ItemStack held=gun(name);
            var cells=BagContents.copy(bag(p));cells.set(10,held.copy());
            bag(p).set(DataComponents.CONTAINER,ItemContainerContents.fromItems(cells));
            if(stow) {
                h.assertTrue(MountExchange.stow(p,38,bag(p).get(BackpackItems.ID),BagContents.revision(bag(p)),1,0)==MountExchange.StowResult.STORED,"legacy gun stows");
                h.assertTrue(ItemStack.matches(held,BagContents.copy(bag(p)).getFirst()),"legacy components kept in storage");
            } else {
                h.assertTrue(swap(p,1),"legacy gun draws");
                h.assertTrue(ItemStack.matches(held,p.getMainHandItem()),"legacy components kept in hand");
                h.assertTrue(!swap(p,1),"drawn long gun cannot reenter small mount");
            }
            h.assertTrue(BagContents.copy(bag(p)).get(10).isEmpty(),"legacy small mount empty after retrieval");
        }
        h.succeed();
    }
}
