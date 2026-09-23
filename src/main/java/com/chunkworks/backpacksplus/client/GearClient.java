/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import com.chunkworks.backpacksplus.domain.GearChoices;
import com.chunkworks.backpacksplus.domain.GearGesture;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import net.minecraft.network.chat.Component;
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
 * Releasing G commits whatever is highlighted, wheel or no wheel ({@link GearGesture}); the highlight
 * opens on the last swap made, else the first mount, never on a deposit ({@link GearChoices#defaultIndex}).
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
    /** AF: a frozen gesture preview. RI: null reason means permitted; renderers never recalculate capacity. */
    record Option(GearChoices.Choice choice, Component title, Component reason) {}
    private static List<Option> options=List.of();
    private static GearChoices.Choice lastChoice;
    private static ItemStack heldAtStart=ItemStack.EMPTY;
    private static final GearGesture GESTURE=new GearGesture();
    private static int selected, hotbar, age, bagSource;
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
    public static boolean browsing() { return GESTURE.browsing(); }
    /** effects: returns the highlighted gear-bar cell while browsing. */
    public static int selection() { return selected; }
    static Option option() { return GESTURE.browsing() && selected<options.size() ? options.get(selected) : null; }
    static List<Option> options() { return options; }
    static boolean selected(GearChoices.Kind kind, int mount) {
        Option option=option(); return option!=null && option.choice().kind()==kind && option.choice().mount()==mount;
    }
    private static int count(View view) { return (QUICK_SLOT ? 1 : 0)+(view==null ? 0 : view.mounts()); }
    private static boolean usable(Minecraft mc) {
        return mc.player!=null && mc.level!=null && mc.screen==null && mc.isWindowActive() && mc.player.isAlive()
                && !mc.player.isSpectator() && !mc.player.isUsingItem() && !(QUICK_SLOT && QuickSlotCompat.driving(mc.player));
    }
    static boolean ownsBrowseKey() {
        var view=self();return usable(Minecraft.getInstance()) && view!=null && !view.bag.isEmpty();
    }
    private static boolean sameSelection(Minecraft mc, View view) {
        UUID current=view==null || view.bag.isEmpty() ? null : view.bag.get(BackpackItems.ID);
        long revision=view==null || view.bag.isEmpty() ? 0 : BagContents.revision(view.bag);
        return java.util.Objects.equals(current,bagId) && (view==null ? -1 : view.state.wornSource())==bagSource && revision==bagRevision && mc.player.getInventory().selected==hotbar && ItemStack.matches(heldAtStart,mc.player.getMainHandItem());
    }
    /** effects: freezes the choices for one gesture and puts the highlight on its default. */
    private static void begin(Minecraft mc, View view) {
        hotbar=mc.player.getInventory().selected;
        heldAtStart=mc.player.getMainHandItem().copy();
        int mounts=view==null ? 0 : view.mounts(), occupied=0;
        for (int i=0;i<mounts;i++) if (!view.mount(i).isEmpty()) occupied|=1<<i;
        var next=new ArrayList<Option>();
        for (var choice:GearChoices.build(QUICK_SLOT,mounts,occupied,!heldAtStart.isEmpty())) {
            Component title,reason=null;
            switch (choice.kind()) {
                case QUICK -> title=Component.translatable("backpacksplus.quick_slot");
                case MOUNT -> {
                    var tier=BagContents.tier(view.bag);
                    title=Component.translatable(tier.mounts().get(choice.mount())==BackpackTier.Mount.LONG
                            ? "backpacksplus.long_mount" : "backpacksplus.small_mount");
                    if (!BagContents.admits(tier,tier.mountSlot(choice.mount()),heldAtStart))
                        reason=!BagContents.storable(heldAtStart) ? Component.translatable("backpacksplus.not_storable") : Component.translatable(tier.mounts().get(choice.mount())==BackpackTier.Mount.LONG
                                ? "backpacksplus.long_only" : "backpacksplus.small_only");
                }
                case STOW_MOUNT, STOW_HELD -> {
                    ItemStack item=choice.kind()==GearChoices.Kind.STOW_HELD ? heldAtStart : view.mount(choice.mount());
                    title=Component.translatable(choice.kind()==GearChoices.Kind.STOW_HELD
                            ? "backpacksplus.stow_held" : "backpacksplus.stow_mount",item.getHoverName());
                    if (!BagContents.storable(item)) reason=Component.translatable("backpacksplus.not_storable");
                    else if (!MountExchange.storageFits(view.bag,item)) reason=Component.translatable("backpacksplus.storage_full");
                }
                default -> throw new IllegalStateException("Unhandled gear choice");
            }
            next.add(new Option(choice,title,reason));
        }
        options=List.copyOf(next); selected=GearChoices.defaultIndex(options.stream().map(Option::choice).toList(),lastChoice);
        bagId=view==null || view.bag.isEmpty() ? null : view.bag.get(BackpackItems.ID);
        bagRevision=view==null || view.bag.isEmpty() ? 0 : BagContents.revision(view.bag);
        bagSource=view==null ? -1 : view.state.wornSource();
        quickRevision=QUICK_SLOT ? QuickSlotCompat.revision(mc.player) : 0;
    }
    /**
     * effects: commits the highlighted choice of the gesture just closed: a refused one shows its
     * reason, a swap or a deposit sends its intent; a swap is remembered as the next default.
     */
    private static void commit(Minecraft mc) {
        if (selected>=options.size()) return;
        Option option=options.get(selected);
        var kind=option.choice().kind();
        if (kind==GearChoices.Kind.QUICK || kind==GearChoices.Kind.MOUNT) lastChoice=option.choice();
        if (option.reason()!=null) { mc.player.displayClientMessage(option.reason(),true); return; }
        switch (kind) {
            case QUICK -> QuickSlotCompat.swap(hotbar,quickRevision);
            case MOUNT -> PacketDistributor.sendToServer(new GearProtocol.Swap(bagId,bagRevision,option.choice().mount(),hotbar));
            case STOW_MOUNT, STOW_HELD -> PacketDistributor.sendToServer(new GearProtocol.Stow(bagId,bagRevision,option.choice().mount(),hotbar));
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance(); age++;
        if (age%20==0) {
            VIEWS.values().removeIf(v -> age-v.receivedAt>100 && (mc.level==null || !v.state.dimension().equals(mc.level.dimension().location())
                    || mc.level.getEntity(v.state.entityId())==null));
            ACTIONS.values().removeIf(a -> mc.level==null || !a.dimension().equals(mc.level.dimension().location()) || mc.level.getGameTime()-a.startedAt()>40);
            MOTIONS.keySet().retainAll(ACTIONS.keySet());
        }
        boolean down=GearClientSetup.BROWSE.isDown(), pressed=false;
        while (GearClientSetup.BROWSE.consumeClick()) pressed=true;   // a press between two ticks still counts
        View view=self();
        if (!usable(mc)) { GESTURE.reset(down); return; }
        while (GearClientSetup.OPEN.consumeClick()) {
            if (view!=null && !view.bag.isEmpty()) PacketDistributor.sendToServer(new GearProtocol.Open(view.bag.get(BackpackItems.ID),BagContents.revision(view.bag)));
        }
        boolean valid=!GESTURE.browsing() || sameSelection(mc,view);
        switch (GESTURE.tick(down,pressed,count(view)>0,valid)) {
            case BEGIN -> begin(mc,view);
            case TAP -> { begin(mc,view); commit(mc); }
            case COMMIT -> commit(mc);
            case CANCEL, NONE -> { }
        }
    }
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc=Minecraft.getInstance();
        if (GESTURE.scrolled(GearClientSetup.BROWSE.isDown(),usable(mc) && count(self())>0)) begin(mc,self());
        if (!GESTURE.browsing() || !usable(mc) || event.getScrollDeltaY()==0) return;
        int count=options.size(); if (count==0) return;
        selected=Math.floorMod(selected-(event.getScrollDeltaY()>0 ? 1 : -1),count); event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear(); ACTIONS.clear(); MOTIONS.clear(); GearPoses.clear(); GESTURE.reset(false); selected=0; age=0; options=List.of(); lastChoice=null; heldAtStart=ItemStack.EMPTY;
    }
    /** effects: invalidates tag-derived mounting directions after server tag synchronization. */
    @SubscribeEvent public static void tags(net.neoforged.neoforge.event.TagsUpdatedEvent event) {
        Minecraft.getInstance().execute(BackpackLayer::clear);
    }
}
