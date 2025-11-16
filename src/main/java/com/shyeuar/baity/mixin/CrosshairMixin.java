package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class CrosshairMixin {
    @ModifyExpressionValue(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"))
    private Perspective baity$forceCrosshairInThirdPersonRear(Perspective original) {
        com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
        boolean crosshairMode = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(smolPeopleModule, "crosshair", false);
        
        if (ConfigManager.smolpeopleMode && crosshairMode && original == Perspective.THIRD_PERSON_BACK) {
            return Perspective.FIRST_PERSON;
        }
        return original;
    }
}

