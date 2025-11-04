package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class KeyMappingUtils {
    
    public static String getKeyDisplayText(int keyCode) {
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_3) return "Mouse 3";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_4) return "Mouse 4";
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_5) return "Mouse 5";
        
        if (keyCode == 47) return null;
        
        switch (keyCode) {
            // 控制键（基于GLFW键码）
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_LEFT_SUPER: return "Left Super";
            case GLFW.GLFW_KEY_RIGHT_SUPER: return "Right Super";
            
            // 功能键
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_SCROLL_LOCK: return "Scroll Lock";
            case GLFW.GLFW_KEY_NUM_LOCK: return "Num Lock";
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "Print Screen";
            case GLFW.GLFW_KEY_PAUSE: return "Pause";
            
            // F键 (F1-F24)
            case GLFW.GLFW_KEY_F1: return "F1";
            case GLFW.GLFW_KEY_F2: return "F2";
            case GLFW.GLFW_KEY_F3: return "F3";
            case GLFW.GLFW_KEY_F4: return "F4";
            case GLFW.GLFW_KEY_F5: return "F5";
            case GLFW.GLFW_KEY_F6: return "F6";
            case GLFW.GLFW_KEY_F7: return "F7";
            case GLFW.GLFW_KEY_F8: return "F8";
            case GLFW.GLFW_KEY_F9: return "F9";
            case GLFW.GLFW_KEY_F10: return "F10";
            case GLFW.GLFW_KEY_F11: return "F11";
            case GLFW.GLFW_KEY_F12: return "F12";
            case GLFW.GLFW_KEY_F13: return "F13";
            case GLFW.GLFW_KEY_F14: return "F14";
            case GLFW.GLFW_KEY_F15: return "F15";
            case GLFW.GLFW_KEY_F16: return "F16";
            case GLFW.GLFW_KEY_F17: return "F17";
            case GLFW.GLFW_KEY_F18: return "F18";
            case GLFW.GLFW_KEY_F19: return "F19";
            case GLFW.GLFW_KEY_F20: return "F20";
            case GLFW.GLFW_KEY_F21: return "F21";
            case GLFW.GLFW_KEY_F22: return "F22";
            case GLFW.GLFW_KEY_F23: return "F23";
            case GLFW.GLFW_KEY_F24: return "F24";
            
            // 主键盘数字键
            case GLFW.GLFW_KEY_0: return "0";
            case GLFW.GLFW_KEY_1: return "1";
            case GLFW.GLFW_KEY_2: return "2";
            case GLFW.GLFW_KEY_3: return "3";
            case GLFW.GLFW_KEY_4: return "4";
            case GLFW.GLFW_KEY_5: return "5";
            case GLFW.GLFW_KEY_6: return "6";
            case GLFW.GLFW_KEY_7: return "7";
            case GLFW.GLFW_KEY_8: return "8";
            case GLFW.GLFW_KEY_9: return "9";
            
            // 字母键
            case GLFW.GLFW_KEY_A: return "A";
            case GLFW.GLFW_KEY_B: return "B";
            case GLFW.GLFW_KEY_C: return "C";
            case GLFW.GLFW_KEY_D: return "D";
            case GLFW.GLFW_KEY_E: return "E";
            case GLFW.GLFW_KEY_F: return "F";
            case GLFW.GLFW_KEY_G: return "G";
            case GLFW.GLFW_KEY_H: return "H";
            case GLFW.GLFW_KEY_I: return "I";
            case GLFW.GLFW_KEY_J: return "J";
            case GLFW.GLFW_KEY_K: return "K";
            case GLFW.GLFW_KEY_L: return "L";
            case GLFW.GLFW_KEY_M: return "M";
            case GLFW.GLFW_KEY_N: return "N";
            case GLFW.GLFW_KEY_O: return "O";
            case GLFW.GLFW_KEY_P: return "P";
            case GLFW.GLFW_KEY_Q: return "Q";
            case GLFW.GLFW_KEY_R: return "R";
            case GLFW.GLFW_KEY_S: return "S";
            case GLFW.GLFW_KEY_T: return "T";
            case GLFW.GLFW_KEY_U: return "U";
            case GLFW.GLFW_KEY_V: return "V";
            case GLFW.GLFW_KEY_W: return "W";
            case GLFW.GLFW_KEY_X: return "X";
            case GLFW.GLFW_KEY_Y: return "Y";
            case GLFW.GLFW_KEY_Z: return "Z";
            
            // 符号键
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return "`";
            case GLFW.GLFW_KEY_MINUS: return "-";
            case GLFW.GLFW_KEY_EQUAL: return "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET: return "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET: return "]";
            case GLFW.GLFW_KEY_BACKSLASH: return "\\";
            case GLFW.GLFW_KEY_SEMICOLON: return ";";
            case GLFW.GLFW_KEY_APOSTROPHE: return "'";
            case GLFW.GLFW_KEY_COMMA: return ",";
            case GLFW.GLFW_KEY_PERIOD: return ".";
            
            // 数字小键盘
            case GLFW.GLFW_KEY_KP_0: return "Num 0";
            case GLFW.GLFW_KEY_KP_1: return "Num 1";
            case GLFW.GLFW_KEY_KP_2: return "Num 2";
            case GLFW.GLFW_KEY_KP_3: return "Num 3";
            case GLFW.GLFW_KEY_KP_4: return "Num 4";
            case GLFW.GLFW_KEY_KP_5: return "Num 5";
            case GLFW.GLFW_KEY_KP_6: return "Num 6";
            case GLFW.GLFW_KEY_KP_7: return "Num 7";
            case GLFW.GLFW_KEY_KP_8: return "Num 8";
            case GLFW.GLFW_KEY_KP_9: return "Num 9";
            case GLFW.GLFW_KEY_KP_DECIMAL: return "Num .";
            case GLFW.GLFW_KEY_KP_DIVIDE: return "Num /";
            case GLFW.GLFW_KEY_KP_MULTIPLY: return "Num *";
            case GLFW.GLFW_KEY_KP_SUBTRACT: return "Num -";
            case GLFW.GLFW_KEY_KP_ADD: return "Num +";
            case GLFW.GLFW_KEY_KP_ENTER: return "Num Enter";
            case GLFW.GLFW_KEY_KP_EQUAL: return "Num =";
            
            default: return "Key " + keyCode;
        }
    }
    
    private static final int MODULE_ENABLED_PURPLE = new java.awt.Color(84, 72, 200).getRGB();
    
    public static String formatKeyDisplay(int keyCode, String defaultText) {
        if (keyCode == 0) {
            return "☄ NOTSET";
        }
        String keyText = getKeyDisplayText(keyCode);
        if (keyText == null) {
            return "☄ NOTSET";
        }
        return "✎ " + keyText;
    }
    
    public static int getModuleEnabledPurpleRGB() {
        return MODULE_ENABLED_PURPLE;
    }
    
    public static boolean isResetKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_BACKSPACE;
    }
   
    public static boolean isKeyPressed(long windowHandle, int keyCode) {
        if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return GLFW.glfwGetMouseButton(windowHandle, keyCode) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
    }
    
    public static boolean isKeySupported(int keyCode) {
        if (keyCode == 0) return false;
        if (keyCode == 47) return false;
        return true;
    }
}
