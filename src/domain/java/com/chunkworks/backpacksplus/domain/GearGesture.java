/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/**
 * The G gesture over client ticks: press to open the gear bar, release to commit the
 * highlighted choice, whether or not the wheel moved.
 * AF: (browsing, held) = whether a gesture is open, and whether the key was down at the last tick.
 * RI: browsing is set by a press or a scroll while the key is down and cleared by exactly one
 * of commit, cancel or reset; a key held through a cancel or a reset opens nothing until released.
 * Mutable; one per client. Free of the game so the rule can be pinned by tests.
 */
public final class GearGesture {
    /** What the client does this tick. */
    public enum Step {
        /** Nothing. */
        NONE,
        /** Freeze the choices and highlight the default; the key is still down. */
        BEGIN,
        /** Freeze the choices and commit the default at once: pressed and released inside one tick. */
        TAP,
        /** Commit the highlighted choice: the key came up. */
        COMMIT,
        /** Drop the gesture: what it was frozen on changed. */
        CANCEL
    }

    private boolean browsing, held;

    /**
     * effects: advances one tick and returns what to do.
     * @param down      the key is down now
     * @param pressed   the key was pressed since the last tick, so a press and release between two
     *                  ticks still counts
     * @param available there is something to browse (a bag's mounts or a quick slot)
     * @param valid     what the open gesture was frozen on still holds; read only while browsing
     */
    public Step tick(boolean down, boolean pressed, boolean available, boolean valid) {
        Step step = Step.NONE;
        if (browsing) {
            if (!valid) { browsing = false; step = Step.CANCEL; }
            else if (!down) { browsing = false; step = Step.COMMIT; }
        } else if (!held && (down || pressed) && available) {
            browsing = down;
            step = down ? Step.BEGIN : Step.TAP;
        }
        held = down;
        return step;
    }

    /**
     * effects: a wheel movement before the first tick saw the key: opens the gesture if the key
     * is down, nothing is open, the key was not already held, and there is something to browse;
     * returns whether it opened.
     */
    public boolean scrolled(boolean down, boolean available) {
        if (browsing || held || !down || !available) return false;
        browsing = true;
        held = true;
        return true;
    }

    /** effects: the client cannot browse (a screen, lost focus, death): drops any open gesture; a key still down opens nothing until released. */
    public void reset(boolean down) {
        browsing = false;
        held = down;
    }

    /** effects: returns whether a gesture is open, so the wheel belongs to the gear bar. */
    public boolean browsing() { return browsing; }
}
