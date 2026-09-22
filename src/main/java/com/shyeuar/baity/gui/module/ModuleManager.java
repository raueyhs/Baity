package com.shyeuar.baity.gui.module;

import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Option;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.EnchantLoreColorEditorValue;
import com.shyeuar.baity.gui.value.ChromaFishingLineColorEditorValue;
import com.shyeuar.baity.gui.value.GradientEditorValue;
import com.shyeuar.baity.features.enchantlore.EnchantLoreColorSettings;
import com.shyeuar.baity.gui.value.TextLineInputValue;
import com.shyeuar.baity.features.droppeditem.SkyblockItemRarity;
import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
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
    private static int modulesRevision = 0;
    
    public static void registerModule(Module module) {
        modules.add(module);
        modulesRevision++;
    }
    
    public static void init() {
        initTooltips();
        
        ModuleRegistry.registerModuleWithValues(
            "BlockAnimation", "BlockAnimation", ModuleCategory.MISC,
            () -> ConfigManager.blockAnimationMode,
            val -> ConfigManager.blockAnimationMode = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue(
                    "anima mode",
                    "anima mode",
                    ConfigManager.blockAnimationAnimaMode,
                    "default",
                    ModuleCategory.MISC,
                    ButtonValue.ButtonValueType.CYCLE,
                    false
                )
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "anima mode",
                    () -> ConfigManager.blockAnimationAnimaMode,
                    val -> ConfigManager.blockAnimationAnimaMode = (String) val
                )
            }
        );
        
        GroupValue customHandPosGroup = new GroupValue("pos", "pos", ModuleCategory.MISC)
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("position x", "position x", 0, -2.5, 1.5, 0.05, ModuleCategory.MISC))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("position y", "position y", 0, -1.5, 1.5, 0.05, ModuleCategory.MISC))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("position z", "position z", 0, -1.5, 3.0, 0.05, ModuleCategory.MISC));

        GroupValue customHandRotGroup = new GroupValue("rot", "rot", ModuleCategory.MISC)
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("rotation x", "rotation x", 0, -180, 180, 1, ModuleCategory.MISC))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("rotation y", "rotation y", 0, -180, 180, 1, ModuleCategory.MISC))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("rotation z", "rotation z", 0, -180, 180, 1, ModuleCategory.MISC));

        ModuleRegistry.registerModuleWithValues(
            "CustomHandHolding", "CustomHandHolding", ModuleCategory.MISC,
            () -> ConfigManager.customHandHoldingEnabled,
            val -> ConfigManager.customHandHoldingEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                customHandPosGroup,
                customHandRotGroup,
                new com.shyeuar.baity.gui.value.SliderValue("scale", "size", 1, 0.1, 3.0, 0.05, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue("swing duration", "swing duration", 6, 1, 20, 1, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.Option("no swing", "no swing", false, ModuleCategory.MISC)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "position x",
                    () -> ConfigManager.customHandHoldingPosX,
                    val -> ConfigManager.customHandHoldingPosX = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "position y",
                    () -> ConfigManager.customHandHoldingPosY,
                    val -> ConfigManager.customHandHoldingPosY = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "position z",
                    () -> ConfigManager.customHandHoldingPosZ,
                    val -> ConfigManager.customHandHoldingPosZ = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rotation x",
                    () -> ConfigManager.customHandHoldingRotX,
                    val -> ConfigManager.customHandHoldingRotX = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rotation y",
                    () -> ConfigManager.customHandHoldingRotY,
                    val -> ConfigManager.customHandHoldingRotY = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rotation z",
                    () -> ConfigManager.customHandHoldingRotZ,
                    val -> ConfigManager.customHandHoldingRotZ = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "scale",
                    () -> ConfigManager.customHandHoldingScale,
                    val -> ConfigManager.customHandHoldingScale = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "swing duration",
                    () -> ConfigManager.swingDuration,
                    val -> ConfigManager.swingDuration = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "no swing",
                    () -> ConfigManager.customHandHoldingNoSwing,
                    val -> ConfigManager.customHandHoldingNoSwing = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "FancyCreeperVeil", "FancyCreeperVeil", ModuleCategory.MISC,
            () -> ConfigManager.fancyCreeperVeilEnabled,
            val -> ConfigManager.fancyCreeperVeilEnabled = val
        );
        
        ModuleRegistry.registerSimpleModule(
            "PepCat", "PepCat", ModuleCategory.MISC,
            () -> ConfigManager.pepCatEnabled,
            val -> ConfigManager.pepCatEnabled = val
        );

        ModuleRegistry.registerModuleWithValues(
            "PaperDoll", "PaperDoll", ModuleCategory.MISC,
            () -> ConfigManager.paperDollEnabled,
            val -> ConfigManager.paperDollEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("head restore", "head restore", false, ModuleCategory.MISC),
                new Option("hide armor", "hide armor", false, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue(
                    "head restore speed", "head restore speed", 1.0, 0.25, 2.0, 0.05, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue(
                    "facing angle", "facing angle", 200.0, 0.0, 360.0, 1.0, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue(
                    "head yaw range", "head yaw range", 30.0, 0.0, 90.0, 1.0, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue(
                    "head pitch range", "head pitch range", 50.0, 0.0, 90.0, 1.0, ModuleCategory.MISC)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "head restore",
                    () -> ConfigManager.paperDollHeadRestore,
                    val -> ConfigManager.paperDollHeadRestore = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "hide armor",
                    () -> ConfigManager.paperDollHideArmor,
                    val -> ConfigManager.paperDollHideArmor = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "head restore speed",
                    () -> (double) ConfigManager.paperDollHeadRestoreSpeed,
                    val -> ConfigManager.paperDollHeadRestoreSpeed = ((Number) val).floatValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "facing angle",
                    () -> (double) ConfigManager.paperDollFacingAngle,
                    val -> ConfigManager.paperDollFacingAngle = ((Number) val).floatValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "head yaw range",
                    () -> (double) ConfigManager.paperDollHeadYawRange,
                    val -> ConfigManager.paperDollHeadYawRange = ((Number) val).floatValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "head pitch range",
                    () -> (double) ConfigManager.paperDollHeadPitchRange,
                    val -> ConfigManager.paperDollHeadPitchRange = ((Number) val).floatValue()
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "SmolPeople", "SmolPeople", ModuleCategory.MISC,
            () -> ConfigManager.smolpeopleMode,
            val -> ConfigManager.smolpeopleMode = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option(
                        "toggle all players into smol", "toggle all players into smol",
                        ConfigManager.smolAllPlayers, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue(
                        "limb swing speed", "Limb Swing Speed",
                        2.5, 0.5, 5.0, 0.1, ModuleCategory.MISC),
                withSeparator(new ButtonValue(
                        "friends", "friends",
                        "Manage",
                        ModuleCategory.MISC,
                        ButtonValue.ButtonValueType.TRIGGER,
                        false))
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "toggle all players into smol",
                    () -> ConfigManager.smolAllPlayers,
                    val -> ConfigManager.smolAllPlayers = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "limb swing speed",
                    () -> ConfigManager.smolLimbSwingSpeed,
                    val -> ConfigManager.smolLimbSwingSpeed = ((Number) val).doubleValue()
                )
            }
        );

        ModuleRegistry.registerModuleWithValues(
            "Crosshair", "Crosshair", ModuleCategory.MISC,
            () -> ConfigManager.crosshairEnabled,
            val -> ConfigManager.crosshairEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("show third-person-back crosshair", "show third-person-back crosshair", true, ModuleCategory.MISC),
                new Option("custom crosshair", "custom crosshair", false, ModuleCategory.MISC),
                new ButtonValue(
                    "anima mode",
                    "anima mode",
                    ConfigManager.crosshairAnimaMode,
                    "always",
                    ModuleCategory.MISC,
                    ButtonValue.ButtonValueType.CYCLE,
                    false
                ),
                new com.shyeuar.baity.gui.value.CrosshairPainterValue(
                    "crosshair painter",
                    "crosshair painter",
                    ModuleCategory.MISC,
                    31,
                    ConfigManager.crosshairStaticLayer,
                    ConfigManager.crosshairActiveLayer,
                    !ConfigManager.crosshairPainterInitialized
                ),
                new Option("chroma crosshair", "chroma crosshair", false, ModuleCategory.MISC)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "show third-person-back crosshair",
                    () -> ConfigManager.thirdPersonBackCrosshairEnabled,
                    val -> ConfigManager.thirdPersonBackCrosshairEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "custom crosshair",
                    () -> ConfigManager.customCrosshairEnabled,
                    val -> ConfigManager.customCrosshairEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "anima mode",
                    () -> ConfigManager.crosshairAnimaMode,
                    val -> ConfigManager.crosshairAnimaMode = (String) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "crosshair painter",
                    () -> "v1|31|" + (ConfigManager.crosshairStaticLayer == null ? "" : ConfigManager.crosshairStaticLayer) + "|" + (ConfigManager.crosshairActiveLayer == null ? "" : ConfigManager.crosshairActiveLayer),
                    val -> {
                        if (!(val instanceof String raw)) return;
                        String[] parts = raw.split("\\|", 4);
                        if (parts.length == 4) {
                            ConfigManager.crosshairStaticLayer = parts[2];
                            ConfigManager.crosshairActiveLayer = parts[3];
                            ConfigManager.crosshairPainterInitialized = true;
                        }
                    }
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma crosshair",
                    () -> ConfigManager.crosshairChromaEnabled,
                    val -> ConfigManager.crosshairChromaEnabled = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoSwimPose", "NoSwimPose", ModuleCategory.QOL,
            () -> ConfigManager.noSwimPoseEnabled,
            val -> ConfigManager.noSwimPoseEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Sounds", "Sounds", ModuleCategory.QOL,
            () -> ConfigManager.soundsEnabled,
            val -> ConfigManager.soundsEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new GroupValue("muffler", "muffler", ModuleCategory.QOL)
                    .setSubModuleSwitchChildName("muffler enabled")
                    .addChild(new Option("muffler enabled", "enabled", true, ModuleCategory.QOL))
                    .addChild(new Option("mute enderman scream", "mute enderman scream", true, ModuleCategory.QOL))
                    .addChild(new Option("mute phantom", "mute phantom", true, ModuleCategory.QOL))
                    .addChild(new Option("mute portal", "mute portal", true, ModuleCategory.QOL))
                    .addChild(new Option("mute vampire", "mute vampire", true, ModuleCategory.QOL))
                    .addChild(new Option("mute drake", "mute drake", true, ModuleCategory.QOL))
                    .addChild(new Option("mute wormhole", "mute wormhole", true, ModuleCategory.QOL))
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "muffler enabled",
                    () -> ConfigManager.mufflerEnabled,
                    val -> ConfigManager.mufflerEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mute enderman scream",
                    () -> ConfigManager.mufflerMuteEndermanScream,
                    val -> ConfigManager.mufflerMuteEndermanScream = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mute phantom",
                    () -> ConfigManager.mufflerMutePhantom,
                    val -> ConfigManager.mufflerMutePhantom = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mute portal",
                    () -> ConfigManager.mufflerMutePortal,
                    val -> ConfigManager.mufflerMutePortal = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mute vampire",
                    () -> ConfigManager.mufflerMuteVampire,
                    val -> ConfigManager.mufflerMuteVampire = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mute drake",
                    () -> ConfigManager.mufflerMuteDrake,
                    val -> ConfigManager.mufflerMuteDrake = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mute wormhole",
                    () -> ConfigManager.mufflerMuteWormhole,
                    val -> ConfigManager.mufflerMuteWormhole = (Boolean) val
                )
            }
        );

        ModuleRegistry.registerModuleWithValues(
            "RadialMenu", "RadialMenu", ModuleCategory.QOL,
            () -> ConfigManager.radialMenuEnabled,
            val -> ConfigManager.radialMenuEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue("keybind", "keybind", 4, ModuleCategory.QOL, ButtonValue.ButtonValueType.KEYBIND, false),
                new ButtonValue("layout", "layout", "Edit", ModuleCategory.QOL, ButtonValue.ButtonValueType.TRIGGER, false)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "keybind",
                    () -> ConfigManager.radialMenuKeybind,
                    val -> ConfigManager.radialMenuKeybind = ((Number) val).intValue()
                )
            }
        );

        ModuleRegistry.registerSimpleModule(
            "NumInputer", "NumInputer", ModuleCategory.QOL,
            () -> ConfigManager.numInputerEnabled,
            val -> ConfigManager.numInputerEnabled = val
        );

        ModuleRegistry.registerSimpleModule(
            "ChatChannelSwitcher", "ChatChannelSwitcher", ModuleCategory.QOL,
            () -> ConfigManager.chatChannelSwitcherEnabled,
            val -> ConfigManager.chatChannelSwitcherEnabled = val
        );

        GroupValue keybindsWardrobeGroup = new GroupValue("wardrobe keybind", "wardrobe keybind", ModuleCategory.QOL)
            .setExpanded(ConfigManager.keybindsWardrobeGroupExpanded)
            .setSubModuleSwitchChildName("wardrobe enabled")
            .addChild(new Option("wardrobe enabled", "enabled", ConfigManager.keybindsWardrobeEnabled, ModuleCategory.QOL))
            .addChild(new Option("wardrobe auto close on use", "auto close on use", ConfigManager.keybindsWardrobeAutoCloseOnUse, ModuleCategory.QOL))
            .addChild(new Option("wardrobe prevent unequip", "prevent unequip", ConfigManager.keybindsWardrobePreventUnequip, ModuleCategory.QOL))
            .addChild(new ButtonValue(
                "wardrobe hold to unequip",
                "hold to unequip",
                ConfigManager.keybindsWardrobeHoldToUnequip,
                ConfigManager.keybindsWardrobeHoldToUnequip,
                ModuleCategory.QOL,
                ButtonValue.ButtonValueType.CYCLE,
                false
            ));

        GroupValue keybindsEquipmentGroup = new GroupValue("equipment keybind", "equipment keybind", ModuleCategory.QOL)
            .setExpanded(ConfigManager.keybindsEquipmentGroupExpanded)
            .setSubModuleSwitchChildName("equipment enabled")
            .addChild(new Option("equipment enabled", "enabled", ConfigManager.keybindsEquipmentEnabled, ModuleCategory.QOL))
            .addChild(new Option("equipment auto close on use", "auto close on use", ConfigManager.keybindsEquipmentAutoCloseOnUse, ModuleCategory.QOL))
            .addChild(new Option("equipment prevent unequip", "prevent unequip", ConfigManager.keybindsEquipmentPreventUnequip, ModuleCategory.QOL))
            .addChild(new ButtonValue(
                "equipment hold to unequip",
                "hold to unequip",
                ConfigManager.keybindsEquipmentHoldToUnequip,
                ConfigManager.keybindsEquipmentHoldToUnequip,
                ModuleCategory.QOL,
                ButtonValue.ButtonValueType.CYCLE,
                false
            ));

        GroupValue keybindsLoadoutGroup = new GroupValue("loadout keybind", "loadout keybind", ModuleCategory.QOL)
            .setExpanded(ConfigManager.keybindsLoadoutGroupExpanded)
            .setSubModuleSwitchChildName("loadout enabled")
            .addChild(new Option("loadout enabled", "enabled", ConfigManager.keybindsLoadoutEnabled, ModuleCategory.QOL))
            .addChild(new Option("loadout auto close on use", "auto close on use", ConfigManager.keybindsLoadoutAutoCloseOnUse, ModuleCategory.QOL))
            .addChild(new ButtonValue(
                "slot 10 keybind",
                "slot 10 keybind",
                ConfigManager.keybindsLoadoutSlot10Key,
                ModuleCategory.QOL,
                ButtonValue.ButtonValueType.KEYBIND,
                false
            ))
            .addChild(new ButtonValue(
                "slot 11 keybind",
                "slot 11 keybind",
                ConfigManager.keybindsLoadoutSlot11Key,
                ModuleCategory.QOL,
                ButtonValue.ButtonValueType.KEYBIND,
                false
            ))
            .addChild(new ButtonValue(
                "slot 12 keybind",
                "slot 12 keybind",
                ConfigManager.keybindsLoadoutSlot12Key,
                ModuleCategory.QOL,
                ButtonValue.ButtonValueType.KEYBIND,
                false
            ));

        ModuleRegistry.registerModuleWithValues(
            "Keybinds", "Keybinds", ModuleCategory.QOL,
            () -> ConfigManager.keybindsEnabled,
            val -> ConfigManager.keybindsEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                keybindsWardrobeGroup,
                keybindsEquipmentGroup,
                keybindsLoadoutGroup
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "wardrobe keybind",
                    () -> ConfigManager.keybindsWardrobeGroupExpanded,
                    val -> ConfigManager.keybindsWardrobeGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "wardrobe enabled",
                    () -> ConfigManager.keybindsWardrobeEnabled,
                    val -> ConfigManager.keybindsWardrobeEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "wardrobe auto close on use",
                    () -> ConfigManager.keybindsWardrobeAutoCloseOnUse,
                    val -> ConfigManager.keybindsWardrobeAutoCloseOnUse = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "wardrobe prevent unequip",
                    () -> ConfigManager.keybindsWardrobePreventUnequip,
                    val -> ConfigManager.keybindsWardrobePreventUnequip = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "wardrobe hold to unequip",
                    () -> ConfigManager.keybindsWardrobeHoldToUnequip,
                    val -> ConfigManager.keybindsWardrobeHoldToUnequip = (String) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "equipment keybind",
                    () -> ConfigManager.keybindsEquipmentGroupExpanded,
                    val -> ConfigManager.keybindsEquipmentGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "equipment enabled",
                    () -> ConfigManager.keybindsEquipmentEnabled,
                    val -> ConfigManager.keybindsEquipmentEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "equipment auto close on use",
                    () -> ConfigManager.keybindsEquipmentAutoCloseOnUse,
                    val -> ConfigManager.keybindsEquipmentAutoCloseOnUse = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "equipment prevent unequip",
                    () -> ConfigManager.keybindsEquipmentPreventUnequip,
                    val -> ConfigManager.keybindsEquipmentPreventUnequip = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "equipment hold to unequip",
                    () -> ConfigManager.keybindsEquipmentHoldToUnequip,
                    val -> ConfigManager.keybindsEquipmentHoldToUnequip = (String) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "loadout keybind",
                    () -> ConfigManager.keybindsLoadoutGroupExpanded,
                    val -> ConfigManager.keybindsLoadoutGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "loadout enabled",
                    () -> ConfigManager.keybindsLoadoutEnabled,
                    val -> ConfigManager.keybindsLoadoutEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "loadout auto close on use",
                    () -> ConfigManager.keybindsLoadoutAutoCloseOnUse,
                    val -> ConfigManager.keybindsLoadoutAutoCloseOnUse = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "slot 10 keybind",
                    () -> ConfigManager.keybindsLoadoutSlot10Key,
                    val -> ConfigManager.keybindsLoadoutSlot10Key = ((Number) val).intValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "slot 11 keybind",
                    () -> ConfigManager.keybindsLoadoutSlot11Key,
                    val -> ConfigManager.keybindsLoadoutSlot11Key = ((Number) val).intValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "slot 12 keybind",
                    () -> ConfigManager.keybindsLoadoutSlot12Key,
                    val -> ConfigManager.keybindsLoadoutSlot12Key = ((Number) val).intValue()
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Reminder", "Reminder", ModuleCategory.QOL,
            () -> ConfigManager.reminderEnabled,
            val -> ConfigManager.reminderEnabled = val,
            new Option[]{
                new Option("cookie buff reminder", "cookie buff reminder", true, ModuleCategory.QOL),
                new Option("god potion reminder", "god potion reminder", true, ModuleCategory.QOL),
                new Option("kat reminder", "kat reminder", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "cookie buff reminder",
                    () -> ConfigManager.reminderCookieBuffEnabled,
                    val -> ConfigManager.reminderCookieBuffEnabled = (Boolean) val,
                    com.shyeuar.baity.features.Reminder::updateSettings
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "god potion reminder",
                    () -> ConfigManager.reminderGodPotionEnabled,
                    val -> ConfigManager.reminderGodPotionEnabled = (Boolean) val,
                    com.shyeuar.baity.features.Reminder::updateSettings
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "kat reminder",
                    () -> ConfigManager.reminderKatEnabled,
                    val -> ConfigManager.reminderKatEnabled = (Boolean) val,
                    com.shyeuar.baity.features.Reminder::updateSettings
                )
            }
        );

        GroupValue nickTweaksChromaGroup = new GroupValue("chroma settings", "chroma settings", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.nickTweaksChromaGroupExpanded)
            .setSubModuleSwitchChildName("chroma settings enabled")
            .addChild(new Option("chroma settings enabled", "enabled", ConfigManager.nickTweaksChromaEnabled, ModuleCategory.RENDER))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma lightness", "chroma lightness", 0.8, 0.2, 1.0, 0.05, ModuleCategory.RENDER
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma chroma", "chroma chroma", 0.2, 0.0, 0.4, 0.01, ModuleCategory.RENDER
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma size", "chroma size", 3.1, 0.5, 10.0, 0.1, ModuleCategory.RENDER
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma speed", "chroma speed", 1.0, 0.1, 8.0, 0.1, ModuleCategory.RENDER
            ));

        ModuleRegistry.registerModuleWithValues(
            "NickTweaks", "NickTweaks", ModuleCategory.RENDER,
            () -> ConfigManager.nickTweaksEnabled,
            val -> ConfigManager.nickTweaksEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new TextLineInputValue("nick changer", "nick changer", "", ModuleCategory.RENDER),
                new Option("bold self nick", "bold self nick", false, ModuleCategory.RENDER),
                new Option("custom nick color", "custom nick color", false, ModuleCategory.RENDER),
                new GradientEditorValue(
                    "gradient editor", "gradient editor", ModuleCategory.RENDER,
                    ConfigManager.nickTweaksGradientStartColor, ConfigManager.nickTweaksGradientEndColor
                ),
                nickTweaksChromaGroup
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "nick changer",
                    () -> ConfigManager.nickTweaksNickChanger,
                    val -> ConfigManager.nickTweaksNickChanger = (String) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "bold self nick",
                    () -> ConfigManager.nickTweaksBoldSelf,
                    val -> ConfigManager.nickTweaksBoldSelf = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "custom nick color",
                    () -> ConfigManager.nickTweaksCustomNickColorEnabled,
                    val -> ConfigManager.nickTweaksCustomNickColorEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma settings",
                    () -> ConfigManager.nickTweaksChromaGroupExpanded,
                    val -> ConfigManager.nickTweaksChromaGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma settings enabled",
                    () -> ConfigManager.nickTweaksChromaEnabled,
                    val -> ConfigManager.nickTweaksChromaEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "gradient editor",
                    () -> String.format("#%06X,#%06X", ConfigManager.nickTweaksGradientStartColor & 0xFFFFFF, ConfigManager.nickTweaksGradientEndColor & 0xFFFFFF),
                    val -> {
                        if (!(val instanceof String raw)) return;
                        String[] parts = raw.split(",", 2);
                        if (parts.length != 2) return;
                        String start = parts[0].trim().replace("#", "");
                        String end = parts[1].trim().replace("#", "");
                        if (!start.matches("^[0-9A-Fa-f]{6}$") || !end.matches("^[0-9A-Fa-f]{6}$")) return;
                        ConfigManager.nickTweaksGradientStartColor = Integer.parseInt(start, 16);
                        ConfigManager.nickTweaksGradientEndColor = Integer.parseInt(end, 16);
                    }
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma lightness",
                    () -> ConfigManager.nickTweaksChromaLightness,
                    val -> ConfigManager.nickTweaksChromaLightness = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma chroma",
                    () -> ConfigManager.nickTweaksChromaChroma,
                    val -> ConfigManager.nickTweaksChromaChroma = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma size",
                    () -> ConfigManager.nickTweaksChromaSize,
                    val -> ConfigManager.nickTweaksChromaSize = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma speed",
                    () -> ConfigManager.nickTweaksChromaSpeed,
                    val -> ConfigManager.nickTweaksChromaSpeed = ((Number) val).doubleValue()
                )
            }
        );

        GroupValue enchantLoreRainbowGroup = new GroupValue("rainbow", "rainbow", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.enchantLoreRainbowGroupExpanded)
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "rainbow speed", "speed", 1.0, 0.1, 2.0, 0.1, ModuleCategory.RENDER
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "rainbow saturation", "saturation", 0.8, 0.0, 1.0, 0.05, ModuleCategory.RENDER
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "rainbow gradient", "gradient", 100.0, 25.0, 200.0, 1.0, ModuleCategory.RENDER
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "rainbow angle", "angle", 45.0, 1.0, 89.0, 1.0, ModuleCategory.RENDER
            ));

        EnchantLoreColorEditorValue enchantLoreColorEditor = new EnchantLoreColorEditorValue(
                "color editer", "color editer", ModuleCategory.RENDER
        );

        ButtonValue enchantLoreLayoutMode = new ButtonValue(
                "layout mode",
                "layout mode",
                ConfigManager.enchantLoreLayoutMode,
                "default",
                ModuleCategory.RENDER,
                ButtonValue.ButtonValueType.CYCLE,
                false
        );

        GroupValue enchantLoreRomanNumeralsGroup = new GroupValue("roman numerals", "roman numerals", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.enchantLoreRomanNumeralsGroupExpanded)
            .setSubModuleSwitchChildName("arabic numerals")
            .addChild(new Option("arabic numerals", "arabic numerals", ConfigManager.enchantLoreArabicNumerals, ModuleCategory.RENDER))
            .addChild(new Option("don't replace item name", "don't replace item name", ConfigManager.enchantLoreDontReplaceRomanInItemName, ModuleCategory.RENDER));

        ModuleRegistry.registerModuleWithValues(
            "EnchantLore", "EnchantLore", ModuleCategory.RENDER,
            () -> ConfigManager.enchantLoreEnabled,
            val -> ConfigManager.enchantLoreEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{enchantLoreLayoutMode, enchantLoreColorEditor, enchantLoreRainbowGroup, enchantLoreRomanNumeralsGroup},
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "layout mode",
                    () -> ConfigManager.enchantLoreLayoutMode,
                    val -> {
                        ConfigManager.enchantLoreLayoutMode = (String) val;
                        com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
                    }
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "color editer",
                    () -> EnchantLoreColorSettings.encode(),
                    val -> {
                        EnchantLoreColorSettings.decode((String) val);
                        com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
                    }
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rainbow",
                    () -> ConfigManager.enchantLoreRainbowGroupExpanded,
                    val -> ConfigManager.enchantLoreRainbowGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rainbow speed",
                    () -> ConfigManager.enchantLoreRainbowSpeed,
                    val -> ConfigManager.enchantLoreRainbowSpeed = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rainbow saturation",
                    () -> ConfigManager.enchantLoreRainbowSaturation,
                    val -> ConfigManager.enchantLoreRainbowSaturation = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rainbow gradient",
                    () -> (double) ConfigManager.enchantLoreRainbowGradient,
                    val -> ConfigManager.enchantLoreRainbowGradient = ((Number) val).intValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rainbow angle",
                    () -> (double) ConfigManager.enchantLoreRainbowAngle,
                    val -> ConfigManager.enchantLoreRainbowAngle = ((Number) val).intValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "roman numerals",
                    () -> ConfigManager.enchantLoreRomanNumeralsGroupExpanded,
                    val -> ConfigManager.enchantLoreRomanNumeralsGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "arabic numerals",
                    () -> ConfigManager.enchantLoreArabicNumerals,
                    val -> {
                        ConfigManager.enchantLoreArabicNumerals = (Boolean) val;
                        com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
                    }
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "don't replace item name",
                    () -> ConfigManager.enchantLoreDontReplaceRomanInItemName,
                    val -> {
                        ConfigManager.enchantLoreDontReplaceRomanInItemName = (Boolean) val;
                        com.shyeuar.baity.features.enchantlore.EnchantLore.invalidateCache();
                    }
                )
            }
        );
        
        Module clickGUI = new Module("ClickGUI", "ClickGUI", ModuleCategory.GUI);
        clickGUI.setEnabled(true);
        registerModule(clickGUI);
        ConfigSynchronizer.registerModuleConfig(
            "ClickGUI",
            () -> ConfigManager.guiEnabled,
            val -> ConfigManager.guiEnabled = val
        );
        

        ModuleRegistry.registerSimpleModule(
            "3DSkins", "3DSkins", ModuleCategory.MISC,
            () -> ConfigManager.skinLayer3DEnabled,
            val -> ConfigManager.skinLayer3DEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Culling", "Culling", ModuleCategory.RENDER,
            () -> ConfigManager.cullingEnabled,
            val -> ConfigManager.cullingEnabled = val,
            new Option[]{
                new Option("hide dying mob", "hide dying mob", false, ModuleCategory.RENDER),
                new Option("hide non-starred mob nametag", "hide non-starred mob nametag", false, ModuleCategory.RENDER),
                withSeparator(new Option("remove underwater fog", "remove underwater fog", false, ModuleCategory.RENDER)),
                new Option("remove rain&snow", "remove rain&snow", true, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "hide dying mob",
                    () -> ConfigManager.cullingHideDyingMob,
                    val -> ConfigManager.cullingHideDyingMob = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "hide non-starred mob nametag",
                    () -> ConfigManager.cullingHideNonStarredNametag,
                    val -> ConfigManager.cullingHideNonStarredNametag = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "remove underwater fog",
                    () -> ConfigManager.cullingRemoveUnderwaterFog,
                    val -> ConfigManager.cullingRemoveUnderwaterFog = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "remove rain&snow",
                    () -> ConfigManager.cullingRemoveRainSnow,
                    val -> ConfigManager.cullingRemoveRainSnow = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "FancyDmgSplash", "FancyDmgSplash", ModuleCategory.MISC,
            () -> ConfigManager.fancyDmgSplashEnabled,
            val -> ConfigManager.fancyDmgSplashEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue(
                    "style",
                    "style",
                    "default",
                    "default",
                    ModuleCategory.MISC,
                    ButtonValue.ButtonValueType.CYCLE,
                    false
                ),
                new Option("sync non-critical dmg", "sync non-critical dmg", false, ModuleCategory.MISC),
                new Option("compact damage number", "compact damage number", true, ModuleCategory.MISC),
                new Option("bold", "bold", false, ModuleCategory.MISC),
                new Option("genshin elemental reaction", "genshin elemental reaction", false, ModuleCategory.MISC),
                new ButtonValue(
                    "separator",
                    "separator",
                    "none",
                    "none",
                    ModuleCategory.MISC,
                    ButtonValue.ButtonValueType.CYCLE,
                    false
                ),
                new com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue("color editor", "color editor", ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.FancyDmgSplashPresetValue("preset", "preset", ModuleCategory.MISC)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "style",
                    () -> ConfigManager.fancyDmgSplashStyle,
                    val -> ConfigManager.fancyDmgSplashStyle = (String) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "sync non-critical dmg",
                    () -> ConfigManager.fancyDmgSplashSyncNonCritical,
                    val -> ConfigManager.fancyDmgSplashSyncNonCritical = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "compact damage number",
                    () -> ConfigManager.fancyDmgSplashCompactDamageNumber,
                    val -> ConfigManager.fancyDmgSplashCompactDamageNumber = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "bold",
                    () -> ConfigManager.fancyDmgSplashBold,
                    val -> ConfigManager.fancyDmgSplashBold = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "separator",
                    () -> ConfigManager.fancyDmgSplashSeparator,
                    val -> ConfigManager.fancyDmgSplashSeparator = (String) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "color editor",
                    () -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.encodeColorEditor(),
                    val -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.decodeColorEditor((String) val)
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "genshin elemental reaction",
                    () -> ConfigManager.fancyDmgSplashGenshinReaction,
                    val -> ConfigManager.fancyDmgSplashGenshinReaction = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "preset",
                    () -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.packPaletteConfig(),
                    val -> com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.unpackPaletteConfig(((Number) val).longValue())
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoHurtCam", "NoHurtCam", ModuleCategory.QOL,
            () -> ConfigManager.noHurtCamEnabled,
            val -> ConfigManager.noHurtCamEnabled = val
        );

        ModuleRegistry.registerSimpleModule(
            "SoftFullscreen", "SoftFullscreen", ModuleCategory.QOL,
            () -> ConfigManager.softFullscreenEnabled,
            val -> ConfigManager.softFullscreenEnabled = val
        );

        ModuleRegistry.registerModuleWithValues(
            "AutoSprint", "AutoSprint", ModuleCategory.QOL,
            () -> ConfigManager.autoSprintEnabled,
            val -> ConfigManager.autoSprintEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue(
                    "keybind",
                    "keybind",
                    ConfigManager.autoSprintKeybind,
                    ModuleCategory.QOL,
                    ButtonValue.ButtonValueType.KEYBIND,
                    true
                ),
                new Option("sprint underwater", "sprint underwater", ConfigManager.autoSprintUnderWater, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "keybind",
                    () -> ConfigManager.autoSprintKeybind,
                    val -> ConfigManager.autoSprintKeybind = ((Number) val).intValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "sprint underwater",
                    () -> ConfigManager.autoSprintUnderWater,
                    val -> ConfigManager.autoSprintUnderWater = (Boolean) val
                )
            }
        );

        ModuleRegistry.registerModuleWithValues(
            "SidePanel", "SidePanel", ModuleCategory.QOL,
            () -> ConfigManager.sidePanelEnabled,
            val -> ConfigManager.sidePanelEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("pet panel", "pet panel", ConfigManager.sidePanelPetEnabled, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "pet panel",
                    () -> ConfigManager.sidePanelPetEnabled,
                    val -> ConfigManager.sidePanelPetEnabled = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "HeldItemTweaks", "HeldItemTweaks", ModuleCategory.RENDER,
            () -> ConfigManager.heldItemTweaksEnabled,
            val -> ConfigManager.heldItemTweaksEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("no itemswap animation", "no itemswap animation", true, ModuleCategory.RENDER),
                new Option("no arm sway", "no arm sway", true, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "no itemswap animation",
                    () -> ConfigManager.heldItemTweaksNoItemswapAnimationEnabled,
                    val -> ConfigManager.heldItemTweaksNoItemswapAnimationEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "no arm sway",
                    () -> ConfigManager.heldItemTweaksNoArmSwayEnabled,
                    val -> ConfigManager.heldItemTweaksNoArmSwayEnabled = (Boolean) val
                )
            }
        );

        ModuleRegistry.registerSimpleModule(
            "NoTextShadow", "NoTextShadow", ModuleCategory.RENDER,
            () -> ConfigManager.noTextShadowEnabled,
            val -> ConfigManager.noTextShadowEnabled = val
        );

        ModuleRegistry.registerModuleWithValues(
            "MotionBlur", "MotionBlur", ModuleCategory.RENDER,
            () -> ConfigManager.motionBlurEnabled,
            val -> ConfigManager.motionBlurEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new com.shyeuar.baity.gui.value.SliderValue(
                    "blur strength", "blur strength", 0.6, 0.0, 2.0, 0.1, ModuleCategory.RENDER
                )
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "blur strength",
                    () -> (double) ConfigManager.motionBlurStrength,
                    val -> ConfigManager.motionBlurStrength = ((Number) val).floatValue()
                )
            }
        );

        GroupValue prismBreakChromaGroup = new GroupValue("chroma settings", "chroma settings", ModuleCategory.RENDER)
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("chroma lightness", "chroma lightness", 0.8, 0.2, 1.0, 0.05, ModuleCategory.RENDER))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("chroma chroma", "chroma chroma", 0.2, 0.0, 0.4, 0.01, ModuleCategory.RENDER))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue("chroma size", "chroma size", 2.5, 0.5, 10.0, 0.1, ModuleCategory.RENDER));

        ModuleRegistry.registerModuleWithValues(
            "PrismBreak", "prismatic breaking glow", ModuleCategory.RENDER,
            () -> ConfigManager.prismBreakEnabled,
            val -> ConfigManager.prismBreakEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new com.shyeuar.baity.gui.value.SliderValue(
                    "flow speed", "flow speed", 1.0, 0.1, 5.0, 0.1, ModuleCategory.RENDER
                ),
                new com.shyeuar.baity.gui.value.SliderValue(
                    "edge width", "edge width", 5.0, 1.0, 20.0, 0.5, ModuleCategory.RENDER
                ),
                prismBreakChromaGroup,
                new Option("replace vanilla", "replace vanilla", ConfigManager.prismBreakReplaceVanilla, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "flow speed",
                    () -> ConfigManager.prismBreakSpeed,
                    val -> ConfigManager.prismBreakSpeed = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "edge width",
                    () -> ConfigManager.prismBreakEdgeWidth,
                    val -> ConfigManager.prismBreakEdgeWidth = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma lightness",
                    () -> ConfigManager.prismBreakChromaLightness,
                    val -> ConfigManager.prismBreakChromaLightness = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma chroma",
                    () -> ConfigManager.prismBreakChromaChroma,
                    val -> ConfigManager.prismBreakChromaChroma = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma size",
                    () -> ConfigManager.prismBreakChromaSize,
                    val -> ConfigManager.prismBreakChromaSize = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "replace vanilla",
                    () -> ConfigManager.prismBreakReplaceVanilla,
                    val -> ConfigManager.prismBreakReplaceVanilla = (Boolean) val
                )
            }
        );

        ModuleRegistry.registerModuleWithValues(
            "VanillaHudHider", "VanillaHudHider", ModuleCategory.RENDER,
            () -> ConfigManager.vanillaHudHiderEnabled,
            val -> ConfigManager.vanillaHudHiderEnabled = val,
            new Option[]{
                new Option("armor bar", "armor bar", false, ModuleCategory.RENDER),
                new Option("health bar", "health bar", false, ModuleCategory.RENDER),
                new Option("food bar", "food bar", false, ModuleCategory.RENDER),
                new Option("air bar", "air bar", false, ModuleCategory.RENDER),
                new Option("mount health", "mount health", false, ModuleCategory.RENDER),
                new Option("experience bar", "experience bar", false, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "armor bar",
                    () -> ConfigManager.vanillaHudHiderArmorBar,
                    val -> ConfigManager.vanillaHudHiderArmorBar = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "health bar",
                    () -> ConfigManager.vanillaHudHiderHealthBar,
                    val -> ConfigManager.vanillaHudHiderHealthBar = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "food bar",
                    () -> ConfigManager.vanillaHudHiderFoodBar,
                    val -> ConfigManager.vanillaHudHiderFoodBar = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "air bar",
                    () -> ConfigManager.vanillaHudHiderAirBar,
                    val -> ConfigManager.vanillaHudHiderAirBar = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mount health",
                    () -> ConfigManager.vanillaHudHiderMountHealth,
                    val -> ConfigManager.vanillaHudHiderMountHealth = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "experience bar",
                    () -> ConfigManager.vanillaHudHiderExperienceBar,
                    val -> ConfigManager.vanillaHudHiderExperienceBar = (Boolean) val
                )
            }
        );

        GroupValue chromaFishingLineChromaGroup = new GroupValue("chroma settings", "chroma settings", ModuleCategory.MISC)
            .setExpanded(ConfigManager.chromaFishingLineChromaGroupExpanded)
            .setSubModuleSwitchChildName("chroma settings enabled")
            .addChild(new Option("chroma settings enabled", "enabled", ConfigManager.chromaFishingLineChromaEnabled, ModuleCategory.MISC))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma lightness", "chroma lightness", 0.8, 0.2, 1.0, 0.05, ModuleCategory.MISC
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma chroma", "chroma chroma", 0.2, 0.0, 0.4, 0.01, ModuleCategory.MISC
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma size", "chroma size", 3.1, 0.5, 10.0, 0.1, ModuleCategory.MISC
            ))
            .addChild(new com.shyeuar.baity.gui.value.SliderValue(
                "chroma speed", "chroma speed", 1.0, 0.1, 8.0, 0.1, ModuleCategory.MISC
            ))
            .addChild(new Option("reverse direction", "reverse direction", ConfigManager.chromaFishingLineChromaReverseDirection, ModuleCategory.MISC));

        ModuleRegistry.registerModuleWithValues(
            "ChromaFishingLine", "ChromaFishingLine", ModuleCategory.MISC,
            () -> ConfigManager.chromaFishingLineEnabled,
            val -> ConfigManager.chromaFishingLineEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ChromaFishingLineColorEditorValue(
                    "gradient editor", "gradient editor", ModuleCategory.MISC
                ),
                chromaFishingLineChromaGroup
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "gradient editor",
                    () -> String.format("#%06X,#%06X",
                            ConfigManager.chromaFishingLineGradientStart & 0xFFFFFF,
                            ConfigManager.chromaFishingLineGradientEnd & 0xFFFFFF),
                    val -> {
                        if (!(val instanceof String raw)) return;
                        String[] parts = raw.split(",", 2);
                        if (parts.length != 2) return;
                        String start = parts[0].trim().replace("#", "");
                        String end = parts[1].trim().replace("#", "");
                        if (!start.matches("^[0-9A-Fa-f]{6}$") || !end.matches("^[0-9A-Fa-f]{6}$")) return;
                        ConfigManager.chromaFishingLineGradientStart = Integer.parseInt(start, 16);
                        ConfigManager.chromaFishingLineGradientEnd = Integer.parseInt(end, 16);
                    }
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma settings",
                    () -> ConfigManager.chromaFishingLineChromaGroupExpanded,
                    val -> ConfigManager.chromaFishingLineChromaGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma settings enabled",
                    () -> ConfigManager.chromaFishingLineChromaEnabled,
                    val -> ConfigManager.chromaFishingLineChromaEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma lightness",
                    () -> ConfigManager.chromaFishingLineChromaLightness,
                    val -> ConfigManager.chromaFishingLineChromaLightness = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma chroma",
                    () -> ConfigManager.chromaFishingLineChromaChroma,
                    val -> ConfigManager.chromaFishingLineChromaChroma = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma size",
                    () -> ConfigManager.chromaFishingLineChromaSize,
                    val -> ConfigManager.chromaFishingLineChromaSize = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "chroma speed",
                    () -> ConfigManager.chromaFishingLineChromaSpeed,
                    val -> ConfigManager.chromaFishingLineChromaSpeed = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "reverse direction",
                    () -> ConfigManager.chromaFishingLineChromaReverseDirection,
                    val -> ConfigManager.chromaFishingLineChromaReverseDirection = (Boolean) val
                )
            }
        );
        
        GroupValue scrollableTooltipGroup = new GroupValue("scrollable tooltip", "scrollable tooltip", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.scrollableTooltipGroupExpanded)
            .setSubModuleSwitchChildName("scrollable enabled")
            .addChild(new Option("scrollable enabled", "enabled", ConfigManager.scrollableTooltipEnabled, ModuleCategory.RENDER))
            .addChild(new Option("keep scroll in gui", "keep scroll in gui", ConfigManager.scrollableTooltipKeepScrollInGui, ModuleCategory.RENDER))
            .addChild(new ButtonValue(
                "horizontal scrolling",
                "horizontal scrolling",
                ConfigManager.scrollableTooltipHorizontalKey,
                ModuleCategory.RENDER,
                ButtonValue.ButtonValueType.KEYBIND,
                false
            ));

        ModuleRegistry.registerModuleWithValues(
            "ModernTooltip", "ModernTooltip", ModuleCategory.RENDER,
            () -> ConfigManager.modernTooltipEnabled,
            val -> ConfigManager.modernTooltipEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("smoothtooltip", "smoothtooltip", ConfigManager.smoothTooltipEnabled, ModuleCategory.RENDER),
                scrollableTooltipGroup
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "smoothtooltip",
                    () -> ConfigManager.smoothTooltipEnabled,
                    val -> ConfigManager.smoothTooltipEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "scrollable tooltip",
                    () -> ConfigManager.scrollableTooltipGroupExpanded,
                    val -> ConfigManager.scrollableTooltipGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "scrollable enabled",
                    () -> ConfigManager.scrollableTooltipEnabled,
                    val -> ConfigManager.scrollableTooltipEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "keep scroll in gui",
                    () -> ConfigManager.scrollableTooltipKeepScrollInGui,
                    val -> ConfigManager.scrollableTooltipKeepScrollInGui = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "horizontal scrolling",
                    () -> ConfigManager.scrollableTooltipHorizontalKey,
                    val -> ConfigManager.scrollableTooltipHorizontalKey = ((Number) val).intValue()
                )
            }
        );

        ModuleRegistry.registerModuleWithValues(
            "Nodebuff", "Nodebuff", ModuleCategory.RENDER,
            () -> ConfigManager.nodebuffEnabled,
            val -> ConfigManager.nodebuffEnabled = val,
            new Option[]{
                new Option("remove nausea", "remove nausea", true, ModuleCategory.RENDER),
                new Option("remove blindness", "remove blindness", true, ModuleCategory.RENDER),
                new Option("remove darkness", "remove darkness", true, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "remove nausea",
                    () -> ConfigManager.nodebuffRemoveNausea,
                    val -> ConfigManager.nodebuffRemoveNausea = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "remove blindness",
                    () -> ConfigManager.nodebuffRemoveBlindness,
                    val -> ConfigManager.nodebuffRemoveBlindness = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "remove darkness",
                    () -> ConfigManager.nodebuffRemoveDarkness,
                    val -> ConfigManager.nodebuffRemoveDarkness = (Boolean) val
                )
            }
        );
        
        GroupValue nametagOptionsGroup = new GroupValue("nametag options", "nametag options", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.nametagOptionsGroupExpanded)
            .addChild(new Option("show distance", "show distance", false, ModuleCategory.RENDER))
            .addChild(new Option("show own nametag", "show own nametag", true, ModuleCategory.RENDER))
            .addChild(new Option("force pink color", "force pink color", true, ModuleCategory.RENDER));

        GroupValue focusPlayerNametagGroup = new GroupValue("focus player nametag", "focus player nametag", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.nametagFocusPlayerGroupExpanded)
            .addChild(new ButtonValue("keybind", "keybind", ConfigManager.nametagFocusPlayerKeybind, ModuleCategory.RENDER, ButtonValue.ButtonValueType.KEYBIND, false))
            .addChild(new ButtonValue(
                "mode",
                "mode",
                ConfigManager.nametagFocusPlayerMode,
                ConfigManager.nametagFocusPlayerMode,
                ModuleCategory.RENDER,
                ButtonValue.ButtonValueType.CYCLE,
                false
            ));

        ModuleRegistry.registerModuleWithValues(
            "Nametag", "Nametag", ModuleCategory.RENDER,
            () -> ConfigManager.nametagEnabled,
            val -> ConfigManager.nametagEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("default nametag", "only show the default nametag", false, ModuleCategory.RENDER),
                new Option("transparentize other tags", "transparent tags", false, ModuleCategory.RENDER),
                nametagOptionsGroup,
                focusPlayerNametagGroup
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "default nametag",
                    () -> ConfigManager.nametagDefaultNametag,
                    val -> ConfigManager.nametagDefaultNametag = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "transparentize other tags",
                    () -> ConfigManager.nametagTransparentizeOtherTags,
                    val -> ConfigManager.nametagTransparentizeOtherTags = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "nametag options",
                    () -> ConfigManager.nametagOptionsGroupExpanded,
                    val -> ConfigManager.nametagOptionsGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "show distance",
                    () -> ConfigManager.nametagShowDistance,
                    val -> ConfigManager.nametagShowDistance = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "show own nametag",
                    () -> ConfigManager.nametagShowOwnNametag,
                    val -> ConfigManager.nametagShowOwnNametag = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "force pink color",
                    () -> ConfigManager.nametagForcePinkColor,
                    val -> ConfigManager.nametagForcePinkColor = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "focus player nametag",
                    () -> ConfigManager.nametagFocusPlayerGroupExpanded,
                    val -> ConfigManager.nametagFocusPlayerGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "keybind",
                    () -> ConfigManager.nametagFocusPlayerKeybind,
                    val -> ConfigManager.nametagFocusPlayerKeybind = ((Number) val).intValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "mode",
                    () -> ConfigManager.nametagFocusPlayerMode,
                    val -> ConfigManager.nametagFocusPlayerMode = (String) val
                )
            }
        );

        GroupValue highlightsPestGroup = new GroupValue("pest", "pest", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.highlightsPestGroupExpanded)
            .setSubModuleSwitchChildName("enabled")
            .addChild(new Option("enabled", "enabled", ConfigManager.highlightsPestEnabled, ModuleCategory.RENDER))
            .addChild(new Option("draw line", "draw line", ConfigManager.highlightsPestDrawLineEnabled, ModuleCategory.RENDER));

        GroupValue highlightsSafariGroup = new GroupValue("safari", "Safari", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.safariGroupExpanded)
            .setSubModuleSwitchChildName("safari enabled")
            .addChild(new Option("safari enabled", "enabled", ConfigManager.safariRenderTargetESP, ModuleCategory.RENDER))
            .addChild(new Option("safari mob", "Mob", ConfigManager.safariMobEnabled, ModuleCategory.RENDER))
            .addChild(new Option("safari hideyho", "Hideyho", ConfigManager.safariHideyhoEnabled, ModuleCategory.RENDER))
            .addChild(new Option("safari npc", "NPC", ConfigManager.safariNpcEnabled, ModuleCategory.RENDER))
            .addChild(new Option("safari floor drop", "FloorDrop", ConfigManager.safariFloorDropEnabled, ModuleCategory.RENDER));

        ModuleRegistry.registerModuleWithValues(
            "Highlights", "Highlights", ModuleCategory.RENDER,
            () -> ConfigManager.highlightsEnabled,
            val -> ConfigManager.highlightsEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("shulker", "shulker", false, ModuleCategory.RENDER),
                new Option("invisibug", "Invisibug", false, ModuleCategory.RENDER),
                highlightsSafariGroup,
                highlightsPestGroup
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "shulker",
                    () -> ConfigManager.highlightsShulkerEnabled,
                    val -> ConfigManager.highlightsShulkerEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "invisibug",
                    () -> ConfigManager.highlightsInvisibugEnabled,
                    val -> ConfigManager.highlightsInvisibugEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "safari",
                    () -> ConfigManager.safariGroupExpanded,
                    val -> ConfigManager.safariGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "safari enabled",
                    () -> ConfigManager.safariRenderTargetESP,
                    val -> ConfigManager.safariRenderTargetESP = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "safari mob",
                    () -> ConfigManager.safariMobEnabled,
                    val -> ConfigManager.safariMobEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "safari hideyho",
                    () -> ConfigManager.safariHideyhoEnabled,
                    val -> ConfigManager.safariHideyhoEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "safari npc",
                    () -> ConfigManager.safariNpcEnabled,
                    val -> ConfigManager.safariNpcEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "safari floor drop",
                    () -> ConfigManager.safariFloorDropEnabled,
                    val -> ConfigManager.safariFloorDropEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "pest",
                    () -> ConfigManager.highlightsPestGroupExpanded,
                    val -> ConfigManager.highlightsPestGroupExpanded = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "enabled",
                    () -> ConfigManager.highlightsPestEnabled,
                    val -> ConfigManager.highlightsPestEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "draw line",
                    () -> ConfigManager.highlightsPestDrawLineEnabled,
                    val -> ConfigManager.highlightsPestDrawLineEnabled = (Boolean) val
                )
            }
        );
        
        GroupValue droppedItemRarityScaleGroup = new GroupValue("rarity scale", "rarity scale", ModuleCategory.RENDER)
            .setExpanded(ConfigManager.droppedItemRarityScaleGroupExpanded)
            .setSubModuleSwitchChildName("enabled")
            .addChild(new Option("enabled", "enabled", ConfigManager.droppedItemRarityScaleEnabled, ModuleCategory.RENDER));
        java.util.ArrayList<ModuleRegistry.ValueConfigInfo> droppedItemValueConfigs = new java.util.ArrayList<>();
        droppedItemValueConfigs.add(new ModuleRegistry.ValueConfigInfo(
            "2D dropped item",
            () -> ConfigManager.twoDdroppedItemEnabled,
            val -> ConfigManager.twoDdroppedItemEnabled = (Boolean) val
        ));
        droppedItemValueConfigs.add(new ModuleRegistry.ValueConfigInfo(
            "rarity scale",
            () -> ConfigManager.droppedItemRarityScaleGroupExpanded,
            val -> ConfigManager.droppedItemRarityScaleGroupExpanded = (Boolean) val
        ));
        droppedItemValueConfigs.add(new ModuleRegistry.ValueConfigInfo(
            "enabled",
            () -> ConfigManager.droppedItemRarityScaleEnabled,
            val -> ConfigManager.droppedItemRarityScaleEnabled = (Boolean) val
        ));
        for (SkyblockItemRarity rarity : SkyblockItemRarity.values()) {
            if (!rarity.hasScaleSlider()) {
                continue;
            }
            String sliderName = rarity.sliderName();
            double defaultScale = ConfigManager.getDroppedItemRarityScale(rarity);
            droppedItemRarityScaleGroup.addChild(new com.shyeuar.baity.gui.value.SliderValue(
                sliderName, sliderName, defaultScale, 1.0, 3.5, 0.05, ModuleCategory.RENDER));
            SkyblockItemRarity captured = rarity;
            droppedItemValueConfigs.add(new ModuleRegistry.ValueConfigInfo(
                sliderName,
                () -> ConfigManager.getDroppedItemRarityScale(captured),
                val -> ConfigManager.setDroppedItemRarityScale(captured, ((Number) val).doubleValue())
            ));
        }

        ModuleRegistry.registerModuleWithValues(
            "DroppedItem", "DroppedItem", ModuleCategory.RENDER,
            () -> ConfigManager.droppedItemEnabled,
            val -> ConfigManager.droppedItemEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("2D dropped item", "2D dropped item", ConfigManager.twoDdroppedItemEnabled, ModuleCategory.RENDER),
                droppedItemRarityScaleGroup
            },
            droppedItemValueConfigs.toArray(ModuleRegistry.ValueConfigInfo[]::new)
        );
        
        ModuleRegistry.registerSimpleModule(
            "OldSneaking", "OldSneaking", ModuleCategory.MISC,
            () -> ConfigManager.oldSneakingEnabled,
            val -> ConfigManager.oldSneakingEnabled = val
        );

        ModuleRegistry.registerSimpleModule(
            "RemoveRecipeBook", "RemoveRecipeBook", ModuleCategory.QOL,
            () -> ConfigManager.removeRecipeBookEnabled,
            val -> ConfigManager.removeRecipeBookEnabled = val
        );

        ModuleRegistry.registerModuleWithValues(
            "FishHookTimer", "FishHookTimer", ModuleCategory.QOL,
            () -> ConfigManager.fishHookTimerEnabled,
            val -> ConfigManager.fishHookTimerEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue(
                        "custom timer template", "custom timer template",
                        "Manage",
                        ModuleCategory.QOL,
                        ButtonValue.ButtonValueType.TRIGGER,
                        false),
                new Option("hide default timer", "hide default timer", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "hide default timer",
                    () -> ConfigManager.fishHookTimerHideDefaultTimer,
                    val -> ConfigManager.fishHookTimerHideDefaultTimer = (Boolean) val
                )
            }
        );
    }
    
    private static <T extends com.shyeuar.baity.gui.value.Value> T withSeparator(T value) {
        if (value instanceof Option) {
            ((Option) value).setNeedsSeparator(true);
        } else if (value instanceof com.shyeuar.baity.gui.value.SliderValue) {
            ((com.shyeuar.baity.gui.value.SliderValue) value).setNeedsSeparator(true);
        }
        return value;
    }
    
    private static void initTooltips() {
        TooltipManager.registerTooltip("SmolPeople", "Make your character smaller and cuter.", 0xFFFFFF);
        TooltipManager.registerTooltip("BlockAnimation", "Restore the blocking animation of version 1.7.", 0xFFFFFF);
        TooltipManager.registerTooltip("PepCat", "Play an animation and give pep talk when you died. It's a skill issue!", 0xFFFFFF);
        TooltipManager.registerTooltip("RadialMenu", "A roulette tool that invokes a shortcut command.", 0xFFFFFF);
        TooltipManager.registerTooltip(
            "Keybinds",
            "你自己的快捷键1-9 → 9个栏位\n左移 → 上一页 / Scroll Left\n右移 → 下一页 / Scroll Right",
            0xFFFF00
        );
        TooltipManager.registerTooltip(
            "NumInputer",
            "Alt+前进/后退换行，Alt+左移/右移移字\nAlt+Move keys move cursor (up/down = line)",
            0xFFFF00
        );
        TooltipManager.registerTooltip(
            "ChatChannelSwitcher",
            MessageUtils.createColoredText("Show quick chat-channel buttons.", 0xFFFFFF)
                .append(MessageUtils.createColoredText(" Click the channel button with the middle button to get the usage method.", 0xFFFF00))
        );
        TooltipManager.registerTooltip("FishHookTimer",
            MessageUtils.createColoredText("Custom timer HUD via resource pack. Use ", 0xFFFF00)
                .append(MessageUtils.createColoredText("custom timer template", 0xADFF2F))
                .append(MessageUtils.createColoredText(" → Manage for the guide.", 0xFFFF00)));
        TooltipManager.registerTooltip("NickTweaks", "DIY your own name display.", 0xFFFFFF);
        TooltipManager.registerTooltip(
            "nick changer",
            "Tip:Support the color code(&).Also,you can use &r to stop the color spreeding.",
            0xFFFF00
        );
        TooltipManager.registerTooltip("chroma settings", "Expandable chroma options container.", 0xFFFFFF);
        TooltipManager.registerTooltip("chroma lightness", "How light each chroma color should be.", 0xFFFFFF);
        TooltipManager.registerTooltip("chroma chroma", "Similar to saturation.", 0xFFFFFF);
        TooltipManager.registerTooltip("chroma size", "Width of each chroma color band.", 0xFFFFFF);
        TooltipManager.registerTooltip("chroma speed", "Speed that chroma colors move.", 0xFFFFFF);
        TooltipManager.registerTooltip(
            "chroma crosshair",
            MessageUtils.createColoredText("Only active on custom crosshair.", 0xFFFF00)
        );
        TooltipManager.registerTooltip("FancyCreeperVeil", "Replace the wither cloak ability creeper model to a fancy one.", 0xFFFFFF);
        TooltipManager.registerTooltip("NoSwimPose", "Only disables the swimming pose and eye height change on your client.", 0xFFFFFF);
        TooltipManager.registerTooltip("SoftFullscreen", "Borderless Fullscreen.", 0xFFFFFF);
        TooltipManager.registerTooltip(
            "SidePanel",
            "Display your equipment and pet on side of inventory.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip(
            "pet panel",
            "Show the pet slot on the side panel.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip(
            "HeldItemTweaks",
            "Client-side tweaks for how first-person held items are rendered.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip(
            "no itemswap animation",
            "Remove the raise/lower animation when switching hotbar items.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip(
            "no arm sway",
            "Remove hand sway when turning your view.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip("DroppedItem", "Client-side dropped item rendering tweaks.", 0xFFFFFF);
        TooltipManager.registerTooltip("2D dropped item", "Render dropped items as 2D sprites.", 0xFFFFFF);
        TooltipManager.registerTooltip("rarity scale", "Scale ground drops by Skyblock rarity.", 0xFFFFFF);
        TooltipManager.registerTooltip("OldSneaking",
            MessageUtils.createColoredText("Restore the sneaking animation of version 1.7.", 0xFFFFFF)
                .append(MessageUtils.createColoredText(" Fake sneaking eye height!", 0xFFFF00)));
        TooltipManager.registerTooltip("RemoveRecipeBook", "Remove the recipe book button and keep the recipe book hidden.", 0xFFFFFF);
        TooltipManager.registerTooltip("arabic numerals", "Replace roman number with arabic number.", 0xFFFFFF);
        TooltipManager.registerTooltip("transparentize other tags", "Remove the black background of tags.", 0xFFFFFF);
        TooltipManager.registerTooltip("NoTextShadow", "Disable all the text shadow in game.", 0xFFFFFF);
        TooltipManager.registerTooltip(
            "ChromaFishingLine",
            "Customize the fishing line color.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip(
            "VanillaHudHider",
            "Selectively hide the original hud, such as health, satiety, etc.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip("sync non-critical dmg",
            "Apply preset colors to plain non-crit damage.", 0xFFFFFF);
        TooltipManager.registerTooltip("mute wormhole", "Only work when wearing froggles.", 0xFFFF00);
        TooltipManager.registerTooltip(
            "keep scroll in gui",
            "Remember each item's tooltip scroll position separately until the screen closes.",
            0xFFFFFF
        );
        TooltipManager.registerTooltip("PrismBreak", "Prismatic breaking glow around the block collision shape while mining.", 0xFFFFFF);
        TooltipManager.registerTooltip("replace vanilla", "Draw the custom prism outline instead of the vanilla breaking cracks.", 0xFFFFFF);
    }
    
    public static List<Module> getModules() {
        return modules;
    }
    
    public static int getModulesRevision() {
        return modulesRevision;
    }
    
    public static List<Module> getModulesByCategory(ModuleCategory category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .sorted(java.util.Comparator.comparing(Module::getName))
                .collect(java.util.stream.Collectors.toList());
    }
    
    public static Module getModuleByName(String name) {
        return modules.stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
