/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.mixin;

import com.chunkworks.backpacksplus.CreativeBagCells;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A creative player's edit of one of the worn bag's cells, which vanilla's handler would drop
 * as out of its 1..45 range (D-0031): taken by {@link CreativeBagCells} once the packet is on the
 * server thread, every other slot left to vanilla. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void backpacksplus$bagCell(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (CreativeBagCells.apply(player, packet.slotNum(), packet.itemStack())) ci.cancel();
    }
}
