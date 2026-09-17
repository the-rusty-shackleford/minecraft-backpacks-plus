/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Partitions: draw/stow/exchange, storage fallback/partial capacity, stale identity/revision,
 * selected slot changes, open menu, invalid mount, empty source, creative and survival. */
@GameTestHolder("backpacksplus")
@PrefixGameTestTemplate(false)
public final class MountGameTests {
    public MountGameTests() {}
    private static Player player(GameTestHelper h, GameType mode) {
        Player p=h.makeMockPlayer(mode); p.getInventory().setItem(38, new ItemStack(BackpackItems.BASIC.get()));
        BagContents.identify(p.getInventory().getItem(38)); return p;
    }
    private static ItemStack bag(Player p) { return p.getInventory().getItem(38); }
    private static boolean swap(Player p, int mount) {
        return MountExchange.swap(p, 38, bag(p).get(BackpackItems.ID), BagContents.revision(bag(p)), mount, p.getInventory().selected);
    }
    @GameTest(template="empty") public void drawStowAndExchangeConserveItems(GameTestHelper h) {
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            Player p=player(h, mode); BagInventory.bind(p, 38).setItem(9, new ItemStack(Items.DIAMOND_SWORD));
            h.assertTrue(swap(p, 0), "draw succeeds");
            h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD) && BagContents.copy(bag(p)).get(9).isEmpty(), "draw transfers ownership");
            h.assertTrue(swap(p, 0), "stow succeeds");
            h.assertTrue(p.getMainHandItem().isEmpty(), "stow clears hand");
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
            h.assertTrue(swap(p, 0), "exchange succeeds");
            h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD) && BagContents.copy(bag(p)).get(9).is(Items.DIAMOND_PICKAXE), "both stacks exchanged exactly once");
        }
        h.succeed();
    }
    @GameTest(template="empty") public void incompatibleHandUsesOrdinaryStorage(GameTestHelper h) {
        Player p=player(h, GameType.SURVIVAL); var inv=BagInventory.bind(p, 38);
        inv.setItem(9, new ItemStack(Items.TRIDENT)); inv.setItem(0, new ItemStack(Items.APPLE, 60));
        p.getInventory().setItem(0, new ItemStack(Items.APPLE, 16));
        h.assertTrue(swap(p, 0), "whole old hand fits by merging and a free cell");
        var cells=BagContents.copy(bag(p));
        h.assertValueEqual(cells.get(0).getCount(), 64, "existing stack filled");
        h.assertValueEqual(cells.get(1).getCount(), 12, "remaining apples stored");
        h.assertTrue(cells.get(9).isEmpty() && p.getMainHandItem().is(Items.TRIDENT), "draw completed"); h.succeed();
    }
    @GameTest(template="empty") public void partialRoomRefusesEntireExchange(GameTestHelper h) {
        Player p=player(h, GameType.SURVIVAL); var inv=BagInventory.bind(p, 38);
        for (int i=0;i<9;i++) inv.setItem(i, new ItemStack(Items.APPLE, i==0 ? 60 : 64));
        inv.setItem(9, new ItemStack(Items.TRIDENT)); p.getInventory().setItem(0, new ItemStack(Items.APPLE, 16));
        ItemStack before=bag(p).copy();
        h.assertTrue(!swap(p, 0), "only four of sixteen fit, so refuse");
        h.assertTrue(ItemStack.matches(before, bag(p)), "no partial merge or revision change");
        h.assertValueEqual(p.getMainHandItem().getCount(), 16, "hand intact"); h.succeed();
    }
    @GameTest(template="empty") public void staleIntentCannotOperateDifferentBagOrSelection(GameTestHelper h) {
        Player p=player(h, GameType.SURVIVAL); var inv=BagInventory.bind(p, 38); inv.setItem(9, new ItemStack(Items.DIAMOND_SWORD));
        UUID id=bag(p).get(BackpackItems.ID); long revision=BagContents.revision(bag(p));
        h.assertTrue(!MountExchange.swap(p,38,UUID.randomUUID(),revision,0,0), "wrong bag refused");
        h.assertTrue(!MountExchange.swap(p,38,id,revision,0,8), "stale hotbar refused");
        h.assertTrue(!MountExchange.swap(p,38,id,revision,-1,0), "negative mount refused");
        h.assertTrue(!MountExchange.swap(p,38,id,revision,4,0), "absent mount refused");
        h.assertTrue(MountExchange.swap(p,38,id,revision,0,0), "current request succeeds");
        h.assertTrue(!MountExchange.swap(p,38,id,revision,0,0), "replayed request cannot undo transaction");
        h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD), "replay left hand untouched"); h.succeed();
    }
    @GameTest(template="empty") public void nestedOrEmptySourceFallbackCannotBypassAdmission(GameTestHelper h) {
        Player p=player(h, GameType.SURVIVAL); p.getInventory().setItem(0, new ItemStack(Items.APPLE, 8));
        h.assertTrue(!swap(p,0), "empty long mount is not an ordinary deposit shortcut");
        BagInventory.bind(p,38).setItem(9,new ItemStack(Items.DIAMOND_SWORD));
        p.getInventory().setItem(0,new ItemStack(Items.SHULKER_BOX));
        h.assertTrue(!swap(p,0), "portable container cannot enter through swap fallback");
        h.assertTrue(p.getMainHandItem().is(Items.SHULKER_BOX) && BagContents.copy(bag(p)).get(9).is(Items.DIAMOND_SWORD), "both items intact"); h.succeed();
    }
    @GameTest(template="empty") public void openMenuBlocksMountExchange(GameTestHelper h) {
        Player p=player(h, GameType.SURVIVAL); BagInventory.bind(p,38).setItem(9,new ItemStack(Items.DIAMOND_SWORD));
        p.containerMenu=new BackpackMenu(1,p.getInventory(),BagInventory.bind(p,38),BackpackTier.BASIC,38);
        h.assertTrue(!swap(p,0), "open inventory owns bag; remote mount swap refused"); h.succeed();
    }
}
