/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import java.util.Objects;

/**
 * AF: the injective mapping of occupied cells from one crafted stage to its successor.
 * RI: storage retains its index; each mount retains its category and order within that
 * category. This stateless value policy never turns an existing mount into storage.
 */
public final class UpgradeLayout {
    private UpgradeLayout() {}

    /**
     * requires: non-null tiers and a source cell in the old tier.
     * effects: returns the corresponding cell in the immediately following tier.
     * throws: IllegalArgumentException for a non-adjacent upgrade; IndexOutOfBoundsException
     * for an invalid source cell; NullPointerException for a null tier.
     */
    public static int destination(BackpackTier from, BackpackTier to, int source) {
        Objects.requireNonNull(from); Objects.requireNonNull(to);
        if (to.ordinal()!=from.ordinal()+1) throw new IllegalArgumentException("Upgrade must advance one tier");
        Objects.checkIndex(source,from.totalSlots());
        if (source<from.storageSlots()) return source;
        int mount=source-from.storageSlots();
        var kind=from.mounts().get(mount);
        int ordinal=0;
        for (int i=0;i<mount;i++) if (from.mounts().get(i)==kind) ordinal++;
        for (int i=0;i<to.mounts().size();i++) {
            if (to.mounts().get(i)==kind && ordinal--==0) return to.mountSlot(i);
        }
        throw new IllegalArgumentException("Upgrade has no matching mount");
    }
}
