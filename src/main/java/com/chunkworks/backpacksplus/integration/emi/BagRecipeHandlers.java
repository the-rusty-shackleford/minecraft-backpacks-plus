/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.integration.emi;

import com.chunkworks.backpacksplus.WornBag;
import com.chunkworks.backpacksplus.WornBagMenu;
import com.chunkworks.backpacksplus.domain.HandlerOrder;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.handler.InventoryRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;

/** EMI counts and fills from a screen's "input sources", which its own handlers fix to the
 * inventory's slot ranges, so the worn bag's cells on the inventory screen (D-0027) and at the
 * crafting table (D-0033) were invisible to it. These are EMI's own handlers with the bag's
 * active storage cells added (never its mounts: gear, not ingredients); EMI's fill then moves
 * items out of them as out of any slot of the menu, on the server, through the cells' own rules.
 * EMI uses the first handler registered for a menu that supports a recipe, and its own are
 * registered by its built-in plugin, so these are put ahead of EMI's own in its list. */
final class BagRecipeHandlers {
    private BagRecipeHandlers() {}

    /** effects: the menu's active bag storage cells, in cell order. */
    static List<Slot> bagCells(AbstractContainerMenu menu) {
        int first = ((WornBagMenu) menu).backpacksplus$first();
        if (first < 0) return List.of();
        var out = new ArrayList<Slot>(WornBag.STORAGE);
        for (int k = 0; k < WornBag.STORAGE; k++) {
            var slot = menu.slots.get(first + k);
            if (slot.isActive()) out.add(slot);
        }
        return out;
    }

    static final class Inventory extends InventoryRecipeHandler {
        @Override public List<Slot> getInputSources(InventoryMenu menu) {
            var sources = new ArrayList<>(super.getInputSources(menu));
            sources.addAll(bagCells(menu));
            return sources;
        }
    }

    static final class Crafting extends CraftingRecipeHandler {
        @Override public List<Slot> getInputSources(CraftingMenu menu) {
            var sources = new ArrayList<>(super.getInputSources(menu));
            sources.addAll(bagCells(menu));
            return sources;
        }
    }

    /** effects: puts the handler just ahead of EMI's own in EMI's list for the menu type (null: the
     * inventory), so it answers before them, and behind any other mod's that stood itself first
     * (HandlerOrder: the order is then the same whichever registered first). EMI keeps that list in
     * an internal class; if a later EMI moves it, the handler stays registered behind EMI's own and
     * the bag is simply not counted, which is logged. */
    static void aheadOfEmis(MenuType<?> type, EmiRecipeHandler<?> handler, Logger log) {
        try {
            var list = EmiRecipeFiller.handlers.get(type);
            if (list != null) HandlerOrder.aheadOfOwn(list, handler, h -> h.getClass().getName().startsWith("dev.emi.emi."));
        } catch (LinkageError | RuntimeException e) {
            log.warn("backpacksplus: could not put the bag's EMI handler ahead of EMI's own for {}: {}", type, e.toString());
        }
    }
}
