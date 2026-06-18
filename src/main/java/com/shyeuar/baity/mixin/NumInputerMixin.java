package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.numinputer.NumInputer;
import com.shyeuar.baity.mixin.accessor.AbstractSignEditScreenAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class NumInputerMixin implements NumInputer.NumInputerSignScreenAccess {
    @Shadow
    @Nullable
    private TextFieldHelper signField;

    @Inject(method = "init", at = @At("TAIL"))
    private void baity$initNumInputer(CallbackInfo ci) {
        NumInputer.onScreenInit((AbstractSignEditScreen) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void baity$renderNumInputer(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NumInputer.render((AbstractSignEditScreen) (Object) this, guiGraphics, mouseX, mouseY);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void baity$tickNumInputer(CallbackInfo ci) {
        NumInputer.tickMouse((AbstractSignEditScreen) (Object) this);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void baity$clearNumInputer(CallbackInfo ci) {
        NumInputer.onScreenClosed();
    }

    @Override
    public void baity$insertIntoSign(char character) {
        if (this.signField == null) {
            return;
        }
        this.signField.insertText(String.valueOf(character));
    }

    @Override
    public void baity$finishSignEditing() {
        ((AbstractSignEditScreenAccessor) this).baity$onDone();
    }
}
