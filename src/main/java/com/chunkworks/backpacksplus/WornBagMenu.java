/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

/** What a menu carrying the worn bag's cells knows about them (the player's inventory menu,
 * D-0027; a chest's, D-0030), implemented by the menu mixins: the bag they show and the menu
 * index of the first of them, which is whatever the menu held when they were added, since other
 * mods add slots of their own; −1 and null on a menu that carries none. */
public interface WornBagMenu {
    WornBag backpacksplus$bag();
    int backpacksplus$first();
}
