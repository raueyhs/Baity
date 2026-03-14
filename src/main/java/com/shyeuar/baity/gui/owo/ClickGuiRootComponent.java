package com.shyeuar.baity.gui.owo;

import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.render.ModuleStyleRenderer;
import com.shyeuar.baity.gui.render.ValueStyleRenderer;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ModuleCategory;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

public class ClickGuiRootComponent extends BaseComponent {
    
    private final ClickGuiState state;
    private final Theme theme;
    private GuiGraphics guiGraphics;
    
    private Function<String, String> getTooltipText;
    private Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors;
    private Function<Object, String> getDisplayTextFormatter;
    private ModuleStyleRenderer.TooltipInfo tooltipInfo;
    
    private List<Module> cachedFilteredModules = null;
    private String cachedSearchText = null;
    private ModuleCategory cachedCategory = null;
    private String cachedAuthorName = null;
    private String cachedModVersion = null;
    
    public ClickGuiRootComponent(ClickGuiState state, Theme theme,
                                 Function<String, String> getTooltipText,
                                 Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                 Function<Object, String> getDisplayTextFormatter,
                                 ModuleStyleRenderer.TooltipInfo tooltipInfo) {
        this.state = state;
        this.theme = theme;
        this.getTooltipText = getTooltipText;
        this.getTooltipTextWithColors = getTooltipTextWithColors;
        this.getDisplayTextFormatter = getDisplayTextFormatter;
        this.tooltipInfo = tooltipInfo;
        this.horizontalSizing(Sizing.fixed((int)ClickGuiState.WIDTH));
        this.verticalSizing(Sizing.fixed((int)ClickGuiState.HEIGHT));
        this.positioning(Positioning.absolute(0, 0));
    }
    
