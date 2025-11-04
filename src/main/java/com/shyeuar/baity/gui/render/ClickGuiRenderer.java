package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Function;

public class ClickGuiRenderer {
    
    public static void render(DrawContext context, MinecraftClient client, 
                             ClickGuiState state, Theme theme,
                             Function<String, String> getTooltipText,
                             Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                             Function<Object, String> getDisplayTextFormatter,
                             ModuleStyleRenderer.TooltipInfo tooltipInfo,
                             double mouseX, double mouseY) {
        
        updateModuleExpandAnimations(state);
        
        state.setHoveredTooltip(null);
        state.setHoveredTooltipText(null);
        tooltipInfo.tooltip = null;
        tooltipInfo.tooltipText = null;
        
        float scaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        context.getMatrices().push();
        context.getMatrices().translate(state.getWindowX(), state.getWindowY(), 0);
        context.getMatrices().scale(scaleRatio, scaleRatio, 1.0f);
        
        renderWindowBackground(context, theme);
        
        renderCategoryBar(context, client, state, theme);
        
        float visibleTop = ClickGuiState.LIST_TOP_PADDING;
        float visibleBottom = ClickGuiState.HEIGHT - 20;
        float visibleHeight = Math.max(0, visibleBottom - visibleTop);
        float contentHeight = ClickGuiLayout.calculateContentHeight(state, visibleHeight);
        
        ClickGuiLayout.ScrollbarInfo scrollbarInfo = ClickGuiLayout.calculateScrollbar(state, contentHeight, visibleHeight);
        ClickGuiLayout.clampScrollOffset(state, scrollbarInfo.maxScroll);
        
        float modY = 60 - state.getScrollOffset();
        List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
        
        if (modules.isEmpty()) {
            renderPlaceholder(context, client, theme, modY);
            modY += 100;
        }
        
        context.enableScissor(0, (int)ClickGuiState.LIST_TOP_PADDING, 
                             (int)ClickGuiState.WIDTH, (int)(ClickGuiState.HEIGHT - 20));
        
        for (Module module : modules) {
            renderModule(context, client, module, theme, state, 
                        ClickGuiState.WIDTH - 20, modY,
                        coords.mouseX, coords.mouseY,
                        getTooltipText, getTooltipTextWithColors, tooltipInfo);
            
            if (tooltipInfo.tooltip != null) {
                state.setHoveredTooltip(tooltipInfo.tooltip);
                state.setHoveredTooltipText(tooltipInfo.tooltipText);
                state.setTooltipX(tooltipInfo.x);
                state.setTooltipY(tooltipInfo.y);
            }
            
            modY += 30;
            
            modY += renderSubOptions(context, client, module, theme, state,
                                   modY, visibleHeight,
                                   coords.mouseX, coords.mouseY,
                                   getTooltipText, getTooltipTextWithColors,
                                   getDisplayTextFormatter, tooltipInfo);
        }
        
        context.disableScissor();
        
        if (contentHeight > visibleHeight) {
            renderScrollbar(context, theme, scrollbarInfo);
        }
        
        renderWatermark(context, client, theme);
        
        context.getMatrices().pop();
        
        if (state.getHoveredTooltip() != null) {
            renderTooltip(context, client, theme, state, mouseX, mouseY);
        }
    }
    
    private static void renderWindowBackground(DrawContext context, Theme theme) {
        GuiRenderUtil.drawRoundedRect(context, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT, 
                                      6, theme.BG.getRGB());
        GuiRenderUtil.stroke1px(context, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT, 
                                new java.awt.Color(255, 255, 255, 20).getRGB());
    }
    
