package com.shyeuar.baity.features.inventorytweaks.sidepanel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class SidePanelMenus {
    public static final Pattern WARDROBE_TITLE = Pattern.compile(
            "^\\(\\d+/\\d+\\) Armor Sets$",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern EQUIPMENT_TITLE = Pattern.compile(
            "^\\((\\d+)/(\\d+)\\) Equipment Sets$",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern LOADOUTS_TITLE = Pattern.compile(
            "^\\((\\d+)/(\\d+)\\) Loadouts$",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern EQUIPMENT_STATS_TITLE = Pattern.compile(
            "^(?:Your Equipment and Stats|Stats & Equipment)$",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern SKYBLOCK_MENU_TITLE = Pattern.compile(
            "^SkyBlock Menu$",
            Pattern.CASE_INSENSITIVE
    );

    public static final int[] LOADOUT_BUTTON_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    private SidePanelMenus() {
    }

    public static boolean isLoadoutsMenu(Component title) {
        return title != null && LOADOUTS_TITLE.matcher(title.getString().trim()).matches();
    }

    public static int loadoutPageFromTitle(Component title) {
        if (title == null) {
            return -1;
        }
        Matcher matcher = LOADOUTS_TITLE.matcher(title.getString().trim());
        if (!matcher.matches()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static int equipmentPageFromTitle(Component title) {
        if (title == null) {
            return -1;
        }
        Matcher matcher = EQUIPMENT_TITLE.matcher(title.getString().trim());
        if (!matcher.matches()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static boolean isEquipmentSetsMenu(Component title) {
        return title != null && EQUIPMENT_TITLE.matcher(title.getString().trim()).matches();
    }

    public static boolean isEquipmentStatsMenu(String title) {
        return EQUIPMENT_STATS_TITLE.matcher(title).matches()
                || title.contains("Equipment and Stats")
                || title.contains("Stats & Equipment");
    }

    public static boolean isLoadoutEquipButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.typeHolder().is(ItemIds.DYE.gray())
                || stack.typeHolder().is(ItemIds.DYE.red())
                || stack.typeHolder().is(BlockItemIds.STAINED_GLASS_PANE.black().item())) {
            return false;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        for (Component line : lore.lines()) {
            if (line.getString().contains("Left-click to equip!")) {
                return true;
            }
        }
        return false;
    }
}
