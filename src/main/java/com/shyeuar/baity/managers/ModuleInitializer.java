package com.shyeuar.baity.managers;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.Value;
import java.util.Set;

public class ModuleInitializer {
    
    private static final Set<String> SMOLPEOPLE_OPTIONS = Set.of("crosshair");
    private static final Set<String> PLAYERESP_OPTIONS = Set.of("show distance", "show own nametag");
    private static final Set<String> REMINDER_OPTIONS = Set.of("cookie buff reminder", "god potion reminder", "meowalert");
    
    public static void initializeModules() {
        initializeSmolPeople();
        initializeBlockAnimation();
        initializePepCat();
        initializeReminder();
        initializeAutoFish();
        initializePlayerESP();
    }
    
    private static void initializeSmolPeople() {
        Module smolPeople = ModuleManager.getModuleByName("SmolPeople");
        if (smolPeople != null) {
            smolPeople.setEnabled(ConfigManager.smolpeopleMode);
            for (Value v : smolPeople.getValues()) {
                if (SMOLPEOPLE_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "crosshair" -> v.setValue(ConfigManager.crosshairMode);
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
                        case "cookie buff reminder" -> v.setValue(ConfigManager.cookieBuffReminderEnabled);
                        case "god potion reminder" -> v.setValue(ConfigManager.godPotionReminderEnabled);
                        case "meowalert" -> v.setValue(ConfigManager.meowAlertEnabled);
                    }
                }
            }
        }
        com.shyeuar.baity.features.Reminder.init();
    }
    
    private static void initializeAutoFish() {
    }
    
    private static void initializePlayerESP() {
        Module playerEsp = ModuleManager.getModuleByName("PlayerESP");
        if (playerEsp != null) {
            playerEsp.setEnabled(ConfigManager.playerEspEnabled);
            for (Value v : playerEsp.getValues()) {
                if (PLAYERESP_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "show distance" -> v.setValue(ConfigManager.playerEspShowDistance);
                        case "show own nametag" -> v.setValue(ConfigManager.playerEspShowOwnNametag);
                    }
                }
            }
        }
    }
}


