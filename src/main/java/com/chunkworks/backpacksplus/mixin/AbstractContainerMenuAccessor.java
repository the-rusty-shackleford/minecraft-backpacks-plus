/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The two protected menu operations the worn-bag slots need (D-0027). */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addSlot") Slot backpacksplus$addSlot(Slot slot);
    @Invoker("moveItemStackTo") boolean backpacksplus$moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);
}
