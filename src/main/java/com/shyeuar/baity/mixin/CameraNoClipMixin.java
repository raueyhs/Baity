package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraNoClipMixin {

    @ModifyArg(
        method = "alignWithEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"),
        index = 0,
        require = 1
    )
    private float baity$overrideCameraDistance(float cameraDist) {
        return ModuleUtils.getCameraDistance(cameraDist);
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void baity$disableCameraClip(float cameraDist, CallbackInfoReturnable<Float> cir) {
        if (ModuleUtils.shouldDisableCameraClip()) {
            cir.setReturnValue(cameraDist);
        }
    }
}
