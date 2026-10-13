package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.utils.RomanNumeralUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        Map<String, Integer> attributes = attributesOn(stack);
        int start = -1;
        int end = -1;
        int lastEnchantLine = -1;
        int maxTooltipWidth = 0;
        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            if (start == -1 || end == -1) {
                String stripped = EnchantLoreRender.stripColor(line.getString());
                if (start == -1) {
                    if (lineContainsItemEnchant(enchantments, attributes, stripped)) {
                        start = i;
                        lastEnchantLine = i;
                    }
                } else if (lineContainsItemEnchant(enchantments, attributes, stripped)) {
                    lastEnchantLine = i;
                } else if (stripped.isBlank()) {
                    end = i - 1;
                }
            }
            if (start == -1 || end != -1) {
                maxTooltipWidth = Math.max(Minecraft.getInstance().font.width(line), maxTooltipWidth);
            }
        }
        if (end == -1 && start != -1 && enchantments.isEmpty()) {
            end = lastEnchantLine;
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

    private record NbtSnapshot(CustomData data, CompoundTag tag,
                               Map<String, Integer> enchantments, Map<String, Integer> attributes) {
    }

    private static final NbtSnapshot EMPTY_SNAPSHOT =
            new NbtSnapshot(null, null, Collections.emptyMap(), Collections.emptyMap());
    private static NbtSnapshot nbtSnapshot = EMPTY_SNAPSHOT;

    private static NbtSnapshot snapshotOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EMPTY_SNAPSHOT;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return EMPTY_SNAPSHOT;
        }
        NbtSnapshot current = nbtSnapshot;
        if (current.data() == data) {
            return current;
        }
        CompoundTag tag = data.copyTag();
        HashMap<String, Integer> enchantments = new HashMap<>();
        tag.getCompound("enchantments").ifPresent(enchants -> enchants.forEach((key, value) ->
                enchantments.put(key, value.asInt().orElse(0))));
        HashMap<String, Integer> attributes = new HashMap<>();
        tag.getCompound("attributes").ifPresent(attrs -> attrs.forEach((key, value) ->
                attributes.put(key, value.asInt().orElse(0))));
        NbtSnapshot fresh = new NbtSnapshot(data, tag, enchantments, attributes);
        nbtSnapshot = fresh;
        return fresh;
    }

    static Map<String, Integer> enchantmentsOn(ItemStack stack) {
        return snapshotOf(stack).enchantments();
    }

    private static Map<String, Integer> attributesOn(ItemStack stack) {
        return snapshotOf(stack).attributes();
    }

    static Set<String> presentNbtNames(List<Component> lore, ItemStack stack) {
        Map<String, Integer> enchantments = enchantmentsOn(stack);
        Set<String> names = new HashSet<>();
        if (!enchantments.isEmpty()) {
            for (String key : enchantments.keySet()) {
                names.add(key.toLowerCase(Locale.US));
            }
            return names;
        }
        Map<String, Integer> attributes = attributesOn(stack);
        for (Component line : lore) {
            Matcher matcher = ENCHANTMENT_PATTERN.matcher(EnchantLoreRender.stripColor(line.getString()));
            while (matcher.find()) {
                EnchantCatalog.EnchantDef def = EnchantCatalog.resolve(
                        matcher.group("enchant"), enchantments, attributes);
                if (def != null && def.nbtName != null) {
                    names.add(def.nbtName.toLowerCase(Locale.US));
                }
            }
        }
        return names;
    }

    private static CompoundTag extraAttributes(ItemStack stack) {
        return snapshotOf(stack).tag();
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
