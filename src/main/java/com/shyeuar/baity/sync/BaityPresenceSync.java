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

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Environment(EnvType.CLIENT)
public final class BaityPresenceSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/PresenceSync");
    private static final long REMOTE_READ_COOLDOWN_MS = 20_000L;
    private static volatile long nextRemoteReadAllowedAt = 0L;
    private static final long REPORT_CHANGE_DEBOUNCE_MS = 3_000L;
    private static final long SYNC_MESSAGE_DELAY_MS = 3_000L;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int FETCH_READ_TIMEOUT_MS = 120_000;
    private static final int NETWORK_RETRY_COUNT = 0;
    private static final long NETWORK_RETRY_BACKOFF_MS = 350L;
    private static final long NETWORK_WARN_THROTTLE_MS = 60_000L;
    private static final String DEFAULT_SYNC_URL = "https://baity-presence-sync.1427637445.workers.dev/users.json";
    private static final String DEFAULT_SYNC_ACCESS_TOKEN = "baity_sync_read_v1_f4c9e7a2d1b84e73";
    private static final long SOFT_STALE_AFTER_MS = 3L * 24L * 60L * 60L * 1000L;
    private static final long HARD_EXPIRE_AFTER_MS = 3L * 24L * 60L * 60L * 1000L;
    private static final Path CACHE_FILE_PATH = BaityConfigDir.getBaityConfigDir().resolve("remote-users-cache.json");
    private static final Gson CACHE_GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);
    private static final AtomicBoolean REPORTING = new AtomicBoolean(false);

    private static volatile long nextReportAllowedAt = 0L;
    private static volatile String lastReportedSignature = "";
    private static volatile UUID lastSeenLocalPlayerUuid = null;
    private static volatile long nextTokenProvisionAllowedAt = 0L;
    private static final AtomicBoolean TOKEN_PROVISIONING = new AtomicBoolean(false);
    private static final AtomicLong LAST_FETCH_EXCEPTION_WARN_AT = new AtomicLong(0L);
    private static final AtomicLong LAST_REPORT_EXCEPTION_WARN_AT = new AtomicLong(0L);
    private static final AtomicLong LAST_REGISTER_EXCEPTION_WARN_AT = new AtomicLong(0L);

    private static final AtomicBoolean MANUAL_SYNC_PENDING = new AtomicBoolean(false);
    private static final AtomicBoolean MANUAL_RESULT_SENT = new AtomicBoolean(false);
    private static volatile int autoStartupSyncResult = 0;
    private static volatile long autoStartupResultSetAt = 0L;
    private static volatile boolean autoStartupResultShownInWorld = false;
    private static volatile boolean autoSyncTriggeredInWorld = false;

    private static volatile boolean remoteSmolUserPresent = false;
    private static final Map<UUID, RemoteUserState> USERS_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, ChromaProfile> CHROMA_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<String, String> CHROMA_DISPLAY_NAME_BY_LOWER = new ConcurrentHashMap<>();

    private BaityPresenceSync() {
    }

    public static void init() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        nextReportAllowedAt = 0L;
        lastReportedSignature = "";
        lastSeenLocalPlayerUuid = null;
        nextTokenProvisionAllowedAt = 0L;
        autoStartupSyncResult = 0;
        autoStartupResultSetAt = 0L;
        autoStartupResultShownInWorld = false;
        autoSyncTriggeredInWorld = false;
        loadCacheFromDisk();
        if (ConfigManager.baityPresenceSyncEnabled) {
            CompletableFuture.runAsync(BaityPresenceSync::runPresenceConnectivityProbe);
        }
    }

    public static void runPresenceConnectivityProbe() {
        String fetchUrl = resolveFetchUrl();
        if (fetchUrl == null || fetchUrl.isBlank()) {
            return;
        }
        PresenceProxyResolver.establishSession(toHealthUrl(fetchUrl.trim()));
    }

    public static void tick() {
        handleImmediateSyncTriggers();
    }

    public static void syncOnce() {
        long now = System.currentTimeMillis();
        nextReportAllowedAt = 0L;
        nextTokenProvisionAllowedAt = 0L;
        MANUAL_SYNC_PENDING.set(true);
        MANUAL_RESULT_SENT.set(false);
        startReadThenWrite(now, true, true);
    }

    static String syncReadToken() {
        return DEFAULT_SYNC_ACCESS_TOKEN;
    }

    public static void onClickGuiClosed() {
        attemptReport(System.currentTimeMillis(), false, false);
    }

    private static void attemptReport(long now, boolean forceUpload, boolean forceRemoteSync) {
        if (!forceRemoteSync && !ConfigManager.baityPresenceSyncEnabled) return;
        String reportUrl = resolveReportUrl();
        if (reportUrl == null || reportUrl.isBlank()) {
            return;
        }
        if (!REPORTING.compareAndSet(false, true)) {
            return;
        }

        LocalUserState state = snapshotLocalState();
        if (state == null) {
            REPORTING.set(false);
            return;
        }

        maybeProvisionWriteToken(reportUrl.trim(), state, forceRemoteSync);
        if (ConfigManager.baityPresenceReportToken == null || ConfigManager.baityPresenceReportToken.isBlank()) {
            REPORTING.set(false);
            return;
        }

        String signature = state.signature();
        boolean changed = !signature.equals(lastReportedSignature);
        boolean allowedByDebounce = now >= nextReportAllowedAt;
        boolean shouldUpload = forceUpload ? allowedByDebounce : (changed && allowedByDebounce);
        if (!shouldUpload) {
            REPORTING.set(false);
            return;
        }

        nextReportAllowedAt = now + REPORT_CHANGE_DEBOUNCE_MS;
        CompletableFuture.runAsync(() -> {
            try {
                boolean success = reportLocalState(reportUrl.trim(), state, forceRemoteSync);
                if (success) {
                    lastReportedSignature = signature;
                    updateCacheForSelfFromLocalState(state);
                } else {
                    setAutoStartupResultIfUnset(-1);
                }
            } finally {
                REPORTING.set(false);
            }
        });
    }

    private static void updateCacheForSelfFromLocalState(LocalUserState local) {
        try {
            String nowIso = java.time.Instant.now().toString();
            Map<UUID, RemoteUserState> snapshot = new HashMap<>(USERS_BY_UUID);
            snapshot.put(local.uuid(), new RemoteUserState(
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
                    nowIsoToEpochMs(nowIso),
                    false
            ));

            JsonObject root = new JsonObject();
            JsonObject usersObj = new JsonObject();

            for (RemoteUserState rs : snapshot.values()) {
                usersObj.add(rs.uuid().toString(), buildUserJsonFromRemote(rs, nowIso));
            }

            root.add("users", usersObj);
            saveCacheToDisk(root.toString());
        } catch (Exception ignored) {
        }
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

    private static void handleImmediateSyncTriggers() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        boolean inWorld = client.level != null && player != null;

        if (inWorld) {
            UUID currentUuid = player.getUUID();
            boolean switchedAccount = lastSeenLocalPlayerUuid != null && !lastSeenLocalPlayerUuid.equals(currentUuid);
            if (switchedAccount || lastSeenLocalPlayerUuid == null) {
                nextReportAllowedAt = 0L;
                nextTokenProvisionAllowedAt = 0L;
                if (switchedAccount) {
                    lastReportedSignature = "";
                    ConfigManager.baityPresenceReportToken = "";
                    ConfigManager.requestSave();
                }
            }
            lastSeenLocalPlayerUuid = currentUuid;
        }

        if (inWorld && ConfigManager.baityPresenceSyncEnabled && !autoSyncTriggeredInWorld) {
            autoSyncTriggeredInWorld = true;
            startReadThenWrite(System.currentTimeMillis(), true);
        }
        if (inWorld && !autoStartupResultShownInWorld && autoStartupSyncResult != 0) {
            long now = System.currentTimeMillis();
            long earliest = autoStartupResultSetAt <= 0L ? now : (autoStartupResultSetAt + SYNC_MESSAGE_DELAY_MS);
            if (now >= earliest) {
                autoStartupResultShownInWorld = true;
                boolean success = autoStartupSyncResult > 0;
                boolean notify = ConfigManager.baityPresenceSyncNotificationEnabled;
                if (notify) {
                    MessageUtils.sendSyncResult(success, true);
                } else if (!success) {
                    MessageUtils.sendSyncResult(false, false);
                }
            }
        }
    }

    private static void setAutoStartupResultIfUnset(int result) {
        if (autoStartupSyncResult != 0) return;
        autoStartupSyncResult = result > 0 ? 1 : -1;
        autoStartupResultSetAt = System.currentTimeMillis();
    }

    private static void completeManualSyncIfPending(boolean fetchOk) {
        if (!MANUAL_SYNC_PENDING.get() || MANUAL_RESULT_SENT.get()) {
            return;
        }
        MANUAL_RESULT_SENT.set(true);
        MANUAL_SYNC_PENDING.set(false);
        if (fetchOk) {
            setAutoStartupResultIfUnset(1);
        } else {
            setAutoStartupResultIfUnset(-1);
        }
        MessageUtils.sendSyncResult(fetchOk, false);
    }

    private static void startReadThenWrite(long now, boolean forceUpload) {
        startReadThenWrite(now, forceUpload, false);
    }

    private static void startReadThenWrite(long now, boolean forceUpload, boolean forceRemoteSync) {
        if (!forceRemoteSync && !ConfigManager.baityPresenceSyncEnabled) return;

        if (!forceRemoteSync && now < nextRemoteReadAllowedAt) {
            CompletableFuture.runAsync(() -> {
                runPresenceConnectivityProbe();
                attemptReport(now, forceUpload, forceRemoteSync);
            });
            return;
        }

        nextRemoteReadAllowedAt = forceRemoteSync ? 0L : now + REMOTE_READ_COOLDOWN_MS;

        String fetchUrl = resolveFetchUrl();
        if (fetchUrl == null || fetchUrl.isBlank()) {
            CompletableFuture.runAsync(() -> {
                if (forceRemoteSync) {
                    completeManualSyncIfPending(false);
                }
                runPresenceConnectivityProbe();
                attemptReport(now, forceUpload, forceRemoteSync);
            });
            return;
        }

        String trimmed = fetchUrl.trim();
        if (!FETCHING.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                if (forceRemoteSync) {
                    completeManualSyncIfPending(false);
                }
                runPresenceConnectivityProbe();
                attemptReport(now, forceUpload, forceRemoteSync);
            });
            return;
        }

        CompletableFuture.runAsync(() -> {
            boolean fetchOk = false;
            try {
                runPresenceConnectivityProbe();
                fetchOk = fetchAndReplace(trimmed, forceRemoteSync);
            } finally {
                FETCHING.set(false);
                if (forceRemoteSync) {
                    completeManualSyncIfPending(fetchOk);
                }
                attemptReport(System.currentTimeMillis(), forceUpload, forceRemoteSync);
            }
        });
    }

    private static String resolveFetchUrl() {
        String syncUrl = ConfigManager.baityPresenceSyncUrl;
        String reportUrl = ConfigManager.baityPresenceReportUrl;
        if (reportUrl != null && !reportUrl.isBlank()) {
            String trimmed = reportUrl.trim();
            if (isLegacyGithubSyncUrl(trimmed)) {
                return DEFAULT_SYNC_URL;
            }
            if (trimmed.endsWith("/report")) {
                return trimmed.substring(0, trimmed.length() - "/report".length()) + "/users.json";
            }
            if (trimmed.endsWith("/")) {
                return trimmed + "users.json";
            }
            if (trimmed.endsWith(".json")) {
                return trimmed;
            }
            return trimmed + "/users.json";
        }
        if (syncUrl == null || syncUrl.isBlank()) {
            return DEFAULT_SYNC_URL;
        }
        String trimmed = syncUrl.trim();
        if (isLegacyGithubSyncUrl(trimmed)) {
            return DEFAULT_SYNC_URL;
        }
        return trimmed;
    }

    private static String resolveReportUrl() {
        String reportUrl = ConfigManager.baityPresenceReportUrl;
        if (reportUrl != null && !reportUrl.isBlank()) {
            String trimmedReport = reportUrl.trim();
            if (isLegacyGithubSyncUrl(trimmedReport)) {
                return DEFAULT_SYNC_URL.substring(0, DEFAULT_SYNC_URL.length() - "/users.json".length()) + "/report";
            }
            return trimmedReport;
        }
        String syncUrl = ConfigManager.baityPresenceSyncUrl;
        if (syncUrl == null || syncUrl.isBlank()) {
            return "";
        }
        String trimmed = syncUrl.trim();
        if (isLegacyGithubSyncUrl(trimmed)) {
            return DEFAULT_SYNC_URL.substring(0, DEFAULT_SYNC_URL.length() - "/users.json".length()) + "/report";
        }
        if (trimmed.endsWith("/users.json")) {
            return trimmed.substring(0, trimmed.length() - "/users.json".length()) + "/report";
        }
        if (trimmed.endsWith(".json")) {
            int slashIdx = trimmed.lastIndexOf('/');
            if (slashIdx > 0) {
                return trimmed.substring(0, slashIdx + 1) + "report";
            }
        }
        if (trimmed.endsWith("/")) {
            return trimmed + "report";
        }
        return trimmed + "/report";
    }

    private static boolean isLegacyGithubSyncUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("raw.githubusercontent.com")
            && lower.contains("baity-sync-data")
            && lower.endsWith("users.json");
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

    private static boolean fetchAndReplace(String url, boolean forceRemoteSync) {
        for (int attempt = 0; attempt <= NETWORK_RETRY_COUNT; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = openHttpConnection(url, attempt);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(FETCH_READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("x-baity-token", DEFAULT_SYNC_ACCESS_TOKEN);
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    connection.setRequestProperty("x-baity-uuid", player.getUUID().toString());
                }

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream stream = connection.getInputStream()) {
                        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        applyPayload(json, true, forceRemoteSync);
                        saveCacheToDisk(json);
                        LOGGER.info("[PresenceSync] fetch ok, code={}, bytes={}, attempt={}", code, json.length(), attempt + 1);
                        setAutoStartupResultIfUnset(1);
                        return true;
                    }
                }
                LOGGER.warn("[PresenceSync] fetch failed, code={}, url={}, attempt={}", code, url, attempt + 1);
                if (attempt < NETWORK_RETRY_COUNT && (code == 429 || code >= 500)) {
                    sleepRetryBackoff();
                    continue;
                }
                setAutoStartupResultIfUnset(-1);
                return false;
            } catch (Exception e) {
                logThrottledWarn(LAST_FETCH_EXCEPTION_WARN_AT, "[PresenceSync] fetch exception, attempt={}, err={}", attempt + 1, e.toString());
                if (attempt < NETWORK_RETRY_COUNT && shouldRetryException(e)) {
                    sleepRetryBackoff();
                    continue;
                }
                setAutoStartupResultIfUnset(-1);
                return false;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return false;
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

    private static boolean reportLocalState(String url, LocalUserState state, boolean forceRemoteSync) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);

        JsonObject user = new JsonObject();
        user.addProperty("uuid", state.uuid().toString());
        user.addProperty("name", state.name());
        user.addProperty("isBaityUser", state.isBaityUser());

        JsonObject features = new JsonObject();

        JsonObject nickTweaks = new JsonObject();
        nickTweaks.addProperty("enabled", state.nickTweaksEnabled());
        nickTweaks.addProperty("boldEnabled", state.boldSelf());
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

        JsonObject smol = new JsonObject();
        smol.addProperty("enabled", state.smolEnabled());
        features.add("smolPeople", smol);
        user.add("features", features);

        JsonObject meta = new JsonObject();
        meta.addProperty("protocol", 1);
        String nowIso = java.time.Instant.now().toString();
        meta.addProperty("reportedAt", nowIso);
        meta.addProperty("lastSeenAt", nowIso);
        user.add("meta", meta);
        root.add("user", user);
        byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);

        for (int attempt = 0; attempt <= NETWORK_RETRY_COUNT; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = openHttpConnection(url, attempt);
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                String token = ConfigManager.baityPresenceReportToken;
                if (token != null && !token.isBlank()) {
                    connection.setRequestProperty("x-baity-token", token.trim());
                }

                connection.getOutputStream().write(payload);
                connection.getOutputStream().flush();

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream responseBody = connection.getInputStream()) {
                        responseBody.transferTo(java.io.OutputStream.nullOutputStream());
                    }
                    LOGGER.info("[PresenceSync] report ok, code={}, uuid={}, attempt={}", code, state.uuid(), attempt + 1);
                    return true;
                }
                try (InputStream errorBody = connection.getErrorStream()) {
                    if (errorBody != null) {
                        errorBody.transferTo(java.io.OutputStream.nullOutputStream());
                    }
                }
                LOGGER.warn("[PresenceSync] report failed, code={}, uuid={}, attempt={}", code, state.uuid(), attempt + 1);
                if (code == 401 || code == 403) {
                    ConfigManager.baityPresenceReportToken = "";
                    ConfigManager.requestSave();
                    return false;
                }
                if (attempt < NETWORK_RETRY_COUNT && (code == 429 || code >= 500)) {
                    sleepRetryBackoff();
                    continue;
                }
                return false;
            } catch (Exception e) {
                logThrottledWarn(LAST_REPORT_EXCEPTION_WARN_AT, "[PresenceSync] report exception, uuid={}, attempt={}, err={}", state.uuid(), attempt + 1, e.toString());
                if (attempt < NETWORK_RETRY_COUNT && shouldRetryException(e)) {
                    sleepRetryBackoff();
                    continue;
                }
                return false;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return false;
    }

    private static void maybeProvisionWriteToken(String reportUrl, LocalUserState state, boolean forceRemoteSync) {
        String existing = ConfigManager.baityPresenceReportToken;
        if (existing != null && !existing.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextTokenProvisionAllowedAt) {
            return;
        }
        if (!TOKEN_PROVISIONING.compareAndSet(false, true)) {
            return;
        }
        nextTokenProvisionAllowedAt = now + 10_000L;
        CompletableFuture.runAsync(() -> {
            try {
                provisionWriteToken(reportUrl, state, forceRemoteSync);
            } finally {
                TOKEN_PROVISIONING.set(false);
            }
        });
    }

    private static void provisionWriteToken(String reportUrl, LocalUserState state, boolean forceRemoteSync) {
        JsonObject root = new JsonObject();
        root.addProperty("uuid", state.uuid().toString());
        root.addProperty("name", state.name());
        byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);
        String registerUrl = resolveRegisterUrl(reportUrl);
        for (int attempt = 0; attempt <= NETWORK_RETRY_COUNT; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = openHttpConnection(registerUrl, attempt);
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("x-baity-token", DEFAULT_SYNC_ACCESS_TOKEN);

                connection.getOutputStream().write(payload);
                connection.getOutputStream().flush();

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream stream = connection.getInputStream()) {
                        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        JsonElement rootElement = JsonParser.parseString(json);
                        if (!rootElement.isJsonObject()) return;
                        JsonObject obj = rootElement.getAsJsonObject();
                        String token = getAsString(obj, "token", "");
                        if (token == null || token.isBlank()) return;
                        ConfigManager.baityPresenceReportToken = token.trim();
                        ConfigManager.requestSave();
                        LOGGER.info("[PresenceSync] register ok, uuid={}, attempt={}", state.uuid(), attempt + 1);
                        nextReportAllowedAt = 0L;
                        attemptReport(System.currentTimeMillis(), true, forceRemoteSync);
                        return;
                    }
                }
                LOGGER.warn("[PresenceSync] register failed, code={}, uuid={}, attempt={}", code, state.uuid(), attempt + 1);
                if (attempt < NETWORK_RETRY_COUNT && (code == 429 || code >= 500)) {
                    sleepRetryBackoff();
                    continue;
                }
                return;
            } catch (Exception e) {
                logThrottledWarn(LAST_REGISTER_EXCEPTION_WARN_AT, "[PresenceSync] register exception, uuid={}, attempt={}, err={}", state.uuid(), attempt + 1, e.toString());
                if (attempt < NETWORK_RETRY_COUNT && shouldRetryException(e)) {
                    sleepRetryBackoff();
                    continue;
                }
                return;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
    }

    private static boolean shouldRetryException(Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SocketTimeoutException) return true;
            t = t.getCause();
        }
        return false;
    }

    private static HttpURLConnection openHttpConnection(String url, int attempt) throws Exception {
        return PresenceProxyResolver.openConnection(url, attempt);
    }

    private static void sleepRetryBackoff() {
        try {
            Thread.sleep(NETWORK_RETRY_BACKOFF_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
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

    static String toHealthUrl(String anyUrl) {
        if (anyUrl == null || anyUrl.isBlank()) return "";
        String u = anyUrl.trim();
        if (u.endsWith("/users.json")) return u.substring(0, u.length() - "/users.json".length()) + "/health";
        if (u.endsWith("/report")) return u.substring(0, u.length() - "/report".length()) + "/health";
        if (u.endsWith("/")) return u + "health";
        return u + "/health";
    }

    private static String resolveRegisterUrl(String reportUrl) {
        String trimmed = reportUrl == null ? "" : reportUrl.trim();
        if (trimmed.endsWith("/report")) {
            return trimmed.substring(0, trimmed.length() - "/report".length()) + "/register";
        }
        if (trimmed.endsWith("/")) {
            return trimmed + "register";
        }
        return trimmed + "/register";
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

    private static void applyPayload(String json, boolean overrideOwnState, boolean forceRemoteSync) {
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

            JsonObject metaObj = userObj.getAsJsonObject("meta");
            long lastSeenEpochMs = parseIsoEpochMs(getAsString(metaObj, "lastSeenAt", ""));
            if (lastSeenEpochMs <= 0L) {
                lastSeenEpochMs = parseIsoEpochMs(getAsString(metaObj, "reportedAt", ""));
            }
            boolean stale = lastSeenEpochMs > 0L && (now - lastSeenEpochMs) > SOFT_STALE_AFTER_MS;
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
                lastSeenEpochMs,
                stale
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

        if (overrideOwnState) {
            tryPushLocalIfRemoteDiffers(forceRemoteSync);
        }
    }

    private static void applyPayload(String json, boolean overrideOwnState) {
        applyPayload(json, overrideOwnState, false);
    }

    private static void tryPushLocalIfRemoteDiffers(boolean forceRemoteSync) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        LocalPlayer player = client.player;
        if (player == null) return;
        RemoteUserState remote = USERS_BY_UUID.get(player.getUUID());
        LocalUserState local = snapshotLocalState();
        if (local == null) return;
        if (remote == null) {
            attemptReport(System.currentTimeMillis(), false, forceRemoteSync);
            return;
        }
        String remoteSig = buildRemoteLikeSignature(remote);
        if (!remoteSig.equals(local.signature())) {
            attemptReport(System.currentTimeMillis(), false, forceRemoteSync);
        }
    }

    private static String buildRemoteLikeSignature(RemoteUserState r) {
        return (r.uuid() + "|" + r.name() + "|" + r.isBaityUser()
            + "|" + r.nickTweaksEnabled() + "|" + r.chromaEnabled() + "|" + r.smolPeopleEnabled()
            + "|" + r.chromaSpeed() + "|" + r.chromaSize() + "|" + r.chromaAmount() + "|" + r.chromaLightness()
            + "|" + (r.gradientStart() & 0xFFFFFF) + "|" + (r.gradientEnd() & 0xFFFFFF)
            + "|" + r.boldEnabled() + "|" + r.customNickColorEnabled() + "|" + (r.nickChanger() == null ? "" : r.nickChanger())
            + "|" + java.util.Arrays.toString(r.chromaPalette() == null ? new int[0] : r.chromaPalette()));
    }


    private static void loadCacheFromDisk() {
        try {
            if (!Files.exists(CACHE_FILE_PATH)) return;
            String json = Files.readString(CACHE_FILE_PATH, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;
            applyPayload(json, false);
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
            long lastSeenEpochMs,
            boolean stale
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
