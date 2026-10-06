/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The worn bag as forty fixed cells under the player's own inventory menu (D-0027): cells 0–35
 * are the bag's storage cells, 36–39 its mounts, whichever bag is worn at the moment of each
 * call, and none of them while no bag is worn. The menu's slots are made once when the player
 * joins; this resolves the bag live, so putting a bag on or taking it off during play shows or
 * hides its cells without a new menu.
 * <p>AF: {@code index(k)} maps a menu cell to the worn bag's real cell, or −1 when the worn bag
 * has no such cell. On the server, edits go through a {@link BagInventory} bound to the worn
 * bag, rebound whenever the bag, its identity or its revision changed under us; on the client
 * the cells are the menu's own, like every other slot of it: seeded from the client's copy of a
 * bag when that bag is first worn, then kept by the menu's slot sync, a client-side write being
 * a prediction that sync overwrites. A sync of the bag itself never replaces them: it can be
 * older than the player's edits still in flight, and the creative screen sends the server every
 * cell it sees change, so a replaced view undid those edits on the server (D-0036).
 * <p>RI: bound is null, or bound.stillValid(owner) held at the last call; snapshot is sized to
 * the snapshot tier's total slots, empty exactly when snapshotTier is null. */
public final class WornBag implements Container {
    public static final int STORAGE = 36, MOUNTS = 4, SIZE = STORAGE + MOUNTS;
    private final Player owner;
    private BagInventory bound;
    private int boundSource = BagLocations.NONE;
    private NonNullList<ItemStack> snapshot = NonNullList.withSize(0, ItemStack.EMPTY);
    private BackpackTier snapshotTier;
    private java.util.UUID snapshotId;

    public WornBag(Player owner) { this.owner = owner; }

    /** effects: the worn bag's address now, or NONE. */
    public int source() { return BagLocations.worn(owner); }
    /** effects: the worn bag's tier now, or null. */
    public BackpackTier tier() {
        var bag = BagLocations.stack(owner, source());
        return BagLocations.isBag(bag) ? BagContents.tier(bag) : null;
    }
    /** effects: the worn bag's real cell for the menu cell, or −1 when the bag has none. */
    public int index(int menuCell) {
        var tier = tier();
        if (tier == null) return -1;
        if (menuCell < STORAGE) return menuCell < tier.storageSlots() ? menuCell : -1;
        int mount = menuCell - STORAGE;
        return mount < tier.totalSlots() - tier.storageSlots() ? tier.mountSlot(mount) : -1;
    }
    /** effects: whether the menu cell exists on the worn bag. */
    public boolean active(int menuCell) { return index(menuCell) >= 0; }

    /** effects: the live server container for the worn bag, rebinding when the bag changed under
     * us; null when no bag is worn. */
    private BagInventory server() {
        int source = source();
        if (source < 0) { bound = null; boundSource = BagLocations.NONE; return null; }
        if (bound == null || boundSource != source || !bound.stillValid(owner)) {
            if (!BagLocations.isBag(BagLocations.stack(owner, source))) { bound = null; return null; }
            bound = BagInventory.bind(owner, source);
            boundSource = source;
        }
        return bound;
    }
    /** effects: the client's view of the worn bag's cells, seeded from the client's copy of the bag
     * when a different bag (identity or tier) is worn, otherwise as the menu's sync left it. */
    private NonNullList<ItemStack> client() {
        var bag = BagLocations.stack(owner, source());
        if (!BagLocations.isBag(bag)) { snapshot = NonNullList.withSize(0, ItemStack.EMPTY); snapshotTier = null; snapshotId = null; return snapshot; }
        var tier = BagContents.tier(bag);
        var id = bag.get(BackpackItems.ID);
        if (tier != snapshotTier || !java.util.Objects.equals(id, snapshotId)) {
            snapshot = BagContents.copy(bag);
            snapshotTier = tier;
            snapshotId = id;
        }
        return snapshot;
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() {
        for (int k = 0; k < SIZE; k++) if (!getItem(k).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int menuCell) {
        int cell = index(menuCell);
        if (cell < 0) return ItemStack.EMPTY;
        if (owner.level().isClientSide()) { var cells = client(); return cell < cells.size() ? cells.get(cell) : ItemStack.EMPTY; }
        var inventory = server();
        return inventory == null ? ItemStack.EMPTY : inventory.getItem(cell);
    }
    @Override public ItemStack removeItem(int menuCell, int count) {
        int cell = index(menuCell);
        if (cell < 0) return ItemStack.EMPTY;
        if (owner.level().isClientSide()) return ContainerHelper.removeItem(client(), cell, count);
        var inventory = server();
        return inventory == null ? ItemStack.EMPTY : inventory.removeItem(cell, count);
    }
    @Override public ItemStack removeItemNoUpdate(int menuCell) {
        int cell = index(menuCell);
        if (cell < 0) return ItemStack.EMPTY;
        if (owner.level().isClientSide()) return ContainerHelper.takeItem(client(), cell);
        var inventory = server();
        return inventory == null ? ItemStack.EMPTY : inventory.removeItemNoUpdate(cell);
    }
    /** effects: sets the cell; a stack the worn bag cannot hold (the bag went away between the
     * click and now) goes back to the player's inventory, or drops, rather than vanishing. */
    @Override public void setItem(int menuCell, ItemStack item) {
        int cell = index(menuCell);
        if (owner.level().isClientSide()) { var cells = client(); if (cell >= 0 && cell < cells.size()) cells.set(cell, item.copy()); return; }
        var inventory = cell < 0 ? null : server();
        if (inventory == null) { if (!item.isEmpty()) owner.getInventory().placeItemBackInInventory(item); return; }
        try { inventory.setItem(cell, item); }
        catch (IllegalStateException | IllegalArgumentException refused) { if (!item.isEmpty()) owner.getInventory().placeItemBackInInventory(item); }
    }
    @Override public boolean canPlaceItem(int menuCell, ItemStack item) {
        int cell = index(menuCell);
        if (cell < 0) return false;
        var tier = tier();
        return tier != null && BagContents.admits(tier, cell, item);
    }
    @Override public int getMaxStackSize() { return 64; }
    @Override public void setChanged() { if (!owner.level().isClientSide()) { var inventory = server(); if (inventory != null && inventory.stillValid(owner)) inventory.setChanged(); } }
    @Override public boolean stillValid(Player player) { return player == owner; }
    @Override public void clearContent() { for (int k = 0; k < SIZE; k++) if (active(k)) setItem(k, ItemStack.EMPTY); }
}
