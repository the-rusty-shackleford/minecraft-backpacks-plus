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
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** AF: a recipe-book-visible, one-bag/one-dye recolor at the same tier.
 * RI: dye replaces rather than mixes the color; input is untouched and all contents,
 * identity and non-color components survive. */
public final class BackpackDyeRecipe extends ShapelessRecipe {
    private final BackpackItem target;
    /** requires: a two-ingredient shapeless recipe producing one backpack.
     * effects: wraps the resolved recipe; throws: IllegalArgumentException otherwise. */
    public BackpackDyeRecipe(ShapelessRecipe base) {
        super(base.getGroup(),base.category(),base.getResultItem(RegistryAccess.EMPTY).copy(),base.getIngredients());
        var result=base.getResultItem(RegistryAccess.EMPTY);
        if (result.getCount()!=1 || !(result.getItem() instanceof BackpackItem bag) || base.getIngredients().size()!=2)
            throw new IllegalArgumentException("Expected backpack and dye recipe");
        target=bag;
    }
    @Override public boolean matches(CraftingInput input,Level level) {
        return super.matches(input,level) && !BagCrafting.result(input,target,true).isEmpty();
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) {
        // A two-ingredient result is validated here too, including calls from automation.
        if (input.ingredientCount()!=2 || !input.stackedContents().canCraft(this,null)) return ItemStack.EMPTY;
        return BagCrafting.result(input,target,true);
    }
    @Override public RecipeSerializer<?> getSerializer() { return BackpackRecipes.DYE.get(); }
    /** Uses vanilla shapeless codecs; no new crafting or network protocol. */
    public static final class Serializer implements RecipeSerializer<BackpackDyeRecipe> {
        private static final MapCodec<BackpackDyeRecipe> CODEC=new ShapelessRecipe.Serializer().codec().xmap(BackpackDyeRecipe::new,r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf,BackpackDyeRecipe> STREAM=new ShapelessRecipe.Serializer().streamCodec().map(BackpackDyeRecipe::new,r -> r);
        @Override public MapCodec<BackpackDyeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,BackpackDyeRecipe> streamCodec() { return STREAM; }
    }
}
