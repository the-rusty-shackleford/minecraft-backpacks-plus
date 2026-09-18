/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import net.neoforged.fml.common.Mod;

/** Development-only entry point; never packaged in the production jar. */
@Mod("backpacksplus_gametest")
public final class TestMod {
    public TestMod(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
            if (net.neoforged.fml.ModList.get().isLoaded("curios")) event.register(CuriosGameTests.class);
        });
    }
}
