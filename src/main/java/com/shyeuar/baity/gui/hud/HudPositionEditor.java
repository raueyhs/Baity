package com.shyeuar.baity.gui.hud;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.theme.Theme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private double grabbedX = 0;
    private double grabbedY = 0;
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.extractMenuBackground(guiGraphics);

        updateDraggedElements();
        
        double[] mousePos = HudScreenUtils.getMousePosPrecise();
        int guiMouseX = (int) mousePos[0];
        int guiMouseY = (int) mousePos[1];
        
        List<HudElement> elements = manager.getElements();
        HudElement hovered = null;
        boolean alreadyHadHover = false;

        for (HudElement element : elements) {
            if (element.shouldRender()) {
                element.render(guiGraphics, partialTicks);
            }
        }
        
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
                guiGraphics.text(this.font, line, 10, textY, 0xFFFFFFFF, true);
                textY += 10;
            }
        } else {
            guiGraphics.text(this.font, "§cHUD Position Editor", 10, 10, 0xFFFFFFFF, true);
            guiGraphics.text(this.font, "§7Left-click and drag to move", 10, 20, 0xFFFFFFFF, true);
            guiGraphics.text(this.font, "§7Scroll wheel to resize", 10, 30, 0xFFFFFFFF, true);
            guiGraphics.text(this.font, "§7Right-click to reset position", 10, 40, 0xFFFFFFFF, true);
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
        double[] mousePos = HudScreenUtils.getMousePosPrecise();
        double mouseX = mousePos[0];
        double mouseY = mousePos[1];
        int button = com.shyeuar.baity.utils.MouseButtonUtils.fromSdl(click.button());
        
        List<HudElement> elements = manager.getElements();
        boolean clickedOnElement = false;
        
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            boolean hovered = isElementHovered(element, (int) mouseX, (int) mouseY);
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
        if (com.shyeuar.baity.utils.MouseButtonUtils.fromSdl(click.button()) == 0) {
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
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dx, double dy) {
        if (com.shyeuar.baity.utils.MouseButtonUtils.fromSdl(click.button()) == 0 && updateDraggedElements()) {
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    private boolean updateDraggedElements() {
        boolean any = false;
        double[] guiMousePos = HudScreenUtils.getMousePosPrecise();
        double guiMouseX = guiMousePos[0];
        double guiMouseY = guiMousePos[1];
        int screenWidth = HudScreenUtils.getScaledWidth();
        int screenHeight = HudScreenUtils.getScaledHeight();

        for (HudElement element : manager.getElements()) {
            if (!element.isClicked()) continue;
            any = true;

            int elementWidth = element.getDummyWidth(false);
            int elementHeight = element.getDummyHeight(false);
            double targetX = guiMouseX - grabbedX;
            double targetY = guiMouseY - grabbedY;

            double currentX = element.getX();
            double currentY = element.getY();
            boolean isRelativeX = currentX >= 0 && currentX <= 1.0;
            boolean isRelativeY = currentY >= 0 && currentY <= 1.0;

            if (isRelativeX) {
                double centerX = targetX + elementWidth / 2.0;
                double newX = centerX / (double) screenWidth;
                if (newX < 0) newX = 0;
                if (newX > 1.0) newX = 1.0;
                element.setX(newX);
            } else {
                if (targetX < 0) targetX = 0;
                if (targetX > screenWidth - elementWidth) targetX = screenWidth - elementWidth;
                element.setX(targetX);
            }

            if (isRelativeY) {
                double centerY = targetY + elementHeight / 2.0;
                double newY = centerY / (double) screenHeight;
                if (newY < 0) newY = 0;
                if (newY > 1.0) newY = 1.0;
                element.setY(newY);
            } else {
                if (targetY < 0) targetY = 0;
                if (targetY > screenHeight - elementHeight) targetY = screenHeight - elementHeight;
                element.setY(targetY);
            }
        }
        return any;
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
