package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.animation.ScalarTransition;
import com.shyeuar.baity.gui.animation.ClickGuiOpenAnimation;
import com.shyeuar.baity.gui.input.LineTextInput;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.render.ModuleStyleRenderer;
import com.shyeuar.baity.gui.render.ValueStyleRenderer;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.ValueTreeUtils;
import com.shyeuar.baity.utils.NickRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Function;

public class ClickGuiRenderer {
    private static final float VERSION_RIGHT_PADDING = 8.0f;
    
    private final ClickGuiState state;
    private final Theme theme;
    private GuiGraphicsExtractor guiGraphics;
    
    private Function<String, String> getTooltipText;
    private Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors;
    private Function<Object, String> getDisplayTextFormatter;
    private ModuleStyleRenderer.TooltipInfo tooltipInfo;
    
    private List<Module> cachedFilteredModules = null;
    private String cachedSearchText = null;
    private ModuleCategory cachedCategory = null;
    private int cachedModulesRevision = -1;
    private String cachedModVersion = null;
    private final ClickGuiTooltipAnimator tooltipAnimator = new ClickGuiTooltipAnimator();
    private final ScalarTransition motion = new ScalarTransition();
    
    public ClickGuiRenderer(ClickGuiState state, Theme theme,
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
    }

    public void setGuiGraphics(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }
    
    public void update(float delta, int mouseX, int mouseY) {
        List<Module> modules = getFilteredModules();
        ClickGuiMotion.updateAnimations(state, modules, motion);
        
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        updateModuleShimmerAnimations(coords.mouseX, coords.mouseY);
    }
    
    public void draw(int mouseX, int mouseY, float partialTicks, float delta) {
        if (guiGraphics == null) return;

        NickRenderUtils.beginClickGuiRenderScope();
        try {
            baity$drawGui(mouseX, mouseY, partialTicks, delta);
        } finally {
            NickRenderUtils.endClickGuiRenderScope();
        }
    }

