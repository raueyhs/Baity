package com.shyeuar.baity.gui.module;

import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Option;
import com.shyeuar.baity.gui.value.ButtonValue;
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
    
    public static void registerModule(Module module) {
        modules.add(module);
    }
    
    public static void init() {
        initTooltips();
        
        ModuleRegistry.registerModuleWithValues(
            "BlockAnimation", "BlockAnimation", ModuleCategory.MISC,
            () -> ConfigManager.blockAnimationMode,
            val -> ConfigManager.blockAnimationMode = val,
            new Option[]{},
            new ModuleRegistry.ValueConfigInfo[]{}
        );
        
        ModuleRegistry.registerModuleWithValues(
            "CustomHandHolding", "CustomHandHolding", ModuleCategory.MISC,
            () -> ConfigManager.customHandHoldingEnabled,
            val -> ConfigManager.customHandHoldingEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new com.shyeuar.baity.gui.value.SliderValue("position x", "position x", 0, -2.5, 1.5, 0.05, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue("position y", "position y", 0, -1.5, 1.5, 0.05, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue("position z", "position z", 0, -1.5, 3.0, 0.05, ModuleCategory.MISC),
                withSeparator(new com.shyeuar.baity.gui.value.SliderValue("rotation x", "rotation x", 0, -180, 180, 1, ModuleCategory.MISC)),
                new com.shyeuar.baity.gui.value.SliderValue("rotation y", "rotation y", 0, -180, 180, 1, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.SliderValue("rotation z", "rotation z", 0, -180, 180, 1, ModuleCategory.MISC),
                withSeparator(new com.shyeuar.baity.gui.value.SliderValue("scale", "size", 1, 0.1, 3.0, 0.05, ModuleCategory.MISC)),
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
            val -> ConfigManager.fancyCreeperVeilEnabled = (Boolean) val
        );
        
        ModuleRegistry.registerSimpleModule(
            "PepCat", "PepCat", ModuleCategory.MISC,
            () -> ConfigManager.pepCatEnabled,
            val -> ConfigManager.pepCatEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "SmolPeople", "SmolPeople", ModuleCategory.MISC,
            () -> ConfigManager.smolpeopleMode,
            val -> ConfigManager.smolpeopleMode = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("crosshair", "crosshair", true, ModuleCategory.MISC),
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
                    "crosshair",
                    () -> ConfigManager.crosshairMode,
                    val -> ConfigManager.crosshairMode = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "limb swing speed",
                    () -> ConfigManager.smolLimbSwingSpeed,
                    val -> ConfigManager.smolLimbSwingSpeed = ((Number) val).doubleValue()
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoSwimChange", "NoSwimChange", ModuleCategory.QOL,
            () -> ConfigManager.noSwimChangeEnabled,
            val -> ConfigManager.noSwimChangeEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Muffler", "Muffler", ModuleCategory.QOL,
            () -> ConfigManager.mufflerEnabled,
            val -> ConfigManager.mufflerEnabled = val,
            new Option[]{
                new Option("mute enderman scream", "mute enderman scream", true, ModuleCategory.QOL),
                new Option("mute phantom", "mute phantom", true, ModuleCategory.QOL),
                new Option("mute portal", "mute portal", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
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
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "RadialMenu", "RadialMenu", ModuleCategory.QOL,
            () -> ConfigManager.radialMenuEnabled,
            val -> ConfigManager.radialMenuEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue("keybind", "keybind", 4, ModuleCategory.QOL, ButtonValue.ButtonValueType.KEYBIND, false)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "keybind",
                    () -> ConfigManager.radialMenuKeybind,
                    val -> ConfigManager.radialMenuKeybind = ((Number) val).intValue()
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
                withSeparator(new Option("meowalert", "meowalert", true, ModuleCategory.QOL))
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "cookie buff reminder",
                    () -> ConfigManager.reminderCookieBuffEnabled,
                    val -> ConfigManager.reminderCookieBuffEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "god potion reminder",
                    () -> ConfigManager.reminderGodPotionEnabled,
                    val -> ConfigManager.reminderGodPotionEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "meowalert",
                    () -> ConfigManager.reminderMeowAlertEnabled,
                    val -> ConfigManager.reminderMeowAlertEnabled = (Boolean) val,
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
                withSeparator(new Option("remove underwater fog", "remove underwater fog", false, ModuleCategory.RENDER))
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
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "FancyDmgSplash", "FancyDmgSplash", ModuleCategory.MISC,
            () -> ConfigManager.fancyDmgSplashEnabled,
            val -> ConfigManager.fancyDmgSplashEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("genshin elemental reaction", "genshin elemental reaction", false, ModuleCategory.MISC),
                new Option("compact damage number", "compact damage number", true, ModuleCategory.MISC),
                new com.shyeuar.baity.gui.value.ColorPaletteValue("color palette", "color palette", ModuleCategory.MISC)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "genshin elemental reaction",
                    () -> ConfigManager.fancyDmgSplashGenshinReaction,
                    val -> ConfigManager.fancyDmgSplashGenshinReaction = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "compact damage number",
                    () -> ConfigManager.fancyDmgSplashCompactDamageNumber,
                    val -> ConfigManager.fancyDmgSplashCompactDamageNumber = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "color palette",
                    () -> ConfigManager.fancyDmgSplashColorPalette,
                    val -> ConfigManager.fancyDmgSplashColorPalette = ((Number) val).intValue()
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoHurtCam", "NoHurtCam", ModuleCategory.QOL,
            () -> ConfigManager.noHurtCamEnabled,
            val -> ConfigManager.noHurtCamEnabled = val
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoSwapAnimation", "NoSwapAnimation", ModuleCategory.RENDER,
            () -> ConfigManager.noSwapAnimationEnabled,
            val -> ConfigManager.noSwapAnimationEnabled = val
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoTextShadow", "NoTextShadow", ModuleCategory.RENDER,
            () -> ConfigManager.noTextShadowEnabled,
            val -> ConfigManager.noTextShadowEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Nodebuff", "Nodebuff", ModuleCategory.RENDER,
            () -> ConfigManager.nodebuffEnabled,
            val -> ConfigManager.nodebuffEnabled = val,
            new Option[]{
                new Option("remove nausea", "remove nausea", true, ModuleCategory.RENDER),
                new Option("remove blindness", "remove blindness", true, ModuleCategory.RENDER)
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
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Nametag", "Nametag", ModuleCategory.RENDER,
            () -> ConfigManager.nametagEnabled,
            val -> ConfigManager.nametagEnabled = val,
            new Option[]{
                new Option("show distance", "show distance", false, ModuleCategory.RENDER),
                new Option("show own nametag", "show own nametag", true, ModuleCategory.RENDER),
                withSeparator(new Option("force pink color", "force pink color", true, ModuleCategory.RENDER)),
                new Option("focus player nametag", "focus player nametag", false, ModuleCategory.RENDER),
                withSeparator(new Option("transparentize other tags", "transparentize other tags", false, ModuleCategory.RENDER))
            },
            new ModuleRegistry.ValueConfigInfo[]{
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
                    () -> ConfigManager.nametagFocusPlayerNametag,
                    val -> ConfigManager.nametagFocusPlayerNametag = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "transparentize other tags",
                    () -> ConfigManager.nametagTransparentizeOtherTags,
                    val -> ConfigManager.nametagTransparentizeOtherTags = (Boolean) val
                )
            }
        );

        ModuleRegistry.registerModuleWithValues(
            "Highlights", "Highlights", ModuleCategory.RENDER,
            () -> ConfigManager.highlightsEnabled,
            val -> ConfigManager.highlightsEnabled = val,
            new Option[]{
                new Option("shulker", "shulker", false, ModuleCategory.RENDER),
                new Option("invisible bug", "invisible bug", false, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "shulker",
                    () -> ConfigManager.highlightsShulkerEnabled,
                    val -> ConfigManager.highlightsShulkerEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "invisible bug",
                    () -> ConfigManager.highlightsInvisibleBugEnabled,
                    val -> ConfigManager.highlightsInvisibleBugEnabled = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "2DdroppedItem", "2DdroppedItem", ModuleCategory.RENDER,
            () -> ConfigManager.twoDdroppedItemEnabled,
            val -> ConfigManager.twoDdroppedItemEnabled = val
        );

        ModuleRegistry.registerSimpleModule(
            "FishHookTimer", "FishHookTimer", ModuleCategory.QOL,
            () -> ConfigManager.fishHookTimerEnabled,
            val -> ConfigManager.fishHookTimerEnabled = val
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
        TooltipManager.registerTooltip("meowalert", 
            MessageUtils.createColoredText("play a ", 0xFFFFFF)
                .append(MessageUtils.createColoredText("ᯠ₋ ̫ ₋.ᯄ ੭", 0xFFC0CB))
                .append(MessageUtils.createColoredText("meow~", 0xFFC0CB))
                .append(MessageUtils.createColoredText(" when you are mentioned in chat.", 0xFFFFFF)));
        TooltipManager.registerTooltip("FishHookTimer",
            MessageUtils.createColoredText("Tip: You can DIY the timer UI by resources. Check ", 0xFFFF00)
                .append(MessageUtils.createColoredText("config\\baity\\FishHookTimer_DIY_UI_Setup_Guide.txt", 0xADFF2F))
                .append(MessageUtils.createColoredText(".", 0xFFFF00)));
        TooltipManager.registerTooltip("Muffler", "Mute the annoying sounds.", 0xFFFFFF);
        TooltipManager.registerTooltip("FancyCreeperVeil", "Replace the wither cloak ability creeper model to a fancy one.", 0xFFFFFF);
        TooltipManager.registerTooltip("NoSwimChange", "Only disables the swimming pose and eye height change on your client.", 0xFFFFFF);
        TooltipManager.registerTooltip("NoSwapAnimation", "Disable the animation of hotbar change.", 0xFFFFFF);
        TooltipManager.registerTooltip("2DdroppedItem", "Render dropped items as 2D sprites.", 0xFFFFFF);
    }
    
    public static List<Module> getModules() {
        return modules;
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
