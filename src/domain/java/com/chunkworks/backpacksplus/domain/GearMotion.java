/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/** Pure animation timing. All inventory operations finish independently on the server. */
public final class GearMotion {
    private GearMotion() {}
    public static final double EXCHANGE_TICKS = 26;
    public static final double OPEN_TICKS = 24;
    /** effects: returns a smooth, clamped transition; throws: IllegalArgumentException for nonfinite input. */
    public static double ease(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite animation time");
        double t = Math.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
    /** effects: reaches the mount at midpoint and returns to normal at either endpoint. */
    public static double reach(double progress) { return ease(1 - Math.abs(2 * Math.clamp(progress, 0, 1) - 1)); }
    /** effects: reverses a possibly interrupted opening without first jumping to the fully open pose. */
    public static double closing(double openedTicks, double closingTicks) {
        return ease(openedTicks/OPEN_TICKS)*(1-ease(closingTicks/OPEN_TICKS));
    }
    /** effects: identifies the server actions which exchange a hand and mount. */
    public static boolean exchange(GearAction action) {
        return action == GearAction.DRAW || action == GearAction.STOW || action == GearAction.EXCHANGE;
    }
}
