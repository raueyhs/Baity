package com.shyeuar.baity.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;

// TODO(ModernUI-26.2): Revisit when ModernUI ships for 26.2. 26.1 used synchronous font.drawInBatch();
// 26.2 uses submitText() which renders later, so ModernUI hooks may need deferred-submit tracking.
public final class FloatingWorldTextCompat {

    private FloatingWorldTextCompat() {
    }

    public static boolean usesHookedFontEngine(Font font) {
        if (font == null) {
            return false;
        }
        return font.getSplitter().getClass() != StringSplitter.class;
    }

    public static void beginFrame() {
        RenderScope.enterFloatingWorldText();
    }

    public static void endFrame() {
        RenderScope.exitFloatingWorldText();
    }

    public static void drawInBatch(
            Font font,
            String text,
            float x,
            float y,
            int color,
            PoseStack poseStack,
            SubmitNodeCollector submits,
            int packedLight
    ) {
        drawInBatch(font, text, x, y, color, poseStack, submits, packedLight, false);
    }

    public static void drawInBatch(
            Font font,
            String text,
            float x,
            float y,
            int color,
            PoseStack poseStack,
            SubmitNodeCollector submits,
            int packedLight,
            boolean dropShadow
    ) {
        submits.submitText(
                poseStack,
                x,
                y,
                Component.literal(text).getVisualOrderText(),
                dropShadow,
                Font.DisplayMode.SEE_THROUGH,
                packedLight,
                color,
                0,
                0
        );
    }

    public static void drawInBatch(
            Font font,
            Component text,
            float x,
            float y,
            int color,
            PoseStack poseStack,
            SubmitNodeCollector submits,
            int packedLight
    ) {
        submits.submitText(
                poseStack,
                x,
                y,
                text.getVisualOrderText(),
                false,
                Font.DisplayMode.SEE_THROUGH,
                packedLight,
                color,
                0,
                0
        );
    }
}
