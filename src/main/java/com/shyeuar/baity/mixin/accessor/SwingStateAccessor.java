package com.shyeuar.baity.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.entity.LivingEntity$SwingState")
public interface SwingStateAccessor {

    @Accessor("ticks")
    int baity$getTicks();

    @Accessor("ticks")
    void baity$setTicks(int ticks);

    @Accessor("currentSwing")
    LivingEntity.SwingDescription baity$getCurrentSwing();

    @Accessor("currentSwing")
    void baity$setCurrentSwing(LivingEntity.SwingDescription swing);
}
