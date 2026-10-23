package com.shyeuar.baity.utils;

import com.shyeuar.baity.mixin.accessor.ClientLevelAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;

/**
 * TODO(migration-26.3): 26.3 builds the weather attribute layers once, when the client level is
 * created, so toggling "remove weather filter" only took effect after rejoining the world.
 * This rebuilds the level attribute system when the setting changes; drop it once the toggle
 * no longer needs to apply instantly.
 */
@Environment(EnvType.CLIENT)
public final class WeatherAttributeSync {

    private static Boolean lastRemoved;

    private WeatherAttributeSync() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.level == null) {
            lastRemoved = null;
            return;
        }
        boolean removed = ModuleUtils.shouldRemoveCullingWeatherFilter();
        if (lastRemoved != null && lastRemoved == removed) {
            return;
        }
        lastRemoved = removed;
        rebuild(client.level);
    }

    private static void rebuild(ClientLevel level) {
        ClientLevelAccessor accessor = (ClientLevelAccessor) level;
        EnvironmentAttributeSystem.Builder builder =
                accessor.baity$addEnvironmentAttributeLayers(EnvironmentAttributeSystem.builder());
        accessor.baity$setEnvironmentAttributes(builder.build());
    }
}
