package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.value.ModuleCategory;
import java.util.HashMap;
import java.util.Map;

public class ClickGuiState {
    private float windowX = 200;
    private float windowY = 200;
    public static final float WIDTH = 500;
    public static final float HEIGHT = 310;
    public static final float BASE_GUI_SCALE = 3.0f;
    
    private float dragX = 0;
    private float dragY = 0;
    private boolean isDragging = false;
    
    private float guiScale = 1.0f;
    
    private ModuleCategory selectedCategory = ModuleCategory.FUN;
    
    private float scrollOffset = 0f;
    public static final float LIST_TOP_PADDING = 60f;
    public static final float ITEM_HEIGHT = 30f;
    
    private boolean isListeningForKey = false;
    private String currentKeyDisplay = "Right Ctrl";
    private String listeningButtonValueName = null;
    
    private final Map<String, Float> moduleExpandAnimations = new HashMap<>();
    
    private String hoveredTooltip = null;
    private net.minecraft.text.Text hoveredTooltipText = null;
    private int tooltipX = 0;
    private int tooltipY = 0;
    
    public float getWindowX() { return windowX; }
    public void setWindowX(float x) { windowX = x; }
    
    public float getWindowY() { return windowY; }
    public void setWindowY(float y) { windowY = y; }
    
    public float getDragX() { return dragX; }
    public void setDragX(float x) { dragX = x; }
    
    public float getDragY() { return dragY; }
    public void setDragY(float y) { dragY = y; }
    
    public boolean isDragging() { return isDragging; }
    public void setDragging(boolean dragging) { isDragging = dragging; }
    
    public float getGuiScale() { return guiScale; }
    public void setGuiScale(float scale) { guiScale = scale; }
    
    public ModuleCategory getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(ModuleCategory category) { selectedCategory = category; }
    
    public float getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(float offset) { scrollOffset = offset; }
    
    public boolean isListeningForKey() { return isListeningForKey; }
    public void setListeningForKey(boolean listening) { isListeningForKey = listening; }
    
    public String getCurrentKeyDisplay() { return currentKeyDisplay; }
    public void setCurrentKeyDisplay(String display) { currentKeyDisplay = display; }
    
    public String getListeningButtonValueName() { return listeningButtonValueName; }
    public void setListeningButtonValueName(String name) { listeningButtonValueName = name; }
    
    public boolean isListeningForInput() {
        return isListeningForKey || listeningButtonValueName != null;
    }
    
    public Map<String, Float> getModuleExpandAnimations() { return moduleExpandAnimations; }
    
    public String getHoveredTooltip() { return hoveredTooltip; }
    public void setHoveredTooltip(String tooltip) { hoveredTooltip = tooltip; }
    
    public net.minecraft.text.Text getHoveredTooltipText() { return hoveredTooltipText; }
    public void setHoveredTooltipText(net.minecraft.text.Text text) { hoveredTooltipText = text; }
    
    public int getTooltipX() { return tooltipX; }
    public void setTooltipX(int x) { tooltipX = x; }
    
    public int getTooltipY() { return tooltipY; }
    public void setTooltipY(int y) { tooltipY = y; }
    
    public void resetDragState() {
        dragX = 0;
        dragY = 0;
        isDragging = false;
    }
}


