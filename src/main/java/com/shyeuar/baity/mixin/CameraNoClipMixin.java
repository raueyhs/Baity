package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraNoClipMixin {

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void baity$disableCameraClip(float cameraDist, CallbackInfoReturnable<Float> cir) {
        if (ConfigManager.cameraNoClipEnabled) {
            cir.setReturnValue(cameraDist);
        }
    }
}
