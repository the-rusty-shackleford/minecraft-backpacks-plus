/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: compatible/incompatible hand, occupied/empty mount, free/full storage;
 * current/stale identity, selection and revision; dead/spectator/busy. JDK domain only. */
class SwapRulesTest {
    @Test void compatibleHandUsesMountRegardlessOfCapacity() {
        for (boolean empty : new boolean[]{true, false}) for (boolean storage : new boolean[]{true, false})
            assertEquals(SwapRules.Destination.MOUNT, SwapRules.destination(empty, true, storage));
    }
    @Test void drawingGearCanStowInStorageButCannotPartiallyExchange() {
        assertEquals(SwapRules.Destination.STORAGE, SwapRules.destination(false, false, true));
        assertEquals(SwapRules.Destination.REFUSE, SwapRules.destination(false, false, false));
    }
    @Test void emptyMountCannotBeUsedAsGenericDepositShortcut() {
        assertEquals(SwapRules.Destination.REFUSE, SwapRules.destination(true, false, true));
    }
    @Test void eachAuthorityPreconditionIsRequired() {
        assertTrue(SwapRules.current(true, false, false, true, 0, 0, 0, 0));
        assertTrue(SwapRules.current(true, false, false, true, 8, 8, 99, 99));
        assertFalse(SwapRules.current(false, false, false, true, 0, 0, 0, 0));
        assertFalse(SwapRules.current(true, true, false, true, 0, 0, 0, 0));
        assertFalse(SwapRules.current(true, false, true, true, 0, 0, 0, 0));
        assertFalse(SwapRules.current(true, false, false, false, 0, 0, 0, 0));
        assertFalse(SwapRules.current(true, false, false, true, -1, -1, 0, 0));
        assertFalse(SwapRules.current(true, false, false, true, 9, 9, 0, 0));
        assertFalse(SwapRules.current(true, false, false, true, 1, 0, 0, 0));
        assertFalse(SwapRules.current(true, false, false, true, 0, 0, -1, -1));
        assertFalse(SwapRules.current(true, false, false, true, 0, 0, 8, 9));
    }
}
