package com.shyeuar.baity.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
public final class BaityPresenceSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/PresenceSync");
    private static final long TOKEN_EXPIRY_MARGIN_MS = 60_000L;
    private static final long SYNC_MESSAGE_DELAY_MS = 3_000L;
    private static final long REPORT_CHANGE_DEBOUNCE_MS = 30_000L;
    private static final long NETWORK_WARN_THROTTLE_MS = 60_000L;
    private static final String LEGACY_SYNC_URL_MARKER = "workers.dev";
    private static final long CACHE_EXPIRE_AFTER_MS = 14L * 24L * 60L * 60L * 1000L;
    private static final long HARD_EXPIRE_AFTER_MS = 14L * 24L * 60L * 60L * 1000L;
    private static final Path CACHE_FILE_PATH = BaityConfigDir.getBaityConfigDir().resolve("remote-users-cache.json");
    private static final Gson CACHE_GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final AtomicBoolean SYNCING = new AtomicBoolean(false);
    private static final AtomicBoolean REPORTING = new AtomicBoolean(false);
    private static final AtomicLong LAST_REGISTER_EXCEPTION_WARN_AT = new AtomicLong(0L);
    private static final Object CACHE_LOCK = new Object();

    private static volatile String lastFailureStage = "";
    private static volatile long lastFailureAt = 0L;
    private static volatile long nextReportAllowedAt = 0L;
    private static volatile long autoStartupResultInWorldAt = 0L;
    private static volatile boolean startupInWorldSyncTriggered = false;

    private static volatile String lastReportedSignature = "";
    private static volatile String pendingReportSignature = "";
    private static volatile UUID lastSeenLocalPlayerUuid = null;

    private static volatile int autoStartupSyncResult = 0;
    private static volatile long autoStartupResultSetAt = 0L;
    private static volatile boolean autoStartupResultShownInWorld = false;

    private static volatile boolean remoteSmolUserPresent = false;
    private static final Map<UUID, RemoteUserState> USERS_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, ChromaProfile> CHROMA_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<String, String> CHROMA_DISPLAY_NAME_BY_LOWER = new ConcurrentHashMap<>();

    private BaityPresenceSync() {
    }

    public static void init() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        lastReportedSignature = "";
        lastSeenLocalPlayerUuid = null;
        autoStartupSyncResult = 0;
        autoStartupResultSetAt = 0L;
        autoStartupResultShownInWorld = false;
        autoStartupResultInWorldAt = 0L;
        startupInWorldSyncTriggered = false;
        nextReportAllowedAt = 0L;
        pendingReportSignature = "";
        loadCacheFromDisk();
        cleanupExpiredCache();
        if (ConfigManager.baityPresenceSyncEnabled) {
            CompletableFuture.runAsync(BaityPresenceSync::startupRefresh);
        }
    }

    public static void tick() {
        handleAccountSwitch();
        handleInWorldStartupSync();
        handlePendingReport();
        handleStartupResultNotice();
    }

    public static void syncOnce() {
        if (!SYNCING.compareAndSet(false, true)) {
            MessageUtils.sendBaityMessage("正在同步中，请稍候。");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                runManualSync();
            } finally {
                SYNCING.set(false);
            }
        });
    }

    private static void startupRefresh() {
        String baseUrl = resolveBaseUrl();
        if (baseUrl.isEmpty()) {
            setAutoStartupResultIfUnset(-1);
            return;
        }
        String usersUrl = SyncClient.health(baseUrl);
        if (usersUrl == null || usersUrl.isBlank()) {
            LOGGER.warn("[PresenceSync] startup refresh failed: no users url");
            setAutoStartupResultIfUnset(-1);
            return;
        }
        boolean ok = mergeUsersPayload(SyncClient.fetch(usersUrl));
        setAutoStartupResultIfUnset(ok ? 1 : -1);
        LOGGER.info("[PresenceSync] startup refresh ok={}", ok);
    }

    private static boolean mergeUsersPayload(String raw) {
        JsonObject root = raw == null ? null : parseJsonObject(raw);
        JsonObject users = root == null ? null : root.getAsJsonObject("users");
        if (users == null || users.isEmpty()) {
            return false;
        }
        JsonObject payload = new JsonObject();
        payload.add("users", users);
        mergeIntoCache(payload.toString());
        LOGGER.info("[PresenceSync] fetched remote users={}", users.size());
        return true;
    }

    private static void handleAccountSwitch() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (client.level == null || player == null) {
            return;
        }
        UUID currentUuid = player.getUUID();
        boolean switchedAccount = lastSeenLocalPlayerUuid != null && !lastSeenLocalPlayerUuid.equals(currentUuid);
        if (switchedAccount) {
            lastReportedSignature = "";
            invalidateToken();
        }
        lastSeenLocalPlayerUuid = currentUuid;
    }

    private static void handleStartupResultNotice() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (client.level == null || player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (autoStartupResultInWorldAt <= 0L) {
            autoStartupResultInWorldAt = now;
        }
        if (autoStartupResultShownInWorld || autoStartupSyncResult == 0) {
            return;
        }
        if (now < Math.max(autoStartupResultInWorldAt, autoStartupResultSetAt) + SYNC_MESSAGE_DELAY_MS) {
            return;
        }
        autoStartupResultShownInWorld = true;
        if (!ConfigManager.baityPresenceSyncNotificationEnabled) {
            return;
        }
        MessageUtils.sendSyncResult(autoStartupSyncResult > 0, true);
    }

    private static void runManualSync() {
        String baseUrl = resolveBaseUrl();
        if (baseUrl.isEmpty()) {
            recordFailure("config/base_url_missing");
            completeSync(false);
            return;
        }

        LocalUserState local = snapshotLocalState();
        if (local == null) {
            recordFailure("local_state_unavailable");
            completeSync(false);
            return;
        }

        SyncClient.SyncResult result = pushLocalState(baseUrl, local);
        if (result == null) {
            completeSync(false);
            return;
        }

        String usersUrl = result.usersUrl() == null ? "" : result.usersUrl().trim();
        if (usersUrl.isEmpty()) {
            usersUrl = SyncClient.health(baseUrl);
        }
        boolean ok = !usersUrl.isEmpty() && mergeUsersPayload(SyncClient.fetch(usersUrl));
        completeSync(ok);
    }

    private static SyncClient.SyncResult pushLocalState(String baseUrl, LocalUserState local) {
        String token = ensureToken(baseUrl, local.uuid(), local.name());
        if (token == null || token.isBlank()) {
            return null;
        }

        long startedAt = System.currentTimeMillis();
        SyncClient.SyncResult result = SyncClient.sync(baseUrl, token, local.name(), buildAppearanceJson(local));
        long elapsed = System.currentTimeMillis() - startedAt;

        if (result == null) {
            invalidateToken();
            LOGGER.warn("[PresenceSync] push failed elapsed={}ms", elapsed);
            recordFailure("sync");
            return null;
        }

        lastReportedSignature = local.signature();
        pendingReportSignature = "";
        LOGGER.info("[PresenceSync] push ok changed={} version={} elapsed={}ms",
                result.changed(), result.version(), elapsed);
        return result;
    }

    public static void onClickGuiClosed() {
        markReportPending();
    }

    private static void markReportPending() {
        if (!ConfigManager.baityPresenceSyncEnabled) return;
        LocalUserState local = snapshotLocalState();
        if (local == null) return;

        String signature = local.signature();
        if (signature.equals(lastReportedSignature)) {
            pendingReportSignature = "";
            nextReportAllowedAt = 0L;
            return;
        }
        if (signature.equals(pendingReportSignature)) {
            return;
        }

        pendingReportSignature = signature;
        nextReportAllowedAt = System.currentTimeMillis() + REPORT_CHANGE_DEBOUNCE_MS;
        LOGGER.info("[PresenceSync] report scheduled in {}ms", REPORT_CHANGE_DEBOUNCE_MS);
    }

    private static void handlePendingReport() {
        long deadline = nextReportAllowedAt;
        if (deadline <= 0L) return;
        if (System.currentTimeMillis() < deadline) return;

        if (!ConfigManager.baityPresenceSyncEnabled) {
            nextReportAllowedAt = 0L;
            pendingReportSignature = "";
            return;
        }
        if (!REPORTING.compareAndSet(false, true)) return;

        LocalUserState local = snapshotLocalState();
        String baseUrl = resolveBaseUrl();
        if (local == null || baseUrl.isEmpty() || local.signature().equals(lastReportedSignature)) {
            nextReportAllowedAt = 0L;
            pendingReportSignature = "";
            REPORTING.set(false);
            return;
        }

        nextReportAllowedAt = 0L;
        CompletableFuture.runAsync(() -> {
            try {
                pushLocalState(baseUrl, local);
            } finally {
                REPORTING.set(false);
            }
        });
    }

    private static void handleInWorldStartupSync() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (client.level == null || player == null) return;
        if (startupInWorldSyncTriggered) return;
        startupInWorldSyncTriggered = true;
        if (!ConfigManager.baityPresenceSyncEnabled) return;
        CompletableFuture.runAsync(BaityPresenceSync::runInWorldStartupSync);
    }

    private static void runInWorldStartupSync() {
        String baseUrl = resolveBaseUrl();
        if (baseUrl.isEmpty()) {
            return;
        }
        LocalUserState local = snapshotLocalState();
        if (local == null) {
            return;
        }

        SyncClient.SyncResult result = pushLocalState(baseUrl, local);
        if (result == null) {
            setInWorldSyncResult(false);
            return;
        }

        String usersUrl = result.usersUrl() == null ? "" : result.usersUrl().trim();
        if (usersUrl.isEmpty()) {
            usersUrl = SyncClient.health(baseUrl);
        }
        boolean ok = !usersUrl.isEmpty() && mergeUsersPayload(SyncClient.fetch(usersUrl));
        LOGGER.info("[PresenceSync] in-world startup sync ok={}", ok);
        setInWorldSyncResult(ok);
    }

    private static void setInWorldSyncResult(boolean success) {
        autoStartupSyncResult = success ? 1 : -1;
        autoStartupResultSetAt = System.currentTimeMillis();
    }

    private static void completeSync(boolean success) {
        MessageUtils.sendSyncResult(success, false);
    }

    private static void setAutoStartupResultIfUnset(int result) {
        if (autoStartupSyncResult != 0) return;
        autoStartupSyncResult = result > 0 ? 1 : -1;
        autoStartupResultSetAt = System.currentTimeMillis();
    }

    private static String ensureToken(String baseUrl, UUID uuid, String name) {
        String existing = ConfigManager.baityPresenceReportToken;
        long now = System.currentTimeMillis();
        if (existing != null && !existing.isBlank()
                && ConfigManager.baityPresenceTokenExpiresAt > now + TOKEN_EXPIRY_MARGIN_MS) {
            return existing.trim();
        }

        String serverId = beginSessionChallenge(uuid);
        if (serverId == null) {
            recordFailure("auth/session_challenge");
            return null;
        }
        SyncClient.AuthResult result = SyncClient.authenticate(baseUrl, uuid.toString(), name, serverId);
        if (result == null || result.token() == null || result.token().isBlank()) {
            LOGGER.warn("[PresenceSync] auth request failed");
            recordFailure("auth/request");
            return null;
        }
        ConfigManager.baityPresenceReportToken = result.token().trim();
        ConfigManager.baityPresenceTokenExpiresAt = result.expiresAt();
        ConfigManager.requestSave();
        return ConfigManager.baityPresenceReportToken;
    }

    private static void invalidateToken() {
        ConfigManager.baityPresenceReportToken = "";
        ConfigManager.baityPresenceTokenExpiresAt = 0L;
        ConfigManager.requestSave();
    }

    private static String beginSessionChallenge(UUID uuid) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getUser() == null) {
            return null;
        }
        String serverId = UUID.randomUUID().toString().replace("-", "");
        try {
            client.services().sessionService()
                    .joinServer(client.getUser().getProfileId(), client.getUser().getAccessToken(), serverId);
            return serverId;
        } catch (Exception e) {
            logThrottledWarn(LAST_REGISTER_EXCEPTION_WARN_AT,
                    "[PresenceSync] session challenge failed, uuid={}, err={}", uuid, e.toString());
            return null;
        }
    }

    private static void logThrottledWarn(AtomicLong gate, String pattern, Object... args) {
        long now = System.currentTimeMillis();
        long last = gate.get();
        if (now - last < NETWORK_WARN_THROTTLE_MS) return;
        if (gate.compareAndSet(last, now)) {
            LOGGER.warn(pattern, args);
        }
    }

    private static String resolveBaseUrl() {
        String url = ConfigManager.baityPresenceSyncUrl;
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.contains(LEGACY_SYNC_URL_MARKER)) {
            return ConfigManager.DEFAULT_BAITY_PRESENCE_SYNC_URL;
        }
        return trimmed;
    }

    private static void recordFailure(String stage) {
        lastFailureStage = stage;
        lastFailureAt = System.currentTimeMillis();
    }

    public static String buildDiagnosticReport() {
        String detail = SyncClient.lastFailure();
        StringBuilder report = new StringBuilder();
        report.append("baity presence sync diagnostic").append('\n');
        report.append("time: ").append(lastFailureAt > 0L
                ? java.time.Instant.ofEpochMilli(lastFailureAt).toString()
                : "no failure recorded").append('\n');
        report.append("stage: ").append(lastFailureStage.isEmpty() ? "none" : lastFailureStage).append('\n');
        report.append("endpoint: ").append(resolveBaseUrl()).append('\n');
        report.append("detail: ").append(detail == null || detail.isEmpty()
                ? "no transport error recorded" : detail).append('\n');
        return report.toString();
    }

    private static String buildAppearanceJson(LocalUserState local) {
        String nowIso = java.time.Instant.now().toString();
        RemoteUserState self = new RemoteUserState(
                local.uuid(),
                local.name(),
                local.isBaityUser(),
                local.smolEnabled(),
                local.nickTweaksEnabled(),
                local.chromaEnabled(),
                local.chromaPalette(),
                local.chromaSpeed(),
                local.chromaSize(),
                local.chromaAmount(),
                local.chromaLightness(),
                local.gradientStart(),
                local.gradientEnd(),
                local.boldSelf(),
                local.customNickColorEnabled(),
                local.nickChanger(),
                nowIsoToEpochMs(nowIso)
        );
        JsonObject user = buildUserJsonFromRemote(self, nowIso);
        user.remove("name");
        return user.toString();
    }

    private static void mergeIntoCache(String usersJson) {
        synchronized (CACHE_LOCK) {
            mergeIntoCacheLocked(usersJson);
        }
    }

    private static void mergeIntoCacheLocked(String usersJson) {
        JsonObject merged = new JsonObject();
        JsonObject mergedUsers = new JsonObject();

        JsonObject cached = readCacheRoot();
        JsonObject cachedUsers = cached == null ? null : cached.getAsJsonObject("users");
        if (cachedUsers != null) {
            for (Map.Entry<String, JsonElement> entry : cachedUsers.entrySet()) {
                mergedUsers.add(entry.getKey(), entry.getValue());
            }
        }

        JsonObject payload = parseJsonObject(usersJson);
        JsonObject payloadUsers = payload == null ? null : payload.getAsJsonObject("users");
        if (payloadUsers != null) {
            for (Map.Entry<String, JsonElement> entry : payloadUsers.entrySet()) {
                JsonObject normalized = normalizeRemoteUser(entry.getValue());
                if (normalized != null) {
                    mergedUsers.add(entry.getKey(), normalized);
                }
            }
        }

        merged.add("users", mergedUsers);
        String json = merged.toString();
        applyPayload(json);
        saveCacheToDisk(json);
    }

    private static JsonObject normalizeRemoteUser(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject user = element.getAsJsonObject();
        JsonObject appearance = user.has("appearance") && user.get("appearance").isJsonObject()
                ? user.getAsJsonObject("appearance")
                : user;
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : appearance.entrySet()) {
            if ("name".equals(entry.getKey())) continue;
            normalized.add(entry.getKey(), entry.getValue());
        }
        String name = getAsString(user, "name", "");
        if (!name.isBlank()) {
            normalized.addProperty("name", name);
        }
        JsonElement updatedAt = user.get("updatedAt");
        if (updatedAt != null) {
            normalized.add("updatedAt", updatedAt);
        }
        return normalized;
    }

    private static JsonObject readCacheRoot() {
        try {
            if (!Files.exists(CACHE_FILE_PATH)) return null;
            String json = Files.readString(CACHE_FILE_PATH, StandardCharsets.UTF_8);
            return parseJsonObject(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject parseJsonObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement rootElement = JsonParser.parseString(json);
            return rootElement.isJsonObject() ? rootElement.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void cleanupExpiredCache() {
        JsonObject root = readCacheRoot();
        JsonObject users = root == null ? null : root.getAsJsonObject("users");
        if (users == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int removed = 0;
        for (String key : new ArrayList<>(users.keySet())) {
            long updatedAt = cacheUpdatedAtMs(users.get(key));
            if (updatedAt > 0L && now - updatedAt > CACHE_EXPIRE_AFTER_MS) {
                users.remove(key);
                removed++;
            }
        }
        if (removed > 0) {
            saveCacheToDisk(root.toString());
        }
    }

    private static long cacheUpdatedAtMs(JsonElement element) {
        if (element == null || !element.isJsonObject()) return -1L;
        JsonObject user = element.getAsJsonObject();
        if (user.has("updatedAt")) {
            try {
                return user.get("updatedAt").getAsLong();
            } catch (Exception ignored) {
            }
        }
        return parseIsoEpochMs(getAsString(user.getAsJsonObject("meta"), "lastSeenAt", ""));
    }

    public static boolean isSmolEnabledFor(UUID uuid) {
        if (uuid == null) return false;
        RemoteUserState state = USERS_BY_UUID.get(uuid);
        return state != null && state.smolPeopleEnabled();
    }

    public static Boolean getRemoteSmolPreference(UUID uuid) {
        if (uuid == null) return null;
        RemoteUserState state = USERS_BY_UUID.get(uuid);
        if (state == null) return null;
        return state.smolPeopleEnabled();
    }

    public static boolean hasRemoteSmolUser() {
        return remoteSmolUserPresent;
    }

    public static ChromaProfile getChromaProfileByName(String name) {
        if (name == null || name.isBlank()) return null;
        return CHROMA_BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static void forEachChromaProfileByCachedName(BiConsumer<String, ChromaProfile> consumer) {
        CHROMA_BY_NAME.forEach((lower, profile) -> {
            if (profile == null || lower == null || lower.isBlank()) return;
            String display = CHROMA_DISPLAY_NAME_BY_LOWER.getOrDefault(lower, lower);
            consumer.accept(display, profile);
        });
    }

    private static long nowIsoToEpochMs(String iso) {
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static JsonObject buildUserJsonFromRemote(RemoteUserState state, String nowIso) {
        JsonObject userObj = new JsonObject();
        userObj.addProperty("name", state.name());
        userObj.addProperty("isBaityUser", state.isBaityUser());

        JsonObject features = new JsonObject();

        JsonObject nickTweaks = new JsonObject();
        nickTweaks.addProperty("enabled", state.nickTweaksEnabled());
        nickTweaks.addProperty("boldEnabled", state.boldEnabled());
        nickTweaks.addProperty("nickChanger", state.nickChanger());

        if (state.nickTweaksEnabled()) {
            nickTweaks.addProperty("chromaEnabled", state.chromaEnabled());
            nickTweaks.addProperty("customNickColorEnabled", state.customNickColorEnabled());

            if (state.chromaEnabled()) {
                JsonObject chroma = new JsonObject();
                chroma.addProperty("enabled", true);
                chroma.addProperty("speed", state.chromaSpeed());
                chroma.addProperty("size", state.chromaSize());
                chroma.addProperty("chroma", state.chromaAmount());
                chroma.addProperty("lightness", state.chromaLightness());
                JsonArray palette = new JsonArray();
                for (int color : state.chromaPalette()) {
                    palette.add(String.format("#%06X", color & 0xFFFFFF));
                }
                chroma.add("palette", palette);
                nickTweaks.add("chroma", chroma);
            } else if (state.customNickColorEnabled()) {
                JsonObject solid = new JsonObject();
                solid.addProperty("customColorStart", String.format("#%06X", state.gradientStart() & 0xFFFFFF));
                solid.addProperty("customColorEnd", String.format("#%06X", state.gradientEnd() & 0xFFFFFF));
                nickTweaks.add("solid", solid);
            }
        }

        features.add("nickTweaks", nickTweaks);

        JsonObject smolPeople = new JsonObject();
        smolPeople.addProperty("enabled", state.smolPeopleEnabled());
        features.add("smolPeople", smolPeople);

        userObj.add("features", features);

        JsonObject meta = new JsonObject();
        meta.addProperty("protocol", 1);
        meta.addProperty("reportedAt", nowIso);
        meta.addProperty("lastSeenAt", nowIso);
        userObj.add("meta", meta);

        return userObj;
    }

    private static LocalUserState snapshotLocalState() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return null;

        String playerName = player.getName().getString();
        if (playerName == null || playerName.isBlank()) return null;

        Module chromaModule = ModuleManager.getModuleByName("NickTweaks");
        boolean nickTweaksEnabled = chromaModule != null && chromaModule.isEnabled();
        boolean chromaEnabled = ConfigManager.nickTweaksChromaEnabled;
        boolean smolEnabled = ConfigManager.smolpeopleMode;
        double chromaSize = Math.max(0.1, Math.min(12.0, ConfigManager.nickTweaksChromaSize));
        double chromaAmount = Math.max(0.0, Math.min(0.4, ConfigManager.nickTweaksChromaChroma));
        double chromaLightness = Math.max(0.2, Math.min(1.0, ConfigManager.nickTweaksChromaLightness));
        int[] palette = generatePalette(chromaAmount, chromaLightness);
        double speed = chromaEnabled ? Math.max(0.0, Math.min(8.0, ConfigManager.nickTweaksChromaSpeed)) : 0.0;

        int gradientStart = ConfigManager.nickTweaksGradientStartColor & 0xFFFFFF;
        int gradientEnd = ConfigManager.nickTweaksGradientEndColor & 0xFFFFFF;
        boolean boldSelf = ConfigManager.nickTweaksBoldSelf;
        boolean customNickColorEnabled = ConfigManager.nickTweaksCustomNickColorEnabled;
        String nickChanger = ConfigManager.nickTweaksNickChanger == null ? "" : ConfigManager.nickTweaksNickChanger;
        LocalUserState state = new LocalUserState(
            player.getUUID(),
            playerName,
            true,
            nickTweaksEnabled,
            chromaEnabled,
            smolEnabled,
            palette,
            speed,
            chromaSize,
            chromaAmount,
            chromaLightness,
            gradientStart,
            gradientEnd,
            boldSelf,
            customNickColorEnabled,
            nickChanger
        );
        return state;
    }

    private static int[] generatePalette(double chroma, double lightness) {
        int count = 6;
        int[] colors = new int[count];
        float saturation = (float) (chroma / 0.4);
        for (int i = 0; i < count; i++) {
            float hue = (float) i / (float) count;
            colors[i] = Mth.hsvToRgb(hue, saturation, (float) lightness);
        }
        return colors;
    }

    private static void applyPayload(String json) {
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) return;

        JsonObject root = rootElement.getAsJsonObject();
        JsonObject users = root.getAsJsonObject("users");
        if (users == null) return;
        long now = System.currentTimeMillis();

        Map<UUID, RemoteUserState> newUsers = new ConcurrentHashMap<>();
        Map<String, ChromaProfile> newChromaByName = new ConcurrentHashMap<>();
        Map<String, String> newChromaDisplayByLower = new ConcurrentHashMap<>();

        for (Map.Entry<String, JsonElement> entry : users.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (Exception ignored) {
                continue;
            }

            if (!entry.getValue().isJsonObject()) continue;
            JsonObject userObj = entry.getValue().getAsJsonObject();
            String name = getAsString(userObj, "name", "");
            if (name.isBlank()) continue;

            boolean isBaityUser = getAsBoolean(userObj, "isBaityUser", false);

            JsonObject features = userObj.getAsJsonObject("features");

            JsonObject smolObj = features == null ? null : features.getAsJsonObject("smolPeople");
            boolean smolEnabled = getAsBoolean(smolObj, "enabled", false);

            JsonObject nickTweaksObj = features == null ? null : features.getAsJsonObject("nickTweaks");
            boolean nickTweaksEnabled = getAsBoolean(nickTweaksObj, "enabled", false);

            boolean chromaEnabled = false;
            boolean boldSelf = false;
            boolean customNickColorEnabled = false;
            String nickChanger = "";
            double speed = 0.0;
            double chromaSize = 3.1;
            double chromaAmount = 0.2;
            double chromaLightness = 0.8;
            int gradientStart = 0xFF4D4D;
            int gradientEnd = 0xC299FF;
            int[] palette = new int[0];

            if (nickTweaksEnabled) {
                boldSelf = getAsBoolean(nickTweaksObj, "boldEnabled", false);
                nickChanger = getAsString(nickTweaksObj, "nickChanger", "");
                chromaEnabled = getAsBoolean(nickTweaksObj, "chromaEnabled", false);
                customNickColorEnabled = getAsBoolean(nickTweaksObj, "customNickColorEnabled", false);

                if (chromaEnabled) {
                    JsonObject chromaObj = nickTweaksObj == null ? null : nickTweaksObj.getAsJsonObject("chroma");
                    speed = clamp(getAsDouble(chromaObj, "speed", 1.0), 0.0, 8.0);
                    chromaSize = clamp(getAsDouble(chromaObj, "size", 3.1), 0.1, 12.0);
                    chromaAmount = clamp(getAsDouble(chromaObj, "chroma", 0.2), 0.0, 0.4);
                    chromaLightness = clamp(getAsDouble(chromaObj, "lightness", 0.8), 0.2, 1.0);
                    palette = parsePalette(chromaObj == null ? null : chromaObj.getAsJsonArray("palette"));
                    if (palette.length == 0) {
                        palette = generatePalette(chromaAmount, chromaLightness);
                    }
                } else if (customNickColorEnabled) {
                    JsonObject solidObj = nickTweaksObj == null ? null : nickTweaksObj.getAsJsonObject("solid");
                    gradientStart = parseHexColor(solidObj, "customColorStart", 0xFF4D4D);
                    gradientEnd = parseHexColor(solidObj, "customColorEnd", 0xC299FF);
                    palette = new int[]{gradientStart, gradientEnd};
                }
            }

            long lastSeenEpochMs = 0L;
            JsonElement updatedAtElement = userObj.get("updatedAt");
            if (updatedAtElement != null && updatedAtElement.isJsonPrimitive()) {
                try {
                    lastSeenEpochMs = updatedAtElement.getAsLong();
                } catch (Exception ignored) {
                }
            }
            if (lastSeenEpochMs <= 0L) {
                JsonObject metaObj = userObj.getAsJsonObject("meta");
                lastSeenEpochMs = parseIsoEpochMs(getAsString(metaObj, "lastSeenAt", ""));
                if (lastSeenEpochMs <= 0L) {
                    lastSeenEpochMs = parseIsoEpochMs(getAsString(metaObj, "reportedAt", ""));
                }
            }
            if (lastSeenEpochMs > 0L && (now - lastSeenEpochMs) > HARD_EXPIRE_AFTER_MS) {
                continue;
            }

            RemoteUserState state = new RemoteUserState(
                uuid,
                name,
                isBaityUser,
                smolEnabled,
                nickTweaksEnabled,
                chromaEnabled,
                palette,
                speed,
                chromaSize,
                chromaAmount,
                chromaLightness,
                gradientStart,
                gradientEnd,
                boldSelf,
                customNickColorEnabled,
                nickChanger,
                lastSeenEpochMs
            );
            newUsers.put(uuid, state);
            if (nickTweaksEnabled) {
                String nameLower = name.toLowerCase(Locale.ROOT);
                newChromaByName.put(
                    nameLower,
                    new ChromaProfile(chromaEnabled, palette, speed, chromaSize, chromaAmount, chromaLightness, gradientStart, gradientEnd, boldSelf, customNickColorEnabled, nickChanger)
                );
                newChromaDisplayByLower.put(nameLower, name);
            }
        }

        USERS_BY_UUID.clear();
        USERS_BY_UUID.putAll(newUsers);
        CHROMA_BY_NAME.clear();
        CHROMA_BY_NAME.putAll(newChromaByName);
        CHROMA_DISPLAY_NAME_BY_LOWER.clear();
        CHROMA_DISPLAY_NAME_BY_LOWER.putAll(newChromaDisplayByLower);

        boolean anyRemoteSmol = false;
        for (RemoteUserState state : newUsers.values()) {
            if (state.smolPeopleEnabled()) {
                anyRemoteSmol = true;
                break;
            }
        }
        remoteSmolUserPresent = anyRemoteSmol;
    }

    private static void loadCacheFromDisk() {
        try {
            if (!Files.exists(CACHE_FILE_PATH)) return;
            String json = Files.readString(CACHE_FILE_PATH, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;
            applyPayload(json);
            // TODO(future): Transitional remote-users-cache.json pretty-print migration — can be removed in a future release when ready.
            if (isMinifiedCacheJson(json)) {
                saveCacheToDisk(json);
            }
            // END TODO(future)
        } catch (Exception ignored) {
        }
    }

    private static void saveCacheToDisk(String json) {
        try {
            if (json == null || json.isBlank()) return;
            Path parent = CACHE_FILE_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(CACHE_FILE_PATH, formatPrettyCacheJson(json), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static String formatPrettyCacheJson(String json) {
        return CACHE_GSON.toJson(JsonParser.parseString(json));
    }

    private static boolean isMinifiedCacheJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        return !json.contains("\n") && !json.contains("\r");
    }

    private static int[] parsePalette(JsonArray array) {
        if (array == null || array.isEmpty()) return new int[0];
        ArrayList<Integer> colors = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) continue;
            String hex = element.getAsString();
            if (hex == null || !hex.matches("^#([A-Fa-f0-9]{6})$")) continue;
            try {
                colors.add(Integer.parseInt(hex.substring(1), 16));
            } catch (Exception ignored) {
            }
        }
        int[] out = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) out[i] = colors.get(i);
        return out;
    }

    private static String getAsString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseHexColor(JsonObject obj, String key, int fallback) {
        String raw = getAsString(obj, key, "");
        if (!raw.matches("^#([A-Fa-f0-9]{6})$")) return fallback;
        try {
            return Integer.parseInt(raw.substring(1), 16);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getAsBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getAsDouble(JsonObject obj, String key, double fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long parseIsoEpochMs(String iso) {
        if (iso == null || iso.isBlank()) return -1L;
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    public record ChromaProfile(
            boolean chromaEnabled,
            int[] palette,
            double speed,
            double chromaSize,
            double chromaAmount,
            double chromaLightness,
            int gradientStart,
            int gradientEnd,
            boolean boldSelf,
            boolean customNickColorEnabled,
            String nickChanger
    ) {
        public ChromaProfile {
            if (palette == null) palette = new int[0];
            gradientStart &= 0xFFFFFF;
            gradientEnd &= 0xFFFFFF;
            if (nickChanger == null) nickChanger = "";
        }

        public int[] paletteView() {
            return palette.length == 0 ? new int[0] : palette.clone();
        }
    }

    public record RemoteUserState(
            UUID uuid,
            String name,
            boolean isBaityUser,
            boolean smolPeopleEnabled,
            boolean nickTweaksEnabled,
            boolean chromaEnabled,
            int[] chromaPalette,
            double chromaSpeed,
            double chromaSize,
            double chromaAmount,
            double chromaLightness,
            int gradientStart,
            int gradientEnd,
            boolean boldEnabled,
            boolean customNickColorEnabled,
            String nickChanger,
            long lastSeenEpochMs
    ) {
    }

    private record LocalUserState(
            UUID uuid,
            String name,
            boolean isBaityUser,
            boolean nickTweaksEnabled,
            boolean chromaEnabled,
            boolean smolEnabled,
            int[] chromaPalette,
            double chromaSpeed,
            double chromaSize,
            double chromaAmount,
            double chromaLightness,
            int gradientStart,
            int gradientEnd,
            boolean boldSelf,
            boolean customNickColorEnabled,
            String nickChanger
    ) {
        String signature() {
            return uuid + "|" + name + "|" + isBaityUser + "|" + nickTweaksEnabled + "|" + chromaEnabled + "|" + smolEnabled + "|" + chromaSpeed
                    + "|" + chromaSize + "|" + chromaAmount + "|" + chromaLightness
                    + "|" + gradientStart + "|" + gradientEnd + "|" + boldSelf + "|" + customNickColorEnabled + "|" + nickChanger
                    + "|" + java.util.Arrays.toString(chromaPalette);
        }
    }
}
