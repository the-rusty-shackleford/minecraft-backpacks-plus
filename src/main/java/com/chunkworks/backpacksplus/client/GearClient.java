/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client input/state adapter. AF: a bounded cache of server equipment snapshots plus a pending G selection.
 * RI: drawing this cache never changes items; releasing G sends identity/revision intent only.
 * GUI opening, focus loss, death, equipment changes or selecting another hotbar cell cancel an in-progress browse.
 */
@EventBusSubscriber(modid=BackpacksPlus.ID, value=Dist.CLIENT)
public final class GearClient {
    private GearClient() {}
    private static final Map<UUID,View> VIEWS = new LinkedHashMap<>();
    private static final Map<UUID,GearProtocol.Action> ACTIONS = new LinkedHashMap<>();
    private static final Map<UUID,Motion> MOTIONS = new LinkedHashMap<>();
    /** AF: action plus display stacks decoded once. RI: renderers treat the stacks as read-only. */
    record Motion(GearProtocol.Action action, ItemStack before, ItemStack after) {}
    static Motion motion(UUID player) { return MOTIONS.get(player); }
    private static boolean held, browsing, moved;
    private static int selected, hotbar, age;
    private static UUID bagId;
    private static long bagRevision, quickRevision;
    static final boolean QUICK_SLOT = ModList.get().isLoaded("quickslot");
    /** AF: decoded equipment used by HUD/body renderers. RI: stacks belong to this view and must be treated read-only. */
    static final class View {
        final GearProtocol.State state;
        final ItemStack bag;
        final NonNullList<ItemStack> cells;
        final int receivedAt;
        final long lastOpenedAt;
        View(GearProtocol.State state, View prior) {
            this.state=state; this.bag=state.bag(); this.receivedAt=age;
            this.lastOpenedAt=state.openedAt()>=0 ? state.openedAt() : prior==null ? -1 : prior.lastOpenedAt;
            this.cells=bag.isEmpty() ? NonNullList.create() : BagContents.copy(bag);
        }
        int mounts() { return bag.isEmpty() ? 0 : BagContents.tier(bag).mounts().size(); }
        ItemStack mount(int index) { return cells.get(BagContents.tier(bag).mountSlot(index)); }
    }
    /** effects: accepts newer server snapshots, bounded even when their entity has not spawned yet. */
    public static void receive(GearProtocol.State packet) {
        View old=VIEWS.get(packet.playerId());
        if (old!=null && old.state.entityId()==packet.entityId() && old.state.dimension().equals(packet.dimension()) && old.state.sequence()>=packet.sequence()) return;
        if (!packet.bag().isEmpty() && !(packet.bag().getItem() instanceof BackpackItem)) return;
        if (VIEWS.size()>=256 && !VIEWS.containsKey(packet.playerId())) VIEWS.remove(VIEWS.keySet().iterator().next());
        VIEWS.put(packet.playerId(),new View(packet,old));
    }
    /** effects: caches ordered semantic actions and display stacks for the animation adapter; never touches inventory. */
    public static void receive(GearProtocol.Action packet) {
        var old=ACTIONS.get(packet.playerId());
        if (old!=null && old.entityId()==packet.entityId() && old.dimension().equals(packet.dimension()) && old.sequence()>=packet.sequence()) return;
        if (ACTIONS.size()>=256 && !ACTIONS.containsKey(packet.playerId())) ACTIONS.remove(ACTIONS.keySet().iterator().next());
        ACTIONS.put(packet.playerId(),packet);
        if (MOTIONS.size()>=256 && !MOTIONS.containsKey(packet.playerId())) MOTIONS.remove(MOTIONS.keySet().iterator().next());
        MOTIONS.put(packet.playerId(),new Motion(packet,packet.before(),packet.after()));
    }
    static View self() {
        Minecraft mc=Minecraft.getInstance(); if (mc.player==null || mc.level==null) return null;
        return view(mc.player.getUUID());
    }
    static View view(UUID player) {
        Minecraft mc=Minecraft.getInstance(); View view=VIEWS.get(player);
        if (view==null || mc.level==null || !view.state.dimension().equals(mc.level.dimension().location())) return null;
        var entity=mc.level.getEntity(view.state.entityId());
        return entity!=null && entity.getUUID().equals(player) ? view : null;
    }
    /** effects: returns this tracked actor's immutable equipment snapshot, or null until it is available. */
    public static GearProtocol.State snapshot(UUID player) { View view=view(player); return view==null ? null : view.state; }
    /** effects: returns the latest cached semantic action, or null; animation consumers never mutate inventory. */
    public static GearProtocol.Action action(UUID player) { return ACTIONS.get(player); }
    /** effects: returns whether G currently owns wheel input. */
    public static boolean browsing() { return browsing; }
    /** effects: returns the highlighted gear-bar cell while browsing. */
    public static int selection() { return selected; }
    private static int count(View view) { return (QUICK_SLOT ? 1 : 0)+(view==null ? 0 : view.mounts()); }
    private static boolean usable(Minecraft mc) {
        return mc.player!=null && mc.level!=null && mc.screen==null && mc.isWindowActive() && mc.player.isAlive()
                && !mc.player.isSpectator() && !mc.player.isUsingItem() && !(QUICK_SLOT && QuickSlotCompat.driving(mc.player));
    }
    private static boolean sameSelection(Minecraft mc, View view) {
        UUID current=view==null || view.bag.isEmpty() ? null : view.bag.get(BackpackItems.ID);
        long revision=view==null || view.bag.isEmpty() ? 0 : BagContents.revision(view.bag);
        return java.util.Objects.equals(current,bagId) && revision==bagRevision && mc.player.getInventory().selected==hotbar;
    }
    private static void begin(Minecraft mc, View view) {
        browsing=true; moved=false; selected=Math.min(selected,count(view)-1); hotbar=mc.player.getInventory().selected;
        bagId=view==null || view.bag.isEmpty() ? null : view.bag.get(BackpackItems.ID);
        bagRevision=view==null || view.bag.isEmpty() ? 0 : BagContents.revision(view.bag);
        quickRevision=QUICK_SLOT ? QuickSlotCompat.revision(mc.player) : 0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance(); age++;
        if (age%20==0) {
            VIEWS.values().removeIf(v -> age-v.receivedAt>100 && (mc.level==null || !v.state.dimension().equals(mc.level.dimension().location())
                    || mc.level.getEntity(v.state.entityId())==null));
            ACTIONS.values().removeIf(a -> mc.level==null || !a.dimension().equals(mc.level.dimension().location()) || mc.level.getGameTime()-a.startedAt()>40);
            MOTIONS.keySet().retainAll(ACTIONS.keySet());
        }
        boolean down=GearClientSetup.BROWSE.isDown(); View view=self();
        if (!usable(mc)) { browsing=false; moved=false; held=down; return; }
        while (GearClientSetup.OPEN.consumeClick()) {
            if (view!=null && !view.bag.isEmpty()) PacketDistributor.sendToServer(new GearProtocol.Open(view.bag.get(BackpackItems.ID),BagContents.revision(view.bag)));
        }
        if (down && !held && count(view)>0) {
            begin(mc,view);
        }
        if (browsing && !sameSelection(mc,view)) { browsing=false; moved=false; }
        if (!down && held && browsing) {
            if (moved) {
                if (QUICK_SLOT && selected==0) QuickSlotCompat.swap(hotbar,quickRevision);
                else if (bagId!=null) PacketDistributor.sendToServer(new GearProtocol.Swap(bagId,bagRevision,selected-(QUICK_SLOT ? 1 : 0),hotbar));
            }
            browsing=false; moved=false;
        }
        held=down;
    }
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc=Minecraft.getInstance();
        if (!held && GearClientSetup.BROWSE.isDown() && usable(mc) && count(self())>0) {
            begin(mc,self()); held=true;
        }
        if (!browsing || !usable(mc) || event.getScrollDeltaY()==0) return;
        int count=count(self()); if (count==0) return;
        selected=Math.floorMod(selected-(event.getScrollDeltaY()>0 ? 1 : -1),count); moved=true; event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear(); ACTIONS.clear(); MOTIONS.clear(); GearPoses.clear(); browsing=false; held=false; moved=false; selected=0; age=0;
    }
}
