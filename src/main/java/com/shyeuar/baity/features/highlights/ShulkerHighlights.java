package com.shyeuar.baity.features.highlights;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.OptionalDouble;

@Environment(EnvType.CLIENT)
public class ShulkerHighlights implements WorldRenderEvents.AfterEntities {

    private static final Minecraft MC = Minecraft.getInstance();
 
    private static RenderType createNoDepthLines(float lineWidth) {
  
        RenderPipeline noDepthPipeline = RenderPipeline.builder(new com.mojang.blaze3d.pipeline.RenderPipeline.Snippet[]{RenderPipelines.LINES_SNIPPET})
                .withLocation("pipeline/baity_shulker_lines_pipeline")
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build();

        return RenderType.create(
            "baity_shulker_lines",
            1536,
            false,
            false,
            noDepthPipeline,
            RenderType.CompositeState.builder()
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(lineWidth)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(false)
        );
    }
    
    private static final RenderType NO_DEPTH_LINES = createNoDepthLines(3.5f);

    @Override
    public void afterEntities(WorldRenderContext context) {
        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.highlightsShulkerEnabled) return;
        if (MC.level == null || MC.player == null) return;
        if (!isInGalatea()) return;

        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        MultiBufferSource buffers = context.consumers();
        if (matrices == null || buffers == null) return;

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (!(entity instanceof Shulker shulker)) continue;
            if (!shulker.isAlive()) continue;

            AABB box = shulker.getBoundingBox().inflate(0.01);

            double x1 = box.minX - cameraPos.x;
            double y1 = box.minY - cameraPos.y;
            double z1 = box.minZ - cameraPos.z;
            double x2 = box.maxX - cameraPos.x;
            double y2 = box.maxY - cameraPos.y;
            double z2 = box.maxZ - cameraPos.z;

            float r = 0.7f;
            float g = 1.0f;
            float b = 0.0f;
            float a = 0.9f;

            VertexConsumer lines = buffers.getBuffer(NO_DEPTH_LINES);

            matrices.pushPose();
            PoseStack.Pose pose = matrices.last();

            drawLine(pose, lines, x1, y1, z1, x2, y1, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z1, x2, y1, z2, r, g, b, a);
            drawLine(pose, lines, x2, y1, z2, x1, y1, z2, r, g, b, a);
            drawLine(pose, lines, x1, y1, z2, x1, y1, z1, r, g, b, a);
            
            drawLine(pose, lines, x1, y2, z1, x2, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y2, z1, x2, y2, z2, r, g, b, a);
            drawLine(pose, lines, x2, y2, z2, x1, y2, z2, r, g, b, a);
            drawLine(pose, lines, x1, y2, z2, x1, y2, z1, r, g, b, a);
            
            drawLine(pose, lines, x1, y1, z1, x1, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z1, x2, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z2, x2, y2, z2, r, g, b, a);
            drawLine(pose, lines, x1, y1, z2, x1, y2, z2, r, g, b, a);
            
            drawLine(pose, lines, x1, y1, z1, x2, y1, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z1, x2, y1, z2, r, g, b, a);
            drawLine(pose, lines, x2, y1, z2, x1, y1, z2, r, g, b, a);
            drawLine(pose, lines, x1, y1, z2, x1, y1, z1, r, g, b, a);
            drawLine(pose, lines, x1, y2, z1, x2, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y2, z1, x2, y2, z2, r, g, b, a);
            drawLine(pose, lines, x2, y2, z2, x1, y2, z2, r, g, b, a);
            drawLine(pose, lines, x1, y2, z2, x1, y2, z1, r, g, b, a);

            matrices.popPose();
        }
    }

    private static boolean isInGalatea() {
        try {
            if (MC.getConnection() != null) {
                for (var entry : MC.getConnection().getOnlinePlayers()) {
                    String line = removeColorCodes(entry.getTabListDisplayName() != null
                        ? entry.getTabListDisplayName().getString()
                        : entry.getProfile().name());
                    if (line == null || line.isBlank()) continue;
                    String lower = line.toLowerCase(java.util.Locale.ROOT);
                    if ((lower.contains("area:") || lower.contains("island:")) && lower.contains("galatea")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            var level = MC.level;
            if (level == null || level.getScoreboard() == null) return false;
            var scoreboard = level.getScoreboard();
            var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) return false;
            for (var holder : tryGetSortedScores(scoreboard, objective)) {
                String line = removeColorCodes(extractScoreOwnerText(holder));
                if (line != null && line.toLowerCase(java.util.Locale.ROOT).contains("galatea")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String removeColorCodes(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)[\\u00A7&][0-9A-FK-OR]", "").trim();
    }

    private static java.util.List<?> tryGetSortedScores(Object scoreboard, Object objective) {
        try {
            Method m = scoreboard.getClass().getMethod("listPlayerScores", objective.getClass());
            Object res = m.invoke(scoreboard, objective);
            if (res instanceof java.util.List<?>) return (java.util.List<?>) res;
        } catch (Exception ignored) {
        }
        return java.util.Collections.emptyList();
    }

    private static String extractScoreOwnerText(Object holder) {
        try {
            Method owner = holder.getClass().getMethod("owner");
            Object ownerObj = owner.invoke(holder);
            if (ownerObj == null) return "";
            try {
                Method name = ownerObj.getClass().getMethod("getName");
                Object n = name.invoke(ownerObj);
                return n == null ? "" : n.toString();
            } catch (Exception ignored) {
            }
            return ownerObj.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer vc,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0.001) {
            float nx = (float) (dx / length);
            float ny = (float) (dy / length);
            float nz = (float) (dz / length);
            vc.addVertex(pose.pose(), (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz);
            vc.addVertex(pose.pose(), (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz);
        }
    }
}


