/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Optional common-side Curios adapter, loaded only after the mod-presence check.
 * AF: functional back equipment is a native source for the existing bag operations.
 * RI: only this mod's items receive capabilities; no other mod's storage or files are
 * changed. Curios owns persistence, equipment sync and its normal death/drop policy.
 */
public final class CuriosCompat {
    private CuriosCompat() {}
    /** requires: Curios present. effects: installs this mod's optional item capabilities. */
    public static void register(IEventBus bus) { bus.addListener(CuriosCompat::capabilities); }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(CuriosCapability.ITEM,(stack,context) -> new ICurio() {
            @Override public ItemStack getStack() { return stack; }
            @Override public boolean canEquip(SlotContext slot) {
                return slot.identifier().equals("back") && !slot.cosmetic() && BagLocations.isBag(stack);
            }
            @Override public boolean canUnequip(SlotContext slot) {
                return !(slot.entity() instanceof Player player && player.containerMenu instanceof BackpackMenu menu
                        && menu.source()==BagLocations.CURIOS_BACK+slot.index() && menu.stillValid(player));
            }
            // Leave canEquipFromUse false: right-click opens the held backpack.
        },BackpackItems.BASIC.get(),BackpackItems.REINFORCED.get(),BackpackItems.EXPEDITION.get());
    }
    private static ICurioStacksHandler back(Player player) {
        var inventory=CuriosApi.getCuriosInventory(player).orElse(null);
        return inventory==null ? null : inventory.getCurios().get("back");
    }
    private static boolean active(ICurioStacksHandler back,int slot) {
        return back!=null && slot>=0 && slot<back.getStacks().getSlots()
                && slot<back.getActiveStates().size() && back.getActiveStates().get(slot);
    }
    static ItemStack stack(Player player,int slot) {
        var back=back(player);return active(back,slot) ? back.getStacks().getStackInSlot(slot) : ItemStack.EMPTY;
    }
    static int firstBack(Player player) {
        var back=back(player);if(back==null)return -1;
        for(int slot=0;slot<back.getStacks().getSlots();slot++)
            if(active(back,slot) && BagLocations.isBag(back.getStacks().getStackInSlot(slot)))return slot;
        return -1;
    }
    static boolean visible(Player player,int slot) {
        var back=back(player);
        return active(back,slot) && slot<back.getRenders().size() && back.getRenders().get(slot)
                && (slot>=back.getCosmeticStacks().getSlots() || back.getCosmeticStacks().getStackInSlot(slot).isEmpty());
    }
    static void changed(Player player,int slot) {
        var back=back(player);
        if(active(back,slot))back.getStacks().setStackInSlot(slot,back.getStacks().getStackInSlot(slot));
    }
}
