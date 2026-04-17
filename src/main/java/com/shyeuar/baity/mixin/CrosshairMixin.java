package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderPipelines;
import com.shyeuar.baity.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;

@Mixin(Gui.class)
public class CrosshairMixin {
    private static final float SWING_BURST_FALL_TICKS = 3.5f;
    private static final float DYNAMIC_GAP_SCALE = 1.0f;
    private static final float SWING_BURST_MAX_GAP = 20.0f;
    private static final float BOW_RELEASE_HOLD_TICKS = 2.0f;
    private static float swingBurstStartTick = -1.0f;
    private static float bowReleaseUntilTick = -1.0f;
    private static int bowReleaseStartGap = 3;
    private static boolean lastSwinging = false;
    private static int lastSwingTime = 0;
    private static final ResourceLocation WHITE_1PX = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    @ModifyExpressionValue(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"))
    private CameraType baity$forceCrosshairInThirdPersonBack(CameraType original) {
        com.shyeuar.baity.gui.module.Module crosshairModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Crosshair");
        if (crosshairModule == null || !crosshairModule.isEnabled()) return original;
        
        boolean thirdPersonBackCrosshairEnabled = ModuleUtils.getOptionBoolean(
            crosshairModule,
            "show third-person-back crosshair",
            false
        );
        if (thirdPersonBackCrosshairEnabled && original == CameraType.THIRD_PERSON_BACK) {
            return CameraType.FIRST_PERSON;
        }
        return original;
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void baity$renderCustomCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        com.shyeuar.baity.gui.module.Module crosshairModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Crosshair");
        if (crosshairModule == null || !crosshairModule.isEnabled()) return;
        if (!ModuleUtils.getOptionBoolean(crosshairModule, "custom crosshair", true)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (!shouldRenderInCurrentPerspective(mc, crosshairModule)) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        int baseGap = 3;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float nowTick = mc.player.tickCount + partialTick;
        int animatedGap = computeDynamicBowGap(mc.player, baseGap, partialTick, nowTick);
        boolean bowOnly = "bow only".equalsIgnoreCase(ConfigManager.crosshairAnimaMode);
        if (!bowOnly) {
            animatedGap += computeSwingBurstGap(mc.player, nowTick);
        } else {
            lastSwinging = mc.player.swinging;
            lastSwingTime = mc.player.swingTime;
        }

        boolean chroma = ModuleUtils.getOptionBoolean(crosshairModule, "chroma crosshair", false) || ConfigManager.crosshairChromaEnabled;
        drawPaintedCrosshair(guiGraphics, cx, cy, baseGap, animatedGap, chroma);
        ci.cancel();
    }

    private static boolean shouldRenderInCurrentPerspective(Minecraft mc, com.shyeuar.baity.gui.module.Module crosshairModule) {
        CameraType cameraType = mc.options.getCameraType();
        if (cameraType == CameraType.FIRST_PERSON) {
            return true;
        }
        if (cameraType == CameraType.THIRD_PERSON_BACK) {
            return ModuleUtils.getOptionBoolean(crosshairModule, "show third-person-back crosshair", false);
        }
        return false;
    }

    private static int computeDynamicBowGap(Player player, int baseGap, float partialTick, float nowTick) {
        if (!player.isUsingItem()) {
            if (bowReleaseUntilTick > nowTick) {
                float t = 1.0f - ((bowReleaseUntilTick - nowTick) / BOW_RELEASE_HOLD_TICKS);
                t = Math.max(0.0f, Math.min(1.0f, t));
                float heldGap = bowReleaseStartGap + (baseGap - bowReleaseStartGap) * t;
                return Math.max(baseGap, Math.round(heldGap));
            }
            return baseGap;
        }
        ItemStack stack = player.getUseItem();
        if (stack == null || stack.isEmpty()) return baseGap;

        float itemDuration;
        if (stack.is(Items.BOW)) {
            itemDuration = 20.0f;
        } else if (stack.is(Items.CROSSBOW)) {
            itemDuration = 10.0f;
        } else if (stack.is(Items.TRIDENT)) {
            itemDuration = stack.getUseDuration(player);
        } else {
            return baseGap;
        }

        float usedTicks = Math.min(itemDuration, (float) player.getTicksUsingItem() + partialTick);
        float dynamicGap = baseGap + (itemDuration - usedTicks) * DYNAMIC_GAP_SCALE;
        int gap = Math.max(baseGap, Math.round(dynamicGap));
        bowReleaseStartGap = gap;
        bowReleaseUntilTick = nowTick + BOW_RELEASE_HOLD_TICKS;
        return gap;
    }

    private static int computeSwingBurstGap(Player player, float nowTick) {
        ItemStack using = player.getUseItem();
        if (player.isUsingItem() && using != null && !using.isEmpty()
            && (using.is(Items.BOW) || using.is(Items.CROSSBOW) || using.is(Items.TRIDENT))) {
            lastSwinging = player.swinging;
            lastSwingTime = player.swingTime;
            return 0;
        }

        boolean swinging = player.swinging;
        int swingTime = player.swingTime;
        boolean swingStarted = swinging && (!lastSwinging || swingTime == 0 || swingTime < lastSwingTime);
        if (swingStarted) {
            swingBurstStartTick = nowTick;
        }

        lastSwinging = swinging;
        lastSwingTime = swingTime;

        return Math.round(evaluateSwingBurstValue(nowTick));
    }

    private static float evaluateSwingBurstValue(float nowTick) {
        if (swingBurstStartTick < 0.0f) return 0.0f;
        float elapsed = nowTick - swingBurstStartTick;
        if (elapsed <= 0.0f) return SWING_BURST_MAX_GAP;
        if (elapsed >= SWING_BURST_FALL_TICKS) return 0.0f;
        return SWING_BURST_MAX_GAP * (1.0f - (elapsed / SWING_BURST_FALL_TICKS));
    }

    private static void drawPaintedCrosshair(GuiGraphics g, int screenCx, int screenCy, int staticGap, int animatedGap, boolean chroma) {
        final int size = 31;
        BitSet staticLayer = decodeLayer(ConfigManager.crosshairStaticLayer, size);
        BitSet activeLayer = decodeLayer(ConfigManager.crosshairActiveLayer, size);

        int center = size / 2;
        long nowMs = System.currentTimeMillis();

        drawLayer(g, screenCx, screenCy, size, center, staticLayer, staticGap, chroma, nowMs, true);
        drawLayer(g, screenCx, screenCy, size, center, activeLayer, animatedGap, chroma, nowMs, false);
    }

    private static void drawLayer(GuiGraphics g, int screenCx, int screenCy, int size, int center, BitSet bits,
                                  int gap, boolean chroma, long nowMs, boolean isStaticLayer) {
        if (bits.isEmpty()) return;

        for (int idx = bits.nextSetBit(0); idx >= 0; idx = bits.nextSetBit(idx + 1)) {
            int x = idx % size;
            int y = idx / size;
            int dx = x - center;
            int dy = y - center;

            int outDx = dx;
            int outDy = dy;
            if (!isStaticLayer) {
                int extra = Math.max(0, gap - 3);
                if (extra > 0 && (dx != 0 || dy != 0)) {
                    double len = Math.sqrt((double) dx * dx + (double) dy * dy);
                    double nx = dx / len;
                    double ny = dy / len;
                    outDx = (int) Math.round(dx + nx * extra);
                    outDy = (int) Math.round(dy + ny * extra);
                }
            }

            int px = screenCx + outDx;
            int py = screenCy + outDy;

            if (!chroma) {
                g.blit(RenderPipelines.GUI_INVERT, WHITE_1PX, px, py, 0, 0, 1, 1, 1, 1, 0xFFFFFFFF);
            } else {
                int rgb = chromaColor(nowMs, idx, bits.length());
                int argb = 0xFF000000 | (rgb & 0xFFFFFF);
                g.fill(px, py, px + 1, py + 1, argb);
            }
        }
    }

    private static int chromaColor(long nowMs, int idx, int len) {
        double lightness = Math.max(0.2, Math.min(1.0, ConfigManager.nickTweaksChromaLightness));
        double chroma = Math.max(0.0, Math.min(0.4, ConfigManager.nickTweaksChromaChroma));
        double size = Math.max(0.1, ConfigManager.nickTweaksChromaSize);
        double speed = Math.max(0.0, Math.min(8.0, ConfigManager.nickTweaksChromaSpeed));
        double phase = (nowMs / 1000.0) * (speed * 0.5);

        double progress = len <= 1 ? 0.0 : (double) idx / (double) (len - 1);
        float saturation = (float) (chroma / 0.4);
        float hue = (float) positiveModulo((progress / size) - phase, 1.0);
        return net.minecraft.util.Mth.hsvToRgb(hue, saturation, (float) lightness);
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }

    private static BitSet decodeLayer(String encoded, int size) {
        BitSet out = new BitSet(size * size);
        if (encoded == null || encoded.isBlank()) return out;
        int total = size * size;
        int idx = 0;
        for (int i = 0; i < encoded.length() && idx < total; i++) {
            int v = decode6(encoded.charAt(i));
            if (v < 0) continue;
            for (int b = 0; b < 6 && idx < total; b++) {
                if (((v >> b) & 1) != 0) out.set(idx);
                idx++;
            }
        }
        return out;
    }

    private static int decode6(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return 26 + (c - 'a');
        if (c >= '0' && c <= '9') return 52 + (c - '0');
        if (c == '-') return 62;
        if (c == '_') return 63;
        return -1;
    }
}
