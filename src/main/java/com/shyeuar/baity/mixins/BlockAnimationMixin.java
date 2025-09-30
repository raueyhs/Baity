package com.shyeuar.baity.mixins;

import com.shyeuar.baity.blockanimation.BlockAnimationUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

public abstract class BlockAnimationMixin {

    // 渲染第一人称动作
    @Mixin(HeldItemRenderer.class)
    public static abstract class MixinHeldItemRenderer {

        @Redirect(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;"))
        private UseAction blockAnimation$changeItemAction(ItemStack stack, @Local(argsOnly = true) AbstractClientPlayerEntity player, @Local(argsOnly = true) Hand hand) {
            UseAction defaultUseAction = stack.getUseAction();
            if (player != null && BlockAnimationUtils.isEntityBlocking(player)) {
                if (BlockAnimationUtils.isSword(stack.getItem())) {
                    Hand blockingHand = BlockAnimationUtils.getBlockingHand(player);
                    if (blockingHand != null && blockingHand == hand) {
                        return UseAction.BLOCK;
                    }
                }
            }
            return defaultUseAction;
        }

        // 修改使用进度
        @Redirect(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getItemUseTimeLeft()I"))
        private int blockAnimation$changeUseTimeLeft(AbstractClientPlayerEntity player, @Local(argsOnly = true) Hand hand, @Local(argsOnly = true) ItemStack stack) {
            if (player == null) {
                return 0;
            }
            if (BlockAnimationUtils.isEntityBlocking(player)) {
                if (BlockAnimationUtils.isSword(stack.getItem())) {
                    return 20;
                }
            }
            return player.getItemUseTimeLeft();
        }

        @Redirect(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingItem()Z"))
        private boolean blockAnimation$forceIsUsingItem(AbstractClientPlayerEntity player, @Local(argsOnly = true) Hand hand, @Local(argsOnly = true) ItemStack stack) {
            if (player == null) {
                return false;
            }
            if (BlockAnimationUtils.isEntityBlocking(player) && BlockAnimationUtils.isSword(stack.getItem())) {
                // 仅当当前渲染的这只手是我们计算的格挡手时，才把 isUsingItem 视为 true
                return BlockAnimationUtils.getBlockingHand(player) == hand || player.isUsingItem();
            }
            return player.isUsingItem();
        }

        @Redirect(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getActiveHand()Lnet/minecraft/util/Hand;"))
        private Hand blockAnimation$forceActiveHand(AbstractClientPlayerEntity player) {
            if (player == null) {
                return Hand.MAIN_HAND;
            }
            if (BlockAnimationUtils.isEntityBlocking(player)) {
                Hand blockingHand = BlockAnimationUtils.getBlockingHand(player);
                if (blockingHand != null) return blockingHand;
            }
            return player.getActiveHand();
        }
    }

}