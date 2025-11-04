package com.shyeuar.baity.gui;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.render.ClickGuiRenderer;
import com.shyeuar.baity.gui.internal.ClickGuiInputHandler;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.tooltip.TooltipManager;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.render.ModuleStyleRenderer;
import com.shyeuar.baity.utils.TimerUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ClickGui extends Screen {
    
    private final ClickGuiState state;
    private final TimerUtils valuetimer;
    private final ClickGuiInputHandler inputHandler;
    
    public static Theme theme = new Theme();
    private final ModuleStyleRenderer.TooltipInfo tooltipInfo = new ModuleStyleRenderer.TooltipInfo();
    
    private static ClickGui currentInstance;
    
    public static ClickGui getInstance() {
        return currentInstance;
    }
    
    public ClickGui() {
        super(Text.literal("Baity ClickGui"));
        this.state = new ClickGuiState();
        this.valuetimer = new TimerUtils();
        this.inputHandler = new ClickGuiInputHandler(state, valuetimer, this::handleTriggerValueClick);
        currentInstance = this;
    }
    
    @Override
    protected void init() {
        super.init();
        theme.setDark(); 
        
        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }

        if (this.client != null) {
            state.setGuiScale(this.client.options.getGuiScale().getValue());
        }
        
        ConfigSynchronizer.syncModuleStates();
        updateKeyDisplay();
        
        if (this.client != null && this.client.getWindow() != null) {
            int screenW = this.client.getWindow().getScaledWidth();
            int screenH = this.client.getWindow().getScaledHeight();
            ClickGuiLayout.initializeWindowPosition(state, screenW, screenH);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();

        ClickGuiRenderer.render(context, client, state, theme,
            this::getTooltipText,
            this::getTooltipTextWithColors,
            this::getDisplayTextFormatter,
            tooltipInfo,
            mouseX, mouseY);
    }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inputHandler.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputHandler.handleMouseClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        inputHandler.handleMouseRelease(button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        inputHandler.handleMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputHandler.handleKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        if (this.client != null && this.client.getWindow() != null) {
            int screenW = this.client.getWindow().getScaledWidth();
            int screenH = this.client.getWindow().getScaledHeight();
            ClickGuiLayout.initializeWindowPosition(state, screenW, screenH);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private String getTooltipText(String name) {
        return TooltipManager.getTooltipText(name);
    }
    
    private net.minecraft.text.Text getTooltipTextWithColors(String name) {
        return TooltipManager.getTooltipTextWithColors(name);
    }
    
    private String getDisplayTextFormatter(Object value) {
        if (value instanceof Integer) {
            int keyCode = (Integer) value;
            return com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(keyCode, "");
        }
        return value != null ? value.toString() : "☄NOTSET";
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
    
    private void handleTriggerValueClick(Module module, ButtonValue buttonValue) {
        String valueName = buttonValue.getName();
        
    }
    
    public boolean isListeningForInput() {
        return state.isListeningForInput();
    }
}

