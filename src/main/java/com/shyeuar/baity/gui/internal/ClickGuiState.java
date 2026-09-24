package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.input.LineTextInput;
import com.shyeuar.baity.gui.value.ModuleCategory;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;

public class ClickGuiState {
    private float windowX = 200;
    private float windowY = 200;
    public static final float WIDTH = 500;
    public static final float HEIGHT = 310;
    public static final float BASE_GUI_SCALE = 3.0f;
    
    public static final float SIDEBAR_WIDTH = 120f;
    public static final float CONTENT_WIDTH = WIDTH - SIDEBAR_WIDTH;
    public static final float HEADER_HEIGHT = 50f;
    public static final float FOOTER_HEIGHT = 20f;
    
    private float dragX = 0;
    private float dragY = 0;
    private boolean isDragging = false;
    
    private float guiScale = 1.0f;
    
    private ModuleCategory selectedCategory = ModuleCategory.MISC;
    
    private float targetScrollOffset = 0f;
    private float animatedScrollOffset = 0f;
    private final Map<ModuleCategory, Float> scrollOffsetByCategory = new HashMap<>();
    public static final float LIST_TOP_PADDING = 60f;
    public static final float ITEM_HEIGHT = 30f;
    
    private final LineTextInput searchInput = new LineTextInput(LineTextInput.Policy.search());
    private boolean isSearchFocused = false;
    
    private boolean isListeningForKey = false;
    private String currentKeyDisplay = "Right Ctrl";
    private String listeningButtonValueModule = null;
    private String listeningButtonValueName = null;
    
    private SliderDragInfo draggingSlider = null;
    private GradientDragInfo draggingGradient = null;
    
    private SliderInputInfo editingSlider = null;
    private final LineTextInput sliderInput = new LineTextInput(LineTextInput.Policy.search());
    
    private final Map<String, Float> moduleExpandAnimations = new HashMap<>();
    private final Map<String, Float> groupExpandAnimations = new HashMap<>();
    
    private final Map<String, ShimmerAnimationState> moduleShimmerAnimations = new HashMap<>();
    
    private String versionCheckStatus = null;
    private String latestVersion = null;
    private long versionCheckStartTime = 0;
    private boolean isVersionChecking = false;
    private boolean isVersionHovered = false;
    private boolean isAutoCheck = false;
    
    private String hoveredTooltip = null;
    private net.minecraft.network.chat.Component hoveredTooltipText = null;
    private int tooltipX = 0;
    private int tooltipY = 0;
    
    private int hudButtonX = 0;
    private int hudButtonY = 0;
    private int hudButtonWidth = 0;
    private int hudButtonHeight = 0;

    private long openAnimationStartMs;

    public void markOpenAnimationStart() {
        openAnimationStartMs = System.currentTimeMillis();
    }

    public long getOpenAnimationElapsedMs() {
        return System.currentTimeMillis() - openAnimationStartMs;
    }

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
    public void setSelectedCategory(ModuleCategory category) {
        if (category == null || category == selectedCategory) {
            return;
        }
        scrollOffsetByCategory.put(selectedCategory, targetScrollOffset);
        selectedCategory = category;
        float restored = scrollOffsetByCategory.getOrDefault(category, 0f);
        targetScrollOffset = restored;
        animatedScrollOffset = restored;
    }
    
    public float getScrollOffset() { return animatedScrollOffset; }

    public float getLayoutScrollOffset() { return Math.round(animatedScrollOffset); }

    public float getTargetScrollOffset() { return targetScrollOffset; }
    public void setTargetScrollOffset(float offset) { targetScrollOffset = offset; }
    public void setAnimatedScrollOffset(float offset) { animatedScrollOffset = offset; }
    
    public boolean isListeningForKey() { return isListeningForKey; }
    public void setListeningForKey(boolean listening) { isListeningForKey = listening; }
    
    public String getCurrentKeyDisplay() { return currentKeyDisplay; }
    public void setCurrentKeyDisplay(String display) { currentKeyDisplay = display; }
    
    public String getListeningButtonValueName() { return listeningButtonValueName; }
    public void setListeningButtonValueName(String name) { listeningButtonValueName = name; }
    
