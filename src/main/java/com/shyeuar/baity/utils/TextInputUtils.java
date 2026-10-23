package com.shyeuar.baity.utils;

import com.mojang.blaze3d.platform.TextInputManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class TextInputUtils {

    private TextInputUtils() {
    }

    public static void sync(boolean focused, Object owner) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        TextInputManager manager = client.textInputManager();
        if (manager == null) {
            return;
        }
        if (focused) {
            manager.startTextInput(owner);
        } else {
            manager.stopTextInput(owner);
        }
    }
}
