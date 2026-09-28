/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.carried.api.Carried;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** D-0034, with the real Vanilla Wheels jar loaded (run with -PtestMountMods=<dir holding it>):
 * a packed vehicle carries its chest in Vanilla Wheels' own cargo component, which the nesting
 * check (D-0003) did not know, so a truck with a full chest could go into a backpack. Partitions:
 * a packed vehicle with cargo and without; by the check, by a pickup into the bag. */
@PrefixGameTestTemplate(false)
public final class VehicleGameTests {
    public VehicleGameTests() {}

    @SuppressWarnings("unchecked")
    private static ItemStack packedTruck(boolean withCargo) {
        var truck = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vanillawheels:vehicle")));
        if (withCargo) {
            var cargo = (DataComponentType<List<ItemContainerContents>>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("vanillawheels:cargo"));
            truck.set(cargo, List.of(ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.GOLD_BLOCK, 64)))));
        }
        return truck;
    }

    @GameTest(template = "empty", templateNamespace = "backpacksplus")
    public void aPackedVehicleNeverGoesIntoABag(GameTestHelper h) {
        h.assertTrue(!BagContents.storable(packedTruck(true)), "a truck with a full chest is portable storage");
        h.assertTrue(!BagContents.storable(packedTruck(false)), "an empty one is the same item, refused as a kind");
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().clearContent();
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        p.getInventory().setItem(38, new ItemStack(BackpackItems.EXPEDITION.get()));
        var truck = packedTruck(true);
        Carried.give(p, truck);
        h.assertTrue(truck.getCount() == 1, "a give leaves it out of the bag");
        h.assertTrue(BagContents.copy(p.getInventory().getItem(38)).stream().allMatch(ItemStack::isEmpty), "the bag holds nothing");
        h.succeed();
    }
}
