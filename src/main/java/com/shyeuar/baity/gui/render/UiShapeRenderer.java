package com.shyeuar.baity.gui.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.shyeuar.baity.gui.render.state.CircleElementRenderState;
import com.shyeuar.baity.gui.render.state.RingElementRenderState;
import com.shyeuar.baity.mixin.accessor.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

public final class UiShapeRenderer {

    private static RenderPipeline circlePipeline() {
        return BaityUiPipelines.GUI_TRIANGLE_FAN;
    }

    private static RenderPipeline ringPipeline() {
        return BaityUiPipelines.GUI_TRIANGLE_STRIP;
    }

    private UiShapeRenderer() {
    }

    public static void drawCircle(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                  int segments, double radius, int color) {
        drawCircle(graphics, centerX, centerY, 0, 360, segments, radius, color);
    }

    public static void drawCircle(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                  double angleFrom, double angleTo, int segments, double radius, int color) {
        drawCircle(graphics, circlePipeline(), centerX, centerY, angleFrom, angleTo, segments, radius, color);
    }

    public static void drawCircle(GuiGraphicsExtractor graphics, RenderPipeline pipeline,
                                  int centerX, int centerY, double angleFrom, double angleTo,
                                  int segments, double radius, int color) {
        if (angleFrom >= angleTo || segments <= 0) {
            return;
        }
        GuiGraphicsExtractorAccessor accessor = (GuiGraphicsExtractorAccessor) graphics;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = accessor.baity$getScissorStack().peek();
        accessor.baity$getGuiRenderState().addGuiElement(
                new CircleElementRenderState(pipeline, pose, scissor,
                        centerX, centerY, angleFrom, angleTo, segments, radius, color));
    }

    public static void drawRing(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                double angleFrom, double angleTo, int segments,
                                double innerRadius, double outerRadius,
                                int innerColor, int outerColor) {
        drawRing(graphics, ringPipeline(), centerX, centerY, angleFrom, angleTo, segments,
                innerRadius, outerRadius, innerColor, outerColor);
    }

    public static void drawRing(GuiGraphicsExtractor graphics, RenderPipeline pipeline,
                                int centerX, int centerY, double angleFrom, double angleTo,
                                int segments, double innerRadius, double outerRadius,
                                int innerColor, int outerColor) {
        if (angleFrom >= angleTo || innerRadius >= outerRadius || segments <= 0) {
            return;
        }
        GuiGraphicsExtractorAccessor accessor = (GuiGraphicsExtractorAccessor) graphics;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = accessor.baity$getScissorStack().peek();
        accessor.baity$getGuiRenderState().addGuiElement(
                new RingElementRenderState(pipeline, pose, scissor,
                        centerX, centerY, angleFrom, angleTo, segments,
                        innerRadius, outerRadius, innerColor, outerColor));
    }
}
