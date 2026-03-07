package com.shyeuar.baity.gui;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.smol.SmolFriendsScreen;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.internal.ClickGuiInputHandler;
import com.shyeuar.baity.gui.owo.ClickGuiRootComponent;
import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.tooltip.TooltipManager;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.utils.TimerUtils;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ClickGui extends BaseOwoScreen<FlowLayout> {
    
    private final ClickGuiState state;
    private final boolean restoredFromSession;
    private final TimerUtils valuetimer;
    private final ClickGuiInputHandler inputHandler;
    private ClickGuiRootComponent rootComponent;
    
    public static Theme theme = new Theme();
    private final com.shyeuar.baity.gui.render.ModuleStyleRenderer.TooltipInfo tooltipInfo = 
        new com.shyeuar.baity.gui.render.ModuleStyleRenderer.TooltipInfo();
    
    private static ClickGui currentInstance;
    private static ClickGuiState lastSessionState;
    private static long lastSessionClosedAt;
    private static final long SESSION_TIMEOUT_MS = 5 * 60_000L;
    
    public static ClickGui getInstance() {
        return currentInstance;
    }
    
    public ClickGui() {
        super(Component.literal("Baity ClickGui"));
        long now = System.currentTimeMillis();
        if (lastSessionState != null && now - lastSessionClosedAt <= SESSION_TIMEOUT_MS) {
            this.state = lastSessionState;
            this.restoredFromSession = true;
        } else {
            this.state = new ClickGuiState();
            this.restoredFromSession = false;
        }
        this.valuetimer = new TimerUtils();
        this.inputHandler = new ClickGuiInputHandler(state, valuetimer, this::handleTriggerValueClick);
        currentInstance = this;
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::horizontalFlow);
    }
    
    @Override
    protected void init() {
        super.init();
        theme.setDark();
        
        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }

        if (!restoredFromSession) {
            for (com.shyeuar.baity.gui.value.ModuleCategory category : com.shyeuar.baity.gui.value.ModuleCategory.values()) {
                java.util.List<Module> categoryModules = ModuleManager.getModulesByCategory(category);
                if (!categoryModules.isEmpty()) {
                    state.setSelectedCategory(category);
                    break;
                }
            }
            for (Module module : ModuleManager.getModules()) {
                module.setExpanded(false);
            }
        }
        
        if (this.minecraft != null) {
            int guiScaleOption = this.minecraft.options.guiScale().get();
            float actualGuiScale = (guiScaleOption <= 0) 
                ? this.minecraft.getWindow().getGuiScale() 
                : guiScaleOption;
            state.setGuiScale(actualGuiScale);
        }
        
        ConfigSynchronizer.syncModuleStates();
        updateKeyDisplay();
        
        if (this.minecraft != null && this.minecraft.getWindow() != null) {
            int screenW = this.minecraft.getWindow().getGuiScaledWidth();
            int screenH = this.minecraft.getWindow().getGuiScaledHeight();
            ClickGuiLayout.initializeWindowPosition(state, screenW, screenH);
        }
        
        if (!restoredFromSession) {
            triggerAutoVersionCheck();
        }
    }
    
    private void triggerAutoVersionCheck() {
        if (state.isVersionChecking()) {
            return;
        }
        
        String currentVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getModContainer("baity")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
        
        state.setVersionChecking(true);
        state.setVersionCheckStatus(null);
        state.setLatestVersion(null);
        state.setAutoCheck(true);
        state.setVersionCheckStartTime(System.currentTimeMillis());
        
        Minecraft mc = this.minecraft;
        com.shyeuar.baity.utils.VersionCheckUtils.checkVersionAsync(currentVersion).thenAccept(result -> {
            if (mc != null && mc.level != null) {
                mc.schedule(() -> {
                    state.setVersionChecking(false);
                    if (result.hasError) {
                        state.setVersionCheckStatus(null);
                        state.setAutoCheck(false);
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
            } else {
                state.setVersionChecking(false);
                state.setAutoCheck(false);
            }
        }).exceptionally(throwable -> {
            if (this.minecraft != null && this.minecraft.level != null) {
                this.minecraft.schedule(() -> {
                    state.setVersionChecking(false);
                    state.setAutoCheck(false);
                });
            } else {
                state.setVersionChecking(false);
                state.setAutoCheck(false);
            }
            return null;
        });
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = new ClickGuiRootComponent(state, theme,
            this::getTooltipText,
            this::getTooltipTextWithColors,
            this::getDisplayTextFormatter,
            tooltipInfo);
        rootComponent.child(this.rootComponent);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (rootComponent != null) {
            rootComponent.setGuiGraphics(graphics);
        }
        
        if (graphics != null) {
            super.render(graphics, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inputHandler.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (inputHandler.handleMouseClick(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, isInsideWindow);
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        inputHandler.handleMouseRelease(click.button());
        return super.mouseReleased(click);
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        inputHandler.handleMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }
    
    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (inputHandler.handleKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(input);
    }
    
    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = (char) input.codepoint();
        if (inputHandler.handleCharTyped(chr, input.modifiers())) {
            return true;
        }
        return super.charTyped(input);
    }
    
    @Override
    public void resize(Minecraft client, int width, int height) {
        super.resize(client, width, height);
        if (this.minecraft != null && this.minecraft.getWindow() != null) {
            int screenW = this.minecraft.getWindow().getGuiScaledWidth();
            int screenH = this.minecraft.getWindow().getGuiScaledHeight();
            ClickGuiLayout.initializeWindowPosition(state, screenW, screenH);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void onClose() {
        if (state.isEditingSlider()) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null && state.getOriginalSliderValue() != null) {
                for (Module module : ModuleManager.getModules()) {
                    if (!module.getName().equals(editInfo.moduleName)) continue;
                    for (com.shyeuar.baity.gui.value.Value value : module.getValues()) {
                        if (value instanceof com.shyeuar.baity.gui.value.SliderValue && value.getName().equals(editInfo.valueName)) {
                            com.shyeuar.baity.gui.value.SliderValue sliderValue = (com.shyeuar.baity.gui.value.SliderValue) value;
                            sliderValue.setValue(state.getOriginalSliderValue());
                            break;
                        }
                    }
                }
            }
            state.setEditingSlider(null);
        }
        lastSessionState = this.state;
        lastSessionClosedAt = System.currentTimeMillis();
        super.onClose();
    }

    @Override
    public void removed() {
        lastSessionState = this.state;
        lastSessionClosedAt = System.currentTimeMillis();
        super.removed();
    }
    
    private String getTooltipText(String name) {
        return TooltipManager.getTooltipText(name);
    }
    
    private net.minecraft.network.chat.Component getTooltipTextWithColors(String name) {
        return TooltipManager.getTooltipTextWithColors(name);
    }
    
    private String getDisplayTextFormatter(Object value) {
        if (value instanceof Integer) {
            int keyCode = (Integer) value;
            return com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(keyCode, "");
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : "☄ NOTSET";
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
    
    private void handleTriggerValueClick(Module module, ButtonValue buttonValue) {
        if (module == null || buttonValue == null) {
            return;
        }

        if ("SmolPeople".equals(module.getName()) && "friends".equals(buttonValue.getName())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.setScreen(new SmolFriendsScreen(this));
            }
        }
    }
    
    public boolean isListeningForInput() {
        return state.isListeningForInput();
    }
}

