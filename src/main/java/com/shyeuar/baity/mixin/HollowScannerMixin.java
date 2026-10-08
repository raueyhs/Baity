package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.hollowscanner.HollowScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class HollowScannerMixin {

    @Mixin(Screen.class)
    public static class ShareClickMixin {

        @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
        private static void baity$sendSharedCoordinates(ClickEvent event, Minecraft minecraft, Screen screen,
                                                        CallbackInfo ci) {
            if (!(event instanceof ClickEvent.RunCommand run)) {
                return;
            }
            String command = run.command();
            if (minecraft.player == null || !command.startsWith(HollowScanner.SHARE_MARKER)) {
                return;
            }
            com.shyeuar.baity.utils.MessageUtils.sendUserText(command.substring(HollowScanner.SHARE_MARKER.length()));
            ci.cancel();
        }
    }
}
