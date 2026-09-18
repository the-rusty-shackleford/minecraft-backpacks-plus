/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.domain.GearChoices;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * AF: persistent mounts plus explicitly selected storage shortcuts during a G gesture.
 * RI: mount cells share one horizontal row; vanilla hotbar, offhand and status space stay clear.
 * Frames use the active resource pack's vanilla sprites. Rendering never simulates storage.
 */
final class GearHud {
    private static final ResourceLocation HOTBAR=ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation SELECTED=ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final Component RELEASE_SWAP=Component.translatable("backpacksplus.release_swap");
    private static final Component RELEASE_STOW=Component.translatable("backpacksplus.release_stow");
    private GearHud() {}

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null || mc.options.hideGui || mc.player.isSpectator()) return;
        var view=GearClient.self(); int mounts=view==null ? 0 : view.mounts();
        if (mounts==0 && !GearClient.browsing()) return;
        boolean right=mc.player.getMainArm()==HumanoidArm.RIGHT;
        int direction=right ? 1 : -1;
        int distance=mc.options.attackIndicator().get()==AttackIndicatorStatus.HOTBAR ? 121 : 97;
        int base=g.guiWidth()/2+(right ? distance : -distance-22), bottom=g.guiHeight()-22;
        boolean heldAction=false, mountActions=false;
        if (GearClient.browsing()) for (var option:GearClient.options()) {
            heldAction|=option.choice().kind()==GearChoices.Kind.STOW_HELD;
            mountActions|=option.choice().kind()==GearChoices.Kind.STOW_MOUNT;
        }
        int columns=mounts+(heldAction ? 1 : 0);
        int edge=right ? base+24+columns*24 : base-columns*24;
        boolean compact=edge<3 || edge>g.guiWidth()-3;
        int rowY=compact ? g.guiHeight()-Math.max(82,Math.max(mc.gui.leftHeight,mc.gui.rightHeight)+26) : bottom;
        int first=compact ? base-direction*(mounts-1)*24 : base+direction*24;
        int last=first+direction*Math.max(0,columns-1)*24;
        int low=Math.min(first,last), high=Math.max(first,last)+22;
        first+=Math.max(0,3-low)-Math.max(0,high-(g.guiWidth()-3));

        if (GearClient.selected(GearChoices.Kind.QUICK,-1)) selection(g,base,bottom,false);
        for (int i=0;i<mounts;i++) {
            int x=first+direction*i*24;
            frame(g,x,rowY);
            var item=view.mount(i);
            if (!item.isEmpty()) { g.renderItem(item,x+3,rowY+3); g.renderItemDecorations(mc.font,item,x+3,rowY+3); }
            if (GearClient.selected(GearChoices.Kind.MOUNT,i)) selection(g,x,rowY,GearClient.option().reason()!=null);
        }
        if (!GearClient.browsing()) return;
        for (var option:GearClient.options()) {
            var choice=option.choice();
            if (choice.kind()!=GearChoices.Kind.STOW_MOUNT && choice.kind()!=GearChoices.Kind.STOW_HELD) continue;
            boolean held=choice.kind()==GearChoices.Kind.STOW_HELD;
            int x=first+direction*(held ? mounts : choice.mount())*24, y=held ? rowY : rowY-34;
            frame(g,x,y); g.renderItem(view.bag,x+3,y+3);
            g.drawString(mc.font,held ? "\u2193" : "\u2191",x+8,held ? y-11 : y+24,0xffeeeeee,true);
            if (GearClient.selected(choice.kind(),choice.mount())) selection(g,x,y,option.reason()!=null);
        }
        var selected=GearClient.option();
        if (selected==null) return;
        int center=first+direction*Math.max(0,columns-1)*12+11;
        int textY=rowY-(mountActions ? 60 : 26);
        text(g,mc,selected.title(),center,textY,0xffffffff);
        Component detail=selected.reason()!=null ? selected.reason()
                : selected.choice().kind()==GearChoices.Kind.STOW_MOUNT || selected.choice().kind()==GearChoices.Kind.STOW_HELD
                ? RELEASE_STOW : RELEASE_SWAP;
        text(g,mc,detail,center,textY+11,selected.reason()!=null ? 0xffff7777 : 0xffd0d0d0);
    }

    private static void text(GuiGraphics g,Minecraft mc,Component text,int center,int y,int color) {
        int width=mc.font.width(text), x=Math.max(4,Math.min(g.guiWidth()-width-4,center-width/2));
        g.fill(x-2,y-2,x+width+2,y+10,0xb0000000);
        g.drawString(mc.font,text,x,y,color,true);
    }

    private static void frame(GuiGraphics g,int x,int y) {
        RenderSystem.enableBlend();
        g.blitSprite(HOTBAR,182,22,0,0,x,y,11,22);
        g.blitSprite(HOTBAR,182,22,171,0,x+11,y,11,22);
        RenderSystem.disableBlend();
    }

    private static void selection(GuiGraphics g,int x,int y,boolean refused) {
        RenderSystem.enableBlend();
        if (refused) g.setColor(1,0.3f,0.3f,1);
        g.blitSprite(SELECTED,x-1,y-1,24,23);
        if (refused) g.setColor(1,1,1,1);
        RenderSystem.disableBlend();
    }
}
