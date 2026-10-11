package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.FogRendererAccessor;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NoDebuffMixin {
    
    @Mixin(GameRenderer.class)
    public static class GameRendererMixin {

        @Shadow @Final private FogRenderer fogRenderer;

        @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotate(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;", ordinal = 0), require = 1)
        private Matrix4f baity$preventNauseaRotation(Matrix4f matrix, float angle, Vector3fc axis, Operation<Matrix4f> original) {
            if (shouldRemoveNausea()) {
                return matrix;
            }
            return original.call(matrix, angle, axis);
        }

        @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotate(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;", ordinal = 1), require = 1)
        private Matrix4f baity$preventNauseaRotationSecond(Matrix4f matrix, float angle, Vector3fc axis, Operation<Matrix4f> original) {
            if (shouldRemoveNausea()) {
                return matrix;
            }
            return original.call(matrix, angle, axis);
        }

        @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;scale(FFF)Lorg/joml/Matrix4f;"), require = 1)
        private Matrix4f baity$preventNauseaScaling(Matrix4f matrix, float x, float y, float z, Operation<Matrix4f> original) {
            if (shouldRemoveNausea()) {
                return matrix;
            }
            return original.call(matrix, x, y, z);
        }

        @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), require = 1)
        private void baity$clearBlindnessFog(DeltaTracker deltaTracker, CallbackInfo ci) {
            var fogEnvironments = FogRendererAccessor.baity$getFogEnvironments();
            if (shouldRemoveBlindness()) {
                fogEnvironments.removeIf(env -> env instanceof BlindnessFogEnvironment);
            }
        }

        private static boolean shouldRemoveNausea() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove nausea", true);
        }

        private static boolean shouldRemoveBlindness() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove blindness", true);
        }
    }

    @Mixin(FogRenderer.class)
    public static class FogRendererMixin {

        @WrapOperation(method = {
                "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
                "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;"
        }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;isApplicable(Lnet/minecraft/world/level/material/FogType;Lnet/minecraft/world/entity/Entity;)Z"), require = 2)
        private boolean baity$skipDarknessFog(FogEnvironment environment, FogType fogType, Entity entity, Operation<Boolean> original) {
            if (environment instanceof DarknessFogEnvironment && shouldRemoveDarkness()) {
                return false;
            }
            return original.call(environment, fogType, entity);
        }

        @WrapOperation(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 1), require = 1)
        private boolean baity$preventDarknessFromBlockingNightVision(LivingEntity entity, Holder<MobEffect> effect, Operation<Boolean> original) {
            if (shouldRemoveDarkness()) {
                return false;
            }
            return original.call(entity, effect);
        }

        private static boolean shouldRemoveDarkness() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove darkness", true);
        }
    }

    @Mixin(Camera.class)
    public static class CameraMixin {

        @WrapOperation(method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 1), require = 1)
        private boolean baity$allowSkyDuringDarkness(LivingEntity entity, Holder<MobEffect> effect, Operation<Boolean> original) {
            if (shouldRemoveDarkness()) {
                return false;
            }
            return original.call(entity, effect);
        }

        private static boolean shouldRemoveDarkness() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove darkness", true);
        }
    }

    @Mixin(LightmapRenderStateExtractor.class)
    public static class LightmapRenderStateExtractorMixin {

        @WrapOperation(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"), require = 1)
        private float baity$removeDarknessBlend(LocalPlayer player, Holder<MobEffect> effect, float partialTick, Operation<Float> original) {
            if (shouldRemoveDarkness()) {
                return 0.0F;
            }
            return original.call(player, effect, partialTick);
        }

        private static boolean shouldRemoveDarkness() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove darkness", true);
        }
    }

    @Mixin(SkyRenderer.class)
    public static class SkyRendererMixin {

        @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
        private void baity$cancelDarknessOverlay(CallbackInfo ci) {
            if (shouldRemoveDarkness()) {
                ci.cancel();
            }
        }

        private static boolean shouldRemoveDarkness() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove darkness", true);
        }
    }

    @Mixin(Hud.class)
    public static class HudMixin {

        @Inject(method = "extractConfusionOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", at = @At("HEAD"), cancellable = true, require = 1)
        private void baity$cancelNauseaOverlay(GuiGraphicsExtractor guiGraphics, float intensity, CallbackInfo ci) {
            if (shouldRemoveNausea()) {
                ci.cancel();
            }
        }

        private static boolean shouldRemoveNausea() {
            Module module = ModuleManager.getModuleByName("NoDebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove nausea", true);
        }
    }
}
