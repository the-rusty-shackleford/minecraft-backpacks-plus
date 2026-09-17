/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import java.util.List;

/** AF: the approved capacities and ordered mounts of a crafted tier. RI: immutable; storage is a multiple of nine. */
public enum BackpackTier {
    BASIC(9, List.of(Mount.LONG, Mount.SMALL)),
    REINFORCED(18, List.of(Mount.LONG, Mount.LONG, Mount.SMALL)),
    EXPEDITION(36, List.of(Mount.LONG, Mount.LONG, Mount.SMALL, Mount.SMALL));

    public enum Mount { LONG, SMALL }
    private final int storage;
    private final List<Mount> mounts;
    BackpackTier(int storage, List<Mount> mounts) { this.storage = storage; this.mounts = List.copyOf(mounts); }
    /** effects: returns the ordinary storage capacity, excluding gear mounts. */
    public int storageSlots() { return storage; }
    /** effects: returns the immutable ordered mount categories. */
    public List<Mount> mounts() { return mounts; }
    /** effects: returns all owned inventory cells, with ordinary storage before mounts. */
    public int totalSlots() { return storage + mounts.size(); }
    /** effects: returns the inventory cell for a mount; throws: IndexOutOfBoundsException for invalid indices. */
    public int mountSlot(int mount) { mounts.get(mount); return storage + mount; }
}
