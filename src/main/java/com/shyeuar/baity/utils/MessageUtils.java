package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

@Environment(EnvType.CLIENT)
public class MessageUtils {
    
    private static final float PASTEL_SATURATION = 0.45f;

    private static final String[] SYNC_HELP_LINES = {
        " 1. 代理必须是 HTTP 代理（不支持 SOCKS）",
        " 2. 代理不在本机：BaityPresenceProxyHost 填内网 IP，Port 填对应端口",
        " 3. 代理需要账号密码：BaityPresenceProxyAuth 填 user:pass",
        " 4. 本机 7890 / 7891 / 7892 会自动探测，通常无需手填；这些值也可能被程序自动写回",
        " 5. 以上都无误仍失败：多为远端服务或网络问题，稍后重试 /baity sync",
        " 6. 我迟早得给这坨重做了，现在没能力没时间"
    };
    
    public static MutableComponent createColoredText(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(color));
    }
    
    public static MutableComponent createStyledText(String text, int color, boolean bold, boolean italic) {
        return Component.literal(text).withStyle(style -> style.withColor(color).withBold(bold).withItalic(italic));
    }
    
    public static MutableComponent createTextWithEmoji(String prefix, String emoji, String suffix, int emojiColor) {
        MutableComponent prefixText = Component.literal(prefix);
        MutableComponent emojiText = Component.literal(emoji).withStyle(style -> style.withColor(emojiColor));
        MutableComponent suffixText = Component.literal(suffix);
        return prefixText.append(emojiText).append(suffixText);
    }
    
    public static MutableComponent createBaityPrefix() {
        int gradientStart = 0xFF00FF;
        int gradientEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
        
        String prefixText = "[baity] ";
        int length = prefixText.length();
        
        MutableComponent result = Component.empty();
        for (int i = 0; i < length; i++) {
            float progress = i / (float)(length - 1);
            int charColor = ColorGradientUtils.blendColors(gradientStart, gradientEnd, progress);
            result.append(createColoredText(String.valueOf(prefixText.charAt(i)), charColor));
        }
        
        return result;
    }
    
    public static MutableComponent createMessageWithPrefix(String message, int messageColor) {
        MutableComponent prefix = createBaityPrefix();
        MutableComponent messageText = createColoredText(message, messageColor);
        return prefix.append(messageText);
    }
    
    public static MutableComponent createMessageWithPrefix(MutableComponent message) {
        MutableComponent prefix = createBaityPrefix();
        return prefix.append(message);
    }
    
    public static int animatedPastelRgbAt(float x, long nowMs) {
        int cycleMs = 4000;
        float time = 1.0f - (nowMs % cycleMs) / (float) cycleMs;
        float hue = (float) positiveModulo(time + (x % 100.0f) / 100.0f, 1.0);
        return net.minecraft.util.Mth.hsvToRgb(hue, PASTEL_SATURATION, 1.0f);
    }
    
    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }
    
    public static void sendBaityMessage(String message) {
        if (Minecraft.getInstance().player != null) {
            MutableComponent prefix = createBaityPrefix();
            MutableComponent messageText = createColoredText(message, 0xFFFFFF);
            MutableComponent fullMessage = prefix.append(messageText);
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(fullMessage);
        }
    }
    
    public static void sendCustomMessage(MutableComponent message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
        }
    }

    public static void sendUserText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return;
        }
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) {
            return;
        }
        if (text.charAt(0) == '/') {
            player.connection.sendCommand(text.substring(1));
        } else {
            player.connection.sendChat(text);
        }
    }

    public static void sendSyncStartForCommand() {
        sendCustomMessage(createMessageWithPrefix(createColoredText("Syncing remote data...", 0xAAAAAA)));
    }

    public static void sendSyncResult(boolean success, boolean isNotification) {
        int color = success ? 0x32CD32 : 0xDC143C;
        String base = success ? "Succeeded to sync remote data!" : "Failed to sync remote data.";
        MutableComponent msg = createColoredText(base + " ", color);

        if (isNotification) {
            MutableComponent stop = Component.literal("[stop notifications]")
                .withStyle(style -> style
                    .withColor(0xFF69B4)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.RunCommand("/baity notification off")));
            msg.append(stop);
            if (!success) {
                msg.append(Component.literal(" "));
                msg.append(buildHelpClickable());
            }
            sendCustomMessage(createMessageWithPrefix(msg));
            return;
        }

        if (!success) {
            msg.append(Component.literal(" "));
            msg.append(buildHelpClickable());
        }
        sendCustomMessage(createMessageWithPrefix(msg));
    }

    private static MutableComponent buildHelpClickable() {
        return Component.literal("[无法同步?]")
            .withStyle(Style.EMPTY
                .withColor(0xFF69B4)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/baity sync help")));
    }

    public static void sendSyncHelpLinesInChat() {
        if (Minecraft.getInstance().player == null) return;
        int yellow = 0xFFFF00;
        MutableComponent line = Component.literal("--------------------------------------------------").withStyle(s -> s.withColor(yellow));
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(line);
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
            Component.literal("[baity] 同步失败排查：").withStyle(s -> s.withColor(0x55FFFF)));
        for (String helpLine : SYNC_HELP_LINES) {
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
                Component.literal(helpLine).withStyle(s -> s.withColor(0xFFFFFF)));
        }
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(line);
    }
}
