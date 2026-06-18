package com.shyeuar.baity.features.numinputer;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.LinearTheme;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class NumInputer {
    private static final int KEY_SIZE = 22;
    private static final int KEY_GAP = 5;
    private static final int SECTION_GAP = 9;
    private static final int PANEL_PADDING = 8;
    private static final double ANCHOR_X_RATIO = 0.75;
    private static final double ANCHOR_Y_RATIO = 0.5;
    private static final String ENTER_LABEL = "\u23CE";

    private static final int PANEL_BG = LinearTheme.BG_SECONDARY.getRGB();
    private static final int KEY_BG = LinearTheme.BG_TERTIARY.getRGB();
    private static final int KEY_BORDER = LinearTheme.BORDER_PRIMARY.getRGB();
    private static final int KEY_TEXT = LinearTheme.TEXT_PRIMARY.getRGB();
    private static final int ACCENT_START = LinearTheme.ACCENT_PRIMARY.getRGB();
    private static final int ACCENT_END = LinearTheme.ACCENT_SECONDARY.getRGB();

    private static final NumInputer INSTANCE = new NumInputer();

    private Layout layout;
    private boolean leftMouseWasDown;

    private NumInputer() {
    }

    public static void onScreenInit(AbstractSignEditScreen screen) {
        INSTANCE.rebuildLayout(screen);
    }

    public static void onScreenClosed() {
        INSTANCE.layout = null;
        INSTANCE.leftMouseWasDown = false;
    }

    public static void render(AbstractSignEditScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        INSTANCE.renderInternal(screen, guiGraphics, mouseX, mouseY);
    }

    public static void tickMouse(AbstractSignEditScreen screen) {
        INSTANCE.tickMouseInternal(screen);
    }

    private void rebuildLayout(Screen screen) {
        if (!ConfigManager.numInputerEnabled) {
            layout = null;
            return;
        }

        int mainWidth = KEY_SIZE * 3 + KEY_GAP * 2;
        int contentWidth = Math.max(KEY_SIZE * 4 + KEY_GAP * 3, mainWidth + SECTION_GAP + KEY_SIZE);
        int panelWidth = PANEL_PADDING * 2 + contentWidth;
        int panelHeight = PANEL_PADDING * 2 + KEY_SIZE * 5 + KEY_GAP * 3 + SECTION_GAP;

        int panelX = (int) Math.round(screen.width * ANCHOR_X_RATIO - panelWidth * 0.5);
        int panelY = (int) Math.round(screen.height * ANCHOR_Y_RATIO - panelHeight * 0.5);

        int contentX = panelX + PANEL_PADDING;
        int contentY = panelY + PANEL_PADDING;

        List<KeyButton> keys = new ArrayList<>();

        int operatorY = contentY;
        addEqualSpacedRow(keys, contentX, operatorY, '+', '-', '*', '/');

        int numberY = operatorY + KEY_SIZE + SECTION_GAP;
        addEqualSpacedRow(keys, contentX, numberY, '7', '8', '9');
        addSideKey(keys, contentX, mainWidth, numberY, 'k');

        int row2Y = numberY + KEY_SIZE + KEY_GAP;
        addEqualSpacedRow(keys, contentX, row2Y, '4', '5', '6');
        addSideKey(keys, contentX, mainWidth, row2Y, 'm');

        int row3Y = row2Y + KEY_SIZE + KEY_GAP;
        addEqualSpacedRow(keys, contentX, row3Y, '1', '2', '3');
        addSideKey(keys, contentX, mainWidth, row3Y, 'b');

        int row4Y = row3Y + KEY_SIZE + KEY_GAP;
        keys.add(KeyButton.rect(contentX, row4Y, KEY_SIZE * 2 + KEY_GAP, KEY_SIZE, '0'));
        keys.add(KeyButton.rect(contentX + (KEY_SIZE + KEY_GAP) * 2, row4Y, KEY_SIZE, KEY_SIZE, '.'));
        keys.add(KeyButton.action(
            contentX + mainWidth + SECTION_GAP,
            row4Y,
            KEY_SIZE,
            KEY_SIZE,
            ENTER_LABEL,
            KeyAction.ENTER
        ));

        layout = new Layout(panelX, panelY, panelX + panelWidth, panelY + panelHeight, keys, screen.width, screen.height);
    }

    private static void addEqualSpacedRow(List<KeyButton> keys, int x, int y, char... labels) {
        for (int i = 0; i < labels.length; i++) {
            int keyX = x + i * (KEY_SIZE + KEY_GAP);
            keys.add(KeyButton.rect(keyX, y, KEY_SIZE, KEY_SIZE, labels[i]));
        }
    }

    private static void addSideKey(List<KeyButton> keys, int contentX, int mainWidth, int y, char label) {
        int x = contentX + mainWidth + SECTION_GAP;
        keys.add(KeyButton.rect(x, y, KEY_SIZE, KEY_SIZE, label));
    }

    private void renderInternal(AbstractSignEditScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!ConfigManager.numInputerEnabled) {
            return;
        }
        if (layout == null || layout.screenWidth != screen.width || layout.screenHeight != screen.height) {
            rebuildLayout(screen);
        }
        if (layout == null) {
            return;
        }

        GuiRenderUtil.draw3DRect(guiGraphics, layout.panelX1, layout.panelY1, layout.panelX2, layout.panelY2, PANEL_BG, 6f);
        GuiRenderUtil.stroke1px(guiGraphics, layout.panelX1, layout.panelY1, layout.panelX2, layout.panelY2, KEY_BORDER);

        Font font = Minecraft.getInstance().font;
        for (KeyButton key : layout.keys) {
            boolean hovered = GuiRenderUtil.isHovered(key.x1, key.y1, key.x2, key.y2, mouseX, mouseY);
            if (hovered) {
                GuiRenderUtil.draw3DGradientRect(guiGraphics, key.x1, key.y1, key.x2, key.y2, ACCENT_START, ACCENT_END, 4f);
            } else {
                GuiRenderUtil.draw3DRect(guiGraphics, key.x1, key.y1, key.x2, key.y2, KEY_BG, 4f);
            }
            GuiRenderUtil.stroke1px(guiGraphics, key.x1, key.y1, key.x2, key.y2, KEY_BORDER);

            int textWidth = font.width(key.label);
            int textX = (int) (key.x1 + (key.x2 - key.x1 - textWidth) * 0.5f);
            int textY = (int) (key.y1 + (key.y2 - key.y1 - font.lineHeight) * 0.5f + 1);
            guiGraphics.drawString(font, key.label, textX, textY, KEY_TEXT, false);
        }
    }

    private void tickMouseInternal(AbstractSignEditScreen screen) {
        if (!ConfigManager.numInputerEnabled || layout == null) {
            leftMouseWasDown = false;
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        long window = client.getWindow().handle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !leftMouseWasDown) {
            var mouseHandler = client.mouseHandler;
            var mcWindow = client.getWindow();
            handleClick(screen, mouseHandler.getScaledXPos(mcWindow), mouseHandler.getScaledYPos(mcWindow));
        }
        leftMouseWasDown = down;
    }

    private void handleClick(AbstractSignEditScreen screen, double mouseX, double mouseY) {
        NumInputerSignScreenAccess access = (NumInputerSignScreenAccess) screen;

        for (KeyButton key : layout.keys) {
            if (!GuiRenderUtil.isHovered(key.x1, key.y1, key.x2, key.y2, (float) mouseX, (float) mouseY)) {
                continue;
            }
            if (key.action == KeyAction.ENTER) {
                SoundUtils.playBubble();
                access.baity$finishSignEditing();
            } else if (key.insertChar != 0) {
                SoundUtils.playWoodenButton();
                access.baity$insertIntoSign(key.insertChar);
            }
            return;
        }
    }

    private record Layout(int panelX1, int panelY1, int panelX2, int panelY2, List<KeyButton> keys, int screenWidth,
                          int screenHeight) {
    }

    private enum KeyAction {
        CHAR,
        ENTER
    }

    private record KeyButton(float x1, float y1, float x2, float y2, String label, char insertChar, KeyAction action) {
        private static KeyButton rect(float x, float y, float width, float height, char insertChar) {
            return new KeyButton(x, y, x + width, y + height, String.valueOf(insertChar), insertChar, KeyAction.CHAR);
        }

        private static KeyButton action(float x, float y, float width, float height, String label, KeyAction action) {
            return new KeyButton(x, y, x + width, y + height, label, (char) 0, action);
        }
    }

    public interface NumInputerSignScreenAccess {
        void baity$insertIntoSign(char character);

        void baity$finishSignEditing();
    }
}
