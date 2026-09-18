/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.client;

import com.chunkworks.backpacksplus.BackpackItems;
import com.chunkworks.backpacksplus.BackpacksPlus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Canvas-only tint shared by inventory, held, worn and opening models. RI: the
 * synchronized vanilla color component is read without mutation or per-frame allocation. */
@EventBusSubscriber(modid=BackpacksPlus.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BackpackColors {
    private BackpackColors() {}
    /** effects: binds tint index zero to canvas; natural leather and metal remain untinted. */
    @SubscribeEvent public static void register(RegisterColorHandlersEvent.Item event) {
        event.register((stack,index) -> {
            return index==0 ? DyedItemColor.getOrDefault(stack,-1) : -1;
        },BackpackItems.BASIC.get(),BackpackItems.REINFORCED.get(),BackpackItems.EXPEDITION.get());
    }
    /** effects: selects neutral canvas only on explicitly dyed inventory/hand models. */
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var item:java.util.List.of(BackpackItems.BASIC.get(),BackpackItems.REINFORCED.get(),BackpackItems.EXPEDITION.get()))
                ItemProperties.register(item,BackpacksPlus.id("dyed"),
                        (stack,level,entity,seed) -> stack.has(DataComponents.DYED_COLOR) ? 1 : 0);
        });
    }
}
