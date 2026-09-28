/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.carried.api.Carried;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Backpacks+ as the Carried provider (D-0032), on a real server.
 * <p>Partitions: a worn bag's storage cells and its mounts; bags worn, in the hotbar, the main
 * slots and the offhand, and their order; a take, a give and a count; a bag open on screen or not;
 * a refusal (portable storage); counting's cost with a real bag. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class ProviderGameTests {
    public ProviderGameTests() {}
    private static final int CHEST = 38;

    static ItemStack bag(Item tier, int[] cells, ItemStack... stacks) {
        var bag = new ItemStack(tier);
        BagContents.identify(bag);
        var all = BagContents.copy(bag);
        for (int i = 0; i < cells.length; i++) all.set(cells[i], stacks[i]);
        BagContents.store(bag, all);
        return bag;
    }
    private static Player player(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    @GameTest(template = "empty") public void aWornBagsStorageCountsAndItsMountsNever(GameTestHelper h) {
        var p = player(h);
        var tier = BackpackTier.EXPEDITION;
        int smallMount = -1;
        for (int m = 0; m < tier.mounts().size(); m++) if (tier.mounts().get(m) == BackpackTier.Mount.SMALL) { smallMount = tier.mountSlot(m); break; }
        var worn = bag(BackpackItems.EXPEDITION.get(), new int[] {2, smallMount}, new ItemStack(Items.ARROW, 5), new ItemStack(Items.ARROW, 7));
        h.assertTrue(BagContents.copy(worn).get(smallMount).getCount() == 7, "the arrangement holds arrows in a small mount");
        p.getInventory().setItem(CHEST, worn);
        h.assertValueEqual(Carried.count(p, Items.ARROW), 5, "the storage cell's five; the mount's seven are gear");
        h.assertTrue(Carried.take(p, s -> s.is(Items.ARROW), 5, s -> {}), "the five can be taken");
        h.assertTrue(!Carried.take(p, s -> s.is(Items.ARROW), 1, s -> {}), "the mount's arrows cannot");
        h.assertValueEqual(BagContents.copy(p.getInventory().getItem(CHEST)).get(smallMount).getCount(), 7, "the mount untouched");
        h.succeed();
    }

    @GameTest(template = "empty") public void everyCarriedBagCountsWornFirst(GameTestHelper h) {
        var p = player(h);
        p.getInventory().setItem(CHEST, bag(BackpackItems.BASIC.get(), new int[] {0}, new ItemStack(Items.EMERALD, 1)));
        p.getInventory().setItem(20, bag(BackpackItems.BASIC.get(), new int[] {4}, new ItemStack(Items.EMERALD, 10)));
        p.getInventory().setItem(3, bag(BackpackItems.REINFORCED.get(), new int[] {17}, new ItemStack(Items.EMERALD, 64)));
        p.getInventory().setItem(Carried.OFFHAND, bag(BackpackItems.BASIC.get(), new int[] {8}, new ItemStack(Items.EMERALD, 2)));
        var order = new ArrayList<String>();
        Carried.forEachStored(p, (store, cell, stack) -> order.add(store + "#" + cell));
        h.assertValueEqual(order, List.of("backpacksplus:slot/38#0", "backpacksplus:slot/3#17", "backpacksplus:slot/20#4", "backpacksplus:slot/40#8"),
                "worn, then the inventory's bags in its order, the offhand last");
        h.assertValueEqual(Carried.count(p, Items.EMERALD), 1 + 64 + 10 + 2, "all four bags");
        h.succeed();
    }

    @GameTest(template = "empty") public void aTakeAndAGiveGoThroughTheBagsRules(GameTestHelper h) {
        var p = player(h);
        p.getInventory().setItem(CHEST, bag(BackpackItems.BASIC.get(), new int[] {0, 1}, new ItemStack(Items.EMERALD, 30), new ItemStack(Items.STONE, 5)));
        long revision = BagContents.revision(p.getInventory().getItem(CHEST));
        h.assertTrue(Carried.take(p, s -> s.is(Items.EMERALD), 12, s -> {}), "twelve of thirty");
        h.assertValueEqual(BagContents.copy(p.getInventory().getItem(CHEST)).get(0).getCount(), 18, "the bag's cell paid");
        h.assertValueEqual(BagContents.revision(p.getInventory().getItem(CHEST)), revision + 1, "one change, one revision");
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        var box = new ItemStack(Items.SHULKER_BOX);
        Carried.give(p, box);
        h.assertTrue(box.getCount() == 1, "the bag refuses portable storage (D-0003)");
        var more = new ItemStack(Items.EMERALD, 50);
        Carried.give(p, more);
        var cells = BagContents.copy(p.getInventory().getItem(CHEST));
        h.assertTrue(more.isEmpty() && cells.get(0).getCount() == 64 && cells.get(2).getCount() == 4, "onto the bag's stack, then an empty cell: " + cells);
        h.succeed();
    }

    @GameTest(template = "empty") public void aTakeFromABagOpenOnScreenKeepsItOpen(GameTestHelper h) {
        var p = player(h);
        p.getInventory().setItem(0, bag(BackpackItems.BASIC.get(), new int[] {3}, new ItemStack(Items.APPLE, 9)));
        var menu = new BackpackMenu(1, p.getInventory(), BagInventory.bind(p, 0), BackpackTier.BASIC, 0);
        p.containerMenu = menu;
        h.assertTrue(Carried.take(p, s -> s.is(Items.APPLE), 4, s -> {}), "four apples out of the open bag");
        h.assertTrue(menu.stillValid(p), "the open bag screen is still valid");
        h.assertValueEqual(menu.getSlot(3).getItem().getCount(), 5, "and shows five");
        h.assertValueEqual(BagContents.copy(p.getInventory().getItem(0)).get(3).getCount(), 5, "the bag itself holds five");
        h.succeed();
    }

    /** Counting with a real worn bag allocates nothing once its snapshot is taken: the snapshot
     * is kept until the bag's contents component changes. */
    @GameTest(template = "empty", timeoutTicks = 400) public void countingARealBagAllocatesNothing(GameTestHelper h) {
        var p = player(h);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        int[] cells = new int[36];
        var stacks = new ItemStack[36];
        for (int i = 0; i < 36; i++) { cells[i] = i; stacks[i] = new ItemStack(i % 2 == 0 ? Items.ARROW : Items.STONE, 32); }
        p.getInventory().setItem(CHEST, bag(BackpackItems.EXPEDITION.get(), cells, stacks));
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long sink = 0;
        for (int i = 0; i < 200_000; i++) sink += Carried.count(p, Items.ARROW);
        int calls = 100_000;
        long before = threads.getCurrentThreadAllocatedBytes(), t0 = System.nanoTime();
        for (int i = 0; i < calls; i++) sink += Carried.count(p, Items.ARROW);
        long elapsed = System.nanoTime() - t0, bytes = threads.getCurrentThreadAllocatedBytes() - before;
        h.assertValueEqual(Carried.count(p, Items.ARROW), 18 * 32, "the count itself");
        com.mojang.logging.LogUtils.getLogger().info("backpacksplus: Carried count, full inventory and a worn Expedition bag: {} ns/call, {} bytes over {} calls (sink {})", elapsed / calls, bytes, calls, sink);
        h.assertTrue(bytes < calls, "no allocation per call: " + bytes + " bytes over " + calls + " calls");
        h.succeed();
    }
}
