package com.shyeuar.baity.features.fishing;

import com.shyeuar.baity.utils.PackMcmetaUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
public final class FishHookTimerTemplateManager {
    public static final String MOD_ID = "fishtimer";
    public static final String TEMPLATE_DIR_NAME = "baity-custom-fishing-timer";
    public static final String DOCS_URL =
            "https://github.com/raueyhs/Baity/blob/HEAD/docs/custom-fishing-timer-template.md";

    private static final String EMBED_PREFIX = "assets/baity/embed/fishing-timer/";
    private static final String PURPLE = "#FF55FF";

    private static final String BAR_TEXTURE_RESOURCE =
            EMBED_PREFIX + "textures/skyblock/fishing_timer_bar.png";

    private FishHookTimerTemplateManager() {}

    public static void init() {
        File templateRoot = templateRoot();
        if (!templateRoot.exists()) {
            createTemplatePack(templateRoot);
        } else {
            PackMcmetaUtils.write(new File(templateRoot, "pack.mcmeta"), packDescriptionJson());
        }
    }

    private static File configBaityDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("baity").toFile();
    }

    private static File templateRoot() {
        return new File(configBaityDir(), TEMPLATE_DIR_NAME);
    }

    private static void createTemplatePack(File root) {
        File textureFile = new File(root, "assets/" + MOD_ID + "/textures/skyblock/fishing_timer_bar.png");
        File soundsDir = new File(root, "assets/" + MOD_ID + "/sounds");
        textureFile.getParentFile().mkdirs();
        soundsDir.mkdirs();
        PackMcmetaUtils.write(new File(root, "pack.mcmeta"), packDescriptionJson());
        writePackIcon(new File(root, "pack.png"));
        copyResource(BAR_TEXTURE_RESOURCE, textureFile);
        for (int i = 0; i < 12; i++) {
            String name = "fishing_timer_" + i + ".ogg";
            copyResource(EMBED_PREFIX + "sounds/" + name, new File(soundsDir, name));
        }
        writeSoundsJson(new File(root, "assets/" + MOD_ID + "/sounds.json"));
    }

    private static void copyResource(String resourcePath, File target) {
        try (InputStream in = FishHookTimerTemplateManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return;
            try (FileOutputStream out = new FileOutputStream(target)) {
                in.transferTo(out);
            }
        } catch (Exception ignored) {
        }
    }

    private static void writeSoundsJson(File soundsJsonFile) {
        soundsJsonFile.getParentFile().mkdirs();
        try (InputStream in = FishHookTimerTemplateManager.class.getClassLoader()
                .getResourceAsStream(EMBED_PREFIX + "sounds.json")) {
            if (in != null) {
                try (FileOutputStream out = new FileOutputStream(soundsJsonFile)) {
                    in.transferTo(out);
                }
                return;
            }
        } catch (Exception ignored) {
        }
        String json = buildDefaultSoundsJson();
        try (FileOutputStream out = new FileOutputStream(soundsJsonFile)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static String buildDefaultSoundsJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        for (int i = 0; i < 12; i++) {
            String key = "fishing_timer_" + i;
            json.append("  \"").append(key).append("\": { \"sounds\": [\"")
                    .append(MOD_ID).append(':').append(key).append("\"] }");
            if (i < 11) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("}\n");
        return json.toString();
    }

    private static String packDescriptionJson() {
        return "[{\"text\":\"custom timer template\",\"color\":\"" + PURPLE + "\"}]";
    }

    private static void writePackIcon(File target) {
        try (InputStream in = FishHookTimerTemplateManager.class.getClassLoader()
                .getResourceAsStream("assets/baity/textures/gui/logo.png")) {
            if (in == null) return;
            try (FileOutputStream out = new FileOutputStream(target)) {
                in.transferTo(out);
            }
        } catch (Exception ignored) {
        }
    }
}