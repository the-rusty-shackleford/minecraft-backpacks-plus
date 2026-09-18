/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/** Pure authorization and all-or-nothing exchange policy; no game or client dependencies. */
public final class SwapRules {
    private SwapRules() {}
    /** effects: returns whether a request still identifies a usable server-owned bag and selected hotbar cell. */
    public static boolean current(boolean alive, boolean spectator, boolean busy, boolean sameBag,
            int selected, int actualSelected, long expectedRevision, long actualRevision) {
        return alive && !spectator && !busy && sameBag && selected >= 0 && selected < 9
                && selected == actualSelected && expectedRevision >= 0 && expectedRevision == actualRevision;
    }
}
