/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.BackpackItems;
import com.chunkworks.backpacksplus.BackpackMenu;
import com.chunkworks.backpacksplus.BagContents;
import com.chunkworks.backpacksplus.BagInventory;
import com.chunkworks.backpacksplus.WornBagMenu;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Picking up off the ground with backpacks (D-0029), through the real ItemEntity.playerTouch and
 * AbstractArrow.playerTouch a survival player's collision calls.
 * <p>Partitions: main inventory full / with room; the same thing in the inventory, the bag, both,
 * neither; the bag with room / full / absent; worn / carried and open on screen; storable /
 * unstorable / gear only a mount would hold; a whole pickup / a partial one; item / arrow. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class PickupGameTests {
    public PickupGameTests() {}
    private static final int CHEST = 38;

    /** A survival player; with {@code bag}, that bag worn on the chest holding {@code cells}. */
    private static Player player(GameTestHelper h, Item bag, List<ItemStack> cells) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        if (bag != null) {
            ItemStack worn = new ItemStack(bag);
            var all = BagContents.copy(worn);
            for (int i = 0; i < cells.size(); i++) all.set(i, cells.get(i));
            BagContents.store(worn, all);
            p.getInventory().setItem(CHEST, worn);
        }
        return p;
    }
    /** effects: fills every main slot (0–35) that is empty with 64 dirt. */
    private static void fillMain(Player p) {
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).isEmpty()) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
    }
    private static ItemEntity drop(GameTestHelper h, Player p, ItemStack stack) {
        var e = new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), stack);
        e.setNoPickUpDelay();
        h.getLevel().addFreshEntity(e);
        e.playerTouch(p);
        return e;
    }
    private static List<ItemStack> worn(Player p) { return BagContents.copy(p.getInventory().getItem(CHEST)); }
    private static int count(Player p, Item item) {
        int n = 0;
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
        return n;
    }
    private static ItemStack cobble(int n) { return new ItemStack(Items.COBBLESTONE, n); }

    @GameTest(template = "empty") public void aFullInventoryOverflowsIntoTheWornBagOnTopOfItsStackFirst(GameTestHelper h) {
        var named = cobble(5); named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep"));
        Player p = player(h, BackpackItems.EXPEDITION.get(), List.of(ItemStack.EMPTY, named, ItemStack.EMPTY, cobble(50)));
        fillMain(p);
        long revision = BagContents.revision(p.getInventory().getItem(CHEST));
        var e = drop(h, p, cobble(30));
        var cells = worn(p);
        h.assertValueEqual(cells.get(3).getCount(), 64, "the bag's own cobblestone topped up first");
        h.assertTrue(cells.get(0).is(Items.COBBLESTONE) && cells.get(0).getCount() == 16, "the rest in the first empty cell: " + cells.get(0));
        h.assertValueEqual(cells.get(1).getCount(), 5, "a named stack is another kind: not merged into");
        h.assertTrue(cells.get(2).isEmpty(), "one empty cell used, not two");
        h.assertTrue(e.isRemoved(), "the whole pickup taken off the ground");
        h.assertTrue(BagContents.revision(p.getInventory().getItem(CHEST)) > revision, "the bag's revision advanced: clients resync");
        h.assertValueEqual(count(p, Items.DIRT), 36 * 64, "the full inventory untouched");
        h.succeed();
    }
    @GameTest(template = "empty") public void withRoomInTheInventoryAStackInTheBagIsToppedUpBeforeAnEmptySlot(GameTestHelper h) {
        Player p = player(h, BackpackItems.BASIC.get(), List.of(ItemStack.EMPTY, ItemStack.EMPTY, cobble(10)));
        drop(h, p, cobble(20));
        h.assertValueEqual(worn(p).get(2).getCount(), 30, "stacked onto the bag's cobblestone");
        h.assertValueEqual(count(p, Items.COBBLESTONE), 0, "no new inventory stack started");
        h.succeed();
    }
    @GameTest(template = "empty") public void theInventorysOwnStackIsToppedUpBeforeTheBags(GameTestHelper h) {
        Player p = player(h, BackpackItems.BASIC.get(), List.of(ItemStack.EMPTY, ItemStack.EMPTY, cobble(10)));
        p.getInventory().setItem(7, cobble(60));
        drop(h, p, cobble(20));
        h.assertValueEqual(p.getInventory().getItem(7).getCount(), 64, "the inventory's stack first");
        h.assertValueEqual(worn(p).get(2).getCount(), 26, "then the bag's");
        h.assertValueEqual(count(p, Items.COBBLESTONE), 64, "and no new inventory stack");
        h.succeed();
    }
    @GameTest(template = "empty") public void somethingTheBagDoesNotHoldGoesToTheInventoryAsBefore(GameTestHelper h) {
        Player p = player(h, BackpackItems.BASIC.get(), List.of(cobble(10)));
        drop(h, p, new ItemStack(Items.APPLE, 7));
        h.assertValueEqual(count(p, Items.APPLE), 7, "into an empty inventory slot, as vanilla");
        h.assertTrue(worn(p).stream().noneMatch(s -> s.is(Items.APPLE)), "the bag's empty cells are for when the inventory is full");
        h.succeed();
    }
    @GameTest(template = "empty") public void aPartialPickupLeavesTheRestOnTheGround(GameTestHelper h) {
        var full = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) full.add(new ItemStack(Items.STONE, 64));
        full.set(4, cobble(60));
        Player p = player(h, BackpackItems.BASIC.get(), full);
        fillMain(p);
        var e = drop(h, p, cobble(10));
        h.assertValueEqual(worn(p).get(4).getCount(), 64, "topped up to the stack's limit");
        h.assertTrue(!e.isRemoved() && e.getItem().getCount() == 6, "the rest stays on the ground: " + e.getItem());
        h.succeed();
    }
    @GameTest(template = "empty") public void aFullBagAndAFullInventoryLeaveItOnTheGroundUntouched(GameTestHelper h) {
        var full = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) full.add(new ItemStack(Items.STONE, 64));
        Player p = player(h, BackpackItems.BASIC.get(), full);
        fillMain(p);
        long revision = BagContents.revision(p.getInventory().getItem(CHEST));
        var e = drop(h, p, cobble(10));
        h.assertTrue(!e.isRemoved() && e.getItem().getCount() == 10, "nothing taken");
        h.assertValueEqual(BagContents.revision(p.getInventory().getItem(CHEST)), revision, "the bag not rewritten");
        h.succeed();
    }
    @GameTest(template = "empty") public void withNoBagAFullInventoryIsVanilla(GameTestHelper h) {
        Player p = player(h, null, List.of());
        fillMain(p);
        var e = drop(h, p, cobble(10));
        h.assertTrue(!e.isRemoved() && e.getItem().getCount() == 10, "stays on the ground");
        Player q = player(h, null, List.of());
        drop(h, q, cobble(10));
        h.assertValueEqual(count(q, Items.COBBLESTONE), 10, "with room, picked up as always");
        h.succeed();
    }
    @GameTest(template = "empty") public void aContainerOrToolOnlyAMountWouldHoldStaysOnTheGround(GameTestHelper h) {
        var full = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) full.add(new ItemStack(Items.STONE, 64));
        Player p = player(h, BackpackItems.BASIC.get(), full);
        fillMain(p);
        var shulker = drop(h, p, new ItemStack(Items.SHULKER_BOX));
        h.assertTrue(!shulker.isRemoved(), "portable storage never goes into a bag");
        var pick = drop(h, p, new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(!pick.isRemoved(), "storage full: the pickaxe is not put in a mount");
        var tier = BackpackTier.BASIC;
        for (int m = tier.storageSlots(); m < tier.totalSlots(); m++) h.assertTrue(worn(p).get(m).isEmpty(), "mount " + m + " still empty");
        h.succeed();
    }
    @GameTest(template = "empty") public void aCarriedBagOpenOnScreenTakesItAndStaysOpen(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().setItem(0, new ItemStack(BackpackItems.BASIC.get()));
        fillMain(p);
        var menu = new BackpackMenu(1, p.getInventory(), BagInventory.bind(p, 0), BackpackTier.BASIC, 0);
        p.containerMenu = menu;
        var e = drop(h, p, new ItemStack(Items.APPLE, 5));
        h.assertTrue(e.isRemoved(), "picked up into the carried bag");
        h.assertValueEqual(BagContents.copy(p.getInventory().getItem(0)).get(0).getCount(), 5, "in its first cell");
        h.assertTrue(menu.stillValid(p), "the open bag screen is still valid");
        h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 5, "and shows the apples");
        h.succeed();
    }
    @GameTest(template = "empty") public void theInventoryScreensBagPanelShowsThePickup(GameTestHelper h) {
        Player p = player(h, BackpackItems.BASIC.get(), List.of(cobble(10)));
        fillMain(p);
        var panel = ((WornBagMenu) p.inventoryMenu).backpacksplus$bag();
        h.assertValueEqual(panel.getItem(0).getCount(), 10, "the panel reads the bag");
        drop(h, p, cobble(4));
        h.assertValueEqual(panel.getItem(0).getCount(), 14, "and the pickup, at once");
        h.succeed();
    }
    @GameTest(template = "empty") public void anArrowInTheGroundGoesIntoTheBagWhenTheInventoryIsFull(GameTestHelper h) {
        Player p = player(h, BackpackItems.BASIC.get(), List.of(new ItemStack(Items.ARROW, 20)));
        fillMain(p);
        var arrow = new Arrow(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        arrow.setNoPhysics(true);
        h.getLevel().addFreshEntity(arrow);
        arrow.playerTouch(p);
        h.assertTrue(arrow.isRemoved(), "pulled out of the ground");
        h.assertValueEqual(worn(p).get(0).getCount(), 21, "onto the bag's arrows");
        var stuck = new Arrow(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.ARROW), null);
        stuck.pickup = AbstractArrow.Pickup.DISALLOWED;
        stuck.setNoPhysics(true);
        h.getLevel().addFreshEntity(stuck);
        stuck.playerTouch(p);
        h.assertTrue(!stuck.isRemoved(), "a mob's or an infinity arrow is still not pickable");
        h.succeed();
    }
}
