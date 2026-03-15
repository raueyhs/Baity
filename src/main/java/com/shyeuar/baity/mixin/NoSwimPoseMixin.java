package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class NoSwimPoseMixin {

    @Mixin(AvatarRenderer.class)
    public static class PlayerRendererTransformMixin {

        @Inject(method = "setupRotations", at = @At("HEAD"))
        private void baity$preventSwimTransform(AvatarRenderState state, PoseStack matrixStack, float f, float g, CallbackInfo ci) {
            if (!NoSwimPoseUtils.isSelfPlayerById(state.id)) return;
            if (!NoSwimPoseUtils.isFeatureActive()) return;

            if (state.isVisuallySwimming || state.swimAmount > 0.0F) {
                state.isVisuallySwimming = false;
                state.swimAmount = 0.0F;

                if (NoSwimPoseUtils.isSneaking()) {
                    state.isCrouching = true;
                }
            }
        }
    }

    @Mixin(PlayerModel.class)
    public static class PlayerModelPoseMixin {

        @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
                at = @At("HEAD"))
        private void baity$modifySwimmingPose(AvatarRenderState state, CallbackInfo ci) {
            if (!NoSwimPoseUtils.isSelfPlayerById(state.id)) return;
            if (!NoSwimPoseUtils.isFeatureActive()) return;

            if (state.isVisuallySwimming || state.swimAmount > 0.0F) {
                state.isVisuallySwimming = false;
                state.swimAmount = 0.0F;

                if (NoSwimPoseUtils.isSneaking()) {
                    state.isCrouching = true;
                }
            }
        }
    }

    @Mixin(Camera.class)
    public static class PlayerCameraMixin {

        @ModifyExpressionValue(method = "setup", 
                at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 1))
        private float baity$modifyEyeHeightLerp(float original, @Local(argsOnly = true) Entity focusedEntity) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || focusedEntity != mc.player) return original;

            if (NoSwimPoseUtils.shouldApplyEyeHeightChange()) {
                return NoSwimPoseUtils.STANDING_EYE_HEIGHT;
            }

            return original;
        }
    }

    @Mixin(AvatarRenderer.class)
    public static class PlayerNameTagMixin {

        @Inject(method = "submitNameTag", at = @At("HEAD"))
        private void baity$adjustNameTagHeight(AvatarRenderState state, com.mojang.blaze3d.vertex.PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue, net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (!NoSwimPoseUtils.isSelfPlayerById(state.id)) return;
            if (!NoSwimPoseUtils.isFeatureActive()) return;
            if (mc.player.getPose() != Pose.SWIMMING) return;

            matrices.translate(0, 1.2, 0);
        }
    }

    @Mixin(Entity.class)
    public static class EntityEyeHeightMixin {

        @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
        private void baity$modifyCameraPosVec(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            Entity self = (Entity) (Object) this;
            if (!(self instanceof Player)) return;
            if (!NoSwimPoseUtils.isSelfPlayer(self)) return;

            if (NoSwimPoseUtils.shouldApplyEyeHeightChange()) {
                double x = mc.player.getX();
                double y = mc.player.getY() + NoSwimPoseUtils.STANDING_EYE_HEIGHT;
                double z = mc.player.getZ();
                cir.setReturnValue(new Vec3(x, y, z));
            }
        }

        @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("HEAD"), cancellable = true)
        private void baity$modifyEyeHeight(Pose pose, CallbackInfoReturnable<Float> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            Entity self = (Entity) (Object) this;
            if (!(self instanceof Player)) return;
            if (!NoSwimPoseUtils.isSelfPlayer(self)) return;

            if (NoSwimPoseUtils.shouldApplyEyeHeightChange()) {
                cir.setReturnValue(NoSwimPoseUtils.STANDING_EYE_HEIGHT);
            }
        }
    }
}
