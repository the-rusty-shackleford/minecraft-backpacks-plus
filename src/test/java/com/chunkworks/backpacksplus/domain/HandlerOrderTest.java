/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Partitions. The handler: absent; present. The viewer's own entries: none; one; two. Other mods'
 * entries: none; one ahead of the viewer's; one behind it. Registration order against a mod that
 * stands its handler first: that mod before the bag; the bag before that mod. Invariants: every
 * entry is kept, once; only the bag's handler moves.
 */
class HandlerOrderTest {
    private static final String BAG = "bag", EMI = "emi:crafting", EMI_2 = "emi:other", WAREHOUSE = "warehouse";
    private static boolean own(String handler) { return handler.startsWith("emi:"); }
    private static List<String> list(String... handlers) { return new ArrayList<>(List.of(handlers)); }

    @Test void anAbsentHandlerChangesNothing() {
        var handlers = list(EMI, WAREHOUSE);
        assertFalse(HandlerOrder.aheadOfOwn(handlers, BAG, HandlerOrderTest::own));
        assertEquals(List.of(EMI, WAREHOUSE), handlers);
    }

    @Test void appendedBehindTheViewersOwnItMovesAheadOfIt() {
        var handlers = list(EMI, BAG);
        assertTrue(HandlerOrder.aheadOfOwn(handlers, BAG, HandlerOrderTest::own));
        assertEquals(List.of(BAG, EMI), handlers);
    }

    @Test void anotherModsHandlerAheadOfTheViewersKeepsTheFront() {
        var handlers = list(WAREHOUSE, EMI, BAG);
        assertTrue(HandlerOrder.aheadOfOwn(handlers, BAG, HandlerOrderTest::own));
        assertEquals(List.of(WAREHOUSE, BAG, EMI), handlers);
    }

    @Test void itStandsAheadOfTheFirstOfSeveralOwnEntriesAndAheadOfAModBehindThem() {
        var handlers = list(EMI, EMI_2, WAREHOUSE, BAG);
        assertTrue(HandlerOrder.aheadOfOwn(handlers, BAG, HandlerOrderTest::own));
        assertEquals(List.of(BAG, EMI, EMI_2, WAREHOUSE), handlers);
    }

    @Test void withNoOwnEntryItGoesToTheEnd() {
        var handlers = list(BAG, WAREHOUSE);
        assertTrue(HandlerOrder.aheadOfOwn(handlers, BAG, HandlerOrderTest::own));
        assertEquals(List.of(WAREHOUSE, BAG), handlers);
    }

    @Test void alreadyInPlaceItStays() {
        var handlers = list(WAREHOUSE, BAG, EMI);
        assertTrue(HandlerOrder.aheadOfOwn(handlers, BAG, HandlerOrderTest::own));
        assertEquals(List.of(WAREHOUSE, BAG, EMI), handlers);
    }

    /** The warehouse registers by appending and moving itself to the front; the bag by appending and
     * this rule. The pack's EMI registered the bag's last in dev; either order must end the same. */
    @Test void theOrderIsTheSameWhicheverModRegistersFirst() {
        var warehouseFirst = list(EMI);
        warehouseFirst.add(0, WAREHOUSE);
        warehouseFirst.add(BAG);
        HandlerOrder.aheadOfOwn(warehouseFirst, BAG, HandlerOrderTest::own);

        var bagFirst = list(EMI);
        bagFirst.add(BAG);
        HandlerOrder.aheadOfOwn(bagFirst, BAG, HandlerOrderTest::own);
        bagFirst.add(0, WAREHOUSE);

        assertEquals(List.of(WAREHOUSE, BAG, EMI), warehouseFirst);
        assertEquals(warehouseFirst, bagFirst);
    }

    @Test void nullsAreRefused() {
        assertThrows(NullPointerException.class, () -> HandlerOrder.aheadOfOwn(list(EMI), null, HandlerOrderTest::own));
        assertThrows(NullPointerException.class, () -> HandlerOrder.aheadOfOwn(list(EMI, BAG), BAG, null));
    }
}
