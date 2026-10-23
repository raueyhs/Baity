package com.shyeuar.baity.features.highlights;

import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.EntityDrawUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class ShulkerHighlights implements LevelRenderEvents.AfterSolidFeatures {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final RenderPipeline BAITY_SHULKER_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/baity_shulker_lines")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .build()
    );

    private static final RenderType NO_DEPTH_LINES = RenderType.create(
            "baity_shulker_lines",
            RenderSetup.builder(BAITY_SHULKER_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup()
    );

    @Override
    public void afterSolidFeatures(LevelRenderContext context) {
        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.highlightsShulkerEnabled) return;
        if (MC.level == null || MC.player == null) return;

        boolean torrhus = LocateUtils.isTorrhusCanyon(MC);
        boolean galatea = LocateUtils.isGalatea(MC);
        if (!torrhus && !galatea) return;

        float r;
        float g;
        float b;
        if (torrhus) {
            r = 1.0f;
            g = 0.78f;
            b = 0.08f;
        } else {
            r = 0.7f;
            g = 1.0f;
            b = 0.0f;
        }
        float a = 0.9f;

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();
        var submits = context.submitNodeCollector();
        if (matrices == null || submits == null) return;

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (!(entity instanceof Shulker shulker)) continue;
            if (!shulker.isAlive()) continue;

            AABB box = shulker.getBoundingBox().inflate(0.01);
            submits.submitCustomGeometry(
                    matrices,
                    NO_DEPTH_LINES,
                    (pose, lines) -> EntityDrawUtils.drawWireBoxAtWorld(pose, lines, box, cameraPos, r, g, b, a)
            );
        }
    }
}
