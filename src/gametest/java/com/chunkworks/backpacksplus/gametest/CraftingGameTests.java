/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.backpacksplus.domain.UpgradeLayout;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Partitions: all tiers and 16 dyes; dyed/undyed creation; both upgrade edges with
 * full storage and mounts; pure repeated previews; normal/shift result clicks;
 * full player inventory; extra/missing/wrong-tier ingredients; corrupt count/overflow/
 * revision; registry save/load and recipe network codecs. FakePlayer provides input;
 * matching, crafting menus, result consumption and serialization are real backends. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class CraftingGameTests {
    public CraftingGameTests() {}
    private static Item item(BackpackTier tier) {
        return switch(tier) {case BASIC -> BackpackItems.BASIC.get();case REINFORCED -> BackpackItems.REINFORCED.get();case EXPEDITION -> BackpackItems.EXPEDITION.get();};
    }
    private static List<ItemStack> grid(BackpackTier result,ItemStack old,ItemStack dye,int count) {
        Item[] materials=switch(result) {
            case BASIC -> new Item[]{Items.STRING,Items.LEATHER,Items.STRING,Items.LEATHER,Items.RED_WOOL,Items.LEATHER,Items.LEATHER,Items.AIR,Items.LEATHER};
            case REINFORCED -> new Item[]{Items.IRON_INGOT,Items.LEATHER,Items.IRON_INGOT,Items.STRING,Items.AIR,Items.STRING,Items.IRON_INGOT,Items.AIR,Items.IRON_INGOT};
            case EXPEDITION -> new Item[]{Items.HONEYCOMB,Items.LEAD,Items.HONEYCOMB,Items.IRON_BLOCK,Items.AIR,Items.IRON_BLOCK,Items.LEATHER,Items.AIR,Items.LEATHER};
        };
        List<ItemStack> cells=new ArrayList<>();for(var material:materials) cells.add(new ItemStack(material,count));
        if(result!=BackpackTier.BASIC) cells.set(4,old);
        cells.set(7,dye);return cells;
    }
    private static CraftingInput input(List<ItemStack> grid) { return CraftingInput.of(3,3,grid); }
    private static ItemStack craft(GameTestHelper h,CraftingInput input) {
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
        return recipe.map(r -> r.value().assemble(input,h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }
    private static ItemStack filled(BackpackTier tier) {
        var bag=new ItemStack(item(tier));BagContents.identify(bag);
        bag.set(DataComponents.CUSTOM_NAME,Component.literal("Trail bag"));
        CompoundTag marker=new CompoundTag();marker.putString("preserve","third-party-data");bag.set(DataComponents.CUSTOM_DATA,CustomData.of(marker));
        bag.set(DataComponents.DYED_COLOR,new DyedItemColor(DyeColor.RED.getTextureDiffuseColor(),true));
        var cells=BagContents.copy(bag);
        for(int i=0;i<tier.storageSlots();i++) cells.set(i,new ItemStack(Items.FURNACE,i+1));
        for(int i=0;i<tier.mounts().size();i++) cells.set(tier.mountSlot(i),new ItemStack(tier.mounts().get(i)==BackpackTier.Mount.LONG ? (i==0?Items.DIAMOND_PICKAXE:Items.IRON_SWORD) : Items.APPLE,i<2?1:7));
        // Basic's second mount is small and may carry a whole stack.
        if(tier==BackpackTier.BASIC) cells.set(tier.mountSlot(1),new ItemStack(Items.APPLE,7));
        cells.get(tier.mountSlot(0)).set(DataComponents.DAMAGE,71);
        BagContents.store(bag,cells);return bag;
    }
    private static void retained(GameTestHelper h,ItemStack old,ItemStack upgraded) {
        var from=BagContents.tier(old);var to=BagContents.tier(upgraded);
        h.assertTrue(old.get(BackpackItems.ID).equals(upgraded.get(BackpackItems.ID)),"UUID retained");
        h.assertTrue(old.get(DataComponents.CUSTOM_NAME).equals(upgraded.get(DataComponents.CUSTOM_NAME)),"name retained");
        h.assertTrue(old.get(DataComponents.CUSTOM_DATA).equals(upgraded.get(DataComponents.CUSTOM_DATA)),"unknown data retained");
        h.assertTrue(BagContents.revision(upgraded)==BagContents.revision(old)+1,"one revision per successful craft");
        var before=BagContents.copy(old);var after=BagContents.copy(upgraded);
        for(int i=0;i<before.size();i++) {
            int dest=from==to?i:UpgradeLayout.destination(from,to,i);
            h.assertTrue(ItemStack.matches(before.get(i),after.get(dest)),"stored item/mount retains count and data at "+i);
        }
        h.assertValueEqual(after.stream().filter(s->!s.isEmpty()).count(),before.stream().filter(s->!s.isEmpty()).count(),"extra cells start empty");
    }
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"crafting-check"));
        p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
        var pos=h.absolutePos(new BlockPos(1,1,1));p.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);return p;
    }
    private static CraftingMenu menu(GameTestHelper h,ServerPlayer p,List<ItemStack> grid) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Blocks.CRAFTING_TABLE);
        var menu=new CraftingMenu(31,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),h.absolutePos(pos)));p.containerMenu=menu;
        for(int i=0;i<9;i++) menu.getSlot(i+1).set(grid.get(i));
        return menu;
    }
    @GameTest(template="empty") public void allRecipesAndUnlocksLoadAndSync(GameTestHelper h) {
        for(var tier:BackpackTier.values()) {
            String name=tier.name().toLowerCase(java.util.Locale.ROOT);
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(BackpacksPlus.id("recipes/"+name))!=null,"recipe book unlock loaded");
            for(String suffix:List.of("","_dyed","_recolor")) {
                var holder=h.getLevel().getRecipeManager().byKey(BackpacksPlus.id(name+suffix)).orElseThrow();
                var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
                try {
                    RecipeHolder.STREAM_CODEC.encode(buffer,holder);
                    var decoded=RecipeHolder.STREAM_CODEC.decode(buffer);
                    h.assertTrue(decoded.id().equals(holder.id()),"recipe ID roundtrip");
                    h.assertTrue(decoded.value().getSerializer()==holder.value().getSerializer(),"custom serializer roundtrip");
                    h.assertTrue(decoded.value().getResultItem(h.getLevel().registryAccess()).is(item(tier)),"recipe-book tier result");
                } finally {buffer.release();}
            }
        }h.succeed();
    }
    @GameTest(template="empty") public void basicCraftsUndyedAndEveryDyeWithColoredWool(GameTestHelper h) {
        var plain=craft(h,input(grid(BackpackTier.BASIC,ItemStack.EMPTY,ItemStack.EMPTY,1)));
        h.assertTrue(plain.is(BackpackItems.BASIC.get()) && !plain.has(DataComponents.DYED_COLOR),"undyed Basic uses natural palette");
        for(var color:DyeColor.values()) {
            var result=craft(h,input(grid(BackpackTier.BASIC,ItemStack.EMPTY,new ItemStack(DyeItem.byColor(color)),1)));
            h.assertTrue(result.is(BackpackItems.BASIC.get()),"Basic crafts with "+color);
            h.assertValueEqual(result.get(DataComponents.DYED_COLOR).rgb(),color.getTextureDiffuseColor(),"exact selected dye");
        }h.succeed();
    }
    @GameTest(template="empty") public void upgradesPreserveEveryCellAndSupportEveryDye(GameTestHelper h) {
        for(int stage=0;stage<2;stage++) {
            var from=BackpackTier.values()[stage];var to=BackpackTier.values()[stage+1];var old=filled(from);var snapshot=old.copy();
            var unchanged=craft(h,input(grid(to,old,ItemStack.EMPTY,1)));retained(h,snapshot,unchanged);
            h.assertTrue(unchanged.get(DataComponents.DYED_COLOR).equals(old.get(DataComponents.DYED_COLOR)),"no dye retains color");
            for(var color:DyeColor.values()) {
                var result=craft(h,input(grid(to,old,new ItemStack(DyeItem.byColor(color)),1)));retained(h,snapshot,result);
                h.assertValueEqual(result.get(DataComponents.DYED_COLOR).rgb(),color.getTextureDiffuseColor(),"upgrade dye overrides color");
                h.assertTrue(ItemStack.matches(old,snapshot),"repeated recipe preview never mutates source");
            }
        }h.succeed();
    }
    @GameTest(template="empty") public void recolorEveryTierAndDyeInTwoByTwo(GameTestHelper h) {
        for(var tier:BackpackTier.values())for(var color:DyeColor.values()) {
            var old=filled(tier);var before=old.copy();
            var result=craft(h,CraftingInput.of(2,2,List.of(ItemStack.EMPTY,new ItemStack(DyeItem.byColor(color)),old,ItemStack.EMPTY)));
            retained(h,before,result);h.assertTrue(result.is(item(tier)),"recolor keeps tier");
            h.assertValueEqual(result.get(DataComponents.DYED_COLOR).rgb(),color.getTextureDiffuseColor(),"recolor replaces rather than mixes");
            h.assertTrue(ItemStack.matches(old,before),"recolor preview leaves input intact");
        }h.succeed();
    }
    @GameTest(template="empty") public void resultClickConsumesExactIngredientsOnce(GameTestHelper h) {
        var p=player(h);var old=filled(BackpackTier.BASIC);var snapshot=old.copy();
        var menu=menu(h,p,grid(BackpackTier.REINFORCED,old,new ItemStack(Items.BLUE_DYE,2),2));
        h.assertTrue(menu.getSlot(0).getItem().is(BackpackItems.REINFORCED.get()),"real menu calculates result");
        menu.clicked(0,0,ClickType.PICKUP,p);var result=menu.getCarried();retained(h,snapshot,result);
        h.assertTrue(menu.getSlot(5).getItem().isEmpty(),"old bag consumed once");
        for(int i=1;i<=9;i++) if(i!=5)h.assertValueEqual(menu.getSlot(i).getItem().getCount(),1,"one material consumed in cell "+i);
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"cannot craft a second copy without source bag");
        menu.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(menu.getCarried()==result,"repeated click does not duplicate");h.succeed();
    }
    @GameTest(template="empty") public void fullInventoryRefusesShiftThenCraftsAfterSpace(GameTestHelper h) {
        var p=player(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        var old=filled(BackpackTier.REINFORCED);var snapshot=old.copy();var menu=menu(h,p,grid(BackpackTier.EXPEDITION,old,ItemStack.EMPTY,1));
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(ItemStack.matches(menu.getSlot(5).getItem(),snapshot),"full inventory leaves source bag untouched");
        h.assertTrue(menu.getSlot(0).getItem().is(BackpackItems.EXPEDITION.get()),"refused result still available");
        p.getInventory().setItem(35,ItemStack.EMPTY);menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        var result=p.getInventory().getItem(35);retained(h,snapshot,result);
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"shift consumed source once");
        for(int i=1;i<=9;i++)h.assertTrue(menu.getSlot(i).getItem().isEmpty(),"all ingredients consumed exactly");h.succeed();
    }
    @GameTest(template="empty") public void invalidAndOverflowInputsRefuseWithoutMutation(GameTestHelper h) {
        var bag=filled(BackpackTier.BASIC);
        for(int mode=0;mode<6;mode++) {
            var bad=bag.copy();
            if(mode==0)bad.setCount(2);
            if(mode==1){var cells=NonNullList.withSize(12,ItemStack.EMPTY);cells.set(11,new ItemStack(Items.DIAMOND));bad.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(cells));}
            if(mode==2)bad.set(BackpackItems.REVISION,Long.MAX_VALUE);
            if(mode==3)bad=new ItemStack(BackpackItems.EXPEDITION.get());
            if(mode==5)bad.set(BackpackItems.REVISION,-1L);
            var cells=grid(BackpackTier.REINFORCED,bad,ItemStack.EMPTY,1);
            if(mode==4)cells.set(0,ItemStack.EMPTY);
            var snapshot=bad.copy();h.assertTrue(craft(h,input(cells)).isEmpty(),"unsafe or incomplete recipe refused "+mode);
            h.assertTrue(ItemStack.matches(bad,snapshot),"input preserved on refusal "+mode);
        }
        h.assertTrue(craft(h,CraftingInput.of(2,2,List.of(bag,new ItemStack(Items.RED_DYE),new ItemStack(Items.BLUE_DYE),ItemStack.EMPTY))).isEmpty(),"multiple dyes refused");
        h.assertTrue(craft(h,CraftingInput.of(2,2,List.of(bag,new ItemStack(Items.RED_DYE),new ItemStack(Items.DIAMOND),ItemStack.EMPTY))).isEmpty(),"extra item refused");h.succeed();
    }
    @GameTest(template="empty") public void craftedUpgradeSurvivesRegistrySaveLoad(GameTestHelper h) {
        var old=filled(BackpackTier.REINFORCED);
        var result=craft(h,input(grid(BackpackTier.EXPEDITION,old,new ItemStack(Items.CYAN_DYE),1)));
        var loaded=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)result.save(h.getLevel().registryAccess()));
        h.assertTrue(ItemStack.matches(result,loaded),"tier color name UUID revision and all items survive disk serialization");
        retained(h,old,loaded);h.succeed();
    }
    @GameTest(template="empty") public void inventoryRecolorConsumesDyeAndSourceOnce(GameTestHelper h) {
        var p=player(h);var old=filled(BackpackTier.BASIC);var before=old.copy();var menu=p.inventoryMenu;p.containerMenu=menu;
        menu.getSlot(1).set(old);menu.getSlot(4).set(new ItemStack(Items.BLUE_DYE,3));
        h.assertTrue(menu.getSlot(0).getItem().is(BackpackItems.BASIC.get()),"inventory 2x2 computes recolor");
        menu.clicked(0,0,ClickType.PICKUP,p);var result=menu.getCarried();retained(h,before,result);
        h.assertValueEqual(result.get(DataComponents.DYED_COLOR).rgb(),DyeColor.BLUE.getTextureDiffuseColor(),"inventory result uses new color");
        h.assertTrue(menu.getSlot(1).getItem().isEmpty(),"source consumed once");
        h.assertValueEqual(menu.getSlot(4).getItem().getCount(),2,"one dye consumed");
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"no duplicate result");h.succeed();
    }
}
