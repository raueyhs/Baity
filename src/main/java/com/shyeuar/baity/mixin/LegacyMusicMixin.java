package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.legacytweaks.legacymusic.LegacyMoongladeReplayFix;
import com.shyeuar.baity.features.legacytweaks.legacymusic.LegacyMusicPriority;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
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
        if (LegacyMusicPriority.shouldBlockMusic(sound)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    // 26.3 resolves the Sound instance inside play(), so volume/pitch are not readable at HEAD
    // anymore (they throw). Analyse the note after the sound has actually been started.
    @Inject(
        method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
        at = @At("RETURN")
    )
    private void baity$legacyMusicPlayStarted(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir
    ) {
        if (cir.getReturnValue() == SoundEngine.PlayResult.NOT_STARTED) {
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

    // 26.3 resolves the volume through the (volume, source) overload, which is also the one used
    // when refreshing category volumes, so the fade has to be applied here as well. 26.2 only had
    // the SoundInstance overload.
    @Inject(
        method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void baity$applyLegacyMusicFadeBySource(
            float volume,
            SoundSource source,
            CallbackInfoReturnable<Float> cir
    ) {
        if (source != SoundSource.MUSIC) {
            return;
        }
        float factor = LegacyMusicPriority.currentFadeFactor();
        if (factor < 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
