package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.CustomHandHoldingManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.shyeuar.baity.mixin.accessor.ItemInHandRendererAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CustomHandHoldingMixin {

    @Mixin(LivingEntity.class)
    public static abstract class SwingDurationMixin {

        @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
        private void baity$customSwingDuration(CallbackInfoReturnable<Integer> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            LivingEntity self = (LivingEntity) (Object) this;
            if (self != mc.player) return;

            int duration = CustomHandHoldingManager.getInstance().getSwingDuration();
            cir.setReturnValue(duration);
        }
    }

    @Mixin(Player.class)
    public static abstract class AttackCooldownMixin {

        @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
        private void baity$removeAttackCooldownAnimation(float tickDelta, CallbackInfoReturnable<Float> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            Player self = (Player) (Object) this;
            if (self != mc.player) return;

            cir.setReturnValue(1.0f);
        }
    }

    @Mixin(value = ItemInHandRenderer.class, priority = 300)
    public static abstract class HeldItemTransformMixin {

        @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                shift = At.Shift.AFTER
            )
        )
        private void baity$applyPositionOnly(AbstractClientPlayer player, float tickDelta, float pitch,
                InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                com.mojang.blaze3d.vertex.PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue,
                int light, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || player != mc.player) return;
            if (item.isEmpty()) return;

            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();

            CustomHandHoldingManager.getInstance().applyPosition(matrices, arm);
        }

        @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
            )
        )
        private void baity$applyTransform(AbstractClientPlayer player, float tickDelta, float pitch,
                InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                com.mojang.blaze3d.vertex.PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue,
                int light, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || player != mc.player) return;
            if (item.isEmpty()) return;

            if (com.shyeuar.baity.utils.BlockAnimationUtils.isFeatureActive() 
                    && com.shyeuar.baity.utils.BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                InteractionHand blockingHand = com.shyeuar.baity.utils.BlockAnimationUtils.getBlockingHand(player);
                if (blockingHand == hand) {
                    return;
                }
            }

            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();

            CustomHandHoldingManager.getInstance().applyRotation(matrices, arm);
            CustomHandHoldingManager.getInstance().applyScale(matrices);
        }


        @Inject(
            method = "swingArm",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$handleNoSwing(float swingProgress, float equipProgress, 
                com.mojang.blaze3d.vertex.PoseStack poseStack, int handSide, HumanoidArm arm, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) {
                return;
            }

            if (!CustomHandHoldingManager.getInstance().isNoSwingEnabled()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            if (com.shyeuar.baity.utils.BlockAnimationUtils.isFeatureActive() 
                    && com.shyeuar.baity.utils.BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) {
                return;
            }

            ci.cancel();
            
            
            ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) this;
            accessor.baity$callApplyItemArmTransform(poseStack, arm, equipProgress);
            accessor.baity$callApplyItemArmAttackTransform(poseStack, arm, swingProgress);
        }
    }


    @Mixin(LevelRenderer.class)
    public static abstract class LevelRendererTickMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void baity$updateCustomHandHoldingAnimations(net.minecraft.client.Camera camera, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule != null && customHandHoldingModule.isEnabled()) {
                CustomHandHoldingManager.getInstance().update();
            }
        }
    }
}
