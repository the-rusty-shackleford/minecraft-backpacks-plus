/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Where the bag's recipe handler stands among a recipe viewer's handlers for one screen (D-0033):
 * just ahead of the viewer's own, behind any other mod's. The viewer asks the first handler for a
 * screen's inventory and the first that supports a recipe to fill it, and another mod may stand
 * its handler first on purpose (Warehouse Manager's, at a table in a building it manages). Taking
 * the front would hide that mod's counts, and which of two mods took it last would depend on the
 * order they registered in. Ahead of the viewer's own, the order is the same either way.
 */
public final class HandlerOrder {
    private HandlerOrder() {}

    /**
     * requires: {@code handlers} is mutable and holds {@code handler} at most once.
     * effects: moves {@code handler} to just before the first other entry {@code own} accepts, or
     * to the end when there is none, and returns true; returns false and changes nothing when
     * {@code handlers} does not hold it.
     */
    public static <T> boolean aheadOfOwn(List<T> handlers, T handler, Predicate<? super T> own) {
        Objects.requireNonNull(handler);
        Objects.requireNonNull(own);
        if (!handlers.remove(handler)) return false;
        int at = handlers.size();
        for (int i = 0; i < handlers.size(); i++) {
            if (own.test(handlers.get(i))) { at = i; break; }
        }
        handlers.add(at, handler);
        return true;
    }
}
