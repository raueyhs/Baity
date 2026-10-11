package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class RemoveRecipeBookMixin {

    @Mixin(value = AbstractRecipeBookScreen.class, priority = 999)
    public static class AbstractRecipeBookScreenMixin {

        @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
        private void baity$skipRecipeBookButton(CallbackInfo ci) {
            if (ModuleUtils.shouldRemoveRecipeBook()) {
                ci.cancel();
            }
        }
    }

    @Mixin(RecipeBookComponent.class)
    public static class RecipeBookComponentMixin {

        @WrapOperation(
            method = "init",
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;visible:Z")
        )
        private void baity$neverVisibleOnScreenInit(RecipeBookComponent<?> component, boolean value, Operation<Void> original) {
            if (ModuleUtils.shouldRemoveRecipeBook()) {
                return;
            }
            original.call(component, value);
        }

        @Inject(method = "setVisible", at = @At("HEAD"), cancellable = true)
        private void baity$keepRecipeBookHidden(boolean visible, CallbackInfo ci) {
            if (ModuleUtils.shouldRemoveRecipeBook()) {
                ci.cancel();
            }
        }

        @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true)
        private void baity$blockRecipeBookToggle(CallbackInfo ci) {
            if (ModuleUtils.shouldRemoveRecipeBook()) {
                ci.cancel();
            }
        }
    }
}
