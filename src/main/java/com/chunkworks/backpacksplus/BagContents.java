/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Immutable item-component snapshots. Mutators replace complete contents, never expose stored mutable stacks. */
public final class BagContents {
    private BagContents() {}
    public static final TagKey<Item> FORBIDDEN = TagKey.create(Registries.ITEM, BackpacksPlus.id("forbidden_containers"));
    public static final TagKey<Item> LONG = TagKey.create(Registries.ITEM, BackpacksPlus.id("mounts/long"));
    public static final TagKey<Item> SMALL = TagKey.create(Registries.ITEM, BackpacksPlus.id("mounts/small"));

    /** effects: returns the tier; throws: IllegalArgumentException unless this is a single backpack. */
    public static BackpackTier tier(ItemStack bag) {
        if (bag.getCount() != 1 || !(bag.getItem() instanceof BackpackItem item)) throw new IllegalArgumentException("Not one backpack");
        return item.tier();
    }
    /** requires: server-owned bag. effects: assigns an identity if absent, retaining all contents. */
    public static UUID identify(ItemStack bag) {
        tier(bag);
        UUID id = bag.get(BackpackItems.ID);
        if (id == null) { id = UUID.randomUUID(); bag.set(BackpackItems.ID, id); }
        return id;
    }
    /** effects: returns the mutation revision, zero for a new bag. */
    public static long revision(ItemStack bag) { return bag.getOrDefault(BackpackItems.REVISION, 0L); }
    /** effects: returns independent copies of all cells; throws: IllegalStateException on unsupported overflow, never truncates. */
    public static NonNullList<ItemStack> copy(ItemStack bag) {
        int size = tier(bag).totalSlots();
        ItemContainerContents contents = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents.getSlots() > size) throw new IllegalStateException("Backpack overflow requires recovery; not truncating");
        NonNullList<ItemStack> result = NonNullList.withSize(size, ItemStack.EMPTY);
        contents.copyInto(result);
        return result;
    }
    /**
     * requires: server-owned bag and a complete working inventory.
     * effects: replaces its immutable contents and advances the revision exactly once if changed.
     * throws: IllegalArgumentException on a wrong size or invalid stack; no partial mutation.
     */
    public static boolean store(ItemStack bag, List<ItemStack> cells) {
        BackpackTier tier = tier(bag);
        if (cells.size() != tier.totalSlots()) throw new IllegalArgumentException("Wrong inventory size");
        NonNullList<ItemStack> old = copy(bag);
        for (int cell = 0; cell < cells.size(); cell++) {
            ItemStack item = cells.get(cell), before = old.get(cell);
            boolean withdrawing = ItemStack.isSameItemSameComponents(before, item) && item.getCount() <= before.getCount();
            if (!validCount(item) || !(withdrawing || admits(tier, cell, item))) throw new IllegalArgumentException("Cell rejects item");
        }
        ItemContainerContents next = ItemContainerContents.fromItems(cells);
        if (bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).equals(next)) return false;
        long revision = Math.incrementExact(revision(bag));
        bag.set(DataComponents.CONTAINER, next);
        bag.set(BackpackItems.REVISION, revision);
        return true;
    }
    /** effects: returns whether a stack obeys its actual item/component stack limit. */
    public static boolean validCount(ItemStack item) { return item.isEmpty() || item.getCount() > 0 && item.getCount() <= item.getMaxStackSize(); }
    /** effects: rejects nested portable inventories even when empty; no item-ID substring guesses. */
    public static boolean storable(ItemStack item) {
        if (item.isEmpty()) return true;
        if (!validCount(item) || item.getItem() instanceof BackpackItem || !item.canFitInsideContainerItems()
                || item.is(FORBIDDEN) || item.has(DataComponents.BUNDLE_CONTENTS)) return false;
        var contents = item.get(DataComponents.CONTAINER);
        if (contents != null && (contents.getSlots() > 0 || !(item.getItem() instanceof BlockItem))) return false;
        if (item.getCapability(Capabilities.ItemHandler.ITEM) != null) return false;
        // Ordinary furnaces, hoppers and other utility blocks are allowed. A creative or
        // modded drop that actually retains an inventory is still a nested container.
        var blockData = item.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockData != null && blockData.contains("Items")) return false;
        return true;
    }
    /** effects: classifies current tagged gear; explicit tags win, with vanilla tool defaults. */
    public static boolean longGear(ItemStack item) {
        if (item.is(SMALL)) return false;
        return item.is(LONG) || item.is(ItemTags.SWORDS) || item.is(ItemTags.AXES) || item.is(ItemTags.PICKAXES)
                || item.is(ItemTags.SHOVELS) || item.is(ItemTags.HOES)
                || item.getItem() instanceof net.minecraft.world.item.SwordItem
                || item.getItem() instanceof net.minecraft.world.item.DiggerItem;
    }
    /** effects: returns whether the cell admits this stack; invalid indices reject rather than wrap. */
    public static boolean admits(BackpackTier tier, int cell, ItemStack item) {
        if (cell < 0 || cell >= tier.totalSlots()) return false;
        if (item.isEmpty()) return true;
        if (!storable(item)) return false;
        if (cell < tier.storageSlots()) return true;
        boolean large = longGear(item);
        return tier.mounts().get(cell - tier.storageSlots()) == BackpackTier.Mount.LONG ? large : !large;
    }
}
