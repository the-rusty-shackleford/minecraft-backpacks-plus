/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: supported explicit wire IDs and out-of-protocol values. */
class GearActionTest {
    @Test void actionsHaveStableDistinctIds() {
        assertEquals(GearAction.DRAW, GearAction.decode(1));
        assertEquals(GearAction.STOW, GearAction.decode(2));
        assertEquals(GearAction.EXCHANGE, GearAction.decode(3));
        assertEquals(GearAction.OPEN, GearAction.decode(4));
        assertEquals(GearAction.RETRIEVE, GearAction.decode(5));
        assertEquals(GearAction.CLOSE, GearAction.decode(6));
    }
    @Test void unknownActionsDoNotSilentlyBecomeAnotherAction() {
        for (int id : new int[]{-1, 0, 7, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> GearAction.decode(id));
    }
}
