package com.shyeuar.baity.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CustomTotemItem {
    
    public static final Item CUSTOM_TOTEM;
    
    static {
        Identifier id = Identifier.of("baity", "custom_totem");
        RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, id);
        CUSTOM_TOTEM = Registry.register(Registries.ITEM, id, new Item(new Item.Settings().registryKey(registryKey)));
    }
    
    public static void register() {
        System.out.println("custom totem item registered: " + CUSTOM_TOTEM);
    }
}

