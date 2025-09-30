package com.shyeuar.baity;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BaityClient implements ClientModInitializer {
    public static boolean smolpeopleMode = false;
    public static boolean blockAnimationMode = false;
    private static final String CONFIG_FILE = "baity_config.txt";

    @Override
    public void onInitializeClient() {
        // 加载保存的配置
        loadConfig();
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("baity")
            .then(ClientCommandManager.literal("smolpeople")
                .then(ClientCommandManager.literal("on")
                    .executes(ctx -> {
                        smolpeopleMode = true;
                        saveConfig();
                        sendBaityMessage("SmolPeople enabled!");
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(ClientCommandManager.literal("off")
                    .executes(ctx -> {
                        smolpeopleMode = false;
                        saveConfig();
                        sendBaityMessage("SmolPeople disabled!");
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(ClientCommandManager.literal("blockanimation")
                .then(ClientCommandManager.literal("on")
                    .executes(ctx -> {
                        blockAnimationMode = true;
                        saveConfig();
                        sendBaityMessage("BlockAnimation enabled!");
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(ClientCommandManager.literal("off")
                    .executes(ctx -> {
                        blockAnimationMode = false;
                        saveConfig();
                        sendBaityMessage("BlockAnimation disabled!");
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
        ));
    }

    public static void sendBaityMessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText leftBracket = Text.literal("[").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
            MutableText baityText = Text.literal("baity").setStyle(Style.EMPTY.withColor(Formatting.YELLOW));
            MutableText rightBracket = Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
            MutableText messageText = Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.WHITE));
            MutableText fullMessage = leftBracket.append(baityText).append(rightBracket).append(messageText);
            MinecraftClient.getInstance().player.sendMessage(fullMessage, false);
        }
    }

    private static void saveConfig() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            String config = smolpeopleMode + "\n" + blockAnimationMode;
            Files.write(configPath, config.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save Baity config: " + e.getMessage());
        }
    }

    private static void loadConfig() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath).trim();
                String[] lines = content.split("\n");
                if (lines.length >= 1) {
                    smolpeopleMode = Boolean.parseBoolean(lines[0]);
                }
                if (lines.length >= 2) {
                    blockAnimationMode = Boolean.parseBoolean(lines[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load Baity config: " + e.getMessage());
        }
    }

}
