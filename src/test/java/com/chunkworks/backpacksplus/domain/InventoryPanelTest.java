/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Partitions. Bag: none; worn. Width: the minimum (320) and just under the panel's (359);
 * exactly the panel's (360); vanilla's own narrow band with the panel (370); a friend's 1280
 * wide at GUI scale 3 (427), where vanilla alone stands the book beside the screen; just under
 * the width panel, tabs, book and screen need (588); exactly it (589); 720p at scale 2 (640);
 * Rusty's ultrawide at scale 5 (688). Book: closed; open. Invariants: the widened layout
 * centers panel and screen together; the open book keeps vanilla's distance from the screen and
 * its tabs clear the panel; a bad width is refused; the record refuses an inconsistent placement.
 */
class InventoryPanelTest {
    /** Vanilla's screen position for a width (InventoryScreen.init through RecipeBookComponent.updateScreenPosition). */
    private static int vanillaLeft(int width, boolean bookBeside) { return bookBeside ? 177 + (width - 376) / 2 : (width - 176) / 2; }
    /** Vanilla's book position for a width when the book stands beside the screen. */
    private static int vanillaBook(int width) { return (width - 147) / 2 - 86; }

    @Test void noBagMeansNoPanelAtAnyWidthAndVanillaIsUntouched() {
        for (int width : new int[] { 320, 360, 427, 589, 640, 1000 }) for (boolean open : new boolean[] { false, true }) {
            var panel = InventoryPanel.of(width, false, open);
            assertSame(InventoryPanel.NONE, panel);
            assertEquals(0, panel.widen());
            assertEquals(width < 379, panel.overlays(width), "vanilla's own threshold at " + width);
        }
    }

    @Test void belowThePanelsMinimumThePanelHides() {
        for (int width : new int[] { 320, 359 }) for (boolean open : new boolean[] { false, true }) {
            var panel = InventoryPanel.of(width, true, open);
            assertSame(InventoryPanel.NONE, panel, "hidden at " + width);
        }
    }

    @Test void fromTheMinimumUpTheClosedBookLeavesThePanelShownAndCentered() {
        for (int width : new int[] { 360, 370, 427, 588 }) {
            var closed = InventoryPanel.of(width, true, false);
            assertTrue(closed.shown(), "shown at " + width); assertFalse(closed.beside(), "no room beside at " + width);
            assertEquals(-180, closed.panelX()); assertEquals(180, closed.widen());
            assertTrue(closed.overlays(width), "opening the book here would not stand it beside the panel");
        }
        // At the minimum, panel and screen each keep the least margin from the edge.
        int left = vanillaLeft(360 + 180, false);
        assertEquals(182, left); assertEquals(2, left - 180); assertEquals(2, 360 - (left + 176));
        // At the friend's width both margins are a hand's breadth.
        left = vanillaLeft(427 + 180, false);
        assertEquals(215, left); assertEquals(35, left - 180); assertEquals(36, 427 - (left + 176));
    }

    @Test void whereTheOpenBookCannotStandBesideThePanelTheBookTakesTheRoom() {
        // Vanilla alone stands the book beside the screen at 427 and overlays it at 370; either way
        // the panel yields and vanilla's own layout applies, so the book's button stays reachable.
        for (int width : new int[] { 360, 370, 427, 588 }) {
            var open = InventoryPanel.of(width, true, true);
            assertSame(InventoryPanel.NONE, open, "the panel yields at " + width);
            assertEquals(0, open.widen());
            assertEquals(width < 379, open.overlays(width), "vanilla's own rule at " + width);
        }
    }

    @Test void fromTheBesideWidthUpTheOpenBookStandsBetweenPanelAndScreen() {
        for (int width : new int[] { 589, 640, 688 }) {
            var closed = InventoryPanel.of(width, true, false);
            var open = InventoryPanel.of(width, true, true);
            assertTrue(closed.shown() && closed.beside(), "closed at " + width);
            assertTrue(open.shown() && open.beside(), "open at " + width);
            assertEquals(-180, closed.panelX(), "closed at " + width);
            assertEquals(-359, open.panelX(), "open at " + width);
            assertFalse(closed.overlays(width)); assertFalse(open.overlays(width));
            assertEquals(180, open.widen());
            // The book stands where vanilla stands it for the widened screen, one or two pixels from
            // it by vanilla's own rounding; its tabs, thirty pixels to its left, clear the panel by the gap.
            int left = vanillaLeft(width + 180, true), book = vanillaBook(width + 180);
            assertTrue(left - book == 148 || left - book == 149, "book to screen at " + width + ": " + (left - book));
            int gap = (book - 30) - (left + open.panelX() + 176);
            assertTrue(gap == 4 || gap == 5, "panel to tabs at " + width + ": " + gap);
            assertTrue(left + open.panelX() >= 0, "the panel is on the screen at " + width);
        }
    }

    @Test void theWidenedLayoutCentersPanelAndScreenTogether() {
        for (int width : new int[] { 360, 427, 640, 688, 1000 }) {
            var closed = InventoryPanel.of(width, true, false);
            int left = vanillaLeft(width + closed.widen(), false);
            int leftMargin = left + closed.panelX(), rightMargin = width - (left + 176);
            assertTrue(Math.abs(leftMargin - rightMargin) <= 1, "closed at " + width + ": " + leftMargin + " vs " + rightMargin);
        }
        for (int width : new int[] { 589, 640, 688, 1000 }) {
            // With the book open the whole row leans left by the tab strip, since the layout is widened by the panel alone.
            var open = InventoryPanel.of(width, true, true);
            int left = vanillaLeft(width + open.widen(), true);
            int leftMargin = left + open.panelX(), rightMargin = width - (left + 176);
            assertTrue(rightMargin - leftMargin >= 20 && rightMargin - leftMargin <= 36, "open at " + width + ": " + leftMargin + " vs " + rightMargin);
        }
    }

    @Test void badInputIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> InventoryPanel.of(0, true, false));
        assertThrows(IllegalArgumentException.class, () -> new InventoryPanel(true, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new InventoryPanel(true, false, -359));
        assertThrows(IllegalArgumentException.class, () -> new InventoryPanel(false, true, 0));
        assertThrows(IllegalArgumentException.class, () -> new InventoryPanel(false, false, -180));
        assertDoesNotThrow(() -> new InventoryPanel(true, true, -359));
        assertDoesNotThrow(() -> new InventoryPanel(true, true, -180));
    }
}
