package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.EnchantLoreColorEditorValue;
import com.shyeuar.baity.gui.value.ChromaFishingLineColorEditorValue;
import com.shyeuar.baity.gui.value.GradientEditorValue;
import com.shyeuar.baity.features.enchantlore.EnchantLore;
import com.shyeuar.baity.gui.value.SliderValue;
import com.shyeuar.baity.gui.input.LineTextInput;
import com.shyeuar.baity.gui.value.TextLineInputValue;
import com.shyeuar.baity.gui.value.ValueTreeUtils;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import com.shyeuar.baity.gui.value.CrosshairPainterValue;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.TimerUtils;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.SoundUtils;
import com.shyeuar.baity.utils.VersionCheckUtils;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;

public class ClickGuiInputHandler {
    private static final float VERSION_RIGHT_PADDING = 8.0f;
    
    private final ClickGuiState state;
    private final TimerUtils timer;
    private final BiConsumer<com.shyeuar.baity.gui.module.Module, com.shyeuar.baity.gui.value.ButtonValue> onTriggerValueClick;
    private String painterDragModule = null;
    private String painterDragValue = null;
    private int painterDragButton = -1;
    private int painterLastPx = Integer.MIN_VALUE;
    private int painterLastPy = Integer.MIN_VALUE;
    public ClickGuiInputHandler(ClickGuiState state, TimerUtils timer, 
                               BiConsumer<com.shyeuar.baity.gui.module.Module, com.shyeuar.baity.gui.value.ButtonValue> onTriggerValueClick) {
        this.state = state;
        this.timer = timer;
        this.onTriggerValueClick = onTriggerValueClick;
    }
   
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if ((state.isListeningForKey() || state.getListeningButtonValueName() != null) && button >= 2 && button <= 4) {
            return handleMouseKeybindBinding(button);
        }
        
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        if (button == 0 && state.isHudButtonHovered(coords.mouseX, coords.mouseY)) {
            SoundUtils.playBubble();
            Minecraft.getInstance().setScreen(new com.shyeuar.baity.gui.hud.HudPositionEditor());
            return true;
        }

        if (handleSearchInput(coords, button)) {
            return true;
        }

        if (button == 0 && state.isEditingGradient()) {
            if (!isClickInsideEditingGradientInput(coords)) {
                state.setEditingGradient(null);
            }
        }
        if (button == 0 && state.isEditingTextInput()) {
            if (!isClickInsideEditingTextInput(coords)) {
                state.setEditingTextInput(null);
            }
        }

        if (handleWatermarkClick(coords, button)) {
            return true;
        }
        
