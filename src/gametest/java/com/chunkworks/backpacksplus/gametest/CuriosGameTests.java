/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Real Curios partitions: all tiers; back/chest/other/cosmetic; shown/hidden/inactive;
 * current/replaced/removed source; ordinary storage/mount exchange; immutable snapshots.
 */
@PrefixGameTestTemplate(false)
public final class CuriosGameTests {
    public CuriosGameTests() {}
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void allTiersUseThePlayersBackSlot(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        h.assertTrue(inventory.getStacksHandler("back").isPresent(),"Backpacks+ supplies a back slot without Sophisticated Backpacks");
        for(var item:java.util.List.of(BackpackItems.BASIC.get(),BackpackItems.REINFORCED.get(),BackpackItems.EXPEDITION.get())) {
            var bag=new ItemStack(item); var context=new SlotContext("back",player,0,false,true);
            h.assertTrue(CuriosApi.isStackValid(context,bag),"back tag admits every tier");
            h.assertTrue(CuriosApi.getCurio(bag).orElseThrow().canEquip(context),"item capability permits the back slot");
        }
        h.succeed();
    }
    private static net.minecraft.world.entity.player.Player equipped(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var bag=new ItemStack(BackpackItems.EXPEDITION.get());BagContents.identify(bag);
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("back",0,bag);
        return p;
    }
    /** D-0029 on the back slot, where players wear their bags: with the inventory full a pickup
     * lands in the Curios bag, not the chest bag, on top of its own stack first. */
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void aPickupOverflowsIntoTheCuriosBag(GameTestHelper h) {
        var p=equipped(h);var chest=new ItemStack(BackpackItems.BASIC.get());p.getInventory().setItem(38,chest);
        BagInventory.bind(p,41).setItem(5,new ItemStack(net.minecraft.world.item.Items.COBBLESTONE,60));
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(net.minecraft.world.item.Items.DIRT,64));
        var e=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),new ItemStack(net.minecraft.world.item.Items.COBBLESTONE,10));
        e.setNoPickUpDelay();h.getLevel().addFreshEntity(e);e.playerTouch(p);
        var cells=BagContents.copy(BagLocations.stack(p,41));
        h.assertTrue(e.isRemoved(),"picked up");
        h.assertTrue(cells.get(5).getCount()==64,"the back bag's stack topped up: "+cells.get(5));
        h.assertTrue(cells.get(0).getCount()==6,"the rest in its first empty cell: "+cells.get(0));
        h.assertTrue(BagContents.copy(chest).getFirst().isEmpty(),"the chest bag untouched");
        h.succeed();
    }
    /** D-0032: with a bag on the back, a second bag in the chest slot is still carried: counted
     * after the back bag, taken from once the back bag has none left. */
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void aSecondBagInTheChestSlotIsCarriedBehindTheBackBag(GameTestHelper h) {
        var p=equipped(h);
        var chest=ProviderGameTests.bag(BackpackItems.BASIC.get(),new int[]{0},new ItemStack(net.minecraft.world.item.Items.EMERALD,9));
        p.getInventory().setItem(38,chest);
        BagInventory.bind(p,41).setItem(2,new ItemStack(net.minecraft.world.item.Items.EMERALD,3));
        var order=new java.util.ArrayList<String>();
        com.chunkworks.carried.api.Carried.forEachStored(p,(store,cell,stack)->order.add(store));
        h.assertValueEqual(order,java.util.List.of("backpacksplus:curios/0","backpacksplus:slot/38"),"the back bag, then the chest bag");
        h.assertValueEqual(com.chunkworks.carried.api.Carried.count(p,net.minecraft.world.item.Items.EMERALD),12,"both counted");
        h.assertTrue(com.chunkworks.carried.api.Carried.take(p,s->s.is(net.minecraft.world.item.Items.EMERALD),5,s->{}),"five taken");
        h.assertTrue(BagContents.copy(BagLocations.stack(p,41)).get(2).isEmpty(),"the back bag's three first");
        h.assertValueEqual(BagContents.copy(p.getInventory().getItem(38)).get(0).getCount(),7,"then two from the chest bag");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void curiosWinsWithoutMutatingTheChestBag(GameTestHelper h) {
        var p=equipped(h);var chest=new ItemStack(BackpackItems.BASIC.get());p.getInventory().setItem(38,chest);
        h.assertTrue(BagLocations.worn(p)==41,"Curios has priority");
        var inv=BagInventory.bind(p,41);inv.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
        h.assertTrue(BagContents.copy(chest).getFirst().isEmpty(),"chest bag was not edited");
        h.assertTrue(BagContents.copy(BagLocations.stack(p,41)).getFirst().getCount()==7,"writes belong to Curios bag");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void hiddenIsFunctionalButInactiveIsNot(GameTestHelper h) {
        var p=equipped(h);var handler=CuriosApi.getCuriosInventory(p).orElseThrow();
        var back=handler.getStacksHandler("back").orElseThrow();back.getRenders().set(0,false);
        h.assertTrue(BagLocations.worn(p)==41 && !BagLocations.visible(p,41),"render toggle is cosmetic only");
        var inv=BagInventory.bind(p,41);inv.setItem(38,new ItemStack(net.minecraft.world.item.Items.TORCH));
        back.getRenders().set(0,true);h.assertTrue(BagLocations.visible(p,41),"render on restored");
        handler.setSlotActive("back",0,false);
        h.assertTrue(BagLocations.worn(p)==-1 && !inv.stillValid(p),"inactive slot closes the storage route");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void replacingOrRemovingTheSourceInvalidatesTheMenu(GameTestHelper h) {
        var p=equipped(h);var handler=CuriosApi.getCuriosInventory(p).orElseThrow();
        var inv=BagInventory.bind(p,41);var original=BagLocations.stack(p,41);
        handler.setEquippedCurio("back",0,original.copy());
        h.assertTrue(!inv.stillValid(p),"even a component-identical replacement invalidates the exact source");
        h.assertTrue(inv.removeItem(0,1).isEmpty(),"stale menu cannot extract");
        boolean refused=false;try{inv.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND));}catch(IllegalStateException expected){refused=true;}
        h.assertTrue(refused,"stale menu cannot write");
        var current=BagInventory.bind(p,41);handler.setEquippedCurio("back",0,ItemStack.EMPTY);
        h.assertTrue(!current.stillValid(p),"removed source invalidates menu");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void curiosMountExchangeAndOpenLockUseTheActualBag(GameTestHelper h) {
        var p=equipped(h);var bag=BagLocations.stack(p,41);var inv=BagInventory.bind(p,41);
        inv.setItem(36,new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        h.assertTrue(MountExchange.swap(p,41,bag.get(BackpackItems.ID),BagContents.revision(bag),0,0),"Curios mount draws");
        h.assertTrue(p.getMainHandItem().is(net.minecraft.world.item.Items.DIAMOND_SWORD),"actual held sword");
        inv=BagInventory.bind(p,41);p.containerMenu=new BackpackMenu(1,p.getInventory(),inv,BagContents.tier(bag),41);
        var curio=CuriosApi.getCurio(bag).orElseThrow();var context=new SlotContext("back",p,0,false,true);
        h.assertTrue(!curio.canUnequip(context),"open bag cannot be unequipped normally");
        h.assertTrue(!curio.canEquip(new SlotContext("belt",p,0,false,true)),"other equipment slots rejected");
        h.assertTrue(!curio.canEquip(new SlotContext("back",p,0,true,true)),"cosmetic slots cannot expose storage");
        h.assertTrue(!curio.canEquipFromUse(context),"held right-click remains open, not equip");
        p.containerMenu=p.inventoryMenu;h.assertTrue(curio.canUnequip(context),"closed bag can be removed");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="backpacksplus")
    public void explicitDepositsUseCuriosBagAndPreserveChestBag(GameTestHelper h) {
        var p=equipped(h);var bag=BagLocations.stack(p,41);var inv=BagInventory.bind(p,41);
        var chest=new ItemStack(BackpackItems.BASIC.get());BagContents.identify(chest);p.getInventory().setItem(38,chest);
        ItemStack chestBefore=chest.copy();
        inv.setItem(36,new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        p.getInventory().setItem(0,new ItemStack(net.minecraft.world.item.Items.APPLE,16));
        h.assertTrue(MountExchange.stow(p,41,bag.get(BackpackItems.ID),BagContents.revision(bag),-1,0)==MountExchange.StowResult.STORED,"held stack enters Curios bag");
        p.getInventory().setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND_AXE));
        h.assertTrue(MountExchange.stow(p,41,bag.get(BackpackItems.ID),BagContents.revision(bag),0,0)==MountExchange.StowResult.STORED,"mounted stack enters Curios storage");
        var cells=BagContents.copy(bag);
        h.assertTrue(cells.get(0).getCount()==16 && cells.get(1).is(net.minecraft.world.item.Items.DIAMOND_SWORD) && cells.get(36).isEmpty(),"both sources stored exactly once");
        h.assertTrue(p.getMainHandItem().is(net.minecraft.world.item.Items.DIAMOND_AXE),"direct mount action preserves hand");
        h.assertTrue(ItemStack.matches(chestBefore,chest),"chest bag contents and identity retained");h.succeed();
    }

}
