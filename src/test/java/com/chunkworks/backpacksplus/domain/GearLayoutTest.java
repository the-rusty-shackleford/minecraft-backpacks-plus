/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Partitions. Width: a wide screen (everything at the usual pitch); the widths a friend's
 * four-mount bag went compact at, 1280 wide at GUI scale 3 (427) and 1920 at scale 4 with the
 * attack indicator on the hotbar (480), where the row closes up instead; the minimum (320),
 * where nothing fits beside the hotbar and the row lifts. Mounts: one to four. Held cell: none;
 * at the row's end when it fits; above the quick slot's column when it does not; in the lifted
 * row. Direction: right- and left-handed mirror. The mounts never move when the held cell
 * appears. Invariants: every mount cell on screen; pitch and held-cell coordinates well formed.
 */
class GearLayoutTest {
    private static final int CROSSHAIR = 97, HOTBAR = 121;

    private static int base(int width, int distance, int direction) { return width/2 + (direction > 0 ? distance : -distance - 22); }

    @Test void aWideScreenKeepsTheUsualPitchWithTheHeldCellAtTheRowsEnd() {
        int base = base(960, CROSSHAIR, 1);
        var idle = GearLayout.of(960, 540, base, 1, 4, false, 39);
        assertEquals(24, idle.pitch()); assertEquals(base + 24, idle.first()); assertEquals(518, idle.rowY());
        assertFalse(idle.lifted()); assertEquals(-1, idle.heldX()); assertEquals(-1, idle.heldY()); assertFalse(idle.heldAbove());
        var browsing = GearLayout.of(960, 540, base, 1, 4, true, 39);
        assertEquals(base + 24 + 4 * 24, browsing.heldX()); assertEquals(518, browsing.heldY()); assertFalse(browsing.heldAbove());
        assertEquals(idle.first(), browsing.first()); assertEquals(idle.pitch(), browsing.pitch());
    }

    @Test void theFriendsFourMountsCloseUpBesideTheHotbarInsteadOfLifting() {
        // 1280 wide at GUI scale 3 with the crosshair indicator: four mounts at 24 overflow by 6 pixels.
        int base = base(427, CROSSHAIR, 1);
        var four = GearLayout.of(427, 240, base, 1, 4, false, 39);
        assertFalse(four.lifted()); assertEquals(22, four.pitch()); assertEquals(base + 24, four.first()); assertEquals(218, four.rowY());
        assertTrue(four.mountX(1, 3) + 22 <= 427 - 3, "the last mount stays on screen");
        // With an item in hand, the held cell no longer fits in the row: it goes above the quick slot's column, the mounts unmoved.
        var held = GearLayout.of(427, 240, base, 1, 4, true, 39);
        assertEquals(four.first(), held.first()); assertEquals(four.pitch(), held.pitch());
        assertEquals(base, held.heldX()); assertEquals(218 - 34, held.heldY()); assertTrue(held.heldAbove());
        // 1920 wide at GUI scale 4 with the attack indicator on the hotbar: over by four pixels at 24, fits at 22.
        int wide = base(480, HOTBAR, 1);
        var four480 = GearLayout.of(480, 270, wide, 1, 4, false, 39);
        assertFalse(four480.lifted()); assertEquals(22, four480.pitch());
        // Three mounts always fit at the usual pitch at these widths.
        assertEquals(24, GearLayout.of(427, 240, base, 1, 3, false, 39).pitch());
        assertEquals(24, GearLayout.of(480, 270, wide, 1, 3, false, 39).pitch());
    }

    @Test void theRowSharesBordersBeforeItLifts() {
        // Four mounts need first + 3*pitch + 22 <= width - 3. A width one pixel short for 22 gives 20; one short for 20 lifts.
        int base = 300, first = base + 24;
        var layout = GearLayout.of(first + 3 * 22 + 22 + 3 - 1, 240, base, 1, 4, false, 39);
        assertEquals(20, layout.pitch()); assertFalse(layout.lifted());
        assertEquals(20, GearLayout.of(first + 3 * 20 + 22 + 3, 240, base, 1, 4, false, 39).pitch(), "exactly enough for 20");
        var short20 = GearLayout.of(first + 3 * 20 + 22 + 3 - 1, 240, base, 1, 4, false, 39);
        assertTrue(short20.lifted(), "a pixel short even at 20: the row lifts");
    }

