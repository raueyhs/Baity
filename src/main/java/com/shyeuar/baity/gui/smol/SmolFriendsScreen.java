package com.shyeuar.baity.gui.smol;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.smolpeople.SmolFriendManager;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.theme.LinearTheme;
import com.shyeuar.baity.utils.MessageUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
public class SmolFriendsScreen extends Screen {
    /** 与 ClickGui 相同的逻辑尺寸（BASE_GUI_SCALE=3 下的设计像素） */
    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 280;
    private static final int ROW_HEIGHT = 16;
    private static final int LIST_PADDING = 12;
    private static final int LIST_GAP = 12;

    private final Screen parentScreen;
    private int lobbyScroll = 0;
    private int friendsScroll = 0;
    private int selectedLobbyPlayer = -1;
    private int selectedFriend = -1;
    private String selectedLobbyPlayerName = null;

    public SmolFriendsScreen(Screen parentScreen) {
        super(Component.literal("Smol Friends"));
        this.parentScreen = parentScreen;
    }

    private static float actualGuiScale(Minecraft mc) {
        if (mc == null || mc.options == null) return 3f;
        int guiScaleOption = mc.options.guiScale().get();
        return (guiScaleOption <= 0) ? (float) mc.getWindow().getGuiScale() : guiScaleOption;
    }

    /** 与 ClickGuiLayout / ClickGuiRootComponent 一致：界面在屏幕上的缩放比例 */
    private static float scaleRatio(Minecraft mc) {
        return ClickGuiState.BASE_GUI_SCALE / actualGuiScale(mc);
    }

