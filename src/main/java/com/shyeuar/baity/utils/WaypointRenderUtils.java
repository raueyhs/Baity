package com.shyeuar.baity.utils;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class WaypointRenderUtils {

    public static final int LABEL_LIGHT = 15728880;
    public static final float DEFAULT_BOX_LINE_ALPHA = 0.9f;

    private static final float LABEL_SCALE_PER_BLOCK = 0.0075f;
    private static final float LABEL_MIN_SCALE = 0.05f;

    private static final Minecraft MC = Minecraft.getInstance();

    private static final RenderPipeline BAITY_WAYPOINT_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/baity_waypoint_lines")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .build()
    );

    private static final RenderType THROUGH_WALLS_LINE = RenderType.create(
            "baity_waypoint_lines",
            RenderSetup.builder(BAITY_WAYPOINT_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    private WaypointRenderUtils() {
    }

    public static void submitBox(PoseStack matrices, SubmitNodeCollector submits, AABB box, Vec3 cameraPos, int rgb) {
        submitBox(matrices, submits, box, cameraPos, rgb, DEFAULT_BOX_LINE_ALPHA);
    }

    public static void submitBox(PoseStack matrices, SubmitNodeCollector submits, AABB box, Vec3 cameraPos, int rgb,
                                 float lineAlpha) {
        float r = ARGB.red(rgb) / 255.0f;
        float g = ARGB.green(rgb) / 255.0f;
        float b = ARGB.blue(rgb) / 255.0f;
        submits.submitCustomGeometry(
                matrices,
                THROUGH_WALLS_LINE,
                (pose, lines) -> EntityDrawUtils.drawWireBoxAtWorld(pose, lines, box, cameraPos, r, g, b, lineAlpha)
        );
    }

    public static void submitLabel(PoseStack matrices, SubmitNodeCollector submits, Vec3 cameraPos, Vec3 worldPos,
                                   List<TextSegment> segments, double distance,
                                   float cameraYaw, float cameraPitch) {
        if (segments.isEmpty()) {
            return;
        }
        int totalWidth = 0;
        for (TextSegment segment : segments) {
            totalWidth += MC.font.width(segment.text());
        }
        float scale = labelScale(distance);
        float x = -totalWidth / 2.0f;

        matrices.pushPose();
        try {
            matrices.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
            matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.mulPose(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            matrices.scale(-scale, -scale, scale);

            for (TextSegment segment : segments) {
                FloatingWorldTextCompat.drawInBatch(MC.font, segment.text(), x, 0, segment.color(),
                        matrices, submits, LABEL_LIGHT, true);
                x += MC.font.width(segment.text());
            }
        } finally {
            matrices.popPose();
        }
    }

    public static float labelScale(double distance) {
        return Math.max(LABEL_MIN_SCALE, (float) (distance * LABEL_SCALE_PER_BLOCK));
    }

    public record TextSegment(String text, int color) {
    }
}