    public String getListeningButtonValueModule() { return listeningButtonValueModule; }
    public void setListeningButtonValueModule(String module) { listeningButtonValueModule = module; }
    
    public void setListeningButtonValue(String moduleName, String valueName) {
        listeningButtonValueModule = moduleName;
        listeningButtonValueName = valueName;
    }
    
    public void clearListeningButtonValue() {
        listeningButtonValueModule = null;
        listeningButtonValueName = null;
    }
    
    public boolean isListeningForInput() {
        return isListeningForKey || listeningButtonValueName != null;
    }
    
    public Map<String, Float> getModuleExpandAnimations() { return moduleExpandAnimations; }
    public Map<String, Float> getGroupExpandAnimations() { return groupExpandAnimations; }
    
    public Map<String, ShimmerAnimationState> getModuleShimmerAnimations() { return moduleShimmerAnimations; }
    
    public String getHoveredTooltip() { return hoveredTooltip; }
    public void setHoveredTooltip(String tooltip) { hoveredTooltip = tooltip; }
    
    public net.minecraft.network.chat.Component getHoveredTooltipText() { return hoveredTooltipText; }
    public void setHoveredTooltipText(net.minecraft.network.chat.Component text) { hoveredTooltipText = text; }
    
    public int getTooltipX() { return tooltipX; }
    public void setTooltipX(int x) { tooltipX = x; }
    
    public int getTooltipY() { return tooltipY; }
    public void setTooltipY(int y) { tooltipY = y; }
    
    public void setHudButtonBounds(int x, int y, int width, int height) {
        hudButtonX = x;
        hudButtonY = y;
        hudButtonWidth = width;
        hudButtonHeight = height;
    }
    
    public boolean isHudButtonHovered(float mouseX, float mouseY) {
        return mouseX >= hudButtonX && mouseX <= hudButtonX + hudButtonWidth &&
               mouseY >= hudButtonY && mouseY <= hudButtonY + hudButtonHeight;
    }
    
