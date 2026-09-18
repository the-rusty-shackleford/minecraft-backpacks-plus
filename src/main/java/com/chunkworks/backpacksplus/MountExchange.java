/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.SwapRules;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-only, all-or-nothing exchange between the selected hotbar cell and one bag mount.
 * AF: a successful transaction moves actual server-owned stacks; a refused one changes nothing.
 * RI: the standalone Quick Slot is never read or modified; calculations use private copies.
 */
public final class MountExchange {
    private MountExchange() {}

    /**
     * requires: server thread and an inventory/equipment source chosen by the server.
     * effects: validates identity, revision, selection and current activity, then exchanges
     * the full stacks. An incompatible old hand stack uses ordinary bag storage only if
     * it fits completely. Returns false without mutation on a stale or impossible intent.
     */
    public static boolean swap(Player player, int source, UUID bagId, long revision, int mount, int selected) {
        if (player.level().isClientSide || source < 0 || source == selected) return false;
        ItemStack bag = BagLocations.stack(player,source);
        if (!(bag.getItem() instanceof BackpackItem) || bag.getCount() != 1) return false;
        boolean busy = player.isUsingItem() || player.containerMenu != player.inventoryMenu;
        if (!SwapRules.current(player.isAlive(), player.isSpectator(), busy, bagId != null && bagId.equals(bag.get(BackpackItems.ID)),
                selected, player.getInventory().selected, revision, BagContents.revision(bag))) return false;
        var tier = BagContents.tier(bag);
        if (mount < 0 || mount >= tier.mounts().size()) return false;
        int cell = tier.mountSlot(mount);
        NonNullList<ItemStack> working = BagContents.copy(bag);
        ItemStack drawn = working.get(cell).copy(), held = player.getMainHandItem().copy();
        if (ItemStack.matches(drawn, held)) return false;
        boolean fitsMount = BagContents.admits(tier, cell, held);
        boolean fitsStorage = !fitsMount && BagContents.storable(held) && insert(working, tier.storageSlots(), held);
        var destination = SwapRules.destination(drawn.isEmpty(), fitsMount, fitsStorage);
        if (destination == SwapRules.Destination.REFUSE) return false;
        working.set(cell, destination == SwapRules.Destination.MOUNT ? held : ItemStack.EMPTY);
        // store validates everything before the first mutation. The remaining vanilla
        // inventory assignment cannot fail for the already-validated hotbar index.
        if (!BagContents.store(bag, working)) return false;
        player.getInventory().setItem(selected, drawn);
        player.getInventory().setChanged();
        BagLocations.changed(player,source);
        player.inventoryMenu.broadcastChanges();
        return true;
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
