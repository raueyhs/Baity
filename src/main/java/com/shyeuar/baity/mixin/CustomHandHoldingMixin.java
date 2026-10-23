package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.CustomHandHoldingManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import com.shyeuar.baity.mixin.accessor.ItemInHandRendererAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CustomHandHoldingMixin {

    @Mixin(LivingEntity.class)
    public static abstract class SwingDurationMixin {

        @Inject(method = "getModifiedSwingDuration(Lnet/minecraft/world/item/component/SwingAnimation;)I", at = @At("HEAD"), cancellable = true)
        private void baity$customSwingDuration(net.minecraft.world.item.component.SwingAnimation animation, CallbackInfoReturnable<Integer> cir) {
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

        @Inject(method = "getAttackStrengthScale(F)F", at = @At("HEAD"), cancellable = true)
        private void baity$removeAttackCooldownAnimation(float tickDelta, CallbackInfoReturnable<Float> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            boolean disable = BlockAnimationUtils.isFeatureActive();
            if (!disable) {
                Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
                disable = customHandHoldingModule != null
                    && customHandHoldingModule.isEnabled()
                    && CustomHandHoldingManager.getInstance().isNoSwingEnabled();
            }
            if (!disable) return;

            Player self = (Player) (Object) this;
            if (self != mc.player) return;

            cir.setReturnValue(1.0f);
        }

        @Inject(method = "getItemSwapScale(F)F", at = @At("HEAD"), cancellable = true)
        private void baity$removeAttackCooldownRaiseAnimation(float tickDelta, CallbackInfoReturnable<Float> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            boolean disable = BlockAnimationUtils.isFeatureActive();
            if (!disable) {
                Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
                disable = customHandHoldingModule != null
                    && customHandHoldingModule.isEnabled()
                    && CustomHandHoldingManager.getInstance().isNoSwingEnabled();
            }
            if (!disable) return;

            Player self = (Player) (Object) this;
            if (self != mc.player) return;

            cir.setReturnValue(1.0f);
        }
    }

    @Mixin(value = FirstPersonHandsAndItemsRenderer.class, priority = 300)
    public static abstract class HeldItemTransformMixin {
        @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                shift = At.Shift.AFTER,
                ordinal = 0
            )
        )
        private void baity$applyPositionOnly(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState,
                float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                com.mojang.blaze3d.vertex.PoseStack matrices, SubmitNodeCollector queue,
                int light, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (item.isEmpty()) return;

            HumanoidArm mainArm = baity$mainArm(playerState);
            if (mainArm == null) return;
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();

            CustomHandHoldingManager.getInstance().applyPosition(matrices, arm);
        }

        @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
        )
        private void baity$applyTransform(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState,
                float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item,
                float equipProgress, com.mojang.blaze3d.vertex.PoseStack matrices, SubmitNodeCollector queue,
                int light, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (item.isEmpty()) return;

            LocalPlayer player = mc.player;
            if (com.shyeuar.baity.utils.BlockAnimationUtils.isFeatureActive() 
                    && com.shyeuar.baity.utils.BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                InteractionHand blockingHand = com.shyeuar.baity.utils.BlockAnimationUtils.getBlockingHand(player);
                if (blockingHand == hand) {
                    return;
                }
            }

            HumanoidArm mainArm = baity$mainArm(playerState);
            if (mainArm == null) return;
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();

            CustomHandHoldingManager.getInstance().applyRotation(matrices, arm);
            CustomHandHoldingManager.getInstance().applyScale(matrices);
        }

        @Inject(
            method = "swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$handleNoSwing(float swingProgress,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         int handSide,
                                         HumanoidArm arm,
                                         CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;
            if (!CustomHandHoldingManager.getInstance().isNoSwingEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (BlockAnimationUtils.isFeatureActive() && BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;

            ci.cancel();
            ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) this;
            accessor.baity$callApplyItemArmAttackTransform(poseStack, arm, swingProgress);
        }

        private static HumanoidArm baity$mainArm(PlayerRenderState playerState) {
            AvatarRenderState avatar = playerState.avatarRenderState;
            return avatar != null ? avatar.mainArm : null;
        }
    }

    @Mixin(FirstPersonHandsAndItems.class)
    public static abstract class ItemInHandRendererTickMixin {
        @Inject(method = "itemUsed(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
        private void baity$disableUseEquipJitter(InteractionHand hand, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;
            if (!CustomHandHoldingManager.getInstance().isNoSwingEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            ItemStack held = mc.player.getItemInHand(hand);
            if (held == null || held.isEmpty()) return;

            int useDuration = held.getUseDuration(mc.player);
            ItemUseAnimation anim = held.getUseAnimation();
            boolean isContinuousUse = useDuration > 0 && anim != ItemUseAnimation.NONE;
            if (isContinuousUse) return;

            ci.cancel();
        }

        @Inject(method = "tick", at = @At("TAIL"))
        private void baity$updateCustomHandHoldingAnimations(LocalPlayer player, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule != null && customHandHoldingModule.isEnabled()) {
                CustomHandHoldingManager.getInstance().update();
            }
        }
    }
}
