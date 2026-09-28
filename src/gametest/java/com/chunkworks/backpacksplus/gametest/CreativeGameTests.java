/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real-server partitions for D-0031, a creative player's edits of the worn bag's cells, sent as
 * vanilla's creative slot packet through the player's real packet handler. Player: creative;
 * survival. Slot: a bag cell; a vanilla slot (the hotbar's first). Stack: an item the cell admits;
 * empty; another bag (nested storage); an item for a cell the worn tier does not have (inactive). */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class CreativeGameTests {
    public CreativeGameTests() {}
    /** effects: a player with a real packet handler over an embedded connection, never placed in
     * the level: the handler runs as for a joined player, and nothing ticks, tracks or syncs it.
     * A placed mock negotiates no channel, and this mod's sync (and Curios', and Quick Slot's)
     * sends to whoever tracks a player, so placed mocks in parallel tests throw in each other's
     * ticks (seen on the first run: SyncGameTests' own channel-less test went red beside these). */
    private static ServerPlayer player(GameTestHelper h, GameType mode) {
        var server = h.getLevel().getServer();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "creative-mock"), false);
        var p = new ServerPlayer(server, h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(server, connection, p, cookie);  // sets p.connection
        p.setGameMode(mode);
        p.getInventory().setItem(BagLocations.CHEST, new ItemStack(BackpackItems.BASIC.get()));
        return p;
    }
    private static int first(ServerPlayer p) { return ((WornBagMenu) p.inventoryMenu).backpacksplus$first(); }
    private static void send(ServerPlayer p, int slot, ItemStack stack) { p.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(slot, stack)); }
    private static ItemStack cell(ServerPlayer p, int cell) { return BagContents.copy(p.getInventory().getItem(BagLocations.CHEST)).get(cell); }

    @GameTest(template = "empty") public void aCreativeEditOfABagCellReachesTheBag(GameTestHelper h) {
        var p = player(h, GameType.CREATIVE);
        send(p, first(p), new ItemStack(Items.APPLE, 16));
        h.assertTrue(cell(p, 0).is(Items.APPLE) && cell(p, 0).getCount() == 16, "cell 0 holds the sixteen apples: " + cell(p, 0));
        send(p, first(p), ItemStack.EMPTY);
        h.assertTrue(cell(p, 0).isEmpty(), "and is emptied the same way: " + cell(p, 0));
        send(p, 36, new ItemStack(Items.BREAD, 3));
        h.assertTrue(p.getInventory().getItem(0).is(Items.BREAD), "a vanilla slot still goes to vanilla: " + p.getInventory().getItem(0));
        h.succeed();
    }

    @GameTest(template = "empty") public void aCreativeEditIsHeldToTheBagsRules(GameTestHelper h) {
        var p = player(h, GameType.CREATIVE);
        send(p, first(p) + 1, new ItemStack(BackpackItems.BASIC.get()));
        h.assertTrue(cell(p, 1).isEmpty(), "no bag in a bag: " + cell(p, 1));
        send(p, first(p) + 20, new ItemStack(Items.APPLE, 5));
        var cells = BagContents.copy(p.getInventory().getItem(BagLocations.CHEST));
        h.assertTrue(cells.stream().allMatch(ItemStack::isEmpty), "a Basic bag has no cell 20, and nothing went in: " + cells);
        h.assertTrue(p.getInventory().getItem(0).isEmpty() && p.getInventory().getItem(9).isEmpty(), "and nothing lands anywhere else");
        h.succeed();
    }

    @GameTest(template = "empty") public void aSurvivalPlayerCannotEditTheBagThroughTheCreativePacket(GameTestHelper h) {
        var p = player(h, GameType.SURVIVAL);
        send(p, first(p), new ItemStack(Items.DIAMOND, 64));
        h.assertTrue(cell(p, 0).isEmpty(), "survival: refused: " + cell(p, 0));
        h.succeed();
    }
}
