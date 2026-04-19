package com.shyeuar.baity.features.blockanimation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shyeuar.baity.mixin.accessor.ItemInHandRendererAccessor;
import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BlockAnimationRenderer {
    private static final float ROTOR_AIM_RADIANS = (float) (Math.PI * 0.5);

    private static final float ROTOR_THIRD_PERSON_ALONG_ARM_LOCAL_SINGLE_AXIS = 0.50f;
    private static final float ROTOR_THIRD_PERSON_TIP_PITCH_DEGREES = 22f;
    private static final float ROTOR_THIRD_PERSON_EXTRA_LIFT_LOCAL_Y = 0.080f;

    private BlockAnimationRenderer() {}

    public enum RenderResult {
        PASS,
        INTERRUPT
    }

    public static RenderResult renderFirstPerson(ItemInHandRenderer itemInHandRenderer,
            InteractionHand interactionHand,
            AbstractClientPlayer player,
            HumanoidArm humanoidArm,
            ItemStack itemStack,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int combinedLight,
            float partialTick,
            float interpolatedPitch,
            float swingProgress,
            float equipProgress) {

        if (!BlockAnimationUtils.isFeatureActive()) return RenderResult.PASS;
        if (BlockAnimationUtils.isUsingConsumableAnimation(player)
                && interactionHand == player.getUsedItemHand()) {
            return RenderResult.PASS;
        }
        boolean blockingNow = BlockAnimationUtils.isPlayerBlockingWithSword(player);
        boolean keepCircleSpin = BlockAnimationUtils.isSpinAnimaMode()
                && BlockAnimationCircleController.hasActiveSpin();
        if (!blockingNow && !keepCircleSpin) return RenderResult.PASS;

        InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(player);
        if (blockingHand != interactionHand) return RenderResult.PASS;

        if (itemStack.isEmpty() || itemStack.has(net.minecraft.core.component.DataComponents.MAP_ID)) {
            return RenderResult.PASS;
        }

        if (itemStack.is(net.minecraft.world.item.Items.CROSSBOW)) {
            return RenderResult.PASS;
        }

        poseStack.pushPose();

        boolean mainHand = interactionHand == InteractionHand.MAIN_HAND;
        HumanoidArm handSide = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isHandSideRight = handSide == HumanoidArm.RIGHT;

        ItemInHandRendererAccessor accessor =
                (ItemInHandRendererAccessor) itemInHandRenderer;
        accessor.baity$callApplyItemArmTransform(poseStack, handSide, equipProgress);

        com.shyeuar.baity.gui.module.Module chModule =
                com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("CustomHandHolding");
        if (chModule != null && chModule.isEnabled()) {
            com.shyeuar.baity.features.CustomHandHoldingManager.getInstance()
                    .applyPosition(poseStack, handSide);
            com.shyeuar.baity.features.CustomHandHoldingManager.getInstance()
                    .applyRotation(poseStack, handSide);
        }

        if (BlockAnimationUtils.isInteractAnimationsEnabled() && !BlockAnimationUtils.isSpinAnimaMode()) {
            accessor.baity$callApplyItemArmAttackTransform(poseStack, handSide, swingProgress);
        }

        if (blockingNow && !BlockAnimationUtils.isRotorAnimaMode()) {
            applyFirstPersonBlockTransform(poseStack, handSide);
        }
        applySpinModeItemRotationFirstPerson(poseStack, handSide, partialTick, player, blockingNow);

        if (chModule != null && chModule.isEnabled()) {
            com.shyeuar.baity.features.CustomHandHoldingManager.getInstance()
                    .applyScale(poseStack);
        }

        itemInHandRenderer.renderItem(player,
                itemStack,
                isHandSideRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND :
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                submitNodeCollector,
                combinedLight);

        poseStack.popPose();

        return RenderResult.INTERRUPT;
    }

    private static void applyFirstPersonBlockTransform(PoseStack matrixStack, HumanoidArm hand) {
        applyBlockingTransformOnly(matrixStack, hand);
    }

    private static void applySpinModeItemRotationFirstPerson(PoseStack poseStack, HumanoidArm handSide,
            float partialTick, AbstractClientPlayer player, boolean blockingNow) {
        if (!BlockAnimationUtils.isSpinAnimaMode()) return;
        float dir = handSide == HumanoidArm.RIGHT ? 1f : -1f;
        if (BlockAnimationUtils.isRotorAnimaMode()) {
            Vec3 look = player.getViewVector(partialTick);
            Vector3f view = new Vector3f((float) look.x, (float) look.y, (float) look.z);
            Vector3f axis = view.cross(new Vector3f(0f, 1f, 0f), new Vector3f());
            if (axis.lengthSquared() < 1e-8f) {
                axis = view.cross(new Vector3f(1f, 0f, 0f), new Vector3f());
            }
            if (blockingNow) {
                float aim = BlockAnimationCircleController.getRotorAim(partialTick);
                float aimDir = handSide == HumanoidArm.LEFT ? -dir : dir;
                applyCircleSpinAboutWorldDirection(poseStack, aimDir, -ROTOR_AIM_RADIANS * aim, axis);
            }
            float angle = BlockAnimationCircleController.getRenderRadians(partialTick);
            if (Math.abs(angle) < 1e-5f) return;
            applyCircleSpinAboutWorldDirection(poseStack, dir, -angle, axis);
        } else {
            float angle = BlockAnimationCircleController.getRenderRadians(partialTick);
            if (Math.abs(angle) < 1e-5f) return;
            Vec3 look = player.getViewVector(partialTick);
            Vector3f view = new Vector3f((float) look.x, (float) look.y, (float) look.z);
            applyCircleSpinAboutWorldDirection(poseStack, dir, -angle, view);
        }
    }

    private static void applyCircleSpinAboutWorldDirection(PoseStack poseStack, float dir, float angle,
            Vector3f worldDir) {
        if (Math.abs(angle) < 1e-5f) return;
        Vector3f w = new Vector3f(worldDir).normalize();
        Matrix4f mat = new Matrix4f(poseStack.last().pose());
        Matrix3f linear = new Matrix3f(mat);
        if (Math.abs(linear.determinant()) < 1e-8f) {
            poseStack.mulPose(Axis.ZP.rotation(dir * angle));
            return;
        }
        orthonormalize3x3Rotation(linear);
        Vector3f axisLocal = linear.transpose().transform(w, new Vector3f());
        if (axisLocal.lengthSquared() < 1e-12f) {
            poseStack.mulPose(Axis.ZP.rotation(dir * angle));
            return;
        }
        axisLocal.normalize();
        poseStack.mulPose(new Quaternionf().rotationAxis(dir * angle, axisLocal.x, axisLocal.y, axisLocal.z));
    }

    private static void orthonormalize3x3Rotation(Matrix3f m) {
        Vector3f c0 = new Vector3f(m.m00, m.m10, m.m20);
        Vector3f c1 = new Vector3f(m.m01, m.m11, m.m21);
        Vector3f c2 = new Vector3f(m.m02, m.m12, m.m22);
        c0.normalize();
        c1.sub(new Vector3f(c0).mul(c0.dot(c1)));
        c1.normalize();
        c2.set(c0).cross(c1).normalize();
        m.m00 = c0.x;
        m.m10 = c0.y;
        m.m20 = c0.z;
        m.m01 = c1.x;
        m.m11 = c1.y;
        m.m21 = c1.z;
        m.m02 = c2.x;
        m.m12 = c2.y;
        m.m22 = c2.z;
    }

    public static void applyBlockingTransformOnly(PoseStack matrixStack, HumanoidArm hand) {
        int direction = hand == HumanoidArm.RIGHT ? 1 : -1;
        matrixStack.translate(direction * -0.14142136F, 0.08F, 0.14142136F);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
        matrixStack.mulPose(Axis.YP.rotationDegrees(direction * 13.365F));
        matrixStack.mulPose(Axis.ZP.rotationDegrees(direction * 78.05F));
    }

    public static void renderThirdPerson(AvatarRenderState renderState,
            ArmedModel<AvatarRenderState> model,
            ItemStackRenderState itemStackRenderState,
            HumanoidArm humanoidArm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            AbstractClientPlayer player,
            boolean blockingNow) {

        poseStack.pushPose();
        ArmedModel<AvatarRenderState> avatarModel = (ArmedModel<AvatarRenderState>) model;
        avatarModel.translateToHand(renderState, humanoidArm, poseStack);
        boolean leftHand = humanoidArm == HumanoidArm.LEFT;

        if (BlockAnimationUtils.isRotorAnimaMode()) {
            applyRotorThirdPersonGripAdjustments(poseStack, leftHand);
        }

        if (blockingNow && !BlockAnimationUtils.isRotorAnimaMode()) {
            applyThirdPersonBlockTransform(poseStack, leftHand);
        }
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

        com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.ItemStackRenderStateAccessorImpl stateAccessor =
                (com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.ItemStackRenderStateAccessorImpl) (Object) itemStackRenderState;
        ItemStackRenderState.LayerRenderState firstLayer = stateAccessor.baity$callFirstLayer();
        if (BlockAnimationUtils.isRotorAnimaMode()) {
            ItemTransform transform = null;
            if (firstLayer != null) {
                com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.LayerRenderStateAccessor layerAccessor =
                        (com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.LayerRenderStateAccessor) (Object) firstLayer;
                transform = layerAccessor.baity$getTransform();
            }
            applyRotorThirdPersonSpinKeepingVanillaPose(poseStack, transform, leftHand, partialTick, player, blockingNow);
        } else {
            applySpinModeItemRotationThirdPerson(poseStack, leftHand, partialTick, player, blockingNow);

            if (firstLayer != null) {
                com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.LayerRenderStateAccessor layerAccessor =
                        (com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.LayerRenderStateAccessor) (Object) firstLayer;
                revertItemTransform(layerAccessor.baity$getTransform(), leftHand, poseStack);
            }
        }
        itemStackRenderState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void applySpinModeItemRotationThirdPerson(PoseStack poseStack, boolean leftHand, float partialTick,
            AbstractClientPlayer player, boolean blockingNow) {
        if (!BlockAnimationUtils.isSpinAnimaMode()) return;
        float dir = leftHand ? -1f : 1f;
        if (BlockAnimationUtils.isRotorAnimaMode()) {
            return;
        } else {
            float angle = BlockAnimationCircleController.getRenderRadians(partialTick);
            if (Math.abs(angle) < 1e-5f) return;
            applyCircleSpinItemLocalZ(poseStack, dir, angle);
        }
    }

    private static void applyCircleSpinItemLocalZ(PoseStack poseStack, float dir, float angle) {
        if (Math.abs(angle) < 1e-5f) return;
        poseStack.mulPose(Axis.ZP.rotation(dir * angle));
    }

    private static void applyRotorThirdPersonGripAdjustments(PoseStack poseStack, boolean leftHand) {
        float wobbleY = BlockAnimationCircleController.getRotorThirdPersonWobbleItemLocalY();
        poseStack.translate(0f,
                ROTOR_THIRD_PERSON_ALONG_ARM_LOCAL_SINGLE_AXIS + ROTOR_THIRD_PERSON_EXTRA_LIFT_LOCAL_Y + wobbleY,
                0f);
        float pitchDeg = leftHand ? ROTOR_THIRD_PERSON_TIP_PITCH_DEGREES : -ROTOR_THIRD_PERSON_TIP_PITCH_DEGREES;
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
    }

    private static void applyThirdPersonBlockTransform(PoseStack poseStack, boolean leftHand) {
        poseStack.translate((leftHand ? 1.0F : -1.0F) / 16.0F, 0.4375F, 0.0625F);
        poseStack.translate(leftHand ? -0.035F : 0.05F, leftHand ? 0.045F : 0.0F, leftHand ? -0.135F : -0.1F);
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -50.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -60.0F));
        poseStack.translate(0.0F, 0.1875F, 0.0F);

        poseStack.scale(0.625F, 0.625F, 0.625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XN.rotationDegrees(-100.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(leftHand ? 35.0F : 45.0F));
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.28125F);
    }

    private static void revertItemTransform(ItemTransform itemTransform, boolean leftHand, PoseStack poseStack) {
        if (itemTransform != ItemTransform.NO_TRANSFORM) {
            float angleX = itemTransform.rotation().x();
            float angleY = leftHand ? -itemTransform.rotation().y() : itemTransform.rotation().y();
            float angleZ = leftHand ? -itemTransform.rotation().z() : itemTransform.rotation().z();
            Quaternionf quaternion = new Quaternionf().rotationXYZ(angleX * 0.017453292F,
                    angleY * 0.017453292F,
                    angleZ * 0.017453292F);
            quaternion.conjugate();
            poseStack.scale(1.0F / itemTransform.scale().x(),
                    1.0F / itemTransform.scale().y(),
                    1.0F / itemTransform.scale().z());
            poseStack.mulPose(quaternion);
            poseStack.translate((leftHand ? -1.0F : 1.0F) * -itemTransform.translation().x(),
                    -itemTransform.translation().y(),
                    -itemTransform.translation().z());
        }
    }

    private static void applyItemTransform(ItemTransform itemTransform, boolean leftHand, PoseStack poseStack) {
        if (itemTransform != ItemTransform.NO_TRANSFORM) {
            float angleX = itemTransform.rotation().x();
            float angleY = leftHand ? -itemTransform.rotation().y() : itemTransform.rotation().y();
            float angleZ = leftHand ? -itemTransform.rotation().z() : itemTransform.rotation().z();
            Quaternionf quaternion = new Quaternionf().rotationXYZ(angleX * 0.017453292F,
                    angleY * 0.017453292F,
                    angleZ * 0.017453292F);
            poseStack.translate((leftHand ? -1.0F : 1.0F) * itemTransform.translation().x(),
                    itemTransform.translation().y(),
                    itemTransform.translation().z());
            poseStack.mulPose(quaternion);
            poseStack.scale(itemTransform.scale().x(), itemTransform.scale().y(), itemTransform.scale().z());
        }
    }

    private static void applyRotorThirdPersonSpinKeepingVanillaPose(PoseStack poseStack,
            ItemTransform transformOrNull,
            boolean leftHand,
            float partialTick,
            AbstractClientPlayer player,
            boolean blockingNow) {
        float angle = BlockAnimationCircleController.getRenderRadians(partialTick);
        if (Math.abs(angle) < 1e-5f) return;
        float dir = leftHand ? -1f : 1f;

        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        Vec3 bodyForward = Vec3.directionFromRotation(0f, bodyYaw);
        Vector3f forward = new Vector3f((float) bodyForward.x, (float) bodyForward.y, (float) bodyForward.z);
        Vector3f axisWorld = forward.cross(new Vector3f(0f, 1f, 0f), new Vector3f());
        if (axisWorld.lengthSquared() < 1e-8f) {
            axisWorld = forward.cross(new Vector3f(1f, 0f, 0f), new Vector3f());
        }

        if (transformOrNull != null && transformOrNull != ItemTransform.NO_TRANSFORM) {
            applyItemTransform(transformOrNull, leftHand, poseStack);
            if (blockingNow) {
                float aim = BlockAnimationCircleController.getRotorAim(partialTick);
                applyCircleSpinAboutWorldDirection(poseStack, dir, -ROTOR_AIM_RADIANS * aim, axisWorld);
            }
            applyCircleSpinAboutWorldDirection(poseStack, dir, -angle, axisWorld);
            revertItemTransform(transformOrNull, leftHand, poseStack);
        } else {
            if (blockingNow) {
                float aim = BlockAnimationCircleController.getRotorAim(partialTick);
                applyCircleSpinAboutWorldDirection(poseStack, dir, -ROTOR_AIM_RADIANS * aim, axisWorld);
            }
            applyCircleSpinAboutWorldDirection(poseStack, dir, -angle, axisWorld);
        }
    }

}