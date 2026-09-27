/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.BackpackItems;
import com.chunkworks.backpacksplus.BagContents;
import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagMenu;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The worn bag's cells on a chest's menu (D-0030), through the real ChestMenu and its clicks.
 * <p>Partitions: bag worn / none; the menu vanilla's / another mod's subclass; shift-click from the
 * chest (inventory with room / full), from the inventory, from a bag cell; a direct click into a
 * cell; a stack a mount would never take. Chest slots 0–26, inventory 27–62, bag cells from 63. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class ChestGameTests {
    public ChestGameTests() {}
    private static final int CHEST = 27, INVENTORY_END = 63;

    private static Player wearing(GameTestHelper h, Item bag) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        if (bag != null) p.getInventory().setItem(38, new ItemStack(bag));
        return p;
    }
    private static ChestMenu chest(Player p, SimpleContainer box) {
        var menu = ChestMenu.threeRows(1, p.getInventory(), box);
        p.containerMenu = menu;
        return menu;
    }
    private static void fillMain(Player p) {
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).isEmpty()) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
    }
    private static java.util.List<ItemStack> worn(Player p) { return BagContents.copy(p.getInventory().getItem(38)); }

    @GameTest(template = "empty") public void aChestsMenuCarriesTheBagsFortyCellsAfterTheInventory(GameTestHelper h) {
        Player p = wearing(h, BackpackItems.BASIC.get());
        var menu = chest(p, new SimpleContainer(27));
        h.assertValueEqual(((WornBagMenu) menu).backpacksplus$first(), INVENTORY_END, "the cells begin after chest and inventory");
        h.assertValueEqual(menu.slots.size(), INVENTORY_END + WornBag.SIZE, "forty of them");
        int active = 0;
        for (int k = 0; k < WornBag.SIZE; k++) if (menu.slots.get(INVENTORY_END + k).isActive()) active++;
        h.assertValueEqual(active, BackpackTier.BASIC.totalSlots(), "active as far as the worn tier goes");
        Player bare = wearing(h, null);
        var none = chest(bare, new SimpleContainer(27));
        for (int k = 0; k < WornBag.SIZE; k++) h.assertTrue(!none.slots.get(INVENTORY_END + k).isActive(), "no bag: no cell active");
        var modded = new ChestMenu(MenuType.GENERIC_9x3, 2, p.getInventory(), new SimpleContainer(27), 3) {};
        h.assertValueEqual(modded.slots.size(), INVENTORY_END, "another mod's chest menu is left as it is");
        h.assertValueEqual(((WornBagMenu) modded).backpacksplus$first(), -1, "and says it carries none");
        h.succeed();
    }
    @GameTest(template = "empty") public void shiftClickFromTheChestFillsTheInventoryFirst(GameTestHelper h) {
        Player p = wearing(h, BackpackItems.BASIC.get());
        var box = new SimpleContainer(27);
        box.setItem(0, new ItemStack(Items.COBBLESTONE, 40));
        var menu = chest(p, box);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, p);
        int inInventory = 0;
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).is(Items.COBBLESTONE)) inInventory += p.getInventory().getItem(i).getCount();
        h.assertValueEqual(inInventory, 40, "all into the inventory");
        h.assertTrue(worn(p).stream().allMatch(ItemStack::isEmpty), "none into the bag while the inventory has room");
        h.assertTrue(box.getItem(0).isEmpty(), "the chest slot emptied");
        h.succeed();
    }
    @GameTest(template = "empty") public void withTheInventoryFullAChestStackGoesIntoTheBagsStorageNeverAMount(GameTestHelper h) {
        Player p = wearing(h, BackpackItems.BASIC.get());
        fillMain(p);
        var box = new SimpleContainer(27);
        box.setItem(4, new ItemStack(Items.COBBLESTONE, 64));
        box.setItem(5, new ItemStack(Items.IRON_PICKAXE));
        var menu = chest(p, box);
        menu.clicked(4, 0, ClickType.QUICK_MOVE, p);
        var cells = worn(p);
        h.assertTrue(cells.get(0).is(Items.COBBLESTONE) && cells.get(0).getCount() == 64, "into the bag's first storage cell: " + cells.get(0));
        h.assertTrue(box.getItem(4).isEmpty(), "out of the chest");
        var tier = BackpackTier.BASIC;
        for (int m = tier.storageSlots(); m < tier.totalSlots(); m++) h.assertTrue(!cells.get(m).is(Items.COBBLESTONE), "no cobblestone in mount " + m);
        menu.clicked(5, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(worn(p).get(1).is(Items.IRON_PICKAXE), "a tool too goes to storage before a mount: " + worn(p).get(1));
        h.succeed();
    }
    @GameTest(template = "empty") public void shiftClickFromTheInventoryStillGoesIntoTheChest(GameTestHelper h) {
        Player p = wearing(h, BackpackItems.BASIC.get());
        p.getInventory().setItem(9, new ItemStack(Items.APPLE, 12));
        var box = new SimpleContainer(27);
        var menu = chest(p, box);
        menu.clicked(CHEST, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(box.getItem(0).is(Items.APPLE) && box.getItem(0).getCount() == 12, "into the chest, as vanilla: " + box.getItem(0));
        h.assertTrue(worn(p).stream().allMatch(ItemStack::isEmpty), "not into the bag");
        h.succeed();
    }
    @GameTest(template = "empty") public void shiftClickFromABagCellGoesIntoTheChest(GameTestHelper h) {
        Player p = wearing(h, BackpackItems.BASIC.get());
        var box = new SimpleContainer(27);
        var menu = chest(p, box);
        menu.slots.get(INVENTORY_END + 2).set(new ItemStack(Items.TORCH, 30));
        h.assertValueEqual(worn(p).get(2).getCount(), 30, "the cell writes the worn bag");
        menu.clicked(INVENTORY_END + 2, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(box.getItem(0).is(Items.TORCH) && box.getItem(0).getCount() == 30, "into the chest: " + box.getItem(0));
        h.assertTrue(worn(p).get(2).isEmpty(), "out of the bag");
        h.succeed();
    }
    @GameTest(template = "empty") public void aClickCarriesAStackFromTheChestIntoABagCell(GameTestHelper h) {
        Player p = wearing(h, BackpackItems.EXPEDITION.get());
        var box = new SimpleContainer(27);
        box.setItem(8, new ItemStack(Items.IRON_INGOT, 9));
        var menu = chest(p, box);
        menu.clicked(8, 0, ClickType.PICKUP, p);
        menu.clicked(INVENTORY_END + 20, 0, ClickType.PICKUP, p);
        h.assertTrue(worn(p).get(20).is(Items.IRON_INGOT) && worn(p).get(20).getCount() == 9, "placed in the bag's cell 20: " + worn(p).get(20));
        h.assertTrue(menu.getCarried().isEmpty() && box.getItem(8).isEmpty(), "nothing on the cursor, nothing left in the chest");
        h.succeed();
    }
    @GameTest(template = "empty") public void withNoBagAFullInventoryLeavesTheChestAsVanilla(GameTestHelper h) {
        Player p = wearing(h, null);
        fillMain(p);
        var box = new SimpleContainer(27);
        box.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        var menu = chest(p, box);
        var moved = menu.quickMoveStack(p, 0);
        h.assertTrue(moved.isEmpty(), "nothing moved");
        h.assertValueEqual(box.getItem(0).getCount(), 10, "the stack stays in the chest");
        h.succeed();
    }
}
