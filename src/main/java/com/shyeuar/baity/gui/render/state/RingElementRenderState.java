package com.shyeuar.baity.gui.render.state;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

public record RingElementRenderState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        ScreenRectangle scissorArea,
        int centerX,
        int centerY,
        double angleFrom,
        double angleTo,
        int segments,
        double innerRadius,
        double outerRadius,
        int innerColor,
        int outerColor
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer consumer) {
        double step = Math.toRadians(angleTo - angleFrom) / segments;
        for (int i = 0; i <= segments; i++) {
            double rad = Math.toRadians(angleFrom) + i * step;
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            consumer.addVertexWith2DPose(pose,
                    (float) (centerX - cos * outerRadius),
                    (float) (centerY - sin * outerRadius)).setColor(outerColor);
            consumer.addVertexWith2DPose(pose,
                    (float) (centerX - cos * innerRadius),
                    (float) (centerY - sin * innerRadius)).setColor(innerColor);
        }
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle bounds() {
        int half = (int) Math.ceil(outerRadius * 2.0);
        ScreenRectangle rect = new ScreenRectangle(
                new ScreenPosition((int) (centerX - outerRadius), (int) (centerY - outerRadius)),
                half,
                half
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }
}
