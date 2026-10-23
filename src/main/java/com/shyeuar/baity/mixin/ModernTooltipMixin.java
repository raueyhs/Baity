package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.moderntooltip.ScrollableTooltip;
import com.shyeuar.baity.features.moderntooltip.TooltipAnimation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class ModernTooltipMixin {

    private static final String TOOLTIP = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Z)V";
    private static final String EXTRACT_BACKGROUND = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V";
    private static final String POP_MATRIX = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;";

    @Inject(method = "extractDeferredElements", at = @At("HEAD"))
    private void baity$modernTooltipPassBegin(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TooltipAnimation.onRenderPassBegin();
    }

    @Inject(method = "extractDeferredElements", at = @At("RETURN"))
    private void baity$modernTooltipPassEnd(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TooltipAnimation.onRenderPassEnd();
    }

    @Inject(method = TOOLTIP, at = @At("HEAD"), cancellable = true)
    private void baity$scrollableTooltip(
            Font font,
            List<ClientTooltipComponent> components,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            Identifier texture,
            boolean animate,
            CallbackInfo ci
    ) {
        if (!ScrollableTooltip.isActive()) {
            return;
        }
        ci.cancel();
        ScrollableTooltip.render(
                (GuiGraphicsExtractor) (Object) this,
                font,
                components,
                x,
                y,
                positioner,
                texture
        );
    }

    @Inject(method = TOOLTIP, at = @At("HEAD"))
    private void baity$modernTooltipBegin(
            Font font,
            List<ClientTooltipComponent> components,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            Identifier texture,
            boolean animate,
            CallbackInfo ci
    ) {
        if (ScrollableTooltip.isActive()) {
            return;
        }
        TooltipAnimation.beginTooltipPass(font, components);
    }

    @WrapOperation(
            method = TOOLTIP,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"
            )
    )
    private Vector2ic baity$modernTooltipPosition(
            ClientTooltipPositioner positioner,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY,
            int tooltipWidth,
            int tooltipHeight,
            Operation<Vector2ic> original
    ) {
        Vector2ic raw = original.call(positioner, screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);
        return TooltipAnimation.smoothPosition(raw, tooltipWidth, tooltipHeight);
    }

    @ModifyArgs(method = TOOLTIP, at = @At(value = "INVOKE", target = EXTRACT_BACKGROUND))
    private void baity$modernTooltipBackgroundArgs(Args args) {
        int width = args.get(3);
        int height = args.get(4);
        args.set(3, TooltipAnimation.getAnimatedContentWidth(width));
        args.set(4, TooltipAnimation.getAnimatedContentHeight(height));
    }

    @Inject(method = TOOLTIP, at = @At(value = "INVOKE", target = EXTRACT_BACKGROUND, shift = At.Shift.AFTER))
    private void baity$modernTooltipPushTextClipAfterBackground(CallbackInfo ci) {
        TooltipAnimation.pushTextClip((GuiGraphicsExtractor) (Object) this);
    }

    @Inject(method = TOOLTIP, at = @At(value = "INVOKE", target = POP_MATRIX, shift = At.Shift.BEFORE))
    private void baity$modernTooltipPopTextClipBeforeMatrixPop(CallbackInfo ci) {
        TooltipAnimation.popTextClip((GuiGraphicsExtractor) (Object) this);
    }

    @Inject(method = TOOLTIP, at = @At("RETURN"))
    private void baity$modernTooltipEnd(
            Font font,
            List<ClientTooltipComponent> components,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            Identifier texture,
            boolean animate,
            CallbackInfo ci
    ) {
        if (ScrollableTooltip.isActive()) {
            return;
        }
        TooltipAnimation.endTooltipPass();
    }
}
