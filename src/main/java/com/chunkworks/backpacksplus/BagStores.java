/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.carried.api.CarriedProvider;
import com.chunkworks.carried.api.CarriedStore;
import com.google.common.collect.MapMaker;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Backpacks+ as a Carried provider (D-0032): a player carries the bag they wear (the first Curios
 * back slot holding one, else the chest slot, as {@link BagLocations#worn} decides), then any
 * other bag on their back, then every bag in the inventory in its order (the hotbar first, then
 * the main slots, a bag in the chest slot while a back bag is worn, then the offhand). Each is its
 * storage cells; mounts never.
 *
 * <p>Listing runs per frame in HUDs and per tick in gunnery, so it allocates nothing once a
 * player's bags have been seen: store objects are kept per player and source and reused, the
 * inventory's lists are read directly (its {@code getItem} walks its compartments with an
 * iterator), and the back slots are read with one Curios lookup into an array kept per player.
 * Players are held weakly and compared by identity: a client's player and the integrated server's
 * are equal by entity id, and must not share stores.
 */
public final class BagStores implements CarriedProvider {
    /** One player's reused listing state; {@code curiosBack} is the Curios back-slot handler as
     * looked up at game time {@code curiosAt}, typed Object so this class loads without Curios. */
    static final class Carrier {
        final Int2ObjectOpenHashMap<BagStore> stores = new Int2ObjectOpenHashMap<>();
        final ItemStack[] backs = new ItemStack[8];
        Object curiosBack;
        long curiosAt = Long.MIN_VALUE;
    }
    private static final ConcurrentMap<Player, Carrier> CARRIERS = new MapMaker().weakKeys().makeMap();

    @Override public String id() { return BackpacksPlus.ID; }

    @Override public void stores(Player player, List<CarriedStore> out) {
        var carrier = CARRIERS.computeIfAbsent(player, p -> new Carrier());
        var inventory = player.getInventory();
        int backs = BagLocations.CURIOS ? CuriosCompat.backBags(player, carrier.backs, carrier) : 0;
        int worn = BagLocations.NONE;
        for (int slot = 0; slot < backs && worn < 0; slot++) if (!carrier.backs[slot].isEmpty()) worn = BagLocations.CURIOS_BACK + slot;
        if (worn < 0 && BagLocations.isBag(slot(inventory, BagLocations.CHEST))) worn = BagLocations.CHEST;
        if (worn >= BagLocations.CURIOS_BACK) add(out, carrier, player, worn, carrier.backs[worn - BagLocations.CURIOS_BACK]);
        else if (worn >= 0) add(out, carrier, player, worn, slot(inventory, worn));
        for (int slot = 0; slot < backs; slot++) {
            int source = BagLocations.CURIOS_BACK + slot;
            if (source != worn && !carrier.backs[slot].isEmpty()) add(out, carrier, player, source, carrier.backs[slot]);
        }
        for (int i = 0; i <= Inventory.SLOT_OFFHAND; i++) {
            if (i == worn) continue;
            var stack = slot(inventory, i);
            if (BagLocations.isBag(stack)) add(out, carrier, player, i, stack);
        }
    }

    /** effects: the inventory's stack at a slot as it numbers them, without its iterator. */
    private static ItemStack slot(Inventory inventory, int i) {
        if (i < Inventory.INVENTORY_SIZE) return inventory.items.get(i);
        if (i < Inventory.SLOT_OFFHAND) return inventory.armor.get(i - Inventory.INVENTORY_SIZE);
        return inventory.offhand.get(0);
    }

    private static void add(List<CarriedStore> out, Carrier carrier, Player player, int source, ItemStack bag) {
        var store = carrier.stores.get(source);
        if (store == null) {
            store = new BagStore(player, source);
            carrier.stores.put(source, store);
        }
        store.refresh(bag);
        if (store.size() > 0) out.add(store);
    }
}
