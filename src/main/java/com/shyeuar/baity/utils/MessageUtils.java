package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class MessageUtils {

    public static MutableText createColoredText(String text, Formatting color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color));
    }

    public static MutableText appendText(MutableText first, MutableText second) {
        return first.append(second);
    }

    public static MutableText createBaityPrefix() {
        MutableText leftBracket = createColoredText("[", Formatting.DARK_GRAY);
        MutableText baityText = createColoredText("baity", Formatting.DARK_PURPLE);
        MutableText rightBracket = createColoredText("] ", Formatting.DARK_GRAY);
        return leftBracket.append(baityText).append(rightBracket);
    }

    public static void sendBaityMessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText prefix = createBaityPrefix();
            MutableText messageText = createColoredText(message, Formatting.WHITE);
            MutableText fullMessage = appendText(prefix, messageText);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(fullMessage);
        }
    }

    public static void sendCustomMessage(MutableText message) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
        }
    }
}

