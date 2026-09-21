package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.utils.RomanNumeralUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
final class EnchantLoreParser {
    private static final Pattern ENCHANTMENT_PATTERN = Pattern.compile(
            "(?<enchant>[A-Za-z][A-Za-z -]+) (?<levelNumeral>(?=[MDCLXVI])M{0,3}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3}))(?=, |$| [\\d,]+$)"
    );
    private EnchantLoreParser() {
    }

    static Section findSection(List<Component> lore, ItemStack stack) {
        Map<String, Integer> enchantments = enchantmentsOn(stack);
        if (enchantments.isEmpty() && !isSuperpairsScreen()) {
            return null;
        }
        Map<String, Integer> attributes = attributesOn(stack);
        int start = -1;
        int end = -1;
        int maxTooltipWidth = 0;
        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            String stripped = EnchantLoreRender.stripColor(line.getString());
            if (start == -1) {
                if (lineContainsItemEnchant(enchantments, attributes, stripped)) {
                    start = i;
                }
            } else if (stripped.isBlank() && end == -1) {
                end = i - 1;
            }
            if (start == -1 || end != -1) {
                maxTooltipWidth = Math.max(Minecraft.getInstance().font.width(line), maxTooltipWidth);
            }
        }
        if (enchantments.isEmpty() && end == -1 && start != -1) {
            end = start;
        }
        if (start == -1 || end == -1) {
            return null;
        }
        maxTooltipWidth = correctTooltipWidth(maxTooltipWidth);
        return new Section(start, end, maxTooltipWidth);
    }

    static CollectResult collectEnchants(List<Component> lore, ItemStack stack, Section section) {
        Map<String, Integer> enchantments = enchantmentsOn(stack);
        Map<String, Integer> attributes = attributesOn(stack);
        TreeSet<ParsedEnchant> ordered = new TreeSet<>();
        ParsedEnchant lastEnchant = null;
        boolean hasLore = false;
        for (int i = section.start(); i <= section.end(); i++) {
            Component originalLine = lore.get(i);
            String unformattedLine = EnchantLoreRender.stripColor(originalLine.getString());
            Matcher matcher = ENCHANTMENT_PATTERN.matcher(unformattedLine);
            boolean containsEnchant = false;
            while (matcher.find()) {
                EnchantCatalog.EnchantDef def = EnchantCatalog.resolve(
                        matcher.group("enchant"), enchantments, attributes);
                if (def == null) {
                    continue;
                }
                int level = RomanNumeralUtils.parseNumeral(matcher.group("levelNumeral"));
                if (level <= 0) {
                    continue;
                }
                ParsedEnchant candidate = new ParsedEnchant(stack, def, level);
                if (!ordered.add(candidate)) {
                    for (ParsedEnchant existing : ordered) {
                        if (existing.compareTo(candidate) == 0) {
                            lastEnchant = existing;
                            break;
                        }
                    }
                } else {
                    lastEnchant = candidate;
                }
                containsEnchant = true;
            }
            if (!containsEnchant && lastEnchant != null) {
                lastEnchant.addLore(originalLine);
                hasLore = true;
            }
        }
        return new CollectResult(ordered, hasLore);
    }

    static InPlaceResult collectInPlace(List<Component> lore, ItemStack stack, Section section, long nowMs) {
        Map<String, Integer> enchantments = enchantmentsOn(stack);
        Map<String, Integer> attributes = attributesOn(stack);
        TreeSet<ParsedEnchant> enchants = new TreeSet<>();
        List<Component> lines = new ArrayList<>(section.end() - section.start() + 1);
        for (int i = section.start(); i <= section.end(); i++) {
            List<ParsedEnchant> lineEnchants = new ArrayList<>();
            String stripped = EnchantLoreRender.stripColor(lore.get(i).getString());
            if (!collectLineEnchants(stripped, stack, enchantments, attributes, lineEnchants)) {
                lines.add(null);
                continue;
            }
            enchants.addAll(lineEnchants);
            lines.add(EnchantLoreRender.formatInPlaceLine(lineEnchants, nowMs));
        }
        return new InPlaceResult(lines, enchants);
    }

    private static boolean collectLineEnchants(
            String strippedLine,
            ItemStack stack,
            Map<String, Integer> enchantments,
            Map<String, Integer> attributes,
            List<ParsedEnchant> out
    ) {
        Matcher matcher = ENCHANTMENT_PATTERN.matcher(strippedLine);
        int lastEnd = 0;
        while (matcher.find()) {
            if (!isEnchantSeparator(strippedLine.substring(lastEnd, matcher.start()))) {
                return false;
            }
            EnchantCatalog.EnchantDef def = EnchantCatalog.resolve(
                    matcher.group("enchant"), enchantments, attributes);
            int level = RomanNumeralUtils.parseNumeral(matcher.group("levelNumeral"));
            if (def == null || level <= 0) {
                return false;
            }
            out.add(new ParsedEnchant(stack, def, level));
            lastEnd = matcher.end();
        }
        return !out.isEmpty() && isEnchantSeparator(strippedLine.substring(lastEnd));
    }

    private static boolean isEnchantSeparator(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() || ",".equals(trimmed);
    }

    static boolean isMiningTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.PICKAXES)) {
            return true;
        }
        CompoundTag extra = extraAttributes(stack);
        if (extra == null) {
            return false;
        }
        if (extra.contains("drill_fuel")) {
            return true;
        }
        return "GEMSTONE_GAUNTLET".equals(extra.getString("id").orElse(null));
    }

    static String skyblockItemId(ItemStack stack) {
        CompoundTag extra = extraAttributes(stack);
        return extra == null ? null : extra.getString("id").orElse(null);
    }

    private static boolean isSuperpairsScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        return EnchantLoreRender.stripColor(screen.getTitle().getString()).contains("Superpairs");
    }

    private static boolean lineContainsItemEnchant(
            Map<String, Integer> enchantments,
            Map<String, Integer> attributes,
            String strippedLine
    ) {
        Matcher matcher = ENCHANTMENT_PATTERN.matcher(strippedLine);
        while (matcher.find()) {
            if (EnchantCatalog.resolve(matcher.group("enchant"), enchantments, attributes) != null) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> enchantmentsOn(ItemStack stack) {
        CompoundTag tag = extraAttributes(stack);
        if (tag == null) {
            return Collections.emptyMap();
        }
        HashMap<String, Integer> map = new HashMap<>();
        tag.getCompound("enchantments").ifPresent(enchants -> enchants.forEach((key, value) ->
                map.put(key, value.asInt().orElse(0))));
        return map;
    }

    private static Map<String, Integer> attributesOn(ItemStack stack) {
        CompoundTag tag = extraAttributes(stack);
        if (tag == null) {
            return Collections.emptyMap();
        }
        HashMap<String, Integer> map = new HashMap<>();
        tag.getCompound("attributes").ifPresent(attrs -> attrs.forEach((key, value) ->
                map.put(key, value.asInt().orElse(0))));
        return map;
    }

    private static CompoundTag extraAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static int correctTooltipWidth(int maxTooltipWidth) {
        var mc = Minecraft.getInstance();
        var window = mc.getWindow();
        int mouseX = (int) mc.mouseHandler.xpos();
        int tooltipX = mouseX + 12;
        if (tooltipX + maxTooltipWidth + 4 > window.getGuiScaledWidth()) {
            tooltipX = mouseX - 16 - maxTooltipWidth;
            if (tooltipX < 4) {
                if (mouseX > window.getGuiScaledWidth() / 2) {
                    maxTooltipWidth = mouseX - 12 - 8;
                } else {
                    maxTooltipWidth = window.getGuiScaledWidth() - 16 - mouseX;
                }
            }
        }
        if (window.getGuiScaledWidth() > 0 && maxTooltipWidth > window.getGuiScaledWidth()) {
            maxTooltipWidth = window.getGuiScaledWidth();
        }
        return maxTooltipWidth;
    }

    record Section(int start, int end, int maxTooltipWidth) {
    }

    record CollectResult(TreeSet<ParsedEnchant> ordered, boolean hasLore) {
    }

    record InPlaceResult(List<Component> lines, TreeSet<ParsedEnchant> enchants) {
    }

    static final class ParsedEnchant implements Comparable<ParsedEnchant> {
        final ItemStack stack;
        final EnchantCatalog.EnchantDef def;
        final int level;
        private final List<Component> loreDescription = new java.util.ArrayList<>();
        ParsedEnchant(ItemStack stack, EnchantCatalog.EnchantDef def, int level) {
            this.stack = stack;
            this.def = def;
            this.level = level;
        }
        void addLore(Component line) {
            loreDescription.add(line);
        }
        List<Component> lore() {
            return loreDescription;
        }
        EnchantLore.Entry toEntry() {
            EnchantLore.Tier tier = def.tierFor(level);
            boolean rainbow = EnchantLore.isEnabled()
                    && !def.ultimate
                    && !def.unknown
                    && level >= def.maxLevel
                    && EnchantLoreColorSettings.isRainbow(tier);
            return new EnchantLore.Entry(def, level, tier, rainbow);
        }
        @Override
        public int compareTo(ParsedEnchant other) {
            return def.compareTo(other.def);
        }
    }
}
