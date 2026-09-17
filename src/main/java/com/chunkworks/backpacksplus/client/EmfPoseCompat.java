/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Isolated optional EMF Compat Core 1.1.2 public API adapter. AF: our named pose source.
 * RI: never clears other mods' sources. Reflection resolves once, avoiding a required
 * binary dependency; no EMF internals or other-mod mixins. Only owned arms get snapshots.
 */
final class EmfPoseCompat {
    private static final String SOURCE="backpacksplus";
    private static final Set<UUID> ACTIVE=new HashSet<>();
    private static Constructor<?> snapshot;
    private static Method save, clear;
    private static boolean available;
    static {
        try {
            Class<?> manager=Class.forName("strm.emfcompat.core.PoseManager");
            Class<?> pose=Class.forName("strm.emfcompat.core.PoseSnapshot");
            snapshot=pose.getConstructor(ModelPart.class,boolean.class);
            save=manager.getMethod("savePoses",UUID.class,String.class,pose,pose);
            clear=manager.getMethod("clearPoses",UUID.class,String.class);
            manager.getMethod("setSourcePriority",String.class,int.class).invoke(null,SOURCE,100);
            available=true;
        } catch (ReflectiveOperationException failure) {
            LogUtils.getLogger().warn("Backpacks+ could not connect to EMF Compat Core pose API",failure);
        }
    }
    private EmfPoseCompat() {}
    /** effects: publishes only this frame's owned arms; null arms release this mod's previous claim. */
    static void publish(UUID id, ModelPart left, ModelPart right) {
        if (!available) return;
        try {
            if (left==null && right==null) {
                if (ACTIVE.remove(id)) clear.invoke(null,id,SOURCE);
            } else {
                save.invoke(null,id,SOURCE,left==null ? null : snapshot.newInstance(left,true),
                        right==null ? null : snapshot.newInstance(right,true));
                ACTIVE.add(id);
            }
        } catch (ReflectiveOperationException failure) {
            LogUtils.getLogger().warn("Backpacks+ EMF pose adapter failed",failure);
            clear(); available=false;
        }
    }
    static void clear() {
        for (UUID id:ACTIVE) try { clear.invoke(null,id,SOURCE); }
        catch (ReflectiveOperationException failure) { LogUtils.getLogger().warn("Cannot clear Backpacks+ EMF pose",failure); }
        ACTIVE.clear();
    }
}
