/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.PickupPlan;
import com.chunkworks.backpacksplus.domain.PickupPlan.Held;
import com.chunkworks.backpacksplus.domain.PickupPlan.Pass;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Items picked up off the ground, and arrows out of it, go into the backpacks as well as the
 * inventory, placed the way the inventory places them (D-0029): first on top of stacks of the
 * same thing, the inventory's then the bags', then into the inventory's empty slots, then into
 * the bags' empty storage cells. The bags are the worn one and then any carried, the order ammo
 * is found in (D-0026); mount cells are never filled; what a bag cannot hold stays on the ground.
 * A bag open on screen is written through that screen's own container, so the screen stays. */
public final class BagPickup {
    private BagPickup() {}

    /**
     * requires: {@code vanilla} is the inventory's own add for this stack (possibly wrapped by
     * other mods). effects: moves as much of {@code stack} as the inventory and the bags take,
     * leaving the rest in it; returns whether any was taken. On a client, or with no bag, exactly
     * {@code vanilla}.
     */
    public static boolean add(Inventory inventory, ItemStack stack, Predicate<ItemStack> vanilla) {
        Player player = inventory.player;
        if (player.level().isClientSide() || stack.isEmpty() || BagAmmo.sources(player).isEmpty()) return vanilla.test(stack);
        int before = stack.getCount();
        topUp(inventory, stack);
        if (!stack.isEmpty()) intoBags(player, stack, Pass.TOP_UP);
        boolean took = !stack.isEmpty() && vanilla.test(stack);
        if (!stack.isEmpty()) intoBags(player, stack, Pass.FILL_EMPTY);
        return took || stack.getCount() < before;
    }

    /** effects: tops up the inventory's stacks of the same thing, in the inventory's own order
     * (selected slot, offhand, then the rest), as its add would before touching an empty slot. */
    static void topUp(Inventory inventory, ItemStack stack) {
        int slot;
        while (!stack.isEmpty() && (slot = inventory.getSlotWithRemainingSpace(stack)) != -1) {
            ItemStack held = inventory.getItem(slot);
            int n = Math.min(stack.getCount(), inventory.getMaxStackSize(held) - held.getCount());
            if (n <= 0) return;
            held.grow(n);
            held.setPopTime(5);
            stack.shrink(n);
        }
    }

    /** effects: one pass of the placement over every bag's storage cells, bag by bag. */
    static void intoBags(Player player, ItemStack stack, Pass pass) {
        if (!BagContents.storable(stack)) return;
        for (int source : BagAmmo.sources(player)) {
            if (stack.isEmpty()) return;
            ItemStack bag = BagLocations.stack(player, source);
            if (!BagLocations.isBag(bag)) continue;
            Container cells = open(player, source);
            int storage = BagContents.tier(bag).storageSlots();
            var held = new ArrayList<Held>(storage);
            for (int cell = 0; cell < storage; cell++) {
                ItemStack in = cells.getItem(cell);
                held.add(in.isEmpty() ? Held.EMPTY : ItemStack.isSameItemSameComponents(in, stack) ? Held.same(in.getCount()) : Held.other(in.getCount()));
            }
            int[] into = PickupPlan.place(held, stack.getCount(), stack.getMaxStackSize(), pass);
            for (int cell = 0; cell < storage; cell++) {
                if (into[cell] == 0) continue;
                ItemStack in = cells.getItem(cell);
                cells.setItem(cell, stack.copyWithCount(in.getCount() + into[cell]));
                stack.shrink(into[cell]);
            }
        }
    }

    /** effects: the container to write the bag through: the open bag screen's when the player has
     * this bag open (a write around it would change the revision it checks and close it), else a
     * fresh binding. */
    static Container open(Player player, int source) {
        if (player.containerMenu instanceof BackpackMenu menu && menu.source() == source && menu.stillValid(player)) return menu.contents();
        return BagInventory.bind(player, source);
    }
}
