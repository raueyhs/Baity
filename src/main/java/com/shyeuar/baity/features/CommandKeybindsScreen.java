package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.input.LineTextInput;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.LinearTheme;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CommandKeybindsScreen extends Screen {

    public static final int PANEL_WIDTH = (int) ClickGuiState.WIDTH;
    public static final int PANEL_HEIGHT = (int) ClickGuiState.HEIGHT;

    private static final int TITLE_BAR_HEIGHT = 22;
    private static final int HEADER_TEXT_Y = TITLE_BAR_HEIGHT + 10;
    private static final int ROW_TOP = 56;
    private static final int ROW_HEIGHT = 34;
    private static final int ROW_PADDING = 12;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int KEY_BUTTON_WIDTH = 132;
    private static final int ICON_SIZE = 12;
    private static final int BUTTON_HEIGHT = 18;
    private static final int TOGGLE_SIZE = 18;
    private static final int FOOTER_BUTTON_WIDTH = 58;
    private static final int FOOTER_BUTTON_GAP = 6;
    private static final int FOOTER_TOP = PANEL_HEIGHT - 30;
    private static final int LIST_BOTTOM = FOOTER_TOP - 6;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int LINE_GRAY = 0xFF787878;

    private final Screen parentScreen;
    private final List<Keybinds.CommandBinding> drafts = new ArrayList<>();
    private final List<LineTextInput> textInputs = new ArrayList<>();
    private int focusedIndex = -1;
    private int bindingIndex = -1;
    private int scrollRow;

    public CommandKeybindsScreen(Screen parentScreen) {
        super(Component.literal("Commands Keybind"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        drafts.clear();
        textInputs.clear();
        for (Keybinds.CommandBinding binding : Keybinds.commandBindings()) {
            drafts.add(binding);
            LineTextInput input = new LineTextInput(LineTextInput.Policy.freeText(-1));
            input.setText(binding.text());
            textInputs.add(input);
        }
        focusedIndex = -1;
        bindingIndex = -1;
        scrollRow = 0;
    }

    private int visibleRows() {
        return Math.max(1, (LIST_BOTTOM - ROW_TOP) / ROW_HEIGHT);
    }

    private int maxScrollRow() {
        return Math.max(0, drafts.size() - visibleRows());
    }

    private LineTextInput inputAt(int index) {
        while (textInputs.size() <= index) {
            textInputs.add(new LineTextInput(LineTextInput.Policy.freeText(-1)));
        }
        return textInputs.get(index);
    }

    private int rowTop(int index) {
        return ROW_TOP + (index - scrollRow) * ROW_HEIGHT;
    }

    private int rowBottom(int index) {
        return rowTop(index) + ROW_HEIGHT - 6;
    }

    private int toggleX1(int index) {
        return ROW_PADDING + 28;
    }

    private int keyX1(int index) {
        return toggleX1(index) + TOGGLE_SIZE + 10;
    }

    private boolean insideKeyBox(int index, int mouseX, int mouseY) {
        int centerY = (rowTop(index) + rowBottom(index)) / 2;
        return GuiRenderUtil.isHovered(keyX1(index), centerY - BUTTON_HEIGHT / 2,
                keyX1(index) + KEY_BUTTON_WIDTH, centerY + BUTTON_HEIGHT / 2, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow()));

        Minecraft mc = Minecraft.getInstance();
        float sr = ClickGuiState.fixedScaleRatio(mc);
        float dispW = PANEL_WIDTH * sr;
        float dispH = PANEL_HEIGHT * sr;
        float originX = (this.width - dispW) / 2f;
        float originY = (this.height - dispH) / 2f;
        float localMx = (mouseX - originX) / sr;
        float localMy = (mouseY - originY) / sr;

        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(originX, originY);
        pose.scale(sr, sr);
        renderPanel(guiGraphics, localMx, localMy);
        pose.popMatrix();
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }

    private void renderPanel(GuiGraphicsExtractor g, float mouseX, float mouseY) {
        GuiRenderUtil.drawFrostedGlass(g, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, LinearTheme.BG_SECONDARY.getRGB(), 8f);
        GuiRenderUtil.draw3DRect(g, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, LinearTheme.BG_SECONDARY.getRGB(), 8f);
        GuiRenderUtil.stroke1px(g, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, LinearTheme.BORDER_PRIMARY.getRGB());
        GuiRenderUtil.draw3DGradientRect(g, 0, 0, PANEL_WIDTH, TITLE_BAR_HEIGHT,
                LinearTheme.ACCENT_PRIMARY.getRGB(), LinearTheme.ACCENT_SECONDARY.getRGB(), 8f);
        g.text(this.font, "Commands Keybind", 10, 7, 0xFFFFFFFF, false);
        g.text(this.font, "Type /<command> to run it as a command, otherwise it is sent as chat.",
                ROW_PADDING, HEADER_TEXT_Y, LinearTheme.TEXT_SECONDARY.getRGB(), false);
        g.text(this.font, "Don't forget to click Save!", ROW_PADDING, HEADER_TEXT_Y + 11, YELLOW, false);

        int last = Math.min(drafts.size(), scrollRow + visibleRows());
        for (int index = scrollRow; index < last; index++) {
            drawBindingRow(g, index, mouseX, mouseY);
        }
        if (drafts.isEmpty()) {
            g.text(this.font, "No bindings yet.", ROW_PADDING + 4, ROW_TOP + 8,
                    LinearTheme.TEXT_SECONDARY.getRGB(), false);
        }
        drawScrollbar(g);

        int addX1 = ROW_PADDING;
        int addX2 = addX1 + 54;
        int saveX2 = PANEL_WIDTH - ROW_PADDING;
        int saveX1 = saveX2 - FOOTER_BUTTON_WIDTH;
        int cancelX2 = saveX1 - FOOTER_BUTTON_GAP;
        int cancelX1 = cancelX2 - FOOTER_BUTTON_WIDTH;
        drawButton(g, addX1, FOOTER_TOP, addX2, FOOTER_TOP + BUTTON_HEIGHT, "+ Add",
                GuiRenderUtil.isHovered(addX1, FOOTER_TOP, addX2, FOOTER_TOP + BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(g, cancelX1, FOOTER_TOP, cancelX2, FOOTER_TOP + BUTTON_HEIGHT, "Cancel",
                GuiRenderUtil.isHovered(cancelX1, FOOTER_TOP, cancelX2, FOOTER_TOP + BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(g, saveX1, FOOTER_TOP, saveX2, FOOTER_TOP + BUTTON_HEIGHT, "Save",
                GuiRenderUtil.isHovered(saveX1, FOOTER_TOP, saveX2, FOOTER_TOP + BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void drawBindingRow(GuiGraphicsExtractor g, int index, float mouseX, float mouseY) {
        Keybinds.CommandBinding draft = drafts.get(index);
        int rowY = rowTop(index);
        int rowY2 = rowBottom(index);
        if (rowY2 > LIST_BOTTOM || rowY < ROW_TOP) {
            return;
        }
        int rowX1 = ROW_PADDING;
        int rowX2 = PANEL_WIDTH - ROW_PADDING - SCROLLBAR_WIDTH - 4;
        GuiRenderUtil.draw3DRect(g, rowX1, rowY, rowX2, rowY2, LinearTheme.BG_TERTIARY.getRGB(), 6f);
        GuiRenderUtil.stroke1px(g, rowX1, rowY, rowX2, rowY2, LinearTheme.BORDER_PRIMARY.getRGB());

        int centerY = (rowY + rowY2) / 2;
        g.text(this.font, String.valueOf(index + 1), rowX1 + 8, centerY - 4,
                LinearTheme.TEXT_SECONDARY.getRGB(), false);

        int toggleX1 = toggleX1(index);
        int toggleY1 = centerY - TOGGLE_SIZE / 2;
        drawToggle(g, toggleX1, toggleY1, draft.enabled(), mouseX, mouseY);

        int keyX1 = keyX1(index);
        int keyX2 = keyX1 + KEY_BUTTON_WIDTH;
        int keyY1 = centerY - BUTTON_HEIGHT / 2;
        boolean keyHovered = GuiRenderUtil.isHovered(keyX1, keyY1, keyX2, keyY1 + BUTTON_HEIGHT, mouseX, mouseY);
        GuiRenderUtil.draw3DRect(g, keyX1, keyY1, keyX2, keyY1 + BUTTON_HEIGHT,
                LinearTheme.BG_SECONDARY.getRGB(), 4f);
        GuiRenderUtil.stroke1px(g, keyX1, keyY1, keyX2, keyY1 + BUTTON_HEIGHT,
                (keyHovered || bindingIndex == index) ? YELLOW : LinearTheme.BORDER_PRIMARY.getRGB());
        drawKeyIcon(g, keyX1 + 5, keyY1 + 3);
        int textX = keyX1 + 6 + ICON_SIZE + 5;
        int keyTextWidth = Math.max(0, keyX2 - textX - 5);
        String keyText;
        if (bindingIndex == index) {
            String recording = draft.keys().isEmpty() ? "press keys..." : joinKeys(draft.keys());
            keyText = ellipsizeTail("< " + recording + " >", keyTextWidth);
        } else {
            keyText = ellipsize(draft.keys().isEmpty() ? "NOTSET" : joinKeys(draft.keys()), keyTextWidth);
        }
        g.text(this.font, keyText, textX, keyY1 + 5, 0xFFFFFFFF, false);

        int deleteX2 = rowX2 - 8;
        int deleteX1 = deleteX2 - 16;
        g.text(this.font, "x", deleteX1 + 5, centerY - 4,
                GuiRenderUtil.isHovered(deleteX1, centerY - 8, deleteX2, centerY + 8, mouseX, mouseY)
                        ? 0xFFFF5555 : LinearTheme.TEXT_SECONDARY.getRGB(), false);

        int lineX1 = keyX2 + 12;
        int lineX2 = deleteX1 - 10;
        boolean focused = focusedIndex == index;
        boolean lineHovered = GuiRenderUtil.isHovered(lineX1, rowY + 6, lineX2, rowY2 - 6, mouseX, mouseY);
        int lineY = rowY2 - 8;
        GuiRenderUtil.drawRoundedRect(g, lineX1, lineY, lineX2, lineY + 1, 0,
                (lineHovered || focused) ? YELLOW : LINE_GRAY);
        LineTextInput input = inputAt(index);
        String text = input.getText();
        int maxTextWidth = Math.max(0, lineX2 - lineX1);
        if (focused) {
            LineTextInput.drawTextWithBlinkCursor(g, this.font, text, input.getCaretCp(),
                    lineX1, lineY - 9, YELLOW, true, LineTextInput.shouldBlinkCursor(), maxTextWidth);
        } else if (!text.isEmpty()) {
            LineTextInput.drawTextWithBlinkCursor(g, this.font, text, 0,
                    lineX1, lineY - 9, LinearTheme.TEXT_PRIMARY.getRGB(), false, false, maxTextWidth);
        }
    }

    private void drawToggle(GuiGraphicsExtractor g, int x1, int y1, boolean checked, float mouseX, float mouseY) {
        int x2 = x1 + TOGGLE_SIZE;
        int y2 = y1 + TOGGLE_SIZE;
        int purple = KeyMappingUtils.getModuleEnabledPurpleRGB();
        boolean hovered = GuiRenderUtil.isHovered(x1, y1, x2, y2, mouseX, mouseY);
        int border = (hovered || checked) ? purple
                : ((new java.awt.Color(220, 220, 220, 255).getRGB() & 0x00FFFFFF) | (255 << 24));
        int innerBg = ((new java.awt.Color(30, 30, 30, 80).getRGB() & 0x00FFFFFF) | (255 << 24));
        if (checked) {
            GuiRenderUtil.drawRoundedRect(g, x1, y1, x2, y2, 2, purple);
        } else {
            GuiRenderUtil.drawRoundedRect(g, x1, y1, x2, y2, 2, innerBg);
        }
        GuiRenderUtil.drawRoundedRectOutline(g, x1, y1, x2, y2, 2, border);
        if (checked) {
            drawToggleCheckmark(g, x1, y1, x2, y2, 0xFFFFFFFF);
        }
    }

    private void drawToggleCheckmark(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int color) {
        float xL = x1 + 4f;
        float yL = y1 + (y2 - y1) * 0.58f;
        float xM = x1 + (x2 - x1) * 0.36f;
        float yM = y2 - 4f;
        float xR = x2 - 4f;
        float yR = y1 + 4f;
        drawThickLine(g, xL, yL, xM, yM, 2, color);
        drawThickLine(g, xM, yM, xR, yR, 2, color);
    }

    private void drawThickLine(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int thickness, int color) {
        int steps = Math.max(1, (int) (Math.hypot(x1 - x0, y1 - y0) * 2f));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int half = thickness / 2;
            int px = (int) (x0 + (x1 - x0) * t);
            int py = (int) (y0 + (y1 - y0) * t);
            g.fill(px - half, py - half, px + half + 1, py + half + 1, color);
        }
    }

    private void drawKeyIcon(GuiGraphicsExtractor g, int x, int y) {
        int iconColor = LinearTheme.TEXT_SECONDARY.getRGB();
        GuiRenderUtil.drawRoundedRect(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 2, 0x40FFFFFF);
        GuiRenderUtil.drawRoundedRectOutline(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 2, iconColor);
        GuiRenderUtil.drawRoundedRect(g, x + 2, y + 3, x + 4, y + 5, 1, iconColor);
        GuiRenderUtil.drawRoundedRect(g, x + 5, y + 3, x + 7, y + 5, 1, iconColor);
        GuiRenderUtil.drawRoundedRect(g, x + 8, y + 3, x + 10, y + 5, 1, iconColor);
        GuiRenderUtil.drawRoundedRect(g, x + 3, y + 7, x + 9, y + 9, 1, iconColor);
    }

    private void drawScrollbar(GuiGraphicsExtractor g) {
        int max = maxScrollRow();
        if (max <= 0) {
            return;
        }
        float trackHeight = (float) (LIST_BOTTOM - ROW_TOP);
        float barHeight = Math.max(12f, trackHeight * (visibleRows() / (float) drafts.size()));
        float barY = ROW_TOP + (trackHeight - barHeight) * (scrollRow / (float) max);
        float barX = PANEL_WIDTH - ROW_PADDING + 2;
        GuiRenderUtil.drawRoundedRect(g, barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 2,
                LinearTheme.ACCENT_PRIMARY.getRGB());
    }

    private String joinKeys(List<Integer> keys) {
        StringBuilder builder = new StringBuilder();
        for (int keyCode : keys) {
            if (builder.length() > 0) {
                builder.append(" + ");
            }
            builder.append(Keybinds.keyDisplay(keyCode));
        }
        return builder.toString();
    }

    private String ellipsize(String text, int maxWidth) {
        if (maxWidth <= 0 || text.isEmpty() || this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int allowed = maxWidth - this.font.width(ellipsis);
        if (allowed <= 0) {
            return ellipsis;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (this.font.width(builder.toString() + text.charAt(i)) > allowed) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + ellipsis;
    }

    private String ellipsizeTail(String text, int maxWidth) {
        if (maxWidth <= 0 || text.isEmpty() || this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int allowed = maxWidth - this.font.width(ellipsis);
        if (allowed <= 0) {
            return ellipsis;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = text.length() - 1; i >= 0; i--) {
            builder.insert(0, text.charAt(i));
            if (this.font.width(builder.toString()) > allowed) {
                builder.deleteCharAt(0);
                break;
            }
        }
        return ellipsis + builder;
    }

    private void drawButton(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, String label, boolean hovered) {
        GuiRenderUtil.draw3DRect(g, x1, y1, x2, y2, LinearTheme.BG_TERTIARY.getRGB(), 5f);
        GuiRenderUtil.stroke1px(g, x1, y1, x2, y2, hovered ? YELLOW : LinearTheme.BORDER_PRIMARY.getRGB());
        int textWidth = this.font.width(label);
        g.text(this.font, label, x1 + (x2 - x1 - textWidth) / 2, y1 + 5, 0xFFFFFFFF, false);
    }

    private void recordKey(int keyCode) {
        if (bindingIndex < 0) {
            return;
        }
        Keybinds.CommandBinding binding = drafts.get(bindingIndex);
        if (binding.orderSensitive() || !binding.keys().contains(keyCode)) {
            List<Integer> keys = new ArrayList<>(binding.keys());
            keys.add(keyCode);
            drafts.set(bindingIndex, binding.withKeys(keys));
            SoundUtils.playWoodenButton();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        float[] local = toLocal((float) click.x(), (float) click.y());
        int mouseX = (int) local[0];
        int mouseY = (int) local[1];

        if (bindingIndex >= 0) {
            if (insideKeyBox(bindingIndex, mouseX, mouseY)) {
                recordKey(Keybinds.mouseKey(click.button()));
            } else {
                bindingIndex = -1;
            }
            return true;
        }

        int addX1 = ROW_PADDING;
        int addX2 = addX1 + 54;
        int saveX2 = PANEL_WIDTH - ROW_PADDING;
        int saveX1 = saveX2 - FOOTER_BUTTON_WIDTH;
        int cancelX2 = saveX1 - FOOTER_BUTTON_GAP;
        int cancelX1 = cancelX2 - FOOTER_BUTTON_WIDTH;

        if (click.button() == 0 && GuiRenderUtil.isHovered(addX1, FOOTER_TOP, addX2,
                FOOTER_TOP + BUTTON_HEIGHT, mouseX, mouseY)) {
            commitFocusedInput();
            drafts.add(new Keybinds.CommandBinding(List.of(), true, "", false, true, 0, Keybinds.CONTEXT_GLOBAL));
            inputAt(drafts.size() - 1);
            SoundUtils.playWoodenButton();
            scrollRow = maxScrollRow();
            return true;
        }
        if (click.button() == 0 && GuiRenderUtil.isHovered(saveX1, FOOTER_TOP, saveX2,
                FOOTER_TOP + BUTTON_HEIGHT, mouseX, mouseY)) {
            SoundUtils.playWoodenButton();
            saveAndClose();
            return true;
        }
        if (click.button() == 0 && GuiRenderUtil.isHovered(cancelX1, FOOTER_TOP, cancelX2,
                FOOTER_TOP + BUTTON_HEIGHT, mouseX, mouseY)) {
            cancelAndClose();
            return true;
        }

        int last = Math.min(drafts.size(), scrollRow + visibleRows());
        for (int index = scrollRow; index < last; index++) {
            Keybinds.CommandBinding draft = drafts.get(index);
            int rowY = rowTop(index);
            int rowY2 = rowBottom(index);
            if (rowY2 > LIST_BOTTOM || rowY < ROW_TOP) {
                break;
            }
            int rowX1 = ROW_PADDING;
            int rowX2 = PANEL_WIDTH - ROW_PADDING - SCROLLBAR_WIDTH - 4;
            int centerY = (rowY + rowY2) / 2;

            int toggleX1 = toggleX1(index);
            int toggleY1 = centerY - TOGGLE_SIZE / 2;
            if (GuiRenderUtil.isHovered(toggleX1, toggleY1, toggleX1 + TOGGLE_SIZE, toggleY1 + TOGGLE_SIZE,
                    mouseX, mouseY)) {
                commitFocusedInput();
                drafts.set(index, draft.withEnabled(!draft.enabled()));
                SoundUtils.playBubble();
                return true;
            }

            int keyX1 = keyX1(index);
            int keyX2 = keyX1 + KEY_BUTTON_WIDTH;
            int keyY1 = centerY - BUTTON_HEIGHT / 2;
            if (GuiRenderUtil.isHovered(keyX1, keyY1, keyX2, keyY1 + BUTTON_HEIGHT, mouseX, mouseY)) {
                commitFocusedInput();
                bindingIndex = index;
                drafts.set(index, draft.withKeys(List.of()));
                SoundUtils.playWoodenButton();
                return true;
            }

            int deleteX2 = rowX2 - 8;
            int deleteX1 = deleteX2 - 16;
            if (click.button() == 0 && GuiRenderUtil.isHovered(deleteX1, centerY - 8, deleteX2, centerY + 8,
                    mouseX, mouseY)) {
                commitFocusedInput();
                drafts.remove(index);
                if (index < textInputs.size()) {
                    textInputs.remove(index);
                }
                bindingIndex = -1;
                SoundUtils.playWoodenButton();
                scrollRow = Math.min(scrollRow, maxScrollRow());
                return true;
            }

            int lineX1 = keyX2 + 12;
            int lineX2 = deleteX1 - 10;
            if (GuiRenderUtil.isHovered(lineX1, rowY + 6, lineX2, rowY2 - 6, mouseX, mouseY)) {
                commitFocusedInput();
                focusedIndex = index;
                inputAt(index).onMousePressed(this.font, mouseX - lineX1, Math.max(0, lineX2 - lineX1));
                return true;
            }
        }

        commitFocusedInput();
        focusedIndex = -1;
        bindingIndex = -1;
        return super.mouseClicked(click, isInsideWindow);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = maxScrollRow();
        if (max > 0 && verticalAmount != 0) {
            scrollRow = Math.max(0, Math.min(max, scrollRow - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();

        if (bindingIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingIndex = -1;
                return true;
            }
            if (KeyMappingUtils.isKeySupported(keyCode)) {
                recordKey(keyCode);
            }
            return true;
        }

        if (focusedIndex >= 0) {
            LineTextInput active = inputAt(focusedIndex);
            LineTextInput.KeyResult result = active.handleKey(keyCode, input.modifiers());
            if (result == LineTextInput.KeyResult.CANCEL) {
                active.setText(drafts.get(focusedIndex).text());
                focusedIndex = -1;
                return true;
            }
            if (result == LineTextInput.KeyResult.COMMIT) {
                commitFocusedInput();
                focusedIndex = -1;
                return true;
            }
            if (result == LineTextInput.KeyResult.HANDLED) {
                commitFocusedInput();
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelAndClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (focusedIndex >= 0 && inputAt(focusedIndex).handleCodePoint(input.codepoint())) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void commitFocusedInput() {
        if (focusedIndex < 0) {
            return;
        }
        if (focusedIndex < drafts.size()) {
            Keybinds.CommandBinding draft = drafts.get(focusedIndex);
            String text = inputAt(focusedIndex).getText();
            if (!text.equals(draft.text())) {
                drafts.set(focusedIndex, draft.withText(text));
            }
        }
    }

    private void saveAndClose() {
        commitFocusedInput();
        Keybinds.saveCommandBindings(new ArrayList<>(drafts));
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null) {
            mc.gui.setScreen(parentScreen);
        }
    }

    private void cancelAndClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null) {
            SoundUtils.playWoodenButton();
            mc.gui.setScreen(parentScreen);
        }
    }

    private float[] toLocal(float mouseX, float mouseY) {
        Minecraft mc = Minecraft.getInstance();
        float sr = ClickGuiState.fixedScaleRatio(mc);
        float dispW = PANEL_WIDTH * sr;
        float dispH = PANEL_HEIGHT * sr;
        float originX = (this.width - dispW) / 2f;
        float originY = (this.height - dispH) / 2f;
        return new float[]{(mouseX - originX) / sr, (mouseY - originY) / sr};
    }
}
