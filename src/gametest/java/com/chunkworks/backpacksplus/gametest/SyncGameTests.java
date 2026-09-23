/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.BackpackItems;
import com.chunkworks.backpacksplus.BagContents;
import com.chunkworks.backpacksplus.GearSync;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Partitions: a server player whose connection never negotiated the gear channel (the framework's
 * mock) logging in, then ticking, with a bag worn: the sync must skip them rather than throw out
 * of the event, which took every later listener of that event down with it.
 */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class SyncGameTests {
    public SyncGameTests() {}

    @GameTest(template="empty") public void aPlayerWithoutTheChannelIsSkippedNotCrashed(GameTestHelper h) {
        ServerPlayer p=h.makeMockServerPlayerInLevel();
        ItemStack bag=new ItemStack(BackpackItems.EXPEDITION.get()); BagContents.identify(bag);
        p.getInventory().setItem(38,bag);
        h.assertFalse(p.connection.hasChannel(com.chunkworks.backpacksplus.GearProtocol.State.TYPE),"the mock negotiated nothing");
        // This mod's listeners, called as the bus would call them. Not posted on the bus: Quick Slot,
        // on this classpath, still sends its own state to any player and would fail the test for us.
        GearSync.login(new PlayerEvent.PlayerLoggedInEvent(p));   // 0.2.1 threw here: the state payload may not be sent
        GearSync.tick(new PlayerTickEvent.Post(p));
        p.getInventory().setItem(38,ItemStack.EMPTY);              // a change the sync would announce
        GearSync.tick(new PlayerTickEvent.Post(p));
        h.succeed();
    }
}
