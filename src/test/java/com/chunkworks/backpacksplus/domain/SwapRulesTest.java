/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: current/stale identity, selection and revision; dead/spectator/busy.
 * JDK authorization only; actual stack transfer is covered by real-server GameTests. */
class SwapRulesTest {
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
