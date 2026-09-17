/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.GearAction;
import com.chunkworks.backpacksplus.domain.GearMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Short server-timed gestures layered over the current locomotion pose. No gameplay mutations. */
public final class GearPoses {
    private GearPoses() {}
    private static final boolean EMF=ModList.get().isLoaded("emf_compat_core");
    static boolean available(AbstractClientPlayer player) {
        return player.isAlive() && !player.isInvisible() && !player.isSpectator() && !player.isSleeping()
                && !player.isFallFlying() && !player.isVisuallySwimming();
    }
    static double time(AbstractClientPlayer player, float partial) { return player.level().getGameTime()+partial; }
    static GearClient.Motion exchange(AbstractClientPlayer player, float partial) {
        GearClient.Motion motion=GearClient.motion(player.getUUID());
        GearClient.View view=GearClient.view(player.getUUID());
        if (!available(player) || player.isUsingItem() || player.swinging || motion==null || view==null || view.bag.isEmpty()) return null;
        var action=motion.action(); double age=time(player,partial)-action.startedAt();
        return GearMotion.exchange(action.kind()) && action.entityId()==player.getId()
                && action.dimension().equals(player.level().dimension().location())
                && action.bag().equals(view.bag.get(BackpackItems.ID)) && action.mount()>=0 && action.mount()<view.mounts()
                && ItemStack.matches(player.getMainHandItem(),motion.after()) && age>=0 && age<GearMotion.EXCHANGE_TICKS ? motion : null;
    }
    static double progress(AbstractClientPlayer player, GearClient.Motion motion, float partial) {
        return (time(player,partial)-motion.action().startedAt())/GearMotion.EXCHANGE_TICKS;
    }
    static double opening(AbstractClientPlayer player, float partial) {
        var view=GearClient.view(player.getUUID());
        if (view==null || view.bag.isEmpty()) return 0;
        return opening(player,view.bag,38,partial);
    }
    static double opening(AbstractClientPlayer player, ItemStack bag, int source, float partial) {
        if (!available(player) || player.isUsingItem() || player.swinging) return 0;
        var view=GearClient.view(player.getUUID());
        if (view==null) return 0;
        if (view.state.openedAt()>=0 && view.state.openSource()==source)
            return GearMotion.ease((time(player,partial)-view.state.openedAt())/GearMotion.OPEN_TICKS);
        var motion=GearClient.motion(player.getUUID());
        if (motion!=null && motion.action().kind()==GearAction.CLOSE && motion.action().bag().equals(bag.get(BackpackItems.ID)))
            return GearMotion.closing(view.lastOpenedAt<0 ? GearMotion.OPEN_TICKS : motion.action().startedAt()-view.lastOpenedAt,
                    time(player,partial)-motion.action().startedAt());
        return 0;
    }
    static int heldSource(AbstractClientPlayer player, HumanoidArm arm) {
        if (arm!=player.getMainArm()) return 40;
        var view=GearClient.view(player.getUUID());
        // Remote players' selected hotbar index is not part of vanilla equipment sync.
        if (view!=null && view.state.openSource()>=0 && view.state.openSource()<9) return view.state.openSource();
        return player.getInventory().selected;
    }
    /** effects: returns whether our layer owns this hand for a bag carry or an active gear exchange. */
    public static boolean ownsHand(AbstractClientPlayer player, HumanoidArm arm) {
        if (!available(player)) return false;
        ItemStack held=arm==player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
        float partial=Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        if (held.getItem() instanceof BackpackItem || opening(player,partial)>0) return true;
        for (HumanoidArm hand:HumanoidArm.values()) {
            ItemStack bag=hand==player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
            if (bag.getItem() instanceof BackpackItem && opening(player,bag,heldSource(player,hand),partial)>0) return true;
        }
        return arm==player.getMainArm() && exchange(player,partial)!=null;
    }
    private static ModelPart arm(PlayerModel<?> model, HumanoidArm arm) { return arm==HumanoidArm.LEFT ? model.leftArm : model.rightArm; }
    private static void blend(ModelPart arm, double weight, double x, double y, double z) {
        arm.xRot+=(float)((x-arm.xRot)*weight); arm.yRot+=(float)((y-arm.yRot)*weight); arm.zRot+=(float)((z-arm.zRot)*weight);
    }
    /** effects: applies owned arm rotations after vanilla setup, and hands those same poses to EMF's public adapter. */
    public static void apply(PlayerModel<?> model, AbstractClientPlayer player, float partial) {
        boolean left=false,right=false;
        if (available(player)) {
            var main=player.getMainArm(); int side=main==HumanoidArm.RIGHT ? -1 : 1;
            for (HumanoidArm hand:HumanoidArm.values()) {
                ItemStack stack=hand==main ? player.getMainHandItem() : player.getOffhandItem();
                if (stack.getItem() instanceof BackpackItem) {
                    blend(arm(model,hand),1,-0.06,0,hand==HumanoidArm.RIGHT ? 0.12 : -0.12);
                    if (hand==HumanoidArm.LEFT) left=true; else right=true;
                }
            }
            var motion=exchange(player,partial);
            if (motion!=null) {
                double reach=GearMotion.reach(progress(player,motion,partial));
                blend(arm(model,main),reach,1.65,-side*0.30,side*0.12);
                if (main==HumanoidArm.LEFT) left=true; else right=true;
            }
            double open=opening(player,partial); boolean fromBack=open>0; HumanoidArm supporting=main.getOpposite();
            for (HumanoidArm hand:HumanoidArm.values()) {
                ItemStack held=hand==main ? player.getMainHandItem() : player.getOffhandItem();
                if (held.getItem() instanceof BackpackItem) {
                    double heldOpen=opening(player,held,heldSource(player,hand),partial);
                    if (heldOpen>open) { open=heldOpen; supporting=hand; fromBack=false; }
                }
            }
            if (open>0 && !player.isUsingItem() && !player.swinging) {
                int supportSide=supporting==HumanoidArm.RIGHT ? -1 : 1;
                double forward=fromBack ? GearMotion.ease((open-0.25)/0.75) : 1;
                blend(arm(model,supporting),fromBack ? Math.min(1,open/0.25) : open,
                        1.75-2.77*forward,-supportSide*0.4*(1-forward),-supportSide*0.14);
                double retrieve=0; var action=GearClient.motion(player.getUUID());
                if (action!=null && action.action().kind()==GearAction.RETRIEVE)
                    retrieve=GearMotion.reach((time(player,partial)-action.action().startedAt())/18);
                blend(arm(model,supporting.getOpposite()),open,-0.72-retrieve*0.65,-supportSide*0.50,supportSide*0.12);
                left=true; right=true;
            }
        }
        if (left) model.leftSleeve.copyFrom(model.leftArm);
        if (right) model.rightSleeve.copyFrom(model.rightArm);
        if (EMF) EmfPoseCompat.publish(player.getUUID(),left ? model.leftArm : null,right ? model.rightArm : null);
    }
    /** effects: drops cached resource geometry and this mod's optional pose claims on disconnect. */
    public static void clear() { BackpackLayer.clear(); if (EMF) EmfPoseCompat.clear(); }
}
