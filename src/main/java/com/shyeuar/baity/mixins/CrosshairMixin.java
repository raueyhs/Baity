package com.shyeuar.baity.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 第三人称背面渲染准心
@Mixin(InGameHud.class)
public class CrosshairMixin {
    @ModifyExpressionValue(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"))
    private Perspective baity$forceCrosshairInThirdPersonRear(Perspective original) {
        if (original == Perspective.THIRD_PERSON_BACK) {
            return Perspective.FIRST_PERSON;
        }
        return original;
    }
}
