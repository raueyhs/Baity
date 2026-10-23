package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MouseButtonUtils {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int MIDDLE = 2;

    private MouseButtonUtils() {
    }

    public static int fromSdl(int sdlButton) {
        return switch (sdlButton) {
            case 1 -> LEFT;
            case 2 -> MIDDLE;
            case 3 -> RIGHT;
            default -> sdlButton - 1;
        };
    }
}
