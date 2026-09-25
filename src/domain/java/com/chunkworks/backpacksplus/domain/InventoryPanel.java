/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/**
 * Where the worn bag's panel sits on the player's inventory screen (D-0027, D-0028), from the
 * screen's width, whether a bag is worn and whether the recipe book is open. Pure, so the rule
 * is pinned by tests. Distances are GUI pixels: the vanilla screen is {@link #SCREEN} wide; its
 * open recipe book takes {@link #BOOK} on the screen's left (the book and the gap vanilla keeps)
 * and its category tabs hang {@link #TABS} further left over whatever is there; the panel is
 * {@link #WIDTH} wide with {@link #GAP} between it and whatever stands to its right.
 *
 * <p>While the panel is shown, a worn bag makes the vanilla layout {@link #widen()} wider: the
 * screen, and the book when it is open, are laid out as vanilla lays them out for a screen that
 * much wider, which moves them right by half of it, so the panel and the vanilla layout are
 * centered as one. The panel is left of everything: left of the screen while the book is
 * closed, left of the book's tabs while the book is open. Vanilla stands the open book beside
 * the screen from {@link #BESIDE} wide up and over it below; with the panel the same rule holds
 * with the panel, its gap and the tab strip added, and where the open book cannot stand beside
 * the panel, the book takes the room: the panel is not shown, its cells are inactive, and
 * vanilla lays the screen out as it would with no bag, so the book's button stays where the
 * player can reach it and closing the book brings the panel back. Where not even panel and
 * screen fit side by side, the panel is never shown.
 *
 * <p>AF: {@code shown} whether the panel and its cells are on the screen; {@code beside} whether
 * the open recipe book stands beside the panel at this width; {@code panelX} the panel's left
 * edge relative to the screen's left edge.<br>
 * RI: when shown, panelX is -(WIDTH+GAP) or, only when beside, -(WIDTH+GAP+TABS+BOOK); when
 * not shown, panelX is 0 and beside is false.
 */
public record InventoryPanel(boolean shown, boolean beside, int panelX) {
    /** The panel's width and the gap to its right; the vanilla screen's width; what the open book takes beside the screen; the tab strip left of the book. */
    public static final int WIDTH = 176, GAP = 4, SCREEN = 176, BOOK = 147 + 2, TABS = 30;
    /** The least margin kept between the panel and the screen's edge. */
    public static final int MARGIN = 2;
    /** Vanilla puts the open book beside the screen from this width up, and over it below. */
    public static final int BESIDE = 379;
    /** No panel: no bag worn, no room, or the open book has the room. */
    public static final InventoryPanel NONE = new InventoryPanel(false, false, 0);

    public InventoryPanel {
        boolean ok = shown ? panelX == -(WIDTH + GAP) || (beside && panelX == -(WIDTH + GAP + TABS + BOOK)) : panelX == 0 && !beside;
        if (!ok) throw new IllegalArgumentException("panel");
    }

    /** requires: width > 0; effects: the placement for a screen {@code width} wide with a bag
     * worn or not and the recipe book open or not. */
    public static InventoryPanel of(int width, boolean worn, boolean bookOpen) {
        if (width <= 0) throw new IllegalArgumentException("width");
        if (!worn || width < WIDTH + GAP + SCREEN + 2 * MARGIN) return NONE;
        boolean beside = width >= BESIDE + WIDTH + GAP + TABS;
        if (bookOpen && !beside) return NONE;
        return new InventoryPanel(true, beside, -(WIDTH + GAP) - (bookOpen ? TABS + BOOK : 0));
    }

    /** effects: how much wider than the screen the vanilla layout is laid out: the panel and its
     * gap while the panel is shown, nothing otherwise. */
    public int widen() { return shown ? WIDTH + GAP : 0; }

    /** effects: whether the open recipe book overlays the screen at this width instead of
     * standing beside it: vanilla's rule, with the panel, its gap and the tab strip added while
     * the panel is shown. */
    public boolean overlays(int width) { return width < BESIDE + (shown ? WIDTH + GAP + TABS : 0); }
}
