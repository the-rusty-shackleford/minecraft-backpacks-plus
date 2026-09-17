/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/**
 * AF: renderer-independent descriptions of observable gear actions.
 * RI: stable wire IDs are explicit, never enum ordinals. Inventory authority stays on the server.
 * A client can animate, simplify or omit a gesture without changing the transaction.
 */
public enum GearAction {
    DRAW(1), STOW(2), EXCHANGE(3), OPEN(4), RETRIEVE(5), CLOSE(6);
    private final int wireId;
    GearAction(int wireId) { this.wireId = wireId; }
    /** effects: returns the version-one protocol identifier. */
    public int wireId() { return wireId; }
    /** effects: decodes a known action; throws: IllegalArgumentException for an unknown ID. */
    public static GearAction decode(int id) {
        return switch (id) {
            case 1 -> DRAW; case 2 -> STOW; case 3 -> EXCHANGE;
            case 4 -> OPEN; case 5 -> RETRIEVE; case 6 -> CLOSE;
            default -> throw new IllegalArgumentException("Unknown gear action: " + id);
        };
    }
}
