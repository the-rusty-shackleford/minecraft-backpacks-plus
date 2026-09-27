/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chunkworks.backpacksplus.domain.PickupPlan.Held;
import com.chunkworks.backpacksplus.domain.PickupPlan.Pass;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Partitions: pass (top up, fill empty); count (0, less than the room, exactly the room, more);
 * cells (none, same kind partial or full, other kind, empty, mixed in any order); limit 1 and 64;
 * bad arguments. */
final class PickupPlanTest {
    private static final List<Held> MIXED = List.of(Held.EMPTY, Held.same(60), Held.other(3), Held.same(10), Held.EMPTY);

    @Test void topUpFillsTheSameKindInOrderAndNothingElse() {
        assertArrayEquals(new int[] {0, 4, 0, 16, 0}, PickupPlan.place(MIXED, 20, 64, Pass.TOP_UP));
        assertArrayEquals(new int[] {0, 4, 0, 54, 0}, PickupPlan.place(MIXED, 58, 64, Pass.TOP_UP), "exactly the room");
        assertArrayEquals(new int[] {0, 4, 0, 54, 0}, PickupPlan.place(MIXED, 90, 64, Pass.TOP_UP), "more than the room: the rest stays");
        assertArrayEquals(new int[] {0, 3, 0, 0, 0}, PickupPlan.place(MIXED, 3, 64, Pass.TOP_UP), "less than the first cell's room");
    }
    @Test void fillEmptyUsesOnlyEmptyCellsUpToTheLimit() {
        assertArrayEquals(new int[] {20, 0, 0, 0, 0}, PickupPlan.place(MIXED, 20, 64, Pass.FILL_EMPTY));
        assertArrayEquals(new int[] {64, 0, 0, 0, 36}, PickupPlan.place(MIXED, 100, 64, Pass.FILL_EMPTY));
        assertArrayEquals(new int[] {64, 0, 0, 0, 64}, PickupPlan.place(MIXED, 200, 64, Pass.FILL_EMPTY));
        assertArrayEquals(new int[] {1, 0, 0, 0, 1}, PickupPlan.place(MIXED, 5, 1, Pass.FILL_EMPTY), "unstackable: one a cell");
    }
    @Test void aFullSameStackAndNoCellsTakeNothing() {
        assertArrayEquals(new int[] {0, 0}, PickupPlan.place(List.of(Held.same(64), Held.other(64)), 10, 64, Pass.TOP_UP));
        assertArrayEquals(new int[] {0, 0}, PickupPlan.place(List.of(Held.same(64), Held.other(64)), 10, 64, Pass.FILL_EMPTY));
        assertArrayEquals(new int[] {}, PickupPlan.place(List.of(), 10, 64, Pass.TOP_UP));
        assertArrayEquals(new int[] {0, 0, 0, 0, 0}, PickupPlan.place(MIXED, 0, 64, Pass.FILL_EMPTY));
    }
    @Test void aCellOverTheLimitIsNotShrunk() {
        assertArrayEquals(new int[] {0, 5}, PickupPlan.place(List.of(Held.same(99), Held.same(11)), 5, 16, Pass.TOP_UP));
    }
    @Test void badArgumentsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> PickupPlan.place(MIXED, -1, 64, Pass.TOP_UP));
        assertThrows(IllegalArgumentException.class, () -> PickupPlan.place(MIXED, 1, 0, Pass.TOP_UP));
        assertThrows(IllegalArgumentException.class, () -> new Held(true, false, 3));
        assertThrows(IllegalArgumentException.class, () -> new Held(true, true, 0));
        assertThrows(IllegalArgumentException.class, () -> Held.same(-1));
    }
}
