package com.shyeuar.baity.config;

public class BaityConfig {
    public static boolean smolpeopleMode = false;
    public static boolean blockAnimationMode = false;
    public static boolean crosshairMode = true;
    public static boolean guiEnabled = true;
    public static int guiKeyCode = 345;
    public static boolean playerEspEnabled = false;
    public static boolean playerEspShowDistance = true;
    public static boolean playerEspShowOwnNametag = false;
    public static boolean pepCatEnabled = true;
    private static final String CONFIG_FILE = "baity_config.txt";

    public static void saveConfig() {
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get(CONFIG_FILE);
            StringBuilder config = new StringBuilder();
            config.append("SmolPeople:").append(smolpeopleMode).append("\n");
            config.append("BlockAnimation:").append(blockAnimationMode).append("\n");
            config.append("Crosshair:").append(crosshairMode).append("\n");
            config.append("ClickGUI:").append(guiEnabled).append("\n");
            config.append("GuiKeyCode:").append(guiKeyCode).append("\n");
            config.append("PlayerESP:").append(playerEspEnabled).append("\n");
            config.append("  ShowDistance:").append(playerEspShowDistance).append("\n");
            config.append("  ShowOwnNametag:").append(playerEspShowOwnNametag).append("\n");
            config.append("PepCat:").append(pepCatEnabled).append("\n");
            java.nio.file.Files.write(configPath, config.toString().getBytes());
        } catch (java.io.IOException e) {
            System.err.println("Failed to save Baity config: " + e.getMessage());
        }
    }

    public static void loadConfig() {
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get(CONFIG_FILE);
            if (java.nio.file.Files.exists(configPath)) {
                String content = java.nio.file.Files.readString(configPath).trim();
                String[] lines = content.split("\n");
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    if (line.startsWith("SmolPeople:")) {
                        smolpeopleMode = Boolean.parseBoolean(line.substring("SmolPeople:".length()));
                    } else if (line.startsWith("BlockAnimation:")) {
                        blockAnimationMode = Boolean.parseBoolean(line.substring("BlockAnimation:".length()));
                    } else if (line.startsWith("Crosshair:")) {
                        crosshairMode = Boolean.parseBoolean(line.substring("Crosshair:".length()));
                    } else if (line.startsWith("ClickGUI:")) {
                        guiEnabled = Boolean.parseBoolean(line.substring("ClickGUI:".length()));
                    } else if (line.startsWith("GuiKeyCode:")) {
                        guiKeyCode = Integer.parseInt(line.substring("GuiKeyCode:".length()));
                    } else if (line.startsWith("PlayerESP:")) {
                        playerEspEnabled = Boolean.parseBoolean(line.substring("PlayerESP:".length()));
                    } else if (line.startsWith("  ShowDistance:")) {
                        playerEspShowDistance = Boolean.parseBoolean(line.substring("  ShowDistance:".length()));
                    } else if (line.startsWith("  ShowOwnNametag:")) {
                        playerEspShowOwnNametag = Boolean.parseBoolean(line.substring("  ShowOwnNametag:".length()));
                    } else if (line.startsWith("PepCat:")) {
                        pepCatEnabled = Boolean.parseBoolean(line.substring("PepCat:".length()));
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load Baity config: " + e.getMessage());
        }
    }
}
