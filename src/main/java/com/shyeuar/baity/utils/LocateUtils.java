package com.shyeuar.baity.utils;

import com.shyeuar.baity.mixin.PlayerListHudMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocateUtils {

    private static final Pattern SCOREBOARD_SKYBLOCK_TITLE = Pattern.compile(
            "SK[YI]BLOCK(?: CO-OP| GUEST)?(?: [♲☀Ⓑ])?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_SKYBLOCK_SHORT = Pattern.compile(
            "SK[YI]BLOCK",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_GUEST_TITLE = Pattern.compile(
            "SK[YI]BLOCK\\s+GUEST",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TAB_AREA_LINE = Pattern.compile(
            "^(?:Area|Island|Dungeon):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SB_SUBAREA_AFTER_LOC = Pattern.compile("⏣\\s*(?<area>.+)");
    private static final Pattern SB_SUBAREA_RIFT = Pattern.compile("\u0444\\s*(?<area>.+)");

    private static long cacheGameTime = Long.MIN_VALUE;
    private static boolean cachedOnHypixel;
    private static boolean cachedScoreboardSkyblock;
    private static boolean cachedSkyblockGuest;
    private static String cachedAreaIslandName = "";
    private static String cachedTabIslandName = "";
    private static String cachedScoreboardSubAreaName = "";

    private LocateUtils() {
    }

    public static boolean onHypixel(Minecraft mc) {
        refresh(mc);
        return cachedOnHypixel;
    }

    public static boolean inSkyBlock(Minecraft mc) {
        refresh(mc);
        return cachedOnHypixel && cachedScoreboardSkyblock;
    }

    public static boolean isSkyBlockGuest(Minecraft mc) {
        refresh(mc);
        return cachedSkyblockGuest;
    }

    public static boolean isOwnGarden(Minecraft mc) {
        refresh(mc);
        if (!cachedOnHypixel || !cachedScoreboardSkyblock || cachedSkyblockGuest) {
            return false;
        }
        String n = normalizeAreaName(cachedAreaIslandName);
        return "Garden".equalsIgnoreCase(n) || "The Garden".equalsIgnoreCase(n);
    }

    public static boolean isGalatea(Minecraft mc) {
        refresh(mc);
        if (!inSkyBlock(mc)) {
            return false;
        }
        String n = normalizeAreaName(cachedAreaIslandName);
        return n.contains("Galatea");
    }

    public static boolean isDungeonHub(Minecraft mc) {
        refresh(mc);
        return normalizeAreaName(cachedAreaIslandName).contains("Dungeon Hub");
    }

    public static boolean isInDungeonRun(Minecraft mc) {
        refresh(mc);
        if (!inSkyBlock(mc)) {
            return false;
        }
        if (isDungeonHub(mc)) {
            return false;
        }
        String n = normalizeAreaName(cachedAreaIslandName);
        if (n.contains("Catacombs")) {
            return true;
        }
        return cachedAreaIslandNameRawLineDungeonPrefix;
    }

    public static String areaIslandName(Minecraft mc) {
        refresh(mc);
        return cachedAreaIslandName;
    }

    public static String scoreboardSubAreaName(Minecraft mc) {
        refresh(mc);
        return cachedScoreboardSubAreaName;
    }

    public static List<String> readSidebarPlainLines(Minecraft mc) {
        return readSidebarPlainLines(mc, true);
    }

    public static List<String> readSidebarPlainLines(Minecraft mc, boolean trimEachLine) {
        List<String> out = new ArrayList<>();
        try {
            if (mc.level == null) {
                return out;
            }
            Scoreboard scoreboard = mc.level.getScoreboard();
            if (scoreboard == null) {
                return out;
            }
            Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebarObjective == null) {
                return out;
            }
            List<?> scores = tryGetSortedScores(scoreboard, sidebarObjective);
            if (scores == null || scores.isEmpty()) {
                return out;
            }
            for (Object scoreObj : scores) {
                String raw = extractScoreOwnerText(scoreObj);
                String clean = removeColorCodes(raw);
                if (trimEachLine) {
                    clean = clean.trim();
                }
                if (!clean.isEmpty()) {
                    out.add(clean);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static boolean inStillgoreChateau(Minecraft mc) {
        refresh(mc);
        String a = cachedScoreboardSubAreaName;
        return "Stillgore Château".equals(a) || "Oubliette".equals(a);
    }

    private static boolean cachedAreaIslandNameRawLineDungeonPrefix;

    private static void refresh(Minecraft mc) {
        if (mc.level == null) {
            clear();
            return;
        }
        long t = mc.level.getGameTime();
        if (cacheGameTime == t) {
            return;
        }
        cacheGameTime = t;

        cachedOnHypixel = false;
        cachedScoreboardSkyblock = false;
        cachedSkyblockGuest = false;
        cachedAreaIslandName = "";
        cachedTabIslandName = "";
        cachedScoreboardSubAreaName = "";
        cachedAreaIslandNameRawLineDungeonPrefix = false;

        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            String ip = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            cachedOnHypixel = ip.contains("hypixel.net") || ip.contains("hypixel");
        }

        String titlePlain = sidebarObjectivePlainTitle(mc);
        if (titlePlain != null && !titlePlain.isEmpty()) {
            String tPlain = removeColorCodes(titlePlain).trim();
            cachedScoreboardSkyblock = SCOREBOARD_SKYBLOCK_TITLE.matcher(tPlain).matches()
                    || SCOREBOARD_SKYBLOCK_SHORT.matcher(tPlain).find();
            cachedSkyblockGuest = SCOREBOARD_GUEST_TITLE.matcher(tPlain).find();
        }

        scanTabList(mc);
        cachedScoreboardSubAreaName = parseScoreboardSubArea(mc);
    }

    private static String parseScoreboardSubArea(Minecraft mc) {
        try {
            Scoreboard scoreboard = mc.level.getScoreboard();
            if (scoreboard == null) {
                return "";
            }
            Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebarObjective == null) {
                return "";
            }
            List<?> scores = tryGetSortedScores(scoreboard, sidebarObjective);
            if (scores == null || scores.isEmpty()) {
                return "";
            }
            for (Object scoreObj : scores) {
                String raw = extractScoreOwnerText(scoreObj);
                String clean = removeColorCodes(raw).trim();
                if (clean.isEmpty()) {
                    continue;
                }
                Matcher m = SB_SUBAREA_AFTER_LOC.matcher(clean);
                if (m.find()) {
                    String area = m.group("area");
                    if (area != null) {
                        return removeColorCodes(area).trim();
                    }
                }
                Matcher r = SB_SUBAREA_RIFT.matcher(clean);
                if (r.find()) {
                    String area = r.group("area");
                    if (area != null) {
                        return removeColorCodes(area).trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static List<?> tryGetSortedScores(Scoreboard scoreboard, Objective objective) {
        try {
            for (java.lang.reflect.Method m : scoreboard.getClass().getMethods()) {
                if (!"getSortedScores".equals(m.getName())) {
                    continue;
                }
                if (m.getParameterCount() != 1) {
                    continue;
                }
                try {
                    Object res = m.invoke(scoreboard, objective);
                    if (res instanceof List<?> list) {
                        return list;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private static String extractScoreOwnerText(Object scoreObj) {
        if (scoreObj == null) {
            return "";
        }
        try {
            for (String methodName : new String[]{"getOwner", "getName", "getPlayerName"}) {
                try {
                    java.lang.reflect.Method m = scoreObj.getClass().getMethod(methodName);
                    Object v = m.invoke(scoreObj);
                    if (v == null) {
                        continue;
                    }
                    try {
                        java.lang.reflect.Method getString = v.getClass().getMethod("getString");
                        Object s = getString.invoke(v);
                        if (s != null) {
                            return String.valueOf(s);
                        }
                    } catch (Exception ignored) {
                    }
                    if (v instanceof String) {
                        return (String) v;
                    }
                    return String.valueOf(v);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(scoreObj);
    }

    private static void scanTabList(Minecraft mc) {
        String area = null;
        String island = null;
        String dungeon = null;
        for (Component line : readSkyBlockTabListComponents(mc)) {
            String text = removeColorCodes(line.getString()).trim();
            if (text.isEmpty()) {
                continue;
            }
            var m = TAB_AREA_LINE.matcher(text);
            if (!m.matches()) {
                continue;
            }
            String isl = m.group(1);
            if (isl == null) {
                continue;
            }
            isl = isl.trim();
            String tl = text.toLowerCase(Locale.ROOT);
            if (tl.startsWith("area:") && area == null) {
                area = isl;
            } else if (tl.startsWith("island:") && island == null) {
                island = isl;
            } else if (tl.startsWith("dungeon:") && dungeon == null) {
                dungeon = isl;
            }
        }
        if (area != null) {
            cachedAreaIslandName = area;
            cachedAreaIslandNameRawLineDungeonPrefix = false;
        } else if (island != null) {
            cachedAreaIslandName = island;
            cachedAreaIslandNameRawLineDungeonPrefix = false;
        } else if (dungeon != null) {
            cachedAreaIslandName = dungeon;
            cachedAreaIslandNameRawLineDungeonPrefix = true;
        }
        cachedTabIslandName = island != null ? island : "";
    }

    private static String sidebarObjectivePlainTitle(Minecraft mc) {
        try {
            Scoreboard sb = mc.level.getScoreboard();
            if (sb == null) {
                return null;
            }
            Objective ob = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (ob == null) {
                return null;
            }
            return ob.getDisplayName().getString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeAreaName(String raw) {
        if (raw == null) {
            return "";
        }
        return removeColorCodes(raw).trim();
    }

    public static String toPlainText(String text) {
        if (text == null) {
            return "";
        }
        return removeColorCodes(text).trim();
    }
    
    public static List<String> readTabListDisplayPlainLines(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        for (Component line : readSkyBlockTabListComponents(mc)) {
            String plain = toPlainText(line.getString());
            if (!plain.isEmpty()) {
                lines.add(plain);
            }
        }
        return lines;
    }

    public static List<Component> readTabListDisplayComponents(Minecraft mc) {
        return readSkyBlockTabListComponents(mc);
    }

    public static List<Component> readSkyBlockTabListComponents(Minecraft mc) {
        List<Component> lines = new ArrayList<>();
        if (mc == null || mc.player == null || mc.getConnection() == null || mc.gui == null) {
            return lines;
        }
        PlayerTabOverlay tabOverlay = mc.gui.getTabList();
        if (tabOverlay == null) {
            return lines;
        }
        List<PlayerInfo> players = ((PlayerListHudMixin) tabOverlay).baity$getPlayerInfos();
        if (players == null || players.isEmpty()) {
            return lines;
        }
        players = new ArrayList<>(players);
        players.sort(LocateUtils::compareTabPlayers);
        for (PlayerInfo info : players) {
            Component display = tabOverlay.getNameForDisplay(info);
            if (display != null) {
                lines.add(display);
            }
        }
        if (lines.size() < 80 && !lines.isEmpty()) {
            lines.remove(lines.size() - 1);
        } else if (lines.size() > 80) {
            lines = new ArrayList<>(lines.subList(0, 80));
        }
        return lines;
    }

    private static int compareTabPlayers(PlayerInfo left, PlayerInfo right) {
        boolean leftSpectator = left.getGameMode() == GameType.SPECTATOR;
        boolean rightSpectator = right.getGameMode() == GameType.SPECTATOR;
        if (leftSpectator != rightSpectator) {
            return leftSpectator ? 1 : -1;
        }
        String leftTeam = left.getTeam() != null ? left.getTeam().getName() : "";
        String rightTeam = right.getTeam() != null ? right.getTeam().getName() : "";
        int teamOrder = leftTeam.compareTo(rightTeam);
        if (teamOrder != 0) {
            return teamOrder;
        }
        return left.getProfile().name().compareTo(right.getProfile().name());
    }

    public static List<String> readTabHudScanPlainLines(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (mc == null) {
            return lines;
        }
        if (mc.gui != null && mc.gui.getTabList() != null) {
            try {
                PlayerListHudMixin tab = (PlayerListHudMixin) mc.gui.getTabList();
                appendPlainLinesFromComponent(lines, tab.getHeader());
                appendPlainLinesFromComponent(lines, tab.getFooter());
            } catch (Exception ignored) {
            }
        }
        lines.addAll(readTabListDisplayPlainLines(mc));
        return lines;
    }

    private static void appendPlainLinesFromComponent(List<String> out, Component component) {
        if (component == null) {
            return;
        }
        String flat = toPlainText(component.getString());
        if (flat.isEmpty()) {
            return;
        }
        for (String segment : flat.split("\\R")) {
            String t = segment.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
    }

    public static String getTabListFooterPlainBestEffort(Minecraft mc) {
        if (mc == null || mc.gui == null || mc.gui.getTabList() == null) {
            return null;
        }
        try {
            Component footer = ((PlayerListHudMixin) mc.gui.getTabList()).getFooter();
            if (footer != null) {
                String s = toPlainText(footer.getString());
                if (!s.isEmpty()) {
                    return s;
                }
            }
        } catch (Exception ignored) {
        }
        if (mc.getConnection() != null) {
            for (var entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getTabListDisplayName() == null) {
                    continue;
                }
                String name = toPlainText(entry.getTabListDisplayName().getString());
                if (name.contains("Cookie Buff")) {
                    return name;
                }
            }
        }
        return null;
    }

    private static String removeColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        return text
                .replaceAll("(?i)\u00A7x(\u00A7[0-9a-f]){6}", "")
                .replaceAll("§[0-9a-fk-or]", "");
    }

    private static void clear() {
        cacheGameTime = Long.MIN_VALUE;
        cachedOnHypixel = false;
        cachedScoreboardSkyblock = false;
        cachedSkyblockGuest = false;
        cachedAreaIslandName = "";
        cachedTabIslandName = "";
        cachedScoreboardSubAreaName = "";
        cachedAreaIslandNameRawLineDungeonPrefix = false;
    }
}