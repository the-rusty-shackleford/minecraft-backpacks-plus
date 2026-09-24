/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/**
 * Where the gear bar's cells go, from the screen and what the bar holds. Pure, so the rule is
 * pinned by tests. Cells are 22 wide and 22 tall. The quick slot's column (or the empty column
 * where it would be) is at {@code base}, beside the hotbar; the mounts run outward from it in
 * {@code direction} (+1 rightward for a right-handed player, -1 for a left-handed one),
 * starting one cell and a gap beyond it.
 *
 * <p>The row stays beside the hotbar wherever the mounts can fit there, closing up from the
 * usual 24-pixel pitch to 22 (frames touching) and then 20 (frames sharing a border, as the
 * hotbar's own cells do) before it gives up; only when even that overflows the screen does the
 * row lift above the status icons with its outer end at the quick slot (D-0011). The mounts'
 * places depend on the mounts alone, so nothing moves when G goes down: while browsing with an
 * item in hand, the "put held item in bag" cell goes at the end of the row when it fits there
 * at the row's pitch, else one row up above the quick slot's column.
 *
 * <p>AF: {@code pitch} is the distance between mount cells; {@code first} the x of mount 0;
 * {@code rowY} the y of the row; {@code lifted} whether the row is above the status icons;
 * {@code heldX}/{@code heldY} the held-deposit cell, or -1/-1 when there is none.<br>
 * RI: pitch is 20, 22 or 24; every mount cell lies within 3 pixels of the screen's edges;
 * heldX and heldY are both -1 or both cell positions.
 */
public record GearLayout(int pitch, int first, int rowY, boolean lifted, int heldX, int heldY) {

    /** A cell's width and height. */
    public static final int CELL = 22;
    /** The usual pitch, and the gap the first mount keeps from the quick slot's column. */
    public static final int PITCH = 24;
    /** How far above the row a deposit cell sits. */
    public static final int UPPER = 34;
    private static final int MARGIN = 3;

    public GearLayout {
        if (pitch != 20 && pitch != 22 && pitch != 24) throw new IllegalArgumentException("Invalid pitch " + pitch);
        if ((heldX < 0) != (heldY < 0)) throw new IllegalArgumentException("A held cell has both coordinates or neither");
    }

    /**
     * effects: returns the layout of {@code mounts} cells (1..4) beside a hotbar whose quick-slot
     * column is at {@code base}, running in {@code direction}, on a screen {@code guiWidth} by
     * {@code guiHeight} whose status icons reach {@code statusHeight} up from the bottom; with a
     * held-deposit cell when {@code held}.
     * throws: IllegalArgumentException for a mount count outside 1..4 or a direction not +1/-1.
     */
    public static GearLayout of(int guiWidth, int guiHeight, int base, int direction, int mounts, boolean held, int statusHeight) {
        if (mounts < 1 || mounts > 4) throw new IllegalArgumentException("Invalid mount count " + mounts);
        if (direction != 1 && direction != -1) throw new IllegalArgumentException("Invalid direction " + direction);
        int bottom = guiHeight - CELL;
        int first = base + direction * PITCH;
        for (int pitch : new int[] {24, 22, 20}) {
            if (!onScreen(guiWidth, first, first + direction * (mounts - 1) * pitch)) continue;
            int heldX = -1, heldY = -1;
            if (held) {
                int end = first + direction * mounts * pitch;
                if (onScreen(guiWidth, end, end)) { heldX = end; heldY = bottom; }
                else { heldX = base; heldY = bottom - UPPER; }
            }
            return new GearLayout(pitch, first, bottom, false, heldX, heldY);
        }
        // Lifted above the status icons (D-0011): the outer end at the quick slot, the whole row
        // with its held cell shifted inward as far as the screen's edge demands.
        int rowY = guiHeight - Math.max(82, statusHeight + 26);
        int columns = mounts + (held ? 1 : 0);
        int liftedFirst = base - direction * (mounts - 1) * PITCH;
        int last = liftedFirst + direction * (columns - 1) * PITCH;
        int low = Math.min(liftedFirst, last), high = Math.max(liftedFirst, last) + CELL;
        liftedFirst += Math.max(0, MARGIN - low) - Math.max(0, high - (guiWidth - MARGIN));
        return new GearLayout(PITCH, liftedFirst, rowY, true,
                held ? liftedFirst + direction * mounts * PITCH : -1, held ? rowY : -1);
    }

    /** effects: returns whether cells at both {@code a} and {@code b} lie within the screen's margins */
    private static boolean onScreen(int guiWidth, int a, int b) {
        return Math.min(a, b) >= MARGIN && Math.max(a, b) + CELL <= guiWidth - MARGIN;
    }

    /** effects: returns the x of mount {@code i} */
    public int mountX(int direction, int i) {
        return first + direction * i * pitch;
    }

    /** effects: returns whether any deposit cell would sit in the row above the mounts: the held cell lifted there */
    public boolean heldAbove() {
        return heldX >= 0 && heldY != rowY;
    }
}
