package com.shyeuar.baity.features;

import com.mojang.blaze3d.platform.InputConstants;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.LinearTheme;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.sdl.SDLKeyboard;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class NumInputer {
    private static final int KEY_SIZE = 22;
    private static final int KEY_GAP = 5;
    private static final int SECTION_GAP = 9;
    private static final int SIDE_COL_GAP = 4;
    private static final int PANEL_PADDING = 8;
    private static final double ANCHOR_X_RATIO = 0.75;
    private static final double ANCHOR_Y_RATIO = 0.5;
    private static final String BACKSPACE_LABEL = "\u232B";

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

    public static boolean handleAltMoveKeyPress(NumInputerSignScreenAccess access, KeyEvent event) {
        if (!ConfigManager.numInputerEnabled) {
            return false;
        }
        if (!event.hasAltDown()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return false;
        }

        Options options = client.options;

        if (options.keyUp.matches(event)) {
            access.baity$moveSignLine(-1);
            return true;
        }
        if (options.keyDown.matches(event)) {
            access.baity$moveSignLine(1);
            return true;
        }
        if (options.keyLeft.matches(event)) {
            access.baity$moveSignCursorHorizontal(-1);
            return true;
        }
        if (options.keyRight.matches(event)) {
            access.baity$moveSignCursorHorizontal(1);
            return true;
        }
        return false;
    }

    public static boolean shouldSuppressAltCharTyped() {
        if (!ConfigManager.numInputerEnabled) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        return isAltDown();
    }

    public static void render(AbstractSignEditScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        INSTANCE.renderInternal(screen, graphics, mouseX, mouseY);
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
        int contentWidth = mainWidth + SIDE_COL_GAP + KEY_SIZE;
        int panelWidth = PANEL_PADDING * 2 + contentWidth;
        int panelHeight = PANEL_PADDING * 2 + KEY_SIZE * 5 + KEY_GAP * 3 + SECTION_GAP;

        int panelX = (int) Math.round(screen.width * ANCHOR_X_RATIO - panelWidth * 0.5);
        int panelY = (int) Math.round(screen.height * ANCHOR_Y_RATIO - panelHeight * 0.5);

        int contentX = panelX + PANEL_PADDING;
        int contentY = panelY + PANEL_PADDING;
        int sideColX = contentX + mainWidth + SIDE_COL_GAP;

        List<KeyButton> keys = new ArrayList<>();

        int operatorY = contentY;
        addEqualSpacedRow(keys, contentX, operatorY, '+', '-', '*');
        addSideKey(keys, sideColX, operatorY, '/');

        int numberY = operatorY + KEY_SIZE + SECTION_GAP;
        addEqualSpacedRow(keys, contentX, numberY, '7', '8', '9');
        addSideKey(keys, sideColX, numberY, 'k');

        int row2Y = numberY + KEY_SIZE + KEY_GAP;
        addEqualSpacedRow(keys, contentX, row2Y, '4', '5', '6');
        addSideKey(keys, sideColX, row2Y, 'm');

        int row3Y = row2Y + KEY_SIZE + KEY_GAP;
        addEqualSpacedRow(keys, contentX, row3Y, '1', '2', '3');
        addSideKey(keys, sideColX, row3Y, 'b');

        int row4Y = row3Y + KEY_SIZE + KEY_GAP;
        keys.add(KeyButton.rect(contentX, row4Y, KEY_SIZE * 2 + KEY_GAP, KEY_SIZE, '0'));
        keys.add(KeyButton.rect(contentX + (KEY_SIZE + KEY_GAP) * 2, row4Y, KEY_SIZE, KEY_SIZE, '.'));
        keys.add(KeyButton.action(
            sideColX,
            row4Y,
            KEY_SIZE,
            KEY_SIZE,
            BACKSPACE_LABEL,
            KeyAction.BACKSPACE
        ));

        layout = new Layout(panelX, panelY, panelX + panelWidth, panelY + panelHeight, keys, screen.width, screen.height);
    }

    private static void addEqualSpacedRow(List<KeyButton> keys, int x, int y, char... labels) {
        for (int i = 0; i < labels.length; i++) {
            int keyX = x + i * (KEY_SIZE + KEY_GAP);
            keys.add(KeyButton.rect(keyX, y, KEY_SIZE, KEY_SIZE, labels[i]));
        }
    }

    private static void addSideKey(List<KeyButton> keys, int sideColX, int y, char label) {
        keys.add(KeyButton.rect(sideColX, y, KEY_SIZE, KEY_SIZE, label));
    }

    private void renderInternal(AbstractSignEditScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!ConfigManager.numInputerEnabled) {
            return;
        }
        if (layout == null || layout.screenWidth != screen.width || layout.screenHeight != screen.height) {
            rebuildLayout(screen);
        }
        if (layout == null) {
            return;
        }

        GuiRenderUtil.draw3DRect(graphics, layout.panelX1, layout.panelY1, layout.panelX2, layout.panelY2, PANEL_BG, 6f);
        GuiRenderUtil.stroke1px(graphics, layout.panelX1, layout.panelY1, layout.panelX2, layout.panelY2, KEY_BORDER);

        Font font = Minecraft.getInstance().font;
        for (KeyButton key : layout.keys) {
            boolean hovered = GuiRenderUtil.isHovered(key.x1, key.y1, key.x2, key.y2, mouseX, mouseY);
            if (hovered) {
                GuiRenderUtil.draw3DGradientRect(graphics, key.x1, key.y1, key.x2, key.y2, ACCENT_START, ACCENT_END, 4f);
            } else {
                GuiRenderUtil.draw3DRect(graphics, key.x1, key.y1, key.x2, key.y2, KEY_BG, 4f);
            }
            GuiRenderUtil.stroke1px(graphics, key.x1, key.y1, key.x2, key.y2, KEY_BORDER);
            drawKeyLabel(graphics, font, key);
        }
    }

    private static void drawKeyLabel(GuiGraphicsExtractor graphics, Font font, KeyButton key) {
        float boxW = key.x2 - key.x1;
        float boxH = key.y2 - key.y1;
        if (key.action == KeyAction.BACKSPACE) {
            float scale = Math.min(boxW / Math.max(1, font.width(key.label)), boxH / Math.max(1, font.lineHeight)) * 0.82f;
            scale = Math.max(scale, 1.0f);
            float scaledW = font.width(key.label) * scale;
            float scaledH = font.lineHeight * scale;
            float textX = key.x1 + (boxW - scaledW) * 0.5f;
            float textY = key.y1 + (boxH - scaledH) * 0.5f + 1;
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(textX, textY);
            pose.scale(scale, scale);
            graphics.text(font, key.label, 0, 0, KEY_TEXT, false);
            pose.popMatrix();
            return;
        }

        int textWidth = font.width(key.label);
        int textX = (int) (key.x1 + (boxW - textWidth) * 0.5f);
        int textY = (int) (key.y1 + (boxH - font.lineHeight) * 0.5f + 1);
        graphics.text(font, key.label, textX, textY, KEY_TEXT, false);
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

        boolean down = com.shyeuar.baity.utils.KeyMappingUtils.isKeyPressed(client.getWindow().handle(),
                com.shyeuar.baity.features.Keybinds.mouseKey(0));
        if (down && !leftMouseWasDown) {
            var mouseHandler = client.mouseHandler;
            var mcWindow = client.getWindow();
            handleClick(screen, mouseHandler.getScaledXPos(mcWindow), mouseHandler.getScaledYPos(mcWindow));
        }
        leftMouseWasDown = down;
    }

    private static boolean isAltDown() {
        return SDLKeyboard.SDL_GetKeyboardState().get(InputConstants.KEY_LALT) != 0
            || SDLKeyboard.SDL_GetKeyboardState().get(InputConstants.KEY_RALT) != 0;
    }

    private void handleClick(AbstractSignEditScreen screen, double mouseX, double mouseY) {
        NumInputerSignScreenAccess access = (NumInputerSignScreenAccess) screen;

        for (KeyButton key : layout.keys) {
            if (!GuiRenderUtil.isHovered(key.x1, key.y1, key.x2, key.y2, (float) mouseX, (float) mouseY)) {
                continue;
            }
            if (key.action == KeyAction.BACKSPACE) {
                SoundUtils.playWoodenButton();
                access.baity$backspaceFromSign();
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
        BACKSPACE
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

        void baity$backspaceFromSign();

        void baity$moveSignLine(int delta);

        void baity$moveSignCursorHorizontal(int delta);
    }
}
