package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.smolpeople.SmolPeopleCamera;
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

import java.util.List;

public final class NoSwimPoseUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;
    public static final float WORLD_SWIM_NAMETAG_Y_OFFSET = 1.2F;

    private static final long EXIT_GRACE_MS = 1000L;
    private static final float SWIM_AMOUNT_EPSILON = 0.001F;

    private static boolean wasInWater = false;
    private static long exitWaterTime = 0L;
    private static float groundedSwimSneakEyeProgress = 0.0F;
    private static boolean poolBottomStandUpLerpActive = false;

    private static final int AREA_CHECK_INTERVAL_TICKS = 10;
    private static final int AREA_CACHE_MISS_GRACE = 3;
    private static boolean areaCacheValid = false;
    private static long areaCacheGameTime = Long.MIN_VALUE;
    private static boolean areaCacheBlocked = false;
    private static int areaCacheIslandMiss = 0;
    private static int areaCacheSubAreaMiss = 0;
    private static String areaCacheIsland = "";
    private static String areaCacheSubArea = "";
    private static String areaBlacklistSource = null;
    private static List<String> areaBlacklistEntries = List.of();

    private static final float POOL_BOTTOM_CAMERA_CONVERGE_EPSILON = 0.01F;

    private NoSwimPoseUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("NoSwimPose");
        return m != null && m.isEnabled() && !isAreaBlocked();
    }

    public static boolean isAreaBlocked() {
        refreshAreaCache();
        return areaCacheBlocked;
    }

    public static boolean matchesCurrentArea(String entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }
        refreshAreaCache();
        return entry.equalsIgnoreCase(areaCacheIsland) || entry.equalsIgnoreCase(areaCacheSubArea);
    }

    private static void refreshAreaCache() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            areaCacheValid = false;
            areaCacheBlocked = false;
            areaCacheIsland = "";
            areaCacheSubArea = "";
            return;
        }
        long gameTime = mc.level.getGameTime();
        if (areaCacheValid && Math.abs(gameTime - areaCacheGameTime) < AREA_CHECK_INTERVAL_TICKS) {
            return;
        }
        areaCacheValid = true;
        areaCacheGameTime = gameTime;
        if (!hasBlacklistEntries()) {
            areaCacheIsland = "";
            areaCacheSubArea = "";
            areaCacheIslandMiss = 0;
            areaCacheSubAreaMiss = 0;
            areaCacheBlocked = false;
            return;
        }
        String island = areaName(LocateUtils.areaIslandName(mc));
        String subArea = areaName(LocateUtils.scoreboardSubAreaName(mc));
        if (island.isEmpty()) {
            if (areaCacheIslandMiss >= AREA_CACHE_MISS_GRACE) {
                areaCacheIsland = "";
            }
            areaCacheIslandMiss++;
        } else {
            areaCacheIsland = island;
            areaCacheIslandMiss = 0;
        }
        if (subArea.isEmpty()) {
            if (areaCacheSubAreaMiss >= AREA_CACHE_MISS_GRACE) {
                areaCacheSubArea = "";
            }
            areaCacheSubAreaMiss++;
        } else {
            areaCacheSubArea = subArea;
            areaCacheSubAreaMiss = 0;
        }
        areaCacheBlocked = matchesBlacklistedArea();
    }

    private static boolean hasBlacklistEntries() {
        String raw = ConfigManager.noSwimPoseAreaBlacklist;
        if (raw == null || raw.isEmpty()) {
            areaBlacklistSource = raw;
            areaBlacklistEntries = List.of();
            return false;
        }
        if (!raw.equals(areaBlacklistSource)) {
            areaBlacklistSource = raw;
            areaBlacklistEntries = TextListCodec.decode(raw);
        }
        for (String entry : areaBlacklistEntries) {
            if (!entry.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesBlacklistedArea() {
        if (areaCacheIsland.isEmpty() && areaCacheSubArea.isEmpty()) {
            return false;
        }
        for (String entry : areaBlacklistEntries) {
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.equalsIgnoreCase(areaCacheIsland) || entry.equalsIgnoreCase(areaCacheSubArea)) {
                return true;
            }
        }
        return false;
    }

    private static String areaName(String name) {
        return name == null ? "" : name.trim();
    }

    public static boolean shouldDeferCameraEyeHeightToVanilla(Player player) {
        if (player == null) {
            return true;
        }
        return player.isSleeping()
            || player.isPassenger()
            || player.isFallFlying();
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

        if (isInSwimAction(player) && isInWaterContext(player)) {
            wasInWater = true;
            exitWaterTime = 0L;
            return SwimVisualPhase.ACTIVE;
        }

        if (!wasInWater) {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || shouldDeferCameraEyeHeightToVanilla(mc.player)) {
            return false;
        }
        if (shouldApplySmolThirdPersonFrontDryExitCamera(mc.player)) {
            return true;
        }
        if (!shouldApplyEyeHeightChange()) {
            return false;
        }
        if (mc.options.getCameraType().isFirstPerson()) {
            return true;
        }
        return isSwimmingPose(mc.player);
    }

    private static boolean shouldApplySmolThirdPersonFrontDryExitCamera(Player player) {
        if (shouldDeferCameraEyeHeightToVanilla(player)) {
            return false;
        }
        if (!isFeatureActive() || !SmolPeopleCamera.isThirdPersonFrontActive()) {
            return false;
        }
        if (isInWaterSwimVisualContext()) {
            return false;
        }
        return !isSwimmingPose(player) && hasResidualSwimVisual(player);
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
        if (shouldDeferCameraEyeHeightToVanilla(player)) {
            return false;
        }
        if (shouldApplySmolThirdPersonFrontDryExitCamera(player)) {
            return true;
        }
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
        if (shouldDeferCameraEyeHeightToVanilla(player)
            || !shouldApplyEyeHeightChange()
            || !isGroundedInWater(player)) {
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
        if (shouldDeferCameraEyeHeightToVanilla(player)) {
            return player.getEyeHeight();
        }
        if (shouldApplySmolThirdPersonFrontDryExitCamera(player)) {
            return STANDING_EYE_HEIGHT;
        }
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && shouldDeferCameraEyeHeightToVanilla(mc.player)) {
            return false;
        }
        return isFeatureActive() && isInWaterSwimVisualContext();
    }

    public static boolean shouldForceStandingModelAppearance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && shouldDeferCameraEyeHeightToVanilla(mc.player)) {
            return false;
        }
        return isFeatureActive() && isInWaterSwimVisualContext();
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
        if (!isFeatureActive()) {
            return false;
        }
        if (!isSelfPlayerById(state.id)) {
            return false;
        }
        if (isPaperDollState(state) || RenderScope.isPaperDollRender()) {
            return shouldForceStandingModelAppearance();
        }
        return isLevelRenderContext(state);
    }

    private static boolean isPaperDollState(AvatarRenderState state) {
        return state instanceof EntityRenderStateInterface context && context.baity$isPaperDollRenderState();
    }

    public static void clearSwimRenderState(AvatarRenderState state) {
        if (state == null) {
            return;
        }
        if (isPaperDollState(state) || RenderScope.isPaperDollRender()) {
            if (!shouldForceStandingModelAppearance()) {
                return;
            }
            applyStandingSwimVisual(state);
            return;
        }
        if (!shouldForceStandingModelAppearance()) {
            return;
        }
        applyStandingSwimVisual(state);
    }

    private static void applyStandingSwimVisual(AvatarRenderState state) {
        state.isVisuallySwimming = false;
        state.swimAmount = 0.0F;
        if (isSneaking()) {
            state.isCrouching = true;
            state.pose = Pose.CROUCHING;
        } else {
            state.pose = Pose.STANDING;
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
