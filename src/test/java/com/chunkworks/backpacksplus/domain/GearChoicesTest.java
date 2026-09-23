/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: Quick Slot present/absent; no bag and all tiers; every mount occupancy;
 * held empty/nonempty; immutable output and invalid source indices/occupancy. The default
 * highlight: the first mount with and without Quick Slot ahead of it, every occupancy, the
 * quick slot alone, and nothing to choose from. */
class GearChoicesTest {
    @Test void theHighlightStartsOnTheFirstMountAndNeverOnADeposit() {
        assertEquals(1,GearChoices.defaultIndex(GearChoices.build(true,3,7,true)));   // quick, mount 0, stow 0, ...: the first mount
        assertEquals(0,GearChoices.defaultIndex(GearChoices.build(false,3,7,true)));  // no quick slot: mount 0 is first
        for (int occupied=0;occupied<8;occupied++) assertEquals(1,GearChoices.defaultIndex(GearChoices.build(true,3,occupied,true)));
        assertEquals(0,GearChoices.defaultIndex(GearChoices.build(false,2,0,false)));
        assertEquals(0,GearChoices.defaultIndex(GearChoices.build(true,0,0,true)));   // no mounts at all: the quick slot
        assertThrows(IllegalArgumentException.class,()->GearChoices.defaultIndex(GearChoices.build(false,0,0,true)));
    }
    @Test void storageActionsFollowOnlyOccupiedMounts() {
        for (boolean quick:new boolean[]{false,true}) for (int mounts:new int[]{0,2,3,4})
            for (int occupied=0;occupied<(1<<mounts);occupied++) for (boolean held:new boolean[]{false,true}) {
                var choices=GearChoices.build(quick,mounts,occupied,held);
                int next=0;
                if (quick) assertEquals(new GearChoices.Choice(GearChoices.Kind.QUICK,-1),choices.get(next++));
                for (int i=0;i<mounts;i++) {
                    assertEquals(new GearChoices.Choice(GearChoices.Kind.MOUNT,i),choices.get(next++));
                    if ((occupied&(1<<i))!=0)
                        assertEquals(new GearChoices.Choice(GearChoices.Kind.STOW_MOUNT,i),choices.get(next++));
                }
                if (mounts>0 && held)
                    assertEquals(new GearChoices.Choice(GearChoices.Kind.STOW_HELD,-1),choices.get(next++));
                assertEquals(next,choices.size());
            }
    }
    @Test void gestureChoicesCannotChangeUnderTheWheel() {
        var choices=GearChoices.build(true,4,15,true);
        assertThrows(UnsupportedOperationException.class,choices::clear);
        assertEquals(10,choices.size());
    }
    @Test void malformedMountDescriptionsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(true,-1,0,false));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(true,5,0,false));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(true,2,4,false));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(true,2,-1,false));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(GearChoices.Kind.MOUNT,-1));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(GearChoices.Kind.STOW_MOUNT,4));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(GearChoices.Kind.STOW_HELD,0));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(null,-1));
    }
}
