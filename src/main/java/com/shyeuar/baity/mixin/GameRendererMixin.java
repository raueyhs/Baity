package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.shyeuar.baity.features.MotionBlur;
import com.shyeuar.baity.mixin.accessor.CameraAccessor;
import com.shyeuar.baity.mixin.accessor.CameraRenderStateAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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

    @Inject(method = "extractCamera(Lnet/minecraft/client/DeltaTracker;F)V", at = @At("TAIL"), require = 1)
    private void baity$populateCameraRenderState(CallbackInfo ci, @Local CameraRenderState cameraRenderState) {
        CameraRenderStateAccessor cameraAccessor = (CameraRenderStateAccessor) cameraRenderState;
        cameraAccessor.baity$setId(this.mainCamera.entity().getId());
        cameraAccessor.baity$setPartialTickTime(this.mainCamera.getCameraEntityPartialTicks(net.minecraft.client.Minecraft.getInstance().getDeltaTracker()));
        cameraAccessor.baity$setOldEyeHeight(((CameraAccessor) this.mainCamera).baity$getOldEyeHeight());
        cameraAccessor.baity$setEyeHeight(((CameraAccessor) this.mainCamera).baity$getEyeHeight());
        cameraAccessor.baity$setWorldCamera(true);
    }

    @Inject(method = "renderLevel()V", at = @At("HEAD"), require = 1)
    private void baity$beginWorldRenderPhase(CallbackInfo ci) {
        com.shyeuar.baity.render.RenderScope.enterWorldRenderPhase();
    }

    @Inject(method = "renderLevel()V", at = @At("RETURN"), require = 1)
    private void baity$endWorldRenderPhase(CallbackInfo ci) {
        com.shyeuar.baity.render.RenderScope.exitWorldRenderPhase();
    }

    @Inject(method = "renderLevel()V", at = @At("TAIL"), require = 1)
    private void baity$clearMotionBlurAllocator(CallbackInfo ci) {
        MotionBlur.clearFrameAllocator();
    }
}
