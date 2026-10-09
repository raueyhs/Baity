package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

public class CullingMixin {
    @Mixin(value = FogRenderer.class, priority = 1500)
    public static class RemoveWaterFogMixin {
        
        @Shadow
        @Final
        private static List<FogEnvironment> FOG_ENVIRONMENTS;
        
        @Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
                at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F", shift = At.Shift.AFTER, ordinal = 0))
        private void baity$removeWaterFog(Camera camera, int viewDistance,
                DeltaTracker tickCounter, float skyDarkness, ClientLevel world, 
                CallbackInfoReturnable<FogData> cir, @Local FogData fogData) {
            
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingRemoveUnderwaterFog) return;
            
            FogType submersionType = camera.getFluidInCamera();
            Entity entity = camera.entity();
            
            if (submersionType != FogType.WATER) return;
            
            for (int i = 0; i < FOG_ENVIRONMENTS.size(); i++) {
                FogEnvironment modifier = FOG_ENVIRONMENTS.get(i);
                if (modifier.isApplicable(submersionType, entity)) {
                    fogData.environmentalStart = Float.MAX_VALUE;
                    fogData.environmentalEnd = Float.MAX_VALUE;
                    fogData.renderDistanceStart = Float.MAX_VALUE;
                    fogData.renderDistanceEnd = Float.MAX_VALUE;
                    break;
                }
            }
        }
    }
    
    @Mixin(WeatherEffectRenderer.class)
    public static class RemoveRainSnowVisualMixin {
        
        @Inject(
            method = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$skipRainSnowRender(
                Vec3 cameraPosition,
                WeatherRenderState renderState,
                CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingRemoveRainSnow) return;
            ci.cancel();
        }

        @Inject(
            method = "tickRainParticles(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/Camera;ILnet/minecraft/server/level/ParticleStatus;I)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$skipRainSnowParticles(
                ClientLevel level,
                Camera camera,
                int ticks,
                ParticleStatus particleStatus,
                int weatherRadius,
                CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingRemoveRainSnow) return;
            ci.cancel();
        }
    }

    @Mixin(net.minecraft.world.attribute.EnvironmentAttributeSystem.class)
    public static class RemoveWeatherFilterMixin {

        @ModifyExpressionValue(method = "addDefaultLayers", require = 1, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/level/Level;canHaveWeather()Z"))
        private static boolean baity$skipWeatherLayerForClientOnly(
                boolean original,
                @Local(argsOnly = true) net.minecraft.world.level.Level level) {
            if (!original) return false;
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel)) return original;
            return !ModuleUtils.shouldRemoveCullingWeatherFilter();
        }
    }

    @Mixin(net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment.class)
    public static class RemoveWeatherFogFilterMixin {

        @Shadow
        private float rainFogMultiplier;

        @Inject(method = "updateRainFogState", at = @At("HEAD"), cancellable = true, require = 1)
        private void baity$skipRainFog(
                net.minecraft.client.Camera camera,
                net.minecraft.client.multiplayer.ClientLevel level,
                net.minecraft.client.DeltaTracker deltaTracker,
                CallbackInfo ci) {
            if (!ModuleUtils.shouldRemoveCullingWeatherFilter()) return;
            this.rainFogMultiplier = 0.0F;
            ci.cancel();
        }

        @Inject(method = "applyWeatherDarken", at = @At("HEAD"), cancellable = true, require = 1)
        private static void baity$skipWeatherDarken(
                int color,
                float rainLevel,
                float thunderLevel,
                CallbackInfoReturnable<Integer> cir) {
            if (!ModuleUtils.shouldRemoveCullingWeatherFilter()) return;
            cir.setReturnValue(color);
        }
    }

    @Mixin(net.minecraft.client.renderer.SkyRenderer.class)
    public static class RemoveWeatherSkyFilterMixin {

        @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
        private void baity$keepSunAndMoon(
                net.minecraft.client.multiplayer.ClientLevel level,
                float partialTicks,
                net.minecraft.client.Camera camera,
                net.minecraft.client.renderer.state.level.SkyRenderState state,
                CallbackInfo ci) {
            if (!ModuleUtils.shouldRemoveCullingWeatherFilter()) return;
            state.rainBrightness = 1.0F;
        }
    }
    
    @Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
    public static class HideDyingMobMixin {
        
        @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$hideDyingMob(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue,
                net.minecraft.client.renderer.state.level.CameraRenderState cameraState, CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingHideDyingMob) return;
            
            if (state.deathTime > 0) {
                ci.cancel();
            }
        }
    }
    
    @Mixin(GameRenderer.class)
    public static class NoHurtCamMixin {
        
        @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
        private void baity$cancelHurtCam(CameraRenderState cameraState, PoseStack matrices, CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("NoHurtCam");
            if (m != null && m.isEnabled()) {
                ci.cancel();
            }
        }
    }
}
