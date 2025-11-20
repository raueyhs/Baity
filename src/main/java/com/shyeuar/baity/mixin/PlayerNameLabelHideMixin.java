package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerNameLabelHideMixin {

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void baity$hideOriginalNameTag(PlayerEntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider providers, int i, CallbackInfo ci) {
        Module m = ModuleManager.getModuleByName("PlayerESP");
        if (m == null || !m.isEnabled()) {
            return; 
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        
        String playerName = state.name;
        if (playerName == null) return;
        
        PlayerEntity player = null;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.getName().getString().equals(playerName)) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        if (com.shyeuar.baity.utils.AntiBotUtils.isBot(player)) {
            ci.cancel();
            return;
        }

        boolean showOwnNametag = ModuleUtils.getOptionBoolean(m, "show own nametag", false);
        if (player == mc.player) {
            if (showOwnNametag) {
                ci.cancel();
                return;
            }
            // 如果未启用show own nametag，让原版标签正常渲染（原版行为）
            return;
        }

        ci.cancel();
    }

}


