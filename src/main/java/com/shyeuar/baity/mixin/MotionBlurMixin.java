package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.shyeuar.baity.features.MotionBlur;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;

public class MotionBlurMixin {


    private static final String RENDER_LEVEL =
            "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Z"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
                    + "Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V";

    @Mixin(value = LevelRenderer.class, priority = 800)
    public abstract static class LevelRendererMixin {

        @Shadow
        @Final
        private LevelTargetBundle targets;

        @Shadow
        private void executeClassicTransparency(
                ChunkSectionsToRender chunkSectionsToRender,
                FeatureRenderDispatcher.PreparedFrame featureFrame,
                RenderPass renderPass) {
            throw new AssertionError();
        }

        @Unique
        private boolean baity$mainPassSplit;

        @Unique
        private boolean baity$insideTransparencyReplacement;

        @Unique
        private RenderPass baity$closedTerrainPass;

        @Inject(method = RENDER_LEVEL, at = @At("HEAD"))
        private void baity$onRenderHead(
                GraphicsResourceAllocator resourceAllocator,
                boolean renderOutline,
                CameraRenderState cameraState,
                GpuBufferSlice terrainFog,
                Vector4f fogColor,
                boolean shouldRenderSky,
                boolean shouldRenderClouds,
                CallbackInfo ci) {
            baity$mainPassSplit = false;
            baity$insideTransparencyReplacement = false;
            baity$closedTerrainPass = null;
            MotionBlur.onRenderHead(
                    resourceAllocator,
                    cameraState.viewRotationMatrix,
                    cameraState.projectionMatrix,
                    cameraState.pos.x(),
                    cameraState.pos.y(),
                    cameraState.pos.z());
        }

        @Redirect(
                method = "executeSolid",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid"
                                + "(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"
                )
        )
        private void baity$renderVelocityBeforeSolidFeatures(
                FeatureRenderDispatcher.PreparedFrame featureFrame,
                RenderPass terrainPass) {
            if (!MotionBlur.needsPreEntityVelocity()) {
                baity$mainPassSplit = false;
                baity$closedTerrainPass = null;
                featureFrame.executeSolid(terrainPass);
                return;
            }

            baity$mainPassSplit = true;
            baity$closedTerrainPass = terrainPass;
            terrainPass.close();
            MotionBlur.onAfterOpaqueTerrain();

            RenderTarget mainTarget = this.targets.main.get();
            try (RenderPass featurePass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "Baity / Motion blur solid features",
                            mainTarget.getColorTextureView(),
                            Optional.empty(),
                            mainTarget.getDepthTextureView(),
                            OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(featurePass);
                featureFrame.executeSolid(featurePass);
            }
        }

        @Inject(method = "executeClassicTransparency", at = @At("HEAD"), cancellable = true)
        private void baity$resumeTransparencyAfterVelocity(
                ChunkSectionsToRender chunkSectionsToRender,
                FeatureRenderDispatcher.PreparedFrame featureFrame,
                RenderPass closedTerrainPass,
                CallbackInfo ci) {
            if (!baity$mainPassSplit || baity$insideTransparencyReplacement) {
                return;
            }
            if (closedTerrainPass != baity$closedTerrainPass) {
                return;
            }

            RenderTarget mainTarget = this.targets.main.get();
            baity$insideTransparencyReplacement = true;
            try (RenderPass transparencyPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "Baity / Motion blur transparency",
                            mainTarget.getColorTextureView(),
                            Optional.empty(),
                            mainTarget.getDepthTextureView(),
                            OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(transparencyPass);
                executeClassicTransparency(chunkSectionsToRender, featureFrame, transparencyPass);
            } finally {
                baity$insideTransparencyReplacement = false;
            }

            baity$closedTerrainPass = null;
            ci.cancel();
        }

        @Inject(method = RENDER_LEVEL, at = @At("TAIL"))
        private void baity$onRenderLevelTail(
                GraphicsResourceAllocator resourceAllocator,
                boolean renderOutline,
                CameraRenderState cameraState,
                GpuBufferSlice terrainFog,
                Vector4f fogColor,
                boolean shouldRenderSky,
                boolean shouldRenderClouds,
                CallbackInfo ci) {
            if (MotionBlur.isActive()) {
                MotionBlur.onAfterLevel();
            }
            baity$mainPassSplit = false;
            baity$closedTerrainPass = null;
        }
    }
}
