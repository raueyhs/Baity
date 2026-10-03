package com.shyeuar.baity.compatibility;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;

@Environment(EnvType.CLIENT)
public final class HypixelModApiLocation {

    private static final String MOD_ID = "hypixel-mod-api";

    private static volatile String serverName = "";
    private static volatile String serverType = "";
    private static volatile String lobbyName = "";
    private static volatile String mode = "";
    private static volatile String map = "";
    private static volatile boolean received;

    private HypixelModApiLocation() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean hasData() {
        return received;
    }

    public static String mode() {
        return mode;
    }

    public static String serverName() {
        return serverName;
    }

    public static String serverType() {
        return serverType;
    }

    public static String lobbyName() {
        return lobbyName;
    }

    public static String map() {
        return map;
    }

    public static void init() {
        if (!isAvailable()) {
            return;
        }
        try {
            HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
            HypixelModAPI.getInstance().registerHandler(ClientboundLocationPacket.class, HypixelModApiLocation::apply);
        } catch (Throwable ignored) {
        }
    }

    private static void apply(ClientboundLocationPacket packet) {
        if (packet == null) {
            return;
        }
        serverName = packet.getServerName();
        serverType = packet.getServerType().map(ServerType::name).orElse("");
        lobbyName = packet.getLobbyName().orElse("");
        mode = packet.getMode().orElse("");
        map = packet.getMap().orElse("");
        received = true;
    }
}
