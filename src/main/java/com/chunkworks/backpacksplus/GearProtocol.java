/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.GearAction;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Version-four semantic gear protocol. Client intent contains no item data or animation poses. */
public final class GearProtocol {
    private GearProtocol() {}
    private static Consumer<State> stateReceiver = state -> {};
    private static Consumer<Action> actionReceiver = action -> {};

    /** AF: requested mount and the server snapshot the user chose it from. RI: all fields untrusted until validated. */
    public record Swap(UUID bag, long revision, int mount, int selected) implements CustomPacketPayload {
        public static final Type<Swap> TYPE = new Type<>(BackpacksPlus.id("swap"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Swap> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Swap::bag, ByteBufCodecs.VAR_LONG, Swap::revision,
                ByteBufCodecs.VAR_INT, Swap::mount, ByteBufCodecs.VAR_INT, Swap::selected, Swap::new);
        @Override public Type<Swap> type() { return TYPE; }
    }

    /** AF: explicit storage request; mount=-1 means held stack. RI: all fields are untrusted intent. */
    public record Stow(UUID bag, long revision, int mount, int selected) implements CustomPacketPayload {
        public static final Type<Stow> TYPE = new Type<>(BackpacksPlus.id("stow"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stow> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Stow::bag, ByteBufCodecs.VAR_LONG, Stow::revision,
                ByteBufCodecs.VAR_INT, Stow::mount, ByteBufCodecs.VAR_INT, Stow::selected, Stow::new);
        @Override public Type<Stow> type() { return TYPE; }
    }

    /** AF: authoritative worn bag, including mount contents. RI: owned stack copy; UUID/dimension prevent ID reuse. */
    public record State(int entityId, UUID playerId, ResourceLocation dimension, long sequence, ItemStack bag, long openedAt, int openSource, int wornSource, boolean visible) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(BackpacksPlus.id("state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            @Override public State decode(RegistryFriendlyByteBuf b) {
                return new State(b.readVarInt(),b.readUUID(),b.readResourceLocation(),b.readVarLong(),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(b),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf b, State s) {
                b.writeVarInt(s.entityId); b.writeUUID(s.playerId); b.writeResourceLocation(s.dimension); b.writeVarLong(s.sequence);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b,s.bag); b.writeVarLong(s.openedAt); b.writeVarInt(s.openSource);
                b.writeVarInt(s.wornSource); b.writeBoolean(s.visible);
            }
        };
        public State(int entityId, UUID playerId, ResourceLocation dimension, long sequence, ItemStack bag) {
            this(entityId, playerId, dimension, sequence, bag, -1, -1, BagLocations.CHEST, true);
        }
        public State(int entityId, UUID playerId, ResourceLocation dimension, long sequence, ItemStack bag, long openedAt, int openSource) {
            this(entityId,playerId,dimension,sequence,bag,openedAt,openSource,BagLocations.CHEST,true);
        }
        public State { bag = bag.copy(); }
        @Override public ItemStack bag() { return bag.copy(); }
        @Override public Type<State> type() { return TYPE; }
    }

    /**
     * AF: one accepted server action with visual before/after stacks and a shared game-time origin.
     * RI: this message cannot mutate inventory; renderer is free to animate or omit it. Stacks are owned copies.
     * The server sequence orders actions, while startedAt lets late viewers start at the current phase.
     */
    public record Action(int entityId, UUID playerId, ResourceLocation dimension, long sequence, UUID bag,
            GearAction kind, int mount, long startedAt, ItemStack before, ItemStack after) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(BackpacksPlus.id("action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = new StreamCodec<>() {
            @Override public Action decode(RegistryFriendlyByteBuf b) {
                return new Action(b.readVarInt(), b.readUUID(), b.readResourceLocation(), b.readVarLong(), b.readUUID(),
                        GearAction.decode(b.readVarInt()), b.readVarInt(), b.readVarLong(),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(b), ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
            }
            @Override public void encode(RegistryFriendlyByteBuf b, Action a) {
                b.writeVarInt(a.entityId); b.writeUUID(a.playerId); b.writeResourceLocation(a.dimension);
                b.writeVarLong(a.sequence); b.writeUUID(a.bag); b.writeVarInt(a.kind.wireId());
                b.writeVarInt(a.mount); b.writeVarLong(a.startedAt);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b, a.before); ItemStack.OPTIONAL_STREAM_CODEC.encode(b, a.after);
            }
        };
        public Action { before = before.copy(); after = after.copy(); }
        @Override public ItemStack before() { return before.copy(); }
        @Override public ItemStack after() { return after.copy(); }
        @Override public Type<Action> type() { return TYPE; }
    }

    /** AF: request to open the current worn bag. RI: no client item data, validated against server equipment. */
    public record Open(UUID bag, long revision) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(BackpacksPlus.id("open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Open::bag, ByteBufCodecs.VAR_LONG, Open::revision, Open::new);
        @Override public Type<Open> type() { return TYPE; }
    }

    /** requires: client setup. effects: installs rendering adapters without loading client classes on a dedicated server. */
    public static void receive(Consumer<State> state, Consumer<Action> action) { stateReceiver=state; actionReceiver=action; }
    /** effects: registers required version-four, main-thread handlers; only the server accepts inventory intent. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(Swap.TYPE, Swap.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) GearSync.swap(player, packet);
        });
        registrar.playToServer(Stow.TYPE, Stow.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) GearSync.stow(player, packet);
        });
        registrar.playToClient(State.TYPE, State.CODEC, (packet, context) -> stateReceiver.accept(packet));
        registrar.playToClient(Action.TYPE, Action.CODEC, (packet, context) -> actionReceiver.accept(packet));
        registrar.playToServer(Open.TYPE, Open.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) GearSync.open(player, packet);
        });
    }
}
