/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.quickslot.Slot;
import com.chunkworks.quickslot.SlotData;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Partitions: held/mounted; every tier and both game modes; empty/partial/full storage;
 * matching/different components; stale UUID/revision/selection; invalid/empty source;
 * open menu, forbidden portable storage. Actual server inventories, no storage mocks. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class StowGameTests {
    public StowGameTests() {}
    private static Player player(GameTestHelper h, GameType mode, BackpackTier tier) {
        Player p=h.makeMockPlayer(mode);
        p.getInventory().setItem(38,new ItemStack(switch(tier) {
            case BASIC -> BackpackItems.BASIC.get(); case REINFORCED -> BackpackItems.REINFORCED.get();
            case EXPEDITION -> BackpackItems.EXPEDITION.get();
        }));
        BagContents.identify(bag(p)); return p;
    }
    private static ItemStack bag(Player p) { return p.getInventory().getItem(38); }
    private static MountExchange.StowResult stow(Player p,int mount) {
        return MountExchange.stow(p,38,bag(p).get(BackpackItems.ID),BagContents.revision(bag(p)),mount,p.getInventory().selected);
    }
    private static void unchanged(GameTestHelper h,Player p,ItemStack bag,ItemStack hand) {
        h.assertTrue(ItemStack.matches(bag,bag(p)),"all bag cells, identity and revision retained");
        h.assertTrue(ItemStack.matches(hand,p.getMainHandItem()),"held stack retained");
    }
    @GameTest(template="empty") public void heldDepositMergesAndSplitsWholeStack(GameTestHelper h) {
        for (GameType mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}) {
            Player p=player(h,mode,BackpackTier.BASIC);var inv=BagInventory.bind(p,38);
            inv.setItem(0,new ItemStack(Items.APPLE,60));inv.setItem(9,new ItemStack(Items.DIAMOND_SWORD));
            p.getInventory().setItem(0,new ItemStack(Items.APPLE,16));Slot.replace(p,new ItemStack(Items.SHEARS));
            long revision=BagContents.revision(bag(p));
            h.assertTrue(stow(p,-1)==MountExchange.StowResult.STORED,"whole held stack stowed");
            var cells=BagContents.copy(bag(p));
            h.assertValueEqual(cells.get(0).getCount(),64,"merge fills first stack");
            h.assertValueEqual(cells.get(1).getCount(),12,"remainder stored once");
            h.assertTrue(p.getMainHandItem().isEmpty() && cells.get(9).is(Items.DIAMOND_SWORD),"hand emptied and mount preserved");
            h.assertTrue(p.getData(SlotData.STACK).is(Items.SHEARS),"original Quick Slot preserved");
            h.assertTrue(BagContents.revision(bag(p))==revision+1,"one atomic revision");
        }
        h.succeed();
    }
    @GameTest(template="empty") public void directMountDepositLeavesHandAndOtherMountsAlone(GameTestHelper h) {
        for (BackpackTier tier:BackpackTier.values()) for (GameType mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}) {
            Player p=player(h,mode,tier);var inv=BagInventory.bind(p,38);
            for (int m=0;m<tier.mounts().size();m++) {
                ItemStack item=new ItemStack(tier.mounts().get(m)==BackpackTier.Mount.LONG ? Items.DIAMOND_SWORD : Items.APPLE);
                item.set(DataComponents.CUSTOM_NAME,Component.literal("Mounted "+m));inv.setItem(tier.mountSlot(m),item);
            }
            p.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,47));
            for (int m=0;m<tier.mounts().size();m++) {
                ItemStack item=BagContents.copy(bag(p)).get(tier.mountSlot(m)).copy();
                h.assertTrue(stow(p,m)==MountExchange.StowResult.STORED,"mounted item stowed for each tier");
                var cells=BagContents.copy(bag(p));
                h.assertTrue(cells.get(tier.mountSlot(m)).isEmpty() && ItemStack.matches(item,cells.get(m)),"components preserved and mount emptied");
                h.assertTrue(p.getMainHandItem().is(Items.COBBLESTONE) && p.getMainHandItem().getCount()==47,"held item untouched");
                for (int other=m+1;other<tier.mounts().size();other++) h.assertTrue(!cells.get(tier.mountSlot(other)).isEmpty(),"unselected mounts retained");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public void partialAndFullStorageRefuseWithoutMutation(GameTestHelper h) {
        for (int source:new int[]{-1,1}) for (int room:new int[]{0,4}) {
            Player p=player(h,GameType.SURVIVAL,BackpackTier.BASIC);var inv=BagInventory.bind(p,38);
            for (int i=0;i<9;i++) inv.setItem(i,new ItemStack(Items.APPLE,i==0 ? 64-room : 64));
            if (source==-1) p.getInventory().setItem(0,new ItemStack(Items.APPLE,16));
            else {inv.setItem(10,new ItemStack(Items.APPLE,16));p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_AXE));}
            ItemStack before=bag(p).copy(),hand=p.getMainHandItem().copy();
            h.assertTrue(stow(p,source)==MountExchange.StowResult.FULL,"whole stack must fit");
            unchanged(h,p,before,hand);
        }
        h.succeed();
    }
    @GameTest(template="empty") public void componentMismatchesCannotUseAnotherStacksRoom(GameTestHelper h) {
        Player p=player(h,GameType.CREATIVE,BackpackTier.BASIC);var inv=BagInventory.bind(p,38);
        for (int i=0;i<9;i++)inv.setItem(i,new ItemStack(Items.APPLE,63));
        ItemStack named=new ItemStack(Items.APPLE);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep this name"));
        p.getInventory().setItem(0,named);ItemStack before=bag(p).copy();
        h.assertTrue(!MountExchange.storageFits(bag(p),named),"preview honors stack components");
        h.assertTrue(stow(p,-1)==MountExchange.StowResult.FULL,"different components cannot merge");
        unchanged(h,p,before,named);h.succeed();
    }
    @GameTest(template="empty") public void staleAndMalformedDepositsCannotMoveAnything(GameTestHelper h) {
        Player p=player(h,GameType.SURVIVAL,BackpackTier.BASIC);p.getInventory().setItem(0,new ItemStack(Items.APPLE,16));
        ItemStack before=bag(p).copy(),hand=p.getMainHandItem().copy();UUID id=bag(p).get(BackpackItems.ID);long revision=BagContents.revision(bag(p));
        h.assertTrue(MountExchange.stow(p,38,UUID.randomUUID(),revision,-1,0)==MountExchange.StowResult.REFUSED,"wrong bag");
        h.assertTrue(MountExchange.stow(p,38,id,revision+1,-1,0)==MountExchange.StowResult.REFUSED,"wrong revision");
        h.assertTrue(MountExchange.stow(p,38,id,revision,-1,8)==MountExchange.StowResult.REFUSED,"stale hotbar");
        h.assertTrue(stow(p,-2)==MountExchange.StowResult.REFUSED && stow(p,2)==MountExchange.StowResult.REFUSED,"invalid mount indices");
        h.assertTrue(stow(p,0)==MountExchange.StowResult.REFUSED,"empty mount");unchanged(h,p,before,hand);
        h.assertTrue(stow(p,-1)==MountExchange.StowResult.STORED,"current request succeeds");
        ItemStack after=bag(p).copy();p.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(MountExchange.stow(p,38,id,revision,-1,0)==MountExchange.StowResult.REFUSED,"replayed intent refused");
        unchanged(h,p,after,new ItemStack(Items.DIAMOND));h.succeed();
    }
    @GameTest(template="empty") public void prohibitedAndBusyDepositsRemainSafe(GameTestHelper h) {
        Player p=player(h,GameType.SURVIVAL,BackpackTier.BASIC);
        for (ItemStack item:new ItemStack[]{new ItemStack(Items.SHULKER_BOX),new ItemStack(BackpackItems.BASIC.get())}) {
            p.getInventory().setItem(0,item);ItemStack before=bag(p).copy();
            h.assertTrue(stow(p,-1)==MountExchange.StowResult.FORBIDDEN,"portable storage forbidden");unchanged(h,p,before,item);
        }
        p.getInventory().setItem(0,new ItemStack(Items.APPLE));ItemStack before=bag(p).copy(),hand=p.getMainHandItem().copy();
        p.containerMenu=new BackpackMenu(1,p.getInventory(),BagInventory.bind(p,38),BackpackTier.BASIC,38);
        h.assertTrue(stow(p,-1)==MountExchange.StowResult.REFUSED,"menu owns bag");unchanged(h,p,before,hand);h.succeed();
    }
}
