/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.BagPickup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** An item picked up off the ground goes into the backpacks too (D-0029). A wrap, not a redirect,
 * so another mod's wrap of the same call still runs: it is the {@code vanilla} step. */
@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean backpacksplus$intoBags(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        return BagPickup.add(inventory, stack, s -> original.call(inventory, s));
    }
}
