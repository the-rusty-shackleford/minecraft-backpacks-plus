/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.gametest;

import com.chunkworks.backpacksplus.*;
import com.chunkworks.quickslot.SlotData;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Test-only atomic IPC and evidence; no replacement network or storage backend. */
public final class NetworkFiles {
    private NetworkFiles() {}
    public static final boolean ENABLED=Boolean.getBoolean("backpacksplus.networkTest");
    private static Path file(String name) { return Path.of(System.getProperty("backpacksplus.testDir"),name+".json"); }
    public static JsonObject read(String name) throws IOException {
        Path p=file(name); return Files.exists(p) ? JsonParser.parseString(Files.readString(p)).getAsJsonObject() : new JsonObject();
    }
    public static int sequence(JsonObject command) { return command.has("seq") ? command.get("seq").getAsInt() : 0; }
    /** effects: ignores commands left by an earlier process, including its final quit request. */
    public static int initialSequence(String role) {
        if (!ENABLED) return 0;
        try { return sequence(read(role+"-command")); }
        catch (IOException failure) { throw new IllegalStateException("Cannot read initial test command",failure); }
    }
    public static void write(String name,JsonObject json) throws IOException {
        Path p=file(name); Files.createDirectories(p.getParent()); Path temporary=p.resolveSibling(p.getFileName()+".tmp");
        Files.writeString(temporary,new Gson().toJson(json)); Files.move(temporary,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    public static JsonObject stack(ItemStack item) {
        JsonObject json=new JsonObject(); json.addProperty("item",item.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
        json.addProperty("count",item.getCount());
        var color=item.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        if(color!=null)json.addProperty("color",color.rgb());
        var name=item.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if(name!=null)json.addProperty("name",name.getString());
        return json;
    }
    public static JsonObject bag(ItemStack bag) {
        JsonObject json=stack(bag);
        if (bag.getItem() instanceof BackpackItem) {
            json.addProperty("id",String.valueOf(bag.get(BackpackItems.ID))); json.addProperty("revision",BagContents.revision(bag));
            JsonArray cells=new JsonArray(); for (ItemStack cell : BagContents.copy(bag)) cells.add(stack(cell)); json.add("cells",cells);
        }
        return json;
    }
    public static JsonObject player(Player p) {
        JsonObject json=new JsonObject(); json.addProperty("uuid",p.getUUID().toString()); json.addProperty("entity",p.getId());
        json.addProperty("pose",p.getPose().name());json.addProperty("vehicle",p.getVehicle()==null?-1:p.getVehicle().getId());
        json.addProperty("x",p.getX());json.addProperty("y",p.getY());json.addProperty("z",p.getZ());
        json.addProperty("alive",p.isAlive()); json.addProperty("fallFlying",p.isFallFlying());
        json.addProperty("selected",p.getInventory().selected); json.addProperty("dimension",p.level().dimension().location().toString());
        json.add("held",stack(p.getMainHandItem())); json.add("bag",bag(BagLocations.stack(p,BagLocations.worn(p))));
        json.addProperty("source",BagLocations.worn(p));
        if(net.neoforged.fml.ModList.get().isLoaded("curios"))json.add("curios",CuriosNetwork.observe(p));
        json.add("quick",stack(SlotData.copy(p))); json.addProperty("menu",p.containerMenu.getClass().getSimpleName());
        json.add("cursor",stack(p.containerMenu.getCarried())); JsonArray slots=new JsonArray();
        for (var slot : p.containerMenu.slots) slots.add(stack(slot.getItem())); json.add("slots",slots); return json;
    }
}
