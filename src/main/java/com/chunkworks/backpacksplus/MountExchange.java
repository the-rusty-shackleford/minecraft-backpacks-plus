/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.SwapRules;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-only, atomic exchanges and explicit deposits into ordinary backpack storage.
 * AF: success moves server-owned stacks exactly once; refusal changes no item or revision.
 * RI: Quick Slot is never read or modified; all capacity calculations use private copies.
 */
public final class MountExchange {
    private MountExchange() {}

    /** Outcomes distinguish a full bag from prohibited contents without exposing item authority. */
    public enum StowResult { STORED, FULL, FORBIDDEN, REFUSED }

    private static ItemStack current(Player player, int source, UUID id, long revision, int selected) {
        if (player.level().isClientSide || source < 0 || source == selected) return ItemStack.EMPTY;
        ItemStack bag = BagLocations.stack(player, source);
        if (!(bag.getItem() instanceof BackpackItem) || bag.getCount() != 1) return ItemStack.EMPTY;
        boolean busy = player.isUsingItem() || player.containerMenu != player.inventoryMenu;
        return SwapRules.current(player.isAlive(), player.isSpectator(), busy,
                id != null && id.equals(bag.get(BackpackItems.ID)), selected,
                player.getInventory().selected, revision, BagContents.revision(bag)) ? bag : ItemStack.EMPTY;
    }

    /**
     * requires: server thread and a server-chosen inventory/equipment source.
     * effects: exchanges the selected hotbar stack with a compatible mount; incompatible,
     * stale or impossible requests leave both stacks and ordinary storage unchanged.
     */
    public static boolean swap(Player player, int source, UUID bagId, long revision, int mount, int selected) {
        ItemStack bag = current(player, source, bagId, revision, selected);
        if (bag.isEmpty()) return false;
        var tier = BagContents.tier(bag);
        if (mount < 0 || mount >= tier.mounts().size()) return false;
        int cell = tier.mountSlot(mount);
        ItemStack held = player.getMainHandItem();
        if (!BagContents.admits(tier, cell, held)) return false;
        NonNullList<ItemStack> working = BagContents.copy(bag);
        ItemStack drawn = working.get(cell);
        if (ItemStack.matches(drawn, held)) return false;
        working.set(cell, held.copy());
        if (!BagContents.store(bag, working)) return false;
        player.getInventory().setItem(selected, drawn);
        changed(player, source);
        return true;
    }

    /**
     * requires: server thread and a server-chosen inventory/equipment source.
     * effects: deposits the entire held stack (mount=-1) or the indicated mount stack
     * into ordinary storage. Refusal preserves all stacks and the bag revision. A mount
     * deposit never changes the selected hotbar item. No other negative mount is valid.
     */
    public static StowResult stow(Player player, int source, UUID bagId, long revision, int mount, int selected) {
        ItemStack bag = current(player, source, bagId, revision, selected);
        if (bag.isEmpty()) return StowResult.REFUSED;
        var tier = BagContents.tier(bag);
        if (mount < -1 || mount >= tier.mounts().size()) return StowResult.REFUSED;
        NonNullList<ItemStack> working = BagContents.copy(bag);
        ItemStack item = mount == -1 ? player.getMainHandItem() : working.get(tier.mountSlot(mount));
        if (item.isEmpty()) return StowResult.REFUSED;
        if (!BagContents.storable(item)) return StowResult.FORBIDDEN;
        if (!insert(working, tier.storageSlots(), item)) return StowResult.FULL;
        if (mount >= 0) working.set(tier.mountSlot(mount), ItemStack.EMPTY);
        if (!BagContents.store(bag, working)) return StowResult.REFUSED;
        if (mount == -1) player.getInventory().setItem(selected, ItemStack.EMPTY);
        changed(player, source);
        return StowResult.STORED;
    }

    /**
     * requires: bag is a backpack and neither argument is mutated concurrently.
     * effects: reports whether the entire nonempty stack fits ordinary storage without
     * changing either argument. Intended for input-time previews, never every render tick.
     */
    public static boolean storageFits(ItemStack bag, ItemStack item) {
        return !item.isEmpty() && BagContents.storable(item)
                && insert(BagContents.copy(bag), BagContents.tier(bag).storageSlots(), item);
    }

    private static void changed(Player player, int source) {
        player.getInventory().setChanged();
        BagLocations.changed(player, source);
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean insert(NonNullList<ItemStack> cells, int storage, ItemStack item) {
        ItemStack remaining = item.copy();
        for (int pass=0; pass<2 && !remaining.isEmpty(); pass++) for (int i=0; i<storage; i++) {
            ItemStack existing = cells.get(i);
            if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize()-existing.getCount());
                if (moved > 0) { existing.grow(moved); remaining.shrink(moved); }
            } else if (pass == 1 && existing.isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                if (moved > 0) cells.set(i, remaining.split(moved));
            }
        }
        return remaining.isEmpty();
    }
}
