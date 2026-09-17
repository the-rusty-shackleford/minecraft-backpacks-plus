/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import com.chunkworks.backpacksplus.domain.GearAction;

/**
 * AF: ordinary storage, dedicated mounts and the player's inventory in one vanilla menu.
 * RI: the opened backpack's source cell cannot be moved by pickup, number-key swap,
 * shift-click or pickup-all; every bag insertion uses the same admission predicate.
 */
public final class BackpackMenu extends AbstractContainerMenu {
    public final BackpackTier tier;
    private final Container contents;
    private final int source;
    /** effects: binds the menu to an existing server inventory or client snapshot. */
    public BackpackMenu(int id, Inventory inventory, Container contents, BackpackTier tier, int source) {
        super(BackpackItems.MENU.get(), id); this.contents = contents; this.tier = tier; this.source = source;
        checkContainerSize(contents, tier.totalSlots());
        for (int i = 0; i < tier.storageSlots(); i++) addSlot(new Slot(contents, i, 80 + i % 9 * 18, 24 + i / 9 * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return BagContents.admits(tier, getContainerSlot(), stack); }
        });
        for (int i = 0; i < tier.mounts().size(); i++) {
            int cell = tier.mountSlot(i);
            addSlot(new Slot(contents, cell, 12 + (i % 2) * 40, 26 + (i / 2) * 44) {
                @Override public boolean mayPlace(ItemStack stack) { return BagContents.admits(tier, getContainerSlot(), stack); }
            });
        }
        int top = inventoryTop();
        for (int i = 9; i < 36; i++) addPlayerSlot(inventory, i, 80 + (i - 9) % 9 * 18, top + (i - 9) / 9 * 18);
        for (int i = 0; i < 9; i++) addPlayerSlot(inventory, i, 80 + i * 18, top + 58);
    }
    /** effects: reads only server-supplied tier/source metadata for the client menu. */
    public static BackpackMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BackpackTier tier = buffer.readEnum(BackpackTier.class);
        int source = buffer.readVarInt();
        if (source < 0 || source >= inventory.getContainerSize()) throw new IllegalArgumentException("Invalid bag source");
        return new BackpackMenu(id, inventory, new SimpleContainer(tier.totalSlots()), tier, source);
    }
    /** effects: returns the first player-inventory row's y coordinate. */
    public int inventoryTop() { return Math.max(112, 38 + tier.storageSlots() / 9 * 18); }
    private void addPlayerSlot(Inventory inventory, int cell, int x, int y) {
        addSlot(new Slot(inventory, cell, x, y) {
            @Override public boolean mayPickup(Player player) { return cell != source; }
            @Override public boolean mayPlace(ItemStack stack) { return cell != source; }
        });
    }
    @Override public boolean stillValid(Player player) { return contents.stillValid(player); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || type == ClickType.SWAP && button == source) return;
        ItemStack before = slot >= 0 && slot < tier.totalSlots() ? contents.getItem(slot).copy() : ItemStack.EMPTY;
        super.clicked(slot, button, type, player);
        if (player instanceof ServerPlayer server && !before.isEmpty() && slot >= 0 && slot < tier.totalSlots()
                && type != ClickType.THROW && contents.getItem(slot).getCount() < before.getCount()) {
            GearSync.action(server, player.getInventory().getItem(source), GearAction.RETRIEVE,
                    slot < tier.storageSlots() ? -1 : slot-tier.storageSlots(), before, contents.getItem(slot));
        }
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer server && contents.stillValid(player))
            GearSync.action(server, player.getInventory().getItem(source), GearAction.CLOSE, -1, ItemStack.EMPTY, ItemStack.EMPTY);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack working = slot.getItem(), original = working.copy();
        int owned = tier.totalSlots();
        // Shift-click into a backpack fills ordinary storage, never a visible mount implicitly.
        if (index < owned ? !moveItemStackTo(working, owned, slots.size(), true)
                : !moveItemStackTo(working, 0, tier.storageSlots(), false)) return ItemStack.EMPTY;
        if (working.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, working);
        return original;
    }
}