    private void baity$drawGui(int mouseX, int mouseY, float partialTicks, float delta) {
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
        boolean suppressTooltips = ClickGuiLayout.shouldSuppressContentTooltips(coords.mouseX, coords.mouseY);

        long openElapsedMs = state.getOpenAnimationElapsedMs();
        ClickGuiOpenAnimation.OpeningFrame openingFrame = ClickGuiOpenAnimation.computeFrame(
                openElapsedMs, ClickGuiState.WIDTH, ClickGuiState.HEIGHT);
        renderOpenBackdrop(client, openElapsedMs);
        
        var matrices = guiGraphics.pose();
        matrices.pushMatrix();
        matrices.translate(Math.round(state.getWindowX()), Math.round(state.getWindowY()));
        matrices.scale(scaleRatio, scaleRatio);

        if (ClickGuiOpenAnimation.isPhase1(openElapsedMs)) {
            com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(
                    guiGraphics,
                    openingFrame.x(),
                    openingFrame.y(),
                    openingFrame.x() + openingFrame.width(),
                    openingFrame.y() + openingFrame.height(),
                    theme.BG.getRGB(),
                    6f);
            matrices.popMatrix();
            tooltipAnimator.endFrame(false);
            return;
        }

        boolean openingClipActive = ClickGuiOpenAnimation.isOpening(openElapsedMs);
        if (openingClipActive) {
            int clipX1 = (int) openingFrame.x();
            int clipY1 = (int) openingFrame.y();
            int clipX2 = clipX1 + Math.max(1, (int) openingFrame.width());
            int clipY2 = clipY1 + Math.max(1, (int) openingFrame.height());
            guiGraphics.enableScissor(clipX1, clipY1, clipX2, clipY2);
        }
        
        renderWindowBackground();
        renderSidebar(client, coords.mouseX, coords.mouseY);
        renderSearchBar(client, coords.mouseX, coords.mouseY);
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        float visibleTop = contentY;
        float visibleBottom = ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        float visibleHeight = Math.max(0, visibleBottom - visibleTop);
        
        List<Module> modules = getFilteredModules();
        float calculatedContentHeight = ClickGuiLayout.calculateContentHeightForModules(modules, visibleHeight, state);
        
        ClickGuiLayout.ScrollbarInfo scrollbarInfo = ClickGuiLayout.calculateScrollbar(state, calculatedContentHeight, visibleHeight);
        ClickGuiLayout.clampScrollOffset(state, scrollbarInfo.maxScroll);
        
        guiGraphics.enableScissor((int)contentX, (int)contentY, 
                                 (int)(contentX + contentWidth), (int)(contentY + visibleHeight));
        
        float modY = contentY + 10 - state.getLayoutScrollOffset();
        
        if (modules.isEmpty()) {
            renderPlaceholder(client, modY, contentX, contentWidth);
            modY += 100;
        }
        
        ModuleStyleRenderer.setState(state);
        
        for (Module module : modules) {
            renderModule(client, module, contentX + 10, modY, contentX + contentWidth - 10,
                        coords.mouseX, coords.mouseY, visibleTop, visibleBottom, suppressTooltips);
            
            if (!suppressTooltips && tooltipInfo != null && tooltipInfo.tooltip != null) {
                state.setHoveredTooltip(tooltipInfo.tooltip);
                state.setHoveredTooltipText(tooltipInfo.tooltipText);
                state.setTooltipX(tooltipInfo.x);
                state.setTooltipY(tooltipInfo.y);
            }
            
            modY += 30;
            
            modY += renderSubOptions(client, module, contentX + 20, modY, contentX + contentWidth - 20,
                                   visibleHeight, coords.mouseX, coords.mouseY, suppressTooltips);
        }
        
        guiGraphics.disableScissor();
        
        if (calculatedContentHeight > visibleHeight) {
            renderScrollbar(scrollbarInfo, contentX + contentWidth);
        }
        
        renderWatermark(client, coords.mouseX, coords.mouseY);
        renderHudButton(client, coords.mouseX, coords.mouseY);
        
        updateVersionCheckStatus();
        renderVersion(client, coords.mouseX, coords.mouseY);

        if (openingClipActive) {
            guiGraphics.disableScissor();
        }
        
        matrices.popMatrix();
        
        if (state.getHoveredTooltip() != null) {
            renderTooltip(client, mouseX, mouseY);
            tooltipAnimator.endFrame(true);
        } else {
            tooltipAnimator.endFrame(false);
        }
    }
    
    private void renderOpenBackdrop(Minecraft client, long openElapsedMs) {
        float opacity = ClickGuiOpenAnimation.backdropOpacity(openElapsedMs);
        if (opacity <= 0.01f || client == null || client.getWindow() == null) {
            return;
        }

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int topAlpha = (int) (0x80 * opacity);
        int bottomAlpha = (int) (0x90 * opacity);
        int topColor = (topAlpha << 24) | 0x101010;
        int bottomColor = (bottomAlpha << 24) | 0x101010;
        int midY = screenH / 2;
        guiGraphics.fill(0, 0, screenW, midY, topColor);
        guiGraphics.fill(0, midY, screenW, screenH, bottomColor);
    }

