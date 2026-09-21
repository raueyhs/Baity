package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Environment(EnvType.CLIENT)
public final class EnchantLore {
    private static final Identifier TOOLTIP_LAST_PHASE = Identifier.fromNamespaceAndPath("baity", "enchant_lore_last");
    private static final LoreCache LORE_CACHE = new LoreCache();
    private EnchantLore() {
    }

    public static void init() {
        EnchantCatalog.init();
        EnchantLoreColorSettings.initDefaults();
        EnchantLoreColorSettings.decode(ConfigManager.enchantLoreColorData);
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, TOOLTIP_LAST_PHASE);
        ItemTooltipCallback.EVENT.register(TOOLTIP_LAST_PHASE, EnchantLore::onTooltip);
    }

    public static void invalidateCache() {
        LORE_CACHE.invalidate();
    }

    public static Component tierPreview(Tier tier) {
        return EnchantLoreRender.formatTierPreview(tier, System.currentTimeMillis());
    }

    public static int tierButtonWidth(net.minecraft.client.gui.Font font) {
        return font.width(EnchantLoreRender.formatTierButtonSlotPreview(System.currentTimeMillis()).getVisualOrderText()) + 16;
    }

    public static int tierButtonHeight(net.minecraft.client.gui.Font font) {
        return font.lineHeight + 8;
    }

    private static void onTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipFlag flag,
            List<Component> lines
    ) {
        applyToTooltipLore(lines, stack);
    }

    static boolean isEnabled() {
        return ConfigManager.enchantLoreEnabled;
    }

    static boolean isDefaultLayout() {
        return !"compress".equalsIgnoreCase(ConfigManager.enchantLoreLayoutMode);
    }

    static void applyToTooltipLore(List<Component> lore, ItemStack stack) {
        if (!isEnabled() || lore == null || lore.isEmpty() || stack == null || stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!LocateUtils.inSkyBlock(mc)) {
            return;
        }
        if (LORE_CACHE.isCached(lore, stack)) {
            lore.clear();
            lore.addAll(LORE_CACHE.cachedAfter);
            return;
        }
        LORE_CACHE.updateBefore(lore, stack);
        EnchantLoreParser.Section section = EnchantLoreParser.findSection(lore, stack);
        if (section == null) {
            EnchantLoreNumeralApplier.applyToTooltip(lore, null, null);
            LORE_CACHE.updateAfter(lore, stack, true);
            return;
        }
        long nowMs = System.currentTimeMillis();
        if (isDefaultLayout()) {
            EnchantLoreParser.InPlaceResult inPlace = EnchantLoreParser.collectInPlace(lore, stack, section, nowMs);
            if (inPlace.enchants().isEmpty()) {
                EnchantLoreNumeralApplier.applyToTooltip(lore, null, null);
                LORE_CACHE.updateAfter(lore, stack, true);
                return;
            }
            for (int i = 0; i < inPlace.lines().size(); i++) {
                Component replacement = inPlace.lines().get(i);
                if (replacement != null) {
                    lore.set(section.start() + i, replacement);
                }
            }
            EnchantLoreNumeralApplier.applyToTooltip(lore, section.start(), section.end());
            LORE_CACHE.updateAfter(lore, stack, !hasAnimatedRainbow(inPlace.enchants()));
            return;
        }
        EnchantLoreParser.CollectResult collected = EnchantLoreParser.collectEnchants(lore, stack, section);
        if (collected.ordered().isEmpty()) {
            EnchantLoreNumeralApplier.applyToTooltip(lore, null, null);
            LORE_CACHE.updateAfter(lore, stack, true);
            return;
        }
        List<Component> insertLines = EnchantLoreRender.buildInsertLines(
                collected.ordered(),
                collected.hasLore(),
                section.maxTooltipWidth(),
                nowMs
        );
        int insertIndex = section.start();
        lore.subList(section.start(), section.end() + 1).clear();
        lore.addAll(Math.min(insertIndex, lore.size()), insertLines);
        int enchantStart = insertIndex;
        int enchantEnd = insertIndex + insertLines.size() - 1;
        EnchantLoreNumeralApplier.applyToTooltip(lore, enchantStart, enchantEnd);
        LORE_CACHE.updateAfter(lore, stack, !hasAnimatedRainbow(collected.ordered()));
    }

    private static boolean hasAnimatedRainbow(TreeSet<EnchantLoreParser.ParsedEnchant> ordered) {
        if (!isEnabled()) {
            return false;
        }
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            if (parsed.level >= parsed.def.maxLevel
                    && !parsed.def.ultimate
                    && !parsed.def.unknown
                    && EnchantLoreColorSettings.isRainbow(EnchantLore.Tier.PERFECT)) {
                return true;
            }
        }
        return false;
    }

    static int rainbowRgbAt(float x, float y, long nowMs) {
        double speed = Math.max(0.1, ConfigManager.enchantLoreRainbowSpeed);
        int cycleMs = (int) (4000.0 / speed);
        float time = 1.0f - (nowMs % cycleMs) / (float) cycleMs;
        int gradient = Math.max(1, ConfigManager.enchantLoreRainbowGradient);
        int angle = Math.max(1, Math.min(89, ConfigManager.enchantLoreRainbowAngle));
        double angleRad = Math.toRadians(angle);
        double coord = Math.abs(y + x * Math.tan(angleRad)) * Math.cos(angleRad);
        float saturation = (float) Math.min(1.0, Math.max(0.0, ConfigManager.enchantLoreRainbowSaturation));
        float hue = (float) positiveModulo((time % 1.0) + (coord % gradient) / gradient, 1.0);
        return net.minecraft.util.Mth.hsvToRgb(hue, saturation, 1.0f);
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }

    record Entry(EnchantCatalog.EnchantDef def, int level, Tier tier, boolean rainbow)
            implements Comparable<Entry> {
        String name() {
            return def.loreName;
        }
        boolean ultimate() {
            return def.ultimate;
        }
        @Override
        public int compareTo(Entry other) {
            return def.compareTo(other.def);
        }
    }

    public enum Tier {
        POOR,
        GOOD,
        GREAT,
        PERFECT,
        ULTIMATE
    }

    private static final class LoreCache {
        private List<Component> cachedBefore = List.of();
        private List<String> cachedBeforeStrings = List.of();
        private List<Component> cachedAfter = List.of();
        private ItemStack itemStack = ItemStack.EMPTY;
        private boolean cacheable;
        boolean isCached(List<Component> lore, ItemStack stack) {
            if (!cacheable || stack != itemStack || lore.size() != cachedBefore.size()) {
                return false;
            }
            for (int i = 0; i < lore.size(); i++) {
                Component current = lore.get(i);
                Component cached = cachedBefore.get(i);
                if (current == cached) {
                    continue;
                }
                if (!current.getString().equals(cachedBeforeStrings.get(i))) {
                    return false;
                }
            }
            return true;
        }
        void updateBefore(List<Component> lore, ItemStack stack) {
            cachedBefore = new ArrayList<>(lore);
            cachedBeforeStrings = new ArrayList<>(lore.size());
            for (Component line : lore) {
                cachedBeforeStrings.add(line.getString());
            }
            itemStack = stack;
            cachedAfter = List.of();
            cacheable = true;
        }
        void updateAfter(List<Component> lore, ItemStack stack, boolean cacheable) {
            this.cacheable = cacheable;
            cachedAfter = cacheable ? new ArrayList<>(lore) : List.of();
            itemStack = stack;
        }

        void invalidate() {
            cacheable = false;
            cachedAfter = List.of();
        }
    }
}
