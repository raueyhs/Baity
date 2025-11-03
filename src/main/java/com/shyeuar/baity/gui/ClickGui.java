package com.shyeuar.baity.gui;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.utils.TimerUtils;
import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.config.ConfigSynchronizer;
import com.shyeuar.baity.gui.tooltip.TooltipManager;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import com.shyeuar.baity.gui.render.ModuleStyleRenderer;
import com.shyeuar.baity.gui.render.ValueStyleRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ClickGui extends Screen {
    
    private float dragX, dragY;
    private boolean drag = false;
    
    private static float windowX = 200, windowY = 200;
    private static final float width = 500, height = 310;
    
    private float guiScale = 1.0f;
    private static final float BASE_GUI_SCALE = 3.0f;
    
    private static ModuleCategory modCategory = ModuleCategory.FUN;
    
    private final TimerUtils valuetimer = new TimerUtils();
    
    public static Theme theme = new Theme();
    
    private boolean isListeningForKey = false;
    private String currentKeyDisplay = "Right Ctrl";
    private String listeningButtonValueName = null;
    
    public boolean isListeningForInput() {
        return isListeningForKey || listeningButtonValueName != null;
    }
    
    private final java.util.Map<String, Float> moduleExpandAnimations = new java.util.HashMap<>();

    private String hoveredTooltip = null;
    private net.minecraft.text.Text hoveredTooltipText = null;
    private int tooltipX = 0, tooltipY = 0;
    private final ModuleStyleRenderer.TooltipInfo tooltipInfo = new ModuleStyleRenderer.TooltipInfo();

    private float scrollOffset = 0f;
    private static final float listTopPadding = 60f;
    private static final float itemHeight = 30f;
    
    public ClickGui() {
        super(Text.literal("Baity ClickGui"));
    }
    
    @Override
    protected void init() {
        super.init();
        theme.setDark(); 
        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }

        if (this.client != null) {
            this.guiScale = this.client.options.getGuiScale().getValue();
        }
        
        ConfigSynchronizer.syncModuleStates();
        updateKeyDisplay();

        float scaleRatio = BASE_GUI_SCALE / guiScale;
        
        if (this.client != null && this.client.getWindow() != null) {
            float screenW = this.client.getWindow().getScaledWidth();
            float screenH = this.client.getWindow().getScaledHeight();
            windowX = (screenW - width * scaleRatio) / 2f;
            windowY = (screenH - height * scaleRatio) / 2f;
        }
    }

    private void updateModuleExpandAnimations() {
        for (Module module : ModuleManager.getModulesByCategory(modCategory)) {
            updateModuleExpandAnimation(module.getName(), module.isExpanded());
        }
    }
    
    private float getModuleExpandProgress(String moduleName) {
        return moduleExpandAnimations.getOrDefault(moduleName, 0.0f);
    }
    
    private void updateModuleExpandAnimation(String moduleName, boolean expanded) {
        float target = expanded ? 1.0f : 0.0f;
        float current = moduleExpandAnimations.getOrDefault(moduleName, 0.0f);

        float speed = 0.18f; 
        float lin = current + (target - current) * speed;
        float t = Math.max(0f, Math.min(1f, lin));
        float eased = t * t * (3 - 2 * t);
        float newValue = eased;
        
        if (Math.abs(newValue - target) < 0.01f) {
            newValue = target;
        }
        
        moduleExpandAnimations.put(moduleName, newValue);
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
        currentKeyDisplay = com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, "");
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();

        updateModuleExpandAnimations();

        hoveredTooltip = null;
        hoveredTooltipText = null;
        tooltipInfo.tooltip = null;
        tooltipInfo.tooltipText = null;

        float scaleRatio = BASE_GUI_SCALE / guiScale;
        
        context.getMatrices().push();
        context.getMatrices().translate(windowX, windowY, 0);
        context.getMatrices().scale(scaleRatio, scaleRatio, 1.0f);
        
        float scaledMouseX = ((float)mouseX - windowX) / scaleRatio;
        float scaledMouseY = ((float)mouseY - windowY) / scaleRatio;
        
        GuiRenderUtil.drawRoundedRect(context, 0, 0, width, height, 6, theme.BG.getRGB());
        GuiRenderUtil.stroke1px(context, 0, 0, width, height, new java.awt.Color(255,255,255,20).getRGB());

        float cateX = 20;
        float cateY = 30;
        for (ModuleCategory category : ModuleCategory.values()) {

            boolean active = category == modCategory;
            String label = category.getDisplayName();
            int w = client.textRenderer.getWidth(label);
            int color = active ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
            context.drawText(client.textRenderer, label, (int)cateX, (int)cateY, color, false);
            if (active) {
                float textLeft = cateX;
                float textRight = cateX + w;
                float textCenterX = (textLeft + textRight) / 2f;
                
                float lineExtension = 6f; 
                float lineLeft = textCenterX - w/2f - lineExtension;
                float lineRight = textCenterX + w/2f + lineExtension;
                
                GuiRenderUtil.divider(context, lineLeft, cateY + 12, lineRight, cateY + 13, new java.awt.Color(255,255,255,64).getRGB());
            }
            cateX += w + 28;
        }

        float visibleTop = listTopPadding;
        float visibleBottom = height - 20; 
        float visibleHeight = Math.max(0, visibleBottom - visibleTop);
        float contentHeight = 0f;
        
        for (Module m : ModuleManager.getModulesByCategory(modCategory)) {
            contentHeight += itemHeight; 
            if (m.isExpanded()) {
                int childCount = 0;
                for (Value v : m.getValues()) {
                    if (!"enabled".equals(v.getName())) childCount++;
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
        
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        
        float modY = 60 - scrollOffset;
        
        List<Module> currentModules = ModuleManager.getModulesByCategory(modCategory);
        if (currentModules.isEmpty()) {
            String placeholderText = "not coming soon~~~";
            int textWidth = client.textRenderer.getWidth(placeholderText);
            int textX = (int)((width - textWidth) / 2);
            int textY = (int)(modY + 50);
                context.drawText(client.textRenderer, placeholderText, textX, textY, theme.FONT.getRGB(), false);
            modY += 100; 
        }
        
        context.enableScissor(0, (int)listTopPadding, (int)width, (int)(height - 20));
        
        for (Module module : ModuleManager.getModulesByCategory(modCategory)) {
            ModuleStyleRenderer.renderModule(context, client, module, theme,
                                            20, modY, width - 20, 25,
                                            (float)scaledMouseX, (float)scaledMouseY,
                                            isListeningForKey, currentKeyDisplay,
                                            this::getTooltipText,
                                            this::getTooltipTextWithColors,
                                            tooltipInfo);
            
            if (tooltipInfo.tooltip != null) {
                hoveredTooltip = tooltipInfo.tooltip;
                hoveredTooltipText = tooltipInfo.tooltipText;
                tooltipX = tooltipInfo.x;
                tooltipY = tooltipInfo.y;
            }

            modY += 30;
            
            int subOptionCount = 0;
            for (Value value : module.getValues()) {
                if (!value.getName().equals("enabled")) subOptionCount++;
            }
            
            if (subOptionCount > 0) {
                float expandProgress = getModuleExpandProgress(module.getName());
                
                if (expandProgress > 0.0f) {
                    int containerPadding = 8; 
                    int subOptionHeight = 20; 
                    int maxContainerHeight = (int)(visibleHeight - 80); 
                    int fullContainerHeight = subOptionCount * subOptionHeight + containerPadding * 2;
                    int containerHeight = Math.min(fullContainerHeight, maxContainerHeight);
                    containerHeight = (int)(containerHeight * expandProgress); 
                    
                    int containerBg = new java.awt.Color(30, 30, 30, 200).getRGB();
                    int containerX1 = (int)(30);
                    int containerY1 = (int)(modY);
                    int containerX2 = (int)(width - 30);
                    int containerY2 = (int)(modY + containerHeight);

                    context.fill(containerX1, containerY1, containerX2, containerY2, containerBg);
                    GuiRenderUtil.stroke1px(context, containerX1, containerY1, containerX2, containerY2, new java.awt.Color(255,255,255,40).getRGB());

                    int innerVisible = Math.max(0, containerHeight - containerPadding * 2);
                    if (innerVisible >= subOptionHeight / 2) {
                        float subModY = modY + containerPadding;
                        int maxVisibleOptions = Math.max(0, innerVisible / subOptionHeight);

                        int renderedCount = 0;
                        for (Value value : module.getValues()) {
                            if (value.getName().equals("enabled")) continue;
                            if (renderedCount >= maxVisibleOptions) break;

                            float localAlphaF = Math.min(1f, Math.max(0f, (innerVisible - renderedCount * subOptionHeight) / (float) subOptionHeight));
                            int localAlpha = (int)(255 * expandProgress * localAlphaF);

                            int subX1 = containerX1 + 4;
                            int subX2 = containerX2 - 4;

                            ValueStyleRenderer.renderValue(context, client, module, value, theme,
                                                         subX1, subModY, subX2, subOptionHeight,
                                                         (float)scaledMouseX, (float)scaledMouseY, localAlpha,
                                                         this::getTooltipText,
                                                         this::getTooltipTextWithColors,
                                                         this::getDisplayTextFormatter,
                                                         listeningButtonValueName,
                                                         tooltipInfo);
                            
                            if (tooltipInfo.tooltip != null) {
                                hoveredTooltip = tooltipInfo.tooltip;
                                hoveredTooltipText = tooltipInfo.tooltipText;
                                tooltipX = tooltipInfo.x;
                                tooltipY = tooltipInfo.y;
                            }

                            subModY += subOptionHeight;
                            
                            renderedCount++;
                        }
                    }

                    modY += containerHeight + 5; 
                }
            }
        }
        
        context.disableScissor();

        if (contentHeight > visibleHeight) {
            float ratio = visibleHeight / contentHeight;
            float barHeight = Math.max(10, visibleHeight * ratio); 
            float travel = visibleHeight - barHeight;
            float progress = maxScroll == 0 ? 0 : (scrollOffset / maxScroll);
            float barY = visibleTop + travel * progress;
            float barX1 = width - 6;
            float barX2 = width - 2;
            GuiRenderUtil.drawRoundedRect(context, barX1, barY, barX2, barY + barHeight, 2, theme.BG_2.getRGB());
        }

        String watermark = "Baity by 11YearCookieBuff (AKA raueyhs , shyeuar)";
        int wmRawWidth = client.textRenderer.getWidth(watermark);
        float wmScale = 0.70f;
        float scaledWidth = wmScale * wmRawWidth;
        
        float baseX = width - scaledWidth - 8; 
        float baseY = 8; 
        
        context.getMatrices().push();
        context.getMatrices().scale(wmScale, wmScale, 1f);
        int wmColor = new java.awt.Color(120, 124, 132).getRGB();
        context.drawText(client.textRenderer, watermark, (int)(baseX / wmScale), (int)(baseY / wmScale), wmColor, false);
        context.getMatrices().pop();
        
        context.getMatrices().pop();
        
        if (hoveredTooltip != null) {
            tooltipX = (int)(mouseX + 5);
            tooltipY = (int)(mouseY + 5);
            
            float tooltipScaleRatio = BASE_GUI_SCALE / guiScale;
            float tipScale = 0.75f * tooltipScaleRatio;
            
            int rawTextWidth;
            if (hoveredTooltipText != null) {
                rawTextWidth = client.textRenderer.getWidth(hoveredTooltipText);
            } else {
                rawTextWidth = client.textRenderer.getWidth(hoveredTooltip);
            }
            
            int bgPadding = 10;
            int rawFontHeight = 9;
            int rawTooltipWidth = rawTextWidth + bgPadding;
            int rawTooltipHeight = rawFontHeight + 8;
            
            int scaledTooltipWidth = (int)(rawTooltipWidth * tipScale);
            int scaledTooltipHeight = (int)(rawTooltipHeight * tipScale);
            
            int finalTooltipX = tooltipX;
            int finalTooltipY = tooltipY;
            
            if (finalTooltipX + scaledTooltipWidth > client.getWindow().getScaledWidth()) {
                finalTooltipX = tooltipX - scaledTooltipWidth - 10;
            }
            if (finalTooltipY - scaledTooltipHeight < 0) {
                finalTooltipY = tooltipY + 10;
            }
            
            context.getMatrices().push();
            context.getMatrices().scale(tipScale, tipScale, 1f);
            
            int bgLeft = (int)(finalTooltipX / tipScale);
            int bgTop = (int)((finalTooltipY - scaledTooltipHeight) / tipScale);
            int bgRight = bgLeft + rawTooltipWidth;
            int bgBottom = bgTop + rawTooltipHeight;
            
            GuiRenderUtil.drawRoundedRect(context, bgLeft, bgTop, bgRight, bgBottom, 3, theme.BG_2.getRGB());
            
            int textDrawX = bgLeft + 5;
            int textDrawY = bgTop + 4;
            
            if (hoveredTooltipText != null) {
                context.drawText(client.textRenderer, hoveredTooltipText, textDrawX, textDrawY, 0xFFFFFF, false);
            } else if (hoveredTooltip != null) {
                context.drawText(client.textRenderer, hoveredTooltip, textDrawX, textDrawY, theme.FONT_C.getRGB(), false);
            }
            context.getMatrices().pop();
        }
    }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                float scaleRatio = BASE_GUI_SCALE / guiScale;
                float scaledMouseX = ((float)mouseX - windowX) / scaleRatio;
                float scaledMouseY = ((float)mouseY - windowY) / scaleRatio;
                
                if (GuiRenderUtil.isHovered(0, listTopPadding, width, height - 20, scaledMouseX, scaledMouseY)) {
            float modY = 60 - scrollOffset;
            for (Module module : ModuleManager.getModulesByCategory(modCategory)) {
                if (module.isExpanded()) {
                    int subOptionCount = 0;
                    for (Value value : module.getValues()) {
                        if (!value.getName().equals("enabled")) subOptionCount++;
                    }
                    
                    if (subOptionCount > 0) {
                        int containerPadding = 8;
                        int subOptionHeight = 20;
                        int containerHeight = subOptionCount * subOptionHeight + containerPadding * 2;
                        
                        int containerX1 = (int)(30);
                        int containerX2 = (int)(width - 30);
                        
                        float subModY = modY + containerPadding;
                        for (Value value : module.getValues()) {
                            if (value.getName().equals("enabled")) continue;
                            
                            Object currentVal = value.getValue();
                            var handler = ValueTypeRegistry.getHandlerForValue(currentVal);
                            if (handler != null && 
                                GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, containerX2 - 4, (int)(subModY + subOptionHeight), scaledMouseX, scaledMouseY)) {
                                
                                Object newValue = handler.updateValue(currentVal, verticalAmount);
                                if (newValue != currentVal) {
                                    value.setValue(newValue);
                                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), newValue);
                                    }
                                    return true;
                                }
                            }
                            
                    if (GuiRenderUtil.isHovered(containerX1, (int)subModY, containerX2, (int)(subModY + subOptionHeight), scaledMouseX, scaledMouseY)) {
                        float delta = (float)(-verticalAmount * 20);
                        scrollOffset += delta;
                        return true;
                    }
                            
                            subModY += subOptionHeight;
                        }
                        
                        modY += containerHeight + 5;
                    }
                }
                modY += 30;
            }
            
            float delta = (float)(-verticalAmount * 20);
            scrollOffset += delta;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((isListeningForKey || listeningButtonValueName != null) && button >= 2 && button <= 4) {
            int mouseKeyCode = button;
            
            if (isListeningForKey) {
                ConfigManager.guiKeyCode = mouseKeyCode;
                ConfigManager.saveConfig();
                updateKeyDisplay();
                isListeningForKey = false;
                return true;
            }
            
            if (listeningButtonValueName != null) {
                for (Module module : ModuleManager.getModules()) {
                    for (Value value : module.getValues()) {
                        if (value instanceof ButtonValue && value.getName().equals(listeningButtonValueName)) {
                            ButtonValue buttonValue = (ButtonValue) value;
                            buttonValue.setValue(mouseKeyCode);
                            
                            if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                                ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), mouseKeyCode);
                            }
                            
                            listeningButtonValueName = null;
                            return true;
                        }
                    }
                }
                listeningButtonValueName = null;
                return true;
            }
        }
        
        float scaleRatio = BASE_GUI_SCALE / guiScale;
        float scaledMouseX = ((float)mouseX - windowX) / scaleRatio;
        float scaledMouseY = ((float)mouseY - windowY) / scaleRatio;
        
        if (button == 0 && GuiRenderUtil.isHovered(0, 0, width, 20, scaledMouseX, scaledMouseY)) {
            if (dragX == 0 && dragY == 0) {
                dragX = scaledMouseX;
                dragY = scaledMouseY;
            } else {
                windowX = (float)mouseX - dragX * scaleRatio;
                windowY = (float)mouseY - dragY * scaleRatio;
            }
            drag = true;
            return true;
        }
        
        float cateX = 20;
        float cateY = 30;
        for (ModuleCategory category : ModuleCategory.values()) {

            assert client != null;
            String label = category.getDisplayName();
            int textWidth = client.textRenderer.getWidth(label);
            if (button == 0 && GuiRenderUtil.isHovered(cateX, cateY, cateX + textWidth, cateY + 12, scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                modCategory = category;
                valuetimer.reset();
                return true;
            }
            cateX += textWidth + 28;
        }
        
        float modY = 60 - scrollOffset;
        List<Module> modules = ModuleManager.getModulesByCategory(modCategory);
        
        for (Module module : modules) {
            if (GuiRenderUtil.isHovered(20, modY, width - 20, modY + 25, scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                if (module.getName().equals("ClickGUI")) {
                    assert client != null;
                    String keyText = isListeningForKey ? "Press a key..." : currentKeyDisplay;
                    String plainText = keyText.replaceAll("§[0-9a-fklmnor]", "");
                    int keyTextWidth = client.textRenderer.getWidth(plainText);
                    int keyBoxWidth = keyTextWidth + 16;
                    float boxCenterY = modY + 25 / 2f;
                    int boxHeight = 12; 
                    int containerX2 = (int)(width - 20);
                    int keyBoxX1 = (int)(containerX2 - keyBoxWidth - 10);
                    int keyBoxY1 = (int)(boxCenterY - boxHeight / 2f);
                    int keyBoxX2 = (int)(containerX2 - 10); 
                    int keyBoxY2 = (int)(boxCenterY + boxHeight / 2f);
                    
                    if (button == 0 && GuiRenderUtil.isHovered(keyBoxX1, keyBoxY1, keyBoxX2, keyBoxY2, scaledMouseX, scaledMouseY)) {
                        isListeningForKey = true;
                        valuetimer.reset();
                    }
                } else {
                    boolean hasChildrenClick = false;
                    for (Value v : module.getValues()) {
                        if (!"enabled".equals(v.getName())) { hasChildrenClick = true; break; }
                    }

                    if (button == 0) {
                        if (hasChildrenClick && GuiRenderUtil.isHovered(width - 35, modY, width - 15, modY + 25, scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                            module.toggleExpanded();
                        } else {
                            module.toggle();
                            if (ConfigSynchronizer.hasModuleConfig(module.getName())) {
                                ConfigSynchronizer.handleModuleToggle(module.getName(), module.isEnabled());
                            }
                        }
                    }
                    else if (button == 1 && hasChildrenClick) {
                        module.toggleExpanded();
                    }
                }
                valuetimer.reset();
                return true;
            }
            
            modY += 30;
            
            if (module.isExpanded()) {
                int subOptionCount = 0;
                for (Value value : module.getValues()) {
                    if (!value.getName().equals("enabled")) subOptionCount++;
                }
                
                if (subOptionCount > 0) {
                    int containerPadding = 8;
                    int subOptionHeight = 20;
                    int containerHeight = subOptionCount * subOptionHeight + containerPadding * 2;
                    
                    int containerX1 = (int)(30);
                    int containerX2 = (int)(width - 30);
                    
                    float subModY = modY + containerPadding;
                    int innerVisible = Math.max(0, containerHeight - containerPadding * 2);
                    int maxVisibleOptions = Math.max(0, innerVisible / subOptionHeight);
                    int renderedCount = 0;
                    for (Value value : module.getValues()) {
                        if (value.getName().equals("enabled")) continue;
                        if (renderedCount >= maxVisibleOptions) break;

                        if (button == 0 && valuetimer.delay(100)) {
                            ValueStyle style = value.getStyle();
                            if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
                                ButtonValue buttonValue = (ButtonValue) value;
                                String boxText = buttonValue.getDisplayText(this::getDisplayTextFormatter);
                                String plainText = boxText.replaceAll("§[0-9a-fklmnor]", "");
                                int boxTextWidth = client.textRenderer.getWidth(plainText);
                                int boxWidth = boxTextWidth + 16;
                                float boxCenterY = subModY + subOptionHeight / 2f;
                                int boxHeight = 12;
                                int subX2 = containerX2 - 4;
                                int boxX1 = (int)(subX2 - boxWidth - 10); 
                                int boxY1 = (int)(boxCenterY - boxHeight / 2f);
                                int boxX2 = (int)(subX2 - 10);
                                int boxY2 = (int)(boxCenterY + boxHeight / 2f); 
                                
                                if (GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, scaledMouseX, scaledMouseY)) {
                                    listeningButtonValueName = value.getName();
                                    valuetimer.reset();
                                    return true;
                                }
                            } else {
                                if (GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, containerX2 - 4, (int)(subModY + subOptionHeight), scaledMouseX, scaledMouseY)) {
                                    if (value.getValue() instanceof Boolean) {
                                        value.setValue(!((Boolean)value.getValue()));
                                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), value.getValue());
                                        }
                                    }
                                    valuetimer.reset();
                                    return true;
                                }
                            }
                        }

                        subModY += subOptionHeight;
                        renderedCount++;
                    }
                    
                    modY += containerHeight + 5;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            drag = false;
            dragX = 0;
            dragY = 0;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (drag) {
            float scaleRatio = BASE_GUI_SCALE / guiScale;
            
            windowX = (float)mouseX - dragX * scaleRatio;
            windowY = (float)mouseY - dragY * scaleRatio;
        }
        super.mouseMoved(mouseX, mouseY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isListeningForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isListeningForKey = false;
                return true;
            }
            
            if (com.shyeuar.baity.utils.KeyMappingUtils.isResetKey(keyCode)) {
                ConfigManager.guiKeyCode = 0;
                ConfigManager.saveConfig();
                updateKeyDisplay();
                isListeningForKey = false;
                return true;
            }
            
            ConfigManager.guiKeyCode = keyCode;
            ConfigManager.saveConfig();
            updateKeyDisplay();
            isListeningForKey = false;
            
            return true;
        }
        
        if (listeningButtonValueName != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningButtonValueName = null;
                return true;
            }
            
            if (com.shyeuar.baity.utils.KeyMappingUtils.isResetKey(keyCode)) {
                for (Module module : ModuleManager.getModules()) {
                    for (Value value : module.getValues()) {
                        if (value instanceof ButtonValue && value.getName().equals(listeningButtonValueName)) {
                            ButtonValue buttonValue = (ButtonValue) value;
                            buttonValue.setValue(0);
                            
                            if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                                ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), 0);
                            }
                            
                            listeningButtonValueName = null;
                            return true;
                        }
                    }
                }
                listeningButtonValueName = null;
                return true;
            }
            
            for (Module module : ModuleManager.getModules()) {
                for (Value value : module.getValues()) {
                    if (value instanceof ButtonValue && value.getName().equals(listeningButtonValueName)) {
                        ButtonValue buttonValue = (ButtonValue) value;
                        buttonValue.setValue(keyCode);
                        
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), keyCode);
                        }
                        
                        listeningButtonValueName = null;
                        return true;
                    }
                }
            }
            
            listeningButtonValueName = null;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isListeningForKey || listeningButtonValueName != null) {
                isListeningForKey = false;
                listeningButtonValueName = null;
                return true;
            }
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        if (this.client != null && this.client.getWindow() != null) {
            float scaleRatio = BASE_GUI_SCALE / guiScale;
            float screenW = this.client.getWindow().getScaledWidth();
            float screenH = this.client.getWindow().getScaledHeight();
            windowX = (screenW - ClickGui.width * scaleRatio) / 2f;
            windowY = (screenH - ClickGui.height * scaleRatio) / 2f;
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
