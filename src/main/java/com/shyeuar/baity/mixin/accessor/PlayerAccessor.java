package com.shyeuar.baity.mixin.accessor;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Player.class)
public interface PlayerAccessor {
    @Invoker("canPlayerFitWithinBlocksAndEntitiesWhen")
    boolean baity$canChangeIntoPose(Pose pose);
}
