package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ValueTreeUtils;

import java.util.List;

public class ClickGuiLayout {
    
    public static float calculateContentHeight(ClickGuiState state, float visibleHeight) {
        float contentHeight = 0f;
        List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
        
        for (Module module : modules) {
            contentHeight += ClickGuiState.ITEM_HEIGHT;
            if (module.isExpanded()) {
                java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
                int childCount = entries.size();
                int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
                if (childCount > 0) {
                    ClickGuiLayout.ContainerDimensions dims = 
                        ClickGuiLayout.calculateSubOptionContainer(childCount, visibleHeight, extraHeight);
                    int fullContainerHeight = childCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
                    contentHeight += fullContainerHeight + 5;
                }
            }
        }
        
        return contentHeight;
    }
    
    public static float calculateContentHeightForModules(List<Module> modules, float visibleHeight) {
        float contentHeight = 0f;
        
        for (Module module : modules) {
            contentHeight += ClickGuiState.ITEM_HEIGHT;
            if (module.isExpanded()) {
                java.util.List<ValueTreeUtils.ValueEntry> entries = ValueTreeUtils.getVisibleEntries(module);
                int childCount = entries.size();
                int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
                if (childCount > 0) {
                    ClickGuiLayout.ContainerDimensions dims = 
                        ClickGuiLayout.calculateSubOptionContainer(childCount, visibleHeight, extraHeight);
                    int fullContainerHeight = childCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
                    contentHeight += fullContainerHeight + 5;
                }
            }
        }
        
        return contentHeight;
    }
   
    public static ContainerDimensions calculateSubOptionContainer(int subOptionCount, float visibleHeight) {
        return calculateSubOptionContainer(subOptionCount, visibleHeight, 0);
    }
    
    public static ContainerDimensions calculateSubOptionContainer(int subOptionCount, float visibleHeight, int extraHeight) {
        int containerPadding = 8;
        int subOptionHeight = 20;
        int maxContainerHeight = (int)(visibleHeight - 80);
        int fullContainerHeight = subOptionCount * subOptionHeight + containerPadding * 2 + extraHeight;
        int containerHeight = fullContainerHeight;
        
        return new ContainerDimensions(
            containerPadding,
            subOptionHeight,
            containerHeight,
            maxContainerHeight
        );
    }
    
    public static int calculateExtraHeight(Module module) {
        return calculateExtraHeight(ValueTreeUtils.getVisibleEntries(module));
    }

    public static int calculateExtraHeight(List<ValueTreeUtils.ValueEntry> entries) {
        int extraHeight = 0;
        Value previousValue = null;
        for (ValueTreeUtils.ValueEntry entry : entries) {
            Value value = entry.value();
            if (value.getStyle() == ValueStyle.COLOR_PALETTE) {
                extraHeight += 20;
            } else if (value.getStyle() == ValueStyle.FANCY_DMG_PRESET) {
                extraHeight += (int) (com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(20) - 20);
            } else if (value.getStyle() == ValueStyle.GRADIENT_EDITOR
                    || value.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                    || value.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                    || value.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) {
                extraHeight += 100;
            } else if (value.getStyle() == ValueStyle.CROSSHAIR_PAINTER) {
                extraHeight += 140;
            }
            if (value.needsSeparatorBefore(previousValue)) {
                extraHeight += 12;
            }
            previousValue = value;
        }
        return extraHeight;
    }
    
    public static ScrollbarInfo calculateScrollbar(ClickGuiState state, float contentHeight, float visibleHeight) {
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentStartY = contentY + 10;
        float contentEndY = contentY + visibleHeight;
        float maxScroll = Math.max(0, contentHeight + contentStartY - contentEndY);
        float ratio = visibleHeight / contentHeight;
        float barHeight = Math.max(10, visibleHeight * ratio);
        float travel = visibleHeight - barHeight;
        float progress = maxScroll == 0 ? 0 : (state.getScrollOffset() / maxScroll);
        float barY = ClickGuiState.LIST_TOP_PADDING + travel * progress;
        
        return new ScrollbarInfo(maxScroll, ratio, barHeight, barY);
    }
    
