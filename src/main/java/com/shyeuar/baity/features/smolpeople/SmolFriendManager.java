package com.shyeuar.baity.features.smolpeople;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.config.BaityConfigDir;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
public final class SmolFriendManager {
    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();
    private static final String FRIENDS_FILE_NAME = "synced_players.txt";

    private SmolFriendManager() {
    }

    public static void reloadFromConfig() {
        FRIENDS.clear();

        if (loadFromFile()) {
            syncLegacyConfigField();
            return;
        }

        String serialized = ConfigManager.smolFriendList;
        if (serialized != null && !serialized.isBlank()) {
            String[] names = serialized.split(",");
            for (String name : names) {
                addNameToMemory(name);
            }
        }
        saveToFile();
        syncLegacyConfigField();
    }

    public static boolean shouldApplySmolTo(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entityId == mc.player.getId()) {
            return true;
        }
        if (!ConfigManager.smolFriendsEnabled) {
            return false;
        }

        Player targetPlayer = getPlayerByEntityId(entityId);
        return targetPlayer != null && isFriend(targetPlayer.getName().getString());
    }

    public static Player getPlayerByEntityId(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof Player player) {
            return player;
        }
        return null;
    }

    public static boolean addFriend(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }

        if (FRIENDS.containsKey(normalized)) {
            return false;
        }

        FRIENDS.put(normalized, name.trim());
        persistToConfig();
        return true;
    }

    public static boolean removeFriend(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }

        if (FRIENDS.remove(normalized) == null) {
            return false;
        }

        persistToConfig();
        return true;
    }

    public static boolean isFriend(String name) {
        String normalized = normalizeName(name);
        return normalized != null && FRIENDS.containsKey(normalized);
    }

    public static String getStoredName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return null;
        }
        return FRIENDS.get(normalized);
    }

    public static List<String> getFriends() {
        List<String> names = new ArrayList<>(FRIENDS.values());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static void persistToConfig() {
        saveToFile();
        syncLegacyConfigField();
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static void addNameToMemory(String rawName) {
        String normalized = normalizeName(rawName);
        if (normalized == null) {
            return;
        }
        FRIENDS.putIfAbsent(normalized, rawName.trim());
    }

    private static boolean loadFromFile() {
        Path filePath = getFriendsFilePath();
        if (!Files.exists(filePath)) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                String[] entries = line.split(",");
                for (String entry : entries) {
                    addNameToMemory(entry);
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("[Baity] Failed to load friend list file: " + e.getMessage());
            return false;
        }
    }

    private static void saveToFile() {
        Path filePath = getFriendsFilePath();
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, new ArrayList<>(FRIENDS.values()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Baity] Failed to save friend list file: " + e.getMessage());
        }
    }

    private static Path getFriendsFilePath() {
        return BaityConfigDir.getBaityConfigDir().resolve(FRIENDS_FILE_NAME);
    }

    private static void syncLegacyConfigField() {
        ConfigManager.smolFriendList = String.join(",", FRIENDS.values());
        ConfigManager.saveConfig();
    }
}
