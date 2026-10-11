package com.shyeuar.baity.managers;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.Value;
import java.util.Set;

public class ModuleInitializer {
    
    private static final Set<String> SMOLPEOPLE_OPTIONS = Set.of();
    private static final Set<String> CROSSHAIR_OPTIONS = Set.of("show third-person-back crosshair", "custom crosshair", "chroma crosshair");
    private static final Set<String> PLAYERESP_OPTIONS = Set.of(
        "show distance",
        "show own nametag",
        "force pink color"
    );
    private static final Set<String> REMINDER_OPTIONS = Set.of("cookie buff reminder", "god potion reminder", "kat reminder");
    private static final Set<String> FANCYDMGSPLASH_HIDDEN_GUI = Set.of(
            "bold",
            "compact damage number"
    );
    private static final Set<String> HIGHLIGHTS_OPTIONS = Set.of("shulker", "invisibug", "safari", "pest");
    private static final Set<String> NODEBUFF_OPTIONS = Set.of("remove nausea", "remove blindness", "remove darkness");

    public static boolean isHiddenGuiValue(String valueName) {
        return FANCYDMGSPLASH_HIDDEN_GUI.contains(valueName);
    }
    
    public static void initializeModules() {
        initializeSmolPeople();
        initializeCrosshair();
        initializeBlockAnimation();
        initializeEnchantLore();
        initializePepCat();
        initializeReminder();
        initializePlayerESP();
        initializeFancyDmgSplash();
        initializeHighlights();
        initializeNodebuff();
        initializeModernTooltip();
        initializeMiscModules();
        
        com.shyeuar.baity.gui.sync.ConfigSynchronizer.syncModuleStates();
        
        com.shyeuar.baity.managers.KeybindManager.markCacheDirty();
    }
    
