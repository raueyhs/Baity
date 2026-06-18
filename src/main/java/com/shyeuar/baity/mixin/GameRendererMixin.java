package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.shyeuar.baity.features.MotionBlur;
import com.shyeuar.baity.mixin.accessor.CameraAccessor;
import com.shyeuar.baity.mixin.accessor.CameraRenderStateAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "extractCamera", at = @At("TAIL"))
    private void baity$populateCameraRenderState(CallbackInfo ci, @Local CameraRenderState cameraRenderState) {
        CameraRenderStateAccessor cameraAccessor = (CameraRenderStateAccessor) cameraRenderState;
        cameraAccessor.baity$setId(this.mainCamera.entity().getId());
        cameraAccessor.baity$setPartialTickTime(this.mainCamera.getPartialTickTime());
        cameraAccessor.baity$setOldEyeHeight(((CameraAccessor) this.mainCamera).baity$getOldEyeHeight());
        cameraAccessor.baity$setEyeHeight(((CameraAccessor) this.mainCamera).baity$getEyeHeight());
        cameraAccessor.baity$setWorldCamera(true);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void baity$beginWorldRenderPhase(CallbackInfo ci) {
        com.shyeuar.baity.render.RenderScope.enterWorldRenderPhase();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void baity$endWorldRenderPhase(CallbackInfo ci) {
        com.shyeuar.baity.render.RenderScope.exitWorldRenderPhase();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void baity$clearMotionBlurAllocator(CallbackInfo ci) {
        MotionBlur.clearFrameAllocator();
    }
}
