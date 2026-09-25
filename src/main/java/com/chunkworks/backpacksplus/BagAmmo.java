/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import java.util.ArrayList;
import java.util.List;

/** Arrows and every other projectile a weapon accepts count when they are in a backpack, the
 * worn one first and then any bag in the inventory, after vanilla has looked in the hands and
 * the inventory and found nothing (D-0026). A bag's contents are an immutable component, so the
 * weapon is handed a copy of the cell's stack marked with the bag's address and the cell; when
 * the weapon takes its shot from that copy on the server, {@link #debit} removes the same from
 * the bag and drops the marker from the arrow so it stacks as before. */
public final class BagAmmo {
    /** The key under CUSTOM_DATA on a stack lent out of a bag: {@code source} and {@code cell}. */
    static final String TAG = "backpacksplus_ammo";
    private BagAmmo() {}
    /** effects: listens for the game's projectile lookup. */
    static void register() { NeoForge.EVENT_BUS.addListener(BagAmmo::lend); }

    /** effects: when vanilla found no projectile for a player's weapon, the first cell of the first
     * bag (worn, then inventory) that holds one the weapon accepts, as a marked copy. */
    static void lend(LivingGetProjectileEvent event) {
        if (!event.getProjectileItemStack().isEmpty() || !(event.getEntity() instanceof Player player)) return;
        var weapon = event.getProjectileWeaponItemStack();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem item)) return;
        var accepts = item.getAllSupportedProjectiles(weapon);
        for (int source : sources(player)) {
            var bag = BagLocations.stack(player, source);
            if (!BagLocations.isBag(bag)) continue;
            var cells = BagContents.copy(bag);
            for (int cell = 0; cell < cells.size(); cell++) {
                var stack = cells.get(cell);
                if (stack.isEmpty() || !accepts.test(stack)) continue;
                var lent = stack.copy();
                var tag = new CompoundTag();
                tag.putInt("source", source);
                tag.putInt("cell", cell);
                CustomData.update(DataComponents.CUSTOM_DATA, lent, t -> t.put(TAG, tag));
                event.setProjectileItemStack(lent);
                return;
            }
        }
    }
    /** effects: the bag addresses to look in: the worn bag, then every bag in the inventory
     * (hotbar first) and the offhand. */
    static List<Integer> sources(Player player) {
        var out = new ArrayList<Integer>();
        int worn = BagLocations.worn(player);
        if (worn >= 0) out.add(worn);
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i == worn || i == BagLocations.CHEST) continue;
            if (BagLocations.isBag(inventory.getItem(i))) out.add(i);
        }
        return out;
    }
    /** effects: after a weapon took {@code taken} out of {@code ammo}: drops the marker from both;
     * on the server, when {@code ammo} was lent out of a bag and something tangible was taken,
     * removes that many from the bag's cell and marks its inventory changed. Nothing when the
     * stack was not lent, the bag is gone, or the cell no longer holds that kind. */
    public static void debit(LivingEntity shooter, ItemStack ammo, ItemStack taken) {
        var data = ammo.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TAG)) return;
        var tag = data.copyTag().getCompound(TAG);
        strip(ammo);
        strip(taken);
        if (!(shooter instanceof Player player) || shooter.level().isClientSide() || taken.isEmpty() || taken.has(DataComponents.INTANGIBLE_PROJECTILE)) return;
        int source = tag.getInt("source"), cell = tag.getInt("cell");
        var bag = BagLocations.stack(player, source);
        if (!BagLocations.isBag(bag)) return;
        var cells = BagContents.copy(bag);
        if (cell < 0 || cell >= cells.size()) return;
        var held = cells.get(cell);
        if (!ItemStack.isSameItemSameComponents(held, taken)) return;
        held.shrink(Math.min(taken.getCount(), held.getCount()));
        if (held.isEmpty()) cells.set(cell, ItemStack.EMPTY);
        if (BagContents.store(bag, cells)) BagLocations.changed(player, source);
    }
    /** effects: removes the marker; a custom-data component left empty is removed whole, so the
     * stack matches an unmarked one. */
    static void strip(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TAG)) return;
        var tag = data.copyTag();
        tag.remove(TAG);
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA); else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    /** effects: whether the stack carries the marker; for the tests. */
    public static boolean lent(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }
}
