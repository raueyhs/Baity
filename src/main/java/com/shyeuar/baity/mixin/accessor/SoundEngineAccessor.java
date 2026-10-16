package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {

    @Accessor("instanceToChannel")
    Map<SoundInstance, ?> baity$getPlayingInstances();
}
