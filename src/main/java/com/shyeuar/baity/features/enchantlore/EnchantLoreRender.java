package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.RomanNumeralUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Environment(EnvType.CLIENT)
final class EnchantLoreRender {
    private static final String COMMA = ", ";

    private EnchantLoreRender() {
    }

    static List<Component> buildInsertLines(
            TreeSet<EnchantLoreParser.ParsedEnchant> ordered,
            boolean hasLore,
            int maxTooltipWidth,
            long nowMs
    ) {
        if (ordered.isEmpty()) {
            return List.of();
        }
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            maxTooltipWidth = Math.max(maxTooltipWidth, getRenderLength(parsed, nowMs));
        }
        if (ordered.size() != 1 && !hasLore) {
            return compress(ordered, maxTooltipWidth, nowMs);
        }
        return expand(ordered, hasLore, nowMs);
    }

    static Component formatEnchant(EnchantLoreParser.ParsedEnchant parsed, long nowMs) {
        EnchantLore.Entry entry = parsed.toEntry();
        String levelText = ConfigManager.enchantLoreArabicNumerals
                ? Integer.toString(parsed.level)
                : RomanNumeralUtils.integerToRoman(parsed.level);
        return formatTierText(parsed.def.loreName + " " + levelText, entry, styleLevelFor(parsed), nowMs, 0.0f);
    }

    static Component formatInPlaceLine(List<EnchantLoreParser.ParsedEnchant> enchants, long nowMs) {
        Font font = Minecraft.getInstance().font;
        int commaLength = font.width(COMMA);
        MutableComponent line = Component.empty();
        float x = 0.0f;
        for (int i = 0; i < enchants.size(); i++) {
            EnchantLoreParser.ParsedEnchant parsed = enchants.get(i);
            Component formatted = formatEnchant(parsed, nowMs);
            line.append(formatted);
            x += font.width(formatted);
            if (i < enchants.size() - 1) {
                appendCommaAfterEnchant(line, parsed, x, nowMs);
                x += commaLength;
            }
        }
        return line;
    }

    static Component formatTierPreview(EnchantLore.Tier tier, long nowMs) {
        String text = EnchantLoreColorSettings.TIER_LABELS[tier.ordinal()];
        Style base = Style.EMPTY;
        if (EnchantLoreColorSettings.isBold(tier)) {
            base = base.withBold(true);
        }
        if (EnchantLore.isEnabled() && EnchantLoreColorSettings.isRainbow(tier)) {
            return rainbowText(text, base, nowMs, 0.0f);
        }
        return gradientText(text, tier, base, 0.0f);
    }

    static Component formatTierButtonSlotPreview(long nowMs) {
        EnchantLore.Tier tier = EnchantLore.Tier.ULTIMATE;
        String text = EnchantLoreColorSettings.TIER_LABELS[tier.ordinal()];
        Style base = Style.EMPTY.withBold(true);
        if (EnchantLore.isEnabled() && EnchantLoreColorSettings.isRainbow(tier)) {
            return rainbowText(text, base, nowMs, 0.0f);
        }
        return gradientText(text, tier, base, 0.0f);
    }

    static String stripColor(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ChatFormatting.PREFIX_CODE && i + 1 < input.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private static List<Component> compress(
            TreeSet<EnchantLoreParser.ParsedEnchant> ordered,
            int maxTooltipWidth,
            long nowMs
    ) {
        Font font = Minecraft.getInstance().font;
        int commaLength = font.width(COMMA);
        List<Component> lines = new ArrayList<>();
        int sum = 0;
        MutableComponent loreLine = Component.empty();
        float lineRainbowX = 0.0f;
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            Component formatted = formatEnchant(parsed, nowMs);
            int renderLength = font.width(formatted);
            if (sum + renderLength > maxTooltipWidth && !loreLine.getSiblings().isEmpty()) {
                trimTrailingComma(loreLine);
                lines.add(loreLine);
                loreLine = Component.empty();
                sum = 0;
                lineRainbowX = 0.0f;
            }
            loreLine.append(formatted);
            appendCommaAfterEnchant(loreLine, parsed, lineRainbowX + renderLength, nowMs);
            lineRainbowX += renderLength + commaLength;
            sum += renderLength + commaLength;
        }
        if (font.width(loreLine) >= commaLength) {
            trimTrailingComma(loreLine);
            lines.add(loreLine);
        }
        return lines;
    }

    private static List<Component> expand(
            TreeSet<EnchantLoreParser.ParsedEnchant> ordered,
            boolean hasLore,
            long nowMs
    ) {
        List<Component> lines = new ArrayList<>((hasLore ? 3 : 1) * ordered.size());
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            lines.add(formatEnchant(parsed, nowMs));
            if (hasLore) {
                lines.addAll(parsed.lore());
            }
        }
        return lines;
    }

    private static int getRenderLength(EnchantLoreParser.ParsedEnchant parsed, long nowMs) {
        return Minecraft.getInstance().font.width(formatEnchant(parsed, nowMs));
    }

    private static Component formatTierText(
            String text,
            EnchantLore.Entry entry,
            int styleLevel,
            long nowMs,
            float xStart
    ) {
        if (entry.rainbow()) {
            Style base = baseStyle(entry, styleLevel);
            return rainbowText(text, base, nowMs, xStart);
        }
        return gradientText(text, entry.tier(), baseStyle(entry, styleLevel), xStart);
    }

    private static Component gradientText(String text, EnchantLore.Tier tier, Style base, float xStart) {
        if (text.isEmpty()) {
            return Component.empty();
        }
        Font font = Minecraft.getInstance().font;
        int totalWidth = font.width(text);
        MutableComponent root = Component.empty();
        float x = xStart;
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            float progress = totalWidth <= 0 ? 0f : (x - xStart) / totalWidth;
            int rgb = EnchantLoreColorSettings.colorAt(tier, progress);
            root.append(Component.literal(glyph).withStyle(base.withColor(rgb)));
            x += font.width(glyph);
        }
        return root;
    }

    private static int styleLevelFor(EnchantLoreParser.ParsedEnchant parsed) {
        if ("efficiency".equals(parsed.def.nbtName)) {
            if (!EnchantLoreParser.isMiningTool(parsed.stack)
                    || "STONK".equals(EnchantLoreParser.skyblockItemId(parsed.stack))) {
                if (parsed.level >= 5) {
                    return parsed.def.maxLevel;
                }
            }
        }
        return parsed.level;
    }

    private static Style baseStyle(EnchantLore.Entry entry, int styleLevel) {
        EnchantLore.Tier styleTier = entry.def().tierFor(styleLevel);
        Style style = Style.EMPTY;
        if (EnchantLoreColorSettings.isBold(styleTier)) {
            style = style.withBold(true);
        }
        return style;
    }

    private static Component rainbowText(String text, Style base, long nowMs, float xStart) {
        if (text.isEmpty()) {
            return Component.empty();
        }
        Font font = Minecraft.getInstance().font;
        MutableComponent root = Component.empty();
        float x = xStart;
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            int rgb = EnchantLore.rainbowRgbAt(x, 0.0f, nowMs);
            root.append(Component.literal(glyph).withStyle(base.withColor(rgb)));
            x += font.width(glyph);
        }
        return root;
    }

    private static void appendCommaAfterEnchant(
            MutableComponent loreLine,
            EnchantLoreParser.ParsedEnchant parsed,
            float commaRainbowX,
            long nowMs
    ) {
        EnchantLore.Entry entry = parsed.toEntry();
        loreLine.append(formatTierText(COMMA, entry, styleLevelFor(parsed), nowMs, commaRainbowX));
    }

    private static void trimTrailingComma(MutableComponent line) {
        if (!line.getSiblings().isEmpty()) {
            line.getSiblings().removeLast();
        }
    }
}
