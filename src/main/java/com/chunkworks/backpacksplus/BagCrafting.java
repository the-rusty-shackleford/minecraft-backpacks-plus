/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus;

import com.chunkworks.backpacksplus.domain.BackpackTier;
import com.chunkworks.backpacksplus.domain.UpgradeLayout;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.common.Tags;

/**
 * AF: a pure, detached result for a validated backpack crafting grid.
 * RI: input stacks are never mutated; one bag enters and one bag leaves; all components
 * survive upgrades, with container indices remapped and revision advanced. Overflow or
 * malformed counts refuse crafting instead of truncating stored player items.
 */
final class BagCrafting {
    private BagCrafting() {}

    /** requires: ingredient shape already matched. effects: returns a new bag or EMPTY
     * for an unsafe input. No identity is assigned while previewing a recipe. */
    static ItemStack result(CraftingInput input, BackpackItem target, boolean recolor) {
        ItemStack source=ItemStack.EMPTY; DyeItem dye=null;
        for (ItemStack item:input.items()) {
            if (item.isEmpty()) continue;
            if (item.getItem() instanceof BackpackItem) {
                if (!source.isEmpty() || item.getCount()!=1) return ItemStack.EMPTY;
                source=item;
            } else if (item.getItem() instanceof DyeItem found) {
                if (dye!=null) return ItemStack.EMPTY;
                dye=found;
            } else if (item.is(Tags.Items.DYES)) return ItemStack.EMPTY;
        }
        if (recolor && (source.isEmpty() || dye==null)) return ItemStack.EMPTY;
        if (source.isEmpty()) {
            if (target.tier()!=BackpackTier.BASIC) return ItemStack.EMPTY;
            ItemStack created=new ItemStack(target);
            if (dye!=null) created.set(DataComponents.DYED_COLOR,new DyedItemColor(dye.getDyeColor().getTextureDiffuseColor(),true));
            return created;
        }
        var oldTier=BagContents.tier(source);
        if (BagContents.revision(source)<0) return ItemStack.EMPTY;
        if (recolor ? oldTier!=target.tier() : oldTier.ordinal()+1!=target.tier().ordinal()) return ItemStack.EMPTY;
        try {
            var old=BagContents.copy(source);
            if (old.stream().anyMatch(item -> !BagContents.validCount(item))) return ItemStack.EMPTY;
            long revision=Math.incrementExact(BagContents.revision(source));
            ItemStack result=source.transmuteCopy(target,1);
            if (!recolor) {
                var next=NonNullList.withSize(target.tier().totalSlots(),ItemStack.EMPTY);
                for (int i=0;i<old.size();i++) next.set(UpgradeLayout.destination(oldTier,target.tier(),i),old.get(i));
                result.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(next));
            }
            if (dye!=null) result.set(DataComponents.DYED_COLOR,new DyedItemColor(dye.getDyeColor().getTextureDiffuseColor(),true));
            result.set(BackpackItems.REVISION,revision);
            return result;
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException invalid) {
            return ItemStack.EMPTY;
        }
    }
}
