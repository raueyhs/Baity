package com.shyeuar.baity.features.fishing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.utils.LocateUtils;
import com.shyeuar.baity.utils.ProxyFallbacks;
import com.shyeuar.baity.utils.RemoteFileFetcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class HypixelFishingRodCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/HypixelFishingRodCatalog");

    private static final String PUBLIC_ITEMS_JSON =
        "https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/main/constants/Items.json";
    private static final String CACHE_FILE_NAME = "hypixel-fishing-rod-catalog.json";

    private static final Set<String> LAVA_ROD_IDS = new HashSet<>();
    private static final Set<String> WATER_ROD_IDS = new HashSet<>();
    private static volatile boolean remoteFetchStarted;
    private static volatile boolean mainHandHoldsHypixelFishingRod;

    static {
        seedBuiltInIds();
    }

    private HypixelFishingRodCatalog() {}

    private static void seedBuiltInIds() {
        for (String id : new String[] {
            "STARTER_LAVA_ROD",
            "INFERNO_ROD",
            "MAGMA_ROD",
            "HELLFIRE_ROD",
            "POLISHED_TOPAZ_ROD",
        }) {
            LAVA_ROD_IDS.add(id);
        }
        for (String id : new String[] {
            "FISHING_ROD",
            "CHALLENGE_ROD",
            "CHAMP_ROD",
            "LEGEND_ROD",
            "ROD_OF_THE_SEA",
            "GIANT_FISHING_ROD",
            "BINGO_ROD",
        }) {
            WATER_ROD_IDS.add(id);
        }
    }

    public static void init() {
        if (remoteFetchStarted) {
            return;
        }
        remoteFetchStarted = true;

        applyCatalogFromBody(loadFromCacheFile(), "cache");

        Thread.ofVirtual().name("baity-hypixel-fishing-rod-catalog").start(HypixelFishingRodCatalog::fetchRemoteCatalogAndApply);
    }

    public static void clientTickRefreshMainHandIfSkyblock(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }
        if (!LocateUtils.inSkyBlock(mc)) {
            return;
        }
        mainHandHoldsHypixelFishingRod = itemMatchesCatalogRod(mc.player.getMainHandItem());
    }

    public static boolean mainHandHoldsHypixelFishingRod() {
        return mainHandHoldsHypixelFishingRod;
    }

    public static synchronized boolean itemMatchesCatalogRod(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String id = readExtraAttributesItemId(stack);
        if (id.isEmpty()) {
            return false;
        }
        return LAVA_ROD_IDS.contains(id) || WATER_ROD_IDS.contains(id);
    }

    private static String readExtraAttributesItemId(ItemStack stack) {
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
                return "";
            }
            CompoundTag root = customData.copyTag();
            if (root == null || !root.contains("id")) {
                return "";
            }
            return root.getString("id").orElse("");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void fetchRemoteCatalogAndApply() {
        String body = RemoteFileFetcher.fetchText(
                List.of(PUBLIC_ITEMS_JSON),
                "HypixelFishingRodCatalog",
                ProxyFallbacks.proxies()
        );
        if (body == null || body.isBlank()) {
            return;
        }

        if (!applyCatalogFromBody(body, "remote")) {
            return;
        }

        if (writeCacheFile(body)) {
            LOGGER.info("Cached hypixel fishing rod catalog to {}", getCachePath());
        } else {
            LOGGER.warn("Fetched fishing rod catalog but failed to write cache; applying in-memory only.");
        }
    }

    private static boolean applyCatalogFromBody(String body, String source) {
        if (body == null || body.isBlank()) {
            return false;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Throwable e) {
            LOGGER.warn("Failed to parse {} fishing rod catalog: {}", source, e.toString());
            return false;
        }

        Set<String> newLava = new HashSet<>();
        Set<String> newWater = new HashSet<>();
        if (!parseIdArray(root.get("lava_fishing_rods"), newLava)
            || !parseIdArray(root.get("water_fishing_rods"), newWater)
            || newLava.isEmpty()
            || newWater.isEmpty()) {
            LOGGER.warn("{} fishing rod catalog parsed empty or invalid; keeping existing data.", source);
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> replaceCatalogIds(newLava, newWater));
        } else {
            replaceCatalogIds(newLava, newWater);
        }
        LOGGER.info("Loaded {} lava and {} water rod ids from {}", newLava.size(), newWater.size(), source);
        return true;
    }

    private static String loadFromCacheFile() {
        try {
            Path cachePath = getCachePath();
            if (!Files.isRegularFile(cachePath)) {
                return null;
            }
            return Files.readString(cachePath, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            LOGGER.warn("Failed to read fishing rod catalog cache: {}", e.toString());
            return null;
        }
    }

    private static boolean writeCacheFile(String body) {
        try {
            Path cachePath = getCachePath();
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, body, StandardCharsets.UTF_8);
            return true;
        } catch (Throwable e) {
            LOGGER.warn("Failed to write fishing rod catalog cache: {}", e.toString());
            return false;
        }
    }

    private static Path getCachePath() {
        return BaityConfigDir.getBaityConfigDir().resolve(CACHE_FILE_NAME);
    }

    private static boolean parseIdArray(JsonElement el, Set<String> out) {
        if (el == null || !el.isJsonArray()) {
            return false;
        }
        JsonArray arr = el.getAsJsonArray();
        for (JsonElement e : arr) {
            if (e != null && e.isJsonPrimitive()) {
                String s = e.getAsString();
                if (s != null && !s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return true;
    }

    private static synchronized void replaceCatalogIds(Set<String> lava, Set<String> water) {
        LAVA_ROD_IDS.clear();
        WATER_ROD_IDS.clear();
        LAVA_ROD_IDS.addAll(lava);
        WATER_ROD_IDS.addAll(water);
    }
}
