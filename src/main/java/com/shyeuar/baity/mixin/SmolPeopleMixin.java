package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SmolPeopleMixin {

    @Mixin(PlayerEntityRenderer.class)
    public static class SmolNameTagMixin {
        
        @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
        private void baity$adjustNameTagHeight(PlayerEntityRenderState playerEntityRenderState, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) {
                return;
            }
            if (ConfigManager.smolpeopleMode && playerEntityRenderState.name != null &&
                playerEntityRenderState.name.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                matrixStack.push();
                matrixStack.translate(0, -0.4, 0); 
            }
        }
        
        @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
        private void baity$restoreNameTagHeight(PlayerEntityRenderState playerEntityRenderState, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) {
                return;
            }
            if (ConfigManager.smolpeopleMode && playerEntityRenderState.name != null && 
                playerEntityRenderState.name.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                matrixStack.pop();
            }
        }
    }
    
    @Mixin(PlayerEntityRenderer.class)
    public static class SmolPlayerEntityRendererMixin {
        
        @Inject(method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
        private void baity$additionalScale(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) {
                return;
            }
            if (ConfigManager.smolpeopleMode && playerEntityRenderState.name != null &&
                playerEntityRenderState.name.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                matrixStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
    }

    @Mixin(PlayerEntityModel.class)
    public static class SmolPlayerRendererMixin {
        
        @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
        private void baity$modifyModel(PlayerEntityRenderState playerEntityRenderState, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) {
                return;
            }
            if (ConfigManager.smolpeopleMode && playerEntityRenderState.name != null &&
                playerEntityRenderState.name.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                
                PlayerEntityModel model = (PlayerEntityModel) (Object) this;
                
                if (playerEntityRenderState.limbSwingAmplitude > 0) {
                    float speedMultiplier = 2.5f;
                    float enhancedLimbAngle = playerEntityRenderState.limbSwingAnimationProgress * speedMultiplier;
                    float enhancedLimbDistance = Math.min(playerEntityRenderState.limbSwingAmplitude * speedMultiplier, 1.0f);
                    
                    model.rightLeg.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f) * 1.4f * enhancedLimbDistance);
                    model.leftLeg.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 1.4f * enhancedLimbDistance);
                    
                    if (!playerEntityRenderState.handSwinging) {
                        model.rightArm.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 2.0f * enhancedLimbDistance * 0.5f);
                        model.leftArm.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f) * 2.0f * enhancedLimbDistance * 0.5f);
                    }
                }
                
                try {
                    org.joml.Vector3f s = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
                    model.head.scale(s);
                } catch (Throwable ignored) {
                    // 忽略潜在的模型实现差异
                }
            }
        }
    }
}

