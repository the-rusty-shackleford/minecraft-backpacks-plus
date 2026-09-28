/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagSlots;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A crafting table's menu carries the worn bag's forty cells after its own 46 slots, on both
 * sides, as the player's inventory and a chest's do (D-0033): the bag's cells are then real slots
 * at the table, so EMI's fill and shift-click reach them. Another mod's subclass of the crafting
 * menu is left as it is.
 * <p>Shift-click keeps vanilla's rules and adds one: what vanilla sends from the result or the
 * grid to the player's inventory and cannot fit there goes into the bag (storage before mounts),
 * where vanilla would have dropped a result at the player's feet. From the inventory a stack goes
 * to the grid as before; from the bag, to the inventory. */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin implements com.chunkworks.backpacksplus.WornBagMenu {
    @Unique private WornBag backpacksplus$bag;
    @Unique private int backpacksplus$first;
    @Override public WornBag backpacksplus$bag() { return backpacksplus$bag; }
    @Override public int backpacksplus$first() { return backpacksplus$bag == null ? -1 : backpacksplus$first; }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void backpacksplus$addBagSlots(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        if ((Object) this.getClass() != CraftingMenu.class) return;
        var bag = new WornBag(inventory.player);
        backpacksplus$first = WornBagSlots.add((CraftingMenu) (Object) this, bag);
        backpacksplus$bag = bag;
    }

    @WrapOperation(method = "quickMoveStack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/CraftingMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean backpacksplus$overflowIntoBag(CraftingMenu menu, ItemStack stack, int start, int end, boolean reverse,
                                                  Operation<Boolean> original, @Local(argsOnly = true) int index) {
        boolean moved = original.call(menu, stack, start, end, reverse);
        if (backpacksplus$bag != null && index < 10 && start == 10 && end == 46 && !stack.isEmpty())
            moved |= WornBagSlots.moveInto(menu, stack);
        return moved;
    }
}
