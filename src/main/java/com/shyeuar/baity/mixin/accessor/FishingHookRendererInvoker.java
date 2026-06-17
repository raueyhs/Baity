package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FishingHookRenderer.class)
public interface FishingHookRendererInvoker {
    @Invoker("getPlayerHandPos")
    Vec3 baity$invokeGetPlayerHandPos(Player player, float attackAnim, float partialTick);
}
