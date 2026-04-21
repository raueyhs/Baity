package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import com.shyeuar.baity.utils.NickRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(NameTagFeatureRenderer.Storage.class)
public class NametagOffsetMixin {
    private static boolean baity$shouldApplyDefaultNametagCompat() {
        Module nametag = ModuleManager.getModuleByName("Nametag");
        if (nametag == null || !nametag.isEnabled()) {
            return false;
        }
        return ModuleUtils.getOptionBoolean(nametag, "default nametag", false);
    }

    @ModifyVariable(
        method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component baity$useProcessedTextForVanillaNameTagLayout(Component originalComponent) {
        return applyProcessedComponentIfNeeded(originalComponent);
    }

    @ModifyVariable(
        method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("STORE"),
        ordinal = 0
    )
    private float baity$recalculateCenteredOffsetFromVisualText(
        float originalOffset,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.world.phys.Vec3 vec3,
        int lineOffset,
        Component component,
        boolean seeThrough,
        int light,
        double distanceSq,
        net.minecraft.client.renderer.state.CameraRenderState cameraState
    ) {
        if (!baity$shouldApplyDefaultNametagCompat()) {
            return originalOffset;
        }
        if (component == null) {
            return originalOffset;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) {
            return originalOffset;
        }
        Component target = component;
        FormattedText processed = NickRenderUtils.handleFormattedText(component);
        if (processed instanceof Component processedComponent) {
            target = processedComponent;
        }
        return -mc.font.width(target.getVisualOrderText()) / 2.0F;
    }

    static Component applyProcessedComponentIfNeeded(Component originalComponent) {
        if (!baity$shouldApplyDefaultNametagCompat()) {
            return originalComponent;
        }

        FormattedText processed = NickRenderUtils.handleFormattedText(originalComponent);
        if (processed instanceof Component processedComponent) {
            return processedComponent;
        }
        return originalComponent;
    }

    @Mixin(AvatarRenderer.class)
    public static class LayoutCompatMixin {
        @ModifyVariable(
            method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("STORE"),
            ordinal = 0
        )
        private Component baity$applyNickTweaksBeforeLayout(Component originalComponent) {
            return NametagOffsetMixin.applyProcessedComponentIfNeeded(originalComponent);
        }
    }
}