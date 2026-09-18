/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: Quick Slot present/absent; no bag and all tiers; every mount occupancy;
 * held empty/nonempty; immutable output and invalid source indices/occupancy. */
class GearChoicesTest {
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
