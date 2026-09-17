/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;

/** Persistent horizontal mounts. RI: every mount shares one y coordinate; vanilla HUD cells retain their space. */
final class GearHud {
    private GearHud() {}
    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null || mc.options.hideGui || mc.player.isSpectator()) return;
        var view=GearClient.self(); int mounts=view==null ? 0 : view.mounts();
        if (mounts==0 && !GearClient.browsing()) return;
        boolean right=mc.player.getMainArm()==HumanoidArm.RIGHT;
        int distance=mc.options.attackIndicator().get()==AttackIndicatorStatus.HOTBAR ? 121 : 97;
        int base=g.guiWidth()/2+(right ? distance : -distance-22), bottom=g.guiHeight()-22;
        int edge=right ? base+24+mounts*24 : base-mounts*24;
        boolean compact=edge<3 || edge>g.guiWidth()-3;
        // When the side margin is too narrow, keep the mount row horizontal and align
        // its outer end with Quick Slot. Draw above the space reserved by status icons.
        int rowY=compact ? g.guiHeight()-Math.max(82,Math.max(mc.gui.leftHeight,mc.gui.rightHeight)+26) : bottom;
        if (GearClient.QUICK_SLOT && GearClient.browsing() && GearClient.selection()==0) border(g,base,bottom,0xffffda7a);
        for (int i=0;i<mounts;i++) {
            int x=compact ? base+(right ? i-mounts+1 : mounts-1-i)*24 : base+(right ? 1 : -1)*(i+1)*24;
            int y=rowY;
            boolean selected=GearClient.browsing() && GearClient.selection()==i+(GearClient.QUICK_SLOT ? 1 : 0);
            g.fill(x,y,x+22,y+22,0xdd27251f); border(g,x,y,selected ? 0xffffda7a : 0xffa09982);
            var item=view.mount(i);
            if (!item.isEmpty()) { g.renderItem(item,x+3,y+3); g.renderItemDecorations(mc.font,item,x+3,y+3); }
        }
    }
    private static void border(GuiGraphics g,int x,int y,int color) {
        g.fill(x,y,x+22,y+1,color); g.fill(x,y+21,x+22,y+22,color);
        g.fill(x,y,x+1,y+22,color); g.fill(x+21,y,x+22,y+22,color);
    }
}
