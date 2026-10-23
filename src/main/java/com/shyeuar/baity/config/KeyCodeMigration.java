package com.shyeuar.baity.config;

/**
 * TODO(migration-26.3): Converts key codes saved by the 26.2 branch (GLFW key codes and
 * 0-based mouse buttons) into the 26.3 encoding (SDL scancodes and 1-based mouse buttons).
 * Remove this class and the KeyCodeFormatMigrated config flag once old configs no longer matter.
 */
public final class KeyCodeMigration {

    private KeyCodeMigration() {
    }

    public static int migrate(int keyCode) {
        if (keyCode < 0) {
            return keyCode;
        }
        if (keyCode == 0) {
            return 0;
        }
        if (keyCode >= 1 && keyCode <= 7) {
            return -glfwButtonToSdl(keyCode);
        }
        if (keyCode < 32) {
            return keyCode;
        }
        int scancode = glfwKeyToScancode(keyCode);
        return scancode != 0 ? scancode : keyCode;
    }

    public static boolean migrateAll() {
        boolean changed = false;
        int[] before = snapshot();
        ConfigManager.guiKeyCode = migrate(ConfigManager.guiKeyCode);
        ConfigManager.nametagFocusPlayerKeybind = migrate(ConfigManager.nametagFocusPlayerKeybind);
        ConfigManager.radialMenuKeybind = migrate(ConfigManager.radialMenuKeybind);
        ConfigManager.keybindsWardrobeHoldToUnequip = migrate(ConfigManager.keybindsWardrobeHoldToUnequip);
        ConfigManager.keybindsEquipmentHoldToUnequip = migrate(ConfigManager.keybindsEquipmentHoldToUnequip);
        ConfigManager.keybindsLoadoutSlot10Key = migrate(ConfigManager.keybindsLoadoutSlot10Key);
        ConfigManager.keybindsLoadoutSlot11Key = migrate(ConfigManager.keybindsLoadoutSlot11Key);
        ConfigManager.keybindsLoadoutSlot12Key = migrate(ConfigManager.keybindsLoadoutSlot12Key);
        ConfigManager.autoSprintKeybind = migrate(ConfigManager.autoSprintKeybind);
        ConfigManager.scrollableTooltipHorizontalKey = migrate(ConfigManager.scrollableTooltipHorizontalKey);
        ConfigManager.enchantLoreMissingKeybind = migrate(ConfigManager.enchantLoreMissingKeybind);
        ConfigManager.middleClickGuiHoldToCancel = migrate(ConfigManager.middleClickGuiHoldToCancel);
        ConfigManager.keybindsCommand1Key = migrate(ConfigManager.keybindsCommand1Key);
        ConfigManager.keybindsCommand2Key = migrate(ConfigManager.keybindsCommand2Key);
        ConfigManager.keybindsCommand3Key = migrate(ConfigManager.keybindsCommand3Key);
        ConfigManager.keybindsCommand4Key = migrate(ConfigManager.keybindsCommand4Key);
        int[] after = snapshot();
        for (int i = 0; i < before.length; i++) {
            if (before[i] != after[i]) {
                changed = true;
                break;
            }
        }
        ConfigManager.keyCodeFormatMigrated = true;
        return changed;
    }

    private static int[] snapshot() {
        return new int[]{
                ConfigManager.guiKeyCode,
                ConfigManager.nametagFocusPlayerKeybind,
                ConfigManager.radialMenuKeybind,
                ConfigManager.keybindsWardrobeHoldToUnequip,
                ConfigManager.keybindsEquipmentHoldToUnequip,
                ConfigManager.keybindsLoadoutSlot10Key,
                ConfigManager.keybindsLoadoutSlot11Key,
                ConfigManager.keybindsLoadoutSlot12Key,
                ConfigManager.autoSprintKeybind,
                ConfigManager.scrollableTooltipHorizontalKey,
                ConfigManager.enchantLoreMissingKeybind,
                ConfigManager.middleClickGuiHoldToCancel,
                ConfigManager.keybindsCommand1Key,
                ConfigManager.keybindsCommand2Key,
                ConfigManager.keybindsCommand3Key,
                ConfigManager.keybindsCommand4Key
        };
    }

    private static int glfwButtonToSdl(int button) {
        return switch (button) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 2;
            default -> button + 1;
        };
    }

    public static int migrateMouseArtifact(int keyCode) {
        if (keyCode >= 2 && keyCode <= 8) {
            return -glfwButtonToSdl(keyCode - 1);
        }
        return keyCode;
    }

    private static int glfwKeyToScancode(int key) {
        if (key >= 65 && key <= 90) {
            return key - 61;
        }
        switch (key) {
            case 32: return 44;
            case 39: return 52;
            case 44: return 54;
            case 45: return 45;
            case 46: return 55;
            case 47: return 56;
            case 48: return 39;
            case 49: return 30;
            case 50: return 31;
            case 51: return 32;
            case 52: return 33;
            case 53: return 34;
            case 54: return 35;
            case 55: return 36;
            case 56: return 37;
            case 57: return 38;
            case 59: return 51;
            case 61: return 46;
            case 91: return 47;
            case 92: return 49;
            case 93: return 48;
            case 96: return 53;
            case 256: return 41;
            case 257: return 40;
            case 258: return 43;
            case 259: return 42;
            case 260: return 73;
            case 261: return 76;
            case 262: return 79;
            case 263: return 80;
            case 264: return 81;
            case 265: return 82;
            case 266: return 75;
            case 267: return 78;
            case 268: return 74;
            case 269: return 77;
            case 280: return 57;
            case 281: return 71;
            case 282: return 83;
            case 283: return 70;
            case 284: return 72;
            case 290: return 58;
            case 291: return 59;
            case 292: return 60;
            case 293: return 61;
            case 294: return 62;
            case 295: return 63;
            case 296: return 64;
            case 297: return 65;
            case 298: return 66;
            case 299: return 67;
            case 300: return 68;
            case 301: return 69;
            case 302: return 104;
            case 303: return 105;
            case 304: return 106;
            case 305: return 107;
            case 306: return 108;
            case 307: return 109;
            case 308: return 110;
            case 309: return 111;
            case 310: return 112;
            case 311: return 113;
            case 312: return 114;
            case 313: return 115;
            case 320: return 98;
            case 321: return 89;
            case 322: return 90;
            case 323: return 91;
            case 324: return 92;
            case 325: return 93;
            case 326: return 94;
            case 327: return 95;
            case 328: return 96;
            case 329: return 97;
            case 330: return 99;
            case 331: return 84;
            case 332: return 85;
            case 333: return 86;
            case 334: return 87;
            case 335: return 88;
            case 336: return 103;
            case 340: return 225;
            case 341: return 224;
            case 342: return 226;
            case 343: return 227;
            case 344: return 229;
            case 345: return 228;
            case 346: return 230;
            case 347: return 231;
            default: return 0;
        }
    }
}