    public void setGuiGraphics(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
    }
    
    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        
        updateModuleExpandAnimations();
        
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        updateModuleShimmerAnimations(coords.mouseX, coords.mouseY);
    }
    
    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (guiGraphics == null) return;
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        state.setHoveredTooltip(null);
        state.setHoveredTooltipText(null);
        if (tooltipInfo != null) {
            tooltipInfo.tooltip = null;
            tooltipInfo.tooltipText = null;
        }
        
        float scaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        OwoRenderAdapter adapter = OwoRenderAdapter.of(context, guiGraphics);
        
        var matrices = guiGraphics.pose();
        matrices.pushMatrix();
        matrices.translate(state.getWindowX(), state.getWindowY());
        matrices.scale(scaleRatio, scaleRatio);
        
        renderWindowBackground(adapter);
        renderSidebar(adapter, client, coords.mouseX, coords.mouseY);
        renderSearchBar(adapter, client, coords.mouseX, coords.mouseY);
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        float visibleTop = contentY;
        float visibleBottom = ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        float visibleHeight = Math.max(0, visibleBottom - visibleTop);
        
        List<Module> modules = getFilteredModules();
        float calculatedContentHeight = ClickGuiLayout.calculateContentHeightForModules(modules, visibleHeight);
        
        ClickGuiLayout.ScrollbarInfo scrollbarInfo = ClickGuiLayout.calculateScrollbar(state, calculatedContentHeight, visibleHeight);
        ClickGuiLayout.clampScrollOffset(state, scrollbarInfo.maxScroll);
        
        guiGraphics.enableScissor((int)contentX, (int)contentY, 
                                 (int)(contentX + contentWidth), (int)(contentY + visibleHeight));
        
        float modY = contentY + 10 - state.getScrollOffset();
        
        if (modules.isEmpty()) {
            renderPlaceholder(adapter, client, modY, contentX, contentWidth);
            modY += 100;
        }
        
        ModuleStyleRenderer.setState(state);
        
        for (Module module : modules) {
            renderModule(adapter, client, module, contentX + 10, modY, contentX + contentWidth - 10,
                        coords.mouseX, coords.mouseY);
            
            if (tooltipInfo != null && tooltipInfo.tooltip != null) {
                state.setHoveredTooltip(tooltipInfo.tooltip);
                state.setHoveredTooltipText(tooltipInfo.tooltipText);
                state.setTooltipX(tooltipInfo.x);
                state.setTooltipY(tooltipInfo.y);
            }
            
            modY += 30;
            
            modY += renderSubOptions(adapter, client, module, contentX + 20, modY, contentX + contentWidth - 20,
                                   visibleHeight, coords.mouseX, coords.mouseY);
        }
        
        guiGraphics.disableScissor();
        
        if (calculatedContentHeight > visibleHeight) {
            renderScrollbar(adapter, scrollbarInfo, contentX + contentWidth);
        }
        
        renderWatermark(adapter, client);
        renderHudButton(adapter, client, coords.mouseX, coords.mouseY);
        
        updateVersionCheckStatus();
        renderVersion(adapter, client, coords.mouseX, coords.mouseY);
        
        matrices.popMatrix();
        
        if (state.getHoveredTooltip() != null) {
            renderTooltip(adapter, client, mouseX, mouseY);
        }
    }
    
    private void renderWindowBackground(OwoRenderAdapter adapter) {
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT, 
                                 theme.BG.getRGB(), 6f);
    }
    
    private List<Module> getFilteredModules() {
        String searchText = state.getSearchText().toLowerCase().trim();
        ModuleCategory selectedCategory = state.getSelectedCategory();
        
        if (cachedFilteredModules != null && 
            cachedSearchText != null && cachedSearchText.equals(searchText) &&
            cachedCategory == selectedCategory) {
            return cachedFilteredModules;
        }
        
        List<Module> modules;
        if (searchText.isEmpty()) {
            modules = ModuleManager.getModulesByCategory(selectedCategory);
        } else {
            modules = ModuleManager.getModules().stream()
                    .filter(module -> module.getName().toLowerCase().contains(searchText))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        cachedFilteredModules = modules;
        cachedSearchText = searchText;
        cachedCategory = selectedCategory;
        
        return modules;
    }
    
    private void renderSidebar(OwoRenderAdapter adapter, Minecraft client, float mouseX, float mouseY) {
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 0, 0, (int)ClickGuiState.SIDEBAR_WIDTH, (int)ClickGuiState.HEIGHT, 
                                 theme.BG.getRGB(), 0f);
        
        renderLogoAndTitle(adapter, client, mouseX, mouseY);
        
        com.shyeuar.baity.gui.render.GuiRenderUtil.divider(guiGraphics, ClickGuiState.SIDEBAR_WIDTH, 0, 
                            ClickGuiState.SIDEBAR_WIDTH, ClickGuiState.HEIGHT,
                            new java.awt.Color(255, 255, 255, 20).getRGB());
        
        float categoryY = ClickGuiState.HEADER_HEIGHT + 20;
        float categorySpacing = 35f;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
            com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            boolean active = category == state.getSelectedCategory();
            boolean hovered = mouseX >= 0 && mouseX < ClickGuiState.SIDEBAR_WIDTH &&
                            mouseY >= categoryY - 5 && mouseY < categoryY + 25;
            
            int categoryBgColor = theme.BG_2.getRGB();
            if (active) {
                com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, categoryBgColor, 0f);
                
                int purpleBar = theme.BG_3.getRGB();
                com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 0, categoryY - 5, 3, categoryY + 25, purpleBar, 0f);
            } else if (hovered) {
                int hoverBg = new java.awt.Color(255, 255, 255, 24).getRGB();
                com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, hoverBg, 0f);
            } else {
                int normalBg = new java.awt.Color(
                    (int)((categoryBgColor >> 16) & 0xFF) - 10,
                    (int)((categoryBgColor >> 8) & 0xFF) - 10,
                    (int)(categoryBgColor & 0xFF) - 10,
                    255
                ).getRGB();
                com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, normalBg, 0f);
            }
            
            String label = category.getDisplayName();
            int textX = 15;
            int textY = (int)categoryY;
            int color = active ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
            
            guiGraphics.drawString(client.font, label, textX, textY, color, false);
            
            categoryY += categorySpacing;
        }
        
        renderGitHubIcon(adapter, client, mouseX, mouseY);
    }
    
    private void renderLogoAndTitle(OwoRenderAdapter adapter, Minecraft client, float mouseX, float mouseY) {
        float availableHeight = ClickGuiState.HEADER_HEIGHT + 20;
        float availableWidth = ClickGuiState.SIDEBAR_WIDTH;
        
        int logoSize = (int)(Math.min(availableWidth, availableHeight) * 1.3f);
        
        int logoX = (int)((availableWidth - logoSize) / 2);
        int logoY = (int)((availableHeight - logoSize) / 2);
        
        ResourceLocation logoTexture = ResourceLocation.fromNamespaceAndPath("baity", "textures/gui/logo.png");
        
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, logoTexture, logoX, logoY, 0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize);
    }
    
    private void renderGitHubIcon(OwoRenderAdapter adapter, Minecraft client, float mouseX, float mouseY) {
        int iconSize = 20;
        int padding = 8;
        int iconX = padding;
        int iconY = (int)(ClickGuiState.HEIGHT - iconSize - padding);
        
        boolean isHovered = mouseX >= iconX && mouseX < iconX + iconSize &&
                          mouseY >= iconY && mouseY < iconY + iconSize;
        
        float alpha = isHovered ? 1.0f : 0.7f;
        int color = (int)(alpha * 255) << 24 | 0xFFFFFF;
        
        ResourceLocation githubIcon = ResourceLocation.fromNamespaceAndPath("baity", "textures/gui/github.png");
        
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, githubIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize, color);
    }
    
    private void renderSearchBar(OwoRenderAdapter adapter, Minecraft client, float mouseX, float mouseY) {
        float iconSize = 12;
        float iconPadding = 4;
        float searchX = ClickGuiState.SIDEBAR_WIDTH + 20;
        float searchY = 15;
        float searchHeight = 20;
        
        float searchWidth = ClickGuiState.CONTENT_WIDTH - 40;
        
        ResourceLocation searchIcon = ResourceLocation.fromNamespaceAndPath("baity", "textures/gui/search.png");
        int iconX = (int)(searchX + iconPadding);
        int iconY = (int)(searchY + (searchHeight - iconSize) / 2);
        int iconColor = 0xFFFFFFFF;
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, searchIcon, iconX, iconY, 0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize, iconColor);
        
        float textStartX = searchX + iconSize + iconPadding * 2;
        
        boolean focused = state.isSearchFocused();
        
        String searchText = state.getSearchText();
        String displayText = (focused && searchText.isEmpty()) ? "" : 
                            (searchText.isEmpty() ? "Search..." : searchText);
        int textColor = searchText.isEmpty() ? 
            theme.FONT.getRGB() : 
            theme.FONT_C.getRGB();
        
        int textX = (int)textStartX;
        int textY = (int)(searchY + 6);
        if (!displayText.isEmpty()) {
            guiGraphics.drawString(client.font, displayText, textX, textY, textColor, false);
        }
        
        if (focused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = textX + client.font.width(displayText);
            guiGraphics.fill(cursorX, textY, cursorX + 1, textY + 9, theme.FONT_C.getRGB());
        }
        
        float lineY = searchY + searchHeight - 1;
        int lineColor = new java.awt.Color(150, 150, 150, 200).getRGB();
        guiGraphics.fill((int)searchX, (int)lineY, (int)(searchX + searchWidth), (int)(lineY + 1), lineColor);
    }
    
    private void renderHudButton(OwoRenderAdapter adapter, Minecraft client, float mouseX, float mouseY) {
        int githubIconSize = 20;
        int githubPadding = 8;
        int githubIconY = (int)(ClickGuiState.HEIGHT - githubIconSize - githubPadding);
        float githubIconCenterY = githubIconY + githubIconSize / 2.0f;
        
        float hudButtonWidth = 30;
        float hudButtonHeight = 16;
        float distanceFromDivider = 10;
        float hudButtonX = ClickGuiState.SIDEBAR_WIDTH - distanceFromDivider - hudButtonWidth;
        float hudButtonY = githubIconCenterY - hudButtonHeight / 2.0f;
        
        boolean hudButtonHovered = mouseX >= hudButtonX && mouseX <= hudButtonX + hudButtonWidth &&
                                   mouseY >= hudButtonY && mouseY <= hudButtonY + hudButtonHeight;
        
        int hudButtonBgColor = hudButtonHovered ? 
            new java.awt.Color(255, 255, 255, 24).getRGB() : 
            theme.BG_2.getRGB();
        
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 
            (int)hudButtonX, (int)hudButtonY, 
            (int)(hudButtonX + hudButtonWidth), (int)(hudButtonY + hudButtonHeight), 
            hudButtonBgColor, 0f);
        
        String hudButtonText = "HUD";
        int hudTextWidth = client.font.width(hudButtonText);
        int hudTextX = (int)(hudButtonX + (hudButtonWidth - hudTextWidth) / 2);
        int hudTextY = (int)(hudButtonY + (hudButtonHeight - client.font.lineHeight) / 2);
        guiGraphics.drawString(client.font, hudButtonText, hudTextX, hudTextY, theme.FONT.getRGB(), false);
        
        state.setHudButtonBounds((int)hudButtonX, (int)hudButtonY, (int)hudButtonWidth, (int)hudButtonHeight);
    }
    
    private void renderPlaceholder(OwoRenderAdapter adapter, Minecraft client, float modY, float contentX, float contentWidth) {
        String placeholderText = "nothing~~~";
        int textWidth = client.font.width(placeholderText);
        int textX = (int)(contentX + (contentWidth - textWidth) / 2);
        int textY = (int)(modY + 50);
        guiGraphics.drawString(client.font, placeholderText, textX, textY, theme.FONT.getRGB(), false);
    }
    
    private void renderModule(OwoRenderAdapter adapter, Minecraft client, Module module,
                             float x, float modY, float width,
                             float mouseX, float mouseY) {
        float x2 = x + (width - x);
        float moduleHeight = 25f;
        
        boolean hovered = mouseX >= x && mouseY >= modY && mouseX <= x2 && mouseY <= modY + moduleHeight;
        
        com.shyeuar.baity.gui.theme.LinearTheme.applyToTheme(theme);
        
        if (module.isEnabled()) {
            int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
            int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
            com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DGradientRect(guiGraphics, x, modY, x2, modY + moduleHeight, accentStart, accentEnd, 6f);
        } else {
            int cardBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
            com.shyeuar.baity.gui.render.GuiRenderUtil.drawFrostedGlass(guiGraphics, x, modY, x2, modY + moduleHeight, cardBg, 6f);
            com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, x, modY, x2, modY + moduleHeight, cardBg, 6f);
        }
        
        if (!"ClickGUI".equals(module.getName())) {
            ClickGuiState.ShimmerAnimationState shimmerState = 
                state.getModuleShimmerAnimations().get(module.getName());
            
            if (shimmerState != null && (shimmerState.isActive || shimmerState.isExiting || shimmerState.progress > 0f)) {
                boolean isEnabled = module.isEnabled();
                com.shyeuar.baity.gui.animation.ShimmerEffect.renderHoverShimmer(
                    guiGraphics, x, modY, x2, modY + moduleHeight,
                    shimmerState.mouseX, shimmerState.mouseY,
                    shimmerState.isActive, shimmerState.isExiting,
                    shimmerState.progress, shimmerState.direction,
                    isEnabled);
            }
        }
        
        String displayName = module.getName();
        if ("ClickGUI".equals(module.getName())) {
            displayName = "ClickGUI";
        }
        guiGraphics.drawString(client.font, displayName, (int)(x + 10), (int)(modY + 8), theme.FONT_C.getRGB(), false);
        
        if (hovered && tooltipInfo != null) {
            String tooltip = getTooltipText.apply(module.getName());
            if (tooltip != null) {
                tooltipInfo.tooltip = tooltip;
                tooltipInfo.tooltipText = getTooltipTextWithColors.apply(module.getName());
                float tooltipOffset = 5f;
                tooltipInfo.x = (int)(mouseX + tooltipOffset);
                tooltipInfo.y = (int)(mouseY + tooltipOffset);
            }
        }
        
        boolean hasChildren = false;
        for (com.shyeuar.baity.gui.value.Value v : module.getValues()) {
            if (!"enabled".equals(v.getName())) {
                hasChildren = true;
                break;
            }
        }
        if (hasChildren && !module.getName().equals("ClickGUI")) {
            String arrow = module.isExpanded() ? "▼" : "▶";
            guiGraphics.drawString(client.font, arrow, (int)(x2 - 25), (int)(modY + 8), theme.FONT_C.getRGB(), false);
        }
        
        if (module.getName().equals("ClickGUI")) {
            renderKeybindBox(adapter, client, x, modY, x2, moduleHeight, mouseX, mouseY);
        }
    }
    
    private void renderKeybindBox(OwoRenderAdapter adapter, Minecraft client,
                                   float containerX1, float containerY, float containerX2, float containerHeight,
                                   float mouseX, float mouseY) {
        boolean isListening = state.isListeningForKey();
        String keyDisplay = state.getCurrentKeyDisplay();
        String keyText = isListening ? "Press a key..." : keyDisplay;
        renderKeybindBoxContent(adapter, client, containerX2, containerY, containerHeight, mouseX, mouseY, isListening, keyText);
    }
    
    private void renderKeybindBoxContent(OwoRenderAdapter adapter, Minecraft client,
                                        float containerX2, float containerY, float containerHeight,
                                        float mouseX, float mouseY, boolean isListening, String displayText) {
        if (displayText == null || displayText.isEmpty()) {
            displayText = "☄ NOTSET";
        }
        String plainText = displayText.replaceAll("§[0-9a-fklmnor]", "");
        int textWidth = client.font.width(plainText);
        int boxWidth = textWidth + 16;
        float boxCenterY = containerY + containerHeight / 2f;
        int boxHeight = 12;
        
        int boxX1 = (int)(containerX2 - boxWidth - 10);
        int boxY1 = (int)(boxCenterY - boxHeight / 2f);
        int boxX2 = (int)(containerX2 - 10);
        int boxY2 = (int)(boxCenterY + boxHeight / 2f);
        
        boolean boxHovered = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, mouseX, mouseY);
        int boxBgColor = isListening ? theme.BG_3.getRGB() :
                        (boxHovered ? new java.awt.Color(255, 255, 255, 24).getRGB() : theme.BG_2.getRGB());
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, boxX1, boxY1, boxX2, boxY2, boxBgColor, 0f);
        
        int baseX = boxX1 + 8;
        int baseY = (int)(boxCenterY - 4);
        
        if (isListening) {
            String hintText = "Press Backspace to reset";
            int hintColor = 0xFFFFFF00;
            int hintX = boxX1 - client.font.width(hintText) - 8;
            guiGraphics.drawString(client.font, hintText, hintX, baseY, hintColor, false);
            
            net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
            guiGraphics.drawString(client.font, textObj, baseX, baseY, theme.FONT_C.getRGB(), false);
        } else {
            String displayPlainText = displayText.replaceAll("§[0-9a-fklmnor]", "");
            
            if (displayPlainText.startsWith("✎")) {
                String prefix = "✎";
                String keyName = displayPlainText.substring(1);
                int prefixRGB = com.shyeuar.baity.utils.KeyMappingUtils.getModuleEnabledPurpleRGB();
                int keyNameRGB = theme.FONT.getRGB();
                
                net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
                guiGraphics.drawString(client.font, prefixText, baseX, baseY, prefixRGB, false);
                
                int prefixWidth = client.font.width(prefix);
                net.minecraft.network.chat.Component keyTextObj = net.minecraft.network.chat.Component.literal(keyName);
                guiGraphics.drawString(client.font, keyTextObj, baseX + prefixWidth, baseY, keyNameRGB, false);
            } else if (displayPlainText.startsWith("☄") || 
                       displayPlainText.toUpperCase().contains("NOTSET") || 
                       displayPlainText.toUpperCase().contains("NONE") || 
                       displayPlainText.toUpperCase().contains("UNKNOWN")) {
                String prefix = "☄";
                String notsetText = displayPlainText.startsWith("☄") ? displayPlainText.substring(1) : (" " + displayPlainText);
                int prefixRGB = 0xFFFFFF00;
                int notsetRGB = 0xFFAAAAAA;
                
                net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
                guiGraphics.drawString(client.font, prefixText, baseX, baseY, prefixRGB, false);
                
                int prefixWidth = client.font.width(prefix);
                net.minecraft.network.chat.Component notsetTextObj = net.minecraft.network.chat.Component.literal(notsetText);
                guiGraphics.drawString(client.font, notsetTextObj, baseX + prefixWidth, baseY, notsetRGB, false);
            } else {
                net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
                guiGraphics.drawString(client.font, textObj, baseX, baseY, theme.FONT.getRGB(), false);
            }
        }
    }
    
    private float renderSubOptions(OwoRenderAdapter adapter, Minecraft client, Module module,
                                  float containerX1, float modY, float containerX2,
                                  float visibleHeight,
                                  float mouseX, float mouseY) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return 0;
        
        float expandProgress = getModuleExpandProgress(module.getName());
        if (expandProgress <= 0.0f) return 0;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        int fullContainerHeight = subOptionCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
        int containerHeight = (int)(fullContainerHeight * expandProgress);

        int containerBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
        float containerY1 = modY;
        float containerY2 = modY + containerHeight;
        
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, containerX1, containerY1, containerX2, containerY2, containerBg, 0f);
        com.shyeuar.baity.gui.render.GuiRenderUtil.stroke1px(guiGraphics, containerX1, containerY1, containerX2, containerY2,
                               com.shyeuar.baity.gui.theme.LinearTheme.BORDER_PRIMARY.getRGB());
        
        int innerVisible = Math.max(0, containerHeight - dims.padding * 2);
        if (innerVisible >= dims.subOptionHeight / 2) {
            float subModY = modY + dims.padding;
            
            Value previousValue = null;
            for (Value value : module.getValues()) {
                if ("enabled".equals(value.getName())) continue;
                
                if (value.needsSeparatorBefore(previousValue)) {
                    subModY += 12; 
                }
                
                float currentHeight = dims.subOptionHeight;
                if (value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.COLOR_PALETTE) {
                    currentHeight = dims.subOptionHeight * 2;
                }
                
                float localAlphaF = Math.min(1f, Math.max(0f, 
                    (innerVisible - (subModY - modY - dims.padding)) / (float)dims.subOptionHeight));
                int localAlpha = (int)(255 * expandProgress * localAlphaF);
                
                int subX1 = (int)(containerX1 + 4);
                int subX2 = (int)(containerX2 - 4);
                
                ValueStyleRenderer.renderValue(guiGraphics, client, module, value, theme,
                                              subX1, subModY, subX2, dims.subOptionHeight,
                                              mouseX, mouseY, localAlpha,
                                              getTooltipText, getTooltipTextWithColors,
                                              getDisplayTextFormatter,
                                              state.getListeningButtonValueName(),
                                              tooltipInfo,
                                              state.getEditingSlider(),
                                              state.getSliderInputText());
                
                if (tooltipInfo != null && tooltipInfo.tooltip != null) {
                    state.setHoveredTooltip(tooltipInfo.tooltip);
                    state.setHoveredTooltipText(tooltipInfo.tooltipText);
                    state.setTooltipX(tooltipInfo.x);
                    state.setTooltipY(tooltipInfo.y);
                }
                
                subModY += currentHeight;
                previousValue = value;
            }
        }
        
        return containerHeight + 5;
    }
    
    private void renderScrollbar(OwoRenderAdapter adapter, ClickGuiLayout.ScrollbarInfo info, float contentRight) {
        float barX1 = contentRight - 6;
        float barX2 = contentRight - 2;
        com.shyeuar.baity.gui.render.GuiRenderUtil.drawRoundedRect(guiGraphics, barX1, info.barY, barX2, 
                                     info.barY + info.barHeight, 2, theme.BG_2.getRGB());
    }
    
    private void renderWatermark(OwoRenderAdapter adapter, Minecraft client) {
        if (cachedAuthorName == null) {
            cachedAuthorName = getAuthorRealName(client);
        }
        String watermark = "By " + cachedAuthorName + " (AKA raueyhs , shyeuar)";
        int wmRawWidth = client.font.width(watermark);
        float wmScale = 0.70f;
        float scaledWidth = wmScale * wmRawWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - 8;
        float baseY = 8;
        
        int wmColor = new java.awt.Color(120, 124, 132).getRGB();
        var matrices = guiGraphics.pose();
        matrices.pushMatrix();
        matrices.scale(wmScale, wmScale);
        guiGraphics.drawString(client.font, watermark, 
                        (int)(baseX / wmScale), (int)(baseY / wmScale), wmColor, false);
        matrices.popMatrix();
    }
    
    private String getAuthorRealName(Minecraft client) {
        java.util.UUID authorUUID = java.util.UUID.fromString("8b8e7203-bdda-489e-bc20-f226f5b59c62");
        
        if (client.player == null || client.player.connection == null) {
            return "11YearCookieBuff";
        }
        
        try {
            net.minecraft.client.multiplayer.ClientPacketListener connection = client.player.connection;
            net.minecraft.client.multiplayer.PlayerInfo playerInfo = connection.getPlayerInfo(authorUUID);
            if (playerInfo != null) {
                com.mojang.authlib.GameProfile profile = playerInfo.getProfile();
                if (profile != null) {
                    try {
                        java.lang.reflect.Method getNameMethod = profile.getClass().getMethod("getName");
                        Object nameObj = getNameMethod.invoke(profile);
                        if (nameObj instanceof String) {
                            String name = (String) nameObj;
                            if (name != null && !name.isEmpty()) {
                                return name;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        
        try {
            java.util.Collection<net.minecraft.client.multiplayer.PlayerInfo> allPlayers = 
                client.player.connection.getOnlinePlayers();
            for (net.minecraft.client.multiplayer.PlayerInfo info : allPlayers) {
                com.mojang.authlib.GameProfile profile = info.getProfile();
                if (profile != null) {
                    try {
                        java.lang.reflect.Method getIdMethod = profile.getClass().getMethod("getId");
                        Object uuidObj = getIdMethod.invoke(profile);
                        if (uuidObj instanceof java.util.UUID && uuidObj.equals(authorUUID)) {
                            java.lang.reflect.Method getNameMethod = profile.getClass().getMethod("getName");
                            Object nameObj = getNameMethod.invoke(profile);
                            if (nameObj instanceof String) {
                                String name = (String) nameObj;
                                if (name != null && !name.isEmpty()) {
                                    return name;
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return "11YearCookieBuff";
    }
    
    private void renderVersion(OwoRenderAdapter adapter, Minecraft client, float mouseX, float mouseY) {
        if (cachedModVersion == null) {
            String version = getModVersion();
            if (version == null || version.isEmpty()) {
                cachedModVersion = "v1.1.7";
            } else {
                if (!version.startsWith("v") && !version.startsWith("V")) {
                    cachedModVersion = "v" + version;
                } else {
                    cachedModVersion = version;
                }
            }
        }
        String currentVersion = cachedModVersion;
        
        long currentTime = System.currentTimeMillis();
        
        float versionScale = 0.70f;
        int currentVersionWidth = client.font.width(currentVersion);
        float scaledCurrentVersionWidth = versionScale * currentVersionWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledCurrentVersionWidth - 8;
        float baseY = ClickGuiState.HEIGHT - (int)(client.font.lineHeight * versionScale) - 8;
        
        var matrices = guiGraphics.pose();
        matrices.pushMatrix();
        matrices.scale(versionScale, versionScale);
        
        if (state.isVersionChecking()) {
            long startTime = state.getVersionCheckStartTime();
            if (startTime <= 0) {
                startTime = currentTime;
                state.setVersionCheckStartTime(currentTime);
            }
            long elapsed = currentTime - startTime;
            int phase = (int)((elapsed / 300) % 3);
            String checkingText = switch (phase) {
                case 1 -> "Checking..";
                case 2 -> "Checking...";
                default -> "Checking.";
            };
            
            int textWidth = client.font.width(checkingText);
            float scaledTextWidth = versionScale * textWidth;
            float renderX = baseX + scaledCurrentVersionWidth - scaledTextWidth;
            
            guiGraphics.drawString(client.font, checkingText,
                    (int)(renderX / versionScale), (int)(baseY / versionScale),
                    0xFFFFFF00, false);
            
            matrices.popMatrix();
            return;
        }
        
        String displayText = currentVersion;
        boolean showFeedback = false;
        
        String checkStatus = state.getVersionCheckStatus();
        long startTime = state.getVersionCheckStartTime();
        boolean isError = false;
        boolean isAutoCheck = state.isAutoCheck();
        long displayDuration = (isAutoCheck && "update_available".equals(checkStatus)) ? 6000 : 2000;
        if (checkStatus != null && startTime > 0) {
            long elapsed = currentTime - startTime;
            if (elapsed < displayDuration) {
            if ("latest".equals(checkStatus)) {
                    if (!isAutoCheck) {
                        showFeedback = true;
                displayText = "It's already the latest version！";
                    }
                } else {
                    showFeedback = true;
                    if ("error".equals(checkStatus)) {
                String errorMsg = state.getLatestVersion();
                if (errorMsg != null && errorMsg.equals("Unknown error")) {
                    displayText = "Unknown error";
                    isError = true;
                } else {
                    displayText = "It's already the latest version！Network error！";
                    isError = true;
                }
            } else if ("update_available".equals(checkStatus)) {
                String latest = state.getLatestVersion();
                if (latest != null) {
                    displayText = "Available updates！Check " + latest + "！";
                } else {
                    displayText = "Available updates！";
                        }
                    }
                }
            }
        }
        
        int displayTextWidth = client.font.width(displayText);
        float scaledDisplayTextWidth = versionScale * displayTextWidth;
        float renderX = baseX + scaledCurrentVersionWidth - scaledDisplayTextWidth;
        
        boolean isHovered = false;
        int versionColor;
        
        if (showFeedback) {
            versionColor = 0xFFFFFF00;
        } else {
            float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
            float lineHeight = 1;
            isHovered = mouseX >= baseX && mouseX <= baseX + scaledCurrentVersionWidth &&
                       ((mouseY >= baseY && mouseY <= baseY + (int)(client.font.lineHeight * versionScale)) ||
                        (mouseY >= lineY && mouseY <= lineY + lineHeight));
            state.setVersionHovered(isHovered);
            versionColor = isHovered ? 0xFFFFFF00 : new java.awt.Color(120, 124, 132).getRGB();
        }
        
        if (showFeedback && "update_available".equals(state.getVersionCheckStatus())) {
            String latest = state.getLatestVersion();
            if (latest != null) {
                String prefix = "Available updates！Check ";
                String suffix = "！";
                int prefixWidth = client.font.width(prefix);
                int versionWidth = client.font.width(latest);
                
                guiGraphics.drawString(client.font, prefix,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                0xFFFFFF00, false);
                guiGraphics.drawString(client.font, latest,
                                (int)((renderX + prefixWidth * versionScale) / versionScale),
                                (int)(baseY / versionScale),
                                0xFF00FF00, false);
                guiGraphics.drawString(client.font, suffix,
                                (int)((renderX + (prefixWidth + versionWidth) * versionScale) / versionScale),
                                (int)(baseY / versionScale),
                                0xFFFFFF00, false);
            } else {
                guiGraphics.drawString(client.font, displayText,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                versionColor, false);
            }
        } else if (showFeedback && isError) {
            String errorMsg = state.getLatestVersion();
            if (errorMsg != null && errorMsg.equals("Unknown error")) {
                guiGraphics.drawString(client.font, displayText,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR, false);
            } else {
                String prefix = "It's already the latest version！";
                String suffix = "Network error！";
                int prefixWidth = client.font.width(prefix);
                
                guiGraphics.drawString(client.font, prefix,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                0xFFFFFF00, false);
                guiGraphics.drawString(client.font, suffix,
                                (int)((renderX + prefixWidth * versionScale) / versionScale),
                                (int)(baseY / versionScale),
                                com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR, false);
            }
        } else if (showFeedback) {
            guiGraphics.drawString(client.font, displayText,
                            (int)(renderX / versionScale), (int)(baseY / versionScale),
                            versionColor, false);
        } else {
            guiGraphics.drawString(client.font, displayText,
                            (int)(baseX / versionScale), (int)(baseY / versionScale),
                            versionColor, false);
        }
        
        matrices.popMatrix();
        
        if (!showFeedback) {
            float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
            float lineX1 = baseX;
            float lineX2 = baseX + scaledCurrentVersionWidth;
            int lineColor = (versionColor & 0xFFFFFF) | 0x64000000;
            guiGraphics.fill((int)lineX1, (int)lineY, (int)lineX2, (int)lineY + 1, lineColor);
        } else if (showFeedback && "update_available".equals(state.getVersionCheckStatus())) {
            String latest = state.getLatestVersion();
            if (latest != null) {
                String prefix = "Available updates！Check ";
                int prefixWidth = client.font.width(prefix);
                int versionWidth = client.font.width(latest);
                
                float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
                float versionLineX1 = renderX + prefixWidth * versionScale;
                float versionLineX2 = versionLineX1 + versionWidth * versionScale;
                int lineColor = (0xFF00FF00 & 0xFFFFFF) | 0x64000000;
                guiGraphics.fill((int)versionLineX1, (int)lineY, (int)versionLineX2, (int)lineY + 1, lineColor);
            }
        }
    }
    
    private String getModVersion() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            java.util.Optional<net.fabricmc.loader.api.ModContainer> modContainer = loader.getModContainer("baity");
            if (modContainer.isPresent()) {
                return modContainer.get().getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    private void updateVersionCheckStatus() {
        long currentTime = System.currentTimeMillis();
        String checkStatus = state.getVersionCheckStatus();
        long startTime = state.getVersionCheckStartTime();
        boolean isAutoCheck = state.isAutoCheck();
        long displayDuration = (isAutoCheck && "update_available".equals(checkStatus)) ? 6000 : 2000;
        if (checkStatus != null && startTime > 0 && 
            currentTime - startTime >= displayDuration) {
            state.setVersionCheckStatus(null);
            state.setLatestVersion(null);
            state.setAutoCheck(false);
        }
    }
    
    private void renderTooltip(OwoRenderAdapter adapter, Minecraft client, double mouseX, double mouseY) {
        float tooltipScaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        float tipScale = 0.75f * tooltipScaleRatio;
        int offsetFromCursor = (int)(3 * tooltipScaleRatio);
        
        int rawTextWidth;
        if (state.getHoveredTooltipText() != null) {
            rawTextWidth = client.font.width(state.getHoveredTooltipText());
        } else {
            rawTextWidth = client.font.width(state.getHoveredTooltip());
        }
        
        int bgPadding = 10;
        int rawFontHeight = 9;
        int rawTooltipWidth = rawTextWidth + bgPadding;
        int rawTooltipHeight = rawFontHeight + 8;
        
        int scaledTooltipWidth = (int)(rawTooltipWidth * tipScale);
        int scaledTooltipHeight = (int)(rawTooltipHeight * tipScale);
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        int finalTooltipX = (int)mouseX + offsetFromCursor;
        int finalTooltipY = (int)mouseY - offsetFromCursor - scaledTooltipHeight;
        
        if (finalTooltipX + scaledTooltipWidth > screenWidth) {
            finalTooltipX = (int)mouseX - scaledTooltipWidth - offsetFromCursor;
        }
        if (finalTooltipY < 2) {
            finalTooltipY = (int)mouseY + offsetFromCursor;
        }
        if (finalTooltipY + scaledTooltipHeight > screenHeight) {
            finalTooltipY = screenHeight - scaledTooltipHeight - 2;
        }
        if (finalTooltipX < 2) {
            finalTooltipX = 2;
        }
        
        var guiMatrices = guiGraphics.pose();
        guiMatrices.pushMatrix();
        guiMatrices.translate((float)finalTooltipX, (float)finalTooltipY);
        guiMatrices.scale(tipScale, tipScale);
        
        com.shyeuar.baity.gui.render.GuiRenderUtil.drawRoundedRect(guiGraphics, 0, 0,
                                      rawTooltipWidth, rawTooltipHeight,
                                      4, theme.BG_2.getRGB());
        
        int textX = bgPadding / 2;
        int textY = (rawTooltipHeight - rawFontHeight) / 2;
        
        if (state.getHoveredTooltipText() != null) {
            guiGraphics.drawString(client.font, state.getHoveredTooltipText(), textX, textY, 0xFFFFFFFF, false);
        } else if (state.getHoveredTooltip() != null) {
            guiGraphics.drawString(client.font, state.getHoveredTooltip(), textX, textY, theme.FONT_C.getRGB() | 0xFF000000, false);
        }
        
        guiMatrices.popMatrix();
    }
    
    private void updateModuleExpandAnimations() {
        java.util.List<Module> modules = getFilteredModules();
        for (Module module : modules) {
            updateModuleExpandAnimation(module.getName(), module.isExpanded());
        }
    }
    
    private void updateModuleExpandAnimation(String moduleName, boolean expanded) {
        float target = expanded ? 1.0f : 0.0f;
        float current = state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
        
        float speed = 0.18f;
        float newValue = com.shyeuar.baity.gui.animation.InterpolationHelper.lerp(current, target, speed);
        
        if (expanded) {
            newValue = com.shyeuar.baity.gui.animation.EasingFunctions.easeOutCubic(newValue);
        } else {
            newValue = com.shyeuar.baity.gui.animation.EasingFunctions.easeInCubic(newValue);
        }
        
        if (Math.abs(newValue - target) < 0.01f) {
            newValue = target;
        }
        
        state.getModuleExpandAnimations().put(moduleName, newValue);
    }
    
    private float getModuleExpandProgress(String moduleName) {
        return state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
    }
    
    private void updateModuleShimmerAnimations(float mouseX, float mouseY) {
        List<Module> modules = getFilteredModules();
        if (modules.isEmpty()) return;
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        boolean mouseInContentArea = mouseX >= contentX && mouseX <= contentX + contentWidth &&
                                    mouseY >= contentY && mouseY <= ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        
        if (!mouseInContentArea) {
            for (Module module : modules) {
                ClickGuiState.ShimmerAnimationState animState = 
                    state.getModuleShimmerAnimations().getOrDefault(module.getName(), null);
                if (animState != null && animState.isActive) {
                    animState.isActive = false;
                    animState.isExiting = true;
                    animState.progress = 1f;
                    animState.lastUpdateTime = System.currentTimeMillis();
                }
            }
        }
        
        float modY = contentY + 10 - state.getScrollOffset();
        long currentTime = System.currentTimeMillis();
        
        for (Module module : modules) {
            float moduleX1 = contentX + 10;
            float moduleX2 = contentX + contentWidth - 10;
            float moduleY1 = modY;
            float moduleY2 = modY + 25;
            
            ClickGuiState.ShimmerAnimationState animState = 
                state.getModuleShimmerAnimations().computeIfAbsent(module.getName(), 
                    k -> new ClickGuiState.ShimmerAnimationState());
            
            boolean hovered = mouseInContentArea && mouseX >= moduleX1 && mouseY >= moduleY1 && 
                             mouseX <= moduleX2 && mouseY <= moduleY2;
            
            if (hovered) {
                if (!animState.isActive) {
                    if (animState.isExiting) {
                        animState.isExiting = false;
                        animState.progress = 0f;
                    }
                    
                    float moduleWidth = moduleX2 - moduleX1;
                    float mouseRelativeX = (mouseX - moduleX1) / moduleWidth;
                    mouseRelativeX = Math.max(0f, Math.min(1f, mouseRelativeX));
                    
                    float appearDistance = mouseRelativeX;
                    float exitDistance = 1f - mouseRelativeX;
                    
                    if (appearDistance <= 0.05f) {
                        animState.progress = 1f;
                        animState.isActive = true;
                        animState.appearSpeed = 0f;
                        animState.exitSpeed = exitDistance / 100f;
                        animState.direction = 1f;
                        animState.isExiting = false;
                        animState.lastUpdateTime = currentTime;
                    } else {
                        float targetTime = 100f;
                        
                        animState.appearSpeed = appearDistance / targetTime;
                        animState.exitSpeed = exitDistance / targetTime;
                        
                        if (animState.appearSpeed < 0.0001f) animState.appearSpeed = 0.0001f;
                        if (animState.exitSpeed < 0.0001f) animState.exitSpeed = 0.0001f;
                        
                        animState.direction = 1f;
                        animState.progress = 0f;
                        animState.isExiting = false;
                        animState.lastUpdateTime = currentTime;
                    }
                }
                animState.isActive = true;
                animState.mouseX = mouseX;
                animState.mouseY = mouseY;
                
                if (animState.progress < 1f) {
                    long elapsed = currentTime - animState.lastUpdateTime;
                    animState.progress = Math.min(1f, animState.progress + elapsed * animState.appearSpeed);
                } else {
                    animState.progress = 1f;
                }
                animState.lastUpdateTime = currentTime;
            } else {
                if (animState.isActive) {
                    animState.isActive = false;
                    animState.isExiting = true;
                    animState.progress = 1f;
                    animState.lastUpdateTime = currentTime;
                }
                
                if (animState.isExiting && animState.progress > 0f) {
                    long elapsed = currentTime - animState.lastUpdateTime;
                    animState.progress = Math.max(0f, animState.progress - elapsed * animState.exitSpeed);
                    
                    if (animState.progress <= 0f) {
                        animState.progress = 0f;
                        animState.isExiting = false;
                    }
                }
            }
            
            modY += 30;
            if (module.isExpanded()) {
                modY += getSubOptionContainerHeight(module);
            }
        }
    }
    
    private float getSubOptionContainerHeight(Module module) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        if (subOptionCount == 0) return 0;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        int fullContainerHeight = subOptionCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
        return fullContainerHeight + 5;
    }
    
    public ClickGuiState getState() {
        return state;
    }
    
    public Theme getTheme() {
        return theme;
    }
}