        if (state.isEditingSlider() && button == 0) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null) {
                boolean clickedOnValueDisplay = false;
                float modY = 60 - state.getScrollOffset();
                List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
                for (Module module : modules) {
                    if (!module.getName().equals(editInfo.moduleName)) {
                        if (module.isExpanded()) {
                            modY += getSubOptionContainerHeight(module);
                        }
                        modY += 30;
                        continue;
                    }
                    if (module.isExpanded()) {
                        java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
                        int subOptionCount = entries.size();
                        if (subOptionCount == 0) break;
                        
                        int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
                        ClickGuiLayout.ContainerDimensions dims = 
                            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
                            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING, extraHeight);
                        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
                        float subModY = modY + dims.padding;
                        
                        Value previousValue = null;
                        for (ValueTreeUtils.ValueEntry entry : entries) {
                            Value v = entry.value();
                            int depth = entry.depth();
                            if (v.needsSeparatorBefore(previousValue)) {
                                subModY += 12;
                            }
                            if (v instanceof SliderValue && v.getName().equals(editInfo.valueName)) {
                                SliderValue sv = (SliderValue) v;
                                int resetBoxWidth = 30;
                                int subX2 = containerX2 - 4 - depth * 8;
                                int resetBoxX = subX2 - resetBoxWidth - 6;
                                String valueText = sv.getFormattedValue();
                                int valueTextWidth = Minecraft.getInstance().font.width(valueText);
                                int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
                                int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
                                int valueDisplayY = (int)(subModY + 2);
                                int valueDisplayHeight = dims.subOptionHeight - 4;
                                
                                if (GuiRenderUtil.isHovered(valueDisplayX, valueDisplayY, 
                                    valueDisplayX + valueDisplayWidth, valueDisplayY + valueDisplayHeight, 
                                    coords.mouseX, coords.mouseY)) {
                                    clickedOnValueDisplay = true;
                                }
                                break;
                            }
                            float currentHeight = dims.subOptionHeight;
                            if (v.getStyle() == ValueStyle.COLOR_PALETTE) {
                                currentHeight = dims.subOptionHeight * 2;
                            } else if (v.getStyle() == ValueStyle.FANCY_DMG_PRESET) {
                                currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
                            } else if (v.getStyle() == ValueStyle.GRADIENT_EDITOR
                                    || v.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                                    || v.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                                    || v.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) {
                                currentHeight = dims.subOptionHeight * 6;
                            }
                            subModY += currentHeight;
                            previousValue = v;
                        }
                    }
                    break;
                }
                
                if (!clickedOnValueDisplay) {
                    cancelSliderInput();
                }
            }
        }
        
        if (button == 0 && handleWindowDrag(coords, mouseX, mouseY)) {
            return true;
        }

        if (handleCategoryClick(coords)) {
            return true;
        }
        
        if (handleGitHubIconClick(coords, button)) {
            return true;
        }

        if (handleVersionUpdateClick(coords, button)) {
            return true;
        }
        
        if (handleVersionClick(coords, button)) {
            return true;
        }
        
        return handleModuleAndSubOptionClick(coords, button);
    }
    
    private void cancelSliderInput() {
        if (state.isEditingSlider()) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null && state.getOriginalSliderValue() != null) {
                for (Module module : ModuleManager.getModules()) {
                    if (!module.getName().equals(editInfo.moduleName)) continue;
                    Value found = ValueTreeUtils.findByName(module, editInfo.valueName);
                    if (found instanceof SliderValue) {
                        SliderValue sliderValue = (SliderValue) found;
                            sliderValue.setValue(state.getOriginalSliderValue());
                    }
                }
            }
            state.setEditingSlider(null);
        }
    }
    
    public boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        float visibleBottom = ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        float visibleHeight = visibleBottom - contentY;
        
        if (GuiRenderUtil.isHovered(contentX, contentY, contentX + contentWidth, visibleBottom, 
                                    coords.mouseX, coords.mouseY)) {
            List<Module> modules = getFilteredModules();
            
            float calculatedContentHeight = ClickGuiLayout.calculateContentHeightForModules(modules, visibleHeight);
            float contentStartY = ClickGuiState.HEADER_HEIGHT + 10;
            float contentEndY = ClickGuiState.HEADER_HEIGHT + visibleHeight;
            float maxScroll = Math.max(0, calculatedContentHeight + contentStartY - contentEndY);
            
            float delta = (float)(-verticalAmount * 20);
            
            float clampedDelta = ClickGuiLayout.clampScrollDelta(state, maxScroll, delta);
            
            if (clampedDelta != 0) {
                state.setScrollOffset(state.getScrollOffset() + clampedDelta);
                
                float modY = contentY + 10 - state.getScrollOffset();
                for (Module module : modules) {
                    modY += 30;
                    if (module.isExpanded()) {
                        handleSubOptionScroll(module, modY, coords, verticalAmount, contentX, contentWidth);
                        modY += getSubOptionContainerHeight(module);
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (state.isSearchFocused()) {
            LineTextInput.KeyResult result = state.getSearchInput().handleKey(keyCode, modifiers);
            if (result == LineTextInput.KeyResult.CANCEL || result == LineTextInput.KeyResult.COMMIT) {
                state.setSearchFocused(false);
                return true;
            }
            if (result == LineTextInput.KeyResult.HANDLED) {
                return true;
            }
        }
        
        if (state.isListeningForKey()) {
            return handleClickGuiKeybindInput(keyCode);
        }
        
        if (state.getListeningButtonValueName() != null) {
            return handleButtonValueKeybindInput(keyCode);
        }
        
        if (state.isEditingSlider()) {
            syncSliderInputPolicy();
            LineTextInput.KeyResult result = state.getSliderInput().handleKey(keyCode, modifiers);
            if (result == LineTextInput.KeyResult.CANCEL) {
                cancelSliderInput();
                return true;
            }
            if (result == LineTextInput.KeyResult.COMMIT) {
                commitSliderInput();
                return true;
            }
            if (result == LineTextInput.KeyResult.HANDLED) {
                return true;
            }
        }
        if (state.isEditingGradient()) {
            syncGradientInputPolicy();
            LineTextInput.KeyResult result = state.getGradientInput().handleKey(keyCode, modifiers);
            if (result == LineTextInput.KeyResult.CANCEL) {
                state.setEditingGradient(null);
                return true;
            }
            if (result == LineTextInput.KeyResult.COMMIT) {
                commitGradientInput();
                return true;
            }
            if (result == LineTextInput.KeyResult.HANDLED) {
                return true;
            }
        }
        if (state.isEditingTextInput()) {
            LineTextInput.KeyResult result = state.getTextLineInput().handleKey(keyCode, modifiers);
            if (result == LineTextInput.KeyResult.CANCEL) {
                state.setEditingTextInput(null);
                return true;
            }
            if (result == LineTextInput.KeyResult.COMMIT) {
                commitTextLineInput();
                return true;
            }
            if (result == LineTextInput.KeyResult.HANDLED) {
                return true;
            }
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (state.isListeningForInput()) {
                state.setListeningForKey(false);
                state.setListeningButtonValueName(null);
                return true;
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        
        return false;
    }
    
    public boolean handleCharTyped(char chr, int modifiers) {
        return handleCodePointTyped((int) chr, modifiers);
    }

    public boolean handleCodePointTyped(int codePoint, int modifiers) {
        if (state.isEditingSlider()) {
            syncSliderInputPolicy();
            return state.getSliderInput().handleCodePoint(codePoint);
        }
        if (state.isEditingGradient()) {
            syncGradientInputPolicy();
            return state.getGradientInput().handleCodePoint(codePoint);
        }
        if (state.isEditingTextInput()) {
            return state.getTextLineInput().handleCodePoint(codePoint);
        }

        if (state.isSearchFocused()) {
            return state.getSearchInput().handleCodePoint(codePoint);
        }

        return false;
    }

    private void syncGradientInputPolicy() {
        ClickGuiState.GradientInputInfo editInfo = state.getEditingGradient();
        if (editInfo == null) {
            return;
        }
        state.getGradientInput().setPolicy(
            editInfo.symbolInput ? LineTextInput.Policy.damageSymbols() : LineTextInput.Policy.hexColor()
        );
    }

    private void commitGradientInput() {
        ClickGuiState.GradientInputInfo editInfo = state.getEditingGradient();
        if (editInfo == null) {
            return;
        }
        String raw = state.getGradientInput().getText();
        if (editInfo.symbolInput) {
            for (Module module : ModuleManager.getModules()) {
                if (!module.getName().equals(editInfo.moduleName)) continue;
                Value found = ValueTreeUtils.findByName(module, editInfo.valueName);
                if (found instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor) {
                    fancyEditor.setSymbols(raw);
                    fancyEditor.persistToConfig();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), fancyEditor.getValue());
                    }
                }
            }
        } else {
            String hex = raw.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.matches("^[0-9A-Fa-f]{6}$")) {
                for (Module module : ModuleManager.getModules()) {
                    if (!module.getName().equals(editInfo.moduleName)) continue;
                    Value found = ValueTreeUtils.findByName(module, editInfo.valueName);
                    if (found instanceof EnchantLoreColorEditorValue colorEditor) {
                        colorEditor.gradient().applyHexToSelected("#" + hex);
                        colorEditor.persistCurrentTier();
                        EnchantLore.invalidateCache();
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), found.getValue());
                        }
                    } else if (found instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor) {
                        fancyEditor.gradient().applyHexToSelected("#" + hex);
                        fancyEditor.persistToConfig();
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), fancyEditor.getValue());
                        }
                    } else if (found instanceof ChromaFishingLineColorEditorValue chromaEditor) {
                        chromaEditor.gradient().applyHexToSelected("#" + hex);
                        chromaEditor.persistToConfig();
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), chromaEditor.getValue());
                        }
                    } else if (found instanceof GradientEditorValue ge) {
                        ge.applyHexToSelected("#" + hex);
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), ge.getValue());
                        }
                    }
                }
            }
        }
        state.setEditingGradient(null);
    }

    private void commitTextLineInput() {
        ClickGuiState.TextInputInfo info = state.getEditingTextInput();
        if (info != null) {
            Module module = ModuleManager.getModuleByName(info.moduleName);
            if (module != null) {
                Value found = ValueTreeUtils.findByName(module, info.valueName);
                if (found instanceof TextLineInputValue) {
                    found.setValue(state.getTextLineInput().getText());
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), found.getValue());
                    }
                }
            }
        }
        state.setEditingTextInput(null);
    }

    private void beginGradientHexEdit(String moduleName, String valueName, int lineIndex, String initialHexWithoutHash) {
        state.getGradientInput().setPolicy(LineTextInput.Policy.hexColor());
        state.setEditingGradient(new ClickGuiState.GradientInputInfo(moduleName, valueName, lineIndex));
        if (initialHexWithoutHash == null || initialHexWithoutHash.isEmpty()) {
            state.getGradientInput().clear();
        } else {
            state.getGradientInput().setTextAndCaretAtEnd(initialHexWithoutHash);
        }
    }

    private void beginGradientSymbolEdit(String moduleName, String valueName, String symbols) {
        state.getGradientInput().setPolicy(LineTextInput.Policy.damageSymbols());
        state.setEditingGradient(new ClickGuiState.GradientInputInfo(moduleName, valueName, 0, true));
        state.getGradientInput().setTextAndCaretAtEnd(symbols == null ? "" : symbols);
    }

    private void syncSliderInputPolicy() {
        ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
        if (editInfo == null) {
            return;
        }
        for (Module module : ModuleManager.getModules()) {
            if (!module.getName().equals(editInfo.moduleName)) {
                continue;
            }
            Value found = ValueTreeUtils.findByName(module, editInfo.valueName);
            if (found instanceof SliderValue sliderValue) {
                state.getSliderInput().setPolicy(LineTextInput.Policy.forSlider(sliderValue));
                return;
            }
        }
    }

    private void beginSliderEdit(SliderValue sliderValue, String moduleName, String valueName) {
        state.getSliderInput().setPolicy(LineTextInput.Policy.forSlider(sliderValue));
        state.setEditingSlider(new ClickGuiState.SliderInputInfo(moduleName, valueName));
        state.getSliderInput().setTextAndCaretAtEnd(sliderValue.getFormattedValue());
        state.setOriginalSliderValue(sliderValue.getDoubleValue());
    }

    private void commitSliderInput() {
        ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
        if (editInfo != null) {
            for (Module module : ModuleManager.getModules()) {
                if (!module.getName().equals(editInfo.moduleName)) {
                    continue;
                }
                Value found = ValueTreeUtils.findByName(module, editInfo.valueName);
                if (found instanceof SliderValue sliderValue) {
                    if (LineTextInput.tryCommitSlider(sliderValue, state.getSliderInput().getText())) {
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), sliderValue.getValue());
                        }
                    }
                    break;
                }
            }
        }
        state.setEditingSlider(null);
    }

    public void handleMouseRelease(int button) {
        if (button == 0) {
            state.resetDragState();
            state.setDraggingSlider(null);
            state.setDraggingGradient(null);
        }
        if (button == painterDragButton || button == 1) {
            clearPainterDrag();
        }
    }
   
    public void handleMouseMove(double mouseX, double mouseY) {
        if (state.isDragging()) {
            ClickGuiLayout.updateWindowPosition(state, mouseX, mouseY, state.getDragX(), state.getDragY());
        }
        
        if (state.getDraggingSlider() != null) {
            handleSliderDrag(mouseX, mouseY);
        }
        if (state.getDraggingGradient() != null) {
            handleGradientDrag(mouseX, mouseY);
        }
        handlePainterDrag(mouseX, mouseY);
    }

    private void clearPainterDrag() {
        painterDragModule = null;
        painterDragValue = null;
        painterDragButton = -1;
        painterLastPx = Integer.MIN_VALUE;
        painterLastPy = Integer.MIN_VALUE;
    }

    private void handlePainterDrag(double mouseX, double mouseY) {
        if (painterDragModule == null || painterDragValue == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        long win = GLFW.glfwGetCurrentContext();
        boolean leftDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if ((painterDragButton == 0 && !leftDown) || (painterDragButton == 1 && !rightDown)) {
            clearPainterDrag();
            return;
        }

        Module module = ModuleManager.getModuleByName(painterDragModule);
        if (module == null || !module.isExpanded()) return;
        Value found = ValueTreeUtils.findByName(module, painterDragValue);
        if (!(found instanceof CrosshairPainterValue painter)) return;

        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        float modY = contentY + 10 - state.getScrollOffset();
        List<Module> modules = getFilteredModules();
        for (Module m : modules) {
            modY += 30;
            if (m != module) {
                if (m.isExpanded()) modY += getSubOptionContainerHeight(m);
                continue;
            }
            java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(m);
            int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
            float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
            ClickGuiLayout.ContainerDimensions dims = ClickGuiLayout.calculateSubOptionContainer(entries.size(), visibleHeight, extraHeight);
            float containerX1 = contentX + 20;
            float containerX2 = contentX + contentWidth - 20;
            float subModY = modY + dims.padding;
            Value previous = null;
            for (ValueTreeUtils.ValueEntry entry : entries) {
                Value v = entry.value();
                int depth = entry.depth();
                if (v.needsSeparatorBefore(previous)) subModY += 12;
                float currentHeight = dims.subOptionHeight;
                if (v.getStyle() == ValueStyle.COLOR_PALETTE) currentHeight = dims.subOptionHeight * 2;
                else if (v.getStyle() == ValueStyle.FANCY_DMG_PRESET) currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
                else if (v.getStyle() == ValueStyle.GRADIENT_EDITOR
                        || v.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                        || v.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                        || v.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) currentHeight = dims.subOptionHeight * 6;
                else if (v.getStyle() == ValueStyle.CROSSHAIR_PAINTER) currentHeight = dims.subOptionHeight * 8;
                if (v == found) {
                    int subX1 = (int)(containerX1 + 4 + depth * 12);
                    int subX2 = (int)(containerX2 - 4 - depth * 8);
                    com.shyeuar.baity.gui.render.ValueStyleRenderer.CrosshairPainterLayout l =
                        com.shyeuar.baity.gui.render.ValueStyleRenderer.computeCrosshairPainterLayout(mc, painter, subX1, subModY, subX2, dims.subOptionHeight);
                    if (!GuiRenderUtil.isHovered(l.canvasX1, l.canvasY1, l.canvasX2, l.canvasY2, coords.mouseX, coords.mouseY)) return;
                    int n = painter.getSize();
                    int px = (int)((coords.mouseX - l.gridX1) / Math.max(1, l.cellPx));
                    int py = (int)((coords.mouseY - l.gridY1) / Math.max(1, l.cellPx));
                    if (px < 0 || py < 0 || px >= n || py >= n) return;
                    if (px == painterLastPx && py == painterLastPy) return;
                    if (painterDragButton == 0) painter.togglePixel(px, py);
                    else painter.clearPixel(px, py);
                    painterLastPx = px;
                    painterLastPy = py;
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), painter.getValue());
                    }
                    return;
                }
                subModY += currentHeight;
                previous = v;
            }
            return;
        }
    }
    
    private void handleSliderDrag(double mouseX, double mouseY) {
        ClickGuiState.SliderDragInfo dragInfo = state.getDraggingSlider();
        if (dragInfo == null) return;
        
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        for (Module module : ModuleManager.getModules()) {
            if (!module.getName().equals(dragInfo.moduleName)) continue;
            
            Value found = ValueTreeUtils.findByName(module, dragInfo.valueName);
            if (found instanceof SliderValue) {
                SliderValue sliderValue = (SliderValue) found;
                    
                    double percentage = (coords.mouseX - dragInfo.sliderX) / (double) dragInfo.sliderWidth;
                    percentage = Math.max(0, Math.min(1, percentage));
                    sliderValue.setFromPercentage(percentage);
                    
                if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                    ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), sliderValue.getValue());
                    }
                    return;
                }
        }
    }

    private void handleGradientDrag(double mouseX, double mouseY) {
        ClickGuiState.GradientDragInfo dragInfo = state.getDraggingGradient();
        if (dragInfo == null) return;

        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        float hue = (float) ((coords.mouseX - dragInfo.mapX1) / Math.max(1f, (dragInfo.mapX2 - dragInfo.mapX1)));
        float sat = (float) (1f - (coords.mouseY - dragInfo.mapY1) / Math.max(1f, (dragInfo.mapY2 - dragInfo.mapY1)));

        for (Module module : ModuleManager.getModules()) {
            if (!module.getName().equals(dragInfo.moduleName)) continue;
            Value found = ValueTreeUtils.findByName(module, dragInfo.valueName);
            GradientEditorValue gradientValue = null;
            EnchantLoreColorEditorValue colorEditor = null;
            com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor = null;
            ChromaFishingLineColorEditorValue chromaEditor = null;
            if (found instanceof EnchantLoreColorEditorValue editor) {
                colorEditor = editor;
                gradientValue = editor.gradient();
            } else if (found instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue editor) {
                fancyEditor = editor;
                gradientValue = editor.gradient();
            } else if (found instanceof ChromaFishingLineColorEditorValue editor) {
                chromaEditor = editor;
                gradientValue = editor.gradient();
            } else if (found instanceof GradientEditorValue ge) {
                gradientValue = ge;
            }
            if (gradientValue != null) {
                if (dragInfo.dragValue) {
                    float valNorm = (float)(1f - (coords.mouseY - dragInfo.mapY1) / Math.max(1f, (dragInfo.mapY2 - dragInfo.mapY1)));
                    gradientValue.setSelectedValue(valNorm);
                } else {
                    gradientValue.setSelectedFromHueSat(hue, sat);
                }
                if (colorEditor != null) {
                    colorEditor.persistCurrentTier();
                    EnchantLore.invalidateCache();
                }
                if (fancyEditor != null) {
                    fancyEditor.persistToConfig();
                }
                if (chromaEditor != null) {
                    chromaEditor.persistToConfig();
                }
                if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                    ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), found.getValue());
                }
                return;
            }
        }
    }
    
    private boolean handleMouseKeybindBinding(int mouseKeyCode) {
        if (state.isListeningForKey()) {
            ConfigManager.guiKeyCode = mouseKeyCode;
            ConfigManager.saveConfig();
            updateKeyDisplay();
            state.setListeningForKey(false);
            return true;
        }
        
        if (state.getListeningButtonValueName() != null) {
            String listeningModule = state.getListeningButtonValueModule();
            String listeningName = state.getListeningButtonValueName();
            Module module = ModuleManager.getModuleByName(listeningModule);
            if (module != null) {
                Value found = ValueTreeUtils.findByName(module, listeningName);
                if (found instanceof ButtonValue) {
                    ButtonValue buttonValue = (ButtonValue) found;
                        buttonValue.setValue(mouseKeyCode);
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), mouseKeyCode);
                        }
                        state.clearListeningButtonValue();
                        return true;
                }
            }
            state.clearListeningButtonValue();
            return true;
        }
        
        return false;
    }
    
    private boolean handleWindowDrag(ClickGuiLayout.ScaledCoordinates coords, double mouseX, double mouseY) {
        if (GuiRenderUtil.isHovered(0, 0, ClickGuiState.WIDTH, 20, coords.mouseX, coords.mouseY)) {
            if (state.getDragX() == 0 && state.getDragY() == 0) {
                state.setDragX(coords.mouseX);
                state.setDragY(coords.mouseY);
            } else {
                ClickGuiLayout.updateWindowPosition(state, mouseX, mouseY, state.getDragX(), state.getDragY());
            }
            state.setDragging(true);
            return true;
        }
        return false;
    }
    
    private boolean handleVersionClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        long currentTime = System.currentTimeMillis();
        boolean inFeedback = state.getVersionCheckStatus() != null && 
                           currentTime - state.getVersionCheckStartTime() < 2000;
        
        if (inFeedback) {
            return false;
        }
        
        String currentVersion = getModVersion();
        if (currentVersion == null || currentVersion.isEmpty()) {
            currentVersion = "v1.1.7";
        } else {
            if (!currentVersion.startsWith("v") && !currentVersion.startsWith("V")) {
                currentVersion = "v" + currentVersion;
            }
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float versionScale = 0.70f;
        int versionRawWidth = client.font.width(currentVersion);
        float scaledWidth = versionScale * versionRawWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - VERSION_RIGHT_PADDING;
        float baseY = ClickGuiState.HEIGHT - 8 - (int)(client.font.lineHeight * versionScale) - 2;
        float baseHeight = (int)(client.font.lineHeight * versionScale);
        float lineY = baseY + baseHeight + 1;
        float lineHeight = 1;
        
        float expandedBottom = lineY + lineHeight + 3;
        if (coords.mouseX >= baseX && coords.mouseX <= baseX + scaledWidth &&
            coords.mouseY >= baseY && coords.mouseY <= expandedBottom) {
            
            if (state.isVersionChecking()) {
                return true;
            }
            
            state.setVersionChecking(true);
            state.setVersionCheckStatus(null);
            state.setLatestVersion(null);
            state.setAutoCheck(false);
            state.setVersionCheckStartTime(System.currentTimeMillis());
            Minecraft mc = client;
            VersionCheckUtils.checkVersionAsync(currentVersion).thenAccept(result -> {
                mc.schedule(() -> {
                    state.setVersionChecking(false);
                    if (result.hasError) {
                        state.setVersionCheckStatus("error");
                        state.setLatestVersion(null);
                        state.setVersionCheckStartTime(System.currentTimeMillis());
                        return;
                    }
                    
                    if (result.isLatest) {
                        state.setVersionCheckStatus("latest");
                    } else {
                        state.setVersionCheckStatus("update_available");
                        state.setLatestVersion(result.latestVersion);
                    }
                    state.setVersionCheckStartTime(System.currentTimeMillis());
                });
            }).exceptionally(throwable -> {
                client.schedule(() -> state.setVersionChecking(false));
                return null;
            });
            
            return true;
        }
        
        return false;
    }
    
    private boolean handleVersionUpdateClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        long currentTime = System.currentTimeMillis();
        if (state.getVersionCheckStatus() == null || 
            !"update_available".equals(state.getVersionCheckStatus()) ||
            currentTime - state.getVersionCheckStartTime() >= 2000) {
            return false;
        }
        
        String latest = state.getLatestVersion();
        if (latest == null || latest.isEmpty()) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float versionScale = 0.70f;
        String prefix = "Available updates！Check ";
        String suffix = "！";
        String displayText = prefix + latest + suffix;
        int displayTextWidth = client.font.width(displayText);
        float scaledWidth = versionScale * displayTextWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - VERSION_RIGHT_PADDING;
        float baseY = ClickGuiState.HEIGHT - 8 - (int)(client.font.lineHeight * versionScale) - 2;
        float baseHeight = (int)(client.font.lineHeight * versionScale);
        
        int prefixWidth = client.font.width(prefix);
        float scaledPrefixWidth = versionScale * prefixWidth;
        float versionX = baseX + scaledPrefixWidth;
        float versionY = baseY;
        float scaledVersionWidth = versionScale * client.font.width(latest);
        
        if (coords.mouseX >= versionX && coords.mouseX <= versionX + scaledVersionWidth &&
            coords.mouseY >= versionY && coords.mouseY <= versionY + baseHeight) {
            
            try {
                String latestTag = latest;
                if (!latestTag.startsWith("v") && !latestTag.startsWith("V")) {
                    latestTag = "v" + latestTag;
                }
                String releaseTag = "baity-1.21.11-" + latestTag;
                net.minecraft.util.Util.getPlatform().openUri(new java.net.URI("https://github.com/raueyhs/Baity/releases/tag/" + releaseTag));
                return true;
            } catch (Exception e) {
                if (client.player != null) {
                    client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("无法打开浏览器，请手动访问: https://github.com/raueyhs/Baity/releases/tag/baity-1.21.11-" + latest),
                        false
                    );
                }
                return true;
            }
        }
        
        return false;
    }
    
    private String getModVersion() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            java.util.Optional<net.fabricmc.loader.api.ModContainer> modContainer = loader.getModContainer("baity");
            if (modContainer.isPresent()) {
                String version = modContainer.get().getMetadata().getVersion().getFriendlyString();
                if (!version.startsWith("v") && !version.startsWith("V")) {
                    version = "v" + version;
                }
                return version;
            }
        } catch (Exception e) {
        }
        return "v1.1.7";
    }
    
    private boolean handleSearchInput(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) {
            return false;
        }

        if (ClickGuiLayout.isSearchBarHovered(coords.mouseX, coords.mouseY)) {
            float textStartX = ClickGuiLayout.searchBarTextStartX();
            Minecraft client = Minecraft.getInstance();
            state.setSearchFocused(true);
            if (client != null) {
                state.getSearchInput().onMousePressed(client.font, coords.mouseX - textStartX);
            }
            return true;
        }

        if (state.isSearchFocused()) {
            state.setSearchFocused(false);
        }

        return false;
    }
    
    private boolean handleGitHubIconClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        int iconSize = 20;
        int padding = 8;
        int iconX = padding;
        int iconY = (int)(ClickGuiState.HEIGHT - iconSize - padding);
        
        if (coords.mouseX >= iconX && coords.mouseX < iconX + iconSize &&
            coords.mouseY >= iconY && coords.mouseY < iconY + iconSize) {
            
            try {
                net.minecraft.util.Util.getPlatform().openUri(new java.net.URI("https://github.com/raueyhs/Baity"));
                return true;
            } catch (Exception e) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("无法打开浏览器，请手动访问: https://github.com/raueyhs/Baity"),
                        false
                    );
                }
                return true;
            }
        }
        
        return false;
    }
    
    private boolean handleWatermarkClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;

        String prefix = "Baity by ";
        String handleName = "@11YearCookieBuff";
        float wmScale = 0.70f;

        int prefixWidth = client.font.width(prefix);
        int handleNameWidth = client.font.width(handleName);

        float totalScaledWidth = wmScale * (prefixWidth + handleNameWidth);
        float baseX = ClickGuiState.WIDTH - totalScaledWidth - 8;
        float baseY = 8;

        float handleX1 = baseX + wmScale * prefixWidth;
        float handleX2 = handleX1 + wmScale * handleNameWidth;
        float lineY = baseY + (int)(client.font.lineHeight * wmScale) + 1;
        float handleY1 = baseY;
        float handleY2 = baseY + (int)(client.font.lineHeight * wmScale);

        boolean hovered =
            coords.mouseX >= handleX1 && coords.mouseX <= handleX2 &&
            ((coords.mouseY >= handleY1 && coords.mouseY <= handleY2) ||
             (coords.mouseY >= lineY && coords.mouseY <= lineY + 1));

        if (!hovered) return false;

        try {
            net.minecraft.util.Util.getPlatform().openUri(new java.net.URI("https://space.bilibili.com/522178337"));
        } catch (Exception e) {
            if (client.player != null) {
                client.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("无法打开浏览器，请手动访问: https://space.bilibili.com/522178337"),
                    false
                );
            }
        }

        return true;
    }
    
    private boolean handleCategoryClick(ClickGuiLayout.ScaledCoordinates coords) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float categoryY = ClickGuiState.HEADER_HEIGHT + 20;
        float categorySpacing = 35f;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
             com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            boolean hovered = coords.mouseX >= 0 && coords.mouseX < ClickGuiState.SIDEBAR_WIDTH &&
                            coords.mouseY >= categoryY - 5 && coords.mouseY < categoryY + 25;
            
            if (hovered && timer.delay(100)) {
                state.setSelectedCategory(category);
                state.getSearchInput().clear();
                state.setSearchFocused(false);
                timer.reset();
                return true;
            }
            
            categoryY += categorySpacing;
        }
        
        return false;
    }
    
    private boolean handleModuleAndSubOptionClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        if (coords.mouseX < contentX || coords.mouseX >= contentX + contentWidth ||
            coords.mouseY < contentY || coords.mouseY >= ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT) {
            return false;
        }
        
        List<Module> modules = getFilteredModules();
        float modY = contentY + 10 - state.getScrollOffset();
        
        for (Module module : modules) {
            float moduleX1 = contentX + 10;
            float moduleX2 = contentX + contentWidth - 10;
            if (GuiRenderUtil.isHovered(moduleX1, modY, moduleX2, modY + 25, 
                                       coords.mouseX, coords.mouseY) && 
                timer.delay(100)) {
                if (handleModuleClick(module, modY, coords, button)) {
                    timer.reset();
                    return true;
                }
            }
            
            modY += 30;
            
            if (module.isExpanded()) {
                if (handleSubOptionClick(module, modY, coords, button, contentX, contentWidth)) {
                    timer.reset();
                    return true;
                }
                modY += getSubOptionContainerHeight(module);
            }
        }
        
        return false;
    }
    
    private List<Module> getFilteredModules() {
        String searchText = state.getSearchInput().getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            return ModuleManager.getModulesByCategory(state.getSelectedCategory());
        }
        
        return ModuleManager.getModules().stream()
                .filter(module -> module.getName().toLowerCase().contains(searchText))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private boolean handleModuleClick(Module module, float modY, 
                                     ClickGuiLayout.ScaledCoordinates coords, int button) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        if ("ClickGUI".equals(module.getName())) {
            String keyText = state.isListeningForKey() ? "Press a key..." : state.getCurrentKeyDisplay();
            String plainText = keyText.replaceAll("§[0-9a-fklmnor]", "");
            int keyTextWidth = client.font.width(plainText);
            int keyBoxWidth = keyTextWidth + 16;
            float boxCenterY = modY + 25 / 2f;
            int boxHeight = 12;
            int containerX2 = (int)(ClickGuiState.WIDTH - 20);
            int keyBoxX1 = (int)(containerX2 - keyBoxWidth - 10);
            int keyBoxY1 = (int)(boxCenterY - boxHeight / 2f);
            int keyBoxX2 = (int)(containerX2 - 10);
            int keyBoxY2 = (int)(boxCenterY + boxHeight / 2f);
            
            if (button == 0 && GuiRenderUtil.isHovered(keyBoxX1, keyBoxY1, keyBoxX2, keyBoxY2, 
                                                      coords.mouseX, coords.mouseY)) {
                state.setListeningForKey(true);
                SoundUtils.playWoodenButton();
                timer.reset();
                return true;
            }
        } else {
            boolean hasChildrenClick = false;
            for (Value v : module.getValues()) {
                if (!"enabled".equals(v.getName())) {
                    hasChildrenClick = true;
                    break;
                }
            }
            
            if (button == 0) {
                if (hasChildrenClick && 
                    GuiRenderUtil.isHovered(ClickGuiState.WIDTH - 35, modY, 
                                          ClickGuiState.WIDTH - 15, modY + 25, 
                                          coords.mouseX, coords.mouseY) && 
                    timer.delay(100)) {
                    module.toggleExpanded();
                } else {
                    module.toggle();
                    SoundUtils.playBubble();
                    if (ConfigSynchronizer.hasModuleConfig(module.getName())) {
                        ConfigSynchronizer.handleModuleToggle(module.getName(), module.isEnabled());
                    }
                }
            } else if (button == 1 && hasChildrenClick) {
                module.toggleExpanded();
            }
            
            return true;
        }
        
        return false;
    }
    
    private boolean handleSubOptionClick(Module module, float modY, 
                                        ClickGuiLayout.ScaledCoordinates coords, int button,
                                        float contentX, float contentWidth) {
        if (!timer.delay(100)) return false;
        
        java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
        int subOptionCount = entries.size();
        if (subOptionCount == 0) return false;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        
        float containerX1 = contentX + 20;
        float containerX2 = contentX + contentWidth - 20;
        float subModY = modY + dims.padding;
        
        int containerHeight = dims.height;
        Value previousValue = null;
        for (ValueTreeUtils.ValueEntry entry : entries) {
            Value value = entry.value();
            int depth = entry.depth();
            
            if (subModY > modY + containerHeight - dims.padding) {
                break;
            }
            
            if (value.needsSeparatorBefore(previousValue)) {
                subModY += 12;
            }
            
            ValueStyle style = value.getStyle();
            int subX1 = (int)(containerX1 + 4 + depth * 12);
            int subX2 = (int)(containerX2 - 4 - depth * 8);
            if (style == ValueStyle.GROUP && value instanceof GroupValue) {
                if ((button == 0 || button == 1) &&
                    GuiRenderUtil.isHovered(subX1, (int) subModY, subX2, (int)(subModY + dims.subOptionHeight), coords.mouseX, coords.mouseY)) {
                    ((GroupValue) value).toggleExpanded();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), value.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
                ButtonValue buttonValue = (ButtonValue) value;
                
                String boxText = buttonValue.getDisplayText(val -> {
                    if (val instanceof Integer) {
                        int keyCode = (Integer) val;
                        return com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(keyCode, "");
                    }
                    return val != null ? val.toString() : "☄ NOTSET";
                });
                String plainText = boxText.replaceAll("§[0-9a-fklmnor]", "");
                Minecraft client = Minecraft.getInstance();
                if (client == null) return false;
                
                int boxTextWidth = client.font.width(plainText);
                int boxWidth = boxTextWidth + 16;
                float boxCenterY = subModY + dims.subOptionHeight / 2f;
                int boxHeight = 12;
                int boxX1 = (int)(subX2 - boxWidth - 10);
                int boxY1 = (int)(boxCenterY - boxHeight / 2f);
                int boxX2 = (int)(subX2 - 10);
                int boxY2 = (int)(boxCenterY + boxHeight / 2f);
                
                if (GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, coords.mouseX, coords.mouseY)) {
                    if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.KEYBIND) {
                        state.setListeningButtonValue(module.getName(), value.getName());
                        SoundUtils.playWoodenButton();
                        timer.reset();
                        return true;
                    } else if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.TRIGGER ||
                               buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.FONT_SELECTOR) {
                        SoundUtils.playWoodenButton();
                        if (onTriggerValueClick != null) {
                            onTriggerValueClick.accept(module, buttonValue);
                        }
                        timer.reset();
                        return true;
                    }
                }
            } else if (button == 0 && style == ValueStyle.SLIDER && value instanceof SliderValue) {
                SliderValue sliderValue = (SliderValue) value;
                Minecraft client = Minecraft.getInstance();
                if (client == null) return false;
                
                int resetBoxWidth = 30;
                int resetBoxHeight = 12;
                int resetBoxX = subX2 - resetBoxWidth - 6;
                int resetBoxY = (int)(subModY + (dims.subOptionHeight - resetBoxHeight) / 2);
                
                int resetClickMargin = 2;
                if (GuiRenderUtil.isHovered(
                    resetBoxX - resetClickMargin, 
                    resetBoxY - resetClickMargin, 
                    resetBoxX + resetBoxWidth + resetClickMargin, 
                    resetBoxY + resetBoxHeight + resetClickMargin, 
                    coords.mouseX, coords.mouseY)) {
                    sliderValue.resetToDefault();
                    SoundUtils.playWoodenButton();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), sliderValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
                
                String valueText = sliderValue.getFormattedValue();
                int valueTextWidth = client.font.width(valueText);
                int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
                int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
                int valueDisplayY = (int)(subModY + 2);
                int valueDisplayHeight = dims.subOptionHeight - 4;
                
                if (GuiRenderUtil.isHovered(valueDisplayX, valueDisplayY, valueDisplayX + valueDisplayWidth, valueDisplayY + valueDisplayHeight, coords.mouseX, coords.mouseY)) {
                    if (state.isEditingSlider()
                        && state.getEditingSlider().moduleName.equals(module.getName())
                        && state.getEditingSlider().valueName.equals(value.getName())) {
                        String display = state.getSliderInput().getText();
                        int textX = valueDisplayX + (valueDisplayWidth - client.font.width(display)) / 2;
                        state.getSliderInput().onMousePressed(client.font, (float) coords.mouseX - textX);
                    } else {
                        beginSliderEdit(sliderValue, module.getName(), value.getName());
                    }
                    timer.reset();
                    return true;
                }
                
                int sliderWidth = 80;
                int sliderHeight = 10;
                int sliderX = valueDisplayX - sliderWidth - 10;
                int sliderY = (int)(subModY + (dims.subOptionHeight - sliderHeight) / 2);
                
                if (GuiRenderUtil.isHovered(sliderX - 5, sliderY - 3, sliderX + sliderWidth + 5, sliderY + sliderHeight + 3, coords.mouseX, coords.mouseY)) {
                    double percentage = (coords.mouseX - sliderX) / (double) sliderWidth;
                    percentage = Math.max(0, Math.min(1, percentage));
                    sliderValue.setFromPercentage(percentage);
                    
                    state.setDraggingSlider(new ClickGuiState.SliderDragInfo(module.getName(), value.getName(), sliderX, sliderWidth));
                    
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), sliderValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else if ((button == 0 || button == 1) && style == ValueStyle.FANCY_DMG_PRESET && value instanceof com.shyeuar.baity.gui.value.FancyDmgSplashPresetValue presetPalette) {
                int hit = com.shyeuar.baity.gui.render.ValueStyleRenderer.getHoveredFancyDmgPresetHit(
                        presetPalette, subX1, subModY, subX2, dims.subOptionHeight,
                        coords.mouseX, coords.mouseY);
                if (hit == com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_NONE) {
                } else if (hit == com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_ADD) {
                    if (button == 0) {
                        com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.addCustomPreset();
                        SoundUtils.playBubble();
                        timer.reset();
                        return true;
                    }
                } else if (hit >= com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_BUILTIN_BASE
                        && hit < com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_CUSTOM_BASE) {
                    int index = hit - com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_BUILTIN_BASE;
                    if (button == 1) {
                        com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.selectEditingBuiltin(index);
                    } else {
                        com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.toggleBuiltin(index);
                    }
                    SoundUtils.playBubble();
                    timer.reset();
                    return true;
                } else if (hit >= com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_CUSTOM_BASE) {
                    int index = hit - com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_CUSTOM_BASE;
                    if (button == 1) {
                        com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.selectEditingCustom(index);
                    } else {
                        com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.toggleCustom(index);
                    }
                    SoundUtils.playBubble();
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.COLOR_PALETTE && value instanceof com.shyeuar.baity.gui.value.ColorPaletteValue) {
                com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue = (com.shyeuar.baity.gui.value.ColorPaletteValue) value;
                
                int hoveredIndex = com.shyeuar.baity.gui.render.ValueStyleRenderer.getHoveredColorIndex(
                    paletteValue, subX1, subModY, subX2, dims.subOptionHeight,
                    coords.mouseX, coords.mouseY);
                
                if (hoveredIndex >= 0) {
                    paletteValue.toggleColor(hoveredIndex);
                    SoundUtils.playBubble();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), paletteValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.FANCY_DMG_COLOR_EDITOR && value instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor) {
                if (handleFancyDmgColorEditorClick(module, fancyEditor, subX1, subX2, subModY, dims, coords)) {
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR && value instanceof ChromaFishingLineColorEditorValue chromaEditor) {
                if (handleChromaFishingLineColorEditorClick(module, chromaEditor, subX1, subX2, subModY, dims, coords)) {
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.ENCHANT_LORE_COLOR_EDITOR && value instanceof EnchantLoreColorEditorValue colorEditor) {
                if (handleEnchantLoreColorEditorClick(module, colorEditor, subX1, subX2, subModY, dims, coords)) {
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.GRADIENT_EDITOR && value instanceof GradientEditorValue gradientValue) {
                Minecraft client = Minecraft.getInstance();
                float blockHeight = dims.subOptionHeight * 6;
                String hex = gradientValue.getSelectedHex();
                com.shyeuar.baity.gui.render.ValueStyleRenderer.GradientEditorBottomLayout bottom =
                        com.shyeuar.baity.gui.render.ValueStyleRenderer.computeGradientEditorBottomLayout(
                                client, subX1, subModY, subX2, blockHeight, hex, true);
                float mapX1 = bottom.mapX1;
                float mapY1 = bottom.mapY1;
                float mapX2 = bottom.mapX2;
                float mapY2 = bottom.mapY2;

                if (GuiRenderUtil.isHovered(mapX1, mapY1, mapX2, mapY2, coords.mouseX, coords.mouseY)) {
                    float hue = (coords.mouseX - mapX1) / Math.max(1f, (mapX2 - mapX1));
                    float sat = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
                    gradientValue.setSelectedFromHueSat(hue, sat);
                    state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), value.getName(), mapX1, mapY1, mapX2, mapY2));
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), gradientValue.getValue());
                    }
                    timer.reset();
                    return true;
                }

                float sliderX1 = subX2 - 48;
                float sliderX2 = subX2 - 36;
                if (GuiRenderUtil.isHovered(sliderX1, mapY1, sliderX2, mapY2, coords.mouseX, coords.mouseY)) {
                    float valNorm = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
                    gradientValue.setSelectedValue(valNorm);
                    state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), value.getName(), mapX1, mapY1, mapX2, mapY2, true));
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), gradientValue.getValue());
                    }
                    timer.reset();
                    return true;
                }

                float boxX1 = subX2 - 30;
                float boxX2 = subX2 - 12;
                if (GuiRenderUtil.isHovered(boxX1, subModY + 24, boxX2, subModY + 42, coords.mouseX, coords.mouseY)) {
                    gradientValue.selectPoint(0);
                    SoundUtils.playBubble();
                    timer.reset();
                    return true;
                }
                float box2Y2 = mapY2;
                float box2Y1 = box2Y2 - 18;
                if (GuiRenderUtil.isHovered(boxX1, box2Y1, boxX2, box2Y2, coords.mouseX, coords.mouseY)) {
                    gradientValue.selectPoint(1);
                    SoundUtils.playBubble();
                    timer.reset();
                    return true;
                }
                if (GuiRenderUtil.isHovered(bottom.inputX1, bottom.inputY - 12, bottom.inputX2, bottom.inputY + 6, coords.mouseX, coords.mouseY)) {
                    SoundUtils.playWoodenButton();
                    beginGradientHexEdit(module.getName(), value.getName(), gradientValue.getSelectedPoint(), "");
                    timer.reset();
                    return true;
                }
                if (GuiRenderUtil.isHovered(bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, coords.mouseX, coords.mouseY)) {
                    SoundUtils.playBubble();
                    gradientValue.syncColors();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), gradientValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
                if (bottom.hasReset && GuiRenderUtil.isHovered(bottom.resetX1, bottom.resetY1, bottom.resetX2, bottom.resetY2, coords.mouseX, coords.mouseY)) {
                    gradientValue.resetToDefault();
                    SoundUtils.playBubble();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), gradientValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else if ((button == 0 || button == 1) && style == ValueStyle.CROSSHAIR_PAINTER && value instanceof CrosshairPainterValue painter) {
                Minecraft client = Minecraft.getInstance();
                if (client == null) return false;
                com.shyeuar.baity.gui.render.ValueStyleRenderer.CrosshairPainterLayout l =
                    com.shyeuar.baity.gui.render.ValueStyleRenderer.computeCrosshairPainterLayout(client, painter, subX1, subModY, subX2, dims.subOptionHeight);

                boolean hitAnyButton = false;
                if (GuiRenderUtil.isHovered(l.activeBtnX1, l.activeBtnY1, l.activeBtnX2, l.activeBtnY2, coords.mouseX, coords.mouseY)) {
                    painter.selectLayer(CrosshairPainterValue.Layer.ACTIVE);
                    painter.disarmReset();
                    hitAnyButton = true;
                    SoundUtils.playBubble();
                } else if (GuiRenderUtil.isHovered(l.staticBtnX1, l.staticBtnY1, l.staticBtnX2, l.staticBtnY2, coords.mouseX, coords.mouseY)) {
                    painter.selectLayer(CrosshairPainterValue.Layer.STATIC);
                    painter.disarmReset();
                    hitAnyButton = true;
                    SoundUtils.playBubble();
                } else if (GuiRenderUtil.isHovered(l.resetX1, l.resetY1, l.resetX2, l.resetY2, coords.mouseX, coords.mouseY)) {
                    hitAnyButton = true;
                    if (!painter.isResetArmed()) {
                        painter.armReset();
                    } else {
                        painter.confirmResetSelectedLayer();
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), painter.getValue());
                        }
                    }
                    SoundUtils.playBubble();
                }

                if (!hitAnyButton && painter.isResetArmed()) {
                    painter.disarmReset();
                }

                if (GuiRenderUtil.isHovered(l.canvasX1, l.canvasY1, l.canvasX2, l.canvasY2, coords.mouseX, coords.mouseY)) {
                    int n = painter.getSize();
                    int px = (int) ((coords.mouseX - l.gridX1) / Math.max(1, l.cellPx));
                    int py = (int) ((coords.mouseY - l.gridY1) / Math.max(1, l.cellPx));
                    if (px < 0 || py < 0 || px >= n || py >= n) {
                        return true;
                    }
                    if (button == 0) {
                        painter.togglePixel(px, py);
                    } else {
                        painter.clearPixel(px, py);
                    }
                    painterDragModule = module.getName();
                    painterDragValue = value.getName();
                    painterDragButton = button;
                    painterLastPx = px;
                    painterLastPy = py;
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), painter.getValue());
                    }
                    timer.reset();
                    return true;
                }

                if (hitAnyButton) {
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), painter.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else if (button == 0 && style == ValueStyle.TEXT_LINE_INPUT && value instanceof TextLineInputValue) {
                float lineX1 = subX1 + (subX2 - subX1) * 0.52f;
                float lineX2 = subX2 - 10;
                float lineY = subModY + dims.subOptionHeight - 4;
                float hoverY1 = lineY - 10;
                float hoverY2 = lineY + 5;
                if (GuiRenderUtil.isHovered(lineX1, hoverY1, lineX2, hoverY2, coords.mouseX, coords.mouseY)) {
                    if (state.isEditingTextInput()
                        && state.getEditingTextInput().moduleName.equals(module.getName())
                        && state.getEditingTextInput().valueName.equals(value.getName())) {
                        Minecraft client = Minecraft.getInstance();
                        state.getTextLineInput().onMousePressed(client.font, (float) coords.mouseX - lineX1);
                    } else {
                        state.setEditingTextInput(new ClickGuiState.TextInputInfo(module.getName(), value.getName()));
                        String start = String.valueOf(value.getValue());
                        state.getTextLineInput().setTextAndCaretAtEnd(start == null ? "" : start);
                    }
                    timer.reset();
                    return true;
                }
            } else if (button == 0) {
                if (GuiRenderUtil.isHovered(subX1, (int)subModY, 
                                           subX2, (int)(subModY + dims.subOptionHeight), 
                                           coords.mouseX, coords.mouseY)) {
                    if (value.getValue() instanceof Boolean) {
                        value.setValue(!((Boolean)value.getValue()));
                        SoundUtils.playBubble();
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), value.getValue());
                        }
                    }
                    timer.reset();
                    return true;
                }
            }
            
            float currentHeight = dims.subOptionHeight;
            if (style == ValueStyle.COLOR_PALETTE) {
                currentHeight = dims.subOptionHeight * 2;
            } else if (style == ValueStyle.FANCY_DMG_PRESET) {
                currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
            } else if (style == ValueStyle.GRADIENT_EDITOR || style == ValueStyle.FANCY_DMG_COLOR_EDITOR || style == ValueStyle.ENCHANT_LORE_COLOR_EDITOR || style == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) {
                currentHeight = dims.subOptionHeight * 6;
            } else if (style == ValueStyle.CROSSHAIR_PAINTER) {
                currentHeight = dims.subOptionHeight * 8;
            }
            subModY += currentHeight;
            previousValue = value;
        }
        
        return false;
    }

    private boolean isClickInsideEditingGradientInput(ClickGuiLayout.ScaledCoordinates coords) {
        ClickGuiState.GradientInputInfo editInfo = state.getEditingGradient();
        if (editInfo == null) return false;

        float modY = ClickGuiState.HEADER_HEIGHT + 10 - state.getScrollOffset();
        List<Module> modules = getFilteredModules();
        for (Module module : modules) {
            modY += 30;
            if (!module.isExpanded()) continue;

            java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
            int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
            float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
            ClickGuiLayout.ContainerDimensions dims = ClickGuiLayout.calculateSubOptionContainer(entries.size(), visibleHeight, extraHeight);

            float containerX1 = ClickGuiState.SIDEBAR_WIDTH + 20;
            float containerX2 = ClickGuiState.SIDEBAR_WIDTH + ClickGuiState.CONTENT_WIDTH - 20;
            float subModY = modY + dims.padding;
            Value previousValue = null;
            for (ValueTreeUtils.ValueEntry entry : entries) {
                Value value = entry.value();
                int depth = entry.depth();
                if (value.needsSeparatorBefore(previousValue)) subModY += 12;
                if ((value.getStyle() == ValueStyle.GRADIENT_EDITOR
                        || value.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                        || value.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                        || value.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR)
                    && module.getName().equals(editInfo.moduleName)
                    && value.getName().equals(editInfo.valueName)) {
                    int subX1 = (int) (containerX1 + 4 + depth * 12);
                    int subX2 = (int) (containerX2 - 4 - depth * 8);
                    float blockHeight = dims.subOptionHeight * 6;
                    Minecraft client = Minecraft.getInstance();
                    String hex = "#FFFFFF";
                    String symbol = "";
                    if (value instanceof GradientEditorValue gv) {
                        hex = gv.getSelectedHex();
                    } else if (value instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor) {
                        hex = fancyEditor.gradient().getSelectedHex();
                        symbol = fancyEditor.getSymbols();
                    } else if (value instanceof EnchantLoreColorEditorValue el) {
                        hex = el.gradient().getSelectedHex();
                    } else if (value instanceof ChromaFishingLineColorEditorValue chromaEditor) {
                        hex = chromaEditor.gradient().getSelectedHex();
                    }
                    com.shyeuar.baity.gui.render.ValueStyleRenderer.GradientEditorBottomLayout bottom =
                            com.shyeuar.baity.gui.render.ValueStyleRenderer.computeGradientEditorBottomLayout(
                                    client, subX1, subModY, subX2, blockHeight, hex, true,
                                    value instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue ? symbol : null);
                    if (editInfo.symbolInput && bottom.hasSymbolInput) {
                        return GuiRenderUtil.isHovered(bottom.symbolInputX1, bottom.symbolInputY - 12, bottom.symbolInputX2, bottom.symbolInputY + 6, coords.mouseX, coords.mouseY);
                    }
                    return GuiRenderUtil.isHovered(bottom.inputX1, bottom.inputY - 12, bottom.inputX2, bottom.inputY + 6, coords.mouseX, coords.mouseY);
                }
                float currentHeight = dims.subOptionHeight;
                if (value.getStyle() == ValueStyle.COLOR_PALETTE) currentHeight = dims.subOptionHeight * 2;
                else if (value.getStyle() == ValueStyle.FANCY_DMG_PRESET) currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
                else if (value.getStyle() == ValueStyle.GRADIENT_EDITOR
                        || value.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                        || value.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                        || value.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) currentHeight = dims.subOptionHeight * 6;
                else if (value.getStyle() == ValueStyle.CROSSHAIR_PAINTER) currentHeight = dims.subOptionHeight * 8;
                subModY += currentHeight;
                previousValue = value;
            }
            modY += getSubOptionContainerHeight(module);
        }
        return false;
    }

    private boolean isClickInsideEditingTextInput(ClickGuiLayout.ScaledCoordinates coords) {
        ClickGuiState.TextInputInfo editInfo = state.getEditingTextInput();
        if (editInfo == null) return false;

        float modY = ClickGuiState.HEADER_HEIGHT + 10 - state.getScrollOffset();
        List<Module> modules = getFilteredModules();

        for (Module module : modules) {
            modY += 30;
            if (!module.isExpanded()) continue;

            java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
            int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
            float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
            ClickGuiLayout.ContainerDimensions dims =
                ClickGuiLayout.calculateSubOptionContainer(entries.size(), visibleHeight, extraHeight);

            float containerX1 = ClickGuiState.SIDEBAR_WIDTH + 20;
            float containerX2 = ClickGuiState.SIDEBAR_WIDTH + ClickGuiState.CONTENT_WIDTH - 20;
            float subModY = modY + dims.padding;
            Value previousValue = null;

            for (ValueTreeUtils.ValueEntry entry : entries) {
                Value value = entry.value();
                int depth = entry.depth();

                if (value.needsSeparatorBefore(previousValue)) subModY += 12;

                int subX1 = (int) (containerX1 + 4 + depth * 12);
                int subX2 = (int) (containerX2 - 4 - depth * 8);

                if (value.getStyle() == ValueStyle.TEXT_LINE_INPUT &&
                    module.getName().equals(editInfo.moduleName) &&
                    value.getName().equals(editInfo.valueName)) {
                    float lineX1 = subX1 + (subX2 - subX1) * 0.52f;
                    float lineX2 = subX2 - 10;
                    float lineY = subModY + dims.subOptionHeight - 4;
                    float hoverY1 = lineY - 10;
                    float hoverY2 = lineY + 5;
                    return GuiRenderUtil.isHovered(lineX1, hoverY1, lineX2, hoverY2, coords.mouseX, coords.mouseY);
                }

                float currentHeight = dims.subOptionHeight;
                if (value.getStyle() == ValueStyle.COLOR_PALETTE) currentHeight = dims.subOptionHeight * 2;
                else if (value.getStyle() == ValueStyle.FANCY_DMG_PRESET) currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
                else if (value.getStyle() == ValueStyle.GRADIENT_EDITOR
                        || value.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                        || value.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                        || value.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) currentHeight = dims.subOptionHeight * 6;
                else if (value.getStyle() == ValueStyle.CROSSHAIR_PAINTER) currentHeight = dims.subOptionHeight * 8;
                subModY += currentHeight;
                previousValue = value;
            }
            modY += getSubOptionContainerHeight(module);
        }
        return false;
    }
    
    private boolean handleSubOptionScroll(Module module, float modY, 
                                         ClickGuiLayout.ScaledCoordinates coords, 
                                         double verticalAmount,
                                         float contentX, float contentWidth) {
        java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
        int subOptionCount = entries.size();
        
        if (subOptionCount == 0) return false;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        
        float containerX1 = contentX + 20;
        float containerX2 = contentX + contentWidth - 20;
        float subModY = modY + dims.padding;
        int containerHeight = dims.height;
        
        if (state.getDraggingSlider() != null) {
            return false;
        }
        
        Value previousValue = null;
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            
            if (subModY > modY + containerHeight - dims.padding) {
                break;
            }
            
            if (value.needsSeparatorBefore(previousValue)) {
                subModY += 12;
            }
            
            Object currentVal = value.getValue();
            var handler = ValueTypeRegistry.getHandlerForValue(currentVal);
            
            if (handler != null && 
                GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, 
                                       containerX2 - 4, (int)(subModY + dims.subOptionHeight), 
                                       coords.mouseX, coords.mouseY)) {
            }
            
            float currentHeight = dims.subOptionHeight;
            if (value.getStyle() == ValueStyle.COLOR_PALETTE) {
                currentHeight = dims.subOptionHeight * 2;
            } else if (value.getStyle() == ValueStyle.FANCY_DMG_PRESET) {
                currentHeight = com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(dims.subOptionHeight);
            } else if (value.getStyle() == ValueStyle.GRADIENT_EDITOR
                    || value.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                    || value.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                    || value.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) {
                currentHeight = dims.subOptionHeight * 6;
            } else if (value.getStyle() == ValueStyle.CROSSHAIR_PAINTER) {
                currentHeight = dims.subOptionHeight * 8;
            }
            subModY += currentHeight;
            previousValue = value;
        }
        
        return false;
    }
    
    private boolean handleClickGuiKeybindInput(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            state.setListeningForKey(false);
            return true;
        }
        
        if (KeyMappingUtils.isResetKey(keyCode)) {
            ConfigManager.guiKeyCode = 0;
            ConfigManager.saveConfig();
            updateKeyDisplay();
            state.setListeningForKey(false);
            return true;
        }
        
        if (!KeyMappingUtils.isKeySupported(keyCode)) {
            return false;
        }
        
        ConfigManager.guiKeyCode = keyCode;
        ConfigManager.saveConfig();
        updateKeyDisplay();
        state.setListeningForKey(false);
        return true;
    }
    
    private boolean handleButtonValueKeybindInput(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            state.clearListeningButtonValue();
            return true;
        }
        
        String listeningModule = state.getListeningButtonValueModule();
        String listeningName = state.getListeningButtonValueName();
        
        if (KeyMappingUtils.isResetKey(keyCode)) {
            Module module = ModuleManager.getModuleByName(listeningModule);
            if (module != null) {
                Value found = ValueTreeUtils.findByName(module, listeningName);
                if (found instanceof ButtonValue) {
                    ButtonValue buttonValue = (ButtonValue) found;
                        buttonValue.setValue(0);
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), 0);
                        }
                        state.clearListeningButtonValue();
                        return true;
                }
            }
            state.clearListeningButtonValue();
            return true;
        }
        
        if (!KeyMappingUtils.isKeySupported(keyCode)) {
            return false;
        }
        
        Module module = ModuleManager.getModuleByName(listeningModule);
        if (module != null) {
            Value found = ValueTreeUtils.findByName(module, listeningName);
            if (found instanceof ButtonValue) {
                ButtonValue buttonValue = (ButtonValue) found;
                    buttonValue.setValue(keyCode);
                if (ConfigSynchronizer.hasValueConfig(module.getName(), found.getName())) {
                    ConfigSynchronizer.handleValueUpdate(module.getName(), found.getName(), keyCode);
                    }
                    state.clearListeningButtonValue();
                    return true;
            }
        }
        
        state.clearListeningButtonValue();
        return true;
    }

    private boolean handleChromaFishingLineColorEditorClick(Module module,
                                                      ChromaFishingLineColorEditorValue chromaEditor,
                                                      float subX1, float subX2, float subModY,
                                                      ClickGuiLayout.ContainerDimensions dims,
                                                      ClickGuiLayout.ScaledCoordinates coords) {
        GradientEditorValue gradientValue = chromaEditor.gradient();
        Minecraft client = Minecraft.getInstance();
        float blockHeight = dims.subOptionHeight * 6;
        String hex = gradientValue.getSelectedHex();
        com.shyeuar.baity.gui.render.ValueStyleRenderer.GradientEditorBottomLayout bottom =
                com.shyeuar.baity.gui.render.ValueStyleRenderer.computeGradientEditorBottomLayout(
                        client, subX1, subModY, subX2, blockHeight, hex, true);
        float mapX1 = bottom.mapX1;
        float mapY1 = bottom.mapY1;
        float mapX2 = bottom.mapX2;
        float mapY2 = bottom.mapY2;

        if (GuiRenderUtil.isHovered(mapX1, mapY1, mapX2, mapY2, coords.mouseX, coords.mouseY)) {
            float hue = (coords.mouseX - mapX1) / Math.max(1f, (mapX2 - mapX1));
            float sat = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
            gradientValue.setSelectedFromHueSat(hue, sat);
            state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), chromaEditor.getName(), mapX1, mapY1, mapX2, mapY2));
            chromaEditor.persistToConfig();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), chromaEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), chromaEditor.getName(), chromaEditor.getValue());
            }
            return true;
        }

        float sliderX1 = subX2 - 48;
        float sliderX2 = subX2 - 36;
        if (GuiRenderUtil.isHovered(sliderX1, mapY1, sliderX2, mapY2, coords.mouseX, coords.mouseY)) {
            float valNorm = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
            gradientValue.setSelectedValue(valNorm);
            state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), chromaEditor.getName(), mapX1, mapY1, mapX2, mapY2, true));
            chromaEditor.persistToConfig();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), chromaEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), chromaEditor.getName(), chromaEditor.getValue());
            }
            return true;
        }

        float boxX1 = subX2 - 30;
        float boxX2 = subX2 - 12;
        if (GuiRenderUtil.isHovered(boxX1, subModY + 24, boxX2, subModY + 42, coords.mouseX, coords.mouseY)) {
            gradientValue.selectPoint(0);
            SoundUtils.playBubble();
            return true;
        }
        float box2Y2 = mapY2;
        float box2Y1 = box2Y2 - 18;
        if (GuiRenderUtil.isHovered(boxX1, box2Y1, boxX2, box2Y2, coords.mouseX, coords.mouseY)) {
            gradientValue.selectPoint(1);
            SoundUtils.playBubble();
            return true;
        }

        if (GuiRenderUtil.isHovered(bottom.inputX1, bottom.inputY - 12, bottom.inputX2, bottom.inputY + 6, coords.mouseX, coords.mouseY)) {
            SoundUtils.playWoodenButton();
            beginGradientHexEdit(module.getName(), chromaEditor.getName(), gradientValue.getSelectedPoint(), "");
            return true;
        }

        if (GuiRenderUtil.isHovered(bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, coords.mouseX, coords.mouseY)) {
            SoundUtils.playBubble();
            gradientValue.syncColors();
            chromaEditor.persistToConfig();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), chromaEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), chromaEditor.getName(), chromaEditor.getValue());
            }
            return true;
        }
        return false;
    }
    
    private boolean handleFancyDmgColorEditorClick(Module module, com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor,
                                                   float subX1, float subX2, float subModY,
                                                   ClickGuiLayout.ContainerDimensions dims,
                                                   ClickGuiLayout.ScaledCoordinates coords) {
        GradientEditorValue gradientValue = fancyEditor.gradient();
        Minecraft client = Minecraft.getInstance();
        float blockHeight = dims.subOptionHeight * 6;
        String hex = gradientValue.getSelectedHex();
        com.shyeuar.baity.gui.render.ValueStyleRenderer.GradientEditorBottomLayout bottom =
                com.shyeuar.baity.gui.render.ValueStyleRenderer.computeGradientEditorBottomLayout(
                        client, subX1, subModY, subX2, blockHeight, hex, true, fancyEditor.getSymbols());
        com.shyeuar.baity.gui.render.ValueStyleRenderer.FancyDmgEditorBottomRowLayout row =
                com.shyeuar.baity.gui.render.ValueStyleRenderer.layoutFancyDmgEditorBottomRow(
                        client, subX1, subModY, blockHeight, bottom.symbolInputX1);

        if (GuiRenderUtil.isHovered(row.previewX1, row.previewY1, row.previewX2, row.previewY2, coords.mouseX, coords.mouseY)) {
            com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.cycleEditingPreset();
            SoundUtils.playBubble();
            return true;
        }

        if (GuiRenderUtil.isHovered(row.compactX1, row.compactY1, row.compactX2, row.compactY2, coords.mouseX, coords.mouseY)) {
            fancyEditor.toggleCompact();
            SoundUtils.playBubble();
            return true;
        }

        if (GuiRenderUtil.isHovered(row.boldX1, row.boldY1, row.boldX2, row.boldY2, coords.mouseX, coords.mouseY)) {
            fancyEditor.toggleBold();
            SoundUtils.playBubble();
            return true;
        }

        if (GuiRenderUtil.isHovered(row.deleteX1, row.deleteY1, row.deleteX2, row.deleteY2, coords.mouseX, coords.mouseY)) {
            if (com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.canDeleteCurrentEditingPreset()) {
                com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.armDeleteCurrentCustom();
                SoundUtils.playBubble();
            }
            return true;
        }
        float mapX1 = bottom.mapX1;
        float mapY1 = bottom.mapY1;
        float mapX2 = bottom.mapX2;
        float mapY2 = bottom.mapY2;

        if (GuiRenderUtil.isHovered(mapX1, mapY1, mapX2, mapY2, coords.mouseX, coords.mouseY)) {
            float hue = (coords.mouseX - mapX1) / Math.max(1f, (mapX2 - mapX1));
            float sat = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
            gradientValue.setSelectedFromHueSat(hue, sat);
            state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), fancyEditor.getName(), mapX1, mapY1, mapX2, mapY2));
            fancyEditor.persistToConfig();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), fancyEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), fancyEditor.getName(), fancyEditor.getValue());
            }
            return true;
        }

        float sliderX1 = subX2 - 48;
        float sliderX2 = subX2 - 36;
        if (GuiRenderUtil.isHovered(sliderX1, mapY1, sliderX2, mapY2, coords.mouseX, coords.mouseY)) {
            float valNorm = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
            gradientValue.setSelectedValue(valNorm);
            state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), fancyEditor.getName(), mapX1, mapY1, mapX2, mapY2, true));
            fancyEditor.persistToConfig();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), fancyEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), fancyEditor.getName(), fancyEditor.getValue());
            }
            return true;
        }

        float boxX1 = subX2 - 30;
        float boxX2 = subX2 - 12;
        if (GuiRenderUtil.isHovered(boxX1, subModY + 24, boxX2, subModY + 42, coords.mouseX, coords.mouseY)) {
            gradientValue.selectPoint(0);
            SoundUtils.playBubble();
            return true;
        }
        float box2Y2 = mapY2;
        float box2Y1 = box2Y2 - 18;
        if (GuiRenderUtil.isHovered(boxX1, box2Y1, boxX2, box2Y2, coords.mouseX, coords.mouseY)) {
            gradientValue.selectPoint(1);
            SoundUtils.playBubble();
            return true;
        }

        if (bottom.hasSymbolInput && GuiRenderUtil.isHovered(bottom.symbolInputX1, bottom.symbolInputY - 12, bottom.symbolInputX2, bottom.symbolInputY + 6, coords.mouseX, coords.mouseY)) {
            SoundUtils.playWoodenButton();
            beginGradientSymbolEdit(module.getName(), fancyEditor.getName(), fancyEditor.getSymbols());
            return true;
        }

        if (GuiRenderUtil.isHovered(bottom.inputX1, bottom.inputY - 12, bottom.inputX2, bottom.inputY + 6, coords.mouseX, coords.mouseY)) {
            SoundUtils.playWoodenButton();
            beginGradientHexEdit(module.getName(), fancyEditor.getName(), gradientValue.getSelectedPoint(), "");
            return true;
        }

        if (bottom.hasReset && GuiRenderUtil.isHovered(bottom.resetX1, bottom.resetY1, bottom.resetX2, bottom.resetY2, coords.mouseX, coords.mouseY)) {
            fancyEditor.resetToDefault();
            SoundUtils.playBubble();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), fancyEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), fancyEditor.getName(), fancyEditor.getValue());
            }
            return true;
        }

        if (GuiRenderUtil.isHovered(bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, coords.mouseX, coords.mouseY)) {
            SoundUtils.playBubble();
            gradientValue.syncColors();
            fancyEditor.persistToConfig();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), fancyEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), fancyEditor.getName(), fancyEditor.getValue());
            }
            return true;
        }
        return false;
    }

    private boolean handleEnchantLoreColorEditorClick(Module module, EnchantLoreColorEditorValue colorEditor,
                                                      float subX1, float subX2, float subModY,
                                                      ClickGuiLayout.ContainerDimensions dims,
                                                      ClickGuiLayout.ScaledCoordinates coords) {
        GradientEditorValue gradientValue = colorEditor.gradient();
        Minecraft client = Minecraft.getInstance();
        float blockHeight = dims.subOptionHeight * 6;
        String hex = gradientValue.getSelectedHex();
        com.shyeuar.baity.gui.render.ValueStyleRenderer.GradientEditorBottomLayout bottom =
                com.shyeuar.baity.gui.render.ValueStyleRenderer.computeGradientEditorBottomLayout(
                        client, subX1, subModY, subX2, blockHeight, hex, true);
        float mapX1 = bottom.mapX1;
        float mapY1 = bottom.mapY1;
        float mapX2 = bottom.mapX2;
        float mapY2 = bottom.mapY2;

        float tierBtnX1 = subX1 + 10;
        float tierBtnY1 = subModY + blockHeight - 22;
        float tierBtnX2 = tierBtnX1 + EnchantLore.tierButtonWidth(Minecraft.getInstance().font);
        float tierBtnY2 = tierBtnY1 + EnchantLore.tierButtonHeight(Minecraft.getInstance().font);
        if (GuiRenderUtil.isHovered(tierBtnX1, tierBtnY1, tierBtnX2, tierBtnY2, coords.mouseX, coords.mouseY)) {
            colorEditor.cycleEditingTier();
            EnchantLore.invalidateCache();
            SoundUtils.playBubble();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }

        float toggleSize = 18;
        float boldX1 = tierBtnX2 + 6;
        float boldY1 = tierBtnY1 - 1;
        float boldX2 = boldX1 + toggleSize;
        float boldY2 = boldY1 + toggleSize;
        if (GuiRenderUtil.isHovered(boldX1, boldY1, boldX2, boldY2, coords.mouseX, coords.mouseY)) {
            colorEditor.toggleBold();
            EnchantLore.invalidateCache();
            SoundUtils.playBubble();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }

        float rainbowX1 = boldX2 + 6;
        float rainbowY1 = boldY1;
        float rainbowX2 = rainbowX1 + toggleSize;
        float rainbowY2 = boldY2;
        if (GuiRenderUtil.isHovered(rainbowX1, rainbowY1, rainbowX2, rainbowY2, coords.mouseX, coords.mouseY)) {
            colorEditor.toggleRainbow();
            EnchantLore.invalidateCache();
            SoundUtils.playBubble();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }

        if (GuiRenderUtil.isHovered(mapX1, mapY1, mapX2, mapY2, coords.mouseX, coords.mouseY)) {
            float hue = (coords.mouseX - mapX1) / Math.max(1f, (mapX2 - mapX1));
            float sat = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
            gradientValue.setSelectedFromHueSat(hue, sat);
            state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), colorEditor.getName(), mapX1, mapY1, mapX2, mapY2));
            colorEditor.persistCurrentTier();
            EnchantLore.invalidateCache();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }

        float sliderX1 = subX2 - 48;
        float sliderX2 = subX2 - 36;
        if (GuiRenderUtil.isHovered(sliderX1, mapY1, sliderX2, mapY2, coords.mouseX, coords.mouseY)) {
            float valNorm = 1f - (coords.mouseY - mapY1) / Math.max(1f, (mapY2 - mapY1));
            gradientValue.setSelectedValue(valNorm);
            state.setDraggingGradient(new ClickGuiState.GradientDragInfo(module.getName(), colorEditor.getName(), mapX1, mapY1, mapX2, mapY2, true));
            colorEditor.persistCurrentTier();
            EnchantLore.invalidateCache();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }

        float boxX1 = subX2 - 30;
        float boxX2 = subX2 - 12;
        if (GuiRenderUtil.isHovered(boxX1, subModY + 24, boxX2, subModY + 42, coords.mouseX, coords.mouseY)) {
            gradientValue.selectPoint(0);
            SoundUtils.playBubble();
            return true;
        }
        float box2Y2 = mapY2;
        float box2Y1 = box2Y2 - 18;
        if (GuiRenderUtil.isHovered(boxX1, box2Y1, boxX2, box2Y2, coords.mouseX, coords.mouseY)) {
            gradientValue.selectPoint(1);
            SoundUtils.playBubble();
            return true;
        }

        if (GuiRenderUtil.isHovered(bottom.inputX1, bottom.inputY - 12, bottom.inputX2, bottom.inputY + 6, coords.mouseX, coords.mouseY)) {
            SoundUtils.playWoodenButton();
            beginGradientHexEdit(module.getName(), colorEditor.getName(), gradientValue.getSelectedPoint(), "");
            return true;
        }

        if (bottom.hasReset && GuiRenderUtil.isHovered(bottom.resetX1, bottom.resetY1, bottom.resetX2, bottom.resetY2, coords.mouseX, coords.mouseY)) {
            colorEditor.resetCurrentTier();
            EnchantLore.invalidateCache();
            SoundUtils.playBubble();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }
        if (GuiRenderUtil.isHovered(bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, coords.mouseX, coords.mouseY)) {
            SoundUtils.playBubble();
            gradientValue.syncColors();
            colorEditor.persistCurrentTier();
            EnchantLore.invalidateCache();
            if (ConfigSynchronizer.hasValueConfig(module.getName(), colorEditor.getName())) {
                ConfigSynchronizer.handleValueUpdate(module.getName(), colorEditor.getName(), colorEditor.getValue());
            }
            return true;
        }
        return false;
    }

    private int getSubOptionContainerHeight(Module module) {
        java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
        int subOptionCount = entries.size();
        if (subOptionCount == 0) return 0;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        int fullContainerHeight = subOptionCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
        
        float expandProgress = state.getModuleExpandAnimations().getOrDefault(module.getName(), 0.0f);
        int containerHeight = (int)(fullContainerHeight * expandProgress);
        
        return containerHeight + 5;
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
}