    @Test void theMinimumScreenLiftsTheRowAboveTheStatusIconsWithItsOuterEndAtTheQuickSlot() {
        int base = base(320, CROSSHAIR, 1);   // 257: no mount fits beside it
        var two = GearLayout.of(320, 240, base, 1, 2, false, 39);
        assertTrue(two.lifted()); assertEquals(24, two.pitch());
        assertEquals(240 - 82, two.rowY(), "above the status icons: at least 82 up");
        assertEquals(base - 24, two.first(), "the outer end at the quick slot");
        assertEquals(base, two.mountX(1, 1));
        var tall = GearLayout.of(320, 240, base, 1, 2, false, 70);
        assertEquals(240 - 96, tall.rowY(), "taller status icons push it higher");
        // With the held cell the whole row shifts inward so the held cell stays on screen.
        var held = GearLayout.of(320, 240, base, 1, 4, true, 39);
        assertTrue(held.lifted());
        assertEquals(held.rowY(), held.heldY()); assertEquals(held.first() + 4 * 24, held.heldX());
        assertTrue(held.heldX() + 22 <= 320 - 3); assertTrue(held.first() >= 3);
    }

    @Test void aLeftHandedPlayerGetsTheMirrorImage() {
        int width = 428;   // even, so the HUD's own base formula mirrors exactly; four mounts close up to 22 here
        int rightBase = base(width, CROSSHAIR, 1), leftBase = base(width, CROSSHAIR, -1);
        var right = GearLayout.of(width, 240, rightBase, 1, 4, true, 39);
        var left = GearLayout.of(width, 240, leftBase, -1, 4, true, 39);
        assertEquals(22, right.pitch()); assertEquals(right.pitch(), left.pitch()); assertEquals(right.lifted(), left.lifted());
        assertTrue(right.heldAbove()); assertTrue(left.heldAbove());
        for (int i = 0; i < 4; i++) assertEquals(width - (right.mountX(1, i) + 22), left.mountX(-1, i), "mount " + i + " mirrored");
        assertEquals(width - (right.heldX() + 22), left.heldX()); assertEquals(right.heldY(), left.heldY());
        assertTrue(left.mountX(-1, 3) >= 3, "the last mount stays on screen on the left");
    }

    @Test void everyMountCellIsOnScreenAtEveryWidthAndCount() {
        for (int width = 320; width <= 1000; width += 7) for (int mounts = 1; mounts <= 4; mounts++) for (boolean held : new boolean[] {false, true})
            for (int direction : new int[] {1, -1}) for (int distance : new int[] {CROSSHAIR, HOTBAR}) {
                var layout = GearLayout.of(width, 240, base(width, distance, direction), direction, mounts, held, 39);
                for (int i = 0; i < mounts; i++) {
                    int x = layout.mountX(direction, i);
                    assertTrue(x >= 3 && x + 22 <= width - 3, "mount " + i + " at width " + width + " x=" + x);
                }
                if (held) assertTrue(layout.heldX() >= 3 && layout.heldX() + 22 <= width - 3, "held at width " + width);
                else assertEquals(-1, layout.heldX());
                if (!layout.lifted()) assertEquals(240 - 22, layout.rowY());
            }
    }

    @Test void malformedLayoutsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GearLayout.of(960, 540, 577, 1, 0, false, 39));
        assertThrows(IllegalArgumentException.class, () -> GearLayout.of(960, 540, 577, 1, 5, false, 39));
        assertThrows(IllegalArgumentException.class, () -> GearLayout.of(960, 540, 577, 0, 2, false, 39));
        assertThrows(IllegalArgumentException.class, () -> new GearLayout(23, 0, 0, false, -1, -1));
        assertThrows(IllegalArgumentException.class, () -> new GearLayout(24, 0, 0, false, 5, -1));
    }
}
