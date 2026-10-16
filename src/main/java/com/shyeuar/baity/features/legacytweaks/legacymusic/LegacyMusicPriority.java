package com.shyeuar.baity.features.legacytweaks.legacymusic;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class LegacyMusicPriority {

    private static final String OFFICIAL_NAMESPACE = "hypixel_skyblock";
    private static final String OFFICIAL_MUSIC_PREFIX = "music.";
    private static final String LEGACY_NAMESPACE = "minecraft";
    private static final String LEGACY_MUSIC_PREFIX = "block.note_block";

    private static final String STEREO_PANTS_ID = "MUSIC_PANTS";
    private static final double STEREO_PANTS_RANGE = 5.0;

    private static final float MIN_VOLUME = 0.02f;
    private static final int SUPPRESS_TICKS = 20;
    private static final float FADE_STEP = 0.05f;
    private static final int BURST_WINDOW = 20;
    private static final int BURST_THRESHOLD = 12;
    private static final int PITCH_WINDOW = 24;

    private static final Deque<Float> recentPitches = new ArrayDeque<>();

    private static int suppressRemaining;
    private static float fadeFactor = 1.0f;
    private static int burstCount;
    private static int burstTimer;

    private LegacyMusicPriority() {
    }

    public static void tick() {
        if (burstTimer > 0) {
            burstTimer--;
            if (burstTimer == 0) {
                burstCount = 0;
                recentPitches.clear();
            }
        }

        if (!ConfigManager.legacyMusicPriorityEnabled) {
            if (suppressRemaining > 0 || fadeFactor < 1.0f) {
                reset();
            }
            return;
        }

        if (suppressRemaining > 0) {
            suppressRemaining--;
            if (fadeFactor > 0.0f) {
                fadeFactor = Math.max(0.0f, fadeFactor - FADE_STEP);
                refreshMusicVolume();
            }
            return;
        }

        if (fadeFactor < 1.0f) {
            fadeFactor = Math.min(1.0f, fadeFactor + FADE_STEP);
            refreshMusicVolume();
        }
    }

    public static void onSoundPlay(SoundInstance instance) {
        if (!ConfigManager.legacyMusicPriorityEnabled || instance == null) {
            return;
        }

        Identifier soundId = instance.getIdentifier();
        if (!isLegacyMusic(soundId)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !LocateUtils.isHubIsland(mc)) {
            return;
        }
        if (instance.getVolume() < MIN_VOLUME) {
            return;
        }

        burstCount++;
        burstTimer = BURST_WINDOW;
        recentPitches.addLast(instance.getPitch());
        while (recentPitches.size() > PITCH_WINDOW) {
            recentPitches.removeFirst();
        }

        if (burstCount < BURST_THRESHOLD) {
            return;
        }
        if (isRegularPitchPattern()) {
            return;
        }
        if (isStereoPantsMusic(instance)) {
            return;
        }

        if (suppressRemaining <= 0) {
            fadeFactor = 1.0f;
        }
        suppressRemaining = SUPPRESS_TICKS;
    }

    private static boolean isRegularPitchPattern() {
        if (recentPitches.size() < PITCH_WINDOW) {
            return false;
        }
        List<Float> pitches = new ArrayList<>(recentPitches);
        Map<Long, Integer> deltas = new HashMap<>();
        for (int i = 1; i < pitches.size(); i++) {
            long delta = Math.round((pitches.get(i) - pitches.get(i - 1)) * 1000.0);
            deltas.merge(delta, 1, Integer::sum);
        }
        int max = 0;
        for (int count : deltas.values()) {
            max = Math.max(max, count);
        }
        return max >= (pitches.size() - 1) * 0.8;
    }

    private static boolean isStereoPantsMusic(SoundInstance sound) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return false;
        }
        AABB area = new AABB(
                sound.getX() - STEREO_PANTS_RANGE,
                sound.getY() - STEREO_PANTS_RANGE,
                sound.getZ() - STEREO_PANTS_RANGE,
                sound.getX() + STEREO_PANTS_RANGE,
                sound.getY() + STEREO_PANTS_RANGE,
                sound.getZ() + STEREO_PANTS_RANGE
        );
        for (Player player : mc.level.getEntitiesOfClass(Player.class, area)) {
            if (isWearingStereoPants(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWearingStereoPants(Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        return !leggings.isEmpty() && STEREO_PANTS_ID.equals(readSkyBlockId(leggings));
    }

    private static String readSkyBlockId(ItemStack stack) {
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

    public static float fadeFactorFor(Identifier soundId) {
        if (fadeFactor >= 1.0f || suppressRemaining <= 0) {
            return 1.0f;
        }
        if (isOfficialMusic(soundId)) {
            return fadeFactor;
        }
        return 1.0f;
    }

    private static void refreshMusicVolume() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
        }
    }

    private static void reset() {
        suppressRemaining = 0;
        fadeFactor = 1.0f;
        burstCount = 0;
        burstTimer = 0;
        recentPitches.clear();
    }

    private static boolean isOfficialMusic(Identifier soundId) {
        return OFFICIAL_NAMESPACE.equals(soundId.getNamespace())
                && soundId.getPath().startsWith(OFFICIAL_MUSIC_PREFIX);
    }

    private static boolean isLegacyMusic(Identifier soundId) {
        return LEGACY_NAMESPACE.equals(soundId.getNamespace())
                && soundId.getPath().startsWith(LEGACY_MUSIC_PREFIX);
    }
}
