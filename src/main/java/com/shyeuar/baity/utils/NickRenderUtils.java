package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.sync.BaityPresenceSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class NickRenderUtils {
    private static final long TARGET_CACHE_MS = 1000L;
    private static volatile long targetsCacheAt = 0L;
    private static volatile String cachePlayerName = "";
    private static volatile List<Target> cachedTargets = List.of();
    private static final ThreadLocal<Boolean> PREVIEW_OVERRIDE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> CLICK_GUI_RENDER_SCOPE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final int MATCH_CACHE_MAX = 2048;
    private static final Object MATCH_CACHE_LOCK = new Object();
    private static final LinkedHashMap<String, TargetMatch[]> MATCH_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TargetMatch[]> eldest) {
            return size() > MATCH_CACHE_MAX;
        }
    };

    private static final int REPLACEMENT_CACHE_MAX = 512;
    private static final Object REPLACEMENT_CACHE_LOCK = new Object();
    private static final LinkedHashMap<String, List<ReplacementCodepoint>> REPLACEMENT_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<ReplacementCodepoint>> eldest) {
            return size() > REPLACEMENT_CACHE_MAX;
        }
    };
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private NickRenderUtils() {
    }

    public static void invalidateLocalTargetsCache() {
        targetsCacheAt = 0L;
        cachePlayerName = "";
        cachedTargets = List.of();
    }

    public static long getTargetsCacheAt() {
        return targetsCacheAt;
    }

    private static boolean isBaityClickGuiScreen() {
        if (Boolean.TRUE.equals(PREVIEW_OVERRIDE.get())) {
            return false;
        }
        return Boolean.TRUE.equals(CLICK_GUI_RENDER_SCOPE.get());
    }
    

    public static String handleString(String text) {
        if (text == null || text.isEmpty()) return text;
        if (isBaityClickGuiScreen()) return text;
        List<Target> targets = collectTargets();
        if (targets.isEmpty()) return text;

        long version = targetsCacheAt;
        String cacheKey = version + "|" + text;
        TargetMatch[] matchByIndex;
        int[] codePoints = null;
        synchronized (MATCH_CACHE_LOCK) {
            matchByIndex = MATCH_CACHE.get(cacheKey);
        }

        if (matchByIndex == null) {
            codePoints = text.codePoints().toArray();
            if (codePoints.length == 0) return text;

            String lower = text.toLowerCase(Locale.ROOT);
            List<Target> matchingTargets = null;
            for (Target target : targets) {
                if (!lower.contains(target.nameLower())) continue;
                if (matchingTargets == null) matchingTargets = new ArrayList<>();
                matchingTargets.add(target);
            }
            if (matchingTargets == null || matchingTargets.isEmpty()) return text;

            matchByIndex = matchTargets(codePoints, matchingTargets);
            synchronized (MATCH_CACHE_LOCK) {
                MATCH_CACHE.put(cacheKey, matchByIndex);
            }
        }

        boolean matched = false;
        for (TargetMatch value : matchByIndex) {
            if (value != null) {
                matched = true;
                break;
            }
        }
        if (!matched) return text;

        if (codePoints == null) codePoints = text.codePoints().toArray();
        java.util.ArrayList<Glyph> glyphs = new java.util.ArrayList<>(codePoints.length);
        for (int cp : codePoints) glyphs.add(new Glyph(cp, Style.EMPTY));

        String out = applySectionColorToStringWithReplacement(text, glyphs, matchByIndex);
        return out;
    }

    public static void beginPreviewOverride() {
        PREVIEW_OVERRIDE.set(Boolean.TRUE);
    }
    public static void endPreviewOverride() {
        PREVIEW_OVERRIDE.set(Boolean.FALSE);
    }

    public static void beginClickGuiRenderScope() {
        CLICK_GUI_RENDER_SCOPE.set(Boolean.TRUE);
    }

    public static void endClickGuiRenderScope() {
        CLICK_GUI_RENDER_SCOPE.set(Boolean.FALSE);
    }

    

    public static FormattedText handleFormattedText(FormattedText text) {
        if (text == null) return null;
        if (isBaityClickGuiScreen()) return text;
        List<Target> targets = collectTargets();
        if (targets.isEmpty()) return text;
        StringBuilder sbPlain = new StringBuilder();
        List<Glyph> originalGlyphs = new ArrayList<>();
        try {
            text.visit((style, str) -> {
                if (str == null || str.isEmpty()) return java.util.Optional.empty();
                for (int i = 0; i < str.length();) {
                    int cp = str.codePointAt(i);
                    sbPlain.appendCodePoint(cp);
                    originalGlyphs.add(new Glyph(cp, style));
                    i += Character.charCount(cp);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        } catch (Throwable ignore) {
            String plainFallback = text.toString();
            for (int i = 0; i < plainFallback.length();) {
                int cp = plainFallback.codePointAt(i);
                sbPlain.appendCodePoint(cp);
                originalGlyphs.add(new Glyph(cp, Style.EMPTY));
                i += Character.charCount(cp);
            }
        }
        String plain = sbPlain.toString();
        if (plain.isEmpty()) return text;

        long version = targetsCacheAt;
        String cacheKey = version + "|" + plain;
        TargetMatch[] matchByIndex;
        synchronized (MATCH_CACHE_LOCK) {
            matchByIndex = MATCH_CACHE.get(cacheKey);
        }
        if (matchByIndex == null) {
            int[] codePoints = plain.codePoints().toArray();
            if (codePoints.length == 0) return text;
            String lower = plain.toLowerCase(Locale.ROOT);
            List<Target> matchingTargets = null;
            for (Target target : targets) {
                if (!lower.contains(target.nameLower())) continue;
                if (matchingTargets == null) matchingTargets = new ArrayList<>();
                matchingTargets.add(target);
            }
            if (matchingTargets == null || matchingTargets.isEmpty()) return text;
            matchByIndex = matchTargets(codePoints, matchingTargets);
            synchronized (MATCH_CACHE_LOCK) {
                MATCH_CACHE.put(cacheKey, matchByIndex);
            }
        }
        boolean matched = false;
        for (TargetMatch value : matchByIndex) {
            if (value != null) {
                matched = true;
                break;
            }
        }
        if (!matched) return text;

        List<Glyph> outGlyphs = applyReplacementAndStyle(originalGlyphs, matchByIndex, System.currentTimeMillis());
        if (outGlyphs.isEmpty()) return text;

        net.minecraft.network.chat.MutableComponent result = Component.empty();
        StringBuilder run = new StringBuilder();
        Style current = outGlyphs.get(0).style();
        for (Glyph g : outGlyphs) {
            if (!g.style().equals(current)) {
                if (run.length() > 0) {
                    result = result.append(Component.literal(run.toString()).setStyle(current));
                    run.setLength(0);
                }
                current = g.style();
            }
            run.appendCodePoint(g.codepoint());
        }
        if (run.length() > 0) {
            result = result.append(Component.literal(run.toString()).setStyle(current));
        }
        return result;
    }

    public static Component handleComponent(Component component) {
        return component;
    }


    public static FormattedCharSequence handleCharSequence(FormattedCharSequence original) {
        if (original == null) return null;
        if (isBaityClickGuiScreen()) return original;
        List<Target> targets = collectTargets();
        if (targets.isEmpty()) return original;

        StringBuilder sbPlain = new StringBuilder();
        original.accept((index, style, codepoint) -> {
            sbPlain.appendCodePoint(codepoint);
            return true;
        });
        String plain = sbPlain.toString();
        if (plain.isEmpty()) return original;

        long version = targetsCacheAt;
        String cacheKey = version + "|" + plain;
        TargetMatch[] matchByIndex;
        synchronized (MATCH_CACHE_LOCK) {
            matchByIndex = MATCH_CACHE.get(cacheKey);
        }

        if (matchByIndex == null) {
            int[] codePoints = plain.codePoints().toArray();

            String lower = plain.toLowerCase(Locale.ROOT);
            List<Target> matchingTargets = null;
            for (Target target : targets) {
                if (!lower.contains(target.nameLower())) continue;
                if (matchingTargets == null) matchingTargets = new ArrayList<>();
                matchingTargets.add(target);
            }
            if (matchingTargets == null || matchingTargets.isEmpty()) return original;

            matchByIndex = matchTargets(codePoints, matchingTargets);
            synchronized (MATCH_CACHE_LOCK) {
                MATCH_CACHE.put(cacheKey, matchByIndex);
            }
        }

        boolean matched = false;
        for (TargetMatch value : matchByIndex) {
            if (value != null) {
                matched = true;
                break;
            }
        }
        if (!matched) return original;

        long nowMs = System.currentTimeMillis();

        List<Glyph> glyphs = new ArrayList<>();
        original.accept((index, style, codepoint) -> {
            glyphs.add(new Glyph(codepoint, style));
            return true;
        });
        if (glyphs.isEmpty()) return original;

        List<Glyph> outGlyphs = applyReplacementAndStyle(glyphs, matchByIndex, nowMs);
        List<FormattedCharSequence> out = new ArrayList<>(outGlyphs.size());
        for (Glyph glyph : outGlyphs) {
            out.add(FormattedCharSequence.codepoint(glyph.codepoint(), glyph.style()));
        }
        FormattedCharSequence result = FormattedCharSequence.composite(out);
        return result;
    }

    private static String applySectionColorToStringWithReplacement(String text, List<Glyph> glyphs, TargetMatch[] matchByIndex) {
        List<Glyph> outGlyphs = applyReplacementAndStyle(glyphs, matchByIndex, System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (Glyph glyph : outGlyphs) {
            if (glyph.style().getColor() != null) {
                appendHexColor(sb, glyph.style().getColor().getValue());
            } else {
                sb.append('\u00A7').append('r');
            }
            if (glyph.style().isBold()) {
                sb.append('\u00A7').append('l');
            }
            sb.appendCodePoint(glyph.codepoint());
        }
        return sb.toString();
    }

    private static List<Glyph> applyReplacementAndStyle(List<Glyph> glyphs, TargetMatch[] matchByIndex, long nowMs) {
        List<Glyph> out = new ArrayList<>(glyphs.size() + 16);
        int i = 0;
        while (i < glyphs.size()) {
            TargetMatch match = matchByIndex[i];
            if (match == null || i != match.start()) {
                out.add(glyphs.get(i));
                i++;
                continue;
            }
            Target target = match.target();
            List<ReplacementCodepoint> replacement = target.replacementCodepoints();
            if (replacement.isEmpty()) {
                i += match.length();
                continue;
            }
            Style base = glyphs.get(i).style();
            int len = replacement.size();
            for (int idx = 0; idx < len; idx++) {
                ReplacementCodepoint cp = replacement.get(idx);
                Style style = base;
                if (target.bold()) {
                    style = style.withBold(true);
                }
                if (cp.explicitColor() != null) {
                    style = style.withColor(cp.explicitColor());
                } else {
                    int rgb = target.colorAt(len == 1 ? 0.0 : (double) idx / (len - 1), nowMs);
                    if (rgb >= 0) {
                        style = style.withColor(rgb);
                    }
                }
                out.add(new Glyph(cp.codepoint(), style));
            }
            i += match.length();
        }
        return out;
    }

    private static void appendHexColor(StringBuilder sb, int rgb) {
        int v = rgb & 0xFFFFFF;
        sb.append('\u00A7').append('x');
        sb.append('\u00A7').append(HEX[(v >> 20) & 0xF]);
        sb.append('\u00A7').append(HEX[(v >> 16) & 0xF]);
        sb.append('\u00A7').append(HEX[(v >> 12) & 0xF]);
        sb.append('\u00A7').append(HEX[(v >> 8) & 0xF]);
        sb.append('\u00A7').append(HEX[(v >> 4) & 0xF]);
        sb.append('\u00A7').append(HEX[v & 0xF]);
    }

    private static TargetMatch[] matchTargets(int[] codePoints, List<Target> targets) {
        TargetMatch[] out = new TargetMatch[codePoints.length];

        int[] lowerCodePoints = new int[codePoints.length];
        for (int i = 0; i < codePoints.length; i++) {
            lowerCodePoints[i] = Character.toLowerCase(codePoints[i]);
        }

        for (Target target : targets) {
            int[] targetLower = target.codePointsLower();
            int tLen = targetLower.length;
            if (tLen == 0 || tLen > codePoints.length) continue;
            int targetFirstLower = target.codePointsFirstLower();

            for (int i = 0; i <= codePoints.length - tLen; i++) {
                if (lowerCodePoints[i] != targetFirstLower) continue;
                if (!isRangeFree(out, i, tLen)) continue;

                boolean ok = true;
                for (int j = 0; j < tLen; j++) {
                    if (lowerCodePoints[i + j] != targetLower[j]) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;

                int end = i + tLen;
                boolean leftBoundary = i == 0 || !isNameCodepoint(codePoints[i - 1]);
                boolean rightBoundary = end >= codePoints.length || !isNameCodepoint(codePoints[end]);
                if (!leftBoundary || !rightBoundary) continue;

                TargetMatch match = new TargetMatch(target, i, tLen);
                for (int j = 0; j < tLen; j++) {
                    out[i + j] = match;
                }
            }
        }
        return out;
    }

    private static boolean isRangeFree(TargetMatch[] matches, int start, int len) {
        for (int i = 0; i < len; i++) {
            if (matches[start + i] != null) return false;
        }
        return true;
    }

    private static boolean isNameCodepoint(int codepoint) {
        return Character.isLetterOrDigit(codepoint) || codepoint == '_';
    }

    private static List<Target> collectTargets() {
        Module module = ModuleManager.getModuleByName("NickTweaks");
        if (module == null || !module.isEnabled()) return List.of();

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return List.of();
        String selfName = client.player.getName().getString();
        long now = System.currentTimeMillis();
        if (now - targetsCacheAt <= TARGET_CACHE_MS && selfName.equals(cachePlayerName)) {
            return cachedTargets;
        }

        List<Target> targets = new ArrayList<>();
        if (selfName != null && !selfName.isBlank()) {
            targets.add(Target.local(selfName));
        }

        final int[] visitedPlayers = {0};
        final int[] chromaProfileTargets = {0};
        if (client.level != null) {
            client.level.players().forEach(player -> {
                String name = player.getName().getString();
                if (name == null || name.isBlank()) return;
                BaityPresenceSync.ChromaProfile profile = BaityPresenceSync.getChromaProfileByName(name);
                if (profile == null) return;
                targets.add(Target.remote(name, profile));
                visitedPlayers[0]++;
                chromaProfileTargets[0]++;
            });
        }

        targets.removeIf(t -> t.name().isBlank());
        targets.sort(Comparator.comparingInt((Target t) -> t.codePoints().length).reversed());
        cachedTargets = List.copyOf(targets);
        cachePlayerName = selfName;
        targetsCacheAt = now;
        return cachedTargets;
    }

    private static int lerpRgb(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = (int) Math.round(ar + (br - ar) * t);
        int rg = (int) Math.round(ag + (bg - ag) * t);
        int rb = (int) Math.round(ab + (bb - ab) * t);
        return (rr << 16) | (rg << 8) | rb;
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Glyph(int codepoint, Style style) {
    }

    private record TargetMatch(Target target, int start, int length) {
    }

    private record Target(
            String name,
            String nameLower,
            int[] codePoints,
            int[] codePointsLower,
            int codePointsFirstLower,
            String displayName,
            boolean local,
            boolean bold,
            int[] palette,
            double speed,
            boolean remoteChromaEnabled,
            boolean customNickColorEnabled,
            int remoteGradientStart,
            int remoteGradientEnd
    ) {
        static Target local(String name) {
            int[] cps = name.codePoints().toArray();
            int[] lower = new int[cps.length];
            for (int i = 0; i < cps.length; i++) lower[i] = Character.toLowerCase(cps[i]);
            return new Target(
                    name,
                    name.toLowerCase(Locale.ROOT),
                    cps,
                    lower,
                    lower.length == 0 ? 0 : lower[0],
                    null,
                    true,
                    ConfigManager.nickTweaksBoldSelf,
                    new int[0],
                    clamp(ConfigManager.nickTweaksChromaSpeed, 0.0, 8.0),
                    false,
                    ConfigManager.nickTweaksCustomNickColorEnabled,
                    0,
                    0
            );
        }

        static Target remote(String name, BaityPresenceSync.ChromaProfile profile) {
            int[] cps = name.codePoints().toArray();
            int[] lower = new int[cps.length];
            for (int i = 0; i < cps.length; i++) lower[i] = Character.toLowerCase(cps[i]);
            return new Target(
                    name,
                    name.toLowerCase(Locale.ROOT),
                    cps,
                    lower,
                    lower.length == 0 ? 0 : lower[0],
                    profile.nickChanger(),
                    false,
                    profile.boldSelf(),
                    profile.paletteView(),
                    clamp(profile.speed(), 0.0, 8.0),
                    profile.chromaEnabled(),
                    profile.customNickColorEnabled(),
                    profile.gradientStart(),
                    profile.gradientEnd()
            );
        }

        int colorAt(double progress, long nowMs) {
            if (!local) {
                if (remoteChromaEnabled && palette.length > 0) {
                    double phase = (nowMs / 1000.0) * speed;
                    double position = positiveModulo(progress + phase, 1.0) * palette.length;
                    int fromIndex = Math.floorMod((int) Math.floor(position), palette.length);
                    int toIndex = Math.floorMod(fromIndex + 1, palette.length);
                    double frac = position - Math.floor(position);
                    return lerpRgb(palette[fromIndex], palette[toIndex], frac);
                }
                if (!customNickColorEnabled) {
                    return -1;
                }
                if (remoteGradientStart == remoteGradientEnd) {
                    return remoteGradientStart;
                }
                return lerpRgb(remoteGradientStart, remoteGradientEnd, progress);
            }

            if (!ConfigManager.nickTweaksChromaEnabled) {
                if (!ConfigManager.nickTweaksCustomNickColorEnabled) {
                    return -1;
                }
                int localStart = ConfigManager.nickTweaksGradientStartColor & 0xFFFFFF;
                int localEnd = ConfigManager.nickTweaksGradientEndColor & 0xFFFFFF;
                if (localStart == localEnd) {
                    return localStart;
                }
                return lerpRgb(
                        localStart,
                        localEnd,
                        progress
                );
            }

            double lightness = clamp(ConfigManager.nickTweaksChromaLightness, 0.2, 1.0);
            double chroma = clamp(ConfigManager.nickTweaksChromaChroma, 0.0, 0.4);
            double size = Math.max(0.1, ConfigManager.nickTweaksChromaSize);
            double phase = (nowMs / 1000.0) * (speed * 0.5);
            float saturation = (float) (chroma / 0.4);
            float hue = (float) positiveModulo((progress / size) - phase, 1.0);
            return Mth.hsvToRgb(hue, saturation, (float) lightness);
        }

        List<ReplacementCodepoint> replacementCodepoints() {
            String raw;
            if (local) {
                raw = ConfigManager.nickTweaksNickChanger;
                if (raw == null || raw.isBlank()) raw = name;
            } else {
                raw = displayName == null || displayName.isBlank() ? name : displayName;
            }

            List<ReplacementCodepoint> cached;
            synchronized (REPLACEMENT_CACHE_LOCK) {
                cached = REPLACEMENT_CACHE.get(raw);
            }
            if (cached != null) return cached;
            List<ReplacementCodepoint> parsed = parseCustomNick(raw);
            synchronized (REPLACEMENT_CACHE_LOCK) {
                REPLACEMENT_CACHE.put(raw, parsed);
            }
            return parsed;
        }
    }


    private static List<ReplacementCodepoint> parseCustomNick(String raw) {
        ArrayList<ReplacementCodepoint> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        Integer explicitColor = null;
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int cpl = Character.charCount(cp);
            if (cp == '&' && i + cpl < raw.length()) {
                int ncp = raw.codePointAt(i + cpl);
                char ch = Character.toLowerCase((char) ncp);
                Integer mapped = mapLegacyColor(ch);
                if (mapped != null) {
                    explicitColor = mapped;
                    i += cpl + Character.charCount(ncp);
                    continue;
                }
                if (ch == 'r') {
                    explicitColor = null;
                    i += cpl + Character.charCount(ncp);
                    continue;
                }
            }
            out.add(new ReplacementCodepoint(cp, explicitColor));
            i += cpl;
        }
        return out;
    }

    private static Integer mapLegacyColor(char c) {
        return switch (c) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> null;
        };
    }

    private record ReplacementCodepoint(int codepoint, Integer explicitColor) {
    }
}

