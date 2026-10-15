package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.LocateUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class LocateMixin {

    @Mixin(ClientPacketListener.class)
    public static class SystemChatMixin {

        @Inject(method = "handleSystemChat", at = @At("HEAD"))
        private void baity$locateOnSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
            LocateUtils.handleDungeonChat(packet.content().getString());
        }
    }
}
