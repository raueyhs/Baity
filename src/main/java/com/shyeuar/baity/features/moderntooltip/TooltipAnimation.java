package com.shyeuar.baity.features.moderntooltip;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.animation.TooltipSizeAnimator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.joml.Vector2ic;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class TooltipAnimation {

    private static final ThreadLocal<AnimationFrame> CURRENT_FRAME = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CLIP_PUSHED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Font> PASS_FONT = new ThreadLocal<>();
    private static final ThreadLocal<List<ClientTooltipComponent>> PASS_COMPONENTS = new ThreadLocal<>();

    private static final TooltipSizeAnimator SIZE_ANIMATOR = new TooltipSizeAnimator();

    private static int tooltipsRenderedThisFrame = 0;
    private static int tooltipsRenderedThisPass = 0;
    private static boolean resetAnimatorNextPass = false;

    private TooltipAnimation() {
    }

    public static boolean isActive() {
        return ModernTooltip.isModuleActive() && ConfigManager.smoothTooltipEnabled;
    }

    public static void beginTooltipPass(Font font, List<ClientTooltipComponent> components) {
        PASS_FONT.set(font);
        PASS_COMPONENTS.set(components);
    }

    public static void endTooltipPass() {
        CLIP_PUSHED.set(false);
        PASS_FONT.remove();
        PASS_COMPONENTS.remove();
        CURRENT_FRAME.remove();
    }

    public static Vector2ic smoothPosition(Vector2ic raw, int tooltipWidth, int tooltipHeight) {
        Font font = PASS_FONT.get();
        List<ClientTooltipComponent> components = PASS_COMPONENTS.get();
        if (font == null || components == null || components.isEmpty()) {
            return raw;
        }

        if (prepare(font, components, raw.x(), raw.y(), tooltipWidth, tooltipHeight) == null) {
            return raw;
        }
        AnimationFrame frame = CURRENT_FRAME.get();
        if (frame == null) {
            return raw;
        }
        return new org.joml.Vector2i(frame.renderX(), frame.renderY());
    }

    public static int getAnimatedContentWidth(int targetWidth) {
        AnimationFrame frame = CURRENT_FRAME.get();
        if (frame == null || !frame.animateBackground()) {
            return targetWidth;
        }
        return Math.max(1, Math.round(frame.animatedWidth()));
    }

    public static int getAnimatedContentHeight(int targetHeight) {
        AnimationFrame frame = CURRENT_FRAME.get();
        if (frame == null || !frame.animateBackground()) {
            return targetHeight;
        }
        return Math.max(1, Math.round(frame.animatedHeight()));
    }

    public static AnimatedBox prepareAnimatedBox(
            Font font,
            List<ClientTooltipComponent> components,
            int targetWidth,
            int targetHeight,
            int targetX,
            int targetY
    ) {
        if (!isActive()) {
            return AnimatedBox.immediate(targetWidth, targetHeight, targetX, targetY);
        }

        if (resetAnimatorNextPass) {
            resetAnimatorNextPass = false;
            SIZE_ANIMATOR.invalidate();
        }

        if (tooltipsRenderedThisPass > 0) {
            return AnimatedBox.immediate(targetWidth, targetHeight, targetX, targetY);
        }

        int signature = computeSignature(font, components);
        TooltipSizeAnimator.Frame frame = updateAnimatedFrame(signature, targetWidth, targetHeight, targetX, targetY);
        tooltipsRenderedThisPass++;
        tooltipsRenderedThisFrame++;
        return new AnimatedBox(
                frame.animatedWidth(),
                frame.animatedHeight(),
                Math.round(frame.animatedX()),
                Math.round(frame.animatedY()),
                targetX,
                targetY,
                frame.animateBackground(),
                frame.needsTextClip()
        );
    }

    private static TooltipSizeAnimator.Frame updateAnimatedFrame(
            int signature,
            float targetWidth,
            float targetHeight,
            int targetX,
            int targetY
    ) {
        return SIZE_ANIMATOR.update(signature, targetWidth, targetHeight, targetX, targetY);
    }

    private static AnimationFrame prepare(
            Font font,
            List<ClientTooltipComponent> components,
            int targetX,
            int targetY,
            int targetWidth,
            int targetHeight
    ) {
        if (!isActive()) {
            CURRENT_FRAME.remove();
            return null;
        }

        if (resetAnimatorNextPass) {
            resetAnimatorNextPass = false;
            SIZE_ANIMATOR.invalidate();
        }

        if (tooltipsRenderedThisPass > 0) {
            CURRENT_FRAME.remove();
            return null;
        }

        int signature = computeSignature(font, components);
        TooltipSizeAnimator.Frame frame = updateAnimatedFrame(signature, targetWidth, targetHeight, targetX, targetY);

        AnimationFrame result = new AnimationFrame(
                Math.round(frame.animatedX()),
                Math.round(frame.animatedY()),
                frame.animatedWidth(),
                frame.animatedHeight(),
                frame.animateBackground(),
                frame.needsTextClip()
        );
        CURRENT_FRAME.set(result);
        tooltipsRenderedThisPass++;
        tooltipsRenderedThisFrame++;
        return result;
    }

    public static void pushTextClip(GuiGraphicsExtractor graphics) {
        if (CLIP_PUSHED.get()) {
            return;
        }

        AnimationFrame frame = CURRENT_FRAME.get();
        if (frame == null || !frame.needsTextClip()) {
            return;
        }

        int x1 = frame.renderX();
        int y1 = frame.renderY();
        int x2 = x1 + Math.max(1, Math.round(frame.animatedWidth()));
        int y2 = y1 + Math.max(1, Math.round(frame.animatedHeight()));
        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        graphics.nextStratum();
        graphics.enableScissor(x1, y1, x2, y2);
        CLIP_PUSHED.set(true);
    }

    public static void popTextClip(GuiGraphicsExtractor graphics) {
        if (!CLIP_PUSHED.get()) {
            return;
        }

        CLIP_PUSHED.set(false);
        graphics.disableScissor();
    }

    public static void onRenderPassBegin() {
        tooltipsRenderedThisPass = 0;
    }

    public static void onRenderPassEnd() {
        if (tooltipsRenderedThisPass > 1) {
            resetAnimatorNextPass = true;
        }
    }

    public static void onScreenRenderEnd() {
        SIZE_ANIMATOR.endFrame(tooltipsRenderedThisFrame > 0);
        tooltipsRenderedThisFrame = 0;
    }

    public static void reset() {
        SIZE_ANIMATOR.reset();
        tooltipsRenderedThisPass = 0;
        tooltipsRenderedThisFrame = 0;
        resetAnimatorNextPass = false;
        CURRENT_FRAME.remove();
        PASS_FONT.remove();
        PASS_COMPONENTS.remove();
        CLIP_PUSHED.set(false);
    }

    private static int computeSignature(Font font, List<ClientTooltipComponent> components) {
        int signature = components.size();
        for (ClientTooltipComponent component : components) {
            signature = 31 * signature + component.getWidth(font);
            signature = 31 * signature + component.getHeight(font);
        }
        return signature;
    }

    public record AnimationFrame(
            int renderX,
            int renderY,
            float animatedWidth,
            float animatedHeight,
            boolean animateBackground,
            boolean needsTextClip
    ) {
    }

    public record AnimatedBox(
            float animatedWidth,
            float animatedHeight,
            int renderX,
            int renderY,
            int targetX,
            int targetY,
            boolean animateBackground,
            boolean needsTextClip
    ) {
        public static AnimatedBox immediate(int width, int height, int targetX, int targetY) {
            return new AnimatedBox(width, height, targetX, targetY, targetX, targetY, false, false);
        }

        public int positionOffsetX() {
            return renderX - targetX;
        }

        public int positionOffsetY() {
            return renderY - targetY;
        }

        public int drawWidth() {
            return Math.max(1, Math.round(animatedWidth));
        }

        public int drawHeight() {
            return Math.max(1, Math.round(animatedHeight));
        }
    }
}
