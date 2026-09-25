/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagSlots;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The player's own inventory menu carries the worn bag's forty cells after vanilla's 46 slots,
 * on both sides, and shift-click from the inventory goes into the bag first (D-0027). */
@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin implements com.chunkworks.backpacksplus.WornBagMenu {
    @Unique private WornBag backpacksplus$bag;
    @Unique private int backpacksplus$first = -1;
    @Override public WornBag backpacksplus$bag() { return backpacksplus$bag; }
    @Override public int backpacksplus$first() { return backpacksplus$first; }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void backpacksplus$addBagSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        backpacksplus$bag = new WornBag(owner);
        backpacksplus$first = WornBagSlots.add((InventoryMenu) (Object) this, backpacksplus$bag);
    }
    /** effects: from an inventory or hotbar slot, into the bag first unless the item equips into
     * an empty armor or offhand slot (vanilla's rule keeps that); when the bag takes it all the
     * click is done, otherwise vanilla goes on with what is left. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void backpacksplus$bagFirst(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 9 || index >= 45 || backpacksplus$bag == null || backpacksplus$bag.tier() == null) return;
        var menu = (InventoryMenu) (Object) this;
        var slot = menu.slots.get(index);
        if (!slot.hasItem()) return;
        var stack = slot.getItem();
        var equipment = player.getEquipmentSlotForItem(stack);
        if (equipment.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !menu.slots.get(8 - equipment.getIndex()).hasItem()) return;
        if (equipment == EquipmentSlot.OFFHAND && !menu.slots.get(45).hasItem()) return;
        var before = stack.copy();
        if (!WornBagSlots.moveInto(menu, stack)) return;
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); cir.setReturnValue(before); }
        else slot.setChanged();
    }
}
