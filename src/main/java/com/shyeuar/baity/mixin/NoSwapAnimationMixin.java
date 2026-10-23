package com.shyeuar.baity.mixin;

import com.shyeuar.baity.mixin.accessor.FirstPersonHandsAndItemsAccessor;
import com.shyeuar.baity.utils.HeldItemTweaksUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public class NoSwapAnimationMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void baity$removeSwapAnimation(LocalPlayer player, CallbackInfo ci) {
        if (!HeldItemTweaksUtils.isNoItemswapAnimationActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        FirstPersonHandsAndItemsAccessor accessor = (FirstPersonHandsAndItemsAccessor) (Object) this;

        accessor.baity$setMainHandItem(mc.player.getMainHandItem());
        accessor.baity$setOffHandItem(mc.player.getOffhandItem());
        accessor.baity$setMainHandHeight(1.0f);
        accessor.baity$setOffHandHeight(1.0f);
    }
}
