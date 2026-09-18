/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** AF: a standard shaped crafting recipe with contents-preserving backpack assembly.
 * RI: result is one backpack; inherited JSON, network, mirroring and recipe-book shape
 * semantics remain vanilla. Recipe checks and previews never mutate inventory. */
public final class BackpackCraftingRecipe extends ShapedRecipe {
    private final BackpackItem target;
    /** requires: a shaped recipe yielding one backpack. effects: wraps its immutable
     * shape and resolved result; throws: IllegalArgumentException for another result. */
    public BackpackCraftingRecipe(ShapedRecipe base) {
        // Vanilla's resolved result getter does not consult the registry provider.
        super(base.getGroup(),base.category(),base.pattern,base.getResultItem(RegistryAccess.EMPTY).copy(),base.showNotification());
        var result=base.getResultItem(RegistryAccess.EMPTY);
        if (result.getCount()!=1 || !(result.getItem() instanceof BackpackItem bag)) throw new IllegalArgumentException("Expected one backpack result");
        target=bag;
    }
    @Override public boolean matches(CraftingInput input,Level level) {
        return super.matches(input,level) && !BagCrafting.result(input,target,false).isEmpty();
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) {
        return pattern.matches(input) ? BagCrafting.result(input,target,false) : ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer() { return BackpackRecipes.CRAFTING.get(); }
    /** Uses vanilla's codecs, retaining recipe book and datapack compatibility. */
    public static final class Serializer implements RecipeSerializer<BackpackCraftingRecipe> {
        private static final MapCodec<BackpackCraftingRecipe> CODEC=new ShapedRecipe.Serializer().codec().xmap(BackpackCraftingRecipe::new,r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf,BackpackCraftingRecipe> STREAM=new ShapedRecipe.Serializer().streamCodec().map(BackpackCraftingRecipe::new,r -> r);
        @Override public MapCodec<BackpackCraftingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,BackpackCraftingRecipe> streamCodec() { return STREAM; }
    }
}
