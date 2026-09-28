/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

/** The creative screen as the container screen's layout asks about it (D-0031). Implemented by
 * the creative screen mixin. */
public interface CreativePanelScreen {
    /** effects: how much wider than the screen the vanilla layout is laid out now, recorded as
     * the widening this layout was made with. */
    int backpacksplus$widenForLayout();
}
