/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real-server partitions for D-0027, the worn bag's cells on the player's own inventory menu:
 * a Basic bag makes nine storage cells and two mounts active and the other twenty-nine
 * inactive; a stack set in a cell lands in the bag's component and comes back out; shift-click
 * from the inventory goes into the bag, from the bag back to the inventory, a helmet with the
 * head slot empty still equips, a mount refuses a block and takes a pickaxe; without a bag every
 * cell is inactive and shift-click is somebody else's (vanilla's, or Quick Slot's when it is
 * loaded); a bag put on after the menu exists activates
 * the cells, and taking it off deactivates them. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class InventoryGameTests {
    public InventoryGameTests() {}
    private static Player player(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }
    private static ItemStack bag(ItemStack... stacks) {
        var bag = new ItemStack(BackpackItems.BASIC.get());
        var cells = BagContents.copy(bag);
        for (int i = 0; i < stacks.length; i++) cells.set(i, stacks[i]);
        BagContents.store(bag, cells);
        return bag;
    }
    private static int first(Player p) { return ((WornBagMenu) p.inventoryMenu).backpacksplus$first(); }
    private static int active(Player p) {
        int n = 0;
        for (int k = 0; k < WornBag.SIZE; k++) if (p.inventoryMenu.slots.get(first(p) + k).isActive()) n++;
        return n;
    }
    private static ItemStack cell(Player p, int cell) { return BagContents.copy(p.getInventory().getItem(BagLocations.CHEST)).get(cell); }

    @GameTest(template = "empty") public void aWornBagsCellsAreLiveOnTheInventoryMenu(GameTestHelper h) {
        var p = player(h);
        var menu = p.inventoryMenu;
        h.assertTrue(first(p) >= 46 && menu.slots.size() >= first(p) + WornBag.SIZE, "forty bag slots after the menu's own: first " + first(p) + " of " + menu.slots.size() + " (slot 46: " + menu.slots.get(46).getClass().getName() + ", last: " + menu.slots.get(menu.slots.size() - 1).getClass().getName() + ")");
        h.assertValueEqual(active(p), 0, "no bag, no active cell");
        p.getInventory().setItem(BagLocations.CHEST, bag(new ItemStack(Items.COBBLESTONE, 12)));
        h.assertValueEqual(active(p), 11, "a Basic bag: nine storage cells and two mounts");
        h.assertTrue(menu.slots.get(first(p)).getItem().is(Items.COBBLESTONE), "cell 0 shows the bag's cobblestone");
        menu.slots.get(first(p) + 1).set(new ItemStack(Items.STONE, 5));
        h.assertTrue(cell(p, 1).is(Items.STONE) && cell(p, 1).getCount() == 5, "a stack set in cell 1 is in the bag's component: " + cell(p, 1));
        var taken = menu.slots.get(first(p)).remove(12);
        h.assertTrue(taken.getCount() == 12 && cell(p, 0).isEmpty(), "taking cell 0 empties it in the bag: " + cell(p, 0));
        p.getInventory().setItem(BagLocations.CHEST, ItemStack.EMPTY);
        h.assertValueEqual(active(p), 0, "bag off, cells inactive again");
        h.assertTrue(menu.slots.get(first(p) + 1).getItem().isEmpty(), "and empty");
        h.succeed();
    }
    @GameTest(template = "empty") public void shiftClickGoesIntoTheBagFirstAndBack(GameTestHelper h) {
        var p = player(h);
        var menu = p.inventoryMenu;
        p.getInventory().setItem(BagLocations.CHEST, bag());
        p.getInventory().setItem(0, new ItemStack(Items.DIRT, 10));
        var moved = menu.quickMoveStack(p, 36);
        h.assertTrue(!moved.isEmpty() && p.getInventory().getItem(0).isEmpty() && cell(p, 0).is(Items.DIRT) && cell(p, 0).getCount() == 10, "shift-click from the hotbar puts the dirt in the bag: " + cell(p, 0));
        p.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));
        menu.quickMoveStack(p, 9);
        h.assertTrue(p.getInventory().getArmor(3).is(Items.IRON_HELMET), "a helmet with the head slot empty equips as vanilla does: " + p.getInventory().getArmor(3));
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND_PICKAXE));
        menu.quickMoveStack(p, 9);
        var tier = BagContents.tier(p.getInventory().getItem(BagLocations.CHEST));
        boolean mounted = false;
        for (int i = 0; i < tier.totalSlots() - tier.storageSlots(); i++) if (cell(p, tier.mountSlot(i)).is(Items.DIAMOND_PICKAXE)) mounted = true;
        h.assertTrue(mounted || cell(p, 1).is(Items.DIAMOND_PICKAXE), "the pickaxe went into the bag (a cell or a mount)");
        h.assertTrue(!menu.slots.get(first(p) + WornBag.STORAGE).mayPlace(new ItemStack(Items.STONE)), "a mount refuses a block");
        var back = menu.quickMoveStack(p, first(p));
        int dirt = 0;
        for (var s : p.getInventory().items) if (s.is(Items.DIRT)) dirt += s.getCount();
        h.assertTrue(!back.isEmpty() && dirt == 10 && cell(p, 0).isEmpty(), "shift-click from the bag brings the dirt back: " + dirt);
        p.getInventory().setItem(BagLocations.CHEST, ItemStack.EMPTY);
        p.getInventory().setItem(0, new ItemStack(Items.SAND, 3));
        menu.quickMoveStack(p, 36);
        boolean inBagCells = false;
        for (int k = 0; k < WornBag.SIZE; k++) if (menu.slots.get(first(p) + k).getItem().is(Items.SAND)) inBagCells = true;
        var where = new StringBuilder();
        for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i).getItem().is(Items.SAND)) where.append(i).append(':').append(menu.slots.get(i).getClass().getSimpleName()).append(' ');
        // Without a bag our cells take nothing and the click is somebody else's: vanilla moves the
        // sand to the main inventory, and Quick Slot, when present, into its own slot first.
        h.assertTrue(!inBagCells && !p.getInventory().getItem(0).is(Items.SAND), "without a bag the cells take nothing and the sand left the hotbar: " + where);
        h.succeed();
    }
}
