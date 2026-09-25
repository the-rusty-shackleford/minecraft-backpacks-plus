/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

/** The worn bag's panel on the player's inventory screen, as other overlays ask about it (EMI
 * keeps its sidebars off it, D-0028). Implemented by the inventory screen mixin. */
public interface BagPanelScreen {
    /** effects: the panel's {x, y, width, height} in GUI pixels, or null while none is shown. */
    int[] backpacksplus$panel();
}
