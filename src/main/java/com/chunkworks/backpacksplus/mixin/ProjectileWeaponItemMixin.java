/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.BagAmmo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Where a bow or crossbow takes its shot out of the projectile stack it was handed: when that
 * stack was lent out of a backpack, the bag pays for it and the arrow loses the marker
 * (D-0026). Vanilla offers no hook between the lookup and the debit. */
@Mixin(ProjectileWeaponItem.class)
abstract class ProjectileWeaponItemMixin {
    @Inject(method = "useAmmo", at = @At("RETURN"))
    private static void backpacksplus$debit(ItemStack weapon, ItemStack ammo, LivingEntity shooter, boolean intangible, CallbackInfoReturnable<ItemStack> cir) {
        BagAmmo.debit(shooter, ammo, cir.getReturnValue());
    }
}
