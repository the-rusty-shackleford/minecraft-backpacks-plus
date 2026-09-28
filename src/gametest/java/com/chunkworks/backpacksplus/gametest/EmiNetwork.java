/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.google.gson.JsonObject;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

/** Test-only bridge to EMI for the fixture (D-0033), loaded only with EMI present; never shipped.
 * Asks EMI, as its sidebar and fill button do, whether a recipe can be crafted from what the open
 * screen offers, and fills it through EMI's own fill path, whose packet the server applies. */
final class EmiNetwork {
    private EmiNetwork() {}
    private static String last = "";

    private static AbstractContainerScreen<?> screen() {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) throw new IllegalStateException("no container screen");
        return screen;
    }
    static void check(String id) {
        var recipe = EmiApi.getRecipeManager().getRecipe(ResourceLocation.parse(id));
        if (recipe == null) throw new IllegalStateException("EMI has no recipe " + id);
        screen();
        last = id + "=" + EmiPlayerInventory.of(Minecraft.getInstance().player).canCraft(recipe);
    }
    static void fill(String id) {
        var recipe = EmiApi.getRecipeManager().getRecipe(ResourceLocation.parse(id));
        if (recipe == null) throw new IllegalStateException("EMI has no recipe " + id);
        last = id + " filled=" + EmiRecipeFiller.performFill(recipe, screen(), EmiCraftContext.Type.FILL_BUTTON, EmiCraftContext.Destination.NONE, 1);
    }
    static void observe(JsonObject state) { state.addProperty("emi", last); }
}