    public String getVersionCheckStatus() { return versionCheckStatus; }
    public void setVersionCheckStatus(String status) { versionCheckStatus = status; }
    
    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String version) { latestVersion = version; }
    
    public long getVersionCheckStartTime() { return versionCheckStartTime; }
    public void setVersionCheckStartTime(long time) { versionCheckStartTime = time; }
    
    public boolean isVersionChecking() { return isVersionChecking; }
    public void setVersionChecking(boolean checking) { isVersionChecking = checking; }
    
    public boolean isVersionHovered() { return isVersionHovered; }
    public void setVersionHovered(boolean hovered) { isVersionHovered = hovered; }
    
    public boolean isAutoCheck() { return isAutoCheck; }
    public void setAutoCheck(boolean autoCheck) { isAutoCheck = autoCheck; }
    
    public LineTextInput getSearchInput() { return searchInput; }

    public boolean isSearchFocused() { return isSearchFocused; }
    public void setSearchFocused(boolean focused) { isSearchFocused = focused; }
    
    public void resetDragState() {
        dragX = 0;
        dragY = 0;
        isDragging = false;
    }
    
    public SliderDragInfo getDraggingSlider() { return draggingSlider; }
    public void setDraggingSlider(SliderDragInfo info) { draggingSlider = info; }
    public GradientDragInfo getDraggingGradient() { return draggingGradient; }
    public void setDraggingGradient(GradientDragInfo info) { draggingGradient = info; }
    
    public SliderInputInfo getEditingSlider() { return editingSlider; }
    public LineTextInput getSliderInput() { return sliderInput; }

    public void setEditingSlider(SliderInputInfo info) {
        editingSlider = info;
        if (info == null) {
            sliderInput.clear();
            originalSliderValue = null;
        }
    }

    public boolean isEditingSlider() { return editingSlider != null; }
    
    private Double originalSliderValue = null;
    public Double getOriginalSliderValue() { return originalSliderValue; }
    public void setOriginalSliderValue(Double value) { originalSliderValue = value; }
    
    public static class SliderDragInfo {
        public final String moduleName;
        public final String valueName;
        public final int sliderX;
        public final int sliderWidth;
        
        public SliderDragInfo(String moduleName, String valueName, int sliderX, int sliderWidth) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.sliderX = sliderX;
            this.sliderWidth = sliderWidth;
        }
    }
    
    public static class SliderInputInfo {
        public final String moduleName;
        public final String valueName;
        
        public SliderInputInfo(String moduleName, String valueName) {
            this.moduleName = moduleName;
            this.valueName = valueName;
        }
    }

    public static class GradientDragInfo {
        public final String moduleName;
        public final String valueName;
        public final float mapX1;
        public final float mapY1;
        public final float mapX2;
        public final float mapY2;
        public final boolean dragValue;

        public GradientDragInfo(String moduleName, String valueName, float mapX1, float mapY1, float mapX2, float mapY2) {
            this(moduleName, valueName, mapX1, mapY1, mapX2, mapY2, false);
        }

        public GradientDragInfo(String moduleName, String valueName, float mapX1, float mapY1, float mapX2, float mapY2, boolean dragValue) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.mapX1 = mapX1;
            this.mapY1 = mapY1;
            this.mapX2 = mapX2;
            this.mapY2 = mapY2;
            this.dragValue = dragValue;
        }
    }
    
    private GradientInputInfo editingGradient = null;
    private final LineTextInput gradientInput = new LineTextInput(LineTextInput.Policy.hexColor());
    private TextInputInfo editingTextInput = null;
    private final LineTextInput textLineInput = new LineTextInput(LineTextInput.Policy.freeText(28));

    public LineTextInput getGradientInput() { return gradientInput; }
    public LineTextInput getTextLineInput() { return textLineInput; }

    public GradientInputInfo getEditingGradient() { return editingGradient; }
    public void setEditingGradient(GradientInputInfo info) {
        editingGradient = info;
        if (info == null) {
            gradientInput.clear();
        }
    }
    public boolean isEditingGradient() { return editingGradient != null; }

    public static class GradientInputInfo {
        public final String moduleName;
        public final String valueName;
        public final int lineIndex;
        public final boolean symbolInput;

        public GradientInputInfo(String moduleName, String valueName, int lineIndex) {
            this(moduleName, valueName, lineIndex, false);
        }

        public GradientInputInfo(String moduleName, String valueName, int lineIndex, boolean symbolInput) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.lineIndex = lineIndex;
            this.symbolInput = symbolInput;
        }
    }

    public TextInputInfo getEditingTextInput() { return editingTextInput; }
    public void setEditingTextInput(TextInputInfo info) {
        editingTextInput = info;
        if (info == null) {
            textLineInput.clear();
        }
    }
    public boolean isEditingTextInput() { return editingTextInput != null; }

    public static class TextInputInfo {
        public final String moduleName;
        public final String valueName;
        public final int rowIndex;
        public TextInputInfo(String moduleName, String valueName) {
            this(moduleName, valueName, -1);
        }
        public TextInputInfo(String moduleName, String valueName, int rowIndex) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.rowIndex = rowIndex;
        }
        public boolean isRow(int row) {
            return rowIndex == row;
        }
    }

    public static class ShimmerAnimationState {
        public boolean isActive = false;
        public float mouseX = 0f;
        public float mouseY = 0f;
        public float progress = 0f;
        public float direction = 1f;
        public long lastUpdateTime = System.currentTimeMillis();
        public boolean isExiting = false;
        public float appearSpeed = 0f;
        public float exitSpeed = 0f;
        
        public void reset() {
            isActive = false;
            isExiting = false;
            progress = 0f;
            direction = 1f;
            appearSpeed = 0f;
            exitSpeed = 0f;
        }
    }

    public static float fixedScaleRatio(Minecraft client) {
        if (client == null || client.options == null) return 1f;
        int guiScaleOption = client.options.guiScale().get();
        float actual = (guiScaleOption <= 0) ? (float) client.getWindow().getGuiScale() : guiScaleOption;
        return BASE_GUI_SCALE / actual;
    }

    public static float fixedCoord(float screenCoord, float pivot, float ratio) {
        return pivot + (screenCoord - pivot) / ratio;
    }
}

