/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.GearAction;
import com.chunkworks.backpacksplus.domain.GearMotion;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

/** Short server-timed gestures layered over the current locomotion pose. No gameplay mutations. */
public final class GearPoses {
    private GearPoses() {}
    private static final HumanoidArm[] ARMS=HumanoidArm.values();
    // Client render-thread workspaces: no per-frame vectors or pose stacks.
    private static final PoseStack TARGET_POSE=new PoseStack();
    private static final Vector3f TARGET=new Vector3f();
    private static final boolean EMF=ModList.get().isLoaded("emf_compat_core");
    static boolean available(AbstractClientPlayer player) {
        return player.isAlive() && !player.isInvisible() && !player.isSpectator() && !player.isSleeping()
                && !player.isFallFlying() && !player.isVisuallySwimming();
    }
    // Cosmetic suppression only: the authoritative equipment snapshot remains available
    // to storage, input and Luminance. Held bags use their separate carry/open path.
    static boolean wornVisible(AbstractClientPlayer player) {
        var view=GearClient.view(player.getUUID());
        return view!=null && view.state.visible() && !view.bag.isEmpty()
                && !player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
    }
    static double time(AbstractClientPlayer player, float partial) { return player.level().getGameTime()+partial; }
    static GearClient.Motion exchange(AbstractClientPlayer player, float partial) {
        GearClient.Motion motion=GearClient.motion(player.getUUID());
        GearClient.View view=GearClient.view(player.getUUID());
        if (!available(player) || player.isUsingItem() || player.swinging || motion==null || !wornVisible(player)) return null;
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
        if (!wornVisible(player)) return 0;
        return opening(player,view.bag,view.state.wornSource(),partial);
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
    static GearClient.Motion retrieval(AbstractClientPlayer player, float partial) {
        var motion=GearClient.motion(player.getUUID());
        var view=GearClient.view(player.getUUID());
        if (!available(player) || player.isUsingItem() || player.swinging || motion==null || view==null
                || motion.action().kind()!=GearAction.RETRIEVE || view.state.openedAt()<0) return null;
        var action=motion.action(); double age=time(player,partial)-action.startedAt();
        if (action.entityId()!=player.getId() || !action.dimension().equals(player.level().dimension().location())
                || age<0 || age>=18) return null;
        if (wornVisible(player) && action.bag().equals(view.bag.get(BackpackItems.ID))
                && view.state.openSource()==view.state.wornSource()) return motion;
        for (HumanoidArm hand:ARMS) {
            ItemStack bag=hand==player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
            if (bag.getItem() instanceof BackpackItem && action.bag().equals(bag.get(BackpackItems.ID))
                    && view.state.openSource()==heldSource(player,hand)) return motion;
        }
        return null;
    }
    static HumanoidArm supportingArm(AbstractClientPlayer player, float partial) {
        HumanoidArm supporting=player.getMainArm().getOpposite();
        double open=opening(player,partial);
        for (HumanoidArm hand:ARMS) {
            ItemStack bag=hand==player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
            if (bag.getItem() instanceof BackpackItem) {
                double heldOpen=opening(player,bag,heldSource(player,hand),partial);
                if (heldOpen>open) { open=heldOpen; supporting=hand; }
            }
        }
        return supporting;
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
        for (HumanoidArm hand:ARMS) {
            ItemStack bag=hand==player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
            if (bag.getItem() instanceof BackpackItem && opening(player,bag,heldSource(player,hand),partial)>0) return true;
        }
        return arm==player.getMainArm() && exchange(player,partial)!=null;
    }
    private static ModelPart arm(PlayerModel<?> model, HumanoidArm arm) { return arm==HumanoidArm.LEFT ? model.leftArm : model.rightArm; }
    private static void blend(ModelPart arm, double weight, double x, double y, double z) {
        arm.xRot+=(float)((x-arm.xRot)*weight); arm.yRot+=(float)((y-arm.yRot)*weight); arm.zRot+=(float)((z-arm.zRot)*weight);
    }
    private static void aim(ModelPart arm, double weight) {
        double dx=TARGET.x-arm.x/16.0,dy=TARGET.y-arm.y/16.0,dz=TARGET.z-arm.z/16.0;
        blend(arm,weight,Math.atan2(dz,dy),0,-Math.atan2(dx,Math.hypot(dy,dz)));
    }
    private static void mountTarget(PlayerModel<?> model, GearClient.View view, int mount) {
        var tier=BagContents.tier(view.bag);
        boolean large=tier.mounts().get(mount)==com.chunkworks.backpacksplus.domain.BackpackTier.Mount.LONG;
        int small=mount-(tier==com.chunkworks.backpacksplus.domain.BackpackTier.BASIC ? 1 : 2);
        TARGET.set(large ? (mount==0 ? -0.34f : 0.34f) : small==0 ? -0.14f : 0.14f,
                large ? 0.13f : 0.46f,large ? 0.38f : 0.59f);
        TARGET_POSE.setIdentity();model.body.translateAndRotate(TARGET_POSE);
        TARGET.mulPosition(TARGET_POSE.last().pose());
    }
    /** effects: applies owned arm rotations after vanilla setup, and hands those same poses to EMF's public adapter. */
    public static void apply(PlayerModel<?> model, AbstractClientPlayer player, float partial) {
        boolean left=false,right=false;
        if (available(player)) {
            var main=player.getMainArm(); int side=main==HumanoidArm.RIGHT ? -1 : 1;
            for (HumanoidArm hand:ARMS) {
                ItemStack stack=hand==main ? player.getMainHandItem() : player.getOffhandItem();
                if (stack.getItem() instanceof BackpackItem) {
                    blend(arm(model,hand),1,-0.06,0,hand==HumanoidArm.RIGHT ? 0.12 : -0.12);
                    if (hand==HumanoidArm.LEFT) left=true; else right=true;
                }
            }
            var motion=exchange(player,partial);
            if (motion!=null) {
                double reach=GearMotion.reach(progress(player,motion,partial));
                mountTarget(model,GearClient.view(player.getUUID()),motion.action().mount());
                aim(arm(model,main),reach);
                if (main==HumanoidArm.LEFT) left=true; else right=true;
            }
            double open=opening(player,partial); boolean fromBack=open>0; HumanoidArm supporting=main.getOpposite();
            for (HumanoidArm hand:ARMS) {
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
                blend(arm(model,supporting.getOpposite()),open,-0.72,-supportSide*0.50,supportSide*0.12);
                var action=retrieval(player,partial);
                if (action!=null) {
                    double reach=GearMotion.reach((time(player,partial)-action.action().startedAt())/18);
                    TARGET_POSE.setIdentity();model.translateToHand(supporting,TARGET_POSE);
                    TARGET.set(supporting==HumanoidArm.RIGHT ? -0.0625f : 0.0625f,fromBack ? 0.625f : 0.5625f,0);
                    TARGET.mulPosition(TARGET_POSE.last().pose());
                    TARGET.add(0,0.10f,0.07f);
                    aim(arm(model,supporting.getOpposite()),open*reach);
                }
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
