package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;

@Environment(EnvType.CLIENT)
public class MessageUtils {
    
    public static MutableText createColoredText(String text, int color) {
        return Text.literal(text).styled(style -> style.withColor(color));
    }
    
    public static MutableText createStyledText(String text, int color, boolean bold, boolean italic) {
        return Text.literal(text).styled(style -> style.withColor(color).withBold(bold).withItalic(italic));
    }
    
    public static MutableText createTextWithEmoji(String prefix, String emoji, String suffix, int emojiColor) {
        MutableText prefixText = Text.literal(prefix);
        MutableText emojiText = Text.literal(emoji).styled(style -> style.withColor(emojiColor));
        MutableText suffixText = Text.literal(suffix);
        return prefixText.append(emojiText).append(suffixText);
    }
    
    public static MutableText createBaityPrefix() {
        MutableText leftBracket = createColoredText("[", 0x555555);
        MutableText baityText = createColoredText("baity", 0x800080);
        MutableText rightBracket = createColoredText("] ", 0x555555);
        return leftBracket.append(baityText).append(rightBracket);
    }
    
    public static MutableText createMessageWithPrefix(String message, int messageColor) {
        MutableText prefix = createBaityPrefix();
        MutableText messageText = createColoredText(message, messageColor);
        return prefix.append(messageText);
    }
    
    public static MutableText createMessageWithPrefix(MutableText message) {
        MutableText prefix = createBaityPrefix();
        return prefix.append(message);
    }
    
    public static void sendBaityMessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText prefix = createBaityPrefix();
            MutableText messageText = createColoredText(message, 0xFFFFFF);
            MutableText fullMessage = prefix.append(messageText);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(fullMessage);
        }
    }
    
    public static void sendCustomMessage(MutableText message) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
        }
    }
}
