/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

/** Common-side entry point. Feature registration follows the reviewed storage/mount design. */
@Mod(BackpacksPlus.ID)
public final class BackpacksPlus {
    public static final String ID = "backpacksplus";

    /** effects: initializes the common mod without loading any client classes. */
    public BackpacksPlus(IEventBus bus) { BackpackItems.register(bus); GearSync.register(bus); }

    /** effects: returns a resource name in this mod's namespace. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
