package com.shyeuar.baity.gui.hud;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.theme.Theme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
public class HudPositionEditor extends Screen {
    private static final int BORDER_WIDTH = 2;
    private static final int BORDER_COLOR_NORMAL = 0x40404080;
    private static final int BORDER_COLOR_HIGHLIGHT = 0xF0F0F080;
    private static final float BASE_GUI_SCALE = 3.0f;
    
    private final HudManager manager;
    private final Theme theme;
    private int grabbedX = 0;
    private int grabbedY = 0;
    private int clickedPos = -1;
    
    private float getGuiScale() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return 1.0f;
        int guiScaleOption = mc.options.guiScale().get();
        return (guiScaleOption <= 0) ? mc.getWindow().getGuiScale() : guiScaleOption;
    }
    
    private float getScaleRatio() {
        return BASE_GUI_SCALE / getGuiScale();
    }
    
    public HudPositionEditor() {
        super(Component.literal("HUD Editor"));
        this.manager = HudManager.getInstance();
        this.theme = new Theme();
        this.theme.setDark();
    }
    
    @Override
    protected void init() {
        super.init();
        manager.deselectAll();
        clickedPos = -1;
        for (HudElement element : manager.getElements()) {
            element.setClicked(false);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderMenuBackground(guiGraphics);

        for (HudElement element : manager.getElements()) {
            if (element.shouldRender()) {
                element.render(guiGraphics, partialTicks);
            }
        }
        
        int[] mousePos = HudScreenUtils.getMousePos();
        int guiMouseX = mousePos[0];
        int guiMouseY = mousePos[1];
        
        List<HudElement> elements = manager.getElements();
        HudElement hovered = null;
        boolean alreadyHadHover = false;
        
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            int elementWidth = element.getDummyWidth(false);
            int elementHeight = element.getDummyHeight(false);
            
            int pixelX = element.getAbsX(elementWidth);
            int pixelY = element.getAbsY(elementHeight);
            
            boolean isHovering = HudScreenUtils.isPointInRect(
                guiMouseX, guiMouseY,
                pixelX - BORDER_WIDTH, pixelY - BORDER_WIDTH,
                elementWidth + BORDER_WIDTH * 2, elementHeight + BORDER_WIDTH * 2
            ) && !alreadyHadHover;
            
            if (isHovering) {
                alreadyHadHover = true;
                hovered = element;
            }
            
            int borderColor = element.isSelected() ? BORDER_COLOR_HIGHLIGHT : 
                             (isHovering ? BORDER_COLOR_HIGHLIGHT : BORDER_COLOR_NORMAL);
            
            guiGraphics.fill(pixelX - BORDER_WIDTH, pixelY - BORDER_WIDTH, 
                           pixelX + elementWidth + BORDER_WIDTH, pixelY, borderColor);
            guiGraphics.fill(pixelX - BORDER_WIDTH, pixelY + elementHeight, 
                           pixelX + elementWidth + BORDER_WIDTH, pixelY + elementHeight + BORDER_WIDTH, borderColor);
            guiGraphics.fill(pixelX - BORDER_WIDTH, pixelY - BORDER_WIDTH, 
                           pixelX, pixelY + elementHeight + BORDER_WIDTH, borderColor);
            guiGraphics.fill(pixelX + elementWidth, pixelY - BORDER_WIDTH, 
                           pixelX + elementWidth + BORDER_WIDTH, pixelY + elementHeight + BORDER_WIDTH, borderColor);
            
            guiGraphics.fill(pixelX, pixelY, pixelX + elementWidth, pixelY + elementHeight, 0x40FFFFFF);
        }
        
        float scaleRatio = getScaleRatio();
        var matrices = guiGraphics.pose();
        matrices.pushMatrix();
        matrices.scale(scaleRatio, scaleRatio);
        
        HudElement displayElement = null;
        if (clickedPos != -1 && clickedPos < elements.size() && elements.get(clickedPos).isClicked()) {
            displayElement = elements.get(clickedPos);
        } else if (hovered != null) {
            displayElement = hovered;
        } else if (manager.getSelectedElement() != null) {
            displayElement = manager.getSelectedElement();
        }
        
        if (displayElement != null) {
            
            List<String> infoText = buildInfoText(displayElement);
            int textY = 10;
            for (String line : infoText) {
                guiGraphics.drawString(this.font, line, 10, textY, 0xFFFFFFFF, true);
                textY += 10;
            }
        } else {
            guiGraphics.drawString(this.font, "§cHUD Position Editor", 10, 10, 0xFFFFFFFF, true);
            guiGraphics.drawString(this.font, "§7Left-click and drag to move", 10, 20, 0xFFFFFFFF, true);
            guiGraphics.drawString(this.font, "§7Scroll wheel to resize", 10, 30, 0xFFFFFFFF, true);
            guiGraphics.drawString(this.font, "§7Right-click to reset position", 10, 40, 0xFFFFFFFF, true);
        }
        
        matrices.popMatrix();
    }
    
    private List<String> buildInfoText(HudElement element) {
        return List.of(
            "§cHUD Position Editor",
            "§b" + element.getDisplayName(),
            String.format("  §7x: §e%.2f§7, y: §e%.2f§7, scale: §e%.2f", 
                         element.getX(), element.getY(), element.getScale()),
            "",
            "§eRight-Click to reset to default position!",
            "§eUse Scroll-Wheel to resize!",
            "§eLeft-Click and drag to move!"
        );
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean isInsideWindow) {
        int[] mousePos = HudScreenUtils.getMousePos();
        int mouseX = mousePos[0];
        int mouseY = mousePos[1];
        int button = click.button();
        
        List<HudElement> elements = manager.getElements();
        boolean clickedOnElement = false;
        
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            boolean hovered = isElementHovered(element, mouseX, mouseY);
            if (!hovered) continue;
            
            clickedOnElement = true;
            
            if (button == 0) {
                if (!element.isClicked()) {
                    clickedPos = i;
                    element.setClicked(true);
                    int elementWidth = element.getDummyWidth(false);
                    int elementHeight = element.getDummyHeight(false);
                    int elementX = element.getAbsX(elementWidth);
                    int elementY = element.getAbsY(elementHeight);
                    grabbedX = mouseX - elementX;
                    grabbedY = mouseY - elementY;
                }
                manager.selectElement(element);
                return true;
            } else if (button == 1) {
                element.reset();
                manager.selectElement(element);
                ConfigManager.saveConfig();
                return true;
            }
            break;
        }
        
        if (!clickedOnElement) {
            if (button == 0) {
                manager.deselectAll();
            } else if (button == 1) {
                HudElement selected = manager.getSelectedElement();
                if (selected != null) {
                    selected.reset();
                    ConfigManager.saveConfig();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(click, isInsideWindow);
    }
    
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) {
        if (click.button() == 0) {
            for (HudElement element : manager.getElements()) {
                if (element.isClicked()) {
                    ConfigManager.saveConfig();
                }
                element.setClicked(false);
            }
        }
        return super.mouseReleased(click);
    }
    
    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double mouseX, double mouseY) {
        if (click.button() == 0) {
            int[] guiMousePos = HudScreenUtils.getMousePos();
            int guiMouseX = guiMousePos[0];
            int guiMouseY = guiMousePos[1];
            
            for (HudElement element : manager.getElements()) {
                if (!element.isClicked()) continue;
                
                int elementWidth = element.getDummyWidth(false);
                int elementHeight = element.getDummyHeight(false);
                int targetX = guiMouseX - grabbedX;
                int targetY = guiMouseY - grabbedY;
                
                int screenWidth = HudScreenUtils.getScaledWidth();
                int screenHeight = HudScreenUtils.getScaledHeight();
                
                double currentX = element.getX();
                double currentY = element.getY();
                boolean isRelativeX = currentX >= 0 && currentX <= 1.0;
                boolean isRelativeY = currentY >= 0 && currentY <= 1.0;
                
                if (isRelativeX) {
                    int centerX = targetX + elementWidth / 2;
                    double newX = centerX / (double)screenWidth;
                    if (newX < 0) newX = 0;
                    if (newX > 1.0) newX = 1.0;
                    element.setX(newX);
                } else {
                    if (targetX < 0) targetX = 0;
                    if (targetX > screenWidth - elementWidth) targetX = screenWidth - elementWidth;
                    element.setX(targetX);
                }
                
                if (isRelativeY) {
                    int centerY = targetY + elementHeight / 2;
                    double newY = centerY / (double)screenHeight;
                    if (newY < 0) newY = 0;
                    if (newY > 1.0) newY = 1.0;
                    element.setY(newY);
                } else {
                    if (targetY < 0) targetY = 0;
                    if (targetY > screenHeight - elementHeight) targetY = screenHeight - elementHeight;
                    element.setY(targetY);
                }
            }
            
            return true;
        }
        return super.mouseDragged(click, mouseX, mouseY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int[] guiMousePos = HudScreenUtils.getMousePos();
        int guiMouseX = guiMousePos[0];
        int guiMouseY = guiMousePos[1];
        
        HudElement element = null;
        for (HudElement e : manager.getElements()) {
            if (isElementHovered(e, guiMouseX, guiMouseY)) {
                element = e;
                break;
            }
        }
        
        if (element == null) {
            element = manager.getSelectedElement();
        }
        
        if (element != null) {
            float currentScale = element.getScale();
            float newScale = currentScale + (float)(verticalAmount * 0.1);
            newScale = Math.max(0.1f, Math.min(10.0f, newScale));
            element.setScale(newScale);
            
            if (!element.isSelected()) {
                manager.selectElement(element);
            }
            
            ConfigManager.saveConfig();
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    private boolean isElementHovered(HudElement element, int mouseX, int mouseY) {
        int elementWidth = element.getDummyWidth(false);
        int elementHeight = element.getDummyHeight(false);
        int pixelX = element.getAbsX(elementWidth);
        int pixelY = element.getAbsY(elementHeight);
        
        return HudScreenUtils.isPointInRect(
            mouseX, mouseY,
            pixelX - BORDER_WIDTH, pixelY - BORDER_WIDTH,
            elementWidth + BORDER_WIDTH * 2, elementHeight + BORDER_WIDTH * 2
        );
    }
    
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
        int keyCode = input.input();
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }
    
    @Override
    public void onClose() {
        clickedPos = -1;
        for (HudElement element : manager.getElements()) {
            element.setClicked(false);
        }
        manager.deselectAll();
        super.onClose();
    }
}
