/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.GearAction;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Partitions: client intent, equipped snapshot, all semantic actions; real registry-backed codecs and copy ownership. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class ProtocolGameTests {
    public ProtocolGameTests() {}
    private static RegistryFriendlyByteBuf buffer(GameTestHelper h) { return new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess()); }
    @GameTest(template="empty") public void clientIntentHasNoItemAuthority(GameTestHelper h) {
        var input=new GearProtocol.Swap(UUID.randomUUID(),19,3,8); var b=buffer(h);
        try {
            GearProtocol.Swap.CODEC.encode(b,input); var decoded=GearProtocol.Swap.CODEC.decode(b);
            h.assertValueEqual(decoded,input,"intent roundtrip contains identity, revision, mount and selection only");
            h.assertValueEqual(b.readableBytes(),0,"complete intent consumed");
        } finally { b.release(); }
        h.succeed();
    }
    @GameTest(template="empty") public void equipmentSnapshotOwnsStacksAndPreservesComponents(GameTestHelper h) {
        ItemStack bag=new ItemStack(BackpackItems.EXPEDITION.get()); BagContents.identify(bag);
        var cells=BagContents.copy(bag); cells.set(36,new ItemStack(Items.TRIDENT)); BagContents.store(bag,cells);
        var input=new GearProtocol.State(12,UUID.randomUUID(),ResourceLocation.withDefaultNamespace("overworld"),22,bag,987,38);
        var b=buffer(h);
        try {
            GearProtocol.State.CODEC.encode(b,input); var decoded=GearProtocol.State.CODEC.decode(b);
            h.assertTrue(ItemStack.matches(decoded.bag(),bag),"bag components survive wire codec");
            h.assertTrue(decoded.openedAt()==987,"persistent menu pose survives snapshot for late trackers");
            h.assertTrue(decoded.openSource()==38,"held and worn menu poses remain distinct");
            input.bag().shrink(1); bag.shrink(1);
            h.assertTrue(!input.bag().isEmpty() && !decoded.bag().isEmpty(),"caller cannot mutate snapshot");
            h.assertTrue(BagContents.copy(decoded.bag()).get(36).is(Items.TRIDENT),"mounted gear retained");
        } finally { b.release(); }
        h.succeed();
    }
    @GameTest(template="empty") public void actionTimelineDoesNotDependOnAnimationLibrary(GameTestHelper h) {
        for (GearAction kind : GearAction.values()) {
            ItemStack before=new ItemStack(Items.DIAMOND_PICKAXE), after=new ItemStack(Items.DIAMOND_SWORD);
            var input=new GearProtocol.Action(14,UUID.randomUUID(),ResourceLocation.withDefaultNamespace("the_nether"),31,UUID.randomUUID(),kind,1,1000,before,after);
            var b=buffer(h);
            try {
                GearProtocol.Action.CODEC.encode(b,input); var decoded=GearProtocol.Action.CODEC.decode(b);
                h.assertValueEqual(decoded.kind(),kind,"semantic action retained");
                h.assertTrue(decoded.startedAt()==1000 && decoded.sequence()==31,"server timing and ordering preserved");
                h.assertTrue(decoded.playerId().equals(input.playerId()) && decoded.bag().equals(input.bag()),"actor/bag identities retained");
                before.shrink(1); after.shrink(1); decoded.before().shrink(1);
                h.assertTrue(input.before().is(Items.DIAMOND_PICKAXE) && decoded.after().is(Items.DIAMOND_SWORD),"visual item snapshots own copies");
            } finally { b.release(); }
        }
        h.succeed();
    }
}
