package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
final class EnchantLoreMissing {
    private static final Pattern ITEM_TYPE_PATTERN = Pattern.compile(
            "\\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)\\b\\s+(?:DUNGEON\\s+)?([A-Z]+(?: [A-Z]+)*)"
    );
    private static final String ULTIMATE_PLACEHOLDER = "Ultimate Enchant";
    private static final String COMMA = ", ";
    private static final int ENCHANTS_PER_LINE = 3;
    private static final String TITLE = "Missing:";

    private EnchantLoreMissing() {
    }

    static boolean isActive() {
        int keybind = ConfigManager.enchantLoreMissingKeybind;
        if (keybind == 0) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        return KeyMappingUtils.isKeyPressed(mc.getWindow().handle(), keybind);
    }

    static List<Component> buildLines(List<Component> lore, ItemStack stack) {
        if (lore == null || lore.isEmpty() || stack == null || stack.isEmpty()) {
            return List.of();
        }
        String itemType = findItemType(lore);
        if (itemType == null) {
            return List.of();
        }
        List<EnchantCatalog.EnchantDef> applicable = EnchantCatalog.applicableEnchants(itemType);
        if (applicable.isEmpty()) {
            return List.of();
        }

        Set<String> present = EnchantLoreParser.presentNbtNames(lore, stack);
        boolean hasUltimate = false;
        for (String nbtName : present) {
            if (nbtName.startsWith("ultimate_")) {
                hasUltimate = true;
                break;
            }
        }
        Set<String> blocked = blockedByPools(present);

        List<String> missing = new ArrayList<>();
        EnchantCatalog.EnchantDef singleUltimate = null;
        int ultimateCount = 0;
        for (EnchantCatalog.EnchantDef def : applicable) {
            String nbtName = def.nbtName.toLowerCase(Locale.US);
            if (present.contains(nbtName) || blocked.contains(nbtName)) {
                continue;
            }
            if (def.ultimate) {
                ultimateCount++;
                singleUltimate = def;
                continue;
            }
            missing.add(def.loreName);
        }
        boolean ultimateFirst = !hasUltimate && ultimateCount > 0;
        if (ultimateFirst) {
            missing.add(0, ultimateCount == 1 ? singleUltimate.loreName : ULTIMATE_PLACEHOLDER);
        }
        if (missing.isEmpty()) {
            return List.of();
        }
        return layout(missing, ultimateFirst);
    }

    private static List<Component> layout(List<String> missing, boolean ultimateFirst) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.literal(TITLE).withStyle(ChatFormatting.RED));

        MutableComponent line = Component.empty();
        int index = 0;
        for (String name : missing) {
            MutableComponent entry = Component.literal(name).withStyle(ChatFormatting.GRAY);
            if (ultimateFirst && index == 0) {
                entry.withStyle(ChatFormatting.BOLD);
            }
            line.append(entry);
            if (index % ENCHANTS_PER_LINE < ENCHANTS_PER_LINE - 1) {
                line.append(Component.literal(COMMA).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(line);
                line = Component.empty();
            }
            index++;
        }
        if (index % ENCHANTS_PER_LINE != 0 && !line.getSiblings().isEmpty()) {
            line.getSiblings().removeLast();
            lines.add(line);
        }
        return lines;
    }

    private static String findItemType(List<Component> lore) {
        for (int i = lore.size() - 1; i >= 0; i--) {
            String stripped = EnchantLoreRender.stripColor(lore.get(i).getString());
            if (stripped.isBlank()) {
                continue;
            }
            Matcher matcher = ITEM_TYPE_PATTERN.matcher(stripped);
            if (!matcher.find()) {
                continue;
            }
            String type = matcher.group(1);
            if (type == null || type.isBlank()) {
                continue;
            }
            String normalized = type.trim().replace(' ', '_').toUpperCase(Locale.US);
            if (!EnchantCatalog.applicableEnchants(normalized).isEmpty()) {
                return normalized;
            }
        }
        return null;
    }

    private static Set<String> blockedByPools(Set<String> present) {
        Set<String> blocked = new HashSet<>();
        for (List<String> pool : EnchantCatalog.enchantPools()) {
            boolean presentInPool = false;
            for (String member : pool) {
                if (member != null && present.contains(member.toLowerCase(Locale.US))) {
                    presentInPool = true;
                    break;
                }
            }
            if (!presentInPool) {
                continue;
            }
            for (String member : pool) {
                if (member != null) {
                    blocked.add(member.toLowerCase(Locale.US));
                }
            }
        }
        return blocked;
    }
}
