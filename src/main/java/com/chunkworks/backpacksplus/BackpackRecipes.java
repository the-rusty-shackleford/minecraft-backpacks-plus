/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Common-side recipe serializer registration. No client classes or player state. */
public final class BackpackRecipes {
    private BackpackRecipes() {}
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,BackpacksPlus.ID);
    public static final DeferredHolder<RecipeSerializer<?>,BackpackCraftingRecipe.Serializer> CRAFTING=SERIALIZERS.register("backpack_crafting",BackpackCraftingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>,BackpackDyeRecipe.Serializer> DYE=SERIALIZERS.register("backpack_dye",BackpackDyeRecipe.Serializer::new);
    /** effects: registers data-driven crafting and recoloring on the common mod bus. */
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
