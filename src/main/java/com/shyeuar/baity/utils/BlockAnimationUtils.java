package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public final class BlockAnimationUtils {
    private BlockAnimationUtils() {}

    public static boolean isEntityBlocking(LivingEntity entity) {
        if (!ConfigManager.blockAnimationMode || entity == null) return false;
        if (!entity.getWorld().isClient) return false;
        return isPlayerRightClicking() && canSwordBlock(entity);
    }

    public static boolean isPlayerRightClicking() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return false;
        return client.options.useKey.isPressed();
    }

    public static boolean canSwordBlock(LivingEntity entity) {
        if (!ConfigManager.blockAnimationMode) return false;
        Item mainHandItem = entity.getMainHandStack().getItem();
        Item offHandItem = entity.getOffHandStack().getItem();
        return isSword(mainHandItem) || isSword(offHandItem);
    }

    public static boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD ||
               item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD ||
               item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD ||
               item == Items.NETHERITE_SWORD;
    }

    public static Hand getBlockingHand(LivingEntity entity) {
        if (!canSwordBlock(entity)) return null;
        return isSword(entity.getMainHandStack().getItem()) ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }
}

