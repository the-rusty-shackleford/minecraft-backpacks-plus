/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.quickslot.compat.Driving;
import net.minecraft.world.entity.player.Player;

/**
 * Loaded only behind the quickslot presence check; original H handling and attachment stay with
 * Quick Slot. G never reaches its slot: the two keys are the two mods'.
 */
final class QuickSlotCompat {
    private QuickSlotCompat() {}
    static void registerPresentation() {
        com.chunkworks.quickslot.client.BodyLayer.backpackVisibility(GearPoses::wornVisible);
    }
    static boolean driving(Player player) { return Driving.isDriver(player); }
}
