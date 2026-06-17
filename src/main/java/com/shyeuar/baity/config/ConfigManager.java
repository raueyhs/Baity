package com.shyeuar.baity.config;

import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/Config");
    private static final long SAVE_DEBOUNCE_MS = 750L;

    public static boolean smolpeopleMode = false;
    public static double smolLimbSwingSpeed = 2.5;
    public static String smolFriendList = "";
    public static boolean smolFriendsEnabled = true;
    public static boolean blockAnimationMode = false;
    public static boolean blockAnimationInteractAnimations = true;
    public static boolean blockAnimationNoReequipWhenUsing = true;
    public static String blockAnimationAnimaMode = "default";
    public static boolean crosshairEnabled = false;
    public static boolean customCrosshairEnabled = false;
    public static boolean thirdPersonBackCrosshairEnabled = true;
    public static boolean crosshairChromaEnabled = false;
    public static String crosshairAnimaMode = "always";
    public static String crosshairStaticLayer = "";
    public static String crosshairActiveLayer = "";
    public static boolean crosshairPainterInitialized = false;
    public static boolean guiEnabled = true;
    public static int guiKeyCode = 345;
    public static boolean nametagEnabled = false;
    public static boolean nametagShowDistance = false;
    public static boolean nametagShowOwnNametag = true;
    public static boolean nametagForcePinkColor = true;
    public static boolean nametagFocusPlayerNametag = false;
    public static boolean nametagTransparentizeOtherTags = false;
    public static boolean nametagOptionsGroupExpanded = false;
    public static boolean pepCatEnabled = false;
    public static boolean reminderEnabled = false;
    public static boolean reminderCookieBuffEnabled = true;
    public static boolean reminderGodPotionEnabled = true;
    
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
    public static boolean chatChannelSwitcherEnabled = false;
    public static String chatChannelSwitcherLastChannel = "";
    public static double chatChannelSwitcherX = Double.NaN;
    public static double chatChannelSwitcherY = Double.NaN;
    public static float chatChannelSwitcherScale = 1.0f;
    public static boolean chatChannelSwitcherHintHidden = false;
    public static long chatChannelSwitcherLastProcessExitAtMs = 0L;
    
    public static boolean fancyDmgSplashEnabled = false;
    public static boolean fancyDmgSplashGenshinReaction = false;
    public static boolean fancyDmgSplashSyncNonCritical = false;
    public static boolean fancyDmgSplashCompactDamageNumber = true;
    public static int fancyDmgSplashBuiltinPresetMask = 0;
    public static int fancyDmgSplashCustomPresetMask = 1;
    public static String fancyDmgSplashCustomPresets = "";
    public static int fancyDmgSplashEditingCustomIndex = 0;
    public static int fancyDmgSplashCritGradientStart = 0xFFFF55;
    public static int fancyDmgSplashCritGradientEnd = 0xFF5555;
    public static int fancyDmgSplashNormalDamageColor = 0xFFFFFF;
    public static String fancyDmgSplashDamageSymbols = "✧";
    public static boolean fancyDmgSplashBold = false;
    public static String fancyDmgSplashSeparator = "none";
    
    public static boolean noSwimPoseEnabled = false; 
    
    public static boolean cullingEnabled = false;
    public static boolean cullingHideDyingMob = true;
    public static boolean cullingHideNonStarredNametag = true;
    public static boolean cullingRemoveUnderwaterFog = true;
    public static boolean cullingRemoveRainSnow = true;
    
    public static boolean skinLayer3DEnabled = false;
    
    public static boolean noHurtCamEnabled = false;
    
    public static boolean heldItemTweaksEnabled = false;
    public static boolean heldItemTweaksNoItemswapAnimationEnabled = false;
    public static boolean heldItemTweaksNoArmSwayEnabled = false;
    
    public static boolean noTextShadowEnabled = false;
    
    public static boolean nodebuffEnabled = false;
    public static boolean nodebuffRemoveNausea = true;
    public static boolean nodebuffRemoveBlindness = true;

    public static boolean vanillaHudHiderEnabled = false;
    public static boolean vanillaHudHiderArmorBar = false;
    public static boolean vanillaHudHiderHealthBar = false;
    public static boolean vanillaHudHiderFoodBar = false;
    public static boolean vanillaHudHiderAirBar = false;
    public static boolean vanillaHudHiderMountHealth = false;
    public static boolean vanillaHudHiderExperienceBar = false;

    public static boolean chromaFishingLineEnabled = false;
    public static int chromaFishingLineGradientStart = 0x000000;
    public static int chromaFishingLineGradientEnd = 0x000000;
    public static boolean chromaFishingLineChromaEnabled = false;
    public static double chromaFishingLineChromaLightness = 0.8;
    public static double chromaFishingLineChromaChroma = 0.2;
    public static double chromaFishingLineChromaSize = 3.1;
    public static double chromaFishingLineChromaSpeed = 1.0;
    public static boolean chromaFishingLineChromaReverseDirection = false;
    public static boolean chromaFishingLineChromaGroupExpanded = false;
    
    public static boolean soundsEnabled = true;
    
    public static boolean mufflerEnabled = false;
    public static boolean mufflerMuteEndermanScream = true;
    public static boolean mufflerMutePhantom = true;
    public static boolean mufflerMutePortal = true;
    public static boolean mufflerMuteVampire = true;
    public static boolean mufflerMuteDrake = true;
    
    public static boolean highlightsEnabled = false;
    public static boolean highlightsShulkerEnabled = true;
    public static boolean highlightsInvisibugEnabled = true;
    public static boolean highlightsPestEnabled = true;
    public static boolean highlightsPestDrawLineEnabled = true;
    public static boolean highlightsPestGroupExpanded = false;
    
    public static boolean fancyCreeperVeilEnabled = false;
    
    public static boolean droppedItemEnabled = false;

    public static boolean twoDdroppedItemEnabled = false;

    public static boolean droppedItemRarityScaleGroupExpanded = false;

    public static boolean droppedItemRarityScaleEnabled = true;

    public static final double[] DROPPED_ITEM_RARITY_SCALE_DEFAULTS = {
            1.0, 1.25, 1.65, 1.95, 2.45, 2.80, 3.10, 2.20, 2.20, 3.50, 3.50
    };

    public static double droppedItemRarityCommon = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[0];
    public static double droppedItemRarityUncommon = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[1];
    public static double droppedItemRarityRare = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[2];
    public static double droppedItemRarityEpic = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[3];
    public static double droppedItemRarityLegendary = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[4];
    public static double droppedItemRarityMythic = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[5];
    public static double droppedItemRarityDivine = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[6];
    public static double droppedItemRaritySpecial = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[7];
    public static double droppedItemRarityVerySpecial = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[8];
    public static double droppedItemRarityUltimate = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[9];
    public static double droppedItemRarityAdmin = DROPPED_ITEM_RARITY_SCALE_DEFAULTS[10];
    
    public static boolean oldSneakingEnabled = false;
    
    public static boolean fishHookTimerEnabled = false;
    public static boolean fishHookTimerHideDefaultTimer = true;
    public static double fishHookTimerX = 0.5;
    public static double fishHookTimerY = 0.6;
    public static float fishHookTimerScale = 1.5f;

    public static boolean nickTweaksEnabled = false;
    public static String nickTweaksNickChanger = "";
    public static boolean nickTweaksChromaEnabled = false;
    public static boolean nickTweaksCustomNickColorEnabled = false;
    public static boolean nickTweaksBoldSelf = false;
    public static double nickTweaksChromaLightness = 0.8;
    public static double nickTweaksChromaChroma = 0.2;
    public static double nickTweaksChromaSize = 3.1;
    public static double nickTweaksChromaSpeed = 1.0;
    public static boolean nickTweaksChromaGroupExpanded = false;
    public static int nickTweaksGradientStartColor = 0xFFFFFF;
    public static int nickTweaksGradientEndColor = 0xFFFFFF;

    public static boolean enchantLoreEnabled = false;
    public static String enchantLoreColorData = "";
    public static int[] enchantLoreTierStartColor;
    public static int[] enchantLoreTierEndColor;
    public static boolean[] enchantLoreTierBold;
    public static boolean[] enchantLoreTierRainbow;
    public static boolean enchantLoreRainbowGroupExpanded = false;
    public static double enchantLoreRainbowSpeed = 1.0;
    public static double enchantLoreRainbowSaturation = 0.8;
    public static int enchantLoreRainbowGradient = 100;
    public static int enchantLoreRainbowAngle = 45;
    public static boolean enchantLoreRomanNumeralsGroupExpanded = false;
    public static boolean enchantLoreArabicNumerals = false;
    public static boolean enchantLoreDontReplaceRomanInItemName = false;
    public static String enchantLoreLayoutMode = "normal";
    public static boolean nametagDefaultNametag = false;
    public static String baityPresenceSyncUrl = "https://baity-presence-sync.1427637445.workers.dev/users.json";
    public static boolean baityPresenceSyncEnabled = true;
    public static String baityPresenceReportUrl = "";
    public static String baityPresenceReportToken = "";
    public static boolean baityPresenceSyncNotificationEnabled = true;
    public static String baityPresenceProxyHost = "";
    public static int baityPresenceProxyPort = 0;
    public static String baityPresenceProxyAuth = "";
    public static boolean baityPresenceProxyFallbackDirect = true;
    public static String baityPresenceProxySource = "none";
    
    
    
    private static final String CONFIG_FILE_NAME = "config.txt";

    private static final Map<String, SettingField> CONFIG_FIELDS = new HashMap<>();
    private static boolean pendingSave = false;
    private static long pendingSaveAt = 0L;
    
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
        registerField("BlockAnimationAnimaMode", String.class,
            c -> ConfigManager.blockAnimationAnimaMode,
            (c, v) -> ConfigManager.blockAnimationAnimaMode = (String) v);
        registerField("CrosshairEnabled", Boolean.class,
            c -> ConfigManager.crosshairEnabled,
            (c, v) -> ConfigManager.crosshairEnabled = (Boolean) v);
        registerField("CustomCrosshairEnabled", Boolean.class,
            c -> ConfigManager.customCrosshairEnabled,
            (c, v) -> ConfigManager.customCrosshairEnabled = (Boolean) v);
        registerField("ThirdPersonBackCrosshair", Boolean.class,
            c -> ConfigManager.thirdPersonBackCrosshairEnabled,
            (c, v) -> ConfigManager.thirdPersonBackCrosshairEnabled = (Boolean) v);
        registerField("CrosshairChromaEnabled", Boolean.class,
            c -> ConfigManager.crosshairChromaEnabled,
            (c, v) -> ConfigManager.crosshairChromaEnabled = (Boolean) v);
        registerField("CrosshairAnimaMode", String.class,
            c -> ConfigManager.crosshairAnimaMode,
            (c, v) -> ConfigManager.crosshairAnimaMode = (String) v);
        registerField("CrosshairStaticLayer", String.class,
            c -> ConfigManager.crosshairStaticLayer,
            (c, v) -> ConfigManager.crosshairStaticLayer = (String) v);
        registerField("CrosshairActiveLayer", String.class,
            c -> ConfigManager.crosshairActiveLayer,
            (c, v) -> ConfigManager.crosshairActiveLayer = (String) v);
        registerField("CrosshairPainterInitialized", Boolean.class,
            c -> ConfigManager.crosshairPainterInitialized,
            (c, v) -> ConfigManager.crosshairPainterInitialized = (Boolean) v);
        registerField("ClickGUI", Boolean.class,
            c -> ConfigManager.guiEnabled,
            (c, v) -> ConfigManager.guiEnabled = (Boolean) v);
        registerField("GuiKeyCode", Integer.class,
            c -> ConfigManager.guiKeyCode,
            (c, v) -> ConfigManager.guiKeyCode = (Integer) v);
        registerField("Nametag", Boolean.class,
            c -> ConfigManager.nametagEnabled,
            (c, v) -> ConfigManager.nametagEnabled = (Boolean) v);
        registerField("  DefaultNametag", Boolean.class,
            c -> ConfigManager.nametagDefaultNametag,
            (c, v) -> ConfigManager.nametagDefaultNametag = (Boolean) v);
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
        registerField("NametagOptionsGroupExpanded", Boolean.class,
            c -> ConfigManager.nametagOptionsGroupExpanded,
            (c, v) -> ConfigManager.nametagOptionsGroupExpanded = (Boolean) v);
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
        registerField("ChatChannelSwitcher", Boolean.class,
            c -> ConfigManager.chatChannelSwitcherEnabled,
            (c, v) -> ConfigManager.chatChannelSwitcherEnabled = (Boolean) v);
        registerField("ChatChannelSwitcherLastChannel", String.class,
            c -> ConfigManager.chatChannelSwitcherLastChannel,
            (c, v) -> ConfigManager.chatChannelSwitcherLastChannel = (String) v);
        registerField("ChatChannelSwitcherX", Double.class,
            c -> ConfigManager.chatChannelSwitcherX,
            (c, v) -> ConfigManager.chatChannelSwitcherX = (Double) v);
        registerField("ChatChannelSwitcherY", Double.class,
            c -> ConfigManager.chatChannelSwitcherY,
            (c, v) -> ConfigManager.chatChannelSwitcherY = (Double) v);
        registerField("ChatChannelSwitcherScale", Float.class,
            c -> ConfigManager.chatChannelSwitcherScale,
            (c, v) -> ConfigManager.chatChannelSwitcherScale = (Float) v);
        registerField("ChatChannelSwitcherHintHidden", Boolean.class,
            c -> ConfigManager.chatChannelSwitcherHintHidden,
            (c, v) -> ConfigManager.chatChannelSwitcherHintHidden = (Boolean) v);
        registerField("ChatChannelSwitcherLastProcessExitAtMs", Long.class,
            c -> ConfigManager.chatChannelSwitcherLastProcessExitAtMs,
            (c, v) -> ConfigManager.chatChannelSwitcherLastProcessExitAtMs = (Long) v);
        registerField("FancyDmgSplash", Boolean.class,
            c -> ConfigManager.fancyDmgSplashEnabled,
            (c, v) -> ConfigManager.fancyDmgSplashEnabled = (Boolean) v);
        registerField("FancyDmgSplashGenshinReaction", Boolean.class,
            c -> ConfigManager.fancyDmgSplashGenshinReaction,
            (c, v) -> ConfigManager.fancyDmgSplashGenshinReaction = (Boolean) v);
        registerField("FancyDmgSplashSyncNonCritical", Boolean.class,
            c -> ConfigManager.fancyDmgSplashSyncNonCritical,
            (c, v) -> ConfigManager.fancyDmgSplashSyncNonCritical = (Boolean) v);
        registerField("FancyDmgSplashCompactDamageNumber", Boolean.class,
            c -> ConfigManager.fancyDmgSplashCompactDamageNumber,
            (c, v) -> ConfigManager.fancyDmgSplashCompactDamageNumber = (Boolean) v);
        registerField("FancyDmgSplashBuiltinPresetMask", Integer.class,
            c -> ConfigManager.fancyDmgSplashBuiltinPresetMask,
            (c, v) -> ConfigManager.fancyDmgSplashBuiltinPresetMask = (Integer) v);
        registerField("FancyDmgSplashCustomPresetMask", Integer.class,
            c -> ConfigManager.fancyDmgSplashCustomPresetMask,
            (c, v) -> ConfigManager.fancyDmgSplashCustomPresetMask = (Integer) v);
        registerField("FancyDmgSplashCustomPresets", String.class,
            c -> ConfigManager.fancyDmgSplashCustomPresets,
            (c, v) -> {
                ConfigManager.fancyDmgSplashCustomPresets = (String) v;
                com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.decodeCustomPresets((String) v);
            });
        registerField("FancyDmgSplashEditingCustomIndex", Integer.class,
            c -> ConfigManager.fancyDmgSplashEditingCustomIndex,
            (c, v) -> ConfigManager.fancyDmgSplashEditingCustomIndex = (Integer) v);
        registerField("FancyDmgSplashPreset", Long.class,
            c -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.packPaletteConfig(),
            (c, v) -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.unpackPaletteConfig(((Number) v).longValue()));
        registerField("FancyDmgSplashCritGradientStart", Integer.class,
            c -> ConfigManager.fancyDmgSplashCritGradientStart,
            (c, v) -> ConfigManager.fancyDmgSplashCritGradientStart = (Integer) v);
        registerField("FancyDmgSplashCritGradientEnd", Integer.class,
            c -> ConfigManager.fancyDmgSplashCritGradientEnd,
            (c, v) -> ConfigManager.fancyDmgSplashCritGradientEnd = (Integer) v);
        registerField("FancyDmgSplashNormalDamageColor", Integer.class,
            c -> ConfigManager.fancyDmgSplashNormalDamageColor,
            (c, v) -> ConfigManager.fancyDmgSplashNormalDamageColor = (Integer) v);
        registerField("FancyDmgSplashDamageSymbols", String.class,
            c -> ConfigManager.fancyDmgSplashDamageSymbols,
            (c, v) -> ConfigManager.fancyDmgSplashDamageSymbols = (String) v);
        registerField("FancyDmgSplashBold", Boolean.class,
            c -> ConfigManager.fancyDmgSplashBold,
            (c, v) -> ConfigManager.fancyDmgSplashBold = (Boolean) v);
        registerField("FancyDmgSplashSeparator", String.class,
            c -> ConfigManager.fancyDmgSplashSeparator,
            (c, v) -> ConfigManager.fancyDmgSplashSeparator = (String) v);
        registerField("FancyDmgSplashColorEditor", String.class,
            c -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.encodeColorEditor(),
            (c, v) -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.decodeColorEditor((String) v));
        registerField("NoSwimPose", Boolean.class,
            c -> ConfigManager.noSwimPoseEnabled,
            (c, v) -> ConfigManager.noSwimPoseEnabled = (Boolean) v);
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
        registerField("CullingRemoveRainSnow", Boolean.class,
            c -> ConfigManager.cullingRemoveRainSnow,
            (c, v) -> ConfigManager.cullingRemoveRainSnow = (Boolean) v);
        registerField("3DSkins", Boolean.class,
            c -> ConfigManager.skinLayer3DEnabled,
            (c, v) -> ConfigManager.skinLayer3DEnabled = (Boolean) v);
        registerField("NoHurtCam", Boolean.class,
            c -> ConfigManager.noHurtCamEnabled,
            (c, v) -> ConfigManager.noHurtCamEnabled = (Boolean) v);
        registerField("HeldItemTweaks", Boolean.class,
            c -> ConfigManager.heldItemTweaksEnabled,
            (c, v) -> ConfigManager.heldItemTweaksEnabled = (Boolean) v);
        registerField("no itemswap animation", Boolean.class,
            c -> ConfigManager.heldItemTweaksNoItemswapAnimationEnabled,
            (c, v) -> ConfigManager.heldItemTweaksNoItemswapAnimationEnabled = (Boolean) v);
        registerField("no arm sway", Boolean.class,
            c -> ConfigManager.heldItemTweaksNoArmSwayEnabled,
            (c, v) -> ConfigManager.heldItemTweaksNoArmSwayEnabled = (Boolean) v);
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
        registerField("VanillaHudHider", Boolean.class,
            c -> ConfigManager.vanillaHudHiderEnabled,
            (c, v) -> ConfigManager.vanillaHudHiderEnabled = (Boolean) v);
        registerField("  ArmorBar", Boolean.class,
            c -> ConfigManager.vanillaHudHiderArmorBar,
            (c, v) -> ConfigManager.vanillaHudHiderArmorBar = (Boolean) v);
        registerField("  HealthBar", Boolean.class,
            c -> ConfigManager.vanillaHudHiderHealthBar,
            (c, v) -> ConfigManager.vanillaHudHiderHealthBar = (Boolean) v);
        registerField("  FoodBar", Boolean.class,
            c -> ConfigManager.vanillaHudHiderFoodBar,
            (c, v) -> ConfigManager.vanillaHudHiderFoodBar = (Boolean) v);
        registerField("  AirBar", Boolean.class,
            c -> ConfigManager.vanillaHudHiderAirBar,
            (c, v) -> ConfigManager.vanillaHudHiderAirBar = (Boolean) v);
        registerField("  MountHealth", Boolean.class,
            c -> ConfigManager.vanillaHudHiderMountHealth,
            (c, v) -> ConfigManager.vanillaHudHiderMountHealth = (Boolean) v);
        registerField("  ExperienceBar", Boolean.class,
            c -> ConfigManager.vanillaHudHiderExperienceBar,
            (c, v) -> ConfigManager.vanillaHudHiderExperienceBar = (Boolean) v);
        registerField("ChromaFishingLine", Boolean.class,
            c -> ConfigManager.chromaFishingLineEnabled,
            (c, v) -> ConfigManager.chromaFishingLineEnabled = (Boolean) v);
        registerField("ChromaFishingLineGradientStart", Integer.class,
            c -> ConfigManager.chromaFishingLineGradientStart,
            (c, v) -> ConfigManager.chromaFishingLineGradientStart = (Integer) v);
        registerField("ChromaFishingLineGradientEnd", Integer.class,
            c -> ConfigManager.chromaFishingLineGradientEnd,
            (c, v) -> ConfigManager.chromaFishingLineGradientEnd = (Integer) v);
        registerField("ChromaFishingLineChromaEnabled", Boolean.class,
            c -> ConfigManager.chromaFishingLineChromaEnabled,
            (c, v) -> ConfigManager.chromaFishingLineChromaEnabled = (Boolean) v);
        registerField("ChromaFishingLineChromaLightness", Double.class,
            c -> ConfigManager.chromaFishingLineChromaLightness,
            (c, v) -> ConfigManager.chromaFishingLineChromaLightness = (Double) v);
        registerField("ChromaFishingLineChromaChroma", Double.class,
            c -> ConfigManager.chromaFishingLineChromaChroma,
            (c, v) -> ConfigManager.chromaFishingLineChromaChroma = (Double) v);
        registerField("ChromaFishingLineChromaSize", Double.class,
            c -> ConfigManager.chromaFishingLineChromaSize,
            (c, v) -> ConfigManager.chromaFishingLineChromaSize = (Double) v);
        registerField("ChromaFishingLineChromaSpeed", Double.class,
            c -> ConfigManager.chromaFishingLineChromaSpeed,
            (c, v) -> ConfigManager.chromaFishingLineChromaSpeed = (Double) v);
        registerField("ChromaFishingLineChromaReverseDirection", Boolean.class,
            c -> ConfigManager.chromaFishingLineChromaReverseDirection,
            (c, v) -> ConfigManager.chromaFishingLineChromaReverseDirection = (Boolean) v);
        registerField("ChromaFishingLineChromaGroupExpanded", Boolean.class,
            c -> ConfigManager.chromaFishingLineChromaGroupExpanded,
            (c, v) -> ConfigManager.chromaFishingLineChromaGroupExpanded = (Boolean) v);
        registerField("Sounds", Boolean.class,
            c -> ConfigManager.soundsEnabled,
            (c, v) -> ConfigManager.soundsEnabled = (Boolean) v);
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
        registerField("  HighlightsInvisibug", Boolean.class,
            c -> ConfigManager.highlightsInvisibugEnabled,
            (c, v) -> ConfigManager.highlightsInvisibugEnabled = (Boolean) v);
        registerField("  HighlightsPest", Boolean.class,
            c -> ConfigManager.highlightsPestEnabled,
            (c, v) -> ConfigManager.highlightsPestEnabled = (Boolean) v);
        registerField("  HighlightsPestDrawLine", Boolean.class,
            c -> ConfigManager.highlightsPestDrawLineEnabled,
            (c, v) -> ConfigManager.highlightsPestDrawLineEnabled = (Boolean) v);
        registerField("  HighlightsPestGroupExpanded", Boolean.class,
            c -> ConfigManager.highlightsPestGroupExpanded,
            (c, v) -> ConfigManager.highlightsPestGroupExpanded = (Boolean) v);
        registerField("FancyCreeperVeil", Boolean.class,
            c -> ConfigManager.fancyCreeperVeilEnabled,
            (c, v) -> ConfigManager.fancyCreeperVeilEnabled = (Boolean) v);
        registerField("DroppedItem", Boolean.class,
            c -> ConfigManager.droppedItemEnabled,
            (c, v) -> ConfigManager.droppedItemEnabled = (Boolean) v);
        registerField("2DdroppedItem", Boolean.class,
            c -> ConfigManager.twoDdroppedItemEnabled,
            (c, v) -> ConfigManager.twoDdroppedItemEnabled = (Boolean) v);
        registerField("DroppedItemRarityScale", Boolean.class,
            c -> ConfigManager.droppedItemRarityScaleGroupExpanded,
            (c, v) -> ConfigManager.droppedItemRarityScaleGroupExpanded = (Boolean) v);
        registerField("DroppedItemRarityScaleEnabled", Boolean.class,
            c -> ConfigManager.droppedItemRarityScaleEnabled,
            (c, v) -> ConfigManager.droppedItemRarityScaleEnabled = (Boolean) v);
        registerDroppedItemRarityScaleFields();
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
        registerField("FishHookTimerHideDefaultTimer", Boolean.class,
            c -> ConfigManager.fishHookTimerHideDefaultTimer,
            (c, v) -> ConfigManager.fishHookTimerHideDefaultTimer = (Boolean) v);
        registerField("NickTweaks", Boolean.class,
            c -> ConfigManager.nickTweaksEnabled,
            (c, v) -> ConfigManager.nickTweaksEnabled = (Boolean) v);
        registerField("NickTweaksNickChanger", String.class,
            c -> ConfigManager.nickTweaksNickChanger,
            (c, v) -> ConfigManager.nickTweaksNickChanger = (String) v);
        registerField("NickTweaksChromaEnabled", Boolean.class,
            c -> ConfigManager.nickTweaksChromaEnabled,
            (c, v) -> ConfigManager.nickTweaksChromaEnabled = (Boolean) v);
        registerField("NickTweaksCustomNickColorEnabled", Boolean.class,
            c -> ConfigManager.nickTweaksCustomNickColorEnabled,
            (c, v) -> ConfigManager.nickTweaksCustomNickColorEnabled = (Boolean) v);
        registerField("NickTweaksBoldSelf", Boolean.class,
            c -> ConfigManager.nickTweaksBoldSelf,
            (c, v) -> ConfigManager.nickTweaksBoldSelf = (Boolean) v);
        registerField("NickTweaksChromaLightness", Double.class,
            c -> ConfigManager.nickTweaksChromaLightness,
            (c, v) -> ConfigManager.nickTweaksChromaLightness = (Double) v);
        registerField("NickTweaksChromaChroma", Double.class,
            c -> ConfigManager.nickTweaksChromaChroma,
            (c, v) -> ConfigManager.nickTweaksChromaChroma = (Double) v);
        registerField("NickTweaksChromaSize", Double.class,
            c -> ConfigManager.nickTweaksChromaSize,
            (c, v) -> ConfigManager.nickTweaksChromaSize = (Double) v);
        registerField("NickTweaksChromaSpeed", Double.class,
            c -> ConfigManager.nickTweaksChromaSpeed,
            (c, v) -> ConfigManager.nickTweaksChromaSpeed = (Double) v);
        registerField("NickTweaksChromaGroupExpanded", Boolean.class,
            c -> ConfigManager.nickTweaksChromaGroupExpanded,
            (c, v) -> ConfigManager.nickTweaksChromaGroupExpanded = (Boolean) v);
        registerField("NickTweaksGradientStartColor", Integer.class,
            c -> ConfigManager.nickTweaksGradientStartColor,
            (c, v) -> ConfigManager.nickTweaksGradientStartColor = (Integer) v);
        registerField("NickTweaksGradientEndColor", Integer.class,
            c -> ConfigManager.nickTweaksGradientEndColor,
            (c, v) -> ConfigManager.nickTweaksGradientEndColor = (Integer) v);
        registerField("EnchantLore", Boolean.class,
            c -> ConfigManager.enchantLoreEnabled,
            (c, v) -> ConfigManager.enchantLoreEnabled = (Boolean) v);
        registerField("EnchantLoreColorData", String.class,
            c -> com.shyeuar.baity.features.enchantlore.EnchantLoreColorSettings.encode(),
            (c, v) -> {
                ConfigManager.enchantLoreColorData = (String) v;
                com.shyeuar.baity.features.enchantlore.EnchantLoreColorSettings.decode(ConfigManager.enchantLoreColorData);
            });
        registerField("EnchantLoreRainbowGroupExpanded", Boolean.class,
            c -> ConfigManager.enchantLoreRainbowGroupExpanded,
            (c, v) -> ConfigManager.enchantLoreRainbowGroupExpanded = (Boolean) v);
        registerField("EnchantLoreRainbowSpeed", Double.class,
            c -> ConfigManager.enchantLoreRainbowSpeed,
            (c, v) -> ConfigManager.enchantLoreRainbowSpeed = (Double) v);
        registerField("EnchantLoreRainbowSaturation", Double.class,
            c -> ConfigManager.enchantLoreRainbowSaturation,
            (c, v) -> ConfigManager.enchantLoreRainbowSaturation = (Double) v);
        registerField("EnchantLoreRainbowGradient", Integer.class,
            c -> ConfigManager.enchantLoreRainbowGradient,
            (c, v) -> ConfigManager.enchantLoreRainbowGradient = (Integer) v);
        registerField("EnchantLoreRainbowAngle", Integer.class,
            c -> ConfigManager.enchantLoreRainbowAngle,
            (c, v) -> ConfigManager.enchantLoreRainbowAngle = (Integer) v);
        registerField("EnchantLoreRomanNumeralsGroupExpanded", Boolean.class,
            c -> ConfigManager.enchantLoreRomanNumeralsGroupExpanded,
            (c, v) -> ConfigManager.enchantLoreRomanNumeralsGroupExpanded = (Boolean) v);
        registerField("EnchantLoreArabicNumerals", Boolean.class,
            c -> ConfigManager.enchantLoreArabicNumerals,
            (c, v) -> {
                ConfigManager.enchantLoreArabicNumerals = (Boolean) v;
                com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
            });
        registerField("EnchantLoreDontReplaceRomanInItemName", Boolean.class,
            c -> ConfigManager.enchantLoreDontReplaceRomanInItemName,
            (c, v) -> {
                ConfigManager.enchantLoreDontReplaceRomanInItemName = (Boolean) v;
                com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
            });
        registerField("EnchantLoreLayoutMode", String.class,
            c -> ConfigManager.enchantLoreLayoutMode,
            (c, v) -> {
                ConfigManager.enchantLoreLayoutMode = (String) v;
                com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
            });
        registerField("BaityPresenceSyncUrl", String.class,
            c -> ConfigManager.baityPresenceSyncUrl,
            (c, v) -> ConfigManager.baityPresenceSyncUrl = (String) v);
        registerField("BaityPresenceSyncEnabled", Boolean.class,
            c -> ConfigManager.baityPresenceSyncEnabled,
            (c, v) -> ConfigManager.baityPresenceSyncEnabled = (Boolean) v);
        registerField("BaityPresenceReportUrl", String.class,
            c -> ConfigManager.baityPresenceReportUrl,
            (c, v) -> ConfigManager.baityPresenceReportUrl = (String) v);
        registerField("BaityPresenceReportToken", String.class,
            c -> ConfigManager.baityPresenceReportToken,
            (c, v) -> ConfigManager.baityPresenceReportToken = (String) v);
        registerField("BaityPresenceSyncNotificationEnabled", Boolean.class,
            c -> ConfigManager.baityPresenceSyncNotificationEnabled,
            (c, v) -> ConfigManager.baityPresenceSyncNotificationEnabled = (Boolean) v);
        registerField("BaityPresenceProxyHost", String.class,
            c -> ConfigManager.baityPresenceProxyHost,
            (c, v) -> {
                ConfigManager.baityPresenceProxyHost = (String) v;
                refreshPresenceProxySourceFromHostPort();
            });
        registerField("BaityPresenceProxyPort", Integer.class,
            c -> ConfigManager.baityPresenceProxyPort,
            (c, v) -> {
                ConfigManager.baityPresenceProxyPort = (Integer) v;
                refreshPresenceProxySourceFromHostPort();
            });
        registerField("BaityPresenceProxySource", String.class,
            c -> ConfigManager.baityPresenceProxySource,
            (c, v) -> ConfigManager.baityPresenceProxySource = normalizePresenceProxySource((String) v));
        registerField("BaityPresenceProxyAuth", String.class,
            c -> ConfigManager.baityPresenceProxyAuth,
            (c, v) -> ConfigManager.baityPresenceProxyAuth = (String) v);
        registerField("BaityPresenceProxyFallbackDirect", Boolean.class,
            c -> ConfigManager.baityPresenceProxyFallbackDirect,
            (c, v) -> ConfigManager.baityPresenceProxyFallbackDirect = (Boolean) v);
        
    }
    
    private static void registerField(String key, Class<?> type,
                                    java.util.function.Function<ConfigManager, Object> getter,
                                    java.util.function.BiConsumer<ConfigManager, Object> setter) {
        CONFIG_FIELDS.put(key, new SettingField(key, getter, setter, type));
    }

    private static String normalizePresenceProxySource(String raw) {
        if (raw == null) {
            return "none";
        }
        String lower = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if ("manual".equals(lower) || "auto".equals(lower)) {
            return lower;
        }
        return "none";
    }

    private static void refreshPresenceProxySourceFromHostPort() {
        String host = baityPresenceProxyHost == null ? "" : baityPresenceProxyHost.trim();
        if (host.isEmpty() || baityPresenceProxyPort <= 0) {
            baityPresenceProxySource = "none";
            return;
        }
        if (!"auto".equalsIgnoreCase(baityPresenceProxySource)) {
            baityPresenceProxySource = "manual";
        }
    }

    private static void registerDroppedItemRarityScaleFields() {
        registerField("DroppedItemRarityCommon", Double.class,
            c -> ConfigManager.droppedItemRarityCommon,
            (c, v) -> ConfigManager.droppedItemRarityCommon = (Double) v);
        registerField("DroppedItemRarityUncommon", Double.class,
            c -> ConfigManager.droppedItemRarityUncommon,
            (c, v) -> ConfigManager.droppedItemRarityUncommon = (Double) v);
        registerField("DroppedItemRarityRare", Double.class,
            c -> ConfigManager.droppedItemRarityRare,
            (c, v) -> ConfigManager.droppedItemRarityRare = (Double) v);
        registerField("DroppedItemRarityEpic", Double.class,
            c -> ConfigManager.droppedItemRarityEpic,
            (c, v) -> ConfigManager.droppedItemRarityEpic = (Double) v);
        registerField("DroppedItemRarityLegendary", Double.class,
            c -> ConfigManager.droppedItemRarityLegendary,
            (c, v) -> ConfigManager.droppedItemRarityLegendary = (Double) v);
        registerField("DroppedItemRarityMythic", Double.class,
            c -> ConfigManager.droppedItemRarityMythic,
            (c, v) -> ConfigManager.droppedItemRarityMythic = (Double) v);
        registerField("DroppedItemRarityDivine", Double.class,
            c -> ConfigManager.droppedItemRarityDivine,
            (c, v) -> ConfigManager.droppedItemRarityDivine = (Double) v);
        registerField("DroppedItemRaritySpecial", Double.class,
            c -> ConfigManager.droppedItemRaritySpecial,
            (c, v) -> ConfigManager.droppedItemRaritySpecial = (Double) v);
        registerField("DroppedItemRarityVerySpecial", Double.class,
            c -> ConfigManager.droppedItemRarityVerySpecial,
            (c, v) -> ConfigManager.droppedItemRarityVerySpecial = (Double) v);
        registerField("DroppedItemRarityUltimate", Double.class,
            c -> ConfigManager.droppedItemRarityUltimate,
            (c, v) -> ConfigManager.droppedItemRarityUltimate = (Double) v);
        registerField("DroppedItemRarityAdmin", Double.class,
            c -> ConfigManager.droppedItemRarityAdmin,
            (c, v) -> ConfigManager.droppedItemRarityAdmin = (Double) v);
    }

    public static double getDroppedItemRarityScale(com.shyeuar.baity.features.droppeditem.SkyblockItemRarity rarity) {
        if (rarity == null || !rarity.hasScaleSlider()) {
            return 1.0;
        }
        return switch (rarity) {
            case COMMON -> droppedItemRarityCommon;
            case UNCOMMON -> droppedItemRarityUncommon;
            case RARE -> droppedItemRarityRare;
            case EPIC -> droppedItemRarityEpic;
            case LEGENDARY -> droppedItemRarityLegendary;
            case MYTHIC -> droppedItemRarityMythic;
            case DIVINE -> droppedItemRarityDivine;
            case SPECIAL -> droppedItemRaritySpecial;
            case VERY_SPECIAL -> droppedItemRarityVerySpecial;
            case ULTIMATE -> droppedItemRarityUltimate;
            case ADMIN -> droppedItemRarityAdmin;
            default -> 1.0;
        };
    }

    public static void setDroppedItemRarityScale(com.shyeuar.baity.features.droppeditem.SkyblockItemRarity rarity, double value) {
        if (rarity == null || !rarity.hasScaleSlider()) {
            return;
        }
        switch (rarity) {
            case COMMON -> droppedItemRarityCommon = value;
            case UNCOMMON -> droppedItemRarityUncommon = value;
            case RARE -> droppedItemRarityRare = value;
            case EPIC -> droppedItemRarityEpic = value;
            case LEGENDARY -> droppedItemRarityLegendary = value;
            case MYTHIC -> droppedItemRarityMythic = value;
            case DIVINE -> droppedItemRarityDivine = value;
            case SPECIAL -> droppedItemRaritySpecial = value;
            case VERY_SPECIAL -> droppedItemRarityVerySpecial = value;
            case ULTIMATE -> droppedItemRarityUltimate = value;
            case ADMIN -> droppedItemRarityAdmin = value;
            default -> {
            }
        }
    }

    public static void saveConfig() {
        pendingSave = false;
        pendingSaveAt = 0L;
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
            
            java.nio.file.Files.writeString(configPath, config.toString(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            LOGGER.error("Failed to save Baity config: {}", e.toString());
        }
    }

    public static void requestSave() {
        pendingSave = true;
        pendingSaveAt = System.currentTimeMillis() + SAVE_DEBOUNCE_MS;
    }

    public static void flushPendingSave() {
        if (!pendingSave) {
            return;
        }

        if (System.currentTimeMillis() < pendingSaveAt) {
            return;
        }

        saveConfig();
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
                LOGGER.debug("Migrated config to new location: {}", newConfigPath);
            }
            
            java.nio.file.Path oldBaityConfigPath = java.nio.file.Paths.get("baity/config.txt");
            if (java.nio.file.Files.exists(oldBaityConfigPath) && !java.nio.file.Files.exists(newConfigPath)) {
                java.nio.file.Files.move(oldBaityConfigPath, newConfigPath);
                LOGGER.debug("Migrated config from old baity directory: {}", newConfigPath);
            }
            
            java.util.HashSet<String> seenKeys = new java.util.HashSet<>();
            if (java.nio.file.Files.exists(newConfigPath)) {
                String content = java.nio.file.Files.readString(newConfigPath).trim();
                String[] lines = content.split("\n");
                
                Map<String, String> legacyKeyAliases = new HashMap<>();
                legacyKeyAliases.put("  TransparentNormalTag", "  TransparentizeOtherTags");
                legacyKeyAliases.put("ColorOwnName", "NickTweaks");
                legacyKeyAliases.put("ColorOwnNameChromaLightness", "NickTweaksChromaLightness");
                legacyKeyAliases.put("ColorOwnNameChromaChroma", "NickTweaksChromaChroma");
                legacyKeyAliases.put("ColorOwnNameChromaSize", "NickTweaksChromaSize");
                legacyKeyAliases.put("ColorOwnNameChromaSpeed", "NickTweaksChromaSpeed");
                legacyKeyAliases.put("ChromaOwnName", "NickTweaks");
                legacyKeyAliases.put("ChromaOwnNameChromaLightness", "NickTweaksChromaLightness");
                legacyKeyAliases.put("ChromaOwnNameChromaChroma", "NickTweaksChromaChroma");
                legacyKeyAliases.put("ChromaOwnNameChromaSize", "NickTweaksChromaSize");
                legacyKeyAliases.put("ChromaOwnNameChromaSpeed", "NickTweaksChromaSpeed");
                legacyKeyAliases.put("Crosshair", "ThirdPersonBackCrosshair");
                legacyKeyAliases.put("ThirdPersonCrosshair", "ThirdPersonBackCrosshair");
                
                legacyKeyAliases.put("FishHookTimerHideArmorStand", "FishHookTimerHideDefaultTimer");
                legacyKeyAliases.put("FancyDmgSplashColorPalette", "FancyDmgSplashBuiltinPresetMask");
                legacyKeyAliases.put("FancyDmgSplashActivePresetIndex", "FancyDmgSplashBuiltinPresetMask");

                boolean legacy2dDroppedItemPresent = false;
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    int colonIdx = line.indexOf(':');
                    if (colonIdx <= 0) continue;
                    
                    String key = line.substring(0, colonIdx);
                    String valueStr = line.substring(colonIdx + 1);

                    if ("2DdroppedItem".equals(key)) {
                        legacy2dDroppedItemPresent = true;
                    }

                    if ("NoSwapAnimation".equals(key)) {
                        Object legacy = parseValue(valueStr, Boolean.class);
                        if (legacy instanceof Boolean enabled && enabled) {
                            ConfigManager.heldItemTweaksEnabled = true;
                            ConfigManager.heldItemTweaksNoItemswapAnimationEnabled = true;
                        }
                        seenKeys.add(key);
                        continue;
                    }
                    if ("HandView".equals(key) || "ArmView".equals(key)) {
                        Object legacy = parseValue(valueStr, Boolean.class);
                        if (legacy instanceof Boolean enabled && enabled) {
                            ConfigManager.heldItemTweaksEnabled = true;
                        }
                        seenKeys.add(key);
                        continue;
                    }
                    if ("InstantHandFollow".equals(key) || "InstantArmFollow".equals(key) || "instant arm follow".equals(key)) {
                        Object legacy = parseValue(valueStr, Boolean.class);
                        if (legacy instanceof Boolean enabled && enabled) {
                            ConfigManager.heldItemTweaksEnabled = true;
                            ConfigManager.heldItemTweaksNoArmSwayEnabled = true;
                        }
                        seenKeys.add(key);
                        continue;
                    }
                    
                    if (!CONFIG_FIELDS.containsKey(key) && legacyKeyAliases.containsKey(key)) {
                        key = legacyKeyAliases.get(key);
                    }
                    
                    SettingField field = CONFIG_FIELDS.get(key);
                    if (field == null) continue;
                    seenKeys.add(key);
                    
                    ConfigManager instance = null;
                            Object value = parseValue(valueStr, field.getType());
                            field.setValue(instance, value);
                }

                if (legacy2dDroppedItemPresent && !seenKeys.contains("DroppedItem") && ConfigManager.twoDdroppedItemEnabled) {
                    ConfigManager.droppedItemEnabled = true;
                }
                if (ConfigManager.droppedItemEnabled && !seenKeys.contains("DroppedItemRarityScaleEnabled")) {
                    ConfigManager.droppedItemRarityScaleEnabled = true;
                }
            }
            boolean needSave = false;
            if (!seenKeys.contains("BaityPresenceProxyHost")) {
                ConfigManager.baityPresenceProxyHost = "";
                needSave = true;
            }
            if (!seenKeys.contains("BaityPresenceProxyPort")) {
                ConfigManager.baityPresenceProxyPort = 0;
                needSave = true;
            }
            if (!seenKeys.contains("BaityPresenceProxyAuth")) {
                ConfigManager.baityPresenceProxyAuth = "";
                needSave = true;
            }
            if (!seenKeys.contains("BaityPresenceProxyFallbackDirect")) {
                ConfigManager.baityPresenceProxyFallbackDirect = true;
                needSave = true;
            }
            if (!seenKeys.contains("BaityPresenceProxySource")) {
                String host = ConfigManager.baityPresenceProxyHost == null ? "" : ConfigManager.baityPresenceProxyHost.trim();
                if (!host.isEmpty() && ConfigManager.baityPresenceProxyPort > 0) {
                    ConfigManager.baityPresenceProxySource = "manual";
                } else {
                    ConfigManager.baityPresenceProxySource = "none";
                }
                needSave = true;
            }
            if (needSave) {
                saveConfig();
            }

            if (!ConfigManager.crosshairPainterInitialized
                && (ConfigManager.crosshairStaticLayer == null || ConfigManager.crosshairStaticLayer.isBlank())
                && (ConfigManager.crosshairActiveLayer == null || ConfigManager.crosshairActiveLayer.isBlank())) {
                int size = 31;
                java.util.BitSet sl = new java.util.BitSet(size * size);
                java.util.BitSet al = new java.util.BitSet(size * size);
                seedDefaultCrosshairStaticLayer(sl, size);
                seedDefaultCrosshairActiveLayer(al, size);
                ConfigManager.crosshairStaticLayer = encodeCrosshairBits(sl, size);
                ConfigManager.crosshairActiveLayer = encodeCrosshairBits(al, size);
                ConfigManager.crosshairPainterInitialized = true;
                saveConfig();
            }
        } catch (java.io.IOException e) {
            LOGGER.error("Failed to load Baity config: {}", e.toString());
        }
    }

    private static void seedDefaultCrosshairStaticLayer(java.util.BitSet layer, int size) {
        int c = size / 2;
        layer.set(c * size + c);
        if (c - 1 >= 0) layer.set((c - 1) * size + c);
        if (c + 1 < size) layer.set((c + 1) * size + c);
        if (c - 1 >= 0) layer.set(c * size + (c - 1));
        if (c + 1 < size) layer.set(c * size + (c + 1));
    }

    private static void seedDefaultCrosshairActiveLayer(java.util.BitSet layer, int size) {
        int c = size / 2;
        for (int d = 2; d <= 4; d++) {
            int upY = c - d;
            int downY = c + d;
            int leftX = c - d;
            int rightX = c + d;
            if (upY >= 0) layer.set(upY * size + c);
            if (downY < size) layer.set(downY * size + c);
            if (leftX >= 0) layer.set(c * size + leftX);
            if (rightX < size) layer.set(c * size + rightX);
        }
    }

    private static String encodeCrosshairBits(java.util.BitSet bits, int size) {
        int total = size * size;
        if (bits.isEmpty()) return "";
        final char[] ALPH = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
        StringBuilder out = new StringBuilder((total + 5) / 6);
        int acc = 0;
        int accBits = 0;
        for (int i = 0; i < total; i++) {
            int bit = bits.get(i) ? 1 : 0;
            acc |= (bit << accBits);
            accBits++;
            if (accBits == 6) {
                out.append(ALPH[acc & 63]);
                acc = 0;
                accBits = 0;
            }
        }
        if (accBits > 0) {
            out.append(ALPH[acc & 63]);
        }
        return out.toString();
    }
    
    private static Object parseValue(String valueStr, Class<?> type) {
        String raw = valueStr == null ? "" : valueStr.trim();
        if (type == Boolean.class) {
            if (raw.isEmpty()) return false;
            return Boolean.parseBoolean(raw);
        } else if (type == Integer.class) {
            if (raw.isEmpty()) return 0;
            return Integer.parseInt(raw);
        } else if (type == Double.class) {
            if (raw.isEmpty()) return 0.0;
            return Double.parseDouble(raw);
        } else if (type == Float.class) {
            if (raw.isEmpty()) return 0.0f;
            return Float.parseFloat(raw);
        } else if (type == Long.class) {
            if (raw.isEmpty()) return 0L;
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        } else if (type == String.class) {
            return valueStr == null ? "" : valueStr;
        }
        return valueStr == null ? "" : valueStr;
    }
}
