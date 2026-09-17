/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.client.GearPoses;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla setup must finish before our short gesture; RenderPlayerEvent.Pre occurs too early. */
@Mixin(PlayerModel.class)
abstract class PlayerModelMixin {
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at=@At("TAIL"))
    private void backpacksplus$pose(LivingEntity entity, float walk, float speed, float age, float yaw, float pitch, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player)
            GearPoses.apply((PlayerModel<?>)(Object)this, player, age-player.tickCount);
    }
}
