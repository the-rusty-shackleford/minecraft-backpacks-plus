/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: both adjacent upgrades; every storage/mount cell; same/skipped/reversed
 * tier; below/above bounds; category order when the new long mount shifts small mounts. */
class UpgradeLayoutTest {
    @Test void allExistingCellsRemainDistinctAndInTheirCategory() {
        for (int stage=0;stage<2;stage++) {
            var from=BackpackTier.values()[stage];var to=BackpackTier.values()[stage+1];
            var seen=new HashSet<Integer>();
            for (int cell=0;cell<from.totalSlots();cell++) {
                int dest=UpgradeLayout.destination(from,to,cell);
                assertTrue(seen.add(dest));assertTrue(dest<to.totalSlots());
                if (cell<from.storageSlots()) assertEquals(cell,dest);
                else assertEquals(from.mounts().get(cell-from.storageSlots()),to.mounts().get(dest-to.storageSlots()));
            }
            assertEquals(from.totalSlots(),seen.size());
        }
    }
    @Test void smallMountMovesPastNewLongMount() {
        assertEquals(18,UpgradeLayout.destination(BackpackTier.BASIC,BackpackTier.REINFORCED,9));
        assertEquals(20,UpgradeLayout.destination(BackpackTier.BASIC,BackpackTier.REINFORCED,10));
        assertEquals(36,UpgradeLayout.destination(BackpackTier.REINFORCED,BackpackTier.EXPEDITION,18));
        assertEquals(37,UpgradeLayout.destination(BackpackTier.REINFORCED,BackpackTier.EXPEDITION,19));
        assertEquals(38,UpgradeLayout.destination(BackpackTier.REINFORCED,BackpackTier.EXPEDITION,20));
    }
    @Test void nonAdjacentAndInvalidCellsReject() {
        for (var from:BackpackTier.values()) for(var to:BackpackTier.values())
            if(to.ordinal()!=from.ordinal()+1) assertThrows(IllegalArgumentException.class,()->UpgradeLayout.destination(from,to,0));
        assertThrows(IndexOutOfBoundsException.class,()->UpgradeLayout.destination(BackpackTier.BASIC,BackpackTier.REINFORCED,-1));
        assertThrows(IndexOutOfBoundsException.class,()->UpgradeLayout.destination(BackpackTier.BASIC,BackpackTier.REINFORCED,11));
    }
}
