/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Partitions on the real server/menu backend: all tiers, ordinary/portable containers,
 * pickup/split/shift/number-key/quick-craft, full capacity, source relocation, stale adapter,
 * serialization/transfer, overflow and ownership. MockPlayer supplies input only. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class StorageGameTests {
    public StorageGameTests() {}
    private static Player player(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().setItem(0, new ItemStack(BackpackItems.BASIC.get()));
        return p;
    }
    private static BackpackMenu menu(Player p) {
        return new BackpackMenu(1, p.getInventory(), BagInventory.bind(p, 0), BackpackTier.BASIC, 0);
    }
    private static void rejects(GameTestHelper h, Runnable operation, String reason) {
        boolean rejected = false;
        try { operation.run(); } catch (IllegalArgumentException | IllegalStateException expected) { rejected = true; }
        h.assertTrue(rejected, reason);
    }
    @GameTest(template="empty") public void allTiersPreserveStorageAndMounts(GameTestHelper h) {
        for (var item : List.of(BackpackItems.BASIC, BackpackItems.REINFORCED, BackpackItems.EXPEDITION)) {
            ItemStack bag = new ItemStack(item.get());
            var tier = BagContents.tier(bag); var cells = BagContents.copy(bag);
            cells.set(tier.storageSlots()-1, new ItemStack(Items.FURNACE, 64));
            cells.set(tier.mountSlot(0), new ItemStack(Items.DIAMOND_PICKAXE));
            BagContents.store(bag, cells);
            var recovered = BagContents.copy(bag);
            h.assertValueEqual(recovered.size(), tier.totalSlots(), "tier capacity includes independent mounts");
            h.assertValueEqual(recovered.get(tier.storageSlots()-1).getCount(), 64, "last storage slot retained");
            h.assertTrue(recovered.get(tier.mountSlot(0)).is(Items.DIAMOND_PICKAXE), "mount retained");
            cells.get(tier.storageSlots()-1).shrink(1);
            h.assertValueEqual(BagContents.copy(bag).get(tier.storageSlots()-1).getCount(), 64, "snapshot owns defensive copies");
        }
        h.succeed();
    }
    @GameTest(template="empty") public void utilityBlocksAllowedPortableStorageRejected(GameTestHelper h) {
        for (var item : List.of(Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.HOPPER, Items.DISPENSER, Items.DROPPER, Items.CRAFTING_TABLE))
            h.assertTrue(BagContents.storable(new ItemStack(item)), "ordinary utility block admitted: " + item);
        for (var item : List.of(Items.CHEST, Items.TRAPPED_CHEST, Items.ENDER_CHEST, Items.BARREL, Items.SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BUNDLE, BackpackItems.BASIC.get()))
            h.assertTrue(!BagContents.storable(new ItemStack(item)), "empty storage container rejected: " + item);
        ItemStack furnace = new ItemStack(Items.FURNACE);
        furnace.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        h.assertTrue(!BagContents.storable(furnace), "utility block retaining an inventory rejected");
        furnace.remove(DataComponents.CONTAINER);
        CompoundTag legacy = new CompoundTag(); legacy.putString("id", "minecraft:furnace"); legacy.put("Items", new ListTag());
        furnace.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(legacy));
        h.assertTrue(!BagContents.storable(furnace), "retained legacy inventory cannot bypass admission");
        h.succeed();
    }
    @GameTest(template="empty") public void clickSplitAndPlaceConserveContents(GameTestHelper h) {
        Player p = player(h); BackpackMenu menu = menu(p);
        p.getInventory().setItem(9, new ItemStack(Items.APPLE, 17));
        menu.clicked(11, 0, ClickType.PICKUP, p); menu.clicked(0, 0, ClickType.PICKUP, p);
        menu.clicked(0, 1, ClickType.PICKUP, p);
        h.assertValueEqual(BagContents.copy(p.getMainHandItem()).get(0).getCount(), 8, "split leaves smaller half");
        h.assertValueEqual(menu.getCarried().getCount(), 9, "cursor keeps larger half");
        menu.clicked(0, 1, ClickType.PICKUP, p);
        h.assertValueEqual(BagContents.copy(p.getMainHandItem()).get(0).getCount(), 9, "single returned item");
        h.assertValueEqual(menu.getCarried().getCount(), 8, "remaining cursor items");
        h.succeed();
    }
    @GameTest(template="empty") public void shiftTransfersThroughRealMenu(GameTestHelper h) {
        Player p = player(h); BackpackMenu menu = menu(p);
        p.getInventory().setItem(9, new ItemStack(Items.FURNACE, 48));
        menu.clicked(11, 0, ClickType.QUICK_MOVE, p);
        h.assertValueEqual(BagContents.copy(p.getMainHandItem()).get(0).getCount(), 48, "utility stack enters bag");
        h.assertTrue(p.getInventory().getItem(9).isEmpty(), "source emptied once");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(BagContents.copy(p.getMainHandItem()).get(0).isEmpty(), "source bag cell cleared");
        h.assertValueEqual(p.getInventory().countItem(Items.FURNACE), 48, "returned exact count");
        h.succeed();
    }
    @GameTest(template="empty") public void insertionRoutesCannotNestStorage(GameTestHelper h) {
        Player p = player(h); BackpackMenu menu = menu(p);
        p.getInventory().setItem(9, new ItemStack(Items.SHULKER_BOX));
        menu.clicked(11, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(p.getInventory().getItem(9).is(Items.SHULKER_BOX), "shift rejects nested storage");
        menu.clicked(11, 0, ClickType.PICKUP, p); menu.clicked(0, 0, ClickType.PICKUP, p);
        h.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "click rejects nested storage");
        menu.clicked(-999, 0, ClickType.QUICK_CRAFT, p); menu.clicked(1, 1, ClickType.QUICK_CRAFT, p); menu.clicked(-999, 2, ClickType.QUICK_CRAFT, p);
        h.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "drag rejects nested storage");
        menu.setCarried(ItemStack.EMPTY); p.getInventory().setItem(2, new ItemStack(Items.CHEST));
        menu.clicked(0, 2, ClickType.SWAP, p);
        h.assertTrue(p.getInventory().getItem(2).is(Items.CHEST), "number key rejects container");
        h.assertTrue(BagContents.copy(p.getMainHandItem()).stream().allMatch(ItemStack::isEmpty), "no insertion route changed bag");
        h.succeed();
    }
    @GameTest(template="empty") public void sourceBagCannotMoveThroughMenu(GameTestHelper h) {
        Player p = player(h); BackpackMenu menu = menu(p); ItemStack bag = p.getMainHandItem();
        int sourceMenuCell = 11 + 27;
        menu.clicked(sourceMenuCell, 0, ClickType.PICKUP, p);
        menu.clicked(sourceMenuCell, 0, ClickType.QUICK_MOVE, p);
        menu.clicked(sourceMenuCell, 1, ClickType.THROW, p);
        menu.clicked(11, 0, ClickType.SWAP, p);
        h.assertTrue(p.getMainHandItem() == bag && menu.getCarried().isEmpty(), "opened bag remains bound to source");
        h.succeed();
    }
    @GameTest(template="empty") public void staleOrRelocatedBagCannotOverwriteNewerContents(GameTestHelper h) {
        Player p = player(h); BagInventory first = BagInventory.bind(p, 0), stale = BagInventory.bind(p, 0);
        first.setItem(0, new ItemStack(Items.EMERALD, 4));
        h.assertTrue(!stale.stillValid(p), "older view invalidated by revision");
        rejects(h, () -> stale.setItem(0, new ItemStack(Items.DIAMOND, 3)), "stale edit rejected");
        h.assertTrue(BagContents.copy(p.getMainHandItem()).get(0).is(Items.EMERALD), "newer snapshot intact");
        ItemStack original = p.getMainHandItem(); p.getInventory().setItem(0, new ItemStack(BackpackItems.BASIC.get()));
        h.assertTrue(!first.stillValid(p), "replacement stack invalidates view");
        h.assertTrue(first.removeItem(0, 2).isEmpty(), "cannot extract from relocated bag");
        h.assertValueEqual(BagContents.copy(original).get(0).getCount(), 4, "original contents preserved");
        h.succeed();
    }
    @GameTest(template="empty") public void fullBagAndPlayerCannotLoseShiftedItems(GameTestHelper h) {
        Player p = player(h); var inv = BagInventory.bind(p, 0);
        for (int i=0; i<9; i++) inv.setItem(i, new ItemStack(Items.STONE, 64));
        BackpackMenu menu = menu(p); p.getInventory().setItem(9, new ItemStack(Items.APPLE, 32));
        long revision = BagContents.revision(p.getMainHandItem());
        menu.clicked(11, 0, ClickType.QUICK_MOVE, p);
        h.assertValueEqual(p.getInventory().getItem(9).getCount(), 32, "full bag rejects shift atomically");
        for (int i=1; i<36; i++) p.getInventory().setItem(i, new ItemStack(Items.APPLE, 64));
        menu.clicked(0, 0, ClickType.QUICK_MOVE, p);
        h.assertValueEqual(BagContents.copy(p.getMainHandItem()).get(0).getCount(), 64, "full player inventory leaves bag intact");
        h.assertTrue(BagContents.revision(p.getMainHandItem()) == revision, "failed moves do not mutate");
        h.succeed();
    }
    @GameTest(template="empty") public void serializedBagCarriesInventoryToNewOwner(GameTestHelper h) {
        Player p = player(h); BagInventory inv = BagInventory.bind(p, 0);
        inv.setItem(0, new ItemStack(Items.FURNACE, 12)); inv.setItem(9, new ItemStack(Items.TRIDENT));
        ItemStack bag = p.getMainHandItem();
        ItemStack loaded = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)bag.save(h.getLevel().registryAccess()));
        h.assertTrue(ItemStack.matches(loaded, bag), "registry-backed save/load preserves identity, revision and contents");
        Player receiver = h.makeMockPlayer(GameType.SURVIVAL); receiver.getInventory().setItem(0, loaded);
        BagInventory owned = BagInventory.bind(receiver, 0);
        h.assertValueEqual(owned.removeItem(0, 3).getCount(), 3, "new owner can retrieve");
        h.assertValueEqual(BagContents.copy(loaded).get(0).getCount(), 9, "remaining contents persisted");
        h.assertValueEqual(BagContents.copy(bag).get(0).getCount(), 12, "roundtrip has no shared mutable stacks");
        h.succeed();
    }
    @GameTest(template="empty") public void overflowAndInvalidStoreNeverTruncate(GameTestHelper h) {
        ItemStack bag = new ItemStack(BackpackItems.BASIC.get());
        NonNullList<ItemStack> oversized = NonNullList.withSize(12, ItemStack.EMPTY); oversized.set(11, new ItemStack(Items.DIAMOND));
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(oversized));
        rejects(h, () -> BagContents.copy(bag), "overflow requires recovery");
        h.assertTrue(bag.get(DataComponents.CONTAINER).getStackInSlot(11).is(Items.DIAMOND), "overflow stack retained");
        bag.remove(DataComponents.CONTAINER); var cells = BagContents.copy(bag); cells.set(0, new ItemStack(Items.SHULKER_BOX));
        rejects(h, () -> BagContents.store(bag, cells), "raw component adapter cannot insert forbidden storage");
        h.assertTrue(BagContents.copy(bag).get(0).isEmpty(), "rejected store unchanged"); h.succeed();
    }
}
