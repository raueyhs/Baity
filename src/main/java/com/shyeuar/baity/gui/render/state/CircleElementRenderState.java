package com.shyeuar.baity.gui.render.state;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

public record CircleElementRenderState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        ScreenRectangle scissorArea,
        int centerX,
        int centerY,
        double angleFrom,
        double angleTo,
        int segments,
        double radius,
        int color
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer consumer) {
        double step = Math.toRadians(angleTo - angleFrom) / segments;
        consumer.addVertexWith2DPose(pose, centerX, centerY).setColor(color);
        for (int i = segments; i >= 0; i--) {
            double rad = Math.toRadians(angleFrom) + i * step;
            float x = (float) (centerX - Math.cos(rad) * radius);
            float y = (float) (centerY - Math.sin(rad) * radius);
            consumer.addVertexWith2DPose(pose, x, y).setColor(color);
        }
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle bounds() {
        int half = (int) Math.ceil(radius * 2.0);
        ScreenRectangle rect = new ScreenRectangle(
                new ScreenPosition((int) (centerX - radius), (int) (centerY - radius)),
                half,
                half
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }
}
