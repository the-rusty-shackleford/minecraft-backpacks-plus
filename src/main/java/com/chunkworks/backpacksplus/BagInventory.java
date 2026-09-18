/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * AF: a vanilla menu's working inventory for exactly one server-owned backpack.
 * RI: source index and stack identity stay fixed; cells have tier size and belong only
 * to this adapter. Edits are committed to immutable item components, never copied to another bag.
 */
public final class BagInventory implements Container {
    private final Player owner;
    private final int source;
    private final ItemStack bag;
    private final UUID identity;
    private final BackpackTier tier;
    private final NonNullList<ItemStack> cells;
    private long revision;
    private BagInventory(Player owner, int source) {
        if (owner.level().isClientSide) throw new IllegalStateException("Server inventory only");
        this.owner = owner; this.source = source; this.bag = BagLocations.stack(owner,source);
        this.tier = BagContents.tier(bag); this.identity = BagContents.identify(bag); this.cells = BagContents.copy(bag);
        this.revision = BagContents.revision(bag);
    }
    /** effects: binds a player's current inventory/equipment cell; throws: IllegalArgumentException for invalid indices or bag. */
    public static BagInventory bind(Player owner, int source) {
        if (!BagLocations.isBag(BagLocations.stack(owner,source))) throw new IllegalArgumentException("Invalid source");
        return new BagInventory(owner, source);
    }
    @Override public int getContainerSize() { return cells.size(); }
    @Override public boolean isEmpty() { return cells.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int cell) { return cells.get(cell); }
    @Override public ItemStack removeItem(int cell, int count) {
        if (!stillValid(owner)) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.removeItem(cells, cell, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int cell) {
        if (!stillValid(owner)) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.takeItem(cells, cell); setChanged(); return result;
    }
    @Override public void setItem(int cell, ItemStack item) {
        if (!stillValid(owner)) throw new IllegalStateException("Backpack moved");
        ItemStack old = cells.get(cell);
        boolean withdrawing = ItemStack.isSameItemSameComponents(old, item) && item.getCount() <= old.getCount();
        if (!BagContents.validCount(item) || !(withdrawing || canPlaceItem(cell, item))) throw new IllegalArgumentException("Cell rejects item");
        cells.set(cell, item.copy()); setChanged();
    }
    @Override public boolean canPlaceItem(int cell, ItemStack item) { return BagContents.admits(tier, cell, item); }
    @Override public void setChanged() {
        if (!stillValid(owner)) throw new IllegalStateException("Backpack moved");
        if (BagContents.store(bag, cells)) {
            revision = BagContents.revision(bag);
            BagLocations.changed(owner,source);
        }
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && BagLocations.stack(owner,source) == bag && bag.getCount() == 1
                && identity.equals(bag.get(BackpackItems.ID)) && revision == BagContents.revision(bag);
    }
    @Override public void clearContent() {
        if (!stillValid(owner)) throw new IllegalStateException("Backpack moved");
        cells.replaceAll(item -> ItemStack.EMPTY); setChanged();
    }
}