    @Override
    protected void init() {
        super.init();
        SmolFriendManager.refreshLobbyPlayersCache();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderMenuBackground(guiGraphics);

        Minecraft mc = Minecraft.getInstance();
        float sr = scaleRatio(mc);
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

        renderPanelContent(guiGraphics, localMx, localMy, delta);

        pose.popMatrix();

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    private void renderPanelContent(GuiGraphics guiGraphics, float mouseX, float mouseY, float delta) {
        int panelX = 0;
        int panelY = 0;
        int panelX2 = PANEL_WIDTH;
        int panelY2 = PANEL_HEIGHT;

        GuiRenderUtil.drawFrostedGlass(guiGraphics, panelX, panelY, panelX2, panelY2, LinearTheme.BG_SECONDARY.getRGB(), 8f);
        GuiRenderUtil.draw3DRect(guiGraphics, panelX, panelY, panelX2, panelY2, LinearTheme.BG_SECONDARY.getRGB(), 8f);
        GuiRenderUtil.stroke1px(guiGraphics, panelX, panelY, panelX2, panelY2, LinearTheme.BORDER_PRIMARY.getRGB());
        GuiRenderUtil.draw3DGradientRect(guiGraphics, panelX, panelY, panelX2, panelY + 22, LinearTheme.ACCENT_PRIMARY.getRGB(), LinearTheme.ACCENT_SECONDARY.getRGB(), 8f);

        guiGraphics.drawString(this.font, "SmolPeople Friends", panelX + 10, panelY + 7, 0xFFFFFFFF, false);

        int listLabelY = panelY + 30;
        int listTop = panelY + 42;
        int listBottom = panelY + PANEL_HEIGHT - 54;
        int totalListWidth = PANEL_WIDTH - LIST_PADDING * 2 - LIST_GAP;
        int singleListWidth = totalListWidth / 2;
        int lobbyListX1 = panelX + LIST_PADDING;
        int lobbyListX2 = lobbyListX1 + singleListWidth;
        int friendsListX1 = lobbyListX2 + LIST_GAP;
        int friendsListX2 = panelX2 - LIST_PADDING;

        guiGraphics.drawString(this.font, "Current Lobby", lobbyListX1 + 2, listLabelY, LinearTheme.TEXT_SECONDARY.getRGB(), false);
        guiGraphics.drawString(this.font, "Friends", friendsListX1 + 2, listLabelY, LinearTheme.TEXT_SECONDARY.getRGB(), false);

        List<String> lobbyPlayers = SmolFriendManager.getCurrentLobbyPlayers();
        List<String> friends = SmolFriendManager.getFriends();
        selectedLobbyPlayer = resolveSelection(selectedLobbyPlayerName, selectedLobbyPlayer, lobbyPlayers);
        selectedLobbyPlayerName = selectedLobbyPlayer >= 0 && selectedLobbyPlayer < lobbyPlayers.size()
            ? lobbyPlayers.get(selectedLobbyPlayer)
            : null;
        selectedFriend = clampSelection(selectedFriend, friends.size());

        lobbyScroll = clampScroll(lobbyScroll, lobbyPlayers.size(), listBottom - listTop);
        friendsScroll = clampScroll(friendsScroll, friends.size(), listBottom - listTop);
        int imx = (int) mouseX;
        int imy = (int) mouseY;
        drawList(guiGraphics, lobbyPlayers, lobbyListX1, listTop, lobbyListX2, listBottom, lobbyScroll, selectedLobbyPlayer, imx, imy);
        drawList(guiGraphics, friends, friendsListX1, listTop, friendsListX2, listBottom, friendsScroll, selectedFriend, imx, imy);

        int buttonY = panelY2 - 30;
        int addX1 = panelX + 18;
        int addX2 = addX1 + 132;
        int removeX1 = addX2 + 8;
        int removeX2 = removeX1 + 132;
        int toggleX1 = panelX2 - 150;
        int toggleX2 = panelX2 - 18;
        boolean canAdd = selectedLobbyPlayer >= 0
            && selectedLobbyPlayer < lobbyPlayers.size()
            && !SmolFriendManager.isFriend(lobbyPlayers.get(selectedLobbyPlayer));
        boolean canRemove = selectedFriend >= 0 && selectedFriend < friends.size();

        drawButton(guiGraphics, addX1, buttonY, addX2, buttonY + 18, "Add", canAdd, imx, imy);
        drawButton(guiGraphics, removeX1, buttonY, removeX2, buttonY + 18, "Remove", canRemove, imx, imy);
        String toggleText = ConfigManager.smolFriendsEnabled ? "FriendSmol ON" : "FriendSmol OFF";
        drawButton(guiGraphics, toggleX1, buttonY, toggleX2, buttonY + 18, toggleText, true, imx, imy);
        guiGraphics.drawString(this.font, "Select player on the left click Add, or use /baity fadd <name>.", panelX + 12, panelY2 - 44, LinearTheme.TEXT_TERTIARY.getRGB(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (click.button() != 0) {
            return super.mouseClicked(click, isInsideWindow);
        }

        Minecraft mc = Minecraft.getInstance();
        float sr = scaleRatio(mc);
        float dispW = PANEL_WIDTH * sr;
        float dispH = PANEL_HEIGHT * sr;
        float originX = (this.width - dispW) / 2f;
        float originY = (this.height - dispH) / 2f;
        double lx = (click.x() - originX) / sr;
        double ly = (click.y() - originY) / sr;

        int panelX = 0;
        int panelY = 0;
        int panelX2 = PANEL_WIDTH;
        final int panelY2 = PANEL_HEIGHT;
        int listTop = panelY + 42;
        int listBottom = panelY + PANEL_HEIGHT - 54;
        int totalListWidth = PANEL_WIDTH - LIST_PADDING * 2 - LIST_GAP;
        int singleListWidth = totalListWidth / 2;
        int lobbyListX1 = panelX + LIST_PADDING;
        int lobbyListX2 = lobbyListX1 + singleListWidth;
        int friendsListX1 = lobbyListX2 + LIST_GAP;
        int friendsListX2 = panelX2 - LIST_PADDING;

        List<String> lobbyPlayers = SmolFriendManager.getCurrentLobbyPlayers();
        List<String> friends = SmolFriendManager.getFriends();

        int clickedLobbyPlayer = getClickedIndex(lx, ly, lobbyListX1, listTop, lobbyListX2, listBottom, lobbyScroll, lobbyPlayers.size());
        if (clickedLobbyPlayer >= 0) {
            selectedLobbyPlayer = clickedLobbyPlayer;
            selectedLobbyPlayerName = lobbyPlayers.get(clickedLobbyPlayer);
            SoundUtils.playWoodenButton();
            return true;
        }

        int clickedFriend = getClickedIndex(lx, ly, friendsListX1, listTop, friendsListX2, listBottom, friendsScroll, friends.size());
        if (clickedFriend >= 0) {
            selectedFriend = clickedFriend;
            SoundUtils.playWoodenButton();
            return true;
        }

        int buttonY = panelY2 - 30;
        int addX1 = panelX + 18;
        int addX2 = addX1 + 132;
        int removeX1 = addX2 + 8;
        int removeX2 = removeX1 + 132;
        int toggleX1 = panelX2 - 150;
        int toggleX2 = panelX2 - 18;

        if (GuiRenderUtil.isHovered(addX1, buttonY, addX2, buttonY + 18, (float) lx, (float) ly)) {
            SoundUtils.playBubble();
            if (selectedLobbyPlayer >= 0 && selectedLobbyPlayer < lobbyPlayers.size()) {
                String name = lobbyPlayers.get(selectedLobbyPlayer);
                if (SmolFriendManager.addFriend(name)) {
                    selectedLobbyPlayer = -1;
                    selectedLobbyPlayerName = null;
                    selectedFriend = SmolFriendManager.getFriends().indexOf(name);
                    MessageUtils.sendBaityMessage("Added SmolPeople friend: " + name);
                } else {
                    MessageUtils.sendBaityMessage(name + " is already in SmolPeople friends.");
                }
            }
            return true;
        }

        if (GuiRenderUtil.isHovered(removeX1, buttonY, removeX2, buttonY + 18, (float) lx, (float) ly)) {
            SoundUtils.playBubble();
            if (selectedFriend >= 0 && selectedFriend < friends.size()) {
                String name = friends.get(selectedFriend);
                if (SmolFriendManager.removeFriend(name)) {
                    selectedFriend = -1;
                    MessageUtils.sendBaityMessage("Removed friend: " + name);
                } else {
                    MessageUtils.sendBaityMessage(name + " is not in SmolPeople friends.");
                }
            }
            return true;
        }

        if (GuiRenderUtil.isHovered(toggleX1, buttonY, toggleX2, buttonY + 18, (float) lx, (float) ly)) {
            SoundUtils.playBubble();
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

        Minecraft mc = Minecraft.getInstance();
        float sr = scaleRatio(mc);
        float dispW = PANEL_WIDTH * sr;
        float dispH = PANEL_HEIGHT * sr;
        float originX = (this.width - dispW) / 2f;
        float originY = (this.height - dispH) / 2f;
        double lx = (mouseX - originX) / sr;
        double ly = (mouseY - originY) / sr;

        int panelX = 0;
        int panelY = 0;
        int panelX2 = PANEL_WIDTH;
        int listTop = panelY + 42;
        int listBottom = panelY + PANEL_HEIGHT - 54;
        int totalListWidth = PANEL_WIDTH - LIST_PADDING * 2 - LIST_GAP;
        int singleListWidth = totalListWidth / 2;
        int lobbyListX1 = panelX + LIST_PADDING;
        int lobbyListX2 = lobbyListX1 + singleListWidth;
        int friendsListX1 = lobbyListX2 + LIST_GAP;
        int friendsListX2 = panelX2 - LIST_PADDING;

        if (GuiRenderUtil.isHovered(lobbyListX1, listTop, lobbyListX2, listBottom, (float) lx, (float) ly)) {
            lobbyScroll += verticalAmount > 0 ? -1 : 1;
            lobbyScroll = clampScroll(lobbyScroll, SmolFriendManager.getCurrentLobbyPlayers().size(), listBottom - listTop);
            SoundUtils.playWoodenButton();
            return true;
        }

        if (GuiRenderUtil.isHovered(friendsListX1, listTop, friendsListX2, listBottom, (float) lx, (float) ly)) {
            friendsScroll += verticalAmount > 0 ? -1 : 1;
            friendsScroll = clampScroll(friendsScroll, SmolFriendManager.getFriends().size(), listBottom - listTop);
            SoundUtils.playWoodenButton();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == 256) {
            SoundUtils.playWoodenButton();
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

    private int resolveSelection(String selectedName, int selectedIndex, List<String> values) {
        if (selectedName != null) {
            int index = values.indexOf(selectedName);
            if (index >= 0) {
                return index;
            }
        }
        return clampSelection(selectedIndex, values.size());
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
