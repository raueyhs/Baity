package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.features.enchantlore.EnchantLore;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class EnchantLoreMixin {

    @WrapOperation(
            method = "componentHoverEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"
            )
    )
    private List<FormattedCharSequence> baity$enchantLoreHoverLines(
            Font font,
            FormattedText text,
            int maxWidth,
            Operation<List<FormattedCharSequence>> original
    ) {
        return original.call(font, EnchantLore.rewriteHoverText(text), maxWidth);
    }
}
