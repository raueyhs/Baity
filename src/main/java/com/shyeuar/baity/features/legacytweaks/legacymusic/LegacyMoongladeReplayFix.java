package com.shyeuar.baity.features.legacytweaks.legacymusic;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.mixin.accessor.SoundEngineAccessor;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;

import java.util.Map;

@Environment(EnvType.CLIENT)
public final class LegacyMoongladeReplayFix {

    private static final Identifier MOONGLADE_TRACK =
            Identifier.fromNamespaceAndPath("hypixel_skyblock", "music.moonglade_marsh");

    private LegacyMoongladeReplayFix() {
    }

    public static boolean shouldBlock(SoundEngine engine, SoundInstance instance) {
        if (!ConfigManager.legacyMoongladeReplayFixEnabled) {
            return false;
        }
        if (!MOONGLADE_TRACK.equals(instance.getIdentifier())) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !LocateUtils.isMoongladeMarsh(mc)) {
            return false;
        }

        return isAlreadyPlaying(engine);
    }

    private static boolean isAlreadyPlaying(SoundEngine engine) {
        Map<SoundInstance, ?> playing = ((SoundEngineAccessor) engine).baity$getPlayingInstances();
        for (SoundInstance sound : playing.keySet()) {
            if (MOONGLADE_TRACK.equals(sound.getIdentifier())) {
                return true;
            }
        }
        return false;
    }
}
