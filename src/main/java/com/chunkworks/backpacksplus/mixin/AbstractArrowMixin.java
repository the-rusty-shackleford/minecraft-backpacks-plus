/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.BagPickup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** An arrow pulled out of the ground goes into the backpacks too when the inventory has no room
 * (D-0029), as a dropped one would. */
@Mixin(AbstractArrow.class)
abstract class AbstractArrowMixin {
    @WrapOperation(method = "tryPickup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean backpacksplus$intoBags(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        return BagPickup.add(inventory, stack, s -> original.call(inventory, s));
    }
}
