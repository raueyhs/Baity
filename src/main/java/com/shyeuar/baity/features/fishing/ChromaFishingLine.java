package com.shyeuar.baity.features.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.mixin.accessor.FishingHookRendererInvoker;
import com.shyeuar.baity.utils.ColorGradientUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class ChromaFishingLine implements WorldRenderEvents.AfterEntities {
    private static final Minecraft MC = Minecraft.getInstance();
    private static volatile LocalLine localLine;
    private static volatile float appropriateLineWidth = 1.0f;

    private ChromaFishingLine() {}

    public record LocalLine(int hookId, Vec3 lineOriginOffset, double renderX, double renderY, double renderZ) {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(new ChromaFishingLine());
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> localLine = null);
    }

    public static boolean isEnabled() {
        return ConfigManager.chromaFishingLineEnabled;
    }

    public static void setLocalLine(int hookId, Vec3 lineOriginOffset, double renderX, double renderY, double renderZ) {
        localLine = new LocalLine(hookId, lineOriginOffset, renderX, renderY, renderZ);
    }

    public static LocalLine getLocalLine() {
        return localLine;
    }

    public static void setAppropriateLineWidth(float lineWidth) {
        appropriateLineWidth = lineWidth;
    }

    public static boolean matchesLocalOrigin(float x, float y, float z) {
        LocalLine line = localLine;
        if (line == null) {
            return false;
        }
        Vec3 origin = line.lineOriginOffset();
        return Math.abs(origin.x - x) < 0.05F
                && Math.abs(origin.y - y) < 0.05F
                && Math.abs(origin.z - z) < 0.05F;
    }

    public static int resolveColorArgb(float progress) {
        return resolveColorArgb(progress, System.currentTimeMillis());
    }

    public static int resolveColorArgb(float progress, long nowMs) {
        if (!isEnabled()) {
            return 0xFF000000;
        }
        float rodToHook = rodToHookProgress(progress);
        int rgb;
        if (ConfigManager.chromaFishingLineChromaEnabled) {
            rgb = chromaRgb(rodToHook, nowMs);
        } else {
            int start = ConfigManager.chromaFishingLineGradientStart & 0xFFFFFF;
            int end = ConfigManager.chromaFishingLineGradientEnd & 0xFFFFFF;
            rgb = start == end ? start : ColorGradientUtils.blendColors(start, end, rodToHook);
        }
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static int previewColorArgb(float progress, int gradientStart, int gradientEnd, long nowMs) {
        float rodToHook = rodToHookProgress(progress);
        if (ConfigManager.chromaFishingLineChromaEnabled) {
            return 0xFF000000 | (chromaRgb(rodToHook, nowMs) & 0xFFFFFF);
        }
        int start = gradientStart & 0xFFFFFF;
        int end = gradientEnd & 0xFFFFFF;
        if (start == end) {
            return 0xFF000000 | start;
        }
        return 0xFF000000 | (ColorGradientUtils.blendColors(start, end, rodToHook) & 0xFFFFFF);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        if (!isEnabled() || MC.level == null || MC.player == null) {
            return;
        }

        PoseStack matrices = context.matrices();
        MultiBufferSource buffers = context.consumers();
        if (matrices == null || buffers == null) {
            return;
        }

        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        float lineWidth = appropriateLineWidth;
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.lines());
        boolean drew = false;

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (!(entity instanceof FishingHook hook) || hook.getOwner() != MC.player) {
                continue;
            }

            LocalLine cached = localLine;
            Vec3 origin;
            double renderX;
            double renderY;
            double renderZ;

            if (cached != null && cached.hookId() == hook.getId()) {
                origin = cached.lineOriginOffset();
                renderX = cached.renderX();
                renderY = cached.renderY();
                renderZ = cached.renderZ();
            } else {
                var renderer = MC.getEntityRenderDispatcher().getRenderer(hook);
                if (!(renderer instanceof FishingHookRendererInvoker invoker)) {
                    continue;
                }
                Player owner = hook.getPlayerOwner();
                if (owner == null) {
                    continue;
                }
                float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                float swing = owner.getAttackAnim(partialTick);
                Vec3 hand = invoker.baity$invokeGetPlayerHandPos(owner, swing, partialTick);
                Vec3 hookPos = hook.getPosition(partialTick).add(0.0, 0.25, 0.0);
                origin = hand.subtract(hookPos);
                Vec3 renderPos = hook.getPosition(partialTick);
                renderX = renderPos.x;
                renderY = renderPos.y;
                renderZ = renderPos.z;
            }

            matrices.pushPose();
            matrices.translate(renderX - cameraPos.x, renderY - cameraPos.y, renderZ - cameraPos.z);
            renderColoredLine(
                    matrices.last(),
                    consumer,
                    (float) origin.x,
                    (float) origin.y,
                    (float) origin.z,
                    lineWidth
            );
            matrices.popPose();
            drew = true;
        }

        if (drew && buffers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(RenderTypes.lines());
        }
    }

    private static void renderColoredLine(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float lineWidth
    ) {
        for (int i = 0; i < 16; i++) {
            float segmentStart = i / 16.0f;
            float segmentEnd = (i + 1) / 16.0f;
            stringVertex(buffer, pose, x, y, z, segmentStart, segmentEnd, lineWidth);
            stringVertex(buffer, pose, x, y, z, segmentEnd, segmentStart, lineWidth);
        }
    }

    private static void stringVertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float segmentStart,
            float segmentEnd,
            float lineWidth
    ) {
        float startX = x * segmentStart;
        float startY = y * (segmentStart * segmentStart + segmentStart) * 0.5F + 0.25F;
        float startZ = z * segmentStart;
        float deltaX = x * segmentEnd - startX;
        float deltaY = y * (segmentEnd * segmentEnd + segmentEnd) * 0.5F + 0.25F - startY;
        float deltaZ = z * segmentEnd - startZ;
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        float normalX = deltaX / length;
        float normalY = deltaY / length;
        float normalZ = deltaZ / length;

        int argb = resolveColorArgb(segmentStart);
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        buffer.addVertex(pose, startX, startY, startZ)
                .setColor(r, g, b, a)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(lineWidth);
    }

    private static float rodToHookProgress(float segmentProgress) {
        return 1.0f - Mth.clamp(segmentProgress, 0f, 1f);
    }

    private static int chromaRgb(float rodToHookProgress, long nowMs) {
        double lightness = Mth.clamp(ConfigManager.chromaFishingLineChromaLightness, 0.2, 1.0);
        double chroma = Mth.clamp(ConfigManager.chromaFishingLineChromaChroma, 0.0, 0.4);
        double size = Math.max(0.1, ConfigManager.chromaFishingLineChromaSize);
        double speed = Mth.clamp(ConfigManager.chromaFishingLineChromaSpeed, 0.0, 8.0);
        double phase = (nowMs / 1000.0) * (speed * 0.5);
        float lineProgress = ConfigManager.chromaFishingLineChromaReverseDirection
                ? 1.0f - rodToHookProgress
                : rodToHookProgress;
        float saturation = (float) (chroma / 0.4);
        float hue = (float) positiveModulo((lineProgress / size) - phase, 1.0);
        return Mth.hsvToRgb(hue, saturation, (float) lightness);
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }
}
