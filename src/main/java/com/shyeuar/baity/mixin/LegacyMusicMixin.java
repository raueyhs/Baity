package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.legacytweaks.legacymusic.LegacyMoongladeReplayFix;
import com.shyeuar.baity.features.legacytweaks.legacymusic.LegacyMusicPriority;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class LegacyMusicMixin {

    @Inject(
        method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void baity$legacyMusicPlay(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir
    ) {
        SoundEngine engine = (SoundEngine) (Object) this;
        if (LegacyMoongladeReplayFix.shouldBlock(engine, sound)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        LegacyMusicPriority.onSoundPlay(sound);
    }

    @Inject(
        method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void baity$applyLegacyMusicFade(
            SoundInstance instance,
            CallbackInfoReturnable<Float> cir
    ) {
        float factor = LegacyMusicPriority.fadeFactorFor(instance.getIdentifier());
        if (factor < 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
