/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real-server partitions for D-0026: a bow finds arrows in the worn bag when the inventory has
 * none, the shot debits the bag's cell by one and the arrow flies unmarked; arrows in the
 * inventory win over the bag; an Infinity bow takes nothing from the bag; a crossbow loads
 * from a bag carried in the inventory and the charged arrow is unmarked; a bag with nothing
 * the weapon accepts lends nothing; the marker on a copy never reaches the bag's own cells. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class AmmoGameTests {
    public AmmoGameTests() {}
    private static Player archer(GameTestHelper h, ItemStack weapon) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().setItem(0, weapon);
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        return p;
    }
    private static ItemStack bagWith(ItemStack... stacks) {
        var bag = new ItemStack(BackpackItems.BASIC.get());
        var cells = BagContents.copy(bag);
        for (int i = 0; i < stacks.length; i++) cells.set(i, stacks[i]);
        BagContents.store(bag, cells);
        return bag;
    }
    private static int arrowsIn(ItemStack bag, int cell) { return BagContents.copy(bag).get(cell).getCount(); }

    @GameTest(template = "empty", timeoutTicks = 100) public void theBowFindsArrowsInTheWornBagAndTheShotDebitsIt(GameTestHelper h) {
        var bow = new ItemStack(Items.BOW);
        var p = archer(h, bow);
        p.getInventory().setItem(BagLocations.CHEST, bagWith(new ItemStack(Items.ARROW, 16)));
        var lent = p.getProjectile(bow);
        h.assertTrue(lent.is(Items.ARROW) && lent.getCount() == 16 && BagAmmo.lent(lent), "the bow sees the bag's sixteen arrows, marked: " + lent);
        h.assertTrue(!BagAmmo.lent(BagContents.copy(p.getInventory().getItem(BagLocations.CHEST)).get(0)), "the bag's own cell is unmarked");
        bow.getItem().releaseUsing(bow, h.getLevel(), p, 72000 - 20);
        h.runAfterDelay(2, () -> {
            var bag = p.getInventory().getItem(BagLocations.CHEST);
            h.assertTrue(arrowsIn(bag, 0) == 15, "one arrow left the bag: " + arrowsIn(bag, 0));
            var arrows = h.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(h.absoluteVec(new net.minecraft.world.phys.Vec3(-16, -16, -16)), h.absoluteVec(new net.minecraft.world.phys.Vec3(32, 48, 32))));
            h.assertTrue(arrows.size() == 1, "one arrow flew: " + arrows.size());
            h.assertTrue(!BagAmmo.lent(arrows.get(0).getPickupItemStackOrigin()) && !arrows.get(0).getPickupItemStackOrigin().has(DataComponents.CUSTOM_DATA), "the arrow carries no marker: " + arrows.get(0).getPickupItemStackOrigin());
            for (var s : p.getInventory().items) h.assertTrue(!s.is(Items.ARROW), "no arrow leaked into the inventory");
            arrows.get(0).discard();
            h.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100) public void inventoryArrowsWinAndInfinityTakesNothing(GameTestHelper h) {
        var bow = new ItemStack(Items.BOW);
        var p = archer(h, bow);
        p.getInventory().setItem(BagLocations.CHEST, bagWith(new ItemStack(Items.ARROW, 8)));
        p.getInventory().setItem(5, new ItemStack(Items.SPECTRAL_ARROW, 3));
        var found = p.getProjectile(bow);
        h.assertTrue(found.is(Items.SPECTRAL_ARROW) && !BagAmmo.lent(found), "arrows in the inventory come first, unmarked: " + found);
        p.getInventory().setItem(5, ItemStack.EMPTY);
        var infinity = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.INFINITY);
        bow.enchant(infinity, 1);
        h.assertTrue(p.getProjectile(bow).is(Items.ARROW), "with Infinity the bag's arrow is still what makes the bow work");
        bow.getItem().releaseUsing(bow, h.getLevel(), p, 72000 - 20);
        h.runAfterDelay(2, () -> {
            h.assertTrue(arrowsIn(p.getInventory().getItem(BagLocations.CHEST), 0) == 8, "Infinity took nothing from the bag: " + arrowsIn(p.getInventory().getItem(BagLocations.CHEST), 0));
            for (var a : h.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(h.absoluteVec(new net.minecraft.world.phys.Vec3(-16, -16, -16)), h.absoluteVec(new net.minecraft.world.phys.Vec3(32, 48, 32))))) a.discard();
            h.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100) public void theCrossbowLoadsFromABagInTheInventory(GameTestHelper h) {
        var crossbow = new ItemStack(Items.CROSSBOW);
        var p = archer(h, crossbow);
        p.getInventory().setItem(7, bagWith(new ItemStack(Items.STONE, 4), new ItemStack(Items.ARROW, 5)));
        crossbow.getItem().releaseUsing(crossbow, h.getLevel(), p, 0);
        var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        h.assertTrue(charged != null && !charged.isEmpty() && charged.getItems().get(0).is(Items.ARROW), "the crossbow loaded an arrow from the bag in slot 7: " + charged);
        h.assertTrue(!BagAmmo.lent(charged.getItems().get(0)) && !charged.getItems().get(0).has(DataComponents.CUSTOM_DATA), "the loaded arrow is unmarked");
        h.assertTrue(arrowsIn(p.getInventory().getItem(7), 1) == 4, "the bag's second cell lost one arrow: " + arrowsIn(p.getInventory().getItem(7), 1));
        h.assertTrue(arrowsIn(p.getInventory().getItem(7), 0) == 4, "the stone beside it is untouched");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100) public void aBagWithoutAmmoLendsNothing(GameTestHelper h) {
        var bow = new ItemStack(Items.BOW);
        var p = archer(h, bow);
        p.getInventory().setItem(BagLocations.CHEST, bagWith(new ItemStack(Items.STONE, 4), new ItemStack(Items.BREAD, 2)));
        h.assertTrue(p.getProjectile(bow).isEmpty(), "nothing the bow accepts, nothing lent");
        p.getInventory().setItem(BagLocations.CHEST, ItemStack.EMPTY);
        h.assertTrue(p.getProjectile(bow).isEmpty(), "no bag, nothing lent");
        h.succeed();
    }
}
