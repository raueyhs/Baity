package com.shyeuar.baity.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("applyItemArmAttackTransform")
    void baity$callApplyItemArmAttackTransform(PoseStack poseStack, HumanoidArm hand, float swingProgress);
    
    @Invoker("applyItemArmTransform")
    void baity$callApplyItemArmTransform(PoseStack poseStack, HumanoidArm hand, float equippedProgress);
}