    private static void initializeSmolPeople() {
        Module smolPeople = ModuleManager.getModuleByName("SmolPeople");
        if (smolPeople != null) {
            smolPeople.setEnabled(ConfigManager.smolpeopleMode);
            for (Value v : smolPeople.getValues()) {
                if (SMOLPEOPLE_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                    }
                }
            }
        }
    }

    private static void initializeCrosshair() {
        Module crosshair = ModuleManager.getModuleByName("Crosshair");
        if (crosshair != null) {
            crosshair.setEnabled(ConfigManager.crosshairEnabled);
            for (Value v : crosshair.getValues()) {
                if (CROSSHAIR_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "custom crosshair" -> v.setValue(ConfigManager.customCrosshairEnabled);
                        case "show third-person-back crosshair" -> v.setValue(ConfigManager.thirdPersonBackCrosshairEnabled);
                        case "chroma crosshair" -> v.setValue(ConfigManager.crosshairChromaEnabled);
                    }
                }
            }
        }
    }
    
    private static void initializeBlockAnimation() {
        Module blockAnimation = ModuleManager.getModuleByName("BlockAnimation");
        if (blockAnimation != null) {
            blockAnimation.setEnabled(ConfigManager.blockAnimationMode);
            for (Value v : blockAnimation.getValues()) {
                if ("anima mode".equals(v.getName())) {
                    v.setValue(ConfigManager.blockAnimationAnimaMode);
                }
            }
        }
    }

    private static void initializeEnchantLore() {
        Module enchantLore = ModuleManager.getModuleByName("EnchantLore");
        if (enchantLore != null) {
            enchantLore.setEnabled(ConfigManager.enchantLoreEnabled);
            for (Value v : enchantLore.getValues()) {
                if ("layout mode".equals(v.getName())) {
                    v.setValue(ConfigManager.enchantLoreLayoutMode);
                }
            }
        }
    }
    
    private static void initializePepCat() {
        Module pepCat = ModuleManager.getModuleByName("PepCat");
        if (pepCat != null) {
            pepCat.setEnabled(ConfigManager.pepCatEnabled);
        }
        com.shyeuar.baity.features.PepCat.init();
    }
    
    private static void initializeReminder() {
        Module reminder = ModuleManager.getModuleByName("Reminder");
        if (reminder != null) {
            reminder.setEnabled(ConfigManager.reminderEnabled);
            for (Value v : reminder.getValues()) {
                if (REMINDER_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "cookie buff reminder" -> v.setValue(ConfigManager.reminderCookieBuffEnabled);
                        case "god potion reminder" -> v.setValue(ConfigManager.reminderGodPotionEnabled);
                        case "kat reminder" -> v.setValue(ConfigManager.reminderKatEnabled);
                    }
                }
            }
        }
        com.shyeuar.baity.features.Reminder.init();
    }
    
    
    
    private static void initializePlayerESP() {
        Module playerEsp = ModuleManager.getModuleByName("Nametag");
        if (playerEsp != null) {
            playerEsp.setEnabled(ConfigManager.nametagEnabled);
            for (Value v : playerEsp.getValues()) {
                switch (v.getName()) {
                    case "default nametag" -> v.setValue(ConfigManager.nametagDefaultNametag);
                    case "transparentize other tags" -> v.setValue(ConfigManager.nametagTransparentizeOtherTags);
                    case "nametag options" -> {
                        if (v instanceof GroupValue group) {
                            group.setExpanded(ConfigManager.nametagOptionsGroupExpanded);
                            for (Value child : group.getChildren()) {
                                if (PLAYERESP_OPTIONS.contains(child.getName())) {
                                    switch (child.getName()) {
                                        case "show distance" -> child.setValue(ConfigManager.nametagShowDistance);
                                        case "show own nametag" -> child.setValue(ConfigManager.nametagShowOwnNametag);
                                        case "force pink color" -> child.setValue(ConfigManager.nametagForcePinkColor);
                                        default -> {
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case "focus player nametag" -> {
                        if (v instanceof GroupValue group) {
                            group.setExpanded(ConfigManager.nametagFocusPlayerGroupExpanded);
                            for (Value child : group.getChildren()) {
                                switch (child.getName()) {
                                    case "keybind" -> child.setValue(ConfigManager.nametagFocusPlayerKeybind);
                                    case "mode" -> child.setValue(ConfigManager.nametagFocusPlayerMode);
                                    default -> {
                                    }
                                }
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }
    }
    
    private static void initializeFancyDmgSplash() {
        Module fancyDmgSplash = ModuleManager.getModuleByName("FancyDmgSplash");
        if (fancyDmgSplash != null) {
            fancyDmgSplash.setEnabled(ConfigManager.fancyDmgSplashEnabled);
            for (Value v : fancyDmgSplash.getValues()) {
                switch (v.getName()) {
                    case "style" -> {
                        if (v instanceof com.shyeuar.baity.gui.value.ButtonValue button) {
                            button.setValue(ConfigManager.fancyDmgSplashStyle);
                        }
                    }
                    case "sync non-critical dmg" -> v.setValue(ConfigManager.fancyDmgSplashSyncNonCritical);
                    case "genshin elemental reaction" -> v.setValue(ConfigManager.fancyDmgSplashGenshinReaction);
                    case "bold" -> v.setValue(ConfigManager.fancyDmgSplashBold);
                    case "compact damage number" -> v.setValue(ConfigManager.fancyDmgSplashCompactDamageNumber);
                    case "color editor" -> {
                        if (v instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue editor) {
                            editor.setValue(com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.encodeColorEditor());
                        }
                    }
                    case "separator" -> {
                        if (v instanceof com.shyeuar.baity.gui.value.ButtonValue button) {
                            button.setValue(ConfigManager.fancyDmgSplashSeparator);
                        }
                    }
                    case "preset" -> v.setValue(com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.packPaletteConfig());
                    default -> {
                    }
                }
            }
            com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.ensureInitialized();
        }
    }

    private static void initializeHighlights() {
        Module highlights = ModuleManager.getModuleByName("Highlights");
        if (highlights != null) {
            highlights.setEnabled(ConfigManager.highlightsEnabled);
            for (Value v : highlights.getValues()) {
                if (HIGHLIGHTS_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "shulker" -> v.setValue(ConfigManager.highlightsShulkerEnabled);
                        case "invisibug" -> v.setValue(ConfigManager.highlightsInvisibugEnabled);
                        case "safari" -> {
                            if (v instanceof GroupValue group) {
                                group.setExpanded(ConfigManager.safariGroupExpanded);
                                for (Value child : group.getChildren()) {
                                    switch (child.getName()) {
                                        case "safari enabled" -> child.setValue(ConfigManager.safariRenderTargetESP);
                                        case "safari mob" -> child.setValue(ConfigManager.safariMobEnabled);
                                        case "safari hideyho" -> child.setValue(ConfigManager.safariHideyhoEnabled);
                                        case "safari npc" -> child.setValue(ConfigManager.safariNpcEnabled);
                                        case "safari floor drop" -> child.setValue(ConfigManager.safariFloorDropEnabled);
                                        default -> {
                                        }
                                    }
                                }
                            }
                        }
                        case "pest" -> {
                            if (v instanceof GroupValue group) {
                                group.setExpanded(ConfigManager.highlightsPestGroupExpanded);
                                for (Value child : group.getChildren()) {
                                    switch (child.getName()) {
                                        case "enabled" -> child.setValue(ConfigManager.highlightsPestEnabled);
                                        case "draw line" -> child.setValue(ConfigManager.highlightsPestDrawLineEnabled);
                                        default -> {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void initializeNodebuff() {
        Module nodebuff = ModuleManager.getModuleByName("Nodebuff");
        if (nodebuff != null) {
            nodebuff.setEnabled(ConfigManager.nodebuffEnabled);
            for (Value v : nodebuff.getValues()) {
                if (NODEBUFF_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "remove nausea" -> v.setValue(ConfigManager.nodebuffRemoveNausea);
                        case "remove blindness" -> v.setValue(ConfigManager.nodebuffRemoveBlindness);
                        case "remove darkness" -> v.setValue(ConfigManager.nodebuffRemoveDarkness);
                    }
                }
            }
        }
    }
    
    private static void initializeModernTooltip() {
        Module module = ModuleManager.getModuleByName("ModernTooltip");
        if (module == null) {
            return;
        }
        module.setEnabled(ConfigManager.modernTooltipEnabled);
        for (Value v : module.getValues()) {
            if ("smoothtooltip".equals(v.getName())) {
                v.setValue(ConfigManager.smoothTooltipEnabled);
            } else if (v instanceof GroupValue group) {
                switch (group.getName()) {
                    case "scrollable tooltip" -> {
                        group.setExpanded(ConfigManager.scrollableTooltipGroupExpanded);
                        for (Value child : group.getChildren()) {
                            switch (child.getName()) {
                                case "scrollable enabled" -> child.setValue(ConfigManager.scrollableTooltipEnabled);
                                case "keep scroll in gui" -> child.setValue(ConfigManager.scrollableTooltipKeepScrollInGui);
                                case "horizontal scrolling" -> child.setValue(ConfigManager.scrollableTooltipHorizontalKey);
                                default -> {
                                }
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }
    }

    private static void initializeMiscModules() {
        setModuleEnabled("OldSneaking", ConfigManager.oldSneakingEnabled);
    }

    private static void setModuleEnabled(String moduleName, boolean enabled) {
        Module module = ModuleManager.getModuleByName(moduleName);
        if (module != null) {
            module.setEnabled(enabled);
        }
    }
}
