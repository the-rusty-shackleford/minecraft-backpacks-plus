/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable scroll choices for one G gesture.
 * AF: quick slot, each mount followed by its explicit deposit action when occupied,
 * then the held-stack deposit when a bag and held stack exist.
 * RI: at most four mounts, no action for an empty mount, and no mutable list escapes.
 */
public final class GearChoices {
    private GearChoices() {}
    public enum Kind { QUICK, MOUNT, STOW_MOUNT, STOW_HELD }

    /** AF: a displayed action and its source mount. RI: only mount actions have indices 0..3. */
    public record Choice(Kind kind, int mount) {
        /** effects: constructs a source-specific choice; throws: IllegalArgumentException for invalid kind/index pairs. */
        public Choice {
            if (kind == null || ((kind == Kind.MOUNT || kind == Kind.STOW_MOUNT)
                    ? mount < 0 || mount > 3 : mount != -1)) throw new IllegalArgumentException("Invalid gear choice");
        }
    }

    /**
     * effects: returns the immutable scroll order, with deposits beside their source.
     * throws: IllegalArgumentException for unsupported mount counts or occupancy bits.
     */
    public static List<Choice> build(boolean quick, int mounts, int occupied, boolean held) {
        if (mounts < 0 || mounts > 4 || occupied < 0 || occupied >= (1 << mounts))
            throw new IllegalArgumentException("Invalid mount occupancy");
        var choices = new ArrayList<Choice>();
        if (quick) choices.add(new Choice(Kind.QUICK, -1));
        for (int i=0; i<mounts; i++) {
            choices.add(new Choice(Kind.MOUNT, i));
            if ((occupied & (1 << i)) != 0) choices.add(new Choice(Kind.STOW_MOUNT, i));
        }
        if (mounts > 0 && held) choices.add(new Choice(Kind.STOW_HELD, -1));
        return List.copyOf(choices);
    }
}
