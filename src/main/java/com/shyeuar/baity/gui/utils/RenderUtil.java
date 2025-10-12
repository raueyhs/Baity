package com.shyeuar.baity.gui.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public class RenderUtil {
    
    public static void drawRoundedRect(DrawContext context, float x, float y, float x2, float y2, float radius, int color) {
        // 简化为普通矩形实现
        context.fill((int)x, (int)y, (int)x2, (int)y2, color);
    }

    public static void stroke1px(DrawContext context, float x, float y, float x2, float y2, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)(y + 1), color);
        context.fill((int)x, (int)(y2 - 1), (int)x2, (int)y2, color);
        context.fill((int)x, (int)y, (int)(x + 1), (int)y2, color);
        context.fill((int)(x2 - 1), (int)y, (int)x2, (int)y2, color);
    }

    public static void divider(DrawContext context, float x, float y, float x2, float y2, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)y2, color);
    }
    
    
    public static boolean isHovered(float x, float y, float x1, float y1, float mouseX, float mouseY) {
        return mouseX > x && mouseY > y && mouseX < x1 && mouseY < y1;
    }
}