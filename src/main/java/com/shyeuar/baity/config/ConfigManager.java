package com.shyeuar.baity.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static boolean smolpeopleMode = false;
    public static double smolLimbSwingSpeed = 2.5;
    public static String smolFriendList = "";
    public static boolean smolFriendsEnabled = true;
    public static boolean blockAnimationMode = false;
    public static boolean blockAnimationInteractAnimations = true;
    public static boolean blockAnimationNoReequipWhenUsing = true;
    public static boolean crosshairMode = true;
    public static boolean guiEnabled = true;
    public static int guiKeyCode = 345;
    public static boolean nametagEnabled = false;
    public static boolean nametagShowDistance = false;
    public static boolean nametagShowOwnNametag = true;
    public static boolean nametagForcePinkColor = true;
    public static boolean nametagFocusPlayerNametag = false;
    public static boolean nametagTransparentizeOtherTags = false;
    public static boolean pepCatEnabled = false;
    public static boolean reminderEnabled = false;
    public static boolean reminderCookieBuffEnabled = true;
    public static boolean reminderGodPotionEnabled = true;
    public static boolean reminderMeowAlertEnabled = true;
    
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
    
    public static boolean fancyDmgSplashEnabled = false;
    public static boolean fancyDmgSplashGenshinReaction = false;
    public static boolean fancyDmgSplashCompactDamageNumber = true;
    public static int fancyDmgSplashColorPalette = 0;
    public static int fancyDmgSplashCritGradientStart = 0xFFFF55;
    public static int fancyDmgSplashCritGradientEnd = 0xFF5555;
    public static int fancyDmgSplashNormalDamageColor = 0xFFFFFF;
    
    public static boolean noSwimChangeEnabled = false; 
    
    public static boolean cullingEnabled = false;
    public static boolean cullingHideDyingMob = false;
    public static boolean cullingHideNonStarredNametag = false;
    public static boolean cullingRemoveUnderwaterFog = false;
    
    public static boolean skinLayer3DEnabled = false;
    
    public static boolean noHurtCamEnabled = false;
    
    public static boolean noSwapAnimationEnabled = false;
    
    public static boolean noTextShadowEnabled = false;
    
    public static boolean nodebuffEnabled = false;
    public static boolean nodebuffRemoveNausea = true;
    public static boolean nodebuffRemoveBlindness = true;
    
    public static boolean mufflerEnabled = false;
    public static boolean mufflerMuteEndermanScream = true;
    public static boolean mufflerMutePhantom = true;
    public static boolean mufflerMutePortal = true;
    public static boolean mufflerMuteVampire = true;
    public static boolean mufflerMuteDrake = true;
    
    public static boolean highlightsEnabled = false;
    public static boolean highlightsShulkerEnabled = false;
    public static boolean highlightsInvisibleBugEnabled = false;
    
    public static boolean fancyCreeperVeilEnabled = false;
    
    public static boolean twoDdroppedItemEnabled = false;
    
    public static boolean oldSneakingEnabled = false;
    
    public static boolean fishHookTimerEnabled = false;
    public static double fishHookTimerX = 0.5;
    public static double fishHookTimerY = 0.6;
    public static float fishHookTimerScale = 1.5f;

    public static boolean chromaOwnNameEnabled = false;
    public static double chromaOwnNameChromaLightness = 0.8;
    public static double chromaOwnNameChromaChroma = 0.2;
    public static double chromaOwnNameChromaSize = 3.1;
    public static double chromaOwnNameChromaSpeed = 1.0;
    
    private static final String CONFIG_FILE_NAME = "config.txt";

    private static final Map<String, SettingField> CONFIG_FIELDS = new HashMap<>();
    
    static {
        registerField("SmolPeople", Boolean.class, 
            c -> ConfigManager.smolpeopleMode, 
            (c, v) -> ConfigManager.smolpeopleMode = (Boolean) v);
        registerField("SmolLimbSwingSpeed", Double.class,
            c -> ConfigManager.smolLimbSwingSpeed,
            (c, v) -> ConfigManager.smolLimbSwingSpeed = (Double) v);
        registerField("SmolFriendList", String.class,
            c -> ConfigManager.smolFriendList,
            (c, v) -> ConfigManager.smolFriendList = (String) v);
        registerField("SmolFriendsEnabled", Boolean.class,
            c -> ConfigManager.smolFriendsEnabled,
            (c, v) -> ConfigManager.smolFriendsEnabled = (Boolean) v);
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
            c -> ConfigManager.nametagEnabled,
            (c, v) -> ConfigManager.nametagEnabled = (Boolean) v);
        registerField("  ShowDistance", Boolean.class,
            c -> ConfigManager.nametagShowDistance,
            (c, v) -> ConfigManager.nametagShowDistance = (Boolean) v);
        registerField("  ShowOwnNametag", Boolean.class,
            c -> ConfigManager.nametagShowOwnNametag,
            (c, v) -> ConfigManager.nametagShowOwnNametag = (Boolean) v);
        registerField("  ForcePinkColor", Boolean.class,
            c -> ConfigManager.nametagForcePinkColor,
            (c, v) -> ConfigManager.nametagForcePinkColor = (Boolean) v);
        registerField("  FocusPlayerNametag", Boolean.class,
            c -> ConfigManager.nametagFocusPlayerNametag,
            (c, v) -> ConfigManager.nametagFocusPlayerNametag = (Boolean) v);
        registerField("  TransparentizeOtherTags", Boolean.class,
            c -> ConfigManager.nametagTransparentizeOtherTags,
            (c, v) -> ConfigManager.nametagTransparentizeOtherTags = (Boolean) v);
        registerField("PepCat", Boolean.class,
            c -> ConfigManager.pepCatEnabled,
            (c, v) -> ConfigManager.pepCatEnabled = (Boolean) v);
        registerField("Reminder", Boolean.class,
            c -> ConfigManager.reminderEnabled,
            (c, v) -> ConfigManager.reminderEnabled = (Boolean) v);
        registerField("CookieBuffReminder", Boolean.class,
            c -> ConfigManager.reminderCookieBuffEnabled,
            (c, v) -> ConfigManager.reminderCookieBuffEnabled = (Boolean) v);
        registerField("GodPotionReminder", Boolean.class,
            c -> ConfigManager.reminderGodPotionEnabled,
            (c, v) -> ConfigManager.reminderGodPotionEnabled = (Boolean) v);
        registerField("MeowAlert", Boolean.class,
            c -> ConfigManager.reminderMeowAlertEnabled,
            (c, v) -> ConfigManager.reminderMeowAlertEnabled = (Boolean) v);
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
        registerField("NoTextShadow", Boolean.class,
            c -> ConfigManager.noTextShadowEnabled,
            (c, v) -> ConfigManager.noTextShadowEnabled = (Boolean) v);
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
        registerField("MufflerMuteVampire", Boolean.class,
            c -> ConfigManager.mufflerMuteVampire,
            (c, v) -> ConfigManager.mufflerMuteVampire = (Boolean) v);
        registerField("MufflerMuteDrake", Boolean.class,
            c -> ConfigManager.mufflerMuteDrake,
            (c, v) -> ConfigManager.mufflerMuteDrake = (Boolean) v);
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
        registerField("2DdroppedItem", Boolean.class,
            c -> ConfigManager.twoDdroppedItemEnabled,
            (c, v) -> ConfigManager.twoDdroppedItemEnabled = (Boolean) v);
        registerField("OldSneaking", Boolean.class,
            c -> ConfigManager.oldSneakingEnabled,
            (c, v) -> ConfigManager.oldSneakingEnabled = (Boolean) v);
        registerField("FishHookTimer", Boolean.class,
            c -> ConfigManager.fishHookTimerEnabled,
            (c, v) -> ConfigManager.fishHookTimerEnabled = (Boolean) v);
        registerField("FishHookTimerX", Double.class,
            c -> ConfigManager.fishHookTimerX,
            (c, v) -> ConfigManager.fishHookTimerX = (Double) v);
        registerField("FishHookTimerY", Double.class,
            c -> ConfigManager.fishHookTimerY,
            (c, v) -> ConfigManager.fishHookTimerY = (Double) v);
        registerField("FishHookTimerScale", Float.class,
            c -> ConfigManager.fishHookTimerScale,
            (c, v) -> ConfigManager.fishHookTimerScale = (Float) v);
        registerField("ChromaOwnName", Boolean.class,
            c -> ConfigManager.chromaOwnNameEnabled,
            (c, v) -> ConfigManager.chromaOwnNameEnabled = (Boolean) v);
        registerField("ChromaOwnNameChromaLightness", Double.class,
            c -> ConfigManager.chromaOwnNameChromaLightness,
            (c, v) -> ConfigManager.chromaOwnNameChromaLightness = (Double) v);
        registerField("ChromaOwnNameChromaChroma", Double.class,
            c -> ConfigManager.chromaOwnNameChromaChroma,
            (c, v) -> ConfigManager.chromaOwnNameChromaChroma = (Double) v);
        registerField("ChromaOwnNameChromaSize", Double.class,
            c -> ConfigManager.chromaOwnNameChromaSize,
            (c, v) -> ConfigManager.chromaOwnNameChromaSize = (Double) v);
        registerField("ChromaOwnNameChromaSpeed", Double.class,
            c -> ConfigManager.chromaOwnNameChromaSpeed,
            (c, v) -> ConfigManager.chromaOwnNameChromaSpeed = (Double) v);
    }
    
    private static void registerField(String key, Class<?> type,
                                    java.util.function.Function<ConfigManager, Object> getter,
                                    java.util.function.BiConsumer<ConfigManager, Object> setter) {
        CONFIG_FIELDS.put(key, new SettingField(key, getter, setter, type));
    }

    public static void saveConfig() {
        try {
            java.nio.file.Path baityDir = BaityConfigDir.getBaityConfigDir();
            if (!java.nio.file.Files.exists(baityDir)) {
                java.nio.file.Files.createDirectories(baityDir);
            }
            
            java.nio.file.Path configPath = baityDir.resolve(CONFIG_FILE_NAME);
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
            java.nio.file.Path baityDir = BaityConfigDir.getBaityConfigDir();
            if (!java.nio.file.Files.exists(baityDir)) {
                java.nio.file.Files.createDirectories(baityDir);
            }
            
            java.nio.file.Path oldConfigPath = java.nio.file.Paths.get("baity_config.txt");
            java.nio.file.Path newConfigPath = baityDir.resolve(CONFIG_FILE_NAME);
            if (java.nio.file.Files.exists(oldConfigPath) && !java.nio.file.Files.exists(newConfigPath)) {
                java.nio.file.Files.move(oldConfigPath, newConfigPath);
                System.out.println("[Baity] Migrated config to new location: " + newConfigPath);
            }
            
            java.nio.file.Path oldBaityConfigPath = java.nio.file.Paths.get("baity/config.txt");
            if (java.nio.file.Files.exists(oldBaityConfigPath) && !java.nio.file.Files.exists(newConfigPath)) {
                java.nio.file.Files.move(oldBaityConfigPath, newConfigPath);
                System.out.println("[Baity] Migrated config from old baity directory: " + newConfigPath);
            }
            
            if (java.nio.file.Files.exists(newConfigPath)) {
                String content = java.nio.file.Files.readString(newConfigPath).trim();
                String[] lines = content.split("\n");
                
                Map<String, String> legacyKeyAliases = new HashMap<>();
                legacyKeyAliases.put("  TransparentNormalTag", "  TransparentizeOtherTags");
                legacyKeyAliases.put("ColorOwnName", "ChromaOwnName");
                legacyKeyAliases.put("ColorOwnNameChromaLightness", "ChromaOwnNameChromaLightness");
                legacyKeyAliases.put("ColorOwnNameChromaChroma", "ChromaOwnNameChromaChroma");
                legacyKeyAliases.put("ColorOwnNameChromaSize", "ChromaOwnNameChromaSize");
                legacyKeyAliases.put("ColorOwnNameChromaSpeed", "ChromaOwnNameChromaSpeed");
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    int colonIdx = line.indexOf(':');
                    if (colonIdx <= 0) continue;
                    
                    String key = line.substring(0, colonIdx);
                    String valueStr = line.substring(colonIdx + 1);
                    
                    if (!CONFIG_FIELDS.containsKey(key) && legacyKeyAliases.containsKey(key)) {
                        key = legacyKeyAliases.get(key);
                    }
                    
                    SettingField field = CONFIG_FIELDS.get(key);
                    if (field == null) continue;
                    
                    ConfigManager instance = null;
                            Object value = parseValue(valueStr, field.getType());
                            field.setValue(instance, value);
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
        } else if (type == Float.class) {
            return Float.parseFloat(valueStr);
        } else if (type == String.class) {
            return valueStr;
        }
        return valueStr;
    }
}
