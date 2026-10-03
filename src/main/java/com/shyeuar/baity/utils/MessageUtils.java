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

    public static void sendSyncStartForCommand() {
        sendCustomMessage(createMessageWithPrefix(createColoredText("Syncing remote data...", 0xAAAAAA)));
    }

    public static void sendSyncResult(boolean success, boolean isNotification) {
        int color = success ? 0x32CD32 : 0xDC143C;
        String base = success ? "Succeeded to sync remote data!" : "Failed to sync remote data.";
        MutableComponent msg = createColoredText(base + " ", color);

        if (isNotification) {
            MutableComponent stop = Component.literal("[stop to prompt]")
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
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("检查你代理工具的 HTTP 代理端口，确保其与配置文件中的 BaityPresenceProxyPort 参数值相同。BaityPresenceProxyHost 为代理地址：代理运行在本机时一般填 127.0.0.1（或 localhost）；只有当代理运行在局域网的另一台设备上时，才需要填写那台设备的内网 IP。").withStyle(s -> s.withColor(0xFFFFFF)));
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(line);
    }
}
