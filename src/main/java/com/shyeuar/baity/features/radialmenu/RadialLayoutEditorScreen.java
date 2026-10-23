package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.features.radialmenu.data.RadialMenuModels;
import com.shyeuar.baity.features.radialmenu.data.RadialPresetStore;
import com.shyeuar.baity.gui.input.LineTextInput;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.radial.RadialWheelRenderer;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.LinearTheme;
import com.shyeuar.baity.utils.MessageUtils;
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
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class RadialLayoutEditorScreen extends Screen {

    public static final int PANEL_WIDTH = (int) ClickGuiState.WIDTH;
    public static final int PANEL_HEIGHT = (int) ClickGuiState.HEIGHT;
    private static final int SIDEBAR_WIDTH = (int) ClickGuiState.SIDEBAR_WIDTH;
    private static final int ROW_HEIGHT = 16;
    private static final int BTN_HEIGHT = 22;
    private static final int TREE_TOP = 28;
    private static final int TREE_BOTTOM = 238;
    private static final int ADD_BTN_Y = 244;
    private static final int DEL_BTN_Y = 270;
    private static final int WHEEL_TOP = 28;
    private static final int WHEEL_BOTTOM = 198;
    private static final int PROPS_TOP = 204;
    private static final int EDITOR_OUTER_RADIUS = 52;
    private static final int PROPS_RIGHT_MARGIN = 10;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int LINE_GRAY = 0xFF787878;
    private static final String UNICODE_ICON_TIP =
            "Tip:Support color code(&).For example,&1 = dark blue";

    private final Screen parentScreen;

    private int treeScroll;
    private String pendingDeleteKey;
    private int dragSlotIndex = -1;
    private int dragHoverSlotIndex = -1;
    private String hoveredPropTooltip;

    private final LineTextInput commandInput = new LineTextInput(LineTextInput.Policy.freeText(-1));
    private final LineTextInput displayNameInput = new LineTextInput(LineTextInput.Policy.freeText(-1));
    private final LineTextInput iconInput = new LineTextInput(LineTextInput.Policy.radialIconId(64));
    private final LineTextInput unicodeInput = new LineTextInput(
            LineTextInput.Policy.freeText(RadialMenuModels.MAX_UNICODE_ICON_CODE_POINTS));

    private enum FocusField {
        NONE, COMMAND, DISPLAY_NAME, ICON, UNICODE
    }

    private FocusField focusField = FocusField.NONE;

    public RadialLayoutEditorScreen(Screen parentScreen) {
        super(Component.literal("Radial Layout Editor"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        RadialIconLibrary.ensureInitialized();
        RadialPresetStore.init();
        syncInputsFromSelection();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        com.shyeuar.baity.utils.TextInputUtils.sync(focusField != FocusField.NONE, this);
        this.extractMenuBackground(guiGraphics);
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
        GuiRenderUtil.draw3DGradientRect(g, 0, 0, PANEL_WIDTH, 22, LinearTheme.ACCENT_PRIMARY.getRGB(), LinearTheme.ACCENT_SECONDARY.getRGB(), 8f);
        g.text(this.font, "Radial Layout Editor", 10, 7, 0xFFFFFFFF, false);

        int treeX1 = 8;
        int treeX2 = SIDEBAR_WIDTH - 4;
        drawTree(g, treeX1, TREE_TOP, treeX2, TREE_BOTTOM, (int) mouseX, (int) mouseY);

        int btnX1 = 10;
        int btnX2 = SIDEBAR_WIDTH - 10;
        drawSidebarButton(g, btnX1, ADD_BTN_Y, btnX2, ADD_BTN_Y + BTN_HEIGHT, true, false, true, (int) mouseX, (int) mouseY);
        boolean canDelete = canDeleteSelection();
        boolean deleteArmed = pendingDeleteKey != null && pendingDeleteKey.equals(currentDeleteKey());
        drawSidebarButton(g, btnX1, DEL_BTN_Y, btnX2, DEL_BTN_Y + BTN_HEIGHT,
                canDelete || deleteArmed, deleteArmed, false, (int) mouseX, (int) mouseY);

        int editX1 = SIDEBAR_WIDTH + 8;
        int editX2 = PANEL_WIDTH - 8;
        int wheelCenterX = editX1 + (editX2 - editX1) / 2;
        int wheelCenterY = WHEEL_TOP + (WHEEL_BOTTOM - WHEEL_TOP) / 2;
        drawWheelEditor(g, wheelCenterX, wheelCenterY, (int) mouseX, (int) mouseY);

        hoveredPropTooltip = null;
        drawPropertyPanel(g, editX1, PROPS_TOP, editX2, PANEL_HEIGHT - 8, (int) mouseX, (int) mouseY);
        if (hoveredPropTooltip != null) {
            drawPropTooltip(g, hoveredPropTooltip, (int) mouseX, (int) mouseY);
        }
    }

    private void drawTree(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        GuiRenderUtil.draw3DRect(g, x1, y1, x2, y2, LinearTheme.BG_TERTIARY.getRGB(), 6f);
        GuiRenderUtil.stroke1px(g, x1, y1, x2, y2, LinearTheme.BORDER_PRIMARY.getRGB());

        List<TreeRow> rows = buildTreeRows();
        treeScroll = clampScroll(treeScroll, rows.size(), (y2 - y1) / ROW_HEIGHT);
        int visibleRows = Math.max(1, (y2 - y1) / ROW_HEIGHT);
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;

        for (int row = 0; row < visibleRows; row++) {
            int index = treeScroll + row;
            if (index >= rows.size()) {
                break;
            }
            TreeRow treeRow = rows.get(index);
            int rowY = y1 + row * ROW_HEIGHT;
            boolean selected = treeRow.presetId.equals(editor.activePresetId)
                    && editor.selectedLayerId.equals(treeRow.layerId)
                    && editor.selectedSlotIndex == treeRow.slotIndex;
            boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            int rowX1 = x1 + 2;
            int rowX2 = x2 - 2;
            if (treeRow.presetRow) {
                if (selected) {
                    g.fill(rowX1, rowY, rowX2, rowY + ROW_HEIGHT, 0x22FFFFFF);
                    g.fill(rowX1, rowY + 1, rowX1 + 3, rowY + ROW_HEIGHT - 1, LinearTheme.ACCENT_SECONDARY.getRGB());
                } else if (hovered) {
                    g.fill(rowX1, rowY, rowX2, rowY + ROW_HEIGHT, 0x12FFFFFF);
                }
            } else {
                if (selected) {
                    g.fill(rowX1, rowY + 1, rowX2, rowY + ROW_HEIGHT - 1, 0x18FFFFFF);
                    g.fill(rowX1, rowY + 1, rowX1 + 2, rowY + ROW_HEIGHT - 1, LinearTheme.ACCENT_PRIMARY.getRGB());
                } else if (hovered) {
                    g.fill(rowX1, rowY + 1, rowX2, rowY + ROW_HEIGHT - 1, 0x10FFFFFF);
                }
            }

            int indent = treeRow.presetRow ? 6 : 6 + treeRow.depth * 8;
            int textX = rowX1 + indent;
            int maxTextWidth = rowX2 - textX - 2;
            String prefix = treeRow.expandKey != null
                    ? (RadialPresetStore.isExpanded(treeRow.expandKey) ? "\u25BC " : "\u25B6 ")
                    : (treeRow.presetRow ? "" : "  ");
            String label = ellipsize(prefix + treeRow.label, maxTextWidth);
            int textColor = selected ? 0xFFFFFFFF : LinearTheme.TEXT_PRIMARY.getRGB();
            g.text(this.font, label, textX, rowY + 4, textColor, false);
        }
    }

    private void drawWheelEditor(GuiGraphicsExtractor g, int centerX, int centerY, int mouseX, int mouseY) {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return;
        }
        String layerId = RadialPresetStore.getBundle().editor.selectedLayerId;
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null) {
            return;
        }

        if (dragSlotIndex >= 0) {
            dragHoverSlotIndex = slotIndexFromAngle(centerX, centerY, mouseX, mouseY, layer.slots.size());
            if (dragHoverSlotIndex < 0) {
                dragHoverSlotIndex = dragSlotIndex;
            }
        }

        float scale = EDITOR_OUTER_RADIUS / (float) RadialWheelRenderer.OUTER_RADIUS;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(scale, scale);
        pose.translate(-centerX, -centerY);
        RadialWheelRenderer.drawWheel(g, centerX, centerY);

        int count = layer.slots.size();
        if (count > 0) {
            double anglePerSection = 360.0 / count;
            double startAngle = RadialWheelRenderer.getStartAngle(count);
            RadialWheelRenderer.drawSectorDividers(g, centerX, centerY, count, startAngle, anglePerSection);

            int highlight = dragSlotIndex >= 0 && dragHoverSlotIndex >= 0
                    ? dragHoverSlotIndex
                    : RadialPresetStore.getBundle().editor.selectedSlotIndex;
            if (highlight >= 0 && highlight < count) {
                double sectionStart = startAngle + highlight * anglePerSection;
                RadialWheelRenderer.drawHoveredSector(g, centerX, centerY, sectionStart, sectionStart + anglePerSection);
            }

            List<RadialMenuModels.RadialSlot> displaySlots = previewSlots(layer.slots, dragSlotIndex, dragHoverSlotIndex);

            for (int i = 0; i < count; i++) {
                RadialMenuModels.RadialSlot slot = displaySlots.get(i);
                float[] iconPos = RadialWheelRenderer.sectorCenter(centerX, centerY, startAngle, anglePerSection, i,
                        RadialWheelRenderer.INNER_RADIUS, RadialWheelRenderer.OUTER_RADIUS);
                RadialSlotRenderer.drawSlotIcon(g, this.font, slot, iconPos[0], iconPos[1]);
            }

            RadialSlotRenderer.drawSlotLabels(g, this.font, centerX, centerY, startAngle, anglePerSection, count,
                    displaySlots, highlight);
        }
        pose.popMatrix();

        drawEditorCenterAdd(g, centerX, centerY, mouseX, mouseY);
    }

    private void drawEditorCenterAdd(GuiGraphicsExtractor g, int centerX, int centerY, int mouseX, int mouseY) {
        String label = "Add";
        int textW = this.font.width(label);
        int textH = this.font.lineHeight;
        int x1 = centerX - textW / 2 - 2;
        int x2 = centerX + textW / 2 + 2;
        int y1 = centerY - textH / 2 - 1;
        int y2 = centerY + textH / 2 + 1;
        boolean hovered = GuiRenderUtil.isHovered(x1, y1, x2, y2, mouseX, mouseY);
        int labelColor = hovered ? YELLOW : LinearTheme.TEXT_PRIMARY.getRGB();
        g.text(this.font, label, centerX - textW / 2, centerY - textH / 2, labelColor, false);
    }

    private boolean isEditorAddHit(int centerX, int centerY, int mx, int my) {
        String label = "Add";
        int textW = this.font.width(label);
        int textH = this.font.lineHeight;
        int x1 = centerX - textW / 2 - 2;
        int x2 = centerX + textW / 2 + 2;
        int y1 = centerY - textH / 2 - 1;
        int y2 = centerY + textH / 2 + 1;
        return GuiRenderUtil.isHovered(x1, y1, x2, y2, mx, my);
    }

    private void drawPropertyPanel(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        GuiRenderUtil.draw3DRect(g, x1, y1, x2, y2, LinearTheme.BG_TERTIARY.getRGB(), 6f);
        GuiRenderUtil.stroke1px(g, x1, y1, x2, y2, LinearTheme.BORDER_PRIMARY.getRGB());

        int rowH = 18;
        int x = x1 + 8;
        int lineRight = x2 - PROPS_RIGHT_MARGIN;
        int y = y1 + 8;
        drawInlineInputRow(g, "command", commandInput, FocusField.COMMAND, x, lineRight, y, mouseX, mouseY);
        y += rowH;
        drawInlineInputRow(g, "display name", displayNameInput, FocusField.DISPLAY_NAME, x, lineRight, y, mouseX, mouseY);
        y += rowH;
        drawInlineInputRow(g, "icon", iconInput, FocusField.ICON, x, lineRight, y, mouseX, mouseY, true);
        y += rowH;
        drawInlineInputRow(g, "unicode icon", unicodeInput, FocusField.UNICODE, x, lineRight, y, mouseX, mouseY);
    }

    private void drawInlineInputRow(GuiGraphicsExtractor g, String label, LineTextInput input, FocusField field,
                                    int x, int lineRight, int y, int mouseX, int mouseY) {
        drawInlineInputRow(g, label, input, field, x, lineRight, y, mouseX, mouseY, false);
    }

    private void drawInlineInputRow(GuiGraphicsExtractor g, String label, LineTextInput input, FocusField field,
                                    int x, int lineRight, int y, int mouseX, int mouseY,
                                    boolean showIconPreview) {
        String prefix = label + ":";
        int labelColor = LinearTheme.TEXT_SECONDARY.getRGB();
        g.text(this.font, prefix, x, y + 2, labelColor, false);

        int lineX1 = x + this.font.width(prefix) + 4;
        int previewSpace = showIconPreview ? 22 : 0;
        int lineX2 = lineRight - previewSpace;
        float lineY = y + this.font.lineHeight + 1;
        float hoverY1 = lineY - 10;
        float hoverY2 = lineY + 5;
        boolean focused = focusField == field;
        boolean hovered = GuiRenderUtil.isHovered(lineX1, hoverY1, lineX2, hoverY2, mouseX, mouseY);
        if (field == FocusField.UNICODE) {
            int labelRight = lineX1 - 2;
            boolean tipHover = GuiRenderUtil.isHovered(x, y, labelRight, y + this.font.lineHeight + 4, mouseX, mouseY)
                    || hovered;
            if (tipHover) {
                hoveredPropTooltip = UNICODE_ICON_TIP;
            }
        }
        int lineColor = (hovered || focused) ? YELLOW : LINE_GRAY;
        GuiRenderUtil.drawRoundedRect(g, lineX1, lineY, lineX2, lineY + 1, 0, lineColor);

        String text = input.getText();
        int textColor = (hovered || focused) ? YELLOW : LinearTheme.TEXT_PRIMARY.getRGB();
        int textY = (int) (lineY - 9);
        int maxTextWidth = Math.max(0, lineX2 - lineX1);
        if (focused) {
            LineTextInput.drawTextWithBlinkCursor(
                    g, this.font, text, input.getCaretCp(),
                    lineX1, textY, textColor, true,
                    LineTextInput.shouldBlinkCursor(),
                    maxTextWidth);
        } else if (!text.isEmpty()) {
            LineTextInput.drawTextWithBlinkCursor(
                    g, this.font, text, 0,
                    lineX1, textY, textColor, false,
                    false,
                    maxTextWidth);
        }

        if (showIconPreview) {
            String previewIcon = focused ? text : iconInput.getText();
            if (previewIcon != null && !previewIcon.isBlank()) {
                if (RadialIconLibrary.isResolvableIcon(previewIcon)) {
                    float previewX = lineRight - 10;
                    float previewY = y + this.font.lineHeight - 2;
                    RadialSlotRenderer.drawIconPreview(g, this.font, previewIcon, previewX, previewY);
                }
            }
        }
    }

    private void drawPropTooltip(GuiGraphicsExtractor g, String tip, int mouseX, int mouseY) {
        int padding = 4;
        int textW = this.font.width(tip);
        int textH = this.font.lineHeight;
        int tipX = mouseX + 8;
        int tipY = mouseY + 8;
        int tipX2 = tipX + textW + padding * 2;
        int tipY2 = tipY + textH + padding * 2;
        if (tipX2 > PANEL_WIDTH - 4) {
            tipX = Math.max(4, mouseX - textW - padding * 2 - 8);
            tipX2 = tipX + textW + padding * 2;
        }
        if (tipY2 > PANEL_HEIGHT - 4) {
            tipY = Math.max(4, mouseY - textH - padding * 2 - 8);
            tipY2 = tipY + textH + padding * 2;
        }
        GuiRenderUtil.draw3DRect(g, tipX, tipY, tipX2, tipY2, 0xEE1A1A1A, 4f);
        GuiRenderUtil.stroke1px(g, tipX, tipY, tipX2, tipY2, LinearTheme.BORDER_PRIMARY.getRGB());
        g.text(this.font, tip, tipX + padding, tipY + padding, YELLOW, false);
    }

    private void drawSidebarButton(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2,
                                   boolean enabled, boolean deleteArmed, boolean isAddButton, int mouseX, int mouseY) {
        boolean hovered = enabled && GuiRenderUtil.isHovered(x1, y1, x2, y2, mouseX, mouseY);
        int bg = !enabled ? LinearTheme.BG_TERTIARY.getRGB()
                : (deleteArmed ? 0x33FF6A5C : (hovered ? 0x18FFFFFF : LinearTheme.BG_SECONDARY.getRGB()));
        GuiRenderUtil.draw3DRect(g, x1, y1, x2, y2, bg, 4f);
        GuiRenderUtil.stroke1px(g, x1, y1, x2, y2, LinearTheme.BORDER_PRIMARY.getRGB());

        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        int iconColor = enabled ? LinearTheme.TEXT_PRIMARY.getRGB() : LinearTheme.TEXT_TERTIARY.getRGB();
        if (isAddButton) {
            String label = "Add";
            g.text(this.font, label, centerX - this.font.width(label) / 2, centerY - this.font.lineHeight / 2, iconColor, false);
        } else if (deleteArmed) {
            g.text(this.font, "Confirm?", centerX - this.font.width("Confirm?") / 2, centerY - this.font.lineHeight / 2,
                    0xFFFF8888, false);
        } else {
            drawTrashIcon(g, centerX, centerY, iconColor);
        }
    }

    private void drawTrashIcon(GuiGraphicsExtractor g, int centerX, int centerY, int color) {
        int lidY = centerY - 5;
        int bodyY1 = centerY - 2;
        int bodyY2 = centerY + 5;
        int bodyX1 = centerX - 4;
        int bodyX2 = centerX + 4;
        g.fill(centerX - 5, lidY, centerX + 5, lidY + 2, color);
        g.fill(centerX - 1, lidY - 2, centerX + 1, lidY, color);
        g.fill(bodyX1, bodyY1, bodyX2, bodyY2, color);
        g.fill(bodyX1 + 1, bodyY1 + 2, bodyX1 + 2, bodyY2 - 1, LinearTheme.BG_SECONDARY.getRGB());
        g.fill(centerX, bodyY1 + 2, centerX + 1, bodyY2 - 1, LinearTheme.BG_SECONDARY.getRGB());
        g.fill(bodyX2 - 2, bodyY1 + 2, bodyX2 - 1, bodyY2 - 1, LinearTheme.BG_SECONDARY.getRGB());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        commitInputs();
        focusField = FocusField.NONE;
        if (click.button() == 1) {
            return handlePrimaryClick(click) || super.mouseClicked(click, isInsideWindow);
        }
        if (click.button() == 3) {
            return handleSecondaryClick(click) || super.mouseClicked(click, isInsideWindow);
        }
        return super.mouseClicked(click, isInsideWindow);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 1 && dragSlotIndex >= 0) {
            commitDrag();
            dragSlotIndex = -1;
            dragHoverSlotIndex = -1;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() == 1 && dragSlotIndex >= 0) {
            float[] local = toLocal((float) click.x(), (float) click.y());
            RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
            if (preset != null) {
                RadialMenuModels.RadialLayer layer = preset.layers.get(RadialPresetStore.getBundle().editor.selectedLayerId);
                if (layer != null) {
                    int centerX = SIDEBAR_WIDTH + 8 + (PANEL_WIDTH - SIDEBAR_WIDTH - 16) / 2;
                    int centerY = WHEEL_TOP + (WHEEL_BOTTOM - WHEEL_TOP) / 2;
                    dragHoverSlotIndex = slotIndexFromAngle(centerX, centerY, (int) local[0], (int) local[1], layer.slots.size());
                    if (dragHoverSlotIndex < 0) {
                        dragHoverSlotIndex = dragSlotIndex;
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        float[] local = toLocal((float) mouseX, (float) mouseY);
        if (local[0] >= 8 && local[0] <= SIDEBAR_WIDTH - 4 && local[1] >= TREE_TOP && local[1] <= TREE_BOTTOM) {
            treeScroll += verticalAmount > 0 ? -1 : 1;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (focusField != FocusField.NONE) {
            LineTextInput active = activeInput();
            LineTextInput.KeyResult result = active.handleKey(input.input(), input.modifiers());
            if (result == LineTextInput.KeyResult.CANCEL) {
                focusField = FocusField.NONE;
                syncInputsFromSelection();
                return true;
            }
            if (result == LineTextInput.KeyResult.COMMIT || result == LineTextInput.KeyResult.HANDLED) {
                commitInputs();
                if (result == LineTextInput.KeyResult.COMMIT) {
                    focusField = FocusField.NONE;
                }
                return true;
            }
        }
        if (input.input() == InputConstants.KEY_ESCAPE) {
            commitInputs();
            SoundUtils.playWoodenButton();
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (focusField != FocusField.NONE && activeInput().handleCodePoint(input.codepoint())) {
            commitInputs();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        com.shyeuar.baity.utils.TextInputUtils.sync(false, this);
        commitInputs();
        if (this.minecraft != null && parentScreen != null) {
            this.minecraft.gui.setScreen(parentScreen);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean handlePrimaryClick(MouseButtonEvent click) {
        float[] local = toLocal((float) click.x(), (float) click.y());
        int mx = (int) local[0];
        int my = (int) local[1];

        if (handleTreeClick(mx, my, false)) {
            return true;
        }
        if (handleSidebarButtons(mx, my)) {
            return true;
        }
        if (handlePropertyClick(mx, my)) {
            return true;
        }
        return handleWheelClick(mx, my);
    }

    private boolean handleSecondaryClick(MouseButtonEvent click) {
        float[] local = toLocal((float) click.x(), (float) click.y());
        return handleTreeClick((int) local[0], (int) local[1], true);
    }

    private boolean handleTreeClick(int mx, int my, boolean rightClick) {
        if (mx < 8 || mx > SIDEBAR_WIDTH - 4 || my < TREE_TOP || my > TREE_BOTTOM) {
            return false;
        }
        List<TreeRow> rows = buildTreeRows();
        int index = treeScroll + (my - TREE_TOP) / ROW_HEIGHT;
        if (index < 0 || index >= rows.size()) {
            return false;
        }
        TreeRow row = rows.get(index);
        if (rightClick) {
            return handleTreeRightClick(row);
        }
        if (row.presetRow) {
            RadialPresetStore.selectPreset(row.presetId);
        } else {
            RadialPresetStore.selectLayer(row.presetId, row.layerId);
            RadialPresetStore.selectSlot(row.slotIndex);
        }
        pendingDeleteKey = null;
        syncInputsFromSelection();
        SoundUtils.playWoodenButton();
        return true;
    }

    private boolean handleTreeRightClick(TreeRow row) {
        if (row.presetRow || row.expandKey == null) {
            return true;
        }
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getBundle().findPreset(row.presetId);
        if (preset == null) {
            return true;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(row.layerId);
        if (layer == null || row.slotIndex < 0 || row.slotIndex >= layer.slots.size()) {
            return true;
        }
        RadialMenuModels.RadialSlot slot = layer.slots.get(row.slotIndex);
        RadialMenuModels.RadialLayer child = slot.childLayerId == null ? null : preset.layers.get(slot.childLayerId);
        if (child != null && child.slots.isEmpty()) {
            RadialPresetStore.setExpanded(row.expandKey, true);
            RadialPresetStore.selectLayer(row.presetId, slot.childLayerId);
            RadialPresetStore.selectSlot(-1);
            pendingDeleteKey = null;
            syncInputsFromSelection();
            SoundUtils.playWoodenButton();
            return true;
        }
        RadialPresetStore.toggleExpanded(row.expandKey);
        SoundUtils.playWoodenButton();
        return true;
    }

    private boolean canDeleteSelection() {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return false;
        }
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;
        if (editor.selectedSlotIndex >= 0) {
            return true;
        }
        return preset.deletable && editor.selectedLayerId.equals(preset.rootLayerId);
    }

    private String currentDeleteKey() {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return null;
        }
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;
        if (editor.selectedSlotIndex >= 0) {
            return "slot:" + editor.activePresetId + ":" + editor.selectedLayerId + ":" + editor.selectedSlotIndex;
        }
        if (preset.deletable && editor.selectedLayerId.equals(preset.rootLayerId)) {
            return "preset:" + preset.id;
        }
        return null;
    }

    private boolean handleSidebarButtons(int mx, int my) {
        int btnX1 = 10;
        int btnX2 = SIDEBAR_WIDTH - 10;
        if (GuiRenderUtil.isHovered(btnX1, ADD_BTN_Y, btnX2, ADD_BTN_Y + BTN_HEIGHT, mx, my)) {
            handleAddAction();
            return true;
        }
        if (GuiRenderUtil.isHovered(btnX1, DEL_BTN_Y, btnX2, DEL_BTN_Y + BTN_HEIGHT, mx, my)) {
            handleDeleteAction();
            return true;
        }
        return false;
    }

    private void handleAddAction() {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return;
        }
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;
        if (editor.selectedLayerId.equals(preset.rootLayerId) && editor.selectedSlotIndex < 0) {
            RadialMenuModels.RadialPreset created = RadialPresetStore.addPreset();
            if (created != null) {
                syncInputsFromSelection();
                MessageUtils.sendBaityMessage("Created preset " + created.name);
            } else {
                MessageUtils.sendBaityMessage("Preset limit reached.");
            }
        } else if (editor.selectedSlotIndex >= 0) {
            RadialMenuModels.RadialLayer child = RadialPresetStore.addChildLayer(preset, editor.selectedLayerId, editor.selectedSlotIndex);
            if (child == null) {
                MessageUtils.sendBaityMessage("Cannot add more sub-layers.");
            } else {
                syncInputsFromSelection();
            }
        } else {
            MessageUtils.sendBaityMessage("Select a sector first to add a sub-layer.");
        }
        SoundUtils.playWoodenButton();
    }

    private void handleDeleteAction() {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null || !canDeleteSelection()) {
            return;
        }
        String deleteKey = currentDeleteKey();
        if (deleteKey == null) {
            return;
        }
        if (!deleteKey.equals(pendingDeleteKey)) {
            pendingDeleteKey = deleteKey;
            SoundUtils.playWoodenButton();
            return;
        }
        RadialPresetStore.deleteSelection(preset);
        pendingDeleteKey = null;
        syncInputsFromSelection();
        SoundUtils.playWoodenButton();
    }

    private boolean handlePropertyClick(int mx, int my) {
        int editX1 = SIDEBAR_WIDTH + 8;
        int editX2 = PANEL_WIDTH - 8;
        if (mx < editX1 || mx > editX2 || my < PROPS_TOP) {
            return false;
        }
        int x = editX1 + 8;
        int lineRight = editX2 - PROPS_RIGHT_MARGIN;
        int y = PROPS_TOP + 8;
        int rowH = 18;
        if (tryFocusInlineField(FocusField.COMMAND, "command", x, lineRight, y, mx, my)) return true;
        y += rowH;
        if (tryFocusInlineField(FocusField.DISPLAY_NAME, "display name", x, lineRight, y, mx, my)) return true;
        y += rowH;
        if (tryFocusInlineField(FocusField.ICON, "icon", x, lineRight, y, mx, my)) return true;
        y += rowH;
        if (tryFocusInlineField(FocusField.UNICODE, "unicode icon", x, lineRight, y, mx, my)) return true;
        focusField = FocusField.NONE;
        return false;
    }

    private boolean tryFocusInlineField(FocusField field, String label, int x, int lineRight, int y,
                                        int mx, int my) {
        int lineX1 = x + this.font.width(label + ":") + 4;
        int previewSpace = field == FocusField.ICON ? 22 : 0;
        int lineX2 = lineRight - previewSpace;
        float lineY = y + this.font.lineHeight + 1;
        float hoverY1 = lineY - 10;
        float hoverY2 = lineY + 5;
        if (GuiRenderUtil.isHovered(lineX1, hoverY1, lineX2, hoverY2, mx, my)) {
            commitInputs();
            focusField = field;
            LineTextInput active = activeInput();
            active.setTextAndCaretAtEnd(active.getText());
            int maxTextWidth = Math.max(0, lineX2 - lineX1);
            active.onMousePressed(this.font, mx - lineX1, maxTextWidth);
            SoundUtils.playWoodenButton();
            return true;
        }
        return false;
    }

    private boolean handleWheelClick(int mx, int my) {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return false;
        }
        int editX1 = SIDEBAR_WIDTH + 8;
        int editX2 = PANEL_WIDTH - 8;
        if (mx < editX1 || mx > editX2 || my < WHEEL_TOP || my > WHEEL_BOTTOM) {
            return false;
        }
        int centerX = editX1 + (editX2 - editX1) / 2;
        int centerY = WHEEL_TOP + (WHEEL_BOTTOM - WHEEL_TOP) / 2;
        if (isEditorAddHit(centerX, centerY, mx, my)) {
            commitInputs();
            RadialPresetStore.addSlot(preset, RadialPresetStore.getBundle().editor.selectedLayerId);
            syncInputsFromSelection();
            SoundUtils.playWoodenButton();
            return true;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(RadialPresetStore.getBundle().editor.selectedLayerId);
        if (layer == null) {
            return false;
        }
        int slotIndex = slotIndexAt(centerX, centerY, mx, my, layer.slots.size());
        if (slotIndex >= 0) {
            commitInputs();
            RadialPresetStore.selectSlot(slotIndex);
            pendingDeleteKey = null;
            syncInputsFromSelection();
            dragSlotIndex = slotIndex;
            dragHoverSlotIndex = slotIndex;
            SoundUtils.playWoodenButton();
            return true;
        }
        return false;
    }

    private void commitDrag() {
        if (dragSlotIndex < 0 || dragHoverSlotIndex < 0) {
            return;
        }
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(RadialPresetStore.getBundle().editor.selectedLayerId);
        if (layer == null) {
            return;
        }
        RadialPresetStore.reorderSlot(layer, dragSlotIndex, dragHoverSlotIndex);
        syncInputsFromSelection();
    }

    private int slotIndexFromAngle(int centerX, int centerY, int mouseX, int mouseY, int count) {
        if (count <= 0) {
            return -1;
        }
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
            return -1;
        }
        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        if (degrees < 0) {
            degrees += 360;
        }
        return RadialWheelRenderer.getSectionFromAngle(degrees, count);
    }

    private int slotIndexAt(int centerX, int centerY, int mouseX, int mouseY, int count) {
        if (count <= 0) {
            return -1;
        }
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double scaledInner = RadialWheelRenderer.INNER_RADIUS * EDITOR_OUTER_RADIUS / (float) RadialWheelRenderer.OUTER_RADIUS;
        double scaledOuter = EDITOR_OUTER_RADIUS;
        if (dist <= scaledInner || dist > scaledOuter + 8) {
            return -1;
        }
        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        if (degrees < 0) {
            degrees += 360;
        }
        return RadialWheelRenderer.getSectionFromAngle(degrees, count);
    }

    private List<RadialMenuModels.RadialSlot> previewSlots(List<RadialMenuModels.RadialSlot> slots, int from, int to) {
        if (from < 0 || to < 0 || from == to || from >= slots.size() || to >= slots.size()) {
            return slots;
        }
        List<RadialMenuModels.RadialSlot> result = new ArrayList<>(slots);
        RadialMenuModels.RadialSlot moved = result.remove(from);
        result.add(to, moved);
        return result;
    }

    private void syncInputsFromSelection() {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            commandInput.clear();
            displayNameInput.clear();
            iconInput.clear();
            unicodeInput.clear();
            return;
        }
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;
        RadialMenuModels.RadialLayer layer = preset.layers.get(editor.selectedLayerId);
        if (layer == null || editor.selectedSlotIndex < 0 || editor.selectedSlotIndex >= layer.slots.size()) {
            commandInput.clear();
            displayNameInput.clear();
            iconInput.clear();
            unicodeInput.clear();
            return;
        }
        RadialMenuModels.RadialSlot slot = layer.slots.get(editor.selectedSlotIndex);
        commandInput.setTextAndCaretAtEnd(slot.command == null ? "" : slot.command);
        displayNameInput.setTextAndCaretAtEnd(slot.displayName == null ? "" : slot.displayName);
        iconInput.setTextAndCaretAtEnd(slot.icon == null ? "" : slot.icon);
        unicodeInput.setTextAndCaretAtEnd(slot.unicodeIcon == null ? "" : slot.unicodeIcon);
    }

    private void commitInputs() {
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return;
        }
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;
        if (editor.selectedSlotIndex < 0) {
            return;
        }
        RadialPresetStore.updateSelectedSlot(
                preset,
                editor.selectedLayerId,
                editor.selectedSlotIndex,
                commandInput.getText(),
                displayNameInput.getText(),
                RadialIconLibrary.normalizeIconName(iconInput.getText()),
                RadialPresetStore.clampUnicode(unicodeInput.getText())
        );
    }

    private LineTextInput activeInput() {
        return switch (focusField) {
            case COMMAND -> commandInput;
            case DISPLAY_NAME -> displayNameInput;
            case ICON -> iconInput;
            case UNICODE -> unicodeInput;
            default -> commandInput;
        };
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

    private List<TreeRow> buildTreeRows() {
        List<TreeRow> rows = new ArrayList<>();
        RadialMenuModels.EditorState editor = RadialPresetStore.getBundle().editor;
        for (RadialMenuModels.RadialPreset preset : RadialPresetStore.getBundle().presets) {
            rows.add(new TreeRow(preset.id, preset.rootLayerId, -1, preset.name, 0, null, true));
            if (preset.id.equals(editor.activePresetId)) {
                appendSlotRows(rows, preset, preset.rootLayerId, 1);
            }
        }
        return rows;
    }

    private void appendSlotRows(List<TreeRow> rows, RadialMenuModels.RadialPreset preset, String layerId, int depth) {
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null) {
            return;
        }
        for (int i = 0; i < layer.slots.size(); i++) {
            RadialMenuModels.RadialSlot slot = layer.slots.get(i);
            String label = slot.displayName == null || slot.displayName.isBlank()
                    ? String.valueOf(slot.serial > 0 ? slot.serial : i + 1)
                    : slot.displayName;
            boolean hasChild = slot.childLayerId != null && preset.layers.containsKey(slot.childLayerId);
            String expandKey = hasChild ? RadialPresetStore.treeNodeId(preset.id, slot.childLayerId) : null;
            rows.add(new TreeRow(preset.id, layerId, i, label, depth, expandKey, false));
            if (hasChild && RadialPresetStore.isExpanded(expandKey)) {
                appendSlotRows(rows, preset, slot.childLayerId, depth + 1);
            }
        }
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String dots = "...";
        int dotsWidth = this.font.width(dots);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String candidate = builder.toString() + new String(Character.toChars(cp));
            if (this.font.width(candidate) + dotsWidth > maxWidth) {
                break;
            }
            builder.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return builder + dots;
    }

    private static int clampScroll(int scroll, int size, int visibleRows) {
        int maxScroll = Math.max(0, size - visibleRows);
        return Math.max(0, Math.min(scroll, maxScroll));
    }

    private record TreeRow(String presetId, String layerId, int slotIndex, String label, int depth,
                           String expandKey, boolean presetRow) {
    }
}
