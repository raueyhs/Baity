package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.TimerUtils;
import com.shyeuar.baity.utils.KeyMappingUtils;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiConsumer;

public class ClickGuiInputHandler {
    
    private final ClickGuiState state;
    private final TimerUtils timer;
    private final BiConsumer<com.shyeuar.baity.gui.module.Module, com.shyeuar.baity.gui.value.ButtonValue> onTriggerValueClick;
    
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
        
        if (button == 0 && handleWindowDrag(coords, mouseX, mouseY)) {
            return true;
        }
        
        if (handleCategoryClick(coords)) {
            return true;
        }
        
        return handleModuleAndSubOptionClick(coords, button);
    }
    
    public boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        if (GuiRenderUtil.isHovered(0, ClickGuiState.LIST_TOP_PADDING, 
                                    ClickGuiState.WIDTH, ClickGuiState.HEIGHT - 20, 
                                    coords.mouseX, coords.mouseY)) {
            float modY = 60 - state.getScrollOffset();
            List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
            
            for (Module module : modules) {
                if (module.isExpanded()) {
                    if (handleSubOptionScroll(module, modY, coords, verticalAmount)) {
                        return true;
                    }
                    modY += getSubOptionContainerHeight(module);
                }
                modY += 30;
            }
            
            float delta = (float)(-verticalAmount * 20);
            state.setScrollOffset(state.getScrollOffset() + delta);
            return true;
        }
        
        return false;
    }
    
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (state.isListeningForKey()) {
            return handleClickGuiKeybindInput(keyCode);
        }
        
        if (state.getListeningButtonValueName() != null) {
            return handleButtonValueKeybindInput(keyCode);
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (state.isListeningForInput()) {
                state.setListeningForKey(false);
                state.setListeningButtonValueName(null);
                return true;
            }
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        
        return false;
    }
    
    public void handleMouseRelease(int button) {
        if (button == 0) {
            state.resetDragState();
        }
    }
   
    public void handleMouseMove(double mouseX, double mouseY) {
        if (state.isDragging()) {
            ClickGuiLayout.updateWindowPosition(state, mouseX, mouseY, state.getDragX(), state.getDragY());
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
            String listeningName = state.getListeningButtonValueName();
            for (Module module : ModuleManager.getModules()) {
                for (Value value : module.getValues()) {
                    if (value instanceof ButtonValue && value.getName().equals(listeningName)) {
                        ButtonValue buttonValue = (ButtonValue) value;
                        buttonValue.setValue(mouseKeyCode);
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), mouseKeyCode);
                        }
                        state.setListeningButtonValueName(null);
                        return true;
                    }
                }
            }
            state.setListeningButtonValueName(null);
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
    
    private boolean handleCategoryClick(ClickGuiLayout.ScaledCoordinates coords) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        float cateX = 20;
        float cateY = 30;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
             com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            String label = category.getDisplayName();
            int textWidth = client.textRenderer.getWidth(label);
            
            if (GuiRenderUtil.isHovered(cateX, cateY, cateX + textWidth, cateY + 12, 
                                       coords.mouseX, coords.mouseY) && 
                timer.delay(100)) {
                state.setSelectedCategory(category);
                timer.reset();
                return true;
            }
            cateX += textWidth + 28;
        }
        
        return false;
    }
    
    private boolean handleModuleAndSubOptionClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        float modY = 60 - state.getScrollOffset();
        List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
        
        for (Module module : modules) {
            if (GuiRenderUtil.isHovered(20, modY, ClickGuiState.WIDTH - 20, modY + 25, 
                                       coords.mouseX, coords.mouseY) && 
                timer.delay(100)) {
                if (handleModuleClick(module, modY, coords, button)) {
                    timer.reset();
                    return true;
                }
            }
            
            modY += 30;
            
            if (module.isExpanded()) {
                if (handleSubOptionClick(module, modY, coords, button)) {
                    timer.reset();
                    return true;
                }
                modY += getSubOptionContainerHeight(module);
            }
        }
        
        return false;
    }
    
    private boolean handleModuleClick(Module module, float modY, 
                                     ClickGuiLayout.ScaledCoordinates coords, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        if ("ClickGUI".equals(module.getName())) {
            String keyText = state.isListeningForKey() ? "Press a key..." : state.getCurrentKeyDisplay();
            String plainText = keyText.replaceAll("§[0-9a-fklmnor]", "");
            int keyTextWidth = client.textRenderer.getWidth(plainText);
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
                                        ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0 || !timer.delay(100)) return false;
        
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return false;
        
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING);
        
        int containerX1 = 30;
        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
        float subModY = modY + dims.padding;
        
        int innerVisible = Math.max(0, dims.height - dims.padding * 2);
        int maxVisibleOptions = Math.max(0, innerVisible / dims.subOptionHeight);
        int renderedCount = 0;
        
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            if (renderedCount >= maxVisibleOptions) break;
            
            ValueStyle style = value.getStyle();
            if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
                ButtonValue buttonValue = (ButtonValue) value;
                
                String boxText = buttonValue.getDisplayText(val -> {
                    if (val instanceof Integer) {
                        int keyCode = (Integer) val;
                        return com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(keyCode, "");
                    }
                    return val != null ? val.toString() : "☄NOTSET";
                });
                String plainText = boxText.replaceAll("§[0-9a-fklmnor]", "");
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) return false;
                
                int boxTextWidth = client.textRenderer.getWidth(plainText);
                int boxWidth = boxTextWidth + 16;
                float boxCenterY = subModY + dims.subOptionHeight / 2f;
                int boxHeight = 12;
                int subX2 = containerX2 - 4;
                int boxX1 = (int)(subX2 - boxWidth - 10);
                int boxY1 = (int)(boxCenterY - boxHeight / 2f);
                int boxX2 = (int)(subX2 - 10);
                int boxY2 = (int)(boxCenterY + boxHeight / 2f);
                
                if (GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, coords.mouseX, coords.mouseY)) {
                    if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.KEYBIND) {
                        state.setListeningButtonValueName(value.getName());
                        timer.reset();
                        return true;
                    } else if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.TRIGGER) {
                        if (onTriggerValueClick != null) {
                            onTriggerValueClick.accept(module, buttonValue);
                        }
                        timer.reset();
                        return true;
                    }
                }
            } else {
                if (GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, 
                                           containerX2 - 4, (int)(subModY + dims.subOptionHeight), 
                                           coords.mouseX, coords.mouseY)) {
                    if (value.getValue() instanceof Boolean) {
                        value.setValue(!((Boolean)value.getValue()));
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), value.getValue());
                        }
                    }
                    timer.reset();
                    return true;
                }
            }
            
            subModY += dims.subOptionHeight;
            renderedCount++;
        }
        
        return false;
    }
    
    private boolean handleSubOptionScroll(Module module, float modY, 
                                         ClickGuiLayout.ScaledCoordinates coords, 
                                         double verticalAmount) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return false;
        
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING);
        
        int containerX1 = 30;
        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
        float subModY = modY + dims.padding;
        
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            
            Object currentVal = value.getValue();
            var handler = ValueTypeRegistry.getHandlerForValue(currentVal);
            
            if (handler != null && 
                GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, 
                                       containerX2 - 4, (int)(subModY + dims.subOptionHeight), 
                                       coords.mouseX, coords.mouseY)) {
                Object newValue = handler.updateValue(currentVal, verticalAmount);
                if (newValue != currentVal) {
                    value.setValue(newValue);
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), newValue);
                    }
                    return true;
                }
            }
            
            if (GuiRenderUtil.isHovered(containerX1, (int)subModY, containerX2, 
                                       (int)(subModY + dims.subOptionHeight), 
                                       coords.mouseX, coords.mouseY)) {
                float delta = (float)(-verticalAmount * 20);
                state.setScrollOffset(state.getScrollOffset() + delta);
                return true;
            }
            
            subModY += dims.subOptionHeight;
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
            state.setListeningButtonValueName(null);
            return true;
        }
        
        if (KeyMappingUtils.isResetKey(keyCode)) {
            String listeningName = state.getListeningButtonValueName();
            for (Module module : ModuleManager.getModules()) {
                for (Value value : module.getValues()) {
                    if (value instanceof ButtonValue && value.getName().equals(listeningName)) {
                        ButtonValue buttonValue = (ButtonValue) value;
                        buttonValue.setValue(0);
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), 0);
                        }
                        state.setListeningButtonValueName(null);
                        return true;
                    }
                }
            }
            state.setListeningButtonValueName(null);
            return true;
        }
        
        if (!KeyMappingUtils.isKeySupported(keyCode)) {
            return false;
        }
        
        String listeningName = state.getListeningButtonValueName();
        for (Module module : ModuleManager.getModules()) {
            for (Value value : module.getValues()) {
                if (value instanceof ButtonValue && value.getName().equals(listeningName)) {
                    ButtonValue buttonValue = (ButtonValue) value;
                    buttonValue.setValue(keyCode);
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), keyCode);
                    }
                    state.setListeningButtonValueName(null);
                    return true;
                }
            }
        }
        
        state.setListeningButtonValueName(null);
        return true;
    }
    
    private int getSubOptionContainerHeight(Module module) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return 0;
        
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING);
        return dims.height + 5;
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
}


