package com.shyeuar.baity.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class CrosshairMixin {
    
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void baity$forceCrosshairInThirdPersonRear(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.options.getPerspective() == Perspective.THIRD_PERSON_BACK) {
            // 在第三人称后视时强制显示准心，不取消渲染
            // 确保准心始终显示，无论其他条件如何
            return;
        }
        // 正面不受影响
        if (client.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            ci.cancel();
        }
    }
}
