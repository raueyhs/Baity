package com.shyeuar.baity.features.chat;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.hud.HudElement;
import com.shyeuar.baity.gui.hud.HudManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class ChatChannelSwitcher implements HudElement {
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_TOP_GAP = 10;
    private static final int BUTTON_MIN_WIDTH = 28;
    private static final int BUTTON_HORIZONTAL_PADDING = 8;
    private static final int TOOLTIP_PADDING = 4;
    private static final int TOOLTIP_OFFSET = 1;
    private static final int BACKGROUND_COLOR = 0x7A000000;
    private static final int HOVER_COLOR = 0x96000000;
    private static final int ACTIVE_COLOR = 0xA0202020;
    private static final int BORDER_COLOR = 0x50FFFFFF;
    private static final int ACTIVE_ACCENT_COLOR = 0xFF35D9FF;
    private static final int TEXT_COLOR = 0xFFF4F4F4;
    private static final int TOOLTIP_BACKGROUND_COLOR = 0xD0101010;
    private static final int TOOLTIP_BORDER_COLOR = 0x90FFFFFF;
    private static final float DEFAULT_SCALE = 1.0f;
    private static final int CHAT_INPUT_LEFT_MARGIN = 4;
    private static final int CHAT_INPUT_BOTTOM_Y = 12;
    private static final String[] HOVER_TOOLTIP_LINES = new String[]{
        "右键临时频道",
        "中键隐藏提示"
    };

    private static final Channel[] CHANNELS = new Channel[]{
        new Channel("All", "all", "/achat "),
        new Channel("Party", "party", "/pchat "),
        new Channel("Guild", "guild", "/gchat "),
        new Channel("Coop", "coop", "/cchat ")
    };

    private static final ChatChannelSwitcher INSTANCE = new ChatChannelSwitcher();
    private static boolean chatListenerRegistered = false;

    private boolean selected;
    private boolean clicked;
    private String lastSelectedChannel = ConfigManager.chatChannelSwitcherLastChannel;
    private String pendingChannel = "";

    private ChatChannelSwitcher() {
    }

    public static void init() {
        HudManager.getInstance().register(INSTANCE);
        if (!chatListenerRegistered) {
            chatListenerRegistered = true;
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                INSTANCE.handleChannelFeedback(message.getString());
            });
        }
    }

    public static void render(Screen screen, EditBox chatField, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        INSTANCE.renderInternal(screen, chatField, guiGraphics, mouseX, mouseY);
    }

    public static boolean mouseClicked(Screen screen, EditBox chatField, MouseButtonEvent click) {
        return INSTANCE.mouseClickedInternal(screen, chatField, click);
    }

    private void renderInternal(Screen screen, EditBox chatField, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!shouldRender(screen, chatField)) {
            return;
        }

        ensureHudPositionInitialized(chatField);

        Font font = Minecraft.getInstance().font;
        List<ChannelButton> buttons = getButtons();
        int scaledWidth = getDummyWidth(false);
        int scaledHeight = getDummyHeight(false);
        int originX = getAbsX(scaledWidth);
        int originY = getAbsY(scaledHeight);
        float scale = getScale();
        String activeChannel = getActiveChannel(chatField);
        ChannelButton hoveredButton = null;

        for (ChannelButton button : buttons) {
            int x1 = originX + Math.round(button.x1() * scale);
            int y1 = originY + Math.round(button.y1() * scale);
            int x2 = originX + Math.round(button.x2() * scale);
            int y2 = originY + Math.round(button.y2() * scale);

            boolean hovered = GuiRenderUtil.isHovered(x1, y1, x2, y2, mouseX, mouseY);
            if (hovered) {
                hoveredButton = button;
            }
            boolean active = button.command().equals(activeChannel);
            int backgroundColor = active ? ACTIVE_COLOR : (hovered ? HOVER_COLOR : BACKGROUND_COLOR);

            guiGraphics.fill(x1, y1, x2, y2, backgroundColor);
            guiGraphics.fill(x1, y1, x2, y1 + 1, BORDER_COLOR);
            guiGraphics.fill(x1, y2 - 1, x2, y2, BORDER_COLOR);
            guiGraphics.fill(x1, y1, x1 + 1, y2, BORDER_COLOR);
            guiGraphics.fill(x2 - 1, y1, x2, y2, BORDER_COLOR);

            if (active) {
                guiGraphics.fill(x1, y2 - Math.max(2, Math.round(2 * scale)), x2, y2, ACTIVE_ACCENT_COLOR);
            }

            int textColor = active ? ACTIVE_ACCENT_COLOR : TEXT_COLOR;
            float textScale = scale;
            int textWidth = Math.round(font.width(button.label()) * textScale);
            int textX = x1 + Math.max(2, (x2 - x1 - textWidth) / 2);
            int textY = y1 + Math.max(1, (y2 - y1 - Math.round(font.lineHeight * textScale)) / 2);

            var matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate((float) textX, (float) textY);
            matrices.scale(textScale, textScale);
            guiGraphics.drawString(font, button.label(), 0, 0, textColor, false);
            matrices.popMatrix();
        }

        if (hoveredButton != null && !ConfigManager.chatChannelSwitcherHintHidden) {
            renderTooltip(guiGraphics, font, hoveredButton, originX, originY, scale);
        }
    }

    private boolean mouseClickedInternal(Screen screen, EditBox chatField, MouseButtonEvent click) {
        if (!shouldRender(screen, chatField)) {
            return false;
        }

        ensureHudPositionInitialized(chatField);

        int scaledWidth = getDummyWidth(false);
        int scaledHeight = getDummyHeight(false);
        int originX = getAbsX(scaledWidth);
        int originY = getAbsY(scaledHeight);
        float scale = getScale();

        for (ChannelButton button : getButtons()) {
            int x1 = originX + Math.round(button.x1() * scale);
            int y1 = originY + Math.round(button.y1() * scale);
            int x2 = originX + Math.round(button.x2() * scale);
            int y2 = originY + Math.round(button.y2() * scale);

            if (!GuiRenderUtil.isHovered(x1, y1, x2, y2, (float) click.x(), (float) click.y())) {
                continue;
            }

            Minecraft client = Minecraft.getInstance();
            if (click.button() == 0 && client.player != null) {
                pendingChannel = button.command();
                client.player.connection.sendCommand("chat " + button.command());
                chatField.setFocused(true);
                return true;
            }

            if (click.button() == 1) {
                chatField.setValue(button.autofillCommand());
                chatField.setCursorPosition(button.autofillCommand().length());
                chatField.moveCursorToEnd(false);
                chatField.setFocused(true);
                return true;
            }

            if (click.button() == 2) {
                ConfigManager.chatChannelSwitcherHintHidden = !ConfigManager.chatChannelSwitcherHintHidden;
                ConfigManager.requestSave();
                chatField.setFocused(true);
                return true;
            }
        }

        return false;
    }

    private boolean shouldRender(Screen screen, EditBox chatField) {
        return ConfigManager.chatChannelSwitcherEnabled
            && screen != null
            && screen instanceof ChatScreen
            && chatField != null
            && Minecraft.getInstance().player != null;
    }

    private void ensureHudPositionInitialized(EditBox chatField) {
        if (!Double.isNaN(ConfigManager.chatChannelSwitcherX) && !Double.isNaN(ConfigManager.chatChannelSwitcherY)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int width = getWidth();
        int height = getHeight();
        int centerX = chatField.getX() + width / 2;
        int centerY = chatField.getY() - BUTTON_TOP_GAP - BUTTON_HEIGHT + height / 2;

        ConfigManager.chatChannelSwitcherX = centerX / (double) screenWidth;
        ConfigManager.chatChannelSwitcherY = centerY / (double) screenHeight;
        ConfigManager.requestSave();
    }

    private List<ChannelButton> getButtons() {
        Font font = Minecraft.getInstance().font;
        List<ChannelButton> buttons = new ArrayList<>(CHANNELS.length);
        int currentX = 0;
        for (Channel channel : CHANNELS) {
            int buttonWidth = Math.max(BUTTON_MIN_WIDTH, font.width(channel.label()) + BUTTON_HORIZONTAL_PADDING * 2);
            buttons.add(new ChannelButton(
                channel.label(),
                channel.command(),
                channel.autofillCommand(),
                currentX,
                0,
                currentX + buttonWidth,
                BUTTON_HEIGHT
            ));
            currentX += buttonWidth + BUTTON_GAP;
        }
        return buttons;
    }

    private String getActiveChannel(EditBox chatField) {
        return normalizeChannel(lastSelectedChannel);
    }

    private void handleChannelFeedback(String messageText) {
        if (pendingChannel.isEmpty()) {
            return;
        }

        String normalizedMessage = messageText == null ? "" : messageText.trim().toLowerCase(Locale.ROOT);
        if (normalizedMessage.isEmpty()) {
            return;
        }

        String confirmedChannel = parseConfirmedChannel(normalizedMessage);
        if (!confirmedChannel.isEmpty()) {
            pendingChannel = "";
            setLastSelectedChannel(confirmedChannel);
            return;
        }

        String failedChannel = parseFailedChannel(normalizedMessage);
        if (!failedChannel.isEmpty() && failedChannel.equals(pendingChannel)) {
            pendingChannel = "";
        }
    }

    private String parseConfirmedChannel(String normalizedMessage) {
        if (!normalizedMessage.startsWith("you are now in the ")) {
            return "";
        }

        if (normalizedMessage.contains(" all channel")) {
            return "all";
        }
        if (normalizedMessage.contains(" party channel")) {
            return "party";
        }
        if (normalizedMessage.contains(" guild channel")) {
            return "guild";
        }
        if (normalizedMessage.contains(" skyblock co-op channel") || normalizedMessage.contains(" co-op channel") || normalizedMessage.contains(" coop channel")) {
            return "coop";
        }
        return "";
    }

    private String parseFailedChannel(String normalizedMessage) {
        if (!normalizedMessage.contains("channel")) {
            return "";
        }

        if (normalizedMessage.contains("party channel")) {
            return "party";
        }
        if (normalizedMessage.contains("guild channel")) {
            return "guild";
        }
        if (normalizedMessage.contains("co-op channel") || normalizedMessage.contains("coop channel")) {
            return "coop";
        }
        if (normalizedMessage.contains("all channel")) {
            return "all";
        }
        return "";
    }

    private void setLastSelectedChannel(String channel) {
        String normalized = normalizeChannel(channel);
        if (normalized.equals(lastSelectedChannel) && normalized.equals(ConfigManager.chatChannelSwitcherLastChannel)) {
            return;
        }
        lastSelectedChannel = normalized;
        ConfigManager.chatChannelSwitcherLastChannel = normalized;
        ConfigManager.requestSave();
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return "";
        }

        String normalized = channel.trim().toLowerCase(Locale.ROOT);
        for (Channel available : CHANNELS) {
            if (available.command().equals(normalized)) {
                return normalized;
            }
        }
        return "";
    }

    @Override
    public String getId() {
        return "chatChannelSwitcher";
    }

    @Override
    public String getDisplayName() {
        return "ChatChannelSwitcher";
    }

    @Override
    public double getX() {
        return Double.isNaN(ConfigManager.chatChannelSwitcherX) ? getDefaultX() : ConfigManager.chatChannelSwitcherX;
    }

    @Override
    public double getY() {
        return Double.isNaN(ConfigManager.chatChannelSwitcherY) ? getDefaultY() : ConfigManager.chatChannelSwitcherY;
    }

    @Override
    public void setX(double x) {
        ConfigManager.chatChannelSwitcherX = x;
    }

    @Override
    public void setY(double y) {
        ConfigManager.chatChannelSwitcherY = y;
    }

    @Override
    public float getScale() {
        return ConfigManager.chatChannelSwitcherScale;
    }

    @Override
    public void setScale(float scale) {
        ConfigManager.chatChannelSwitcherScale = Math.max(0.5f, Math.min(4.0f, scale));
    }

    @Override
    public double getDefaultX() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0.33;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        return (CHAT_INPUT_LEFT_MARGIN + getWidth() / 2.0) / screenWidth;
    }

    @Override
    public double getDefaultY() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0.84;
        }

        int screenHeight = client.getWindow().getGuiScaledHeight();
        int topY = screenHeight - CHAT_INPUT_BOTTOM_Y - BUTTON_TOP_GAP - BUTTON_HEIGHT;
        return (topY + getHeight() / 2.0) / screenHeight;
    }

    @Override
    public float getDefaultScale() {
        return DEFAULT_SCALE;
    }

    @Override
    public void reset() {
        ConfigManager.chatChannelSwitcherX = getDefaultX();
        ConfigManager.chatChannelSwitcherY = getDefaultY();
        ConfigManager.chatChannelSwitcherScale = DEFAULT_SCALE;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isClicked() {
        return clicked;
    }

    @Override
    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    @Override
    public int getWidth() {
        List<ChannelButton> buttons = getButtons();
        return buttons.isEmpty() ? 0 : buttons.get(buttons.size() - 1).x2();
    }

    @Override
    public int getHeight() {
        return BUTTON_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTicks) {
    }

    @Override
    public boolean shouldRender() {
        return ConfigManager.chatChannelSwitcherEnabled && Minecraft.getInstance().screen instanceof ChatScreen;
    }

    private void renderTooltip(GuiGraphics guiGraphics, Font font, ChannelButton button, int originX, int originY, float scale) {
        int textWidth = 0;
        for (String line : HOVER_TOOLTIP_LINES) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int tooltipWidth = textWidth + TOOLTIP_PADDING * 2;
        int tooltipHeight = HOVER_TOOLTIP_LINES.length * font.lineHeight + TOOLTIP_PADDING * 2;

        int buttonX1 = originX + Math.round(button.x1() * scale);
        int buttonY1 = originY + Math.round(button.y1() * scale);
        int buttonX2 = originX + Math.round(button.x2() * scale);

        int x1 = buttonX1 + (buttonX2 - buttonX1 - tooltipWidth) / 2;
        int y1 = buttonY1 - tooltipHeight - TOOLTIP_OFFSET;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        if (x1 < 2) {
            x1 = 2;
        } else if (x1 + tooltipWidth > screenWidth - 2) {
            x1 = screenWidth - tooltipWidth - 2;
        }

        if (y1 < 2) {
            y1 = buttonY1 + Math.round(BUTTON_HEIGHT * scale) + TOOLTIP_OFFSET;
        }

        int x2 = x1 + tooltipWidth;
        int y2 = y1 + tooltipHeight;

        guiGraphics.fill(x1, y1, x2, y2, TOOLTIP_BACKGROUND_COLOR);
        guiGraphics.fill(x1, y1, x2, y1 + 1, TOOLTIP_BORDER_COLOR);
        guiGraphics.fill(x1, y2 - 1, x2, y2, TOOLTIP_BORDER_COLOR);
        guiGraphics.fill(x1, y1, x1 + 1, y2, TOOLTIP_BORDER_COLOR);
        guiGraphics.fill(x2 - 1, y1, x2, y2, TOOLTIP_BORDER_COLOR);

        int textY = y1 + TOOLTIP_PADDING;
        for (String line : HOVER_TOOLTIP_LINES) {
            guiGraphics.drawString(font, line, x1 + TOOLTIP_PADDING, textY, TEXT_COLOR, false);
            textY += font.lineHeight;
        }
    }

    private record Channel(String label, String command, String autofillCommand) {
    }

    private record ChannelButton(String label, String command, String autofillCommand, int x1, int y1, int x2, int y2) {
    }
}
