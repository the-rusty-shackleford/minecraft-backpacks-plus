/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.compat.Driving;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Loaded only behind the quickslot presence check; original H handling and attachment stay with Quick Slot. */
final class QuickSlotCompat {
    private QuickSlotCompat() {}
    static void registerPresentation() {
        com.chunkworks.quickslot.client.BodyLayer.backpackVisibility(GearPoses::wornVisible);
    }
    static long revision(Player player) { return SlotData.revision(player); }
    static boolean driving(Player player) { return Driving.isDriver(player); }
    static void swap(int selected, long revision) { PacketDistributor.sendToServer(new Payloads.Swap(selected,revision)); }
}
