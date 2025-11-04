package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.Value;

import java.util.List;

public class ClickGuiLayout {
    
    public static float calculateContentHeight(ClickGuiState state, float visibleHeight) {
        float contentHeight = 0f;
        List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
        
        for (Module module : modules) {
            contentHeight += ClickGuiState.ITEM_HEIGHT;
            if (module.isExpanded()) {
                int childCount = 0;
                for (Value value : module.getValues()) {
                    if (!"enabled".equals(value.getName())) childCount++;
                }
                if (childCount > 0) {
                    int containerPadding = 8;
                    int subOptionHeight = 20;
                    int maxContainerHeight = (int)(visibleHeight - 80);
                    int fullContainerHeight = childCount * subOptionHeight + containerPadding * 2;
                    int containerHeight = Math.min(fullContainerHeight, maxContainerHeight);
                    contentHeight += containerHeight + 5;
                }
            }
        }
        
        return contentHeight;
    }
   
    public static ContainerDimensions calculateSubOptionContainer(int subOptionCount, float visibleHeight) {
        int containerPadding = 8;
        int subOptionHeight = 20;
        int maxContainerHeight = (int)(visibleHeight - 80);
        int fullContainerHeight = subOptionCount * subOptionHeight + containerPadding * 2;
        int containerHeight = Math.min(fullContainerHeight, maxContainerHeight);
        
        return new ContainerDimensions(
            containerPadding,
            subOptionHeight,
            containerHeight,
            maxContainerHeight
        );
    }
    
    public static ScrollbarInfo calculateScrollbar(ClickGuiState state, float contentHeight, float visibleHeight) {
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
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


