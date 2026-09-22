package com.shyeuar.baity.features.enchantlore;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.utils.ProxyFallbacks;
import com.shyeuar.baity.utils.RemoteFileFetcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
final class EnchantCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/EnchantCatalog");
    private static final Gson GSON = new Gson();

    private static final List<String> REMOTE_CATALOG_URLS = List.of(
        "https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/main/constants/Enchants.json",
        "https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/main/constants/enchants.json"
    );
    private static final String CACHE_FILE_NAME = "enchants.json";
    private static final String EMBEDDED_RESOURCE = "/assets/baity/enchants.json";

    private static volatile Catalog catalog = new Catalog();
    private static volatile boolean refreshStarted;

    private EnchantCatalog() {
    }

    static void init() {
        if (refreshStarted) {
            return;
        }
        refreshStarted = true;

        Catalog cached = Catalog.parse(readTextFile(getCachePath()));
        if (cached != null) {
            catalog = cached;
        } else {
            Catalog embedded = Catalog.parse(readResource(EMBEDDED_RESOURCE));
            if (embedded != null) {
                catalog = embedded;
            }
        }

        Thread.ofVirtual().name("baity-enchant-catalog").start(EnchantCatalog::refreshFromRemote);
    }

    static EnchantDef resolve(String loreName, Map<String, Integer> enchantments, Map<String, Integer> attributes) {
        if (loreName == null || loreName.isBlank() || enchantments.isEmpty()) {
            return null;
        }
        Catalog current = catalog;
        EnchantDef known = current.fromLore(loreName);
        if (known != null) {
            String nbtKey = findNbtKey(known.nbtName, enchantments);
            if (nbtKey == null || containsKeyIgnoreCase(attributes, nbtKey)) {
                return null;
            }
            return known;
        }
        String nbtKey = findNbtKey(normalizeLoreName(loreName.trim()), enchantments);
        if (nbtKey == null || containsKeyIgnoreCase(attributes, nbtKey)) {
            return null;
        }
        EnchantDef byNbtKey = current.fromNbtKey(nbtKey);
        if (byNbtKey != null) {
            return byNbtKey.withLoreName(loreName.trim());
        }
        return EnchantDef.unknown(loreName.trim(), nbtKey);
    }

    private static void refreshFromRemote() {
        String body = RemoteFileFetcher.fetchText(
                REMOTE_CATALOG_URLS,
                "EnchantCatalog",
                ProxyFallbacks.proxies()
        );
        if (body == null || body.isBlank()) {
            return;
        }

        Catalog parsed = Catalog.parse(body);
        if (parsed == null) {
            LOGGER.warn("Remote enchant catalog parsed without usable data; keeping existing data.");
            return;
        }

        if (writeCacheFile(body)) {
            LOGGER.info("Cached enchant catalog to {}", getCachePath());
        } else {
            LOGGER.warn("Fetched enchant catalog but failed to write cache; applying in-memory only.");
        }

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> install(parsed));
        } else {
            install(parsed);
        }
    }

    private static void install(Catalog parsed) {
        catalog = parsed;
        EnchantLore.invalidateCache();
    }

    private static String readResource(String resource) {
        try (InputStream in = EnchantCatalog.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String readTextFile(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean writeCacheFile(String body) {
        try {
            Path cachePath = getCachePath();
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, stripBom(body), StandardCharsets.UTF_8);
            return true;
        } catch (Throwable e) {
            LOGGER.warn("Failed to write enchant catalog cache: {}", e.toString());
            return false;
        }
    }

    private static Path getCachePath() {
        return BaityConfigDir.getBaityConfigDir().resolve(CACHE_FILE_NAME);
    }

    private static String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private static String normalizeLoreName(String loreName) {
        StringBuilder out = new StringBuilder(loreName.length());
        for (int i = 0; i < loreName.length(); i++) {
            char c = loreName.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                out.append(Character.toLowerCase(c));
            } else if (c == ' ' || c == '-') {
                out.append('_');
            }
        }
        return out.toString();
    }

    private static String findNbtKey(String nbtName, Map<String, Integer> enchantments) {
        if (nbtName == null || nbtName.isBlank()) {
            return null;
        }
        if (enchantments.containsKey(nbtName)) {
            return nbtName;
        }
        for (String key : enchantments.keySet()) {
            if (key.equalsIgnoreCase(nbtName)) {
                return key;
            }
        }
        return null;
    }

    private static boolean containsKeyIgnoreCase(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            return true;
        }
        for (String candidate : map.keySet()) {
            if (candidate.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    static final class Catalog {
        @SerializedName("NORMAL")
        HashMap<String, RawEnchant> normal = new HashMap<>();
        @SerializedName("ULTIMATE")
        HashMap<String, RawEnchant> ultimate = new HashMap<>();
        @SerializedName("STACKING")
        HashMap<String, RawEnchant> stacking = new HashMap<>();
        final Map<String, EnchantDef> byLoreKey = new HashMap<>();
        final Map<String, EnchantDef> byNbtKey = new HashMap<>();

        static Catalog parse(String body) {
            if (body == null || body.isBlank()) {
                return null;
            }
            Catalog loaded;
            try {
                loaded = GSON.fromJson(body, Catalog.class);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse enchant catalog: {}", e.toString());
                return null;
            }
            if (loaded == null) {
                return null;
            }
            loaded.index();
            if (loaded.normal.isEmpty() || loaded.ultimate.isEmpty()) {
                LOGGER.warn("Enchant catalog parsed without normal or ultimate enchants.");
                return null;
            }
            return loaded;
        }

        void index() {
            indexGroup(normal, false, false);
            indexGroup(ultimate, true, false);
            indexGroup(stacking, false, true);
        }

        void indexGroup(Map<String, RawEnchant> group, boolean ultimate, boolean stacking) {
            for (RawEnchant raw : group.values()) {
                if (raw == null || raw.loreName == null || raw.loreName.isBlank() || raw.nbtName == null) {
                    continue;
                }
                EnchantDef def = new EnchantDef(
                        raw.loreName,
                        raw.nbtName,
                        raw.goodLevel,
                        raw.maxLevel,
                        ultimate,
                        stacking,
                        raw.maxLevel <= 0
                );
                byLoreKey.put(raw.loreName.toLowerCase(Locale.US), def);
                byNbtKey.putIfAbsent(raw.nbtName.toLowerCase(Locale.US), def);
            }
        }

        EnchantDef fromLore(String loreName) {
            if (loreName == null || loreName.isBlank()) {
                return null;
            }
            return byLoreKey.get(loreName.trim().toLowerCase(Locale.US));
        }

        EnchantDef fromNbtKey(String nbtName) {
            if (nbtName == null || nbtName.isBlank()) {
                return null;
            }
            return byNbtKey.get(nbtName.toLowerCase(Locale.US));
        }
    }

    static final class EnchantDef implements Comparable<EnchantDef> {
        private static final Comparator<EnchantDef> ENCHANT_ORDER = Comparator
                .comparingInt((EnchantDef e) -> e.ultimate ? 0 : 1)
                .thenComparingInt(e -> e.stacking ? 0 : 1)
                .thenComparing(e -> e.loreName);
        final String loreName;
        final String nbtName;
        final int goodLevel;
        final int maxLevel;
        final boolean ultimate;
        final boolean stacking;
        final boolean unknown;

        EnchantDef(String loreName, String nbtName, int goodLevel, int maxLevel,
                   boolean ultimate, boolean stacking, boolean unknown) {
            this.loreName = loreName;
            this.nbtName = nbtName;
            this.goodLevel = goodLevel;
            this.maxLevel = maxLevel;
            this.ultimate = ultimate;
            this.stacking = stacking;
            this.unknown = unknown;
        }

        static EnchantDef unknown(String loreName, String nbtName) {
            return new EnchantDef(
                    loreName,
                    nbtName,
                    0,
                    0,
                    nbtName.toLowerCase(Locale.US).startsWith("ultimate_"),
                    false,
                    true
            );
        }

        EnchantDef withLoreName(String name) {
            return new EnchantDef(name, nbtName, goodLevel, maxLevel, ultimate, stacking, unknown);
        }

        EnchantLore.Tier tierFor(int level) {
            if (ultimate) {
                return EnchantLore.Tier.ULTIMATE;
            }
            if (unknown) {
                return EnchantLore.Tier.GOOD;
            }
            if (level >= maxLevel) {
                return EnchantLore.Tier.PERFECT;
            }
            if (level > goodLevel) {
                return EnchantLore.Tier.GREAT;
            }
            if (level == goodLevel) {
                return EnchantLore.Tier.GOOD;
            }
            return EnchantLore.Tier.POOR;
        }

        @Override
        public int compareTo(EnchantDef other) {
            return ENCHANT_ORDER.compare(this, other);
        }
    }

    private static final class RawEnchant {
        @SerializedName("loreName")
        String loreName;
        @SerializedName("nbtName")
        String nbtName;
        @SerializedName("goodLevel")
        int goodLevel;
        @SerializedName("maxLevel")
        int maxLevel;
    }
}
