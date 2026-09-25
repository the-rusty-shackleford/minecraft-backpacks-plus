/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

/** What the player's inventory menu knows about its worn-bag cells (D-0027), implemented by the
 * menu mixin: the bag they show and the menu index of the first of them, which is whatever the
 * menu held when they were added, since other mods add slots of their own. */
public interface WornBagMenu {
    WornBag backpacksplus$bag();
    int backpacksplus$first();
}
