package com.shyeuar.baity.features.blockanimation;

import com.shyeuar.baity.features.CustomHandHoldingManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.LivingEntityAccessor;
import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class BlockAnimationCircleController {
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final int SWING_BLOCK_GRACE_TICKS = 4;
    private static final int ROTOR_AIM_BASELINE_SWING_DURATION_TICKS = 6;
    private static final float ROTOR_AIM_SPEED_MUL = 1.25f;

    private static final float ROTOR_TP_WOBBLE_TICKS_PERIOD = 11f;
    private static final float ROTOR_TP_WOBBLE_ARM_RADIANS = 0.048f;
    private static final float ROTOR_TP_WOBBLE_ITEM_LOCAL_Y = 0.014f;

    private static float displayRadians;
    private static float prevDisplayRadians;
    private static float cycleEndRadians;
    private static int pendingCyclesAfterThis;
    private static boolean spinning;
    private static boolean normalizePhaseNextTick;

    private static long lastAcceptedSwingGameTime = Long.MIN_VALUE;
    private static long lastSpinCommitGameTime = -1L;

    private static float rotorAim;
    private static float prevRotorAim;

    private static int pendingSpinCount;
    private static int swingGraceTicks;

    private BlockAnimationCircleController() {}

    public static void queueSpin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        long gameTime = mc.level != null ? mc.level.getGameTime() : System.nanoTime();
        if (gameTime == lastAcceptedSwingGameTime) {
            return;
        }
        lastAcceptedSwingGameTime = gameTime;

        if (!spinning) {
            spinning = true;
            pendingCyclesAfterThis = 0;
            cycleEndRadians = nextWholeCircle(displayRadians);
        } else {
            pendingCyclesAfterThis++;
        }
        markSpinCommit(mc.player);
    }

    private static void markSpinCommit(@Nullable Player player) {
        if (player != null && player.level() != null) {
            lastSpinCommitGameTime = player.level().getGameTime();
        }
    }

    public static void queueSwingGraceSpin() {
        pendingSpinCount = 1;
        swingGraceTicks = SWING_BLOCK_GRACE_TICKS;
    }

    private static int getSwingSilenceTicks(Player player) {
        int dur = ((LivingEntityAccessor) (Object) player).baity$getCurrentSwingDuration();
        return Math.max(1, dur);
    }

    private static int getCircleRotationDurationTicks(Player player) {
        int vanillaOrReported = ((LivingEntityAccessor) (Object) player).baity$getCurrentSwingDuration();
        if (vanillaOrReported < 1) {
            vanillaOrReported = 1;
        }
        Module ch = ModuleManager.getModuleByName("CustomHandHolding");
        if (ch == null || !ch.isEnabled()) {
            return vanillaOrReported;
        }
        int setting = CustomHandHoldingManager.getInstance().getSwingDuration();
        if (setting < 1) {
            setting = 1;
        }
        return Math.min(ROTOR_AIM_BASELINE_SWING_DURATION_TICKS, setting);
    }

    public static void tick(Player player, Minecraft mc) {
        prevDisplayRadians = displayRadians;
        prevRotorAim = rotorAim;
        if (!BlockAnimationUtils.isFeatureActive() || !BlockAnimationUtils.isSpinAnimaMode()) {
            resetAngles();
            return;
        }
        if (player == null || mc == null || mc.options == null || player.level() == null) {
            resetAngles();
            return;
        }

        if (!spinning && normalizePhaseNextTick) {
            displayRadians = 0f;
            prevDisplayRadians = 0f;
            normalizePhaseNextTick = false;
        }

        boolean blocking = BlockAnimationUtils.isPlayerBlockingWithSword(player);
        int rotationDur = getCircleRotationDurationTicks(player);
        float step = TAU / (float) rotationDur;

        long gameTime = player.level().getGameTime();
        int silenceTicks = getSwingSilenceTicks(player);
        if (lastSpinCommitGameTime >= 0L
                && (spinning || pendingCyclesAfterThis > 0 || pendingSpinCount > 0)) {
            if (gameTime - lastSpinCommitGameTime >= silenceTicks) {
                pendingCyclesAfterThis = 0;
            }
        }

        if (!blocking && swingGraceTicks > 0) {
            swingGraceTicks--;
            if (swingGraceTicks <= 0) {
                pendingSpinCount = 0;
            }
        }
        if (blocking && pendingSpinCount > 0) {
            queueSpin();
            pendingSpinCount = 0;
            swingGraceTicks = 0;
        }

        if (spinning) {
            displayRadians = Math.min(displayRadians + step, cycleEndRadians);
            if (cycleEndRadians - displayRadians <= 1e-4f) {
                if (pendingCyclesAfterThis > 0) {
                    pendingCyclesAfterThis--;
                    cycleEndRadians += TAU;
                } else {
                    spinning = false;
                    normalizePhaseNextTick = true;
                    cycleEndRadians = 0f;
                }
            }
        }

        if (BlockAnimationUtils.isRotorAnimaMode() && blocking) {
            int baselineDur = Math.max(1, Math.min(rotationDur, ROTOR_AIM_BASELINE_SWING_DURATION_TICKS));
            float baselineStep = TAU / (float) baselineDur;
            float aimStep = ROTOR_AIM_SPEED_MUL * baselineStep / ((float) (Math.PI * 0.5));
            rotorAim = Mth.clamp(rotorAim + aimStep, 0f, 1f);
        } else {
            rotorAim = 0f;
        }

        if (!blocking && !spinning && pendingSpinCount <= 0 && swingGraceTicks <= 0) {
            resetAngles();
        }
    }

    public static float getRenderRadians(float partialTick) {
        if (isClientPaused()) {
            return displayRadians;
        }
        return Mth.lerp(partialTick, prevDisplayRadians, displayRadians);
    }

    public static float getRotorAim(float partialTick) {
        if (isClientPaused()) {
            return rotorAim;
        }
        return Mth.lerp(partialTick, prevRotorAim, rotorAim);
    }

    public static float getRotorAimThirdPerson(float partialTick) {
        return getRotorAim(partialTick);
    }

    public static float getRotorAimNow() {
        return rotorAim;
    }

    public static float getRotorWobblePhase(float partialTick) {
        if (!BlockAnimationUtils.isRotorAnimaMode() || !hasActiveSpin()) return 0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return 0f;
        float t = mc.level.getGameTime() + partialTick;
        return t * (TAU / ROTOR_TP_WOBBLE_TICKS_PERIOD);
    }

    public static float getRotorThirdPersonWobbleArmRadians() {
        if (!BlockAnimationUtils.isRotorAnimaMode() || !hasActiveSpin()) return 0f;
        Minecraft mc = Minecraft.getInstance();
        float pt = mc != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(false) : 0f;
        return ROTOR_TP_WOBBLE_ARM_RADIANS * Mth.sin(getRotorWobblePhase(pt));
    }

    public static float getRotorThirdPersonWobbleItemLocalY() {
        if (!BlockAnimationUtils.isRotorAnimaMode() || !hasActiveSpin()) return 0f;
        Minecraft mc = Minecraft.getInstance();
        float pt = mc != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(false) : 0f;
        return ROTOR_TP_WOBBLE_ITEM_LOCAL_Y * Mth.sin(getRotorWobblePhase(pt));
    }

    private static boolean isClientPaused() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.isPaused();
    }

    public static float getRotorAimThirdPersonNow() {
        return rotorAim;
    }

    public static boolean hasActiveSpin() {
        return spinning;
    }

    private static float nextWholeCircle(float radians) {
        float n = (float) (Math.ceil(radians / TAU - 1e-5f) * TAU);
        if (n - radians < 1e-4f) {
            n += TAU;
        }
        return n;
    }

    private static void resetAngles() {
        displayRadians = 0f;
        prevDisplayRadians = 0f;
        cycleEndRadians = 0f;
        pendingCyclesAfterThis = 0;
        spinning = false;
        normalizePhaseNextTick = false;
        lastAcceptedSwingGameTime = Long.MIN_VALUE;
        lastSpinCommitGameTime = -1L;
        pendingSpinCount = 0;
        swingGraceTicks = 0;
        rotorAim = 0f;
        prevRotorAim = 0f;
    }
}
