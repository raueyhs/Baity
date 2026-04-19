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
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.player.LocalPlayer;
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

    @Mixin(value = ItemInHandRenderer.class, priority = 500)
    public static abstract class ItemInHandRendererMixin {
        @Shadow
        @Final
        private Minecraft minecraft;

        @Invoker("applyItemArmAttackTransform")
        public abstract void baity$callApplyItemArmAttackTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, HumanoidArm hand, float swingProgress);

        @Invoker("applyItemArmTransform")
        public abstract void baity$callApplyItemArmTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, HumanoidArm hand, float equippedProgress);

        @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
        private void baity$itemUsed(InteractionHand interactionHand, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (!BlockAnimationUtils.isNoReequipWhenUsingEnabled()) return;

            if (this.minecraft.player != null && this.minecraft.player.isUsingItem() 
                    && this.minecraft.player.getUsedItemHand() == interactionHand) {
                if (BlockAnimationUtils.isUsingConsumableAnimation(this.minecraft.player)) return;
                if (BlockAnimationUtils.isPlayerBlockingWithSword(this.minecraft.player)) {
                    callback.cancel();
                }
            }
        }

        @WrapOperation(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z")
        )
        private boolean baity$wrapIsUsingItem(net.minecraft.client.player.AbstractClientPlayer player, Operation<Boolean> original) {
            if (player != null && BlockAnimationUtils.isUsingConsumableAnimation(player)) {
                return original.call(player);
            }
            if (player != null && BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                return true;
            }
            return original.call(player);
        }

        @WrapOperation(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I")
        )
        private int baity$wrapGetItemUseTimeLeft(net.minecraft.client.player.AbstractClientPlayer player, Operation<Integer> original) {
            if (player != null && BlockAnimationUtils.isUsingConsumableAnimation(player)) {
                return original.call(player);
            }
            if (player != null && BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                return BlockAnimationUtils.DEFAULT_ITEM_USE_DURATION;
            }
            return original.call(player);
        }

        @WrapOperation(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;")
        )
        private InteractionHand baity$wrapGetActiveHand(net.minecraft.client.player.AbstractClientPlayer player, Operation<InteractionHand> original) {
            if (player != null && BlockAnimationUtils.isUsingConsumableAnimation(player)) {
                return original.call(player);
            }
            if (player != null && BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(player);
                if (blockingHand != null) {
                    return blockingHand;
                }
            }
            return original.call(player);
        }

        @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
        private void baity$renderArmWithItem(net.minecraft.client.player.AbstractClientPlayer player, float partialTicks, float pitch, 
                InteractionHand interactionHand, float swingProgress, net.minecraft.world.item.ItemStack stack, float equippedProgress, 
                com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, 
                int combinedLight, CallbackInfo callback) {
            
            ItemInHandRenderer itemInHandRenderer = (ItemInHandRenderer) (Object) this;
            com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.RenderResult result = 
                    com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.renderFirstPerson(
                            itemInHandRenderer,
                            interactionHand,
                            player,
                            interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(),
                            stack,
                            poseStack,
                            submitNodeCollector,
                            combinedLight,
                            partialTicks,
                            pitch,
                            swingProgress,
                            equippedProgress
                    );
            
            if (result == com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.RenderResult.INTERRUPT) {
                callback.cancel();
            }
        }

        @Inject(
            method = "renderArmWithItem",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                shift = At.Shift.BEFORE
            )
        )
        private void baity$applyConsumableUseSwingBeforeRenderItem(net.minecraft.client.player.AbstractClientPlayer player, float partialTicks, float pitch,
                InteractionHand interactionHand, float swingProgress, net.minecraft.world.item.ItemStack stack,
                float equippedProgress, com.mojang.blaze3d.vertex.PoseStack poseStack,
                net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int combinedLight,
                CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (player == null || !player.isUsingItem()) return;
            if (player.getUsedItemHand() != interactionHand) return;
            if (!BlockAnimationUtils.isUsingConsumableAnimation(player)) return;
            HumanoidArm arm = interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            this.baity$callApplyItemArmAttackTransform(poseStack, arm, swingProgress);
        }

        @Inject(
            method = "renderArmWithItem",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
            )
        )
        private void baity$keepSwingWhileUsingBow(net.minecraft.client.player.AbstractClientPlayer player, float partialTicks, float pitch,
                                                   InteractionHand interactionHand, float swingProgress, net.minecraft.world.item.ItemStack stack,
                                                   float equippedProgress, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int combinedLight,
                                                   CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (player == null) return;
            if (swingProgress <= 0.0f) return;
            if (!player.isUsingItem() || player.getUsedItemHand() != interactionHand) return;
            if (!(stack.is(Items.BOW) || stack.is(Items.CROSSBOW))) return;

            HumanoidArm arm = interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            this.baity$callApplyItemArmAttackTransform(poseStack, arm, swingProgress);
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
            com.shyeuar.baity.features.blockanimation.BlockAnimationCircleController.tick(
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

            baity$lastSwinging = this.player.swinging;
            baity$lastSwingTime = this.player.swingTime;
            baity$lastSwingArm = this.player.swingingArm;

            if (!this.player.isUsingItem()) return;
            ItemStack using = this.player.getUseItem();
            if (using == null || using.isEmpty()) return;
            if (!(using.is(Items.BOW) || using.is(Items.CROSSBOW))) return;

            if (prevSwinging && !this.player.swinging) {
                this.player.swinging = true;
                this.player.swingTime = prevSwingTime;
                this.player.swingingArm = prevSwingArm;
            }
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

        /** 在 resetPose 与手臂姿态逻辑之后、挥击动画写入模型之前清零 attackTime，避免在 super.setupAnim(重置骨骼) 之前改状态导致异常 */
        @Inject(
            method = "setupAnim",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;F)V",
                shift = At.Shift.BEFORE
            )
        )
        private void baity$zeroAttackAnimCircleBlock(T renderState, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive() || !BlockAnimationUtils.isSpinAnimaMode()) return;
            if (!(renderState instanceof AvatarRenderState avatarState)) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (avatarState.id != mc.player.getId()) return;
            if (BlockAnimationUtils.isUsingConsumableAnimation(mc.player)) return;
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;

            avatarState.attackTime = 0f;
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

            InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(mc.player);
            if (blockingHand == null) return;
            
            InteractionHand interactionHand =
                    renderState.mainArm == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            float raiseFactor = 1f;
            float rotorWobble = 0f;
            if (BlockAnimationUtils.isRotorAnimaMode()) {
                raiseFactor = com.shyeuar.baity.features.blockanimation.BlockAnimationCircleController
                        .getRotorAimNow();
                rotorWobble = com.shyeuar.baity.features.blockanimation.BlockAnimationCircleController
                        .getRotorThirdPersonWobbleArmRadians();
            }

            if (blockingHand == interactionHand) {
                this.rightArm.xRot = this.rightArm.xRot - (Mth.PI * 2.0F / 10.0F) * raiseFactor + rotorWobble;
            } else {
                this.leftArm.xRot = this.leftArm.xRot - (Mth.PI * 2.0F / 10.0F) * raiseFactor + rotorWobble;
            }
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
                    && com.shyeuar.baity.features.blockanimation.BlockAnimationCircleController.hasActiveSpin();
            if (!blockingNow && !keepCircleSpin) return;

            InteractionHand interactionHand =
                    humanoidArm == renderState.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(mc.player);
            if (blockingHand != interactionHand) return;
            
            
            @SuppressWarnings("unchecked")
            ArmedModel<AvatarRenderState> model = 
                    (ArmedModel<AvatarRenderState>) this.getParentModel();
            
            com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.renderThirdPerson(
                    (AvatarRenderState) renderState,
                    model,
                    itemStackRenderState,
                    humanoidArm,
                    poseStack,
                    submitNodeCollector,
                    packedLight,
                    mc.player,
                    blockingNow);
            
            callback.cancel();
        }
    }

    @Mixin(net.minecraft.world.entity.LivingEntity.class)
    public static class LivingEntitySwingMixin {
        @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
        private void baity$queueCircleSpinWhileBlocking(InteractionHand hand, boolean fromServer, CallbackInfo ci) {
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
                com.shyeuar.baity.features.blockanimation.BlockAnimationCircleController.queueSpin();
            } else {
                com.shyeuar.baity.features.blockanimation.BlockAnimationCircleController.queueSwingGraceSpin();
            }
        }
    }
    
}