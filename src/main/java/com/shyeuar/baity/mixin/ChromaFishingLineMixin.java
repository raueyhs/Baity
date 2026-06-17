package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.features.fishing.ChromaFishingLine;
import com.shyeuar.baity.features.fishing.ChromaFishingLineRenderStateExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(FishingHookRenderer.class)
public class ChromaFishingLineMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V",
            at = @At("RETURN")
    )
    private void baity$trackLocalHook(FishingHook hook, FishingHookRenderState state, float partialTick, CallbackInfo ci) {
        ChromaFishingLineRenderStateExtension ext = (ChromaFishingLineRenderStateExtension) (Object) state;
        ext.baity$setHookEntityId(hook.getId());
        boolean local = hook.getOwner() == Minecraft.getInstance().player;
        ext.baity$setLocalPlayerHook(local);
        if (ChromaFishingLine.isEnabled() && local && hook.getPlayerOwner() != null) {
            ChromaFishingLine.setLocalLine(hook.getId(), state.lineOriginOffset, state.x, state.y, state.z);
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;lines()Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    shift = At.Shift.BEFORE
            )
    )
    private void baity$captureLineWidth(
            FishingHookRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraRenderState,
            CallbackInfo ci,
            @Local(ordinal = 3) float lineWidth
    ) {
        if (ChromaFishingLine.isEnabled()) {
            ChromaFishingLine.setAppropriateLineWidth(lineWidth);
        }
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"
            )
    )
    private void baity$skipLocalVanillaLineSubmit(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer renderer,
            Operation<Void> original,
            FishingHookRenderState state
    ) {
        if (renderType != RenderTypes.lines() || !baity$isLocalPlayerHook(state)) {
            original.call(collector, poseStack, renderType, renderer);
            return;
        }

        ChromaFishingLineRenderStateExtension ext = (ChromaFishingLineRenderStateExtension) (Object) state;
        ChromaFishingLine.setLocalLine(
                ext.baity$getHookEntityId(),
                state.lineOriginOffset,
                state.x,
                state.y,
                state.z
        );
    }

    @Inject(method = "lambda$submit$1", at = @At("HEAD"), cancellable = true)
    private static void baity$cancelLocalVanillaDeferredLine(
            float x,
            float y,
            float z,
            float lineWidth,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            CallbackInfo ci
    ) {
        if (ChromaFishingLine.isEnabled() && ChromaFishingLine.matchesLocalOrigin(x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(method = "stringVertex", at = @At("HEAD"), cancellable = true)
    private static void baity$cancelLocalVanillaStringVertex(
            float x,
            float y,
            float z,
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float segmentStart,
            float segmentEnd,
            float lineWidth,
            CallbackInfo ci
    ) {
        if (ChromaFishingLine.isEnabled() && ChromaFishingLine.matchesLocalOrigin(x, y, z)) {
            ci.cancel();
        }
    }

    private static boolean baity$isLocalPlayerHook(FishingHookRenderState state) {
        if (!ChromaFishingLine.isEnabled()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }

        ChromaFishingLineRenderStateExtension ext = (ChromaFishingLineRenderStateExtension) (Object) state;
        if (ext.baity$isLocalPlayerHook()) {
            return true;
        }

        int hookId = ext.baity$getHookEntityId();
        if (hookId >= 0) {
            Entity entity = mc.level.getEntity(hookId);
            if (entity instanceof FishingHook hook) {
                return hook.getOwner() == mc.player;
            }
        }

        return false;
    }
}
