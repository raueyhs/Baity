package com.shyeuar.baity.utils;

import com.shyeuar.baity.compatibility.HypixelModApiLocation;
import com.shyeuar.baity.features.sidepanel.SidePanel;
import com.shyeuar.baity.mixin.PlayerListHudMixin;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocateUtils {

    private static final Pattern HYPIXEL_SERVER_BRAND = Pattern.compile(
            ".*Hypixel BungeeCord.*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_SKYBLOCK_TITLE = Pattern.compile(
            "SK[YI]BLOCK(?: CO-OP| GUEST)?(?: [\u2672\u2600\u24b7])?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_SKYBLOCK_SHORT = Pattern.compile(
            "SK[YI]BLOCK",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_GUEST_TITLE = Pattern.compile(
            "SK[YI]BLOCK\\s+GUEST",
            Pattern.CASE_INSENSITIVE);
    private static final String[] SKYBLOCK_TITLE_PREFIXES = {
            "SKYBLOCK",
            "\u7a7a\u5c9b\u751f\u5b58",
            "\u7a7a\u5cf6\u751f\u5b58"
    };
    private static final long SKYBLOCK_LEAVE_GRACE_MS = 10_000L;
    private static final Pattern TAB_AREA_LINE = Pattern.compile(
            "^(?:Area|Island|Dungeon):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SB_SUBAREA_AFTER_LOC = Pattern.compile("\u23e3\\s*(?<area>.+)");
    private static final Pattern SB_SUBAREA_RIFT = Pattern.compile("\u0444\\s*(?<area>.+)");
    private static final Set<String> GALATEA_SUBAREAS = Set.of(
            "moonglade marsh",
            "tangleburg",
            "tangleburg bank",
            "tangleburg library",
            "tangleburg's path",
            "evergreen plateau",
            "north reaches",
            "south reaches",
            "west reaches",
            "verdant summit",
            "murkwater loch",
            "murkwater shallows",
            "murkwater depths",
            "murkwater outpost",
            "north wetlands",
            "south wetlands",
            "westbound wetlands",
            "moonglade's edge",
            "wyrmgrove tomb",
            "tomb floodway",
            "fusion house",
            "tranquil pass",
            "tranquility sanctum",
            "side-ember way",
            "stride-ember fissure",
            "dive-ember pass",
            "driptoad delve",
            "kelpwoven tunnels",
            "swampcut inc.",
            "red house",
            "bubbleboost column"
    );
    private static final Set<String> TORRHUS_SUBAREAS = Set.of(
            "torrhus canyon",
            "spring path",
            "torrhus springs",
            "spring shallows",
            "spring depths",
            "torrhus heights",
            "miria's hut",
            "critter safari entrance",
            "safari zone entrance",
            "ant's cave",
            "hotspot haven",
            "desert temple",
            "goblin hideaway"
    );

    private static long cacheGameTime = Long.MIN_VALUE;
    private static long lastFoundScoreboardMs = -1L;
    private static boolean stickyOnSkyblock;
    private static boolean cachedOnHypixel;
    private static boolean cachedScoreboardSkyblock;
    private static boolean cachedSkyblockGuest;
    private static String cachedAreaIslandName = "";
    private static String cachedTabIslandName = "";
    private static String cachedScoreboardSubAreaName = "";
    private static boolean cachedInRift;
    private static boolean cachedInSafari;
    private static boolean cachedOnGalatea;
    private static boolean cachedOnTorrhus;
    private static boolean cachedInCrystalHollows;
    private static String crystalHollowsLocateKey = "";
    private static String shulkerIslandLocateKey = "";

    private static final String CRYSTAL_HOLLOWS_MODE = "crystal_hollows";
    private static final long LOCRAW_MIN_INTERVAL_MS = 3000L;
    private static final long LOCRAW_PENDING_TIMEOUT_MS = 5000L;
    private static final long LOCRAW_STALE_MS = 10000L;

    private static String locrawServer = "";
    private static String locrawGametype = "";
    private static String locrawMode = "";
    private static long lastLocrawRequestMs;
    private static long lastLocrawReplyMs;
    private static boolean locrawPending;

    private LocateUtils() {
    }

    public static void registerClientEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetShulkerIslandLocateCache());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetShulkerIslandLocateCache());
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleLocrawReply(message.getString()));
        HypixelModApiLocation.init();
    }

    public static boolean isCrystalHollowsMode() {
        String mode = effectiveIslandMode();
        return !mode.isEmpty() && CRYSTAL_HOLLOWS_MODE.equalsIgnoreCase(mode);
    }

    public static String effectiveIslandMode() {
        String pushed = HypixelModApiLocation.mode();
        return pushed.isEmpty() ? locrawMode : pushed;
    }

    public static String locrawMode() {
        return locrawMode;
    }

    private static void handleLocrawReply(String raw) {
        if (raw == null) {
            return;
        }
        String text = removeColorCodes(raw).trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(text.substring(start, end + 1)).getAsJsonObject();
            locrawServer = optString(object, "server");
            locrawGametype = optString(object, "gametype");
            locrawMode = optString(object, "mode");
            lastLocrawReplyMs = System.currentTimeMillis();
            locrawPending = false;
        } catch (Exception ignored) {
        }
    }

    private static String optString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static void maybeRequestLocraw(Minecraft mc, boolean islandKeyChanged) {
        if (!cachedOnHypixel || !cachedScoreboardSkyblock || mc.player == null || mc.player.connection == null) {
            return;
        }
        if (HypixelModApiLocation.hasData()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean stale = locrawMode.isEmpty() && now - lastLocrawReplyMs > LOCRAW_STALE_MS;
        if (!islandKeyChanged && !stale) {
            return;
        }
        if (now - lastLocrawRequestMs < LOCRAW_MIN_INTERVAL_MS) {
            return;
        }
        if (locrawPending && now - lastLocrawRequestMs < LOCRAW_PENDING_TIMEOUT_MS) {
            return;
        }
        lastLocrawRequestMs = now;
        locrawPending = true;
        mc.player.connection.sendCommand("locraw");
    }

    private static void resetShulkerIslandLocateCache() {
        shulkerIslandLocateKey = "";
        crystalHollowsLocateKey = "";
        cachedOnGalatea = false;
        cachedOnTorrhus = false;
        cachedInCrystalHollows = false;
        locrawServer = "";
        locrawGametype = "";
        locrawMode = "";
        lastLocrawRequestMs = 0L;
        lastLocrawReplyMs = 0L;
        locrawPending = false;
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
        return cachedOnGalatea;
    }

    public static boolean isTorrhusCanyon(Minecraft mc) {
        refresh(mc);
        return cachedOnTorrhus;
    }

    public static boolean isInCrystalHollows(Minecraft mc) {
        refresh(mc);
        if (isCrystalHollowsMode()) {
            return true;
        }
        return cachedInCrystalHollows;
    }

    private static void updateCrystalHollowsFlag(Minecraft mc) {
        if (!cachedScoreboardSkyblock || !cachedOnHypixel) {
            crystalHollowsLocateKey = "";
            cachedInCrystalHollows = false;
            return;
        }
        String key = buildShulkerIslandLocateKey();
        if (key.equals(crystalHollowsLocateKey)) {
            return;
        }
        crystalHollowsLocateKey = key;
        cachedInCrystalHollows = false;
        maybeRequestLocraw(mc, true);

        if (isCrystalHollowsLocationName(cachedAreaIslandName)
                || isCrystalHollowsLocationName(cachedTabIslandName)
                || isCrystalHollowsLocationName(cachedScoreboardSubAreaName)) {
            cachedInCrystalHollows = true;
            return;
        }

        for (String line : readTabHudScanPlainLines(mc)) {
            Matcher tabLine = TAB_AREA_LINE.matcher(line);
            if (tabLine.matches()) {
                String label = line.toLowerCase(Locale.ROOT);
                if (label.startsWith("island:") || label.startsWith("area:")
                        || label.startsWith("dungeon:")) {
                    if (isCrystalHollowsLocationName(tabLine.group(1))) {
                        cachedInCrystalHollows = true;
                        return;
                    }
                }
            } else if (isCrystalHollowsLocationName(line)) {
                cachedInCrystalHollows = true;
                return;
            }
        }

        for (String line : readSidebarPlainLines(mc, true)) {
            if (isCrystalHollowsLocationName(line)) {
                cachedInCrystalHollows = true;
                return;
            }
            Matcher subArea = SB_SUBAREA_AFTER_LOC.matcher(line);
            if (subArea.find() && isCrystalHollowsLocationName(subArea.group("area"))) {
                cachedInCrystalHollows = true;
                return;
            }
        }
    }

    private static boolean isCrystalHollowsLocationName(String raw) {
        String n = normalizeAreaName(raw);
        if (n.isEmpty()) {
            return false;
        }
        String lower = n.toLowerCase(Locale.ROOT);
        return lower.contains("crystal hollows") || lower.contains("crystal nucleus");
    }

    private static boolean isGalateaLocationName(String raw) {
        String n = normalizeAreaName(raw);
        if (n.isEmpty()) {
            return false;
        }
        String lower = n.toLowerCase(Locale.ROOT);
        if (lower.contains("galatea")) {
            return true;
        }
        if (GALATEA_SUBAREAS.contains(lower)) {
            return true;
        }
        return lower.contains("moonglade")
                || lower.contains("tangleburg")
                || lower.contains("murkwater")
                || lower.contains("wyrmgrove");
    }

    private static boolean isTorrhusLocationName(String raw) {
        String n = normalizeAreaName(raw);
        if (n.isEmpty()) {
            return false;
        }
        String lower = n.toLowerCase(Locale.ROOT);
        if (lower.contains("torrhus")) {
            return true;
        }
        if (TORRHUS_SUBAREAS.contains(lower)) {
            return true;
        }
        return lower.contains("spring path")
                || lower.contains("spring shallows")
                || lower.contains("spring depths")
                || lower.contains("miria's hut")
                || lower.contains("safari zone")
                || lower.contains("critter safari");
    }

    public static boolean isHub(Minecraft mc) {
        refresh(mc);
        if (!inSkyBlock(mc) || isDungeonHub(mc)) {
            return false;
        }
        String area = normalizeAreaName(cachedAreaIslandName);
        return "Hub".equalsIgnoreCase(area);
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

    public static boolean isInRift(Minecraft mc) {
        refresh(mc);
        return cachedInRift;
    }

    public static boolean isInSafari(Minecraft mc) {
        refresh(mc);
        return cachedInSafari;
    }

    public static SidePanel.Island panelIsland(Minecraft mc) {
        refresh(mc);
        if (cachedInRift) {
            return SidePanel.Island.RIFT;
        }
        if (cachedInSafari) {
            return SidePanel.Island.SAFARI;
        }
        return SidePanel.Island.MAIN;
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
        return "Stillgore Ch\u00e2teau".equals(a) || "Oubliette".equals(a);
    }

    private static boolean cachedAreaIslandNameRawLineDungeonPrefix;

    private static void refresh(Minecraft mc) {
        if (mc.level == null || mc.isLocalServer()) {
            clear();
            return;
        }
        long t = mc.level.getGameTime();
        if (cacheGameTime == t) {
            return;
        }
        cacheGameTime = t;

        cachedOnHypixel = detectOnHypixel(mc);
        cachedScoreboardSkyblock = false;
        cachedSkyblockGuest = false;
        cachedAreaIslandName = "";
        cachedTabIslandName = "";
        cachedScoreboardSubAreaName = "";
        cachedAreaIslandNameRawLineDungeonPrefix = false;
        cachedInRift = false;
        cachedInSafari = false;

        boolean foundScoreboard = false;
        boolean foundSkyblockTitle = false;
        if (cachedOnHypixel) {
            String titlePlain = sidebarObjectivePlainTitle(mc);
            if (titlePlain != null && !titlePlain.isEmpty()) {
                foundScoreboard = true;
                lastFoundScoreboardMs = System.currentTimeMillis();
                String strippedTitle = removeColorCodes(titlePlain).trim();
                foundSkyblockTitle = isSkyblockScoreboardTitle(strippedTitle);
                cachedSkyblockGuest = strippedTitle.endsWith("GUEST")
                        || SCOREBOARD_GUEST_TITLE.matcher(strippedTitle).find();
            }
        }

        if (foundSkyblockTitle) {
            stickyOnSkyblock = true;
        } else if (stickyOnSkyblock
                && (foundScoreboard || System.currentTimeMillis() - lastFoundScoreboardMs > SKYBLOCK_LEAVE_GRACE_MS)) {
            stickyOnSkyblock = false;
        }
        cachedScoreboardSkyblock = stickyOnSkyblock && cachedOnHypixel;

        scanTabList(mc);
        cachedScoreboardSubAreaName = parseScoreboardSubArea(mc);
        updatePanelIslandFlags(mc);
        updateCrystalHollowsFlag(mc);
        updateShulkerIslandFlags(mc);
    }

    private static String buildShulkerIslandLocateKey() {
        if (!cachedScoreboardSkyblock || !cachedOnHypixel) {
            return "";
        }
        return cachedAreaIslandName + "\0" + cachedTabIslandName + "\0" + cachedScoreboardSubAreaName;
    }

    private static void updateShulkerIslandFlags(Minecraft mc) {
        if (!cachedScoreboardSkyblock || !cachedOnHypixel) {
            resetShulkerIslandLocateCache();
            return;
        }
        String key = buildShulkerIslandLocateKey();
        if (key.equals(shulkerIslandLocateKey)) {
            return;
        }
        shulkerIslandLocateKey = key;
        cachedOnGalatea = false;
        cachedOnTorrhus = false;

        markShulkerIslandsFromName(cachedAreaIslandName);
        markShulkerIslandsFromName(cachedTabIslandName);
        markShulkerIslandsFromName(cachedScoreboardSubAreaName);
        if (cachedOnGalatea && cachedOnTorrhus) {
            return;
        }
        for (String line : readTabHudScanPlainLines(mc)) {
            Matcher tabLine = TAB_AREA_LINE.matcher(line);
            if (tabLine.matches()) {
                String label = line.toLowerCase(Locale.ROOT);
                String value = tabLine.group(1);
                if (label.startsWith("island:") || label.startsWith("area:")) {
                    if (!cachedOnGalatea && isGalateaLocationName(value)) {
                        cachedOnGalatea = true;
                    }
                    if (!cachedOnTorrhus && isTorrhusLocationName(value)) {
                        cachedOnTorrhus = true;
                    }
                }
            } else {
                if (!cachedOnGalatea && isGalateaLocationName(line)) {
                    cachedOnGalatea = true;
                }
                if (!cachedOnTorrhus && isTorrhusLocationName(line)) {
                    cachedOnTorrhus = true;
                }
            }
            if (cachedOnGalatea && cachedOnTorrhus) {
                return;
            }
        }
        for (String line : readSidebarPlainLines(mc, true)) {
            if (!cachedOnGalatea && line.contains("Galatea")) {
                cachedOnGalatea = true;
            }
            if (!cachedOnTorrhus && line.contains("Torrhus")) {
                cachedOnTorrhus = true;
            }
            Matcher subArea = SB_SUBAREA_AFTER_LOC.matcher(line);
            if (subArea.find()) {
                String area = subArea.group("area");
                if (!cachedOnGalatea && isGalateaLocationName(area)) {
                    cachedOnGalatea = true;
                }
                if (!cachedOnTorrhus && isTorrhusLocationName(area)) {
                    cachedOnTorrhus = true;
                }
            }
            if (cachedOnGalatea && cachedOnTorrhus) {
                return;
            }
        }
    }

    private static void markShulkerIslandsFromName(String raw) {
        if (isGalateaLocationName(raw)) {
            cachedOnGalatea = true;
        }
        if (isTorrhusLocationName(raw)) {
            cachedOnTorrhus = true;
        }
    }

    private static boolean detectOnHypixel(Minecraft mc) {
        if (mc.player != null && mc.player.connection != null) {
            String brand = mc.player.connection.serverBrand();
            if (brand != null) {
                if (HYPIXEL_SERVER_BRAND.matcher(brand).matches()) {
                    return true;
                }
                if (brand.toLowerCase(Locale.ROOT).contains("hypixel")) {
                    return true;
                }
            }
        }
        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            String ip = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            return ip.contains("hypixel.net") || ip.contains("hypixel");
        }
        return false;
    }

    private static boolean isSkyblockScoreboardTitle(String strippedTitle) {
        if (strippedTitle == null || strippedTitle.isEmpty()) {
            return false;
        }
        for (String prefix : SKYBLOCK_TITLE_PREFIXES) {
            if (strippedTitle.startsWith(prefix)) {
                return true;
            }
        }
        return SCOREBOARD_SKYBLOCK_TITLE.matcher(strippedTitle).matches()
                || SCOREBOARD_SKYBLOCK_SHORT.matcher(strippedTitle).find();
    }

    private static void updatePanelIslandFlags(Minecraft mc) {
        String area = normalizeAreaName(cachedAreaIslandName);
        String tabIsland = normalizeAreaName(cachedTabIslandName);
        cachedInSafari = "Safari".equalsIgnoreCase(area) || "Safari".equalsIgnoreCase(tabIsland);

        for (String line : readSidebarPlainLines(mc, true)) {
            if (line.contains("\uE020") || line.startsWith("\u0444 ")) {
                cachedInRift = true;
                cachedInSafari = false;
                return;
            }
            if (line.startsWith("Motes:")) {
                cachedInRift = true;
                cachedInSafari = false;
                return;
            }
        }

        cachedInRift = "The Rift".equalsIgnoreCase(area) || "The Rift".equalsIgnoreCase(tabIsland);
        if (cachedInRift) {
            cachedInSafari = false;
        }
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
        if (mc.getConnection() == null) {
            return;
        }
        String area = null;
        String island = null;
        String dungeon = null;
        for (String text : readTabHudScanPlainLines(mc)) {
            text = text.trim();
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
        if (mc == null || mc.getConnection() == null) {
            return lines;
        }
        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) {
                continue;
            }
            String plain = toPlainText(entry.getTabListDisplayName().getString());
            if (!plain.isEmpty()) {
                lines.add(plain);
            }
        }
        return lines;
    }

    public static List<String> readTabHudScanPlainLines(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (mc == null) {
            return lines;
        }
        if (mc.gui != null && mc.gui.hud.getTabList() != null) {
            try {
                PlayerListHudMixin tab = (PlayerListHudMixin) mc.gui.hud.getTabList();
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
        if (mc == null || mc.gui == null || mc.gui.hud.getTabList() == null) {
            return null;
        }
        try {
            Component footer = ((PlayerListHudMixin) mc.gui.hud.getTabList()).getFooter();
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
                .replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    private static void clear() {
        cacheGameTime = Long.MIN_VALUE;
        lastFoundScoreboardMs = -1L;
        stickyOnSkyblock = false;
        cachedOnHypixel = false;
        cachedScoreboardSkyblock = false;
        cachedSkyblockGuest = false;
        cachedAreaIslandName = "";
        cachedTabIslandName = "";
        cachedScoreboardSubAreaName = "";
        cachedAreaIslandNameRawLineDungeonPrefix = false;
        cachedInRift = false;
        cachedInSafari = false;
        resetShulkerIslandLocateCache();
    }
}
