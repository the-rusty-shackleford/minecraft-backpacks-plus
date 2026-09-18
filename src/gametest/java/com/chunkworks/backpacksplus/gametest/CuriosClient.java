/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;
import top.theillusivec4.curios.common.network.client.CPacketToggleRender;

/** Test-only bridge to the exact native Curios UI packets; never shipped with the mod. */
final class CuriosClient {
    private CuriosClient() {}
    static void open() { PacketDistributor.sendToServer(new CPacketOpenCurios(Minecraft.getInstance().player.containerMenu.getCarried().copy())); }
    static void toggle() { PacketDistributor.sendToServer(new CPacketToggleRender("back",0)); }
    static void observe(JsonObject state) {
        var p=Minecraft.getInstance().player;if(p==null)return;
        for(var slot:p.containerMenu.slots)if(slot instanceof CurioSlot curio && curio.getIdentifier().equals("back") && !curio.getSlotContext().cosmetic()) {
            state.addProperty("backMenuSlot",slot.index);break;
        }
    }
}
