package com.shyeuar.baity.features.blockanimation;

import com.shyeuar.baity.config.BaityConfigDir;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class BlockAnimationSwordCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/BlockAnimationSwordCatalog");

    private static final String REMOTE_CATALOG_URL =
        "https://raw.githubusercontent.com/raueyhs/Baity-repo/main/blockanimation-sword-catalog.tsv";

    private static final String CACHE_FILE_NAME = "blockanimation-sword-catalog.tsv";
    private static final String EMBEDDED_RESOURCE = "/baity/blockanimation-sword-catalog.tsv";

    private static final Set<String> SWORD_IDS = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean refreshStarted;

    private BlockAnimationSwordCatalog() {}

    public static void init() {
        if (refreshStarted) {
            return;
        }
        refreshStarted = true;

        Set<String> cached = loadFromCacheFile();
        if (!cached.isEmpty()) {
            replaceIds(cached);
        } else {
            Set<String> embedded = loadFromEmbeddedResource();
            if (!embedded.isEmpty()) {
                replaceIds(embedded);
            }
        }

        Thread.ofVirtual().name("baity-blockanimation-sword-catalog").start(BlockAnimationSwordCatalog::refreshFromRemote);
    }

    public static boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String itemId = readExtraAttributesItemId(stack);
        if (itemId.isEmpty()) {
            return false;
        }
        synchronized (SWORD_IDS) {
            return SWORD_IDS.contains(itemId);
        }
    }

    private static void refreshFromRemote() {
        String body = RemoteFileFetcher.fetchText(
                List.of(REMOTE_CATALOG_URL),
                "BlockAnimationSwordCatalog",
                ProxyFallbacks.proxies()
        );
        if (body == null || body.isBlank()) {
            return;
        }

        Set<String> remoteIds = parseTsv(body);
        if (remoteIds.isEmpty()) {
            LOGGER.warn("Remote block animation sword catalog parsed empty; keeping existing data.");
            return;
        }

        if (writeCacheFile(body)) {
            LOGGER.info("Cached {} sword ids to {}", remoteIds.size(), getCachePath());
        } else {
            LOGGER.warn("Fetched sword catalog but failed to write cache; applying in-memory only.");
        }

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> replaceIds(remoteIds));
        } else {
            replaceIds(remoteIds);
        }
    }

    private static void replaceIds(Set<String> ids) {
        synchronized (SWORD_IDS) {
            SWORD_IDS.clear();
            SWORD_IDS.addAll(ids);
        }
    }

    private static Set<String> loadFromCacheFile() {
        try {
            Path cachePath = getCachePath();
            if (!Files.isRegularFile(cachePath)) {
                return Set.of();
            }
            String body = Files.readString(cachePath, StandardCharsets.UTF_8);
            return parseTsv(body);
        } catch (Throwable ignored) {
            return Set.of();
        }
    }

    private static Set<String> loadFromEmbeddedResource() {
        try (InputStream in = BlockAnimationSwordCatalog.class.getResourceAsStream(EMBEDDED_RESOURCE)) {
            if (in == null) {
                return Set.of();
            }
            return parseTsv(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            return Set.of();
        }
    }

    private static boolean writeCacheFile(String body) {
        try {
            Path cachePath = getCachePath();
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, stripBom(body), StandardCharsets.UTF_8);
            return true;
        } catch (Throwable e) {
            LOGGER.warn("Failed to write sword catalog cache: {}", e.toString());
            return false;
        }
    }

    private static Path getCachePath() {
        return BaityConfigDir.getBaityConfigDir().resolve(CACHE_FILE_NAME);
    }

    static Set<String> parseTsv(String body) {
        Set<String> ids = new HashSet<>();
        for (String rawLine : stripBom(body).split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("item_id\t") || line.startsWith("item_id,")) {
                continue;
            }
            String itemId;
            int tab = line.indexOf('\t');
            if (tab >= 0) {
                itemId = line.substring(0, tab).trim();
            } else {
                int comma = line.indexOf(',');
                itemId = comma >= 0 ? line.substring(0, comma).trim() : line.trim();
            }
            if (!itemId.isEmpty() && !"item_id".equalsIgnoreCase(itemId)) {
                ids.add(itemId);
            }
        }
        return ids;
    }

    private static String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
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
}