    private static void renderCategoryBar(DrawContext context, MinecraftClient client,
                                        ClickGuiState state, Theme theme) {
        float cateX = 20;
        float cateY = 30;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
             com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            boolean active = category == state.getSelectedCategory();
            String label = category.getDisplayName();
            int w = client.textRenderer.getWidth(label);
            int color = active ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
            context.drawText(client.textRenderer, label, (int)cateX, (int)cateY, color, false);
            
            if (active) {
                float textLeft = cateX;
                float textRight = cateX + w;
                float textCenterX = (textLeft + textRight) / 2f;
                float lineExtension = 6f;
                float lineLeft = textCenterX - w/2f - lineExtension;
                float lineRight = textCenterX + w/2f + lineExtension;
                GuiRenderUtil.divider(context, lineLeft, cateY + 12, lineRight, cateY + 13,
                                     new java.awt.Color(255, 255, 255, 64).getRGB());
            }
            cateX += w + 28;
        }
    }
    
    private static void renderPlaceholder(DrawContext context, MinecraftClient client,
                                         Theme theme, float modY) {
        String placeholderText = "not coming soon~~~";
        int textWidth = client.textRenderer.getWidth(placeholderText);
        int textX = (int)((ClickGuiState.WIDTH - textWidth) / 2);
        int textY = (int)(modY + 50);
        context.drawText(client.textRenderer, placeholderText, textX, textY, theme.FONT.getRGB(), false);
    }
    
    private static void renderModule(DrawContext context, MinecraftClient client,
                                   Module module, Theme theme, ClickGuiState state,
                                   float width, float modY,
                                   float mouseX, float mouseY,
                                   Function<String, String> getTooltipText,
                                   Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                   ModuleStyleRenderer.TooltipInfo tooltipInfo) {
        ModuleStyleRenderer.renderModule(context, client, module, theme,
                                        20, modY, width, 25,
                                        mouseX, mouseY,
                                        state.isListeningForKey(), state.getCurrentKeyDisplay(),
                                        getTooltipText, getTooltipTextWithColors, tooltipInfo);
    }
    
    private static float renderSubOptions(DrawContext context, MinecraftClient client,
                                         Module module, Theme theme, ClickGuiState state,
                                         float modY, float visibleHeight,
                                         float mouseX, float mouseY,
                                         Function<String, String> getTooltipText,
                                         Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                         Function<Object, String> getDisplayTextFormatter,
                                         ModuleStyleRenderer.TooltipInfo tooltipInfo) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return 0;
        
        float expandProgress = getModuleExpandProgress(state, module.getName());
        if (expandProgress <= 0.0f) return 0;
        
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight);
        int containerHeight = (int)(dims.height * expandProgress);

        int containerBg = new java.awt.Color(30, 30, 30, 200).getRGB();
        int containerX1 = 30;
        int containerY1 = (int)modY;
        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
        int containerY2 = (int)(modY + containerHeight);
        
        context.fill(containerX1, containerY1, containerX2, containerY2, containerBg);
        GuiRenderUtil.stroke1px(context, containerX1, containerY1, containerX2, containerY2,
                               new java.awt.Color(255, 255, 255, 40).getRGB());
        
        int innerVisible = Math.max(0, containerHeight - dims.padding * 2);
        if (innerVisible >= dims.subOptionHeight / 2) {
            float subModY = modY + dims.padding;
            int maxVisibleOptions = Math.max(0, innerVisible / dims.subOptionHeight);
            int renderedCount = 0;
            
            for (Value value : module.getValues()) {
                if ("enabled".equals(value.getName())) continue;
                if (renderedCount >= maxVisibleOptions) break;
                
                float localAlphaF = Math.min(1f, Math.max(0f, 
                    (innerVisible - renderedCount * dims.subOptionHeight) / (float)dims.subOptionHeight));
                int localAlpha = (int)(255 * expandProgress * localAlphaF);
                
                int subX1 = containerX1 + 4;
                int subX2 = containerX2 - 4;
                
                ValueStyleRenderer.renderValue(context, client, module, value, theme,
                                              subX1, subModY, subX2, dims.subOptionHeight,
                                              mouseX, mouseY, localAlpha,
                                              getTooltipText, getTooltipTextWithColors,
                                              getDisplayTextFormatter,
                                              state.getListeningButtonValueName(),
                                              tooltipInfo);
                
                if (tooltipInfo.tooltip != null) {
                    state.setHoveredTooltip(tooltipInfo.tooltip);
                    state.setHoveredTooltipText(tooltipInfo.tooltipText);
                    state.setTooltipX(tooltipInfo.x);
                    state.setTooltipY(tooltipInfo.y);
                }
                
                subModY += dims.subOptionHeight;
                renderedCount++;
            }
        }
        
        return containerHeight + 5;
    }
    
    private static void renderScrollbar(DrawContext context, Theme theme,
                                       ClickGuiLayout.ScrollbarInfo info) {
        float barX1 = ClickGuiState.WIDTH - 6;
        float barX2 = ClickGuiState.WIDTH - 2;
        GuiRenderUtil.drawRoundedRect(context, barX1, info.barY, barX2, 
                                     info.barY + info.barHeight, 2, theme.BG_2.getRGB());
    }
    
    private static void renderWatermark(DrawContext context, MinecraftClient client, Theme theme) {
        String watermark = "Baity by 11YearCookieBuff (AKA raueyhs , shyeuar)";
        int wmRawWidth = client.textRenderer.getWidth(watermark);
        float wmScale = 0.70f;
        float scaledWidth = wmScale * wmRawWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - 8;
        float baseY = 8;
        
        context.getMatrices().push();
        context.getMatrices().scale(wmScale, wmScale, 1f);
        int wmColor = new java.awt.Color(120, 124, 132).getRGB();
        context.drawText(client.textRenderer, watermark, 
                        (int)(baseX / wmScale), (int)(baseY / wmScale), wmColor, false);
        context.getMatrices().pop();
    }
    
    private static void renderTooltip(DrawContext context, MinecraftClient client,
                                     Theme theme, ClickGuiState state,
                                     double mouseX, double mouseY) {
        int tooltipX = (int)(mouseX + 5);
        int tooltipY = (int)(mouseY + 5);
        
        float tooltipScaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        float tipScale = 0.75f * tooltipScaleRatio;
        
        int rawTextWidth;
        if (state.getHoveredTooltipText() != null) {
            rawTextWidth = client.textRenderer.getWidth(state.getHoveredTooltipText());
        } else {
            rawTextWidth = client.textRenderer.getWidth(state.getHoveredTooltip());
        }
        
        int bgPadding = 10;
        int rawFontHeight = 9;
        int rawTooltipWidth = rawTextWidth + bgPadding;
        int rawTooltipHeight = rawFontHeight + 8;
        
        int scaledTooltipWidth = (int)(rawTooltipWidth * tipScale);
        int scaledTooltipHeight = (int)(rawTooltipHeight * tipScale);
        
        int finalTooltipX = tooltipX;
        int finalTooltipY = tooltipY;
        
        if (finalTooltipX + scaledTooltipWidth > client.getWindow().getScaledWidth()) {
            finalTooltipX = tooltipX - scaledTooltipWidth - 10;
        }
        if (finalTooltipY - scaledTooltipHeight < 0) {
            finalTooltipY = tooltipY + 10;
        }
        
        context.getMatrices().push();
        context.getMatrices().scale(tipScale, tipScale, 1f);
        
        int bgLeft = (int)(finalTooltipX / tipScale);
        int bgTop = (int)((finalTooltipY - scaledTooltipHeight) / tipScale);
        int bgRight = bgLeft + rawTooltipWidth;
        int bgBottom = bgTop + rawTooltipHeight;
        
        GuiRenderUtil.drawRoundedRect(context, bgLeft, bgTop, bgRight, bgBottom, 3, theme.BG_2.getRGB());
        
        int textDrawX = bgLeft + 5;
        int textDrawY = bgTop + 4;
        
        if (state.getHoveredTooltipText() != null) {
            context.drawText(client.textRenderer, state.getHoveredTooltipText(), 
                           textDrawX, textDrawY, 0xFFFFFF, false);
        } else if (state.getHoveredTooltip() != null) {
            context.drawText(client.textRenderer, state.getHoveredTooltip(), 
                           textDrawX, textDrawY, theme.FONT_C.getRGB(), false);
        }
        context.getMatrices().pop();
    }
    
    private static void updateModuleExpandAnimations(ClickGuiState state) {
        for (Module module : ModuleManager.getModulesByCategory(state.getSelectedCategory())) {
            updateModuleExpandAnimation(state, module.getName(), module.isExpanded());
        }
    }
    
    private static void updateModuleExpandAnimation(ClickGuiState state, 
                                                    String moduleName, boolean expanded) {
        float target = expanded ? 1.0f : 0.0f;
        float current = state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
        
        float speed = 0.18f;
        float lin = current + (target - current) * speed;
        float t = Math.max(0f, Math.min(1f, lin));
        float eased = t * t * (3 - 2 * t);
        float newValue = eased;
        
        if (Math.abs(newValue - target) < 0.01f) {
            newValue = target;
        }
        
        state.getModuleExpandAnimations().put(moduleName, newValue);
    }
    
    private static float getModuleExpandProgress(ClickGuiState state, String moduleName) {
        return state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
    }
}


