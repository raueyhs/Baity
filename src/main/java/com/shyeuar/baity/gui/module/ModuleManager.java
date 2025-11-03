package com.shyeuar.baity.gui.module;

import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Option;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.config.ConfigSynchronizer;
import com.shyeuar.baity.gui.tooltip.TooltipManager;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ModuleManager {
    private static final ArrayList<Module> modules = new ArrayList<>();
    
    public static void registerModule(Module module) {
        modules.add(module);
    }
    
    public static void init() {
        initTooltips();
        
        ModuleRegistry.registerModuleWithValues(
            "SmolPeople", "SmolPeople", ModuleCategory.FUN,
            () -> ConfigManager.smolpeopleMode,
            val -> ConfigManager.smolpeopleMode = val,
            new Option[]{
                new Option("crosshair", "render third-person-back crosshair", true, ModuleCategory.FUN)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "crosshair",
                    () -> ConfigManager.crosshairMode,
                    val -> ConfigManager.crosshairMode = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "BlockAnimation", "BlockAnimation", ModuleCategory.FUN,
            () -> ConfigManager.blockAnimationMode,
            val -> ConfigManager.blockAnimationMode = val
        );
        
        Module pepCatModule = ModuleRegistry.registerSimpleModule(
            "PepCat", "PepCat", ModuleCategory.FUN,
            () -> ConfigManager.pepCatEnabled,
            val -> ConfigManager.pepCatEnabled = val
        );
        pepCatModule.setEnabled(true);
        
        ModuleRegistry.registerModuleWithValues(
            "Reminder", "Reminder", ModuleCategory.QOL,
            () -> ConfigManager.reminderEnabled,
            val -> ConfigManager.reminderEnabled = val,
            new Option[]{
                new Option("cookie buff reminder", "cookie buff reminder", true, ModuleCategory.QOL),
                new Option("god potion reminder", "god potion reminder", true, ModuleCategory.QOL),
                new Option("meowalert", "meowalert", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "cookie buff reminder",
                    () -> ConfigManager.cookieBuffReminderEnabled,
                    val -> ConfigManager.cookieBuffReminderEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "god potion reminder",
                    () -> ConfigManager.godPotionReminderEnabled,
                    val -> ConfigManager.godPotionReminderEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "meowalert",
                    () -> ConfigManager.meowAlertEnabled,
                    val -> ConfigManager.meowAlertEnabled = (Boolean) val,
                    () -> com.shyeuar.baity.features.Reminder.updateSettings()
                )
            }
        );
        
        Module clickGUI = new Module("ClickGUI", "ClickGUI", ModuleCategory.HUD);
        clickGUI.setEnabled(true);
        registerModule(clickGUI);
        ConfigSynchronizer.registerModuleConfig(
            "ClickGUI",
            () -> ConfigManager.guiEnabled,
            val -> ConfigManager.guiEnabled = val
        );

        ModuleRegistry.registerModuleWithValues(
            "PlayerESP", "PlayerESP", ModuleCategory.RENDER,
            () -> ConfigManager.playerEspEnabled,
            val -> ConfigManager.playerEspEnabled = val,
            new Option[]{
                new Option("show distance", "show distance", true, ModuleCategory.RENDER),
                new Option("show own nametag", "show own nametag", false, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "show distance",
                    () -> ConfigManager.playerEspShowDistance,
                    val -> ConfigManager.playerEspShowDistance = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "show own nametag",
                    () -> ConfigManager.playerEspShowOwnNametag,
                    val -> ConfigManager.playerEspShowOwnNametag = (Boolean) val
                )
            }
        );
    }
    
    private static void initTooltips() {
        TooltipManager.registerTooltip("SmolPeople", "Make your character smaller and cuter", 0xFFFFFF);
        TooltipManager.registerTooltip("BlockAnimation", "Restored the blocking animation of version 1.8", 0xFFFFFF);
        TooltipManager.registerTooltip("PepCat", "Play an animation and give pep talk when you died. It's a skill issue!", 0xFFFFFF);
        TooltipManager.registerTooltip("show own nametag", 
            "Due to skill issue,you should switch off all the other nametag functions before using this feature,or the game will crash!", 
            com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR);
        TooltipManager.registerTooltip("meowalert", 
            MessageUtils.createColoredText("play a ", 0xFFFFFF)
                .append(MessageUtils.createColoredText("ᯠ₋ ̫ ₋.ᯄ ੭", 0xFFC0CB))
                .append(MessageUtils.createColoredText("meow~", 0xFFC0CB))
                .append(MessageUtils.createColoredText(" when you are mentioned in chat", 0xFFFFFF)));
    }
    
    public static List<Module> getModules() {
        return modules;
    }
    
    public static List<Module> getModulesByCategory(ModuleCategory category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public static Module getModuleByName(String name) {
        return modules.stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}

