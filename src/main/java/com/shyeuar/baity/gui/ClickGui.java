package com.shyeuar.baity.gui;

import com.shyeuar.baity.config.BaityConfig;
import com.shyeuar.baity.gui.modules.Module;
import com.shyeuar.baity.gui.modules.ModuleManager;
import com.shyeuar.baity.gui.utils.RenderUtil;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.utils.TimerUtil;
import com.shyeuar.baity.gui.values.ModuleCategory;
import com.shyeuar.baity.gui.values.Value;
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
    // 基准缩放比例（GUI缩放为3时）
    private static final float BASE_GUI_SCALE = 3.0f;
    
    // 选择状态
    private static ModuleCategory modCategory = ModuleCategory.FUN;
    
    private final TimerUtil valuetimer = new TimerUtil();
    
    public static Theme theme = new Theme();
    
    private boolean isListeningForKey = false;
    private String currentKeyDisplay = "Right Ctrl";
    
    private final java.util.Map<String, Float> moduleExpandAnimations = new java.util.HashMap<>();

    private String hoveredTooltip = null;
    private int tooltipX = 0, tooltipY = 0;

    private float scrollOffset = 0f;
    private static final float listTopPadding = 60f;
    private static final float itemHeight = 30f;
    
    public ClickGui() {
        super(Text.literal("Baity ClickGui"));
    }
    
    @Override
    protected void init() {
        super.init();
        theme.setDark(); // Set default theme
        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }

        // 获取GUI缩放
        if (this.client != null) {
            this.guiScale = this.client.options.getGuiScale().getValue();
        }
        
        syncModuleStates();

        // 计算缩放比例
        float scaleRatio = BASE_GUI_SCALE / guiScale;
        
        // 居中计算（基于固定尺寸）
        if (this.client != null && this.client.getWindow() != null) {
            float screenW = this.client.getWindow().getScaledWidth();
            float screenH = this.client.getWindow().getScaledHeight();
            windowX = (screenW - width * scaleRatio) / 2f;
            windowY = (screenH - height * scaleRatio) / 2f;
        }
    }

    private void syncModuleStates() {
        Module smolPeopleModule = ModuleManager.getModuleByName("SmolPeople");
        if (smolPeopleModule != null) {
            smolPeopleModule.setEnabled(BaityConfig.smolpeopleMode);
            for (Value value : smolPeopleModule.getValues()) {
                if (value.getName().equals("crosshair")) {
                    value.setValue(BaityConfig.crosshairMode);
                }
            }
        }
        
        Module blockAnimationModule = ModuleManager.getModuleByName("BlockAnimation");
        if (blockAnimationModule != null) {
            blockAnimationModule.setEnabled(BaityConfig.blockAnimationMode);
        }
        
        Module ClickGUIModule = ModuleManager.getModuleByName("ClickGUI");
        if (ClickGUIModule != null) {
            ClickGUIModule.setEnabled(BaityConfig.guiEnabled);
        }

        Module playerEsp = ModuleManager.getModuleByName("PlayerESP");
        if (playerEsp != null) {
            playerEsp.setEnabled(BaityConfig.playerEspEnabled);
            for (Value value : playerEsp.getValues()) {
                if (value.getName().equals("show distance")) {
                    value.setValue(BaityConfig.playerEspShowDistance);
                } else if (value.getName().equals("show own nametag")) {
                    value.setValue(BaityConfig.playerEspShowOwnNametag);
                }
            }
        }
        
        updateKeyDisplay();
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
    
    private String getTooltipText(String moduleName) {
        return switch (moduleName) {
            case "SmolPeople" -> "Make your character smaller and cuter";
            case "BlockAnimation" -> "Restored the blocking animation of version 1.8";
            case "PepCat" -> "Play an animation and give pep talk when you died. It's a skill issue!";
            default -> null;
        };
    }
    
    private void updateKeyDisplay() {
        // 将键码转换为可读的按键名称
        switch (BaityConfig.guiKeyCode) {
            // 控制键（基于GLFW键码）
            case 340: currentKeyDisplay = "Left Shift"; break;
            case 344: currentKeyDisplay = "Right Shift"; break;
            case 341: currentKeyDisplay = "Left Ctrl"; break;
            case 345: currentKeyDisplay = "Right Ctrl"; break;
            case 342: currentKeyDisplay = "Left Alt"; break;
            case 346: currentKeyDisplay = "Right Alt"; break;
            case 343: currentKeyDisplay = "Left Super"; break;
            case 347: currentKeyDisplay = "Right Super"; break;
            
            // 功能键
            case 32: currentKeyDisplay = "Space"; break;
            case 257: currentKeyDisplay = "Enter"; break;
            case 256: currentKeyDisplay = "Escape"; break;
            case 258: currentKeyDisplay = "Tab"; break;
            case 259: currentKeyDisplay = "Backspace"; break;
            case 260: currentKeyDisplay = "Insert"; break;
            case 261: currentKeyDisplay = "Delete"; break;
            case 262: currentKeyDisplay = "Right"; break;
            case 263: currentKeyDisplay = "Left"; break;
            case 264: currentKeyDisplay = "Down"; break;
            case 265: currentKeyDisplay = "Up"; break;
            case 266: currentKeyDisplay = "Page Up"; break;
            case 267: currentKeyDisplay = "Page Down"; break;
            case 268: currentKeyDisplay = "Home"; break;
            case 269: currentKeyDisplay = "End"; break;
            case 280: currentKeyDisplay = "Caps Lock"; break;
            case 281: currentKeyDisplay = "Scroll Lock"; break;
            case 282: currentKeyDisplay = "Num Lock"; break;
            case 283: currentKeyDisplay = "Print Screen"; break;
            case 284: currentKeyDisplay = "Pause"; break;
            
            // F键
            case 290: currentKeyDisplay = "F1"; break;
            case 291: currentKeyDisplay = "F2"; break;
            case 292: currentKeyDisplay = "F3"; break;
            case 293: currentKeyDisplay = "F4"; break;
            case 294: currentKeyDisplay = "F5"; break;
            case 295: currentKeyDisplay = "F6"; break;
            case 296: currentKeyDisplay = "F7"; break;
            case 297: currentKeyDisplay = "F8"; break;
            case 298: currentKeyDisplay = "F9"; break;
            case 299: currentKeyDisplay = "F10"; break;
            case 300: currentKeyDisplay = "F11"; break;
            case 301: currentKeyDisplay = "F12"; break;
            
            // 数字
            case 48: currentKeyDisplay = "0"; break;
            case 49: currentKeyDisplay = "1"; break;
            case 50: currentKeyDisplay = "2"; break;
            case 51: currentKeyDisplay = "3"; break;
            case 52: currentKeyDisplay = "4"; break;
            case 53: currentKeyDisplay = "5"; break;
            case 54: currentKeyDisplay = "6"; break;
            case 55: currentKeyDisplay = "7"; break;
            case 56: currentKeyDisplay = "8"; break;
            case 57: currentKeyDisplay = "9"; break;
            
            // 字母
            case 65: currentKeyDisplay = "A"; break;
            case 66: currentKeyDisplay = "B"; break;
            case 67: currentKeyDisplay = "C"; break;
            case 68: currentKeyDisplay = "D"; break;
            case 69: currentKeyDisplay = "E"; break;
            case 70: currentKeyDisplay = "F"; break;
            case 71: currentKeyDisplay = "G"; break;
            case 72: currentKeyDisplay = "H"; break;
            case 73: currentKeyDisplay = "I"; break;
            case 74: currentKeyDisplay = "J"; break;
            case 75: currentKeyDisplay = "K"; break;
            case 76: currentKeyDisplay = "L"; break;
            case 77: currentKeyDisplay = "M"; break;
            case 78: currentKeyDisplay = "N"; break;
            case 79: currentKeyDisplay = "O"; break;
            case 80: currentKeyDisplay = "P"; break;
            case 81: currentKeyDisplay = "Q"; break;
            case 82: currentKeyDisplay = "R"; break;
            case 83: currentKeyDisplay = "S"; break;
            case 84: currentKeyDisplay = "T"; break;
            case 85: currentKeyDisplay = "U"; break;
            case 86: currentKeyDisplay = "V"; break;
            case 87: currentKeyDisplay = "W"; break;
            case 88: currentKeyDisplay = "X"; break;
            case 89: currentKeyDisplay = "Y"; break;
            case 90: currentKeyDisplay = "Z"; break;
            
            // 小键盘
            case 320: currentKeyDisplay = "Num 0"; break;
            case 321: currentKeyDisplay = "Num 1"; break;
            case 322: currentKeyDisplay = "Num 2"; break;
            case 323: currentKeyDisplay = "Num 3"; break;
            case 324: currentKeyDisplay = "Num 4"; break;
            case 325: currentKeyDisplay = "Num 5"; break;
            case 326: currentKeyDisplay = "Num 6"; break;
            case 327: currentKeyDisplay = "Num 7"; break;
            case 328: currentKeyDisplay = "Num 8"; break;
            case 329: currentKeyDisplay = "Num 9"; break;
            
            // 其他
            case 96: currentKeyDisplay = "`"; break;
            case 45: currentKeyDisplay = "-"; break;
            case 61: currentKeyDisplay = "="; break;
            case 91: currentKeyDisplay = "["; break;
            case 93: currentKeyDisplay = "]"; break;
            case 92: currentKeyDisplay = "\\"; break;
            case 59: currentKeyDisplay = ";"; break;
            case 39: currentKeyDisplay = "'"; break;
            case 44: currentKeyDisplay = ","; break;
            case 46: currentKeyDisplay = "."; break;
            case 47: currentKeyDisplay = "/"; break;
            
            default: currentKeyDisplay = "Key " + BaityConfig.guiKeyCode; break;
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();

        updateModuleExpandAnimations();

        hoveredTooltip = null;

        // 计算缩放比例
        float scaleRatio = BASE_GUI_SCALE / guiScale;
        
        // 应用矩阵变换，保持固定尺寸
        context.getMatrices().push();
        context.getMatrices().translate(windowX, windowY, 0);
        context.getMatrices().scale(scaleRatio, scaleRatio, 1.0f);
        
        // 转换鼠标坐标到固定坐标系
        float scaledMouseX = ((float)mouseX - windowX) / scaleRatio;
        float scaledMouseY = ((float)mouseY - windowY) / scaleRatio;
        
        // 绘制主窗口（使用固定坐标系）
        RenderUtil.drawRoundedRect(context, 0, 0, width, height, 6, theme.BG.getRGB());
        RenderUtil.stroke1px(context, 0, 0, width, height, new java.awt.Color(255,255,255,20).getRGB());

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
                
                RenderUtil.divider(context, lineLeft, cateY + 12, lineRight, cateY + 13, new java.awt.Color(255,255,255,64).getRGB());
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
        
        // 检查当前分类是否有模块，如果没有则显示占位文本
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
            boolean hovered = RenderUtil.isHovered(20, modY, width - 20, modY + 25, (float)scaledMouseX, (float)scaledMouseY);
            int enabledBg = new java.awt.Color(54, 42, 150).getRGB();
            int cardBg = module.isEnabled() ? enabledBg : theme.Modules.getRGB();
            RenderUtil.drawRoundedRect(context, 20, modY, width - 20, modY + 25, 6, cardBg);
            if (hovered && !"ClickGUI".equals(module.getName())) {
                int hi = new java.awt.Color(255,255,255,24).getRGB();
                int lx = (int)(21);
                int ty = (int)(modY + 1);
                int rx = (int)(width - 21);
                int by = (int)(modY + 24);
                context.fill(lx, ty, rx, by, hi);
            }

            String displayName = module.getName();
            if ("ClickGUI".equals(module.getName())) {
                displayName = "ClickGUI";
            }
            context.drawText(client.textRenderer, displayName, (int)(30), (int)(modY + 8), theme.FONT_C.getRGB(), false);
            
            if (RenderUtil.isHovered(20, modY, width - 20, modY + 25, (float)scaledMouseX, (float)scaledMouseY)) {
                String tooltip = getTooltipText(module.getName());
                if (tooltip != null) {
                    hoveredTooltip = tooltip;
                    // 根据GUI缩放调整tooltip与鼠标的距离
                    float tooltipOffset = 10f * (BASE_GUI_SCALE / guiScale);
                    tooltipX = (int)(mouseX + tooltipOffset);
                    tooltipY = (int)(mouseY - tooltipOffset);
                }
            }
            
            boolean hasChildren = false;
            for (Value v : module.getValues()) {
                if (!"enabled".equals(v.getName())) { hasChildren = true; break; }
            }
            if (hasChildren && !module.getName().equals("ClickGUI")) {
                String arrow = module.isExpanded() ? "▼" : "▶";
                context.drawText(client.textRenderer, arrow, (int)(width - 40), (int)(modY + 8), theme.FONT_C.getRGB(), false);
            }
            
            if (module.getName().equals("ClickGUI")) {
                int keyTextWidth = client.textRenderer.getWidth("Right Shift");
                int keyBoxWidth = keyTextWidth + 20; 
                
                boolean keyHovered = RenderUtil.isHovered(width - keyBoxWidth - 50, modY, width - 50, modY + 20, (float)scaledMouseX, (float)scaledMouseY);
                int keyBgColor = isListeningForKey ? theme.BG_3.getRGB() : (keyHovered ? new java.awt.Color(255,255,255,24).getRGB() : theme.BG_2.getRGB());
                int kx1 = (int)(width - keyBoxWidth - 49);
                int ky1 = (int)(modY + 6);
                int kx2 = (int)(width - 51);
                int ky2 = (int)(modY + 19);
                context.fill(kx1, ky1, kx2, ky2, keyBgColor);
                
                String keyText = isListeningForKey ? "Press a key..." : currentKeyDisplay;
                int keyColor = isListeningForKey ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
                context.drawText(client.textRenderer, keyText, (int)(width - keyBoxWidth - 40), (int)(modY + 8), keyColor, false);
            }

            modY += 30;
            
            int subOptionCount = 0;
            for (Value value : module.getValues()) {
                if (!value.getName().equals("enabled")) subOptionCount++;
            }
            
            if (subOptionCount > 0) {
                float expandProgress = getModuleExpandProgress(module.getName());
                
                // 只有当动画进度大于0时才渲染（展开或收回动画中）
                if (expandProgress > 0.0f) {
                    int containerPadding = 8; // 大框内边距
                    int subOptionHeight = 20; // 每个子选项高度
                    int maxContainerHeight = (int)(visibleHeight - 80); // 限制最大高度，比GUI底部短80px
                    int fullContainerHeight = subOptionCount * subOptionHeight + containerPadding * 2;
                    int containerHeight = Math.min(fullContainerHeight, maxContainerHeight);
                    containerHeight = (int)(containerHeight * expandProgress); // 应用动画
                    
                    int containerBg = new java.awt.Color(30, 30, 30, 200).getRGB();
                    int containerX1 = (int)(30);
                    int containerY1 = (int)(modY);
                    int containerX2 = (int)(width - 30);
                    int containerY2 = (int)(modY + containerHeight);

                    context.fill(containerX1, containerY1, containerX2, containerY2, containerBg);
                    RenderUtil.stroke1px(context, containerX1, containerY1, containerX2, containerY2, new java.awt.Color(255,255,255,40).getRGB());

                    int innerVisible = Math.max(0, containerHeight - containerPadding * 2);
                    // 如果内部可见高度太小，不渲染子项，避免早期闪烁
                    // 提高显示阈值：至少达到半个子项高度才开始渲染首项
                    if (innerVisible >= subOptionHeight / 2) {
                        float subModY = modY + containerPadding;
                        int maxVisibleOptions = Math.max(0, innerVisible / subOptionHeight);

                        int renderedCount = 0;
                        for (Value value : module.getValues()) {
                            if (value.getName().equals("enabled")) continue;
                            if (renderedCount >= maxVisibleOptions) break;

                            // 针对当前项的局部透明度（项的可见部分占比，缓解项出现/消失边缘的突变）
                            float localAlphaF = Math.min(1f, Math.max(0f, (innerVisible - renderedCount * subOptionHeight) / (float) subOptionHeight));
                            int localAlpha = (int)(255 * expandProgress * localAlphaF);

                            boolean subHovered = RenderUtil.isHovered(containerX1 + 4, (int)subModY, containerX2 - 4, (int)(subModY + subOptionHeight), (float)scaledMouseX, (float)scaledMouseY);
                            int baseValueColor = subHovered ? new java.awt.Color(60, 60, 60, 80).getRGB() : new java.awt.Color(40, 40, 40, 50).getRGB();
                            int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
                            
                            if (subHovered && "show own nametag".equals(value.getName())) {
                                hoveredTooltip = "Due to skill issue,you should switch off all the other nametag functions before using this feature,or the game will crash!";
                                // 根据GUI缩放调整tooltip与鼠标的距离
                                float tooltipOffset = 10f * (BASE_GUI_SCALE / guiScale);
                                tooltipX = (int)(mouseX + tooltipOffset);
                                tooltipY = (int)(mouseY - tooltipOffset);
                            }

                            int subX1 = containerX1 + 4;
                            int subY1 = (int)subModY;
                            int subX2 = containerX2 - 4;
                            int subY2 = (int)(subModY + subOptionHeight);
                            RenderUtil.drawRoundedRect(context, subX1, subY1, subX2, subY2, 6, valueColor);

                            int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
                            
                            // 添加警告标识，由于技术问题，需要关闭其他nametag功能，否则游戏会崩溃
                            String displayText = value.getDisplayName();
                            int warningTextColor = textColor;
                            if ("show own nametag".equals(value.getName())) {
                                displayText = "⚠ " + displayText;
                                warningTextColor = (com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR & 0x00FFFFFF) | (localAlpha << 24);
                            }
                            context.drawText(client.textRenderer, displayText, subX1 + 8, (int)(subModY + 6), warningTextColor, false);

                            String status;
                            int statusColor;
                            if (value.getValue() instanceof Boolean) {
                                boolean boolValue = (Boolean) value.getValue();
                                status = boolValue ? "ON" : "OFF";
                                statusColor = boolValue ? theme.BG_3.getRGB() : theme.FONT.getRGB();
                            } else if (value.getValue() instanceof Double) {
                                double doubleValue = (Double) value.getValue();
                                status = String.format("%.1f", doubleValue);
                                statusColor = theme.FONT.getRGB();
                            } else if (value.getValue() instanceof String) {
                                status = (String) value.getValue();
                                statusColor = theme.FONT.getRGB();
                            } else {
                                status = value.getValue().toString();
                                statusColor = theme.FONT.getRGB();
                            }
                            if (!module.isEnabled()) {
                                statusColor = theme.FONT.getRGB();
                            }
                            statusColor = (statusColor & 0x00FFFFFF) | (localAlpha << 24);

                            int statusX = subX2 - 40;
                            if (value.getValue() instanceof Double) {
                                statusX = subX2 - 60;
                            } else if (value.getValue() instanceof String) {
                                statusX = subX2 - 80;
                            }
                            context.drawText(client.textRenderer, status, statusX, (int)(subModY + 6), statusColor, false);

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
            RenderUtil.drawRoundedRect(context, barX1, barY, barX2, barY + barHeight, 2, theme.BG_2.getRGB());
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
        
        // 恢复矩阵变换
        context.getMatrices().pop();
        
        // 渲染tooltip（在矩阵变换之外，使用屏幕坐标）
        if (hoveredTooltip != null) {
            // 计算tooltip缩放比例，保持固定尺寸
            float tooltipScaleRatio = BASE_GUI_SCALE / guiScale;
            float tipScale = 0.75f * tooltipScaleRatio;
            
            int rawTextWidth = client.textRenderer.getWidth(hoveredTooltip);
            int bgPadding = 10;
            int rawFontHeight = 9; // vanilla text height近似
            int tooltipWidth = (int)(rawTextWidth * tipScale) + bgPadding;
            int tooltipHeight = (int)(rawFontHeight * tipScale) + 8;
            
            // 使用原始鼠标坐标计算tooltip位置
            int finalTooltipX = tooltipX;
            int finalTooltipY = tooltipY;
            
            // 边界检查
            if (finalTooltipX + tooltipWidth > client.getWindow().getScaledWidth()) {
                finalTooltipX = tooltipX - tooltipWidth - 20;
            }
            if (finalTooltipY - tooltipHeight < 0) {
                finalTooltipY = tooltipY + 20;
            }
            
            int bgLeft = finalTooltipX;
            int bgTop = finalTooltipY - tooltipHeight;
            int bgRight = finalTooltipX + tooltipWidth;
            int bgBottom = finalTooltipY;
            
            RenderUtil.drawRoundedRect(context, bgLeft, bgTop, bgRight, bgBottom, 3, theme.BG_2.getRGB());
            
            context.getMatrices().push();
            context.getMatrices().scale(tipScale, tipScale, 1f);
            int textDrawX = (int)((bgLeft + 5) / tipScale);
            int textDrawY = (int)(((bgTop + 4)) / tipScale);
            
            // 为"show own nametag"的tooltip使用浅红色
            int tooltipColor = hoveredTooltip.contains("Due to skill issue") ? 
                com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR : theme.FONT_C.getRGB();
            context.drawText(client.textRenderer, hoveredTooltip, textDrawX, textDrawY, tooltipColor, false);
            context.getMatrices().pop();
        }
    }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                // 转换鼠标坐标到固定坐标系
                float scaleRatio = BASE_GUI_SCALE / guiScale;
                float scaledMouseX = ((float)mouseX - windowX) / scaleRatio;
                float scaledMouseY = ((float)mouseY - windowY) / scaleRatio;
                
                if (RenderUtil.isHovered(0, listTopPadding, width, height - 20, scaledMouseX, scaledMouseY)) {
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
                            
                            if (value.getValue() instanceof Double && 
                                RenderUtil.isHovered(containerX1 + 4, (int)subModY, containerX2 - 4, (int)(subModY + subOptionHeight), scaledMouseX, scaledMouseY)) {
                                
                                double currentValue = (Double) value.getValue();
                                double increment = 0.1; 
                                if (verticalAmount > 0) {
                                    currentValue += increment;
                                } else if (verticalAmount < 0) {
                                    currentValue -= increment;
                                }
                                
                                // 数值范围限制（简化实现）
                                currentValue = Math.max(0.0, Math.min(100.0, currentValue));
                                
                                value.setValue(currentValue);
                                return true;
                            }
                            
                    if (RenderUtil.isHovered(containerX1, (int)subModY, containerX2, (int)(subModY + subOptionHeight), scaledMouseX, scaledMouseY)) {
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
        // 转换鼠标坐标到固定坐标系
        float scaleRatio = BASE_GUI_SCALE / guiScale;
        float scaledMouseX = ((float)mouseX - windowX) / scaleRatio;
        float scaledMouseY = ((float)mouseY - windowY) / scaleRatio;
        
        if (button == 0 && RenderUtil.isHovered(0, 0, width, 20, scaledMouseX, scaledMouseY)) {
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
            int textWidth = client.textRenderer.getWidth(category.getDisplayName());
            if (button == 0 && RenderUtil.isHovered(cateX, cateY, cateX + textWidth, cateY + 12, scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                modCategory = category;
                valuetimer.reset();
                return true;
            }
            cateX += textWidth + 28;
        }
        
        float modY = 60 - scrollOffset;
        List<Module> modules = ModuleManager.getModulesByCategory(modCategory);
        
        for (Module module : modules) {
            if (RenderUtil.isHovered(20, modY, width - 20, modY + 25, scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                if (module.getName().equals("ClickGUI")) {
                    assert client != null;
                    int keyTextWidth = client.textRenderer.getWidth("Right Shift");
                    int keyBoxWidth = keyTextWidth + 20;
                    
                    if (button == 0 && RenderUtil.isHovered(width - keyBoxWidth - 50, modY, width - 50, modY + 25, scaledMouseX, scaledMouseY)) {
                        isListeningForKey = true;
                    }
                } else {
                    boolean hasChildrenClick = false;
                    for (Value v : module.getValues()) {
                        if (!"enabled".equals(v.getName())) { hasChildrenClick = true; break; }
                    }

                    if (button == 0) {
                        if (hasChildrenClick && RenderUtil.isHovered(width - 50, modY, width - 20, modY + 25, scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                            module.toggleExpanded();
                        } else {
                module.toggle();
                            switch (module.getName()) {
                                case "SmolPeople" -> {
                                    BaityConfig.smolpeopleMode = module.isEnabled();
                                    BaityConfig.saveConfig();
                                }
                                case "BlockAnimation" -> {
                                    BaityConfig.blockAnimationMode = module.isEnabled();
                                    BaityConfig.saveConfig();
                                }
                                case "PlayerESP" -> {
                                    BaityConfig.playerEspEnabled = module.isEnabled();
                                    BaityConfig.saveConfig();
                                }
                                case "PepCat" -> {
                                    BaityConfig.pepCatEnabled = module.isEnabled();
                                    BaityConfig.saveConfig();
                                }
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

                        if (button == 0 && RenderUtil.isHovered(containerX1 + 4, (int)subModY, containerX2 - 4, (int)(subModY + subOptionHeight), scaledMouseX, scaledMouseY) && valuetimer.delay(100)) {
                            if (value.getValue() instanceof Boolean) {
                                value.setValue(!((Boolean)value.getValue()));
                                if (value.getName().equals("crosshair")) {
                                    BaityConfig.crosshairMode = (Boolean)value.getValue();
                                    BaityConfig.saveConfig();
                                } else if (value.getName().equals("show distance")) {
                                    BaityConfig.playerEspShowDistance = (Boolean)value.getValue();
                                    BaityConfig.saveConfig();
                                } else if (value.getName().equals("show own nametag")) {
                                    BaityConfig.playerEspShowOwnNametag = (Boolean)value.getValue();
                                    BaityConfig.saveConfig();
                                }
                            }
                            valuetimer.reset();
                            return true;
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
            // 转换鼠标坐标到固定坐标系
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
            
            BaityConfig.guiKeyCode = keyCode;
            BaityConfig.saveConfig();
            updateKeyDisplay();
            isListeningForKey = false;
            
            // 重新注册按键绑定
            try {
                // 这里需要重新创建keybinding，但fabric的限制使得这比较复杂
                // 暂时只更新配置，重启后生效
            } catch (Exception e) {
                // 忽略错误
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isListeningForKey) {
                isListeningForKey = false;
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
        // 重新计算居中位置
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
