package com.shyeuar.baity.gui.smol;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.smolpeople.SmolFriendManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.LinearTheme;
import com.shyeuar.baity.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
public class SmolFriendsScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 280;
    private static final int ROW_HEIGHT = 16;
    private static final int LIST_PADDING = 12;

    private final Screen parentScreen;
    private int friendsScroll = 0;
    private int selectedFriend = -1;

    public SmolFriendsScreen(Screen parentScreen) {
        super(Component.literal("Smol Friends"));
        this.parentScreen = parentScreen;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderMenuBackground(guiGraphics);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int panelX2 = panelX + PANEL_WIDTH;
        int panelY2 = panelY + PANEL_HEIGHT;

        GuiRenderUtil.drawFrostedGlass(guiGraphics, panelX, panelY, panelX2, panelY2, LinearTheme.BG_SECONDARY.getRGB(), 8f);
        GuiRenderUtil.draw3DRect(guiGraphics, panelX, panelY, panelX2, panelY2, LinearTheme.BG_SECONDARY.getRGB(), 8f);
        GuiRenderUtil.stroke1px(guiGraphics, panelX, panelY, panelX2, panelY2, LinearTheme.BORDER_PRIMARY.getRGB());
        GuiRenderUtil.draw3DGradientRect(guiGraphics, panelX, panelY, panelX2, panelY + 22, LinearTheme.ACCENT_PRIMARY.getRGB(), LinearTheme.ACCENT_SECONDARY.getRGB(), 8f);

        guiGraphics.drawString(this.font, "SmolPeople Friends", panelX + 10, panelY + 7, 0xFFFFFFFF, false);

        int listTop = panelY + 34;
        int listBottom = panelY + PANEL_HEIGHT - 54;
        int listX1 = panelX + LIST_PADDING;
        int listX2 = panelX2 - LIST_PADDING;

        List<String> friends = SmolFriendManager.getFriends();
        selectedFriend = clampSelection(selectedFriend, friends.size());

        friendsScroll = clampScroll(friendsScroll, friends.size(), listBottom - listTop);
        drawList(guiGraphics, friends, listX1, listTop, listX2, listBottom, friendsScroll, selectedFriend, mouseX, mouseY);

        int buttonY = panelY2 - 30;
        int removeX1 = panelX2 - 290;
        int removeX2 = panelX2 - 158;
        int toggleX1 = panelX2 - 150;
        int toggleX2 = panelX2 - 18;
        boolean canRemove = selectedFriend >= 0 && selectedFriend < friends.size();

        drawButton(guiGraphics, removeX1, buttonY, removeX2, buttonY + 18, "Remove", canRemove, mouseX, mouseY);
        String toggleText = ConfigManager.smolFriendsEnabled ? "FriendSmol ON" : "FriendSmol OFF";
        drawButton(guiGraphics, toggleX1, buttonY, toggleX2, buttonY + 18, toggleText, true, mouseX, mouseY);
        guiGraphics.drawString(this.font, "Use /baity fadd <name> to add friend.", panelX + 12, panelY2 - 44, LinearTheme.TEXT_TERTIARY.getRGB(), false);

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (click.button() != 0) {
            return super.mouseClicked(click, isInsideWindow);
        }

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int panelX2 = panelX + PANEL_WIDTH;
        int panelY2 = panelY + PANEL_HEIGHT;
        int listTop = panelY + 34;
        int listBottom = panelY + PANEL_HEIGHT - 54;
        int listX1 = panelX + LIST_PADDING;
        int listX2 = panelX2 - LIST_PADDING;

        List<String> friends = SmolFriendManager.getFriends();

        int clickedFriend = getClickedIndex(click.x(), click.y(), listX1, listTop, listX2, listBottom, friendsScroll, friends.size());
        if (clickedFriend >= 0) {
            selectedFriend = clickedFriend;
            return true;
        }

        int buttonY = panelY2 - 30;
        int removeX1 = panelX2 - 290;
        int removeX2 = panelX2 - 158;
        int toggleX1 = panelX2 - 150;
        int toggleX2 = panelX2 - 18;

        if (GuiRenderUtil.isHovered(removeX1, buttonY, removeX2, buttonY + 18, (float) click.x(), (float) click.y())) {
            if (selectedFriend >= 0 && selectedFriend < friends.size()) {
                String name = friends.get(selectedFriend);
                if (SmolFriendManager.removeFriend(name)) {
                    MessageUtils.sendBaityMessage("Removed friend: " + name);
                } else {
                    MessageUtils.sendBaityMessage(name + " is not in SmolPeople friends.");
                }
            }
            return true;
        }

        if (GuiRenderUtil.isHovered(toggleX1, buttonY, toggleX2, buttonY + 18, (float) click.x(), (float) click.y())) {
            ConfigManager.smolFriendsEnabled = !ConfigManager.smolFriendsEnabled;
            ConfigManager.saveConfig();
            MessageUtils.sendBaityMessage("FriendSmol is now " + (ConfigManager.smolFriendsEnabled ? "enabled" : "disabled") + ".");
            return true;
        }

        return super.mouseClicked(click, isInsideWindow);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int panelX2 = panelX + PANEL_WIDTH;
        int listTop = panelY + 34;
        int listBottom = panelY + PANEL_HEIGHT - 54;
        int listX1 = panelX + LIST_PADDING;
        int listX2 = panelX2 - LIST_PADDING;

        if (GuiRenderUtil.isHovered(listX1, listTop, listX2, listBottom, (float) mouseX, (float) mouseY)) {
            friendsScroll += verticalAmount > 0 ? -1 : 1;
            friendsScroll = clampScroll(friendsScroll, SmolFriendManager.getFriends().size(), listBottom - listTop);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.parentScreen != null) {
            this.minecraft.setScreen(this.parentScreen);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawList(GuiGraphics guiGraphics, List<String> values,
                          int x1, int y1, int x2, int y2,
                          int scroll, int selected, int mouseX, int mouseY) {
        GuiRenderUtil.draw3DRect(guiGraphics, x1, y1, x2, y2, LinearTheme.BG_TERTIARY.getRGB(), 6f);
        GuiRenderUtil.stroke1px(guiGraphics, x1, y1, x2, y2, LinearTheme.BORDER_PRIMARY.getRGB());

        int visibleRows = Math.max(1, (y2 - y1) / ROW_HEIGHT);
        for (int row = 0; row < visibleRows; row++) {
            int index = scroll + row;
            if (index >= values.size()) {
                break;
            }

            int rowY1 = y1 + row * ROW_HEIGHT;
            int rowY2 = rowY1 + ROW_HEIGHT;
            boolean hovered = GuiRenderUtil.isHovered(x1 + 1, rowY1, x2 - 1, rowY2, mouseX, mouseY);
            boolean isSelected = index == selected;

            if (isSelected) {
                GuiRenderUtil.drawGradientRect(guiGraphics, x1 + 1, rowY1, x2 - 1, rowY2, LinearTheme.ACCENT_PRIMARY.getRGB(), LinearTheme.ACCENT_SECONDARY.getRGB(), 0f);
            } else if (hovered) {
                guiGraphics.fill(x1 + 1, rowY1, x2 - 1, rowY2, LinearTheme.BG_HOVER.getRGB());
            }

            int textColor = isSelected ? 0xFFFFFFFF : LinearTheme.TEXT_SECONDARY.getRGB();
            guiGraphics.drawString(this.font, values.get(index), x1 + 6, rowY1 + 4, textColor, false);
        }
    }

    private void drawButton(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, String text, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && GuiRenderUtil.isHovered(x1, y1, x2, y2, mouseX, mouseY);
        int bgColor;
        if (!enabled) {
            bgColor = LinearTheme.BG_PRIMARY.getRGB();
        } else if (hovered) {
            bgColor = LinearTheme.BG_HOVER.getRGB();
        } else {
            bgColor = LinearTheme.BG_ACTIVE.getRGB();
        }

        GuiRenderUtil.draw3DRect(guiGraphics, x1, y1, x2, y2, bgColor, 4f);
        GuiRenderUtil.stroke1px(guiGraphics, x1, y1, x2, y2, enabled ? LinearTheme.BORDER_ACCENT.getRGB() : LinearTheme.BORDER_PRIMARY.getRGB());

        int textColor = enabled ? LinearTheme.TEXT_PRIMARY.getRGB() : LinearTheme.TEXT_TERTIARY.getRGB();
        int textX = x1 + (x2 - x1 - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, textX, y1 + 5, textColor, false);
    }

    private int clampScroll(int scroll, int itemCount, int contentHeight) {
        int visibleRows = Math.max(1, contentHeight / ROW_HEIGHT);
        int maxScroll = Math.max(0, itemCount - visibleRows);
        if (scroll < 0) {
            return 0;
        }
        return Math.min(scroll, maxScroll);
    }

    private int clampSelection(int selected, int size) {
        if (selected < 0 || selected >= size) {
            return -1;
        }
        return selected;
    }

    private int getClickedIndex(double mouseX, double mouseY, int x1, int y1, int x2, int y2, int scroll, int size) {
        if (!GuiRenderUtil.isHovered(x1 + 1, y1, x2 - 1, y2, (float) mouseX, (float) mouseY)) {
            return -1;
        }

        int row = (int) ((mouseY - y1) / ROW_HEIGHT);
        int index = scroll + row;
        if (index < 0 || index >= size) {
            return -1;
        }
        return index;
    }
}
