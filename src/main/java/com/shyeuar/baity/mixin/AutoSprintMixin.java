package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class AutoSprintMixin {

    @ModifyExpressionValue(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"),
        require = 1
    )
    private boolean baity$autoSprint(boolean original) {
        if (!ConfigManager.autoSprintEnabled) {
            return original;
        }
        if (!ConfigManager.autoSprintUnderWater && ((LocalPlayer) (Object) this).isInWater()) {
            return original;
        }
        return true;
    }
}
