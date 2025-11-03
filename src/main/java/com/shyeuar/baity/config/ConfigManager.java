package com.shyeuar.baity.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static boolean smolpeopleMode = false;
    public static boolean blockAnimationMode = false;
    public static boolean crosshairMode = true;
    public static boolean guiEnabled = true;
    public static int guiKeyCode = 345;
    public static boolean playerEspEnabled = false;
    public static boolean playerEspShowDistance = true;
    public static boolean playerEspShowOwnNametag = false;
    public static boolean pepCatEnabled = true;
    public static boolean reminderEnabled = false;
    public static boolean cookieBuffReminderEnabled = true;
    public static boolean godPotionReminderEnabled = true;
    public static boolean meowAlertEnabled = true;
    private static final String CONFIG_FILE = "baity_config.txt";

    private static final Map<String, SettingField> CONFIG_FIELDS = new HashMap<>();
    
    static {
        registerField("SmolPeople", Boolean.class, 
            c -> ConfigManager.smolpeopleMode, 
            (c, v) -> ConfigManager.smolpeopleMode = (Boolean) v);
        registerField("BlockAnimation", Boolean.class,
            c -> ConfigManager.blockAnimationMode,
            (c, v) -> ConfigManager.blockAnimationMode = (Boolean) v);
        registerField("Crosshair", Boolean.class,
            c -> ConfigManager.crosshairMode,
            (c, v) -> ConfigManager.crosshairMode = (Boolean) v);
        registerField("ClickGUI", Boolean.class,
            c -> ConfigManager.guiEnabled,
            (c, v) -> ConfigManager.guiEnabled = (Boolean) v);
        registerField("GuiKeyCode", Integer.class,
            c -> ConfigManager.guiKeyCode,
            (c, v) -> ConfigManager.guiKeyCode = (Integer) v);
        registerField("PlayerESP", Boolean.class,
            c -> ConfigManager.playerEspEnabled,
            (c, v) -> ConfigManager.playerEspEnabled = (Boolean) v);
        registerField("  ShowDistance", Boolean.class,
            c -> ConfigManager.playerEspShowDistance,
            (c, v) -> ConfigManager.playerEspShowDistance = (Boolean) v);
        registerField("  ShowOwnNametag", Boolean.class,
            c -> ConfigManager.playerEspShowOwnNametag,
            (c, v) -> ConfigManager.playerEspShowOwnNametag = (Boolean) v);
        registerField("PepCat", Boolean.class,
            c -> ConfigManager.pepCatEnabled,
            (c, v) -> ConfigManager.pepCatEnabled = (Boolean) v);
        registerField("Reminder", Boolean.class,
            c -> ConfigManager.reminderEnabled,
            (c, v) -> ConfigManager.reminderEnabled = (Boolean) v);
        registerField("CookieBuffReminder", Boolean.class,
            c -> ConfigManager.cookieBuffReminderEnabled,
            (c, v) -> ConfigManager.cookieBuffReminderEnabled = (Boolean) v);
        registerField("GodPotionReminder", Boolean.class,
            c -> ConfigManager.godPotionReminderEnabled,
            (c, v) -> ConfigManager.godPotionReminderEnabled = (Boolean) v);
        registerField("MeowAlert", Boolean.class,
            c -> ConfigManager.meowAlertEnabled,
            (c, v) -> ConfigManager.meowAlertEnabled = (Boolean) v);
    }
    
    private static void registerField(String key, Class<?> type,
                                    java.util.function.Function<ConfigManager, Object> getter,
                                    java.util.function.BiConsumer<ConfigManager, Object> setter) {
        CONFIG_FIELDS.put(key, new SettingField(key, getter, setter, type));
    }

    public static void saveConfig() {
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get(CONFIG_FILE);
            StringBuilder config = new StringBuilder();
            
            ConfigManager instance = null; 
            for (Map.Entry<String, SettingField> entry : CONFIG_FIELDS.entrySet()) {
                String key = entry.getKey();
                SettingField field = entry.getValue();
                Object value = field.getValue(instance);
                config.append(key).append(":").append(value).append("\n");
            }
            
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
                    
                    ConfigManager instance = null; 
                    for (Map.Entry<String, SettingField> entry : CONFIG_FIELDS.entrySet()) {
                        String key = entry.getKey();
                        if (line.startsWith(key + ":")) {
                            SettingField field = entry.getValue();
                            String valueStr = line.substring(key.length() + 1);
                            Object value = parseValue(valueStr, field.getType());
                            field.setValue(instance, value);
                            break;
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load Baity config: " + e.getMessage());
        }
    }
    
    private static Object parseValue(String valueStr, Class<?> type) {
        if (type == Boolean.class) {
            return Boolean.parseBoolean(valueStr);
        } else if (type == Integer.class) {
            return Integer.parseInt(valueStr);
        } else if (type == String.class) {
            return valueStr;
        }
        return valueStr;
    }
}


