/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The worn bag at the crafting table (D-0033), on a real server with a real table.
 * <p>Partitions: the table's menu with a bag worn and without; shift-click from the result with
 * the inventory full and with room, from the grid, from a bag cell; the recipe book's fill with
 * the ingredients only in the bag. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class CraftingTableGameTests {
    public CraftingTableGameTests() {}
    private static final int CHEST = 38;

    /** A fake server player: not in the level's player list, so not tracked by the other tests'
     * players, whose mock connections never negotiated this mod's payloads (the gear sync would
     * send them the worn bag's state and fail the tick). */
    private static ServerPlayer crafter(GameTestHelper h, ItemStack worn) {
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "table-check"));
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        p.getInventory().clearContent();
        p.getInventory().setItem(CHEST, worn);
        return p;
    }
    private static CraftingMenu table(GameTestHelper h, ServerPlayer p) {
        var pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, Blocks.CRAFTING_TABLE);
        var menu = new CraftingMenu(7, p.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(pos)));
        p.containerMenu = menu;
        return menu;
    }
    private static void fillMain(ServerPlayer p) { for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64)); }

    @GameTest(template = "empty") public void theTablesMenuCarriesTheBagsCellsAfterItsOwn(GameTestHelper h) {
        var p = crafter(h, ProviderGameTests.bag(BackpackItems.BASIC.get(), new int[] {4}, new ItemStack(Items.APPLE, 6)));
        var menu = table(h, p);
        h.assertValueEqual(((WornBagMenu) menu).backpacksplus$first(), 46, "after the result, the grid and the inventory");
        h.assertValueEqual(menu.slots.size(), 46 + WornBag.SIZE, "forty cells");
        h.assertValueEqual(menu.getSlot(46 + 4).getItem().getCount(), 6, "the cells read the worn bag");
        h.assertTrue(!menu.getSlot(46 + 9).isActive(), "a Basic bag's tenth cell is not there");
        h.succeed();
    }

    @GameTest(template = "empty") public void aShiftClickedResultGoesIntoTheBagWhenTheInventoryIsFull(GameTestHelper h) {
        var p = crafter(h, new ItemStack(BackpackItems.BASIC.get()));
        fillMain(p);
        var menu = table(h, p);
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 2));
        h.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_PLANKS), "the table shows planks");
        menu.quickMoveStack(p, 0);
        var cells = BagContents.copy(p.getInventory().getItem(CHEST));
        h.assertTrue(cells.get(0).is(Items.OAK_PLANKS) && cells.get(0).getCount() == 4, "the planks went into the bag: " + cells.get(0));
        h.assertValueEqual(menu.getSlot(1).getItem().getCount(), 1, "one log crafted");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(4)).isEmpty(), "nothing dropped at the player's feet");
        h.succeed();
    }

    @GameTest(template = "empty") public void withRoomTheResultGoesToTheInventoryAndABagCellGoesThereToo(GameTestHelper h) {
        var p = crafter(h, ProviderGameTests.bag(BackpackItems.BASIC.get(), new int[] {2}, new ItemStack(Items.STICK, 5)));
        var menu = table(h, p);
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 1));
        menu.quickMoveStack(p, 0);
        int planks = 0;
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).is(Items.OAK_PLANKS)) planks += p.getInventory().getItem(i).getCount();
        h.assertValueEqual(planks, 4, "with room, the inventory as vanilla");
        h.assertTrue(BagContents.copy(p.getInventory().getItem(CHEST)).get(0).isEmpty(), "the bag untouched");
        menu.quickMoveStack(p, 46 + 2);
        int sticks = 0;
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).is(Items.STICK)) sticks += p.getInventory().getItem(i).getCount();
        h.assertValueEqual(sticks, 5, "a bag cell's stack shift-clicks into the inventory");
        h.assertTrue(BagContents.copy(p.getInventory().getItem(CHEST)).get(2).isEmpty(), "out of the bag");
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = "empty") public void theRecipeBookFillsTheTableFromTheBag(GameTestHelper h) {
        var p = crafter(h, ProviderGameTests.bag(BackpackItems.BASIC.get(), new int[] {0, 1}, new ItemStack(Items.STICK, 4), new ItemStack(Items.COBBLESTONE, 10)));
        var menu = table(h, p);
        var recipe = (RecipeHolder<CraftingRecipe>) h.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("stone_pickaxe")).orElseThrow();
        p.awardRecipes(List.of(recipe));
        menu.handlePlacement(false, recipe, p);
        h.assertTrue(menu.getSlot(0).getItem().is(Items.STONE_PICKAXE), "the pickaxe is ready: " + menu.getSlot(0).getItem());
        var cells = BagContents.copy(p.getInventory().getItem(CHEST));
        h.assertValueEqual(cells.get(0).getCount(), 2, "two sticks from the bag");
        h.assertValueEqual(cells.get(1).getCount(), 7, "three cobblestone from the bag");
        h.succeed();
    }
}
