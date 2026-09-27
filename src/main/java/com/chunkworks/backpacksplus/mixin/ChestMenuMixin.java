/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A chest's menu carries the worn bag's forty cells after the chest's and the player's slots,
 * on both sides, as the player's own inventory menu does (D-0027, D-0030): every vanilla chest
 * screen (single, double, trapped, barrel, ender chest). Another mod's subclass of the chest
 * menu is left as it is: its own shift-click may count on the slots it knows.
 * <p>Shift-click is taken over while the cells are there, since vanilla's sends a chest stack to
 * every slot after the chest's from the end, which would fill the bag's mounts first: a chest
 * stack goes to the player's inventory as vanilla sends it, and only what is left into the bag
 * (storage before mounts); an inventory stack goes into the chest as before; a bag stack goes
 * into the chest. */
@Mixin(ChestMenu.class)
abstract class ChestMenuMixin implements com.chunkworks.backpacksplus.WornBagMenu {
    @Shadow @Final private int containerRows;
    @Unique private WornBag backpacksplus$bag;
    @Unique private int backpacksplus$first;
    @Override public WornBag backpacksplus$bag() { return backpacksplus$bag; }
    @Override public int backpacksplus$first() { return backpacksplus$bag == null ? -1 : backpacksplus$first; }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V", at = @At("TAIL"))
    private void backpacksplus$addBagSlots(MenuType<?> type, int id, Inventory inventory, Container container, int rows, CallbackInfo ci) {
        if ((Object) this.getClass() != ChestMenu.class) return;
        var bag = new WornBag(inventory.player);
        backpacksplus$first = WornBagSlots.add((ChestMenu) (Object) this, bag);
        backpacksplus$bag = bag;
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void backpacksplus$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (backpacksplus$bag == null) return;
        var menu = (ChestMenu) (Object) this;
        var access = (AbstractContainerMenuAccessor) menu;
        int chest = containerRows * 9, inventoryEnd = chest + 36;
        var slot = menu.slots.get(index);
        if (slot == null || !slot.hasItem()) { cir.setReturnValue(ItemStack.EMPTY); return; }
        var stack = slot.getItem();
        var before = stack.copy();
        boolean moved;
        if (index < chest) {
            moved = access.backpacksplus$moveItemStackTo(stack, chest, inventoryEnd, true);
            if (!stack.isEmpty()) moved |= WornBagSlots.moveInto(menu, stack);
        } else {
            moved = access.backpacksplus$moveItemStackTo(stack, 0, chest, false);
        }
        if (!moved) { cir.setReturnValue(ItemStack.EMPTY); return; }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        cir.setReturnValue(before);
    }
}
