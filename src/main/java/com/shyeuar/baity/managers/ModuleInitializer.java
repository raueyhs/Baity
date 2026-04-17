package com.shyeuar.baity.managers;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.Value;
import java.util.Set;

public class ModuleInitializer {
    
    private static final Set<String> SMOLPEOPLE_OPTIONS = Set.of();
    private static final Set<String> CROSSHAIR_OPTIONS = Set.of("show third-person-back crosshair", "custom crosshair", "chroma crosshair");
    private static final Set<String> PLAYERESP_OPTIONS = Set.of(
        "show distance",
        "show own nametag",
        "force pink color",
        "focus player nametag",
        "transparentize other tags"
    );
    private static final Set<String> REMINDER_OPTIONS = Set.of("cookie buff reminder", "god potion reminder");
    private static final Set<String> FANCYDMGSPLASH_OPTIONS = Set.of("genshin elemental reaction", "compact damage number");
    private static final Set<String> HIGHLIGHTS_OPTIONS = Set.of("shulker", "invisible bug");
    private static final Set<String> NODEBUFF_OPTIONS = Set.of("remove nausea", "remove blindness");
    
    public static void initializeModules() {
        initializeSmolPeople();
        initializeCrosshair();
        initializeBlockAnimation();
        initializePepCat();
        initializeReminder();
        initializePlayerESP();
        initializeFancyDmgSplash();
        initializeHighlights();
        initializeNodebuff();
        initializeOldSneaking();
        
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
                if (PLAYERESP_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "show distance" -> v.setValue(ConfigManager.nametagShowDistance);
                        case "show own nametag" -> v.setValue(ConfigManager.nametagShowOwnNametag);
                        case "force pink color" -> v.setValue(ConfigManager.nametagForcePinkColor);
                        case "focus player nametag" -> v.setValue(ConfigManager.nametagFocusPlayerNametag);
                        case "transparentize other tags" -> v.setValue(ConfigManager.nametagTransparentizeOtherTags);
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
                if (FANCYDMGSPLASH_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "genshin elemental reaction" -> v.setValue(ConfigManager.fancyDmgSplashGenshinReaction);
                        case "compact damage number" -> v.setValue(ConfigManager.fancyDmgSplashCompactDamageNumber);
                    }
                }
            }
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
                        case "invisible bug" -> v.setValue(ConfigManager.highlightsInvisibleBugEnabled);
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
                    }
                }
            }
        }
    }
    
    private static void initializeOldSneaking() {
        Module oldSneaking = ModuleManager.getModuleByName("OldSneaking");
        if (oldSneaking != null) {
            oldSneaking.setEnabled(ConfigManager.oldSneakingEnabled);
        }
    }
}
