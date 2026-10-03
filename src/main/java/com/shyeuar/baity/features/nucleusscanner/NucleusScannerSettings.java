package com.shyeuar.baity.features.nucleusscanner;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.value.ValueCycleUtils;

public final class NucleusScannerSettings {

    public static final String[] COLOR_NAMES = {
            "magenta", "cyan", "orange", "purple", "green", "yellow", "lime", "white",
            "olive", "brown", "gray", "red", "blue", "aqua", "pink", "black"
    };

    private static final int[] COLOR_VALUES = {
            0xFF55FF, 0x55FFFF, 0xFFAA00, 0xAA00AA, 0x00AA00, 0xFFFF55, 0x55FF55, 0xFFFFFF,
            0x506E00, 0x6E2A00, 0xAAAAAA, 0xFF5555, 0x5555FF, 0x55FFAA, 0xFFAACC, 0x000000
    };

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_COLOR = "color";
    private static final String KEY_SELECT_AREAS_EXPANDED = "select areas.expanded";
    private static final String KEY_DISPLAY_NAME = "display name";
    private static final String KEY_SHOW_DISTANCE = "show distance";
    private static final String KEY_WORM_FISHING = "worm fishing";

    private static final State[] STATES = new State[NucleusFamily.values().length];

    private static String loadedSource = null;
    private static boolean selectAreasExpanded = false;
    private static boolean displayName = true;
    private static boolean showDistance = true;
    private static boolean wormFishing = false;

    static {
        NucleusFamily[] families = NucleusFamily.values();
        for (int i = 0; i < families.length; i++) {
            STATES[i] = new State(families[i].defaultColorName());
        }
    }

    private NucleusScannerSettings() {
    }

    public static synchronized void ensureLoaded() {
        String raw = ConfigManager.nucleusScannerSettings;
        if (raw == null) {
            raw = "";
        }
        if (raw.equals(loadedSource)) {
            return;
        }
        loadedSource = raw;
        resetToDefaults();
        applyEncoded(raw);
    }

    public static boolean isEnabled(NucleusFamily family) {
        ensureLoaded();
        return state(family).enabled;
    }

    public static void setEnabled(NucleusFamily family, boolean value) {
        ensureLoaded();
        state(family).enabled = value;
        persist();
    }

    public static String color(NucleusFamily family) {
        ensureLoaded();
        return state(family).color;
    }

    public static void setColor(NucleusFamily family, String value) {
        ensureLoaded();
        state(family).color = value;
        persist();
    }

    public static boolean isSelectAreasExpanded() {
        ensureLoaded();
        return selectAreasExpanded;
    }

    public static void setSelectAreasExpanded(boolean value) {
        ensureLoaded();
        selectAreasExpanded = value;
        persist();
    }

    public static boolean isDisplayName() {
        ensureLoaded();
        return displayName;
    }

    public static void setDisplayName(boolean value) {
        ensureLoaded();
        displayName = value;
        persist();
    }

    public static boolean isShowDistance() {
        ensureLoaded();
        return showDistance;
    }

    public static void setShowDistance(boolean value) {
        ensureLoaded();
        showDistance = value;
        persist();
    }

    public static boolean isWormFishing() {
        ensureLoaded();
        return wormFishing;
    }

    public static void setWormFishing(boolean value) {
        ensureLoaded();
        wormFishing = value;
        persist();
    }

    public static String cycleColor(String current, boolean forward) {
        return ValueCycleUtils.cycle(current, COLOR_NAMES, forward);
    }

    public static int colorRgb(String name) {
        if (name != null) {
            for (int i = 0; i < COLOR_NAMES.length; i++) {
                if (COLOR_NAMES[i].equalsIgnoreCase(name)) {
                    return COLOR_VALUES[i];
                }
            }
        }
        return 0xFFFFFF;
    }

    public static boolean isFamilyName(String valueName) {
        return valueName != null && familyByKey(valueName) != null;
    }

    public static int tintFor(String valueName) {
        NucleusFamily family = familyByKey(valueName);
        if (family == null) {
            return 0;
        }
        return 0xFF000000 | colorRgb(color(family));
    }

    private static State state(NucleusFamily family) {
        return STATES[family.ordinal()];
    }

    private static synchronized void persist() {
        String encoded = encode();
        loadedSource = encoded;
        ConfigManager.nucleusScannerSettings = encoded;
    }

    private static void resetToDefaults() {
        NucleusFamily[] families = NucleusFamily.values();
        for (int i = 0; i < families.length; i++) {
            State state = STATES[i];
            state.enabled = false;
            state.color = families[i].defaultColorName();
        }
        selectAreasExpanded = false;
        displayName = true;
        showDistance = true;
        wormFishing = false;
    }

    private static String encode() {
        StringBuilder builder = new StringBuilder();
        for (NucleusFamily family : NucleusFamily.values()) {
            State state = state(family);
            append(builder, family.key() + "." + KEY_ENABLED, String.valueOf(state.enabled), "false");
            append(builder, family.key() + "." + KEY_COLOR, state.color, family.defaultColorName());
        }
        append(builder, KEY_SELECT_AREAS_EXPANDED, String.valueOf(selectAreasExpanded), "false");
        append(builder, KEY_DISPLAY_NAME, String.valueOf(displayName), "true");
        append(builder, KEY_SHOW_DISTANCE, String.valueOf(showDistance), "true");
        append(builder, KEY_WORM_FISHING, String.valueOf(wormFishing), "false");
        return builder.toString();
    }

    private static void append(StringBuilder builder, String key, String value, String defaultValue) {
        if (value == null || value.equals(defaultValue)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(';');
        }
        builder.append(key).append('=').append(value);
    }

    private static void applyEncoded(String raw) {
        if (raw.isEmpty()) {
            return;
        }
        for (String token : raw.split(";")) {
            int separator = token.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = token.substring(0, separator);
            String value = token.substring(separator + 1);
            if (KEY_SELECT_AREAS_EXPANDED.equals(key)) {
                selectAreasExpanded = Boolean.parseBoolean(value);
                continue;
            }
            if (KEY_DISPLAY_NAME.equals(key)) {
                displayName = Boolean.parseBoolean(value);
                continue;
            }
            if (KEY_SHOW_DISTANCE.equals(key)) {
                showDistance = Boolean.parseBoolean(value);
                continue;
            }
            if (KEY_WORM_FISHING.equals(key)) {
                wormFishing = Boolean.parseBoolean(value);
                continue;
            }
            int dot = key.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            NucleusFamily family = familyByKey(key.substring(0, dot));
            if (family == null) {
                continue;
            }
            State state = state(family);
            switch (key.substring(dot + 1)) {
                case KEY_ENABLED -> state.enabled = Boolean.parseBoolean(value);
                case KEY_COLOR -> state.color = value;
                default -> {
                }
            }
        }
    }

    private static NucleusFamily familyByKey(String key) {
        if (key == null) {
            return null;
        }
        for (NucleusFamily family : NucleusFamily.values()) {
            if (family.key().equals(key)) {
                return family;
            }
        }
        return null;
    }

    private static final class State {
        private volatile boolean enabled = false;
        private volatile String color;

        private State(String color) {
            this.color = color;
        }
    }
}
