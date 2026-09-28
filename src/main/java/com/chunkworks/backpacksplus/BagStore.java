/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.carried.api.CarriedStore;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A carried bag's storage cells as a Carried store (D-0032). Its mounts are gear the player
 * holstered, never stock, and are not part of it.
 *
 * <p>AF: the storage cells of the bag at {@code source} as of the last {@link #refresh}: cell i
 * holds {@code snapshot.get(i)}; no cells when no bag is there.<br>
 * RI: {@code snapshot} is {@code BagContents.copy} of {@code snapshotBag} taken while that bag
 * held {@code snapshotContents}; {@code tier} is its tier, null exactly when no bag was there.
 * The snapshot is a copy, so nobody reading a cell can change the bag, and it is taken again only
 * when the bag or its contents component changed: the component is immutable, so a new one is the
 * only way the contents change, and comparing references is exact.
 */
final class BagStore implements CarriedStore {
    private final Player player;
    private final int source;
    private final String address;
    private ItemStack snapshotBag = ItemStack.EMPTY;
    private ItemContainerContents snapshotContents;
    private NonNullList<ItemStack> snapshot = NonNullList.withSize(0, ItemStack.EMPTY);
    private BackpackTier tier;

    BagStore(Player player, int source) {
        this.player = player;
        this.source = source;
        this.address = source >= BagLocations.CURIOS_BACK ? "backpacksplus:curios/" + (source - BagLocations.CURIOS_BACK) : "backpacksplus:slot/" + source;
    }

    /** requires: {@code bag} is the stack at the source now, as the provider just read it.
     * effects: takes a new snapshot when the bag or its contents changed. */
    void refresh(ItemStack bag) {
        if (!BagLocations.isBag(bag)) {
            snapshotBag = ItemStack.EMPTY; snapshotContents = null; tier = null;
            if (!snapshot.isEmpty()) snapshot = NonNullList.withSize(0, ItemStack.EMPTY);
            return;
        }
        var contents = bag.get(DataComponents.CONTAINER);
        if (bag != snapshotBag || contents != snapshotContents || tier == null) {
            snapshot = BagContents.copy(bag);
            tier = BagContents.tier(bag);
            snapshotBag = bag;
            snapshotContents = contents;
        }
    }

    @Override public String address() { return address; }
    @Override public int size() { return tier == null ? 0 : tier.storageSlots(); }
    @Override public ItemStack peek(int cell) { return snapshot.get(cell); }
    @Override public boolean admits(int cell, ItemStack stack) { return tier != null && cell < tier.storageSlots() && BagContents.admits(tier, cell, stack); }

    /** effects: stores the storage cells, the mounts as they are. A bag open on screen is written
     * through that screen's own container (a write around it would change the revision it checks
     * and close it under the player); otherwise the bag's component is replaced once.
     * throws: IllegalStateException when the bag is no longer where it was listed or this is a
     * client; IllegalArgumentException when a cell refuses its stack (nothing changes then). */
    @Override public void write(List<ItemStack> cells) {
        if (player.level().isClientSide()) throw new IllegalStateException("server only");
        var bag = BagLocations.stack(player, source);
        if (bag != snapshotBag || tier == null) throw new IllegalStateException("the bag moved since it was listed");
        int storage = tier.storageSlots();
        if (cells.size() != storage) throw new IllegalArgumentException("wrong size");
        for (int c = 0; c < storage; c++) {
            var before = snapshot.get(c);
            var after = cells.get(c);
            boolean withdrawing = after.isEmpty() || ItemStack.isSameItemSameComponents(before, after) && after.getCount() <= before.getCount();
            if (!BagContents.validCount(after) || !(withdrawing || BagContents.admits(tier, c, after))) throw new IllegalArgumentException("cell " + c + " refuses " + after);
        }
        if (player.containerMenu instanceof BackpackMenu menu && menu.source() == source && menu.stillValid(player)) {
            var open = menu.contents();
            for (int c = 0; c < storage; c++) if (!ItemStack.matches(open.getItem(c), cells.get(c))) open.setItem(c, cells.get(c).copy());
            return;
        }
        var all = BagContents.copy(bag);
        for (int c = 0; c < storage; c++) all.set(c, cells.get(c).copy());
        if (BagContents.store(bag, all)) BagLocations.changed(player, source);
    }
}
