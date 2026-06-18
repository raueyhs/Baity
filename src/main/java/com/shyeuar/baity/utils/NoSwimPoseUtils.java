package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class NoSwimPoseUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;
    public static final float WORLD_SWIM_NAMETAG_Y_OFFSET = 1.2F;

    private static final long EXIT_GRACE_MS = 1000L;
    private static final float SWIM_AMOUNT_EPSILON = 0.001F;

    private static boolean wasInWater = false;
    private static long exitWaterTime = 0L;
    private static float groundedSwimSneakEyeProgress = 0.0F;
    private static boolean poolBottomStandUpLerpActive = false;

    private static final float POOL_BOTTOM_CAMERA_CONVERGE_EPSILON = 0.01F;

    private NoSwimPoseUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("NoSwimPose");
        return m != null && m.isEnabled();
    }

    private enum SwimVisualPhase {
        NONE,
        ACTIVE,
        GRACE
    }

    public static boolean isInWaterContext(Player player) {
        return player.isEyeInFluid(FluidTags.WATER) || player.isInWater();
    }

    private static boolean isInSwimAction(Player player) {
        return player.getPose() == Pose.SWIMMING || player.isSwimming();
    }

    private static SwimVisualPhase resolveSwimVisualPhase() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return SwimVisualPhase.NONE;
        }

        Player player = mc.player;
        long now = System.currentTimeMillis();

        if (isInSwimAction(player)) {
            if (isInWaterContext(player)) {
                wasInWater = true;
                exitWaterTime = 0L;
                return SwimVisualPhase.ACTIVE;
            }
            return SwimVisualPhase.NONE;
        }

        if (!wasInWater) {
            return SwimVisualPhase.NONE;
        }

        if (!isInWaterContext(player)) {
            wasInWater = false;
            exitWaterTime = 0L;
            return SwimVisualPhase.NONE;
        }

        if (exitWaterTime == 0L) {
            exitWaterTime = now;
        }
        if (now - exitWaterTime < EXIT_GRACE_MS) {
            return SwimVisualPhase.GRACE;
        }

        wasInWater = false;
        exitWaterTime = 0L;
        return SwimVisualPhase.NONE;
    }

    public static boolean isInWaterSwimVisualContext() {
        SwimVisualPhase phase = resolveSwimVisualPhase();
        return phase == SwimVisualPhase.ACTIVE || phase == SwimVisualPhase.GRACE;
    }

    public static boolean isInActiveSwimVisualContext() {
        return resolveSwimVisualPhase() == SwimVisualPhase.ACTIVE;
    }

    public static boolean shouldApplyEyeHeightChange() {
        return isFeatureActive() && isInActiveSwimVisualContext();
    }

    public static boolean shouldApplyCameraEyeHeightChange() {
        if (!shouldApplyEyeHeightChange()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return false;
        }
        if (mc.options.getCameraType().isFirstPerson()) {
            return true;
        }
        return isSwimmingPose(mc.player);
    }

    public static boolean isGroundedInWater(Player player) {
        return isInWaterContext(player) && (player.onGround() || player.horizontalCollision);
    }

    public static boolean usesGroundedPoolBottomSneakCamera(Player player) {
        return shouldApplyEyeHeightChange()
            && isGroundedInWater(player)
            && (isSneaking() || groundedSwimSneakEyeProgress > 0.001F);
    }

    public static boolean shouldSnapCameraEyeHeight(Player player) {
        return shouldApplyCameraEyeHeightChange()
            && isSwimmingPose(player)
            && !usesGroundedPoolBottomSneakCamera(player)
            && !poolBottomStandUpLerpActive;
    }

    public static boolean isPoolBottomStandUpLerpActive() {
        return poolBottomStandUpLerpActive;
    }

    public static float getPoolBottomCameraConvergeEpsilon() {
        return POOL_BOTTOM_CAMERA_CONVERGE_EPSILON;
    }

    public static void beginPoolBottomStandUpLerp() {
        poolBottomStandUpLerpActive = true;
    }

    public static void completePoolBottomStandUpLerp() {
        poolBottomStandUpLerpActive = false;
    }

    public static boolean isSwimmingPose(Player player) {
        return player.getPose() == Pose.SWIMMING;
    }

    public static void tickGroundedSwimCameraState(Player player) {
        if (!shouldApplyEyeHeightChange() || !isGroundedInWater(player)) {
            groundedSwimSneakEyeProgress = 0.0F;
            poolBottomStandUpLerpActive = false;
            return;
        }

        if (isSneaking()) {
            poolBottomStandUpLerpActive = false;
        }

        float vanillaStand = player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
        float crouch = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        float span = vanillaStand - crouch;
        if (span <= 0.001F) {
            return;
        }

        float currentEye = player.getEyeHeight();
        if (currentEye > crouch + 0.01F && currentEye <= vanillaStand + 0.01F) {
            groundedSwimSneakEyeProgress = Mth.clamp((vanillaStand - currentEye) / span, 0.0F, 1.0F);
            return;
        }

        if (!isSwimmingPose(player)) {
            return;
        }

        float step = span / 3.0F;
        if (isSneaking()) {
            groundedSwimSneakEyeProgress = Mth.clamp(groundedSwimSneakEyeProgress + step / span, 0.0F, 1.0F);
        } else {
            if (groundedSwimSneakEyeProgress > 0.001F) {
                poolBottomStandUpLerpActive = true;
            }
            groundedSwimSneakEyeProgress = Mth.clamp(groundedSwimSneakEyeProgress - step / span, 0.0F, 1.0F);
        }
    }

    public static float getCameraEyeHeight(Player player) {
        if (!shouldApplyEyeHeightChange() || !shouldApplyCameraEyeHeightChange()) {
            return player.getEyeHeight();
        }
        if (isSwimmingPose(player)) {
            if (usesGroundedPoolBottomSneakCamera(player)) {
                return getGroundedPoolBottomCameraEye(player);
            }
            return STANDING_EYE_HEIGHT;
        }
        return getWadingEyeHeight(player);
    }

    private static float getPoolBottomSneakProgress(Player player) {
        float vanillaStand = player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
        float crouch = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        float span = vanillaStand - crouch;
        if (span <= 0.001F) {
            return isSneaking() ? 1.0F : 0.0F;
        }

        float currentEye = player.getEyeHeight();
        if (currentEye > crouch + 0.01F && currentEye <= vanillaStand + 0.01F) {
            return Mth.clamp((vanillaStand - currentEye) / span, 0.0F, 1.0F);
        }
        return groundedSwimSneakEyeProgress;
    }

    private static float getGroundedPoolBottomCameraEye(Player player) {
        float progress = getPoolBottomSneakProgress(player);
        if (ConfigManager.oldSneakingEnabled && OldSneakingUtils.isEligiblePlayer(player)) {
            return OldSneakingUtils.getLegacyStyleEyeHeight(player, STANDING_EYE_HEIGHT, progress);
        }
        float crouch = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        return Mth.lerp(progress, STANDING_EYE_HEIGHT, crouch);
    }

    private static float getWadingEyeHeight(Player player) {
        if (usesGroundedPoolBottomSneakCamera(player)) {
            return getGroundedPoolBottomCameraEye(player);
        }
        float standing = STANDING_EYE_HEIGHT;
        if (!isSneaking() && !player.isCrouching()) {
            return standing;
        }
        float crouch = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        float vanillaStand = player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
        float span = vanillaStand - crouch;
        if (span > 0.001F) {
            float progress = Mth.clamp((vanillaStand - player.getEyeHeight()) / span, 0.0F, 1.0F);
            return Mth.lerp(progress, standing, crouch);
        }
        return crouch;
    }

    public static boolean shouldApplyVisualOverrides() {
        return isFeatureActive() && isInWaterSwimVisualContext();
    }

    /**
     * Keep forcing standing model appearance while in water swim context, or while the local
     * player (or their extracted render state) still carries swim animation fields.
     */
    public static boolean shouldForceStandingModelAppearance() {
        if (!isFeatureActive()) {
            return false;
        }
        if (isInWaterSwimVisualContext()) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && hasResidualSwimVisual(mc.player);
    }

    public static boolean shouldForceStandingModelAppearance(AvatarRenderState state) {
        if (shouldForceStandingModelAppearance()) {
            return true;
        }
        if (state == null) {
            return false;
        }
        return state.swimAmount > SWIM_AMOUNT_EPSILON || state.isVisuallySwimming;
    }

    private static boolean hasResidualSwimVisual(Player player) {
        if (player.getPose() == Pose.SWIMMING) {
            return true;
        }
        if (player.isVisuallySwimming()) {
            return true;
        }
        return player.getSwimAmount(1.0F) > SWIM_AMOUNT_EPSILON;
    }

    public static boolean shouldApplyWorldSwimNametagOffset(Player player) {
        return shouldApplyVisualOverrides() && player != null && player.getPose() == Pose.SWIMMING;
    }

    public static float getWorldSwimSneakNametagExtraOffset(Player player) {
        if (!isSneaking()) {
            return 0.0F;
        }
        float vanillaStand = player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
        float crouch = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        return vanillaStand - crouch;
    }

    public static boolean isSelfPlayerById(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    public static boolean isSelfPlayer(Object entity) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && entity == mc.player;
    }

    public static boolean isSneaking() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.options.keyShift.isDown();
    }

    public static boolean isLevelRenderContext(AvatarRenderState state) {
        if (state instanceof EntityRenderStateInterface context) {
            return context.baity$isWorldCameraContext();
        }
        return false;
    }

    public static boolean shouldClearSelfSwimState(AvatarRenderState state) {
        if (!isSelfPlayerById(state.id)) {
            return false;
        }
        if (RenderScope.isPaperDollRender()) {
            return isFeatureActive();
        }
        return isLevelRenderContext(state);
    }

    public static boolean isAbnormalDrySwimPose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isFeatureActive()) {
            return false;
        }
        return !isInWaterSwimVisualContext() && hasResidualSwimVisual(mc.player);
    }

    public static void clearSwimRenderState(AvatarRenderState state) {
        if (RenderScope.isPaperDollRender()) {
            if (!isFeatureActive() || state == null) {
                return;
            }
            state.isVisuallySwimming = false;
            state.swimAmount = 0.0F;
            if (isSneaking()) {
                state.isCrouching = true;
            }
            return;
        }
        if (!shouldForceStandingModelAppearance(state)) {
            return;
        }
        state.isVisuallySwimming = false;
        state.swimAmount = 0.0F;

        if (isSneaking()) {
            state.isCrouching = true;
        }
    }

    public static void restoreSwimRenderStateFromEntity(AvatarRenderState state, LivingEntity entity) {
        if (state == null || entity == null) {
            return;
        }
        state.swimAmount = entity.getSwimAmount(1.0F);
        state.isVisuallySwimming = entity.isVisuallySwimming();
    }
}
