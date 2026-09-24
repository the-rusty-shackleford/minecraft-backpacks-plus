/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: every tier's mount count; every mount occupancy; held empty/nonempty; no
 * quick slot ever; immutable output and invalid source indices/occupancy; no mounts refused.
 * The default highlight: the first mount at every occupancy, and nothing to choose from. */
class GearChoicesTest {
    @Test void theHighlightStartsOnTheFirstMountAndNeverOnADeposit() {
        for (int occupied=0;occupied<8;occupied++) assertEquals(0,GearChoices.defaultIndex(GearChoices.build(3,occupied,true)));
        assertEquals(0,GearChoices.defaultIndex(GearChoices.build(2,0,false)));
        assertEquals(GearChoices.Kind.MOUNT,GearChoices.build(4,15,true).get(GearChoices.defaultIndex(GearChoices.build(4,15,true))).kind());
        assertThrows(IllegalArgumentException.class,()->GearChoices.defaultIndex(java.util.List.of()));
    }
    @Test void storageActionsFollowOnlyOccupiedMountsAndTheQuickSlotIsNeverOffered() {
        for (int mounts:new int[]{1,2,3,4})
            for (int occupied=0;occupied<(1<<mounts);occupied++) for (boolean held:new boolean[]{false,true}) {
                var choices=GearChoices.build(mounts,occupied,held);
                int next=0;
                for (int i=0;i<mounts;i++) {
                    assertEquals(new GearChoices.Choice(GearChoices.Kind.MOUNT,i),choices.get(next++));
                    if ((occupied&(1<<i))!=0)
                        assertEquals(new GearChoices.Choice(GearChoices.Kind.STOW_MOUNT,i),choices.get(next++));
                }
                if (held) assertEquals(new GearChoices.Choice(GearChoices.Kind.STOW_HELD,-1),choices.get(next++));
                assertEquals(next,choices.size());
            }
    }
    @Test void withoutABagThereIsNothingToChooseFrom() {
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(0,0,true));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(0,0,false));
    }
    @Test void gestureChoicesCannotChangeUnderTheWheel() {
        var choices=GearChoices.build(4,15,true);
        assertThrows(UnsupportedOperationException.class,choices::clear);
        assertEquals(9,choices.size());
    }
    @Test void malformedMountDescriptionsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(-1,0,false));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(5,0,false));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(2,4,false));
        assertThrows(IllegalArgumentException.class,()->GearChoices.build(2,-1,false));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(GearChoices.Kind.MOUNT,-1));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(GearChoices.Kind.STOW_MOUNT,4));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(GearChoices.Kind.STOW_HELD,0));
        assertThrows(IllegalArgumentException.class,()->new GearChoices.Choice(null,-1));
    }
}
