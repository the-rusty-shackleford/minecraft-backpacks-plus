/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/** Common-side registry composition; no client types are loaded here. */
public final class BackpackItems {
    private BackpackItems() {}
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BackpacksPlus.ID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BackpacksPlus.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BackpacksPlus.ID);
    public static final DeferredItem<BackpackItem> BASIC = register("basic_backpack", BackpackTier.BASIC);
    public static final DeferredItem<BackpackItem> REINFORCED = register("reinforced_backpack", BackpackTier.REINFORCED);
    public static final DeferredItem<BackpackItem> EXPEDITION = register("expedition_backpack", BackpackTier.EXPEDITION);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ID = COMPONENTS.registerComponentType("bag_id",
            b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> REVISION = COMPONENTS.registerComponentType("revision",
            b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> MENU = MENUS.register("backpack",
            () -> IMenuTypeExtension.create(BackpackMenu::fromNetwork));
    private static DeferredItem<BackpackItem> register(String id, BackpackTier tier) {
        return ITEMS.register(id, () -> new BackpackItem(new Item.Properties().stacksTo(1), tier));
    }
    /** effects: registers this mod's items, item data and menu type on the mod bus. */
    public static void register(IEventBus bus) { ITEMS.register(bus); COMPONENTS.register(bus); MENUS.register(bus); }
}
