/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.chunkworks.backpacksplus.domain.GearGesture.Step.*;

/** Partitions: a hold over several ticks, released with or without the wheel moving; a press and
 * release inside one tick; a press with nothing to browse; a frozen selection that breaks while
 * held, and the key kept down after; the wheel opening the gesture before a tick sees the key;
 * a reset while held; a press the tick sees directly against one it only learns of. */
class GearGestureTest {
    @Test void releasingCommitsTheHighlightWhetherOrNotTheWheelMoved() {
        var gesture=new GearGesture();
        assertEquals(BEGIN,gesture.tick(true,true,true,true));
        assertTrue(gesture.browsing());
        assertEquals(NONE,gesture.tick(true,false,true,true));
        assertEquals(NONE,gesture.tick(true,false,true,true));
        assertEquals(COMMIT,gesture.tick(false,false,true,true));
        assertFalse(gesture.browsing());
        assertEquals(NONE,gesture.tick(false,false,true,true));
    }
    @Test void aPressAndReleaseInsideOneTickIsATap() {
        var gesture=new GearGesture();
        assertEquals(TAP,gesture.tick(false,true,true,true));
        assertFalse(gesture.browsing());
        assertEquals(NONE,gesture.tick(false,false,true,true));
    }
    @Test void aPressWithNothingToBrowseOpensNothingAndDoesNotLinger() {
        var gesture=new GearGesture();
        assertEquals(NONE,gesture.tick(true,true,false,true));
        assertFalse(gesture.browsing());
        assertEquals(NONE,gesture.tick(true,false,true,true));   // still held: a bag appearing does not open it
        assertEquals(NONE,gesture.tick(false,false,true,true));
        assertEquals(BEGIN,gesture.tick(true,true,true,true));
    }
    @Test void aBrokenSelectionCancelsAndTheHeldKeyOpensNothingUntilReleased() {
        var gesture=new GearGesture();
        assertEquals(BEGIN,gesture.tick(true,true,true,true));
        assertEquals(CANCEL,gesture.tick(true,false,true,false));
        assertFalse(gesture.browsing());
        assertEquals(NONE,gesture.tick(true,false,true,true));
        assertEquals(NONE,gesture.tick(false,false,true,true));   // the release commits nothing
        assertEquals(BEGIN,gesture.tick(true,true,true,true));
    }
    @Test void theWheelOpensTheGestureBeforeATickSeesTheKey() {
        var gesture=new GearGesture();
        assertFalse(gesture.scrolled(false,true));
        assertFalse(gesture.scrolled(true,false));
        assertTrue(gesture.scrolled(true,true));
        assertTrue(gesture.browsing());
        assertFalse(gesture.scrolled(true,true));
        assertEquals(NONE,gesture.tick(true,true,true,true));    // the press the tick now sees does not reopen
        assertEquals(COMMIT,gesture.tick(false,false,true,true));
    }
    @Test void aResetDropsTheGestureAndAKeyStillDownOpensNothing() {
        var gesture=new GearGesture();
        assertEquals(BEGIN,gesture.tick(true,true,true,true));
        gesture.reset(true);
        assertFalse(gesture.browsing());
        assertEquals(NONE,gesture.tick(true,false,true,true));
        assertFalse(gesture.scrolled(true,true));
        assertEquals(NONE,gesture.tick(false,false,true,true));
        assertEquals(BEGIN,gesture.tick(true,true,true,true));
        gesture.reset(false);
        assertEquals(TAP,gesture.tick(false,true,true,true));
    }
}
