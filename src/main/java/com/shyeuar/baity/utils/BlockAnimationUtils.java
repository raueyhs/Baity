package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@Environment(EnvType.CLIENT)
public final class BlockAnimationUtils {
    private BlockAnimationUtils() {}
    
    public static final int DEFAULT_ITEM_USE_DURATION = 72_000;

    public static boolean isFeatureActive() {
        com.shyeuar.baity.gui.module.Module blockAnimationModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("BlockAnimation");
        return blockAnimationModule != null && blockAnimationModule.isEnabled();
    }

    public static boolean isInteractAnimationsEnabled() {
        return com.shyeuar.baity.config.ConfigManager.blockAnimationInteractAnimations;
    }

    public static boolean isNoReequipWhenUsingEnabled() {
        return com.shyeuar.baity.config.ConfigManager.blockAnimationNoReequipWhenUsing;
    }

    public static boolean isCircleAnimaMode() {
        return "circle".equalsIgnoreCase(com.shyeuar.baity.config.ConfigManager.blockAnimationAnimaMode);
    }

    public static boolean isRotorAnimaMode() {
        return "rotor".equalsIgnoreCase(com.shyeuar.baity.config.ConfigManager.blockAnimationAnimaMode);
    }

    public static boolean isSpinAnimaMode() {
        return isCircleAnimaMode() || isRotorAnimaMode();
    }

    public static boolean isPlayerBlockingWithSword(Player player) {
        if (!isFeatureActive()) return false;
        if (player == null) return false;
        return isPlayerRightClicking() && canSwordBlock(player);
    }

    public static boolean isUsingConsumableAnimation(Player player) {
        if (player == null || !player.isUsingItem()) return false;
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) return false;
        ItemUseAnimation anim = stack.getUseAnimation();
        return anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK;
    }

    public static boolean isPlayerRightClicking() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return false;
        return client.options.keyUse.isDown();
    }
  
    public static boolean canSwordBlock(Player player) {
        if (!isFeatureActive()) return false;
        if (player == null) return false;
        Item mainHandItem = player.getMainHandItem().getItem();
        Item offHandItem = player.getOffhandItem().getItem();
        return isSword(mainHandItem) || isSword(offHandItem);
    }
    
    public static InteractionHand getBlockingHand(Player player) {
        if (!canSwordBlock(player)) return null;
        return isSword(player.getMainHandItem().getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD ||
               item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD ||
               item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD ||
               item == Items.NETHERITE_SWORD ||
               item == Items.COPPER_SWORD;
    }

    public static boolean canActivateBlocking(Player player, ItemStack offHand) {
        if (offHand.isEmpty()) return true;
        
        switch (offHand.getUseAnimation()) {
            case BLOCK, SPYGLASS, BRUSH -> {
                return false;
            }
            default -> {
                return true;
            }
        }
    }

    public static boolean isNotSwinging(Player player) {
        if (player == null) return false;
        int swingDuration = ((com.shyeuar.baity.mixin.accessor.LivingEntityAccessor) (Object) player).baity$getCurrentSwingDuration();
        return !player.swinging || player.swingTime >= swingDuration / 2 || player.swingTime < 0;
    }

    public static void fakeHandSwing(Player player, InteractionHand hand) {
        if (player == null || hand == null) return;
        if (isNotSwinging(player)) {
            player.swingTime = -1;
            player.swinging = true;
            player.swingingArm = hand;
        }
    }

    public static void applySwingWhileUsingConsumable(ClientLevel level, Player player, HitResult hitResult) {
        if (player == null) return;
        if (!player.isUsingItem()) return;
        if (!isUsingConsumableAnimation(player)) return;
        if (player.getItemInHand(player.getUsedItemHand()).isEmpty()) return;

        fakeHandSwing(player, InteractionHand.MAIN_HAND);

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK && level != null) {
            BlockHitResult bhr = (BlockHitResult) hitResult;
            BlockPos pos = bhr.getBlockPos();
            if (!level.getBlockState(pos).isAir()) {
                level.addBreakingBlockEffect(pos, bhr.getDirection());
            }
        }
    }
}

