/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A slot's x is fixed at construction in vanilla; the worn bag's cells follow their panel,
 * which moves when the recipe book opens beside it (D-0028). Client side only: the server
 * never reads a slot's position. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Accessor("x") @Mutable void backpacksplus$setX(int x);
}
