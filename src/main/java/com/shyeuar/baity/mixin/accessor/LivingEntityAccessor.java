package com.shyeuar.baity.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("swimAmount")
    float baity$getSwimAmountField();

    @Accessor("swimAmount")
    void baity$setSwimAmountField(float swimAmount);

    @Accessor("swimAmountO")
    float baity$getSwimAmountOField();

    @Accessor("swimAmountO")
    void baity$setSwimAmountOField(float swimAmountO);

    @Accessor("swingState")
    LivingEntity.SwingState baity$getSwingState();
}

