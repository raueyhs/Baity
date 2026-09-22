package com.shyeuar.baity.features.smolpeople;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.sync.BaityPresenceSync;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class SmolFriendManager {
    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();
    private static final String FRIENDS_FILE_NAME = "smol-friends.txt";
    private static final Pattern VALID_PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern MOB_PET_NAMETAG_PREFIX = Pattern.compile("^\\[Lv\\d+]", Pattern.CASE_INSENSITIVE);
    private static final long LOBBY_PLAYERS_REFRESH_INTERVAL_MS = 500L;
    private static final double MIRROR_ARMOR_STAND_HORIZONTAL_HALF_EXTENT = 1.0;
    private static final double MIRROR_ARMOR_STAND_COLUMN_HEIGHT_BLOCKS = 4.0;
    private static List<String> cachedLobbyPlayers = List.of();
    private static long lastLobbyPlayersRefreshTime = 0L;

    private record SmolMirrorSource(UUID uuid, Player player, PlayerInfo tabInfo) {
    }

    private record ProfileIdCacheEntry(String textureValue, UUID profileId) {
    }

    private static final Map<UUID, ProfileIdCacheEntry> PROFILE_ID_CACHE = new ConcurrentHashMap<>();
    private static final UUID UNSET_SKIN_OWNER_ID = new UUID(0L, 0L);

    private SmolFriendManager() {
    }

    public static void reloadFromConfig() {
        FRIENDS.clear();

        if (loadFromFile()) {
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
    }

    public static boolean shouldApplySmolTo(int entityId) {
        if (!ConfigManager.smolpeopleMode && !BaityPresenceSync.hasRemoteSmolUser()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entityId == mc.player.getId()) {
            return ConfigManager.smolpeopleMode;
        }

        Player targetPlayer = getPlayerByEntityId(entityId);
        if (targetPlayer == null) {
            return false;
        }

        if (isMirrorOfAnySmolSource(targetPlayer)) {
            return true;
        }

        Boolean remotePreference = BaityPresenceSync.getRemoteSmolPreference(targetPlayer.getUUID());
        if (remotePreference != null) {
            return remotePreference;
        }

        if (!ConfigManager.smolpeopleMode || !ConfigManager.smolFriendsEnabled) {
            return false;
        }

        return isFriend(targetPlayer.getName().getString());
    }

    public static boolean isMirrorNametagArmorStand(int entityId) {
        if (!ConfigManager.smolpeopleMode && !BaityPresenceSync.hasRemoteSmolUser()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return false;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof ArmorStand armorStand)) {
            return false;
        }
        if (!isNametagArmorStandCandidate(armorStand)) {
            return false;
        }

        String standDisplayName = getArmorStandDisplayName(armorStand);
        for (SmolMirrorSource source : collectSmolMirrorSources(mc)) {
            if (!nametagDisplayNamesMatch(getSourceDisplayName(source), standDisplayName)) {
                continue;
            }
            for (Player mirrorPlayer : mc.level.players()) {
                if (mirrorPlayer == source.player()) {
                    continue;
                }
                UUID candidateSkinOwnerId = resolveSkinOwnerId(mirrorPlayer);
                if (candidateSkinOwnerId == null || !isMirrorOf(source, mirrorPlayer, candidateSkinOwnerId)) {
                    continue;
                }
                if (isWithinMirrorArmorStandRadius(mirrorPlayer, armorStand)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMirrorOfAnySmolSource(Player candidate) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || candidate == null) {
            return false;
        }
        UUID candidateSkinOwnerId = resolveSkinOwnerId(candidate);
        if (candidateSkinOwnerId == null) {
            return false;
        }
        for (SmolMirrorSource source : collectSmolMirrorSources(mc)) {
            if (isMirrorOf(source, candidate, candidateSkinOwnerId)) {
                return true;
            }
        }
        return false;
    }

    private static UUID resolveSkinOwnerId(Player player) {
        UUID profileId = getSkinTextureProfileId(player);
        return profileId == UNSET_SKIN_OWNER_ID ? null : profileId;
    }

    private static List<SmolMirrorSource> collectSmolMirrorSources(Minecraft mc) {
        List<SmolMirrorSource> sources = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        if (mc.player != null && ConfigManager.smolpeopleMode) {
            sources.add(new SmolMirrorSource(mc.player.getUUID(), mc.player, null));
            seen.add(mc.player.getUUID());
        }
        if (mc.getConnection() == null) {
            return sources;
        }
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info == null || info.getProfile() == null) {
                continue;
            }
            UUID uuid = info.getProfile().id();
            if (uuid == null || !seen.add(uuid)) {
                continue;
            }
            if (!shouldSmolifySourcePlayer(mc, uuid, info.getProfile().name())) {
                continue;
            }
            Player worldPlayer = mc.level != null ? mc.level.getPlayerByUUID(uuid) : null;
            sources.add(new SmolMirrorSource(uuid, worldPlayer, info));
        }
        return sources;
    }

    private static boolean shouldSmolifySourcePlayer(Minecraft mc, UUID uuid, String name) {
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            return ConfigManager.smolpeopleMode;
        }
        Boolean remotePreference = BaityPresenceSync.getRemoteSmolPreference(uuid);
        if (remotePreference != null) {
            return remotePreference;
        }
        return ConfigManager.smolpeopleMode && ConfigManager.smolFriendsEnabled && isFriend(name);
    }

    private static boolean isMirrorOf(SmolMirrorSource source, Player candidate, UUID candidateSkinOwnerId) {
        if (source == null || candidate == null || candidate == source.player()) {
            return false;
        }
        if (candidateSkinOwnerId.equals(source.uuid())) {
            return true;
        }
        if (source.player() != null) {
            return matchesLegacyMirrorPath(source.player(), candidate);
        }
        if (source.tabInfo() != null) {
            return matchesLegacyMirrorPathFromTab(source.tabInfo(), candidate);
        }
        return false;
    }

    private static boolean matchesLegacyMirrorPath(Player source, Player other) {
        if (other.getUUID().equals(source.getUUID())) {
            return true;
        }

        String sourceName = source.getGameProfile().name();
        String otherName = other.getGameProfile().name();
        if (namesMatchMirror(sourceName, otherName)) {
            return true;
        }

        if (matchesVisibleName(source, other)) {
            return true;
        }

        if (!(source instanceof AbstractClientPlayer sourceClient)) {
            return false;
        }
        if (!(other instanceof AbstractClientPlayer otherClient)) {
            return false;
        }

        var sourceSkin = sourceClient.getSkin();
        var otherSkin = otherClient.getSkin();
        if (sourceSkin == null || otherSkin == null
                || sourceSkin.body() == null || otherSkin.body() == null) {
            return false;
        }
        if (!sourceSkin.body().texturePath().equals(otherSkin.body().texturePath())) {
            return false;
        }
        return !isListedInTab(other.getUUID());
    }

    private static boolean matchesLegacyMirrorPathFromTab(PlayerInfo info, Player other) {
        if (info.getProfile() == null) {
            return false;
        }
        UUID sourceUuid = info.getProfile().id();
        if (sourceUuid != null && other.getUUID().equals(sourceUuid)) {
            return true;
        }

        String sourceName = info.getProfile().name();
        String otherName = other.getGameProfile().name();
        if (namesMatchMirror(sourceName, otherName)) {
            return true;
        }

        String tabDisplayName = info.getTabListDisplayName() != null
                ? LocateUtils.toPlainText(info.getTabListDisplayName().getString())
                : null;
        if (tabDisplayName != null) {
            for (String otherVisible : collectVisibleNames(other)) {
                if (namesMatchMirror(tabDisplayName, otherVisible)) {
                    return true;
                }
            }
        }

        String sourceTexture = getTexturesPropertyValue(info.getProfile());
        String otherTexture = getTexturesPropertyValue(other.getGameProfile());
        if (sourceTexture == null || otherTexture == null || !sourceTexture.equals(otherTexture)) {
            return false;
        }
        return !isListedInTab(other.getUUID());
    }

    private static boolean isListedInTab(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || uuid == null) {
            return false;
        }
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            if (entry != null && entry.getProfile() != null && uuid.equals(entry.getProfile().id())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesVisibleName(Player self, Player other) {
        for (String selfVisible : collectVisibleNames(self)) {
            for (String otherVisible : collectVisibleNames(other)) {
                if (namesMatchMirror(selfVisible, otherVisible)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String[] collectVisibleNames(Player player) {
        String plainName = LocateUtils.toPlainText(player.getName().getString());
        String displayName = player.getDisplayName() != null
            ? LocateUtils.toPlainText(player.getDisplayName().getString())
            : null;
        if (displayName != null && displayName.equals(plainName)) {
            return new String[] { plainName };
        }
        return new String[] { plainName, displayName };
    }

    private static boolean namesMatchMirror(String selfName, String otherName) {
        if (selfName == null || selfName.isBlank() || otherName == null || otherName.isBlank()) {
            return false;
        }
        return selfName.equalsIgnoreCase(otherName)
            || otherName.equalsIgnoreCase(reverse(selfName));
    }

    private static String reverse(String value) {
        return new StringBuilder(value).reverse().toString();
    }

    private static UUID getSkinTextureProfileId(Player player) {
        if (player == null) {
            return UNSET_SKIN_OWNER_ID;
        }
        UUID uuid = player.getUUID();
        String textureValue = getTexturesPropertyValue(player.getGameProfile());
        ProfileIdCacheEntry cached = PROFILE_ID_CACHE.get(uuid);
        if (cached != null && cached.textureValue().equals(textureValue)) {
            UUID cachedProfileId = cached.profileId();
            return cachedProfileId != null ? cachedProfileId : UNSET_SKIN_OWNER_ID;
        }
        if (textureValue == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
                if (info != null && info.getProfile() != null) {
                    textureValue = getTexturesPropertyValue(info.getProfile());
                }
            }
        }
        UUID profileId = textureValue != null ? parseProfileIdFromTextureValue(textureValue) : null;
        PROFILE_ID_CACHE.put(uuid, new ProfileIdCacheEntry(textureValue, profileId));
        return profileId != null ? profileId : UNSET_SKIN_OWNER_ID;
    }

    private static String getTexturesPropertyValue(GameProfile profile) {
        if (profile == null || profile.properties() == null) {
            return null;
        }
        var textures = profile.properties().get("textures");
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        for (Property property : textures) {
            if (property != null && property.value() != null && !property.value().isBlank()) {
                return property.value();
            }
        }
        return null;
    }

    private static UUID parseProfileIdFromTextureValue(String base64Value) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (!object.has("profileId") || object.get("profileId").isJsonNull()) {
                return null;
            }
            return parseUuidFlexible(object.get("profileId").getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuidFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim();
        if (!id.contains("-") && id.length() == 32) {
            id = id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                    + "-" + id.substring(16, 20) + "-" + id.substring(20, 32);
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isWithinMirrorArmorStandRadius(Player mirrorPlayer, ArmorStand armorStand) {
        double dx = armorStand.getX() - mirrorPlayer.getX();
        double dy = armorStand.getY() - mirrorPlayer.getY();
        double dz = armorStand.getZ() - mirrorPlayer.getZ();
        if (Math.abs(dx) > MIRROR_ARMOR_STAND_HORIZONTAL_HALF_EXTENT
                || Math.abs(dz) > MIRROR_ARMOR_STAND_HORIZONTAL_HALF_EXTENT) {
            return false;
        }
        double playerCenterYOffset = 1.0;
        double halfHeight = MIRROR_ARMOR_STAND_COLUMN_HEIGHT_BLOCKS * 0.5;
        return dy >= playerCenterYOffset - halfHeight
            && dy <= playerCenterYOffset + halfHeight;
    }

    private static boolean isNametagArmorStandCandidate(ArmorStand armorStand) {
        return armorStand.isInvisible()
            && armorStand.hasCustomName()
            && !isExcludedNametagArmorStand(armorStand);
    }

    private static boolean isExcludedNametagArmorStand(ArmorStand armorStand) {
        String standName = getArmorStandDisplayName(armorStand);
        if (standName.isBlank()) {
            return true;
        }
        if (standName.equalsIgnoreCase("CLICK")) {
            return true;
        }
        if (standName.equalsIgnoreCase("Armor Stand")) {
            return true;
        }
        return MOB_PET_NAMETAG_PREFIX.matcher(standName).find() || standName.indexOf('\u2764') >= 0;
    }

    private static String getArmorStandDisplayName(ArmorStand armorStand) {
        if (armorStand.getCustomName() != null) {
            return LocateUtils.toPlainText(armorStand.getCustomName().getString()).trim();
        }
        return LocateUtils.toPlainText(armorStand.getName().getString()).trim();
    }

    private static String getPlayerDisplayName(Player player) {
        if (player.getDisplayName() != null) {
            return LocateUtils.toPlainText(player.getDisplayName().getString()).trim();
        }
        return LocateUtils.toPlainText(player.getName().getString()).trim();
    }

    private static String getSourceDisplayName(SmolMirrorSource source) {
        if (source.player() != null) {
            return getPlayerDisplayName(source.player());
        }
        if (source.tabInfo() != null && source.tabInfo().getTabListDisplayName() != null) {
            return LocateUtils.toPlainText(source.tabInfo().getTabListDisplayName().getString()).trim();
        }
        if (source.tabInfo() != null && source.tabInfo().getProfile() != null) {
            return source.tabInfo().getProfile().name();
        }
        return "";
    }

    private static boolean nametagDisplayNamesMatch(String sourceDisplayName, String standDisplayName) {
        if (sourceDisplayName == null || standDisplayName == null) {
            return false;
        }
        String source = sourceDisplayName.trim();
        String stand = standDisplayName.trim();
        if (source.isEmpty() || stand.isEmpty()) {
            return false;
        }
        if (source.equalsIgnoreCase(stand)) {
            return true;
        }
        if (source.length() > stand.length()
                && source.regionMatches(true, 0, stand, 0, stand.length())
                && !Character.isLetterOrDigit(source.charAt(stand.length()))) {
            return true;
        }
        return false;
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
        refreshLobbyPlayersCache();
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
        refreshLobbyPlayersCache();
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

    public static List<String> getCurrentLobbyPlayers() {
        long now = System.currentTimeMillis();
        if (now - lastLobbyPlayersRefreshTime >= LOBBY_PLAYERS_REFRESH_INTERVAL_MS) {
            refreshLobbyPlayersCache();
        }
        return cachedLobbyPlayers;
    }

    public static void refreshLobbyPlayersCache() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            cachedLobbyPlayers = List.of();
            lastLobbyPlayersRefreshTime = System.currentTimeMillis();
            return;
        }

        String selfName = mc.player != null ? mc.player.getName().getString() : null;
        String normalizedSelfName = normalizeName(selfName);
        Map<String, String> lobbyPlayers = new LinkedHashMap<>();

        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry == null || entry.getProfile() == null) {
                continue;
            }

            String rawName = entry.getProfile().name();
            String normalizedName = normalizeLobbyPlayerName(rawName);
            if (normalizedName == null || normalizedName.equals(normalizedSelfName) || FRIENDS.containsKey(normalizedName)) {
                continue;
            }

            lobbyPlayers.putIfAbsent(normalizedName, rawName.trim());
        }

        List<String> names = new ArrayList<>(lobbyPlayers.values());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        cachedLobbyPlayers = List.copyOf(names);
        lastLobbyPlayersRefreshTime = System.currentTimeMillis();
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

    private static String normalizeLobbyPlayerName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.startsWith("!")) {
            return null;
        }

        if (!VALID_PLAYER_NAME_PATTERN.matcher(trimmed).matches()) {
            return null;
        }

        return normalized;
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

                for (String entry : line.split(",")) {
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
        ConfigManager.requestSave();
    }
}
