/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.backpacksplus.domain.ModelBounds;
import com.chunkworks.backpacksplus.domain.GearMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.NonNullList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.Tags;

/**
 * AF: a compact pack with real rendered mount models and a server-timed handoff.
 * RI: resource geometry is measured once per immutable view stack, weakly cached;
 * no renderer mutates a stack. Model/pose quality requires actual wide/slim client capture.
 */
public final class BackpackLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private static final HumanoidArm[] ARMS=HumanoidArm.values();
    private enum MountFacing { BLADE, HEADED_TOOL, SHIELD, SMALL_TOOL, TORCH, LANTERN, DEFAULT }
    private record Display(ItemStack item, RenderedBounds.Shape shape, boolean hidden, MountFacing facing) {}
    private static final Map<ItemStack,Display> DISPLAYS=new WeakHashMap<>();
    private record HeldBag(long revision, NonNullList<ItemStack> cells) {}
    private static final Map<ItemStack,HeldBag> HELD=new WeakHashMap<>();
    private final PoseStack local=new PoseStack();
    private final Matrix4f inverseBody=new Matrix4f();
    private final Vector3f grip=new Vector3f();
    private static final ModelResourceLocation[] BODIES=new ModelResourceLocation[3], LIDS=new ModelResourceLocation[3];
    private static final ModelResourceLocation[] DYED_BODIES=new ModelResourceLocation[3], DYED_LIDS=new ModelResourceLocation[3];
    static {
        for (BackpackTier tier:BackpackTier.values()) {
            String name=tier.name().toLowerCase(java.util.Locale.ROOT);
            BODIES[tier.ordinal()]=ModelResourceLocation.standalone(BackpacksPlus.id("item/"+name+"_body"));
            LIDS[tier.ordinal()]=ModelResourceLocation.standalone(BackpacksPlus.id("item/"+name+"_lid"));
            DYED_BODIES[tier.ordinal()]=ModelResourceLocation.standalone(BackpacksPlus.id("item/"+name+"_body_dyed"));
            DYED_LIDS[tier.ordinal()]=ModelResourceLocation.standalone(BackpacksPlus.id("item/"+name+"_lid_dyed"));
        }
    }
    /** effects: binds the layer to a wide or slim renderer. */
    public BackpackLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    static void registerModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        for (var model:BODIES) event.register(model);
        for (var model:LIDS) event.register(model);
        for (var model:DYED_BODIES) event.register(model);
        for (var model:DYED_LIDS) event.register(model);
    }
    /** effects: invalidates all measured geometry after resource reload. */
    public static void clear() { DISPLAYS.clear(); HELD.clear(); }
    private static Display display(AbstractClientPlayer player, ItemStack stack, int light) {
        Display result=DISPLAYS.get(stack);
        if (result==null) {
            ItemStack item=stack.copyWithCount(1); var food=item.get(DataComponents.FOOD);
            boolean hidden=item.getCraftingRemainingItem().is(Items.BOWL)
                    || food!=null && food.usingConvertsTo().filter(s -> s.is(Items.BOWL)).isPresent();
            result=new Display(item,hidden ? null : RenderedBounds.measure(player,item,light),hidden,facing(item));
            DISPLAYS.put(stack,result);
        }
        return result;
    }
    private static MountFacing facing(ItemStack item) {
        if(item.getItem() instanceof BlockItem block) {
            if(block.getBlock() instanceof net.minecraft.world.level.block.BaseTorchBlock)return MountFacing.TORCH;
            if(block.getBlock() instanceof net.minecraft.world.level.block.LanternBlock)return MountFacing.LANTERN;
        }
        if (item.is(ItemTags.SWORDS) || item.getItem() instanceof SwordItem || item.is(Tags.Items.TOOLS_SPEAR)) return MountFacing.BLADE;
        if (item.is(ItemTags.PICKAXES) || item.is(ItemTags.AXES) || item.is(ItemTags.HOES)
                || item.is(Tags.Items.TOOLS_FISHING_ROD) || item.getItem() instanceof PickaxeItem
                || item.getItem() instanceof AxeItem || item.getItem() instanceof HoeItem) return MountFacing.HEADED_TOOL;
        if (item.is(Tags.Items.TOOLS_SHIELD)) return MountFacing.SHIELD;
        if (item.is(Tags.Items.TOOLS_BRUSH) || item.is(Tags.Items.TOOLS_SHEAR)
                || item.is(Tags.Items.TOOLS_IGNITER) || item.is(Tags.Items.TOOLS_WRENCH)) return MountFacing.SMALL_TOOL;
        return MountFacing.DEFAULT;
    }
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        var view=GearClient.view(player.getUUID());
        var motion=GearPoses.exchange(player,partial);
        double progress=motion==null ? 1 : GearPoses.progress(player,motion,partial);
        double open=GearPoses.opening(player,partial);
        int side=player.getMainArm()==HumanoidArm.RIGHT ? -1 : 1;
        if (GearPoses.wornVisible(player)) {
            pose.pushPose(); getParentModel().body.translateAndRotate(pose);
            // Keep the pack near the torso; the open path goes around the supporting side.
            if (open>0) {
                local.setIdentity(); getParentModel().translateToHand(player.getMainArm().getOpposite(),local);
                grip.set(-side*0.0625f,0.625f,0).mulPosition(local.last().pose());
                local.setIdentity(); getParentModel().body.translateAndRotate(local);
                grip.mulPosition(inverseBody.set(local.last().pose()).invert());
            }
            pose.translate(grip.x*open-side*0.20*Math.sin(Math.PI*open),0.375*(1-open)+(grip.y+0.40)*open,
                    0.36*(1-open)+(grip.z+0.0625)*open);
            pose.mulPose(Axis.YP.rotationDegrees((float)(-side*20*open)));
            renderBag(view.bag,pose,buffers,light,open);
            for (int i=0;i<view.mounts();i++) {
                ItemStack stack=view.mount(i);
                if (motion!=null && motion.action().mount()==i && progress<0.5) stack=motion.after();
                if (stack.isEmpty()) continue;
                renderMount(player,stack,BagContents.tier(view.bag),i,pose,buffers,light);
            }
            pose.popPose();
        }
        if (GearPoses.available(player)) {
            for (HumanoidArm arm:ARMS) {
                ItemStack held=arm==player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
                if (held.getItem() instanceof BackpackItem) {
                    pose.pushPose(); getParentModel().translateToHand(arm,pose);
                    pose.translate(arm==HumanoidArm.RIGHT ? -0.0625 : 0.0625,0.5625,0);
                    var part=arm==HumanoidArm.RIGHT ? getParentModel().rightArm : getParentModel().leftArm;
                    pose.mulPose(Axis.XP.rotation(-part.xRot)); pose.mulPose(Axis.YP.rotation(-part.yRot)); pose.mulPose(Axis.ZP.rotation(-part.zRot));
                    pose.translate(0,0.40,0.0625);
                    renderBag(held,pose,buffers,light,GearPoses.opening(player,held,GearPoses.heldSource(player,arm),partial));
                    HeldBag contents=HELD.get(held); long revision=BagContents.revision(held);
                    if (contents==null || contents.revision()!=revision) { contents=new HeldBag(revision,BagContents.copy(held)); HELD.put(held,contents); }
                    BackpackTier tier=BagContents.tier(held);
                    for (int i=0;i<tier.mounts().size();i++) {
                        var item=contents.cells().get(tier.mountSlot(i));
                        if (!item.isEmpty()) renderMount(player,item,tier,i,pose,buffers,light);
                    }
                    pose.popPose();
                }
            }
        }
        if (motion!=null) renderHand(player,player.getMainArm(),progress<0.5 ? motion.before() : motion.after(),pose,buffers,light,1);
        var retrieval=GearPoses.retrieval(player,partial);
        if (retrieval!=null) {
            double t=(GearPoses.time(player,partial)-retrieval.action().startedAt())/18;
            // The existing action carries the actual withdrawn stack. Show one model
            // as the hand leaves the bag, then tuck it away; gameplay already finished.
            double visible=GearMotion.ease((t-0.38)/0.14)*(1-GearMotion.ease((t-0.82)/0.18));
            if (visible>0) renderHand(player,GearPoses.supportingArm(player,partial).getOpposite(),
                    retrieval.before(),pose,buffers,light,visible);
        }
    }
    private static void renderBag(ItemStack bag, PoseStack pose, MultiBufferSource buffers, int light, double open) {
        Minecraft mc=Minecraft.getInstance(); int tier=BagContents.tier(bag).ordinal();
        boolean dyed=bag.has(DataComponents.DYED_COLOR);
        pose.pushPose(); pose.scale(1,-1,-1);
        mc.getItemRenderer().render(bag,ItemDisplayContext.NONE,false,pose,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel((dyed ? DYED_BODIES : BODIES)[tier]));
        pose.pushPose(); pose.translate(0,5.0/16,2.0/16);
        pose.mulPose(Axis.XP.rotationDegrees((float)(135*GearMotion.ease((open-0.65)/0.35))));
        pose.translate(0,-5.0/16,-2.0/16);
        mc.getItemRenderer().render(bag,ItemDisplayContext.NONE,false,pose,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel((dyed ? DYED_LIDS : LIDS)[tier]));
        pose.popPose(); pose.popPose();
    }
    private static void renderMount(AbstractClientPlayer player, ItemStack stack, BackpackTier tier, int mount,
            PoseStack pose, MultiBufferSource buffers, int light) {
        Display display=display(player,stack,light); if (display.hidden()) return;
        boolean large=tier.mounts().get(mount)==BackpackTier.Mount.LONG;
        int side=mount==0 ? -1 : 1;
        int small=mount-(tier==BackpackTier.BASIC ? 1 : 2);
        boolean torch=!large && display.facing()==MountFacing.TORCH;
        boolean lantern=!large && display.facing()==MountFacing.LANTERN;
        boolean lamp=torch || lantern;
        // Vanilla sprites include transparent padding. Match their visible height to
        // placed torches (10px) / lanterns including handle (11px), not the full quad.
        double length=torch ? (display.shape().sprite() ? 1.0 : 10.0/16)
                : lantern ? (display.shape().sprite() ? 11.0/13 : 11.0/16) : large ? 0.92 : 0.26;
        var b=display.shape().bounds(); double scale=b.fit(length);
        boolean blade=large && display.facing()==MountFacing.BLADE;
        double sideDistance=0.34;
        if (blade) {
            // Contact the side rail/pocket of the current bag, accounting for the
            // actual model thickness. A fixed outward lean leaves the tip floating.
            double surface=switch (tier) { case BASIC -> 4.0/16; case REINFORCED -> 4.5/16; case EXPEDITION -> 5.1/16; };
            double thickness=switch (b.plane()) { case XY -> b.depth(); case YZ -> b.width(); case XZ -> b.height(); };
            // The guard/pommel set the bounds, but the blade is thinner. Seat swords
            // half a model pixel into that clearance so the blade contacts the bag.
            double inset=(display.item().is(ItemTags.SWORDS) || display.item().getItem() instanceof SwordItem) ? 0.5/16 : 0;
            sideDistance=surface+thickness*scale/2+1.0/512-inset;
        }
        pose.pushPose();
        double smallSide=lantern ? 0.20 : torch ? 0.12 : 0.14;
        // Keep the larger lamp against the outer pocket, with room for two adjacent
        // lanterns. Its back surface touches the bag rather than sinking into it.
        pose.translate(large ? side*sideDistance : (small==0 ? -smallSide : smallSide),large ? 0 : lamp ? 0.02 : 0.10,
                large ? 0.02 : lamp ? 0.245+b.depth()*scale/2 : 0.22);
        pose.scale(1,-1,-1);
        boolean shield=display.facing()==MountFacing.SHIELD;
        pose.mulPose(Axis.ZP.rotationDegrees(large ? (shield || blade ? 0 : side*8) : lamp ? 0 : small==0 ? -8 : 8));
        // Blades point down with the grip accessible above the bag. Turn the broad
        // heads of 3-D tools along its side, instead of out across the player's arm.
        // Sprite tools stay against the surface: mirror their head inward, never edge-on.
        if (large && display.facing()==MountFacing.HEADED_TOOL)
            pose.mulPose(Axis.YP.rotationDegrees(display.shape().sprite() ? (side<0 ? 180 : 0) : side*90));
        if (large && shield) pose.mulPose(Axis.YP.rotationDegrees(side*90));
        if (blade) {
            pose.mulPose(Axis.YP.rotationDegrees(side*90));
            pose.mulPose(Axis.ZP.rotationDegrees(180));
        }
        if (large && display.shape().sprite()) pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.scale((float)scale,(float)scale,(float)scale);
        if (!large && display.facing()==MountFacing.SMALL_TOOL && b.depth()>b.height()*1.4 && b.depth()>b.width()*1.4) {
            // Some 3-D brushes are modeled handle-first along Z, with a square
            // cross-section. Their longest axis, not a thin face, determines upright.
            pose.mulPose(Axis.XP.rotationDegrees(90));
        } else if (large || display.facing()==MountFacing.SMALL_TOOL) {
            if (b.plane()==ModelBounds.Plane.YZ) pose.mulPose(Axis.YP.rotationDegrees(90));
            if (b.plane()==ModelBounds.Plane.XZ) pose.mulPose(Axis.XP.rotationDegrees(90));
        }
        double padding=display.shape().sprite() ? torch ? 3.0/16 : lantern ? -0.5/16 : 0 : 0;
        pose.translate(-b.centerX(),-b.centerY()+padding,-b.centerZ());
        Minecraft.getInstance().getItemRenderer().renderStatic(player,display.item(),ItemDisplayContext.NONE,false,pose,buffers,player.level(),light,OverlayTexture.NO_OVERLAY,player.getId());
        pose.popPose();
    }
    private void renderHand(AbstractClientPlayer player, HumanoidArm hand, ItemStack item, PoseStack pose,
            MultiBufferSource buffers, int light, double scale) {
        if (item.isEmpty()) return;
        boolean left=hand==HumanoidArm.LEFT;
        pose.pushPose(); getParentModel().translateToHand(hand,pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90)); pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate((left ? -1 : 1)/16.0,0.125,-0.625);
        pose.scale((float)scale,(float)scale,(float)scale);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player,item,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,left,pose,buffers,light);
        pose.popPose();
    }
}
