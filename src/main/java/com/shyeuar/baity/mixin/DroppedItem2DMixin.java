package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shyeuar.baity.features.droppeditem.DroppedItemScale;
import com.shyeuar.baity.features.droppeditem.ItemEntityRenderStateExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class DroppedItem2DMixin {

    @Shadow private RandomSource random;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void baity$prepareDrop(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        ItemEntityRenderStateExtension ext = (ItemEntityRenderStateExtension) state;
        ext.baity$setItemStack(entity.getItem());
        ext.baity$setDropScale(DroppedItemScale.multiplier(entity.getItem()));
    }

    @Inject(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;submitMultipleFromCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/phys/AABB;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void baity$scaleDrop(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState, CallbackInfo ci) {
        float scale = ((ItemEntityRenderStateExtension) state).baity$getDropScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void baity$render2d(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState, CallbackInfo ci) {
        if (!DroppedItemScale.is2dActive() || state.item.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        net.minecraft.client.Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null) {
            return;
        }
        poseStack.pushPose();
        float scale = ((ItemEntityRenderStateExtension) state).baity$getDropScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
        AABB box = state.item.getModelBoundingBox();
        float baseOffset = -((float) box.minY) + 0.0625F;
        float bob = (float) Math.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
        poseStack.translate(0.0F, bob + baseOffset, 0.0F);
        float yaw = camera.yRot();
        float pitch = camera.xRot();
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.rotate(Axis.XP.rotationDegrees(-pitch));
        poseStack.scale(1.0F, 1.0F, 0.01F);
        ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state, this.random, box);
        poseStack.popPose();
        ci.cancel();
    }
}