    public static ScaledCoordinates getScaledCoordinates(ClickGuiState state, double mouseX, double mouseY) {
        float scaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        float scaledMouseX = ((float)mouseX - state.getWindowX()) / scaleRatio;
        float scaledMouseY = ((float)mouseY - state.getWindowY()) / scaleRatio;
        return new ScaledCoordinates(scaledMouseX, scaledMouseY, scaleRatio);
    }

    public static float searchBarX() {
        return ClickGuiState.SIDEBAR_WIDTH + 20;
    }

    public static float searchBarY() {
        return 15;
    }

    public static float searchBarWidth() {
        return ClickGuiState.CONTENT_WIDTH - 40;
    }

    public static float searchBarHeight() {
        return 20;
    }

    public static float searchBarTextStartX() {
        float iconSize = 12;
        float iconPadding = 4;
        return searchBarX() + iconSize + iconPadding * 2;
    }

    public static boolean isSearchBarHovered(float mouseX, float mouseY) {
        return com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(
            searchBarX(),
            searchBarY(),
            searchBarX() + searchBarWidth(),
            searchBarY() + searchBarHeight(),
            mouseX,
            mouseY
        );
    }

    public static boolean shouldSuppressContentTooltips(float mouseX, float mouseY) {
        if (isSearchBarHovered(mouseX, mouseY)) {
            return true;
        }
        return mouseY < ClickGuiState.LIST_TOP_PADDING;
    }
    
    public static void initializeWindowPosition(ClickGuiState state, int screenWidth, int screenHeight) {
        float scaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        float windowX = (screenWidth - ClickGuiState.WIDTH * scaleRatio) / 2f;
        float windowY = (screenHeight - ClickGuiState.HEIGHT * scaleRatio) / 2f;
        state.setWindowX(windowX);
        state.setWindowY(windowY);
    }
    
    public static void updateWindowPosition(ClickGuiState state, double mouseX, double mouseY, float dragX, float dragY) {
        float scaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        state.setWindowX((float)mouseX - dragX * scaleRatio);
        state.setWindowY((float)mouseY - dragY * scaleRatio);
    }
    
    public static void clampScrollOffset(ClickGuiState state, float maxScroll) {
        float scrollOffset = state.getScrollOffset();
        if (scrollOffset < 0) {
            state.setScrollOffset(0);
        } else if (scrollOffset > maxScroll) {
            state.setScrollOffset(maxScroll);
        }
    }
    
    public static float clampScrollDelta(ClickGuiState state, float maxScroll, float delta) {
        float currentOffset = state.getScrollOffset();
        float newOffset = currentOffset + delta;
        
        if (currentOffset <= 0 && delta < 0) {
            return 0;
        }
        if (currentOffset >= maxScroll && delta > 0) {
            return 0;
        }
        
        if (newOffset < 0) {
            return -currentOffset;
        }
        if (newOffset > maxScroll) {
            return maxScroll - currentOffset;
        }
        
        return delta;
    }
    
    public static class ContainerDimensions {
        public final int padding;
        public final int subOptionHeight;
        public final int height;
        public final int maxHeight;
        
        public ContainerDimensions(int padding, int subOptionHeight, int height, int maxHeight) {
            this.padding = padding;
            this.subOptionHeight = subOptionHeight;
            this.height = height;
            this.maxHeight = maxHeight;
        }
    }
    
    public static class ScrollbarInfo {
        public final float maxScroll;
        public final float ratio;
        public final float barHeight;
        public final float barY;
        
        public ScrollbarInfo(float maxScroll, float ratio, float barHeight, float barY) {
            this.maxScroll = maxScroll;
            this.ratio = ratio;
            this.barHeight = barHeight;
            this.barY = barY;
        }
    }
    
    public static class ScaledCoordinates {
        public final float mouseX;
        public final float mouseY;
        public final float scaleRatio;
        
        public ScaledCoordinates(float mouseX, float mouseY, float scaleRatio) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.scaleRatio = scaleRatio;
        }
    }
}

