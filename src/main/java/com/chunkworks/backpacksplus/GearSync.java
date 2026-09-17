/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.GearAction;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Server tracking/state adapter. RI: one intent per tick; equipment identity/revision checks allocate only on changes. */
@EventBusSubscriber(modid=BackpacksPlus.ID)
public final class GearSync {
    private GearSync() {}
    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BackpacksPlus.ID);
    private static final Supplier<AttachmentType<Session>> SESSION = TYPES.register("session", () -> AttachmentType.builder(Session::new).build());
    private static final class Session {
        ItemStack seen = ItemStack.EMPTY;
        long revision = -1, sequence, lastRequest = Long.MIN_VALUE;
        long openedAt = -1;
        int openSource = -1;
        GearProtocol.Action action;
    }
    /** effects: registers nonpersistent session state and the network protocol. */
    public static void register(IEventBus bus) { TYPES.register(bus); bus.addListener(GearProtocol::register); }

    /** effects: returns the native chest-route bag, empty when none is equipped. Curios routing is a later adapter. */
    public static ItemStack worn(ServerPlayer player) {
        ItemStack item=player.getItemBySlot(EquipmentSlot.CHEST);
        return item.getItem() instanceof BackpackItem && item.getCount()==1 ? item : ItemStack.EMPTY;
    }
    private static GearProtocol.State snapshot(ServerPlayer player, Session session) {
        return new GearProtocol.State(player.getId(), player.getUUID(), player.level().dimension().location(), session.sequence, worn(player), session.openedAt, session.openSource);
    }
    private static void observe(ServerPlayer player) {
        Session state=player.getData(SESSION); ItemStack bag=worn(player);
        if (!bag.isEmpty()) BagContents.identify(bag);
        long revision=bag.isEmpty() ? 0 : BagContents.revision(bag);
        int openSource=player.containerMenu instanceof BackpackMenu menu && menu.stillValid(player) ? menu.source() : -1;
        boolean changed=openSource!=state.openSource;
        if (changed) { state.openedAt=openSource>=0 ? player.level().getGameTime() : -1; state.openSource=openSource; }
        if (state.seen==bag && state.revision==revision && !changed) return;
        state.seen=bag; state.revision=revision; state.sequence++;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, snapshot(player,state));
    }
    /** effects: accepts one current intent per tick, using only the server's equipped bag and actual held item. */
    public static void swap(ServerPlayer player, GearProtocol.Swap request) {
        Session state=player.getData(SESSION); long now=player.level().getGameTime();
        if (state.lastRequest==now) return; state.lastRequest=now;
        ItemStack bag=worn(player);
        if (bag.isEmpty()) return;
        ItemStack before=player.getMainHandItem().copy();
        if (!MountExchange.swap(player,38,request.bag(),request.revision(),request.mount(),request.selected())) return;
        observe(player);
        ItemStack after=player.getMainHandItem();
        GearAction kind=before.isEmpty() ? GearAction.DRAW : after.isEmpty() ? GearAction.STOW : GearAction.EXCHANGE;
        action(player,bag,kind,request.mount(),before,after);
    }
    /** effects: opens only the exact current worn bag for a living, non-spectating player outside another menu. */
    public static void open(ServerPlayer player, GearProtocol.Open request) {
        ItemStack bag=worn(player);
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem() || player.containerMenu!=player.inventoryMenu
                || bag.isEmpty() || !request.bag().equals(bag.get(BackpackItems.ID)) || request.revision()!=BagContents.revision(bag)) return;
        BackpackItem.open(player,bag,38);
        observe(player);
    }
    /** effects: broadcasts a semantic action after its associated server operation succeeds. */
    public static void action(ServerPlayer player, ItemStack bag, GearAction kind, int mount, ItemStack before, ItemStack after) {
        Session state=player.getData(SESSION);
        state.action=new GearProtocol.Action(player.getId(),player.getUUID(),player.level().dimension().location(),++state.sequence,
                BagContents.identify(bag),kind,mount,player.level().getGameTime(),before,after);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,state.action);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            observe(player);
            Session state=player.getData(SESSION);
            if (state.action!=null && player.level().getGameTime()-state.action.startedAt()>40) state.action=null;
        }
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            observe(target); Session state=target.getData(SESSION);
            PacketDistributor.sendToPlayer(viewer,snapshot(target,state));
            if (state.action!=null && target.level().getGameTime()-state.action.startedAt()<=40)
                PacketDistributor.sendToPlayer(viewer,state.action);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { refresh(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { refresh(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { refresh(event.getEntity()); }
    private static void refresh(net.minecraft.world.entity.player.Player entity) {
        if (entity instanceof ServerPlayer player) {
            Session state=player.getData(SESSION); state.revision=-1; state.action=null; observe(player);
        }
    }
}
