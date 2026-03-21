package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.chat.ChatChannelSwitcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatChannelSwitcherMixin extends Screen {
    @Shadow private EditBox input;

    protected ChatChannelSwitcherMixin(Component title) {
        super(title);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            shift = At.Shift.BEFORE
        )
    )
    private void baity$renderChatChannelButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChatChannelSwitcher.render(this, this.input, guiGraphics, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void baity$handleChatChannelButtons(MouseButtonEvent click, boolean isInsideWindow, CallbackInfoReturnable<Boolean> cir) {
        if (ChatChannelSwitcher.mouseClicked(this, this.input, click)) {
            cir.setReturnValue(true);
        }
    }
}
