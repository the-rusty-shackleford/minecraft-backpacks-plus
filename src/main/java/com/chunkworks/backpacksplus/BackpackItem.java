/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** AF: a single portable inventory at the specified tier. RI: tier immutable, maximum stack count one. */
public final class BackpackItem extends Item implements Equipable {
    private final BackpackTier tier;
    /** effects: constructs a nonstacking bag of the specified tier. */
    public BackpackItem(Properties properties, BackpackTier tier) { super(properties); this.tier = tier; }
    /** effects: returns the bag's immutable tier. */
    public BackpackTier tier() { return tier; }
    @Override public EquipmentSlot getEquipmentSlot() { return EquipmentSlot.CHEST; }
    @Override public boolean canFitInsideContainerItems(ItemStack stack) { return false; }

    /** effects: opens the actual held bag on the server; never accepts client-authored contents. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isAlive() || player.isSpectator()) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer server && player.containerMenu == player.inventoryMenu) {
            int source = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            BagInventory inventory = BagInventory.bind(player, source);
            var opened = server.openMenu(new SimpleMenuProvider((id, inv, p) -> new BackpackMenu(id, inv, inventory, tier, source), stack.getHoverName()),
                    buffer -> { buffer.writeEnum(tier); buffer.writeVarInt(source); });
            if (opened.isPresent()) GearSync.action(server, stack, com.chunkworks.backpacksplus.domain.GearAction.OPEN, -1, ItemStack.EMPTY, ItemStack.EMPTY);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
