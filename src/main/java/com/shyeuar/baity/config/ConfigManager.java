package com.shyeuar.baity.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static boolean smolpeopleMode = false;
    public static double smolLimbSwingSpeed = 2.5;
    public static boolean blockAnimationMode = false;
    public static boolean blockAnimationInteractAnimations = true;
    public static boolean blockAnimationNoReequipWhenUsing = true;
    public static boolean crosshairMode = true;
    public static boolean guiEnabled = true;
    public static int guiKeyCode = 345;
    public static boolean playerEspEnabled = false;
    public static boolean playerEspShowDistance = true;
    public static boolean playerEspShowOwnNametag = false;
    public static boolean playerEspForcePinkColor = true;
    public static boolean pepCatEnabled = true;
    public static boolean reminderEnabled = false;
    public static boolean cookieBuffReminderEnabled = true;
    public static boolean godPotionReminderEnabled = true;
    public static boolean meowAlertEnabled = true;
    
    public static boolean customHandHoldingEnabled = false;
    public static double swingDuration = 6;
    public static double customHandHoldingPosX = 0;
    public static double customHandHoldingPosY = 0;
    public static double customHandHoldingPosZ = 0;
    public static double customHandHoldingRotX = 0;
    public static double customHandHoldingRotY = 0;
    public static double customHandHoldingRotZ = 0;
    public static double customHandHoldingScale = 1.0; 
    public static boolean customHandHoldingNoSwing = false;
    
    public static boolean radialMenuEnabled = true;
    public static int radialMenuKeybind = 4;
    
    public static boolean fancyDmgSplashEnabled = true;
    public static boolean fancyDmgSplashGenshinReaction = false;
    public static boolean fancyDmgSplashCompactDamageNumber = true;
    public static int fancyDmgSplashColorPalette = 0;
    public static int fancyDmgSplashCritGradientStart = 0xFFFF55;
    public static int fancyDmgSplashCritGradientEnd = 0xFF5555;
    public static int fancyDmgSplashNormalDamageColor = 0xFFFFFF;
    
    public static boolean noSwimChangeEnabled = true; 
    
    public static boolean cullingEnabled = false;
    public static boolean cullingHideDyingMob = false;
    public static boolean cullingHideNonStarredNametag = false;
    public static boolean cullingRemoveUnderwaterFog = false;
    
    public static boolean skinLayer3DEnabled = false;
    
    public static boolean noHurtCamEnabled = false;
    
    public static boolean noSwapAnimationEnabled = false;
    
    public static boolean nodebuffEnabled = true;
    public static boolean nodebuffRemoveNausea = true;
    public static boolean nodebuffRemoveBlindness = true;
    
    public static boolean mufflerEnabled = false;
    public static boolean mufflerMuteEndermanScream = true;
    public static boolean mufflerMutePhantom = true;
    public static boolean mufflerMutePortal = true;
    
    public static boolean highlightsEnabled = false;
    public static boolean highlightsShulkerEnabled = false;
    public static boolean highlightsInvisibleBugEnabled = false;
    
    public static boolean fancyCreeperVeilEnabled = true;
    
    private static final String BAITY_DIR = "baity";
    private static final String CONFIG_FILE = "baity/config.txt";

    private static final Map<String, SettingField> CONFIG_FIELDS = new HashMap<>();
    
    static {
        registerField("SmolPeople", Boolean.class, 
            c -> ConfigManager.smolpeopleMode, 
            (c, v) -> ConfigManager.smolpeopleMode = (Boolean) v);
        registerField("SmolLimbSwingSpeed", Double.class,
            c -> ConfigManager.smolLimbSwingSpeed,
            (c, v) -> ConfigManager.smolLimbSwingSpeed = (Double) v);
        registerField("BlockAnimation", Boolean.class,
            c -> ConfigManager.blockAnimationMode,
            (c, v) -> ConfigManager.blockAnimationMode = (Boolean) v);
        registerField("BlockAnimationInteractAnimations", Boolean.class,
            c -> ConfigManager.blockAnimationInteractAnimations,
            (c, v) -> ConfigManager.blockAnimationInteractAnimations = (Boolean) v);
        registerField("BlockAnimationNoReequipWhenUsing", Boolean.class,
            c -> ConfigManager.blockAnimationNoReequipWhenUsing,
            (c, v) -> ConfigManager.blockAnimationNoReequipWhenUsing = (Boolean) v);
        registerField("Crosshair", Boolean.class,
            c -> ConfigManager.crosshairMode,
            (c, v) -> ConfigManager.crosshairMode = (Boolean) v);
        registerField("ClickGUI", Boolean.class,
            c -> ConfigManager.guiEnabled,
            (c, v) -> ConfigManager.guiEnabled = (Boolean) v);
        registerField("GuiKeyCode", Integer.class,
            c -> ConfigManager.guiKeyCode,
            (c, v) -> ConfigManager.guiKeyCode = (Integer) v);
        registerField("Nametag", Boolean.class,
            c -> ConfigManager.playerEspEnabled,
            (c, v) -> ConfigManager.playerEspEnabled = (Boolean) v);
        registerField("  ShowDistance", Boolean.class,
            c -> ConfigManager.playerEspShowDistance,
            (c, v) -> ConfigManager.playerEspShowDistance = (Boolean) v);
        registerField("  ShowOwnNametag", Boolean.class,
            c -> ConfigManager.playerEspShowOwnNametag,
            (c, v) -> ConfigManager.playerEspShowOwnNametag = (Boolean) v);
        registerField("  ForcePinkColor", Boolean.class,
            c -> ConfigManager.playerEspForcePinkColor,
            (c, v) -> ConfigManager.playerEspForcePinkColor = (Boolean) v);
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
        registerField("CustomHandHolding", Boolean.class,
            c -> ConfigManager.customHandHoldingEnabled,
            (c, v) -> ConfigManager.customHandHoldingEnabled = (Boolean) v);
        registerField("SwingDuration", Double.class,
            c -> ConfigManager.swingDuration,
            (c, v) -> ConfigManager.swingDuration = (Double) v);
        registerField("CustomHandHoldingPosX", Double.class,
            c -> ConfigManager.customHandHoldingPosX,
            (c, v) -> ConfigManager.customHandHoldingPosX = (Double) v);
        registerField("CustomHandHoldingPosY", Double.class,
            c -> ConfigManager.customHandHoldingPosY,
            (c, v) -> ConfigManager.customHandHoldingPosY = (Double) v);
        registerField("CustomHandHoldingPosZ", Double.class,
            c -> ConfigManager.customHandHoldingPosZ,
            (c, v) -> ConfigManager.customHandHoldingPosZ = (Double) v);
        registerField("CustomHandHoldingRotX", Double.class,
            c -> ConfigManager.customHandHoldingRotX,
            (c, v) -> ConfigManager.customHandHoldingRotX = (Double) v);
        registerField("CustomHandHoldingRotY", Double.class,
            c -> ConfigManager.customHandHoldingRotY,
            (c, v) -> ConfigManager.customHandHoldingRotY = (Double) v);
        registerField("CustomHandHoldingRotZ", Double.class,
            c -> ConfigManager.customHandHoldingRotZ,
            (c, v) -> ConfigManager.customHandHoldingRotZ = (Double) v);
        registerField("CustomHandHoldingScale", Double.class,
            c -> ConfigManager.customHandHoldingScale,
            (c, v) -> ConfigManager.customHandHoldingScale = (Double) v);
        registerField("CustomHandHoldingNoSwing", Boolean.class,
            c -> ConfigManager.customHandHoldingNoSwing,
            (c, v) -> ConfigManager.customHandHoldingNoSwing = (Boolean) v);
        registerField("RadialMenu", Boolean.class,
            c -> ConfigManager.radialMenuEnabled,
            (c, v) -> ConfigManager.radialMenuEnabled = (Boolean) v);
        registerField("RadialMenuKeybind", Integer.class,
            c -> ConfigManager.radialMenuKeybind,
            (c, v) -> ConfigManager.radialMenuKeybind = (Integer) v);
        registerField("FancyDmgSplash", Boolean.class,
            c -> ConfigManager.fancyDmgSplashEnabled,
            (c, v) -> ConfigManager.fancyDmgSplashEnabled = (Boolean) v);
        registerField("FancyDmgSplashGenshinReaction", Boolean.class,
            c -> ConfigManager.fancyDmgSplashGenshinReaction,
            (c, v) -> ConfigManager.fancyDmgSplashGenshinReaction = (Boolean) v);
        registerField("FancyDmgSplashCompactDamageNumber", Boolean.class,
            c -> ConfigManager.fancyDmgSplashCompactDamageNumber,
            (c, v) -> ConfigManager.fancyDmgSplashCompactDamageNumber = (Boolean) v);
        registerField("FancyDmgSplashColorPalette", Integer.class,
            c -> ConfigManager.fancyDmgSplashColorPalette,
            (c, v) -> ConfigManager.fancyDmgSplashColorPalette = (Integer) v);
        registerField("FancyDmgSplashCritGradientStart", Integer.class,
            c -> ConfigManager.fancyDmgSplashCritGradientStart,
            (c, v) -> ConfigManager.fancyDmgSplashCritGradientStart = (Integer) v);
        registerField("FancyDmgSplashCritGradientEnd", Integer.class,
            c -> ConfigManager.fancyDmgSplashCritGradientEnd,
            (c, v) -> ConfigManager.fancyDmgSplashCritGradientEnd = (Integer) v);
        registerField("FancyDmgSplashNormalDamageColor", Integer.class,
            c -> ConfigManager.fancyDmgSplashNormalDamageColor,
            (c, v) -> ConfigManager.fancyDmgSplashNormalDamageColor = (Integer) v);
        registerField("NoSwimChange", Boolean.class,
            c -> ConfigManager.noSwimChangeEnabled,
            (c, v) -> ConfigManager.noSwimChangeEnabled = (Boolean) v);
        registerField("Culling", Boolean.class,
            c -> ConfigManager.cullingEnabled,
            (c, v) -> ConfigManager.cullingEnabled = (Boolean) v);
        registerField("CullingHideDyingMob", Boolean.class,
            c -> ConfigManager.cullingHideDyingMob,
            (c, v) -> ConfigManager.cullingHideDyingMob = (Boolean) v);
        registerField("CullingHideNonStarredNametag", Boolean.class,
            c -> ConfigManager.cullingHideNonStarredNametag,
            (c, v) -> ConfigManager.cullingHideNonStarredNametag = (Boolean) v);
        registerField("CullingRemoveUnderwaterFog", Boolean.class,
            c -> ConfigManager.cullingRemoveUnderwaterFog,
            (c, v) -> ConfigManager.cullingRemoveUnderwaterFog = (Boolean) v);
        registerField("3DSkins", Boolean.class,
            c -> ConfigManager.skinLayer3DEnabled,
            (c, v) -> ConfigManager.skinLayer3DEnabled = (Boolean) v);
        registerField("NoHurtCam", Boolean.class,
            c -> ConfigManager.noHurtCamEnabled,
            (c, v) -> ConfigManager.noHurtCamEnabled = (Boolean) v);
        registerField("NoSwapAnimation", Boolean.class,
            c -> ConfigManager.noSwapAnimationEnabled,
            (c, v) -> ConfigManager.noSwapAnimationEnabled = (Boolean) v);
        registerField("Nodebuff", Boolean.class,
            c -> ConfigManager.nodebuffEnabled,
            (c, v) -> ConfigManager.nodebuffEnabled = (Boolean) v);
        registerField("  RemoveNausea", Boolean.class,
            c -> ConfigManager.nodebuffRemoveNausea,
            (c, v) -> ConfigManager.nodebuffRemoveNausea = (Boolean) v);
        registerField("  RemoveBlindness", Boolean.class,
            c -> ConfigManager.nodebuffRemoveBlindness,
            (c, v) -> ConfigManager.nodebuffRemoveBlindness = (Boolean) v);
        registerField("Muffler", Boolean.class,
            c -> ConfigManager.mufflerEnabled,
            (c, v) -> ConfigManager.mufflerEnabled = (Boolean) v);
        registerField("MufflerMuteEndermanScream", Boolean.class,
            c -> ConfigManager.mufflerMuteEndermanScream,
            (c, v) -> ConfigManager.mufflerMuteEndermanScream = (Boolean) v);
        registerField("MufflerMutePhantom", Boolean.class,
            c -> ConfigManager.mufflerMutePhantom,
            (c, v) -> ConfigManager.mufflerMutePhantom = (Boolean) v);
        registerField("MufflerMutePortal", Boolean.class,
            c -> ConfigManager.mufflerMutePortal,
            (c, v) -> ConfigManager.mufflerMutePortal = (Boolean) v);
        registerField("Highlights", Boolean.class,
            c -> ConfigManager.highlightsEnabled,
            (c, v) -> ConfigManager.highlightsEnabled = (Boolean) v);
        registerField("  HighlightsShulker", Boolean.class,
            c -> ConfigManager.highlightsShulkerEnabled,
            (c, v) -> ConfigManager.highlightsShulkerEnabled = (Boolean) v);
        registerField("  HighlightsInvisibleBug", Boolean.class,
            c -> ConfigManager.highlightsInvisibleBugEnabled,
            (c, v) -> ConfigManager.highlightsInvisibleBugEnabled = (Boolean) v);
        registerField("FancyCreeperVeil", Boolean.class,
            c -> ConfigManager.fancyCreeperVeilEnabled,
            (c, v) -> ConfigManager.fancyCreeperVeilEnabled = (Boolean) v);
    }
    
    private static void registerField(String key, Class<?> type,
                                    java.util.function.Function<ConfigManager, Object> getter,
                                    java.util.function.BiConsumer<ConfigManager, Object> setter) {
        CONFIG_FIELDS.put(key, new SettingField(key, getter, setter, type));
    }

    public static void saveConfig() {
        try {
            java.nio.file.Path baityDir = java.nio.file.Paths.get(BAITY_DIR);
            if (!java.nio.file.Files.exists(baityDir)) {
                java.nio.file.Files.createDirectories(baityDir);
            }
            
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
            java.nio.file.Path baityDir = java.nio.file.Paths.get(BAITY_DIR);
            if (!java.nio.file.Files.exists(baityDir)) {
                java.nio.file.Files.createDirectories(baityDir);
            }
            
            java.nio.file.Path oldConfigPath = java.nio.file.Paths.get("baity_config.txt");
            java.nio.file.Path newConfigPath = java.nio.file.Paths.get(CONFIG_FILE);
            if (java.nio.file.Files.exists(oldConfigPath) && !java.nio.file.Files.exists(newConfigPath)) {
                java.nio.file.Files.move(oldConfigPath, newConfigPath);
                System.out.println("[Baity] Migrated config to new location: " + CONFIG_FILE);
            }
            
            if (java.nio.file.Files.exists(newConfigPath)) {
                String content = java.nio.file.Files.readString(newConfigPath).trim();
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
        } else if (type == Double.class) {
            return Double.parseDouble(valueStr);
        } else if (type == String.class) {
            return valueStr;
        }
        return valueStr;
    }
}
