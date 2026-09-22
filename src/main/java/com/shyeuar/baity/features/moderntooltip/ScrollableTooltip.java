package com.shyeuar.baity.features.moderntooltip;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.joml.Vector2ic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class ScrollableTooltip {

    static final int SCREEN_EDGE_MARGIN = 12;
    private static final int BOX_PADDING = 4;
    private static final double SCROLL_SPEED = 8.0;

    private static final Map<Integer, ScrollState> scrollStateBySignature = new HashMap<>();

    private static double scrollX;
    private static double scrollY;
    private static double minScrollY;
    private static double maxScrollY;
    private static double minScrollX;
    private static double maxScrollX;
    private static int contentSignature = Integer.MIN_VALUE;
    private static int visibleTick = -1;

    private ScrollableTooltip() {
    }

    public static boolean isActive() {
        return ModernTooltip.isModuleActive() && ConfigManager.scrollableTooltipEnabled;
    }

    public static void render(
            GuiGraphicsExtractor graphics,
            Font font,
            List<ClientTooltipComponent> components,
            int mouseX,
            int mouseY,
            ClientTooltipPositioner positioner,
            Identifier texture
    ) {
        if (components.isEmpty()) {
            return;
        }

        onBeforeRender(font, components);

        int contentWidth = 0;
        int contentHeight = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            contentWidth = Math.max(contentWidth, component.getWidth(font));
            contentHeight += component.getHeight(font);
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int edgeMargin = SCREEN_EDGE_MARGIN;

        Vector2ic targetPosition = positioner.positionTooltip(
                screenWidth,
                screenHeight,
                mouseX,
                mouseY,
                contentWidth,
                contentHeight
        );
        int targetTooltipX = targetPosition.x();
        int targetTooltipY = targetPosition.y();

        int targetBoxX = targetTooltipX - BOX_PADDING;
        int targetBoxY;
        int maxBoxHeight = screenHeight - edgeMargin * 2;
        if (contentHeight + 8 <= maxBoxHeight) {
            targetBoxY = Math.max(edgeMargin, Math.min(targetTooltipY - BOX_PADDING, screenHeight - edgeMargin - (contentHeight + 8)));
        } else {
            targetBoxY = edgeMargin;
        }

        int boxW = Math.min(contentWidth + 8, screenWidth - edgeMargin * 2);
        int boxH = Math.min(contentHeight + 8, screenHeight - edgeMargin - targetBoxY);
        int visibleContentW = Math.max(0, boxW - 8);
        int visibleContentH = Math.max(0, boxH - 8);

        setVerticalScrollBounds(contentHeight, visibleContentH);
        setHorizontalScrollBounds(targetBoxX, boxW, screenWidth);

        TooltipAnimation.AnimatedBox animatedBox = TooltipAnimation.prepareAnimatedBox(
                font,
                components,
                visibleContentW,
                visibleContentH,
                targetTooltipX,
                targetTooltipY
        );
        int drawContentW = animatedBox.drawWidth();
        int drawContentH = animatedBox.drawHeight();
        int drawBoxW = drawContentW + 8;
        int drawBoxH = drawContentH + 8;

        int textStartY = targetBoxY + BOX_PADDING + Math.min(0, getVerticalScrollOffset());
        int textX = targetTooltipX;

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(getHorizontalOffset(), 0.0f);
        pose.translate(animatedBox.positionOffsetX(), animatedBox.positionOffsetY());

        TooltipRenderUtil.extractTooltipBackground(graphics, targetBoxX, targetBoxY, drawBoxW, drawBoxH, texture);

        graphics.enableScissor(targetBoxX, targetBoxY, targetBoxX + drawBoxW, targetBoxY + drawBoxH);

        int drawY = textStartY;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.extractText(graphics, font, textX, drawY);
            drawY += component.getHeight(font) + (i == 0 ? 2 : 0);
        }

        drawY = textStartY;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.extractImage(font, textX, drawY, contentWidth, contentHeight, graphics);
            drawY += component.getHeight(font) + (i == 0 ? 2 : 0);
        }

        graphics.disableScissor();
        pose.popMatrix();

        if (ConfigManager.scrollableTooltipKeepScrollInGui) {
            persistScrollState(contentSignature);
        }
    }

    private static void onBeforeRender(Font font, List<ClientTooltipComponent> components) {
        int signature = computeSignature(font, components);
        if (signature != contentSignature) {
            if (ConfigManager.scrollableTooltipKeepScrollInGui) {
                persistScrollState(contentSignature);
                restoreScrollState(signature);
            } else {
                resetScrollOffsets();
            }
            contentSignature = signature;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.gui != null && client.gui.hud != null) {
            visibleTick = client.gui.hud.getGuiTicks();
        }
    }

    private static void persistScrollState(int signature) {
        if (signature == Integer.MIN_VALUE) {
            return;
        }
        scrollStateBySignature.put(signature, new ScrollState(scrollX, scrollY));
    }

    private static void restoreScrollState(int signature) {
        ScrollState saved = scrollStateBySignature.get(signature);
        if (saved == null) {
            scrollX = 0.0;
            scrollY = 0.0;
            return;
        }
        scrollX = saved.scrollX();
        scrollY = saved.scrollY();
    }

    private static void setVerticalScrollBounds(int contentHeight, int visibleHeight) {
        minScrollY = -Math.max(0, contentHeight - visibleHeight);
        maxScrollY = 0.0;
        scrollY = clamp(scrollY, minScrollY, maxScrollY);
    }

    private static void setHorizontalScrollBounds(int boxX, int boxWidth, int screenWidth) {
        int rightEdge = boxX + boxWidth;
        int margin = SCREEN_EDGE_MARGIN;
        minScrollX = margin - boxX;
        maxScrollX = (screenWidth - margin) - rightEdge;
        if (minScrollX > maxScrollX) {
            double mid = (minScrollX + maxScrollX) * 0.5;
            minScrollX = mid;
            maxScrollX = mid;
        }
        scrollX = clamp(scrollX, minScrollX, maxScrollX);
    }

    public static int getVerticalScrollOffset() {
        return (int) scrollY;
    }

    public static float getHorizontalOffset() {
        return (float) scrollX;
    }

    public static void onMouseScroll(double verticalAmount) {
        if (!isActive() || !isTooltipVisible()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) {
            return;
        }

        long windowHandle = client.getWindow().handle();
        if (KeyMappingUtils.isKeyPressed(windowHandle, ConfigManager.scrollableTooltipHorizontalKey)) {
            scrollX = clamp(scrollX + verticalAmount * SCROLL_SPEED, minScrollX, maxScrollX);
        } else {
            double next = scrollY + verticalAmount * SCROLL_SPEED;
            next = clamp(next, minScrollY, maxScrollY);
            if (scrollY != 0.0 && (next > 0.0) != (scrollY > 0.0)) {
                scrollY = 0.0;
            } else {
                scrollY = next;
            }
        }

        if (ConfigManager.scrollableTooltipKeepScrollInGui) {
            persistScrollState(contentSignature);
        }
    }

    public static void reset() {
        resetScrollOffsets();
        scrollStateBySignature.clear();
        contentSignature = Integer.MIN_VALUE;
        visibleTick = -1;
    }

    private static void resetScrollOffsets() {
        scrollX = 0.0;
        scrollY = 0.0;
        minScrollY = 0.0;
        maxScrollY = 0.0;
        minScrollX = 0.0;
        maxScrollX = 0.0;
    }

    private static boolean isTooltipVisible() {
        Minecraft client = Minecraft.getInstance();
        return client.gui != null
                && client.gui.hud != null
                && visibleTick == client.gui.hud.getGuiTicks();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int computeSignature(Font font, List<ClientTooltipComponent> components) {
        int signature = components.size();
        for (ClientTooltipComponent component : components) {
            signature = 31 * signature + component.getWidth(font);
            signature = 31 * signature + component.getHeight(font);
        }
        return signature;
    }

    private record ScrollState(double scrollX, double scrollY) {
    }
}