    private void renderWindowBackground() {
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT,
                                 theme.BG.getRGB(), 6f);
    }
    
    private List<Module> getFilteredModules() {
        String searchText = state.getSearchInput().getText().toLowerCase().trim();
        ModuleCategory selectedCategory = state.getSelectedCategory();
        int modulesRevision = ModuleManager.getModulesRevision();
        
        if (cachedFilteredModules != null && 
            cachedSearchText != null && cachedSearchText.equals(searchText) &&
            cachedCategory == selectedCategory &&
            cachedModulesRevision == modulesRevision) {
            return cachedFilteredModules;
        }
        
        List<Module> modules = ClickGuiSearchUtils.filterModules(searchText, selectedCategory);
        
        cachedFilteredModules = modules;
        cachedSearchText = searchText;
        cachedCategory = selectedCategory;
        cachedModulesRevision = modulesRevision;
        
        return modules;
    }

    private void renderSidebar(Minecraft client, float mouseX, float mouseY) {
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 0, 0, (int)ClickGuiState.SIDEBAR_WIDTH, (int)ClickGuiState.HEIGHT, 
                                 theme.BG.getRGB(), 0f);
        
        renderLogoAndTitle(client, mouseX, mouseY);
        
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
                    ((categoryBgColor >> 16) & 0xFF) - 10,
                    ((categoryBgColor >> 8) & 0xFF) - 10,
                    (categoryBgColor & 0xFF) - 10,
                    255
                ).getRGB();
                com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, normalBg, 0f);
            }
            
            String label = category.getDisplayName();
            int textX = 15;
            int textY = (int)categoryY;
            int color = active ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
            
            guiGraphics.text(client.font, label, textX, textY, color, false);
            
            categoryY += categorySpacing;
        }
        
        renderGitHubIcon(client, mouseX, mouseY);
    }
    
    private void renderLogoAndTitle(Minecraft client, float mouseX, float mouseY) {
        float availableHeight = ClickGuiState.HEADER_HEIGHT + 20;
        float availableWidth = ClickGuiState.SIDEBAR_WIDTH;
        
        int logoSize = (int)(Math.min(availableWidth, availableHeight) * 1.3f);
        
        int logoX = (int)((availableWidth - logoSize) / 2);
        int logoY = (int)((availableHeight - logoSize) / 2);
        
        Identifier logoTexture = Identifier.fromNamespaceAndPath("baity", "textures/gui/logo.png");
        
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, logoTexture, logoX, logoY, 0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize);
    }
    
    private void renderGitHubIcon(Minecraft client, float mouseX, float mouseY) {
        int iconSize = 20;
        int padding = 8;
        int iconX = padding;
        int iconY = (int)(ClickGuiState.HEIGHT - iconSize - padding);
        
        boolean isHovered = mouseX >= iconX && mouseX < iconX + iconSize &&
                          mouseY >= iconY && mouseY < iconY + iconSize;
        
        float alpha = isHovered ? 1.0f : 0.7f;
        int color = (int)(alpha * 255) << 24 | 0xFFFFFF;
        
        Identifier githubIcon = Identifier.fromNamespaceAndPath("baity", "textures/gui/github.png");
        
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, githubIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize, color);
    }
    
    private void renderSearchBar(Minecraft client, float mouseX, float mouseY) {
        float iconSize = 12;
        float iconPadding = 4;
        float searchX = ClickGuiLayout.searchBarX();
        float searchY = ClickGuiLayout.searchBarY();
        float searchHeight = ClickGuiLayout.searchBarHeight();
        float searchWidth = ClickGuiLayout.searchBarWidth();
        
        Identifier searchIcon = Identifier.fromNamespaceAndPath("baity", "textures/gui/search.png");
        int iconX = (int)(searchX + iconPadding);
        int iconY = (int)(searchY + (searchHeight - iconSize) / 2);
        int iconColor = 0xFFFFFFFF;
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, searchIcon, iconX, iconY, 0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize, iconColor);
        
        float textStartX = ClickGuiLayout.searchBarTextStartX();
        int searchTextMaxWidth = Math.max(0, Math.round(
                ClickGuiLayout.searchBarX() + ClickGuiLayout.searchBarWidth() - textStartX - 4f));
        
        boolean focused = state.isSearchFocused();
        LineTextInput search = state.getSearchInput();
        String searchText = search.getText();
        boolean empty = searchText.isEmpty();

        int textX = (int) textStartX;
        int textY = (int) (searchY + 6);
        if (focused) {
            if (empty) {
                guiGraphics.text(client.font, "Search...", textX, textY, theme.FONT.getRGB(), false);
            }
            LineTextInput.drawTextWithBlinkCursor(
                guiGraphics,
                client.font,
                empty ? "" : searchText,
                search.getCaretCp(),
                textX,
                textY,
                theme.FONT_C.getRGB(),
                true,
                LineTextInput.shouldBlinkCursor(),
                searchTextMaxWidth
            );
        } else if (!empty) {
            LineTextInput.drawTextWithBlinkCursor(
                guiGraphics,
                client.font,
                searchText,
                0,
                textX,
                textY,
                theme.FONT_C.getRGB(),
                false,
                false,
                searchTextMaxWidth
            );
        } else {
            guiGraphics.text(client.font, "Search...", textX, textY, theme.FONT.getRGB(), false);
        }
        
        float lineY = searchY + searchHeight - 1;
        int lineColor = new java.awt.Color(150, 150, 150, 200).getRGB();
        guiGraphics.fill((int)searchX, (int)lineY, (int)(searchX + searchWidth), (int)(lineY + 1), lineColor);
    }
    
    private void renderHudButton(Minecraft client, float mouseX, float mouseY) {
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
        guiGraphics.text(client.font, hudButtonText, hudTextX, hudTextY, theme.FONT.getRGB(), false);
        
        state.setHudButtonBounds((int)hudButtonX, (int)hudButtonY, (int)hudButtonWidth, (int)hudButtonHeight);
    }
    
    private void renderPlaceholder(Minecraft client, float modY, float contentX, float contentWidth) {
        String placeholderText = "nothing~~~";
        int textWidth = client.font.width(placeholderText);
        int textX = (int)(contentX + (contentWidth - textWidth) / 2);
        int textY = (int)(modY + 50);
        guiGraphics.text(client.font, placeholderText, textX, textY, theme.FONT.getRGB(), false);
    }
    
    private void renderModule(Minecraft client, Module module,
                             float x, float modY, float width,
                             float mouseX, float mouseY, float visibleTop, float visibleBottom,
                             boolean suppressTooltips) {
        float x2 = x + (width - x);
        float moduleHeight = 25f;
        
        boolean visible = modY + moduleHeight >= visibleTop && modY <= visibleBottom;
        boolean hovered = visible && mouseX >= x && mouseY >= modY && mouseX <= x2 && mouseY <= modY + moduleHeight;
        
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
        guiGraphics.text(client.font, displayName, (int)(x + 10), (int)(modY + 8), theme.FONT_C.getRGB(), false);
        
        if (hovered && !suppressTooltips && tooltipInfo != null) {
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
            String arrow = module.isExpanded() ? "\u25bc" : "\u25b6";
            guiGraphics.text(client.font, arrow, (int)(x2 - 25), (int)(modY + 8), theme.FONT_C.getRGB(), false);
        }
        
        if (module.getName().equals("ClickGUI")) {
            renderKeybindBox(client, x, modY, x2, moduleHeight, mouseX, mouseY);
        }
    }
    
    private void renderKeybindBox(Minecraft client,
                                   float containerX1, float containerY, float containerX2, float containerHeight,
                                   float mouseX, float mouseY) {
        boolean isListening = state.isListeningForKey();
        String keyDisplay = state.getCurrentKeyDisplay();
        String keyText = isListening ? "Press a key..." : keyDisplay;
        renderKeybindBoxContent(client, containerX2, containerY, containerHeight, mouseX, mouseY, isListening, keyText);
    }
    
    private void renderKeybindBoxContent(Minecraft client,
                                        float containerX2, float containerY, float containerHeight,
                                        float mouseX, float mouseY, boolean isListening, String displayText) {
        if (displayText == null || displayText.isEmpty()) {
            displayText = "\u2604 NOTSET";
        }
        String plainText = displayText.replaceAll("\u00a7[0-9a-fklmnor]", "");
        int textWidth = client.font.width(plainText);
        int boxWidth = textWidth + 16;
        float boxCenterY = containerY + containerHeight / 2f;
        int boxHeight = 12;
        
        int boxX1 = (int)(containerX2 - boxWidth - 10);
        int boxY1 = (int)(boxCenterY - boxHeight / 2f);
        int boxX2 = (int)(containerX2 - 10);
        int boxY2 = (int)(boxCenterY + boxHeight / 2f);
        
        boolean boxHovered = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, mouseX, mouseY);
        int boxBgColor = boxHovered
            ? new java.awt.Color(58, 58, 68, 255).getRGB()
            : new java.awt.Color(36, 36, 44, 255).getRGB();
        int boxBorderColor = new java.awt.Color(82, 82, 94, 255).getRGB();
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, boxX1, boxY1, boxX2, boxY2, boxBgColor, 0f);
        com.shyeuar.baity.gui.render.GuiRenderUtil.stroke1px(guiGraphics, boxX1, boxY1, boxX2, boxY2, boxBorderColor);
        
        int baseX = boxX1 + 8;
        int baseY = (int)(boxCenterY - 4);
        
        if (isListening) {
            String hintText = "Press Backspace to reset";
            int hintColor = 0xFFFFFF00;
            int hintX = boxX1 - client.font.width(hintText) - 8;
            guiGraphics.text(client.font, hintText, hintX, baseY, hintColor, false);
            
            net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
            guiGraphics.text(client.font, textObj, baseX, baseY, theme.FONT_C.getRGB(), false);
        } else {
            String displayPlainText = displayText.replaceAll("\u00a7[0-9a-fklmnor]", "");
            
            if (displayPlainText.startsWith("\u270e")) {
                String prefix = "\u270e";
                String keyName = displayPlainText.substring(1);
                int prefixRGB = com.shyeuar.baity.utils.KeyMappingUtils.getModuleEnabledPurpleRGB();
                int keyNameRGB = theme.FONT.getRGB();
                
                net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
                guiGraphics.text(client.font, prefixText, baseX, baseY, prefixRGB, false);
                
                int prefixWidth = client.font.width(prefix);
                net.minecraft.network.chat.Component keyTextObj = net.minecraft.network.chat.Component.literal(keyName);
                guiGraphics.text(client.font, keyTextObj, baseX + prefixWidth, baseY, keyNameRGB, false);
            } else if (displayPlainText.startsWith("\u2604") || 
                       displayPlainText.toUpperCase().contains("NOTSET") || 
                       displayPlainText.toUpperCase().contains("UNKNOWN")) {
                String prefix = "\u2604";
                String notsetText = displayPlainText.startsWith("\u2604") ? displayPlainText.substring(1) : (" " + displayPlainText);
                int prefixRGB = 0xFFFFFF00;
                int notsetRGB = 0xFFAAAAAA;
                
                net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
                guiGraphics.text(client.font, prefixText, baseX, baseY, prefixRGB, false);
                
                int prefixWidth = client.font.width(prefix);
                net.minecraft.network.chat.Component notsetTextObj = net.minecraft.network.chat.Component.literal(notsetText);
                guiGraphics.text(client.font, notsetTextObj, baseX + prefixWidth, baseY, notsetRGB, false);
            } else {
                net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
                guiGraphics.text(client.font, textObj, baseX, baseY, theme.FONT.getRGB(), false);
            }
        }
    }
    
    private float renderSubOptions(Minecraft client, Module module,
                                  float containerX1, float modY, float containerX2,
                                  float visibleHeight,
                                  float mouseX, float mouseY, boolean suppressTooltips) {
        java.util.List<ValueTreeUtils.ValueEntry> entries = ClickGuiMotion.getVisibleEntries(module, state);
        int subOptionCount = entries.size();
        
        if (subOptionCount == 0) return 0;
        
        float expandProgress = ClickGuiMotion.getModuleExpandProgress(state, module);
        if (expandProgress <= 0.0f) return 0;
        
        float fullContainerHeight = ClickGuiMotion.calculateEntriesHeight(entries, state, module, visibleHeight);
        int containerHeight = (int)(fullContainerHeight * expandProgress);

        int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);

        int containerBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
        float containerY1 = (float) Math.floor(modY);
        float containerY2 = containerY1 + containerHeight;
        
        com.shyeuar.baity.gui.render.GuiRenderUtil.draw3DRect(guiGraphics, containerX1, containerY1, containerX2, containerY2, containerBg, 0f);
        com.shyeuar.baity.gui.render.GuiRenderUtil.stroke1px(guiGraphics, containerX1, containerY1, containerX2, containerY2,
                               com.shyeuar.baity.gui.theme.LinearTheme.BORDER_PRIMARY.getRGB());
        
        int innerVisible = Math.max(0, containerHeight - dims.padding * 2);
        if (innerVisible >= dims.subOptionHeight / 2) {
            guiGraphics.enableScissor(
                    (int) containerX1,
                    (int) containerY1,
                    (int) containerX2,
                    (int) containerY2
            );
            float subModY = containerY1 + dims.padding;
            
            Value previousValue = null;
            for (ValueTreeUtils.ValueEntry entry : entries) {
                Value value = entry.value();
                int depth = entry.depth();
                
                if (value.needsSeparatorBefore(previousValue)) {
                    float separatorFactor = ClickGuiMotion.getEntryGroupFactor(state, module, entry);
                    subModY += 12 * separatorFactor;
                }
                
                float currentHeight = dims.subOptionHeight;
                if (value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.COLOR_PALETTE) {
                    currentHeight = dims.subOptionHeight * 2;
                } else if (value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.FANCY_DMG_PRESET) {
                    currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
                } else if (value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.GRADIENT_EDITOR
                        || value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.FANCY_DMG_COLOR_EDITOR
                        || value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                        || value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) {
                    currentHeight = dims.subOptionHeight * 6;
                } else if (value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.CROSSHAIR_PAINTER) {
                    currentHeight = dims.subOptionHeight * 8;
                }

                float groupFactor = ClickGuiMotion.getEntryGroupFactor(state, module, entry);
                float effectiveHeight = currentHeight * groupFactor;
                if (effectiveHeight <= 0.01f) {
                    previousValue = value;
                    continue;
                }
                if (subModY + effectiveHeight < ClickGuiState.HEADER_HEIGHT ||
                    subModY > ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT) {
                    subModY += effectiveHeight;
                    previousValue = value;
                    continue;
                }
                
                float localAlphaF = Math.min(1f, Math.max(0f, 
                    (innerVisible - (subModY - containerY1 - dims.padding)) / (float)dims.subOptionHeight));
                int localAlpha = (int)(255 * expandProgress * groupFactor * localAlphaF);
                
                int subX1 = (int)(containerX1 + 4 + depth * 12);
                int subX2 = (int)(containerX2 - 4 - depth * 8);
                
                float drawY = (float) Math.floor(subModY);
                float drawH = (float) Math.floor(effectiveHeight);
                guiGraphics.enableScissor(subX1, (int) drawY, subX2, (int) (drawY + drawH));
                ValueStyleRenderer.renderValue(
                        guiGraphics,
                        client,
                        module,
                        value,
                        theme,
                        subX1,
                        drawY,
                        subX2,
                        dims.subOptionHeight,
                        mouseX,
                        mouseY,
                        localAlpha,
                        getTooltipText,
                        getTooltipTextWithColors,
                        getDisplayTextFormatter,
                        state.getListeningButtonValueName(),
                        tooltipInfo,
                        state.getEditingSlider(),
                        state.getSliderInput().getText(),
                        state.getEditingGradient(),
                        state.getGradientInput().getText(),
                        state.getGradientInput().getCaretCp(),
                        state.getSliderInput().getCaretCp(),
                        state.getEditingTextInput(),
                        state.getTextLineInput().getText(),
                        state.getTextLineInput().getCaretCp()
                );
                guiGraphics.disableScissor();
                
                
                if (!suppressTooltips && tooltipInfo != null && tooltipInfo.tooltip != null) {
                    state.setHoveredTooltip(tooltipInfo.tooltip);
                    state.setHoveredTooltipText(tooltipInfo.tooltipText);
                    state.setTooltipX(tooltipInfo.x);
                    state.setTooltipY(tooltipInfo.y);
                }
                
                subModY += effectiveHeight;
                previousValue = value;
            }
            guiGraphics.disableScissor();
        }
        
        return containerHeight + 5;
    }
    
    private void renderScrollbar(ClickGuiLayout.ScrollbarInfo info, float contentRight) {
        float barX1 = contentRight - 6;
        float barX2 = contentRight - 2;
        com.shyeuar.baity.gui.render.GuiRenderUtil.drawRoundedRect(guiGraphics, barX1, info.barY, barX2, 
                                     info.barY + info.barHeight, 2, theme.BG_2.getRGB());
    }
    
    private void renderWatermark(Minecraft client, float mouseX, float mouseY) {
        ClickGuiWatermark.Layout layout = ClickGuiWatermark.layout(client);
        String prefix = ClickGuiWatermark.PREFIX;
        String handleName = ClickGuiWatermark.handleName();
        float wmScale = ClickGuiWatermark.SCALE;

        float baseX = layout.baseX();
        float baseY = layout.baseY();
        float handleX1 = layout.handleX1();
        float handleX2 = layout.handleX2();
        float lineY = layout.lineY();

        int baseColor = new java.awt.Color(120, 124, 132).getRGB();
        int hoverColor = 0xFFFFFF00;

        boolean isHovered = ClickGuiWatermark.isHandleHovered(mouseX, mouseY, layout);

        int prefixTextColor = baseColor;
        int handleTextColor = isHovered ? hoverColor : baseColor;
        int underlineColor = (isHovered ? hoverColor : baseColor) & 0xFFFFFF | 0x64000000;

        var matrices = guiGraphics.pose();
        matrices.pushMatrix();
        matrices.scale(wmScale, wmScale);
        guiGraphics.text(client.font, prefix,
            (int)(baseX / wmScale), (int)(baseY / wmScale), prefixTextColor, false);
        guiGraphics.text(client.font, handleName,
            (int)(handleX1 / wmScale), (int)(baseY / wmScale), handleTextColor, false);
        matrices.popMatrix();

        guiGraphics.fill((int)handleX1, (int)lineY, (int)handleX2, (int)lineY + 1, underlineColor);
    }
    
    private void renderVersion(Minecraft client, float mouseX, float mouseY) {
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
        
        float baseX = ClickGuiState.WIDTH - scaledCurrentVersionWidth - VERSION_RIGHT_PADDING;
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
            String checkingText;
            switch (phase) {
                case 1:
                    checkingText = "Checking..";
                    break;
                case 2:
                    checkingText = "Checking...";
                    break;
                default:
                    checkingText = "Checking.";
                    break;
            }
            
            int textWidth = client.font.width(checkingText);
            float scaledTextWidth = versionScale * textWidth;
            float renderX = baseX + scaledCurrentVersionWidth - scaledTextWidth;
            
            guiGraphics.text(client.font, checkingText,
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
                        displayText = "It's already the latest version\uff01";
                    }
                } else if ("error".equals(checkStatus)) {
                    showFeedback = true;
                    displayText = "Network error\uff01";
                    isError = true;
                } else if ("update_available".equals(checkStatus)) {
                    showFeedback = true;
                    String latest = state.getLatestVersion();
                    if (latest != null) {
                        displayText = "Available updates\uff01Check " + latest + "\uff01";
                    } else {
                        displayText = "Available updates\uff01";
                    }
                }
            }
        }
        
        int displayTextWidth = measureVersionTextWidth(client, displayText);
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
        
        int textX = (int)(renderX / versionScale);
        int textY = (int)(baseY / versionScale);
        if (showFeedback && "update_available".equals(checkStatus)) {
            String latest = state.getLatestVersion();
            if (latest != null) {
                String prefix = "Available updates\uff01Check ";
                String suffix = "\uff01";
                int prefixWidth = client.font.width(prefix);
                int versionWidth = client.font.width(latest);
                
                guiGraphics.text(client.font, prefix, textX, textY, 0xFFFFFF00, false);
                guiGraphics.text(client.font, latest,
                        (int)((renderX + prefixWidth * versionScale) / versionScale), textY, 0xFF00FF00, false);
                guiGraphics.text(client.font, suffix,
                        (int)((renderX + (prefixWidth + versionWidth) * versionScale) / versionScale), textY, 0xFFFFFF00, false);
            } else {
                guiGraphics.text(client.font, displayText, textX, textY, versionColor, false);
            }
        } else if (showFeedback && isError) {
            guiGraphics.text(client.font, displayText, textX, textY,
                    0xFFFF6B6B, false);
        } else if (showFeedback) {
            guiGraphics.text(client.font, displayText, textX, textY, versionColor, false);
        } else {
            guiGraphics.text(client.font, displayText,
                    (int)(baseX / versionScale), textY,
                    versionColor, false);
        }
        
        matrices.popMatrix();
        
        if (!showFeedback) {
            float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
            float lineX1 = baseX;
            float lineX2 = baseX + scaledCurrentVersionWidth;
            int lineColor = (versionColor & 0xFFFFFF) | 0x64000000;
            guiGraphics.fill((int)lineX1, (int)lineY, (int)lineX2, (int)lineY + 1, lineColor);
        } else if (showFeedback && "update_available".equals(checkStatus)) {
            String latest = state.getLatestVersion();
            if (latest != null) {
                String prefix = "Available updates\uff01Check ";
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

    private static int measureVersionTextWidth(Minecraft client, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int measured = client.font.width(text);
        int perGlyph = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            perGlyph += client.font.width(Character.toString(codePoint));
            i += Character.charCount(codePoint);
        }
        return Math.max(measured, perGlyph);
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
    
    public void resetTooltipAnimation() {
        tooltipAnimator.reset();
    }

    private void renderTooltip(Minecraft client, double mouseX, double mouseY) {
        float tooltipScaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        float tipScale = 0.75f * tooltipScaleRatio;
        int offsetFromCursor = (int)(3 * tooltipScaleRatio);

        net.minecraft.network.chat.Component coloredText = state.getHoveredTooltipText();
        String plainText;
        if (coloredText != null) {
            plainText = coloredText.getString();
        } else if (state.getHoveredTooltip() != null) {
            plainText = state.getHoveredTooltip();
        } else {
            return;
        }

        String[] lines = plainText.split("\n", -1);
        if (lines.length == 0) {
            return;
        }

        int bgPadding = 10;
        int rawFontHeight = 9;
        int lineSpacing = 2;
        int contentHeight = lines.length * rawFontHeight + Math.max(0, lines.length - 1) * lineSpacing;

        int rawTextWidth = 0;
        for (String line : lines) {
            rawTextWidth = Math.max(rawTextWidth, client.font.width(line));
        }

        int rawTooltipWidth = rawTextWidth + bgPadding;
        int rawTooltipHeight = contentHeight + 8;

        int signature = 31 * plainText.hashCode() + 31 * rawTooltipWidth + rawTooltipHeight;
        var sizeFrame = tooltipAnimator.update(signature, rawTooltipWidth, rawTooltipHeight);
        int drawTooltipWidth = sizeFrame.animateBackground()
                ? Math.max(1, Math.round(sizeFrame.animatedWidth()))
                : rawTooltipWidth;
        int drawTooltipHeight = sizeFrame.animateBackground()
                ? Math.max(1, Math.round(sizeFrame.animatedHeight()))
                : rawTooltipHeight;

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
                                      drawTooltipWidth, drawTooltipHeight,
                                      4, theme.BG_2.getRGB());

        boolean clipText = sizeFrame.needsTextClip();
        if (clipText) {
            guiGraphics.nextStratum();
            guiGraphics.enableScissor(0, 0, drawTooltipWidth, drawTooltipHeight);
        }

        int textX = bgPadding / 2;
        int textY = clipText ? 4 : (rawTooltipHeight - contentHeight) / 2;

        int textColor = theme.FONT_C.getRGB() | 0xFF000000;
        if (coloredText != null && coloredText.getStyle().getColor() != null) {
            textColor = coloredText.getStyle().getColor().getValue() | 0xFF000000;
        }

        if (lines.length == 1 && coloredText != null) {
            guiGraphics.text(client.font, coloredText, textX, textY, 0xFFFFFFFF, false);
        } else {
            for (int i = 0; i < lines.length; i++) {
                int lineY = textY + i * (rawFontHeight + lineSpacing);
                guiGraphics.text(client.font, lines[i], textX, lineY, textColor, false);
            }
        }

        if (clipText) {
            guiGraphics.disableScissor();
        }

        guiMatrices.popMatrix();
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
        
        float modY = contentY + 10 - state.getLayoutScrollOffset();
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
            modY += getSubOptionContainerHeight(module);
        }
    }
    
    private float getSubOptionContainerHeight(Module module) {
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        return ClickGuiMotion.calculateModuleSubOptionsHeight(module, state, visibleHeight);
    }
    
    public ClickGuiState getState() {
        return state;
    }
    
    public Theme getTheme() {
        return theme;
    }
}
