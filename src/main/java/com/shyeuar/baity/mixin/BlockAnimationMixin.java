package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

public abstract class BlockAnimationMixin {

    @Mixin(value = FirstPersonHandsAndItemsRenderer.class, priority = 500)
    public static abstract class ItemInHandRendererMixin {
        @Shadow
        @Final
        private Minecraft minecraft;

        @Invoker("applyItemArmAttackTransform")
        public abstract void baity$callApplyItemArmAttackTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, HumanoidArm hand, float swingProgress);

        @Invoker("applyItemArmTransform")
        public abstract void baity$callApplyItemArmTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, HumanoidArm hand, float equippedProgress);

        @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At("HEAD"), cancellable = true)
        private void baity$submitArmWithItem(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState,
                float partialTicks, float pitch, InteractionHand interactionHand, float swingProgress,
                net.minecraft.world.item.ItemStack stack, float equippedProgress,
                com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, 
                int combinedLight, CallbackInfo callback) {
            
            LocalPlayer player = minecraft.player;
            if (player == null) return;

            FirstPersonHandsAndItemsRenderer itemInHandRenderer = (FirstPersonHandsAndItemsRenderer) (Object) this;
            com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationRenderer.RenderResult result = 
                    com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationRenderer.renderFirstPerson(
                            itemInHandRenderer,
                            interactionHand,
                            player,
                            interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(),
                            handsState,
                            stack,
                            poseStack,
                            submitNodeCollector,
                            combinedLight,
                            partialTicks,
                            pitch,
                            swingProgress,
                            equippedProgress
                    );
            
            if (result == com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationRenderer.RenderResult.INTERRUPT) {
                callback.cancel();
            }
        }

        @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                shift = At.Shift.BEFORE
            )
        )
        private void baity$applyConsumableUseSwingBeforeRenderItem(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState,
                float partialTicks, float pitch, InteractionHand interactionHand, float swingProgress,
                net.minecraft.world.item.ItemStack stack, float equippedProgress,
                com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
                int combinedLight, CallbackInfo callback) {
            LocalPlayer player = minecraft.player;
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (player == null || !player.isUsingItem()) return;
            if (player.getUsedItemHand() != interactionHand) return;
            if (!BlockAnimationUtils.isUsingConsumableAnimation(player)) return;
            HumanoidArm arm = interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            this.baity$callApplyItemArmAttackTransform(poseStack, arm, swingProgress);
        }

        @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
        )
        private void baity$keepSwingWhileUsingBow(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState,
                float partialTicks, float pitch, InteractionHand interactionHand, float swingProgress,
                net.minecraft.world.item.ItemStack stack, float equippedProgress,
                com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
                int combinedLight, CallbackInfo callback) {
            LocalPlayer player = minecraft.player;
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (player == null) return;
            if (swingProgress <= 0.0f) return;
            if (!player.isUsingItem() || player.getUsedItemHand() != interactionHand) return;
            if (!(stack.is(Items.BOW) || stack.is(Items.CROSSBOW))) return;

            HumanoidArm arm = interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            this.baity$callApplyItemArmAttackTransform(poseStack, arm, swingProgress);
        }
    }

    @Mixin(FirstPersonHandsAndItems.class)
    public static abstract class FirstPersonHandsAndItemsMixin {
        @Inject(method = "itemUsed(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
        private void baity$itemUsed(InteractionHand interactionHand, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (!BlockAnimationUtils.isNoReequipWhenUsingEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.isUsingItem() 
                    && mc.player.getUsedItemHand() == interactionHand) {
                if (BlockAnimationUtils.isUsingConsumableAnimation(mc.player)) return;
                if (BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) {
                    callback.cancel();
                }
            }
        }
    }

    @Mixin(Minecraft.class)
    public static abstract class MinecraftMixin {
        @Shadow @Nullable public net.minecraft.client.player.LocalPlayer player;
        @Shadow @Final public Options options;
        @Shadow @Nullable public HitResult hitResult;
        @Shadow @Nullable public ClientLevel level;

        @org.spongepowered.asm.mixin.Unique
        private static boolean baity$lastSwinging = false;
        @org.spongepowered.asm.mixin.Unique
        private static int baity$lastSwingTime = 0;
        @org.spongepowered.asm.mixin.Unique
        private static InteractionHand baity$lastSwingArm = InteractionHand.MAIN_HAND;

        @Inject(method = "tick", at = @At("TAIL"))
        private void baity$blockAnimationCircleTick(CallbackInfo ci) {
            if (this.player == null) return;
            Minecraft self = (Minecraft) (Object) this;
            if (self.isPaused()) return;
            com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationCircleController.tick(
                    this.player, self);
        }

        @Inject(method = "tick", at = @At("TAIL"))
        private void baity$applySwingWhilstMining(CallbackInfo ci) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (this.player == null) return;
            if (!this.player.isUsingItem()) return;
            if (!BlockAnimationUtils.isUsingConsumableAnimation(this.player)) return;
            if (this.player.getItemInHand(this.player.getUsedItemHand()).isEmpty()) return;
            KeyMapping attack = this.options.keyAttack;
            if (!attack.isDown() && !attack.consumeClick()) return;
            BlockAnimationUtils.applySwingWhileUsingConsumable(this.level, this.player, this.hitResult);
        }

        @Inject(method = "tick", at = @At("TAIL"))
        private void baity$preserveSwingWhenStartingUse(CallbackInfo ci) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (this.player == null || this.options == null) return;

            boolean prevSwinging = baity$lastSwinging;
            int prevSwingTime = baity$lastSwingTime;
            InteractionHand prevSwingArm = baity$lastSwingArm;

            baity$lastSwinging = this.player.isSwinging();
            baity$lastSwingTime = baity$swingTicks(this.player);
            baity$lastSwingArm = baity$currentSwingHand(this.player);

            if (!this.player.isUsingItem()) return;
            ItemStack using = this.player.getUseItem();
            if (using == null || using.isEmpty()) return;
            if (!(using.is(Items.BOW) || using.is(Items.CROSSBOW))) return;

            if (prevSwinging && !this.player.isSwinging()) {
                baity$restartSwing(this.player, prevSwingArm, prevSwingTime);
            }
        }

        private static int baity$swingTicks(LocalPlayer player) {
            return ((com.shyeuar.baity.mixin.accessor.SwingStateAccessor)
                    ((com.shyeuar.baity.mixin.accessor.LivingEntityAccessor) (Object) player).baity$getSwingState())
                    .baity$getTicks();
        }

        private static InteractionHand baity$currentSwingHand(LocalPlayer player) {
            net.minecraft.world.entity.LivingEntity.SwingDescription swing = player.getCurrentSwing();
            return swing != null ? swing.hand() : InteractionHand.MAIN_HAND;
        }

        private static void baity$restartSwing(LocalPlayer player, InteractionHand hand, int ticks) {
            net.minecraft.world.entity.LivingEntity.SwingState swingState =
                    ((com.shyeuar.baity.mixin.accessor.LivingEntityAccessor) (Object) player).baity$getSwingState();
            swingState.startIfAble(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, 6);
            ((com.shyeuar.baity.mixin.accessor.SwingStateAccessor) swingState).baity$setTicks(ticks);
        }
    }


    @Mixin(HumanoidModel.class)
    public static abstract class HumanoidModelMixin<T extends HumanoidRenderState> extends net.minecraft.client.model.EntityModel<T> {
        @Shadow
        public ModelPart rightArm;
        @Shadow
        public ModelPart leftArm;

        protected HumanoidModelMixin(ModelPart root) {
            super(root);
        }

        @Inject(method = "setupAnim", at = @At("HEAD"))
        private void baity$zeroAttackAnimCircleBlock(T renderState, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive() || !BlockAnimationUtils.isSpinAnimaMode()) return;
            if (!(renderState instanceof AvatarRenderState avatarState)) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (avatarState.id != mc.player.getId()) return;
            if (BlockAnimationUtils.isUsingConsumableAnimation(mc.player)) return;
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;

            avatarState.swingAnimation = 0f;
        }

        @Inject(method = "setupAnim", at = @At("TAIL"))
        private void baity$setupAnim(T renderState, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (!(renderState instanceof AvatarRenderState)) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            AvatarRenderState avatarState = (AvatarRenderState) renderState;
            if (avatarState.id != mc.player.getId()) return;

            if (BlockAnimationUtils.isUsingConsumableAnimation(mc.player)) return;
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;

            if (BlockAnimationUtils.getBlockingHand(mc.player) == null) return;

            HumanoidArm swordArm = BlockAnimationUtils.getBlockingArm(mc.player, avatarState.mainArm);
            ModelPart arm = swordArm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;

            float raiseFactor = 1f;
            float rotorWobble = 0f;
            if (BlockAnimationUtils.isRotorAnimaMode()) {
                raiseFactor = com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationCircleController
                        .getRotorAimNow();
                rotorWobble = com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationCircleController
                        .getRotorThirdPersonWobbleArmRadians();
            }

            arm.xRot = arm.xRot - (Mth.PI * 2.0F / 10.0F) * raiseFactor + rotorWobble;
        }
    }
    
    @Mixin(net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer.class)
    public static abstract class PlayerItemInHandLayerMixin<S extends AvatarRenderState, M extends EntityModel<S> & ArmedModel<S>> extends ItemInHandLayer<S, M> {
        
        public PlayerItemInHandLayerMixin(net.minecraft.client.renderer.entity.RenderLayerParent<S, M> renderLayerParent) {
            super(renderLayerParent);
        }
        
        @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
        private void baity$submitArmWithItem(S renderState,
                net.minecraft.client.renderer.item.ItemStackRenderState itemStackRenderState,
                net.minecraft.world.item.ItemStack itemStack,
                HumanoidArm humanoidArm,
                com.mojang.blaze3d.vertex.PoseStack poseStack,
                net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
                int packedLight,
                CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (itemStackRenderState.isEmpty()) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            if (renderState.id != mc.player.getId()) return;

            if (BlockAnimationUtils.isUsingConsumableAnimation(mc.player)) return;

            boolean blockingNow = BlockAnimationUtils.isPlayerBlockingWithSword(mc.player);
            boolean keepCircleSpin = BlockAnimationUtils.isSpinAnimaMode()
                    && com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationCircleController.hasActiveSpin();
            if (!blockingNow && !keepCircleSpin) return;

            InteractionHand interactionHand =
                    humanoidArm == renderState.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(mc.player);
            if (blockingHand != interactionHand) return;
            
            
            @SuppressWarnings("unchecked")
            ArmedModel<AvatarRenderState> model = 
                    (ArmedModel<AvatarRenderState>) this.getParentModel();
            
            com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationRenderer.renderThirdPerson(
                    (AvatarRenderState) renderState,
                    model,
                    itemStackRenderState,
                    humanoidArm,
                    poseStack,
                    submitNodeCollector,
                    packedLight,
                    itemStack,
                    mc.player,
                    blockingNow);
            
            callback.cancel();
        }
    }

    @Mixin(net.minecraft.world.entity.LivingEntity.class)
    public static class LivingEntitySwingMixin {
        @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)V", at = @At("HEAD"))
        private void baity$queueCircleSpinWhileBlocking(InteractionHand hand, net.minecraft.world.item.component.SwingAnimation animation, boolean fromServer, CallbackInfo ci) {
            net.minecraft.world.entity.LivingEntity self = (net.minecraft.world.entity.LivingEntity) (Object) this;
            if (self.level() == null || !self.level().isClientSide()) return;
            if (!(self instanceof LocalPlayer player)) return;
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (!BlockAnimationUtils.isSpinAnimaMode()) return;
            if (fromServer) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.isPaused()) return;
            if (BlockAnimationUtils.isUsingConsumableAnimation(player)) return;
            InteractionHand swordHand = BlockAnimationUtils.getBlockingHand(player);
            if (swordHand == null || hand != swordHand) {
                return;
            }
            if (BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationCircleController.queueSpin();
            } else {
                com.shyeuar.baity.features.legacytweaks.blockanimation.BlockAnimationCircleController.queueSwingGraceSpin();
            }
        }
    }
    
}
