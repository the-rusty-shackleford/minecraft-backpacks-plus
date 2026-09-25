/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.integration.emi;

import com.chunkworks.backpacksplus.client.BagPanelScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** EMI lays its sidebars out from the vanilla screen's own rectangle and would paint its left
 * one over the worn bag's panel beside the inventory screen (D-0028). This plugin declares the
 * panel's column so EMI keeps off it. Loaded by EMI's entrypoint scan only when EMI is
 * installed; nothing else references it. */
@EmiEntrypoint
public final class BackpacksEmiPlugin implements EmiPlugin {
    /** effects: excludes the panel's column at the screen's full height. EMI shrinks a sidebar
     * away from an excluded rectangle only when the rectangle covers a third of the sidebar's
     * height or two thirds of its width; for anything smaller it keeps the sidebar and merely
     * skips the entries under the rectangle, which its themed background then paints over. A
     * Basic bag's panel is 68 pixels tall and fell under that line; the column at the screen's
     * 166 does not. */
    @Override public void register(EmiRegistry registry) {
        registry.addExclusionArea(InventoryScreen.class, (screen, out) -> {
            var panel = ((BagPanelScreen) screen).backpacksplus$panel();
            if (panel != null) out.accept(new Bounds(panel[0], screen.getGuiTop(), panel[2], Math.max(panel[3], screen.getYSize())));
        });
    }
}
