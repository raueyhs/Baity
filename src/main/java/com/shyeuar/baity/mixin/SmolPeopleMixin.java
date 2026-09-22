package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.smolpeople.SmolFriendManager;
import com.shyeuar.baity.features.smolpeople.SmolPeopleCamera;
import com.shyeuar.baity.features.smolpeople.SmolPeopleNametag;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SmolPeopleMixin {

    private static final String AVATAR_SUBMIT_NAME_DISPLAY =
        "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Mixin(AvatarRenderer.class)
    public static class SmolNameTagMixin {

        @Inject(
            method = AVATAR_SUBMIT_NAME_DISPLAY,
            at = @At("HEAD")
        )
        private void baity$adjustNameTagHeight(
            AvatarRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            float offset = SmolPeopleNametag.getVerticalOffset(state.id);
            if (offset != 0f) {
                matrices.translate(0, offset, 0);
            }
        }
    }

    @Mixin(AvatarRenderer.class)
    public static abstract class SmolPlayerEntityRendererMixin
            extends LivingEntityRenderer<
                    net.minecraft.client.player.AbstractClientPlayer,
                    AvatarRenderState,
                    PlayerModel> {

        protected SmolPlayerEntityRendererMixin() {
            super(null, null, 0);
        }

        @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
        private void baity$additionalScale(AvatarRenderState playerEntityRenderState, PoseStack matrixStack, CallbackInfo ci) {
            if (SmolFriendManager.shouldApplySmolTo(playerEntityRenderState.id)) {
                matrixStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
    }

    @Mixin(PlayerModel.class)
    public static class SmolPlayerRendererMixin {

        @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
        private void baity$modifyModel(AvatarRenderState playerEntityRenderState, CallbackInfo ci) {
            if (!SmolFriendManager.shouldApplySmolTo(playerEntityRenderState.id)) {
                return;
            }

            PlayerModel model = (PlayerModel) (Object) this;
            model.head.xScale = 2.0f;
            model.head.yScale = 2.0f;
            model.head.zScale = 2.0f;

            Player targetPlayer = SmolFriendManager.getPlayerByEntityId(playerEntityRenderState.id);
            if (targetPlayer != null && (targetPlayer.isSwimming() || targetPlayer.isUnderWater())) {
                return;
            }

            if (playerEntityRenderState.walkAnimationSpeed > 0) {
                float speedMultiplier = (float) ConfigManager.smolLimbSwingSpeed;
                float enhancedLimbAngle = playerEntityRenderState.walkAnimationPos * speedMultiplier;
                float enhancedLimbDistance = Math.min(playerEntityRenderState.walkAnimationSpeed * speedMultiplier, 1.0f);

                model.rightLeg.xRot = (float) (Math.cos(enhancedLimbAngle * 0.6662f) * 1.4f * enhancedLimbDistance);
                model.leftLeg.xRot = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 1.4f * enhancedLimbDistance);

                float originalLimbAngle = playerEntityRenderState.walkAnimationPos;
                float originalArmSwing = (float) (Math.cos(originalLimbAngle * 0.6662f + Math.PI) * 2.0f * playerEntityRenderState.walkAnimationSpeed * 0.5f);
                float enhancedArmSwing = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 2.0f * enhancedLimbDistance * 0.5f);
                float armSwingDelta = enhancedArmSwing - originalArmSwing;

                model.rightArm.xRot += armSwingDelta;
                model.leftArm.xRot -= armSwingDelta;
            }
        }
    }

    @Mixin(Camera.class)
    public static class SmolCameraMixin {

        @Shadow
        private Vec3 position;

        @Shadow
        private Entity entity;

        @Inject(method = "alignWithEntity", at = @At("TAIL"))
        private void baity$adjustCameraForSmolPeople(float tickDelta, CallbackInfo ci) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || this.entity != mc.player) {
                return;
            }
            this.position = SmolPeopleCamera.applyThirdPersonFrontOffset(this.position);
        }
    }
}
