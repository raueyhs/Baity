package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.NickRenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Font.class)
public class NickTweaksMixin {
    @ModifyVariable(
            method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private String baity$chromaPrepareString(String text) {
        return NickRenderUtils.handleString(text);
    }

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private FormattedCharSequence baity$chromaPrepareSequence(FormattedCharSequence text) {
        return NickRenderUtils.handleCharSequence(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String baity$chromaWidthString(String text) {
        return NickRenderUtils.handleString(text);
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void baity$chromaWidthFormattedText(FormattedText text, CallbackInfoReturnable<Integer> cir) {
        FormattedCharSequence visualOrder = Language.getInstance().getVisualOrder(text);
        cir.setReturnValue(((Font) (Object) this).width(visualOrder));
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence baity$chromaWidthSequence(FormattedCharSequence text) {
        return NickRenderUtils.handleCharSequence(text);
    }
}
