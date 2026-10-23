package com.shyeuar.baity.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class KeyMappingUtils {
    
    public static String getKeyDisplayText(int keyCode) {
        if (keyCode == 0) return null;
        if (keyCode < 0) return "Mouse " + (-keyCode);

        if (keyCode == InputConstants.KEY_SLASH) return null;

        switch (keyCode) {
            case InputConstants.KEY_ESCAPE: return "Escape";
            case InputConstants.KEY_BACKSPACE: return "Backspace";
            case InputConstants.KEY_LSHIFT: return "Left Shift";
            case InputConstants.KEY_RSHIFT: return "Right Shift";
            case InputConstants.KEY_LCONTROL: return "Left Ctrl";
            case InputConstants.KEY_RCONTROL: return "Right Ctrl";
            case InputConstants.KEY_LALT: return "Left Alt";
            case InputConstants.KEY_RALT: return "Right Alt";
            case InputConstants.KEY_LGUI: return "Left Super";
            case InputConstants.KEY_RGUI: return "Right Super";
            
            // 功能键
            case InputConstants.KEY_SPACE: return "Space";
            case InputConstants.KEY_RETURN: return "Enter";
            case InputConstants.KEY_TAB: return "Tab";
            case InputConstants.KEY_INSERT: return "Insert";
            case InputConstants.KEY_DELETE: return "Delete";
            case InputConstants.KEY_RIGHT: return "Right";
            case InputConstants.KEY_LEFT: return "Left";
            case InputConstants.KEY_DOWN: return "Down";
            case InputConstants.KEY_UP: return "Up";
            case InputConstants.KEY_PAGEUP: return "Page Up";
            case InputConstants.KEY_PAGEDOWN: return "Page Down";
            case InputConstants.KEY_HOME: return "Home";
            case InputConstants.KEY_END: return "End";
            case InputConstants.KEY_CAPSLOCK: return "Caps Lock";
            case InputConstants.KEY_SCROLLLOCK: return "Scroll Lock";
            case InputConstants.KEY_NUMLOCK: return "Num Lock";
            case InputConstants.KEY_PRINTSCREEN: return "Print Screen";
            case InputConstants.KEY_PAUSE: return "Pause";
            
            // F键 (F1-F24)
            case InputConstants.KEY_F1: return "F1";
            case InputConstants.KEY_F2: return "F2";
            case InputConstants.KEY_F3: return "F3";
            case InputConstants.KEY_F4: return "F4";
            case InputConstants.KEY_F5: return "F5";
            case InputConstants.KEY_F6: return "F6";
            case InputConstants.KEY_F7: return "F7";
            case InputConstants.KEY_F8: return "F8";
            case InputConstants.KEY_F9: return "F9";
            case InputConstants.KEY_F10: return "F10";
            case InputConstants.KEY_F11: return "F11";
            case InputConstants.KEY_F12: return "F12";
            case InputConstants.KEY_F13: return "F13";
            case InputConstants.KEY_F14: return "F14";
            case InputConstants.KEY_F15: return "F15";
            case InputConstants.KEY_F16: return "F16";
            case InputConstants.KEY_F17: return "F17";
            case InputConstants.KEY_F18: return "F18";
            case InputConstants.KEY_F19: return "F19";
            case InputConstants.KEY_F20: return "F20";
            case InputConstants.KEY_F21: return "F21";
            case InputConstants.KEY_F22: return "F22";
            case InputConstants.KEY_F23: return "F23";
            case InputConstants.KEY_F24: return "F24";
            
            // 主键盘数字键
            case InputConstants.KEY_0: return "0";
            case InputConstants.KEY_1: return "1";
            case InputConstants.KEY_2: return "2";
            case InputConstants.KEY_3: return "3";
            case InputConstants.KEY_4: return "4";
            case InputConstants.KEY_5: return "5";
            case InputConstants.KEY_6: return "6";
            case InputConstants.KEY_7: return "7";
            case InputConstants.KEY_8: return "8";
            case InputConstants.KEY_9: return "9";
            
            // 字母键
            case InputConstants.KEY_A: return "A";
            case InputConstants.KEY_B: return "B";
            case InputConstants.KEY_C: return "C";
            case InputConstants.KEY_D: return "D";
            case InputConstants.KEY_E: return "E";
            case InputConstants.KEY_F: return "F";
            case InputConstants.KEY_G: return "G";
            case InputConstants.KEY_H: return "H";
            case InputConstants.KEY_I: return "I";
            case InputConstants.KEY_J: return "J";
            case InputConstants.KEY_K: return "K";
            case InputConstants.KEY_L: return "L";
            case InputConstants.KEY_M: return "M";
            case InputConstants.KEY_N: return "N";
            case InputConstants.KEY_O: return "O";
            case InputConstants.KEY_P: return "P";
            case InputConstants.KEY_Q: return "Q";
            case InputConstants.KEY_R: return "R";
            case InputConstants.KEY_S: return "S";
            case InputConstants.KEY_T: return "T";
            case InputConstants.KEY_U: return "U";
            case InputConstants.KEY_V: return "V";
            case InputConstants.KEY_W: return "W";
            case InputConstants.KEY_X: return "X";
            case InputConstants.KEY_Y: return "Y";
            case InputConstants.KEY_Z: return "Z";
            
            // 符号键
            case InputConstants.KEY_GRAVE: return "`";
            case InputConstants.KEY_MINUS: return "-";
            case InputConstants.KEY_EQUALS: return "=";
            case InputConstants.KEY_LBRACKET: return "[";
            case InputConstants.KEY_RBRACKET: return "]";
            case InputConstants.KEY_BACKSLASH: return "\\";
            case InputConstants.KEY_SEMICOLON: return ";";
            case InputConstants.KEY_APOSTROPHE: return "'";
            case InputConstants.KEY_COMMA: return ",";
            case InputConstants.KEY_PERIOD: return ".";
            
            // 数字小键盘
            case InputConstants.KEY_NUMPAD0: return "Num 0";
            case InputConstants.KEY_NUMPAD1: return "Num 1";
            case InputConstants.KEY_NUMPAD2: return "Num 2";
            case InputConstants.KEY_NUMPAD3: return "Num 3";
            case InputConstants.KEY_NUMPAD4: return "Num 4";
            case InputConstants.KEY_NUMPAD5: return "Num 5";
            case InputConstants.KEY_NUMPAD6: return "Num 6";
            case InputConstants.KEY_NUMPAD7: return "Num 7";
            case InputConstants.KEY_NUMPAD8: return "Num 8";
            case InputConstants.KEY_NUMPAD9: return "Num 9";
            case SDLScancode.SDL_SCANCODE_KP_PERIOD: return "Num .";
            case SDLScancode.SDL_SCANCODE_KP_DIVIDE: return "Num /";
            case InputConstants.KEY_MULTIPLY: return "Num *";
            case SDLScancode.SDL_SCANCODE_KP_MINUS: return "Num -";
            case InputConstants.KEY_ADD: return "Num +";
            case InputConstants.KEY_NUMPADENTER: return "Num Enter";
            case InputConstants.KEY_NUMPADEQUALS: return "Num =";
            
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
        return keyCode == InputConstants.KEY_BACKSPACE;
    }
   
    public static boolean isKeyPressed(long windowHandle, int keyCode) {
        if (keyCode < 0) {
            int button = -keyCode - 1;
            int sdlBit = switch (button) {
                case 1 -> 2;
                case 2 -> 1;
                default -> button;
            };
            int state;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                state = SDLMouse.SDL_GetMouseState(stack.mallocFloat(1), stack.mallocFloat(1));
            }
            return (state & (1 << sdlBit)) != 0;
        }
        ByteBuffer keys = SDLKeyboard.SDL_GetKeyboardState();
        return keyCode > 0 && keyCode < keys.capacity() && keys.get(keyCode) != 0;
    }
    
    public static boolean isKeySupported(int keyCode) {
        if (keyCode == 0) return false;
        if (keyCode == InputConstants.KEY_SLASH) return false;
        return true;
    }
}
