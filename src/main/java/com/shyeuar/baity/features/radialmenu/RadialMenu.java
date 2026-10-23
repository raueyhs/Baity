package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.radialmenu.data.RadialMenuModels;
import com.shyeuar.baity.features.radialmenu.data.RadialPresetStore;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;

@Environment(EnvType.CLIENT)
public final class RadialMenu {

    private static boolean isOpen = false;
    private static boolean wasKeyPressed = false;

    private RadialMenu() {
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void init() {
        RadialIconLibrary.ensureInitialized();
        RadialPresetStore.init();
    }

    public static void tick(Minecraft client) {
        if (client.gui.screen() != null) {
            if (client.gui.screen() instanceof DynamicRadialMenuScreen) {
                return;
            }
            if (isOpen) {
                forceClose(client);
            }
            wasKeyPressed = false;
            return;
        }

        Module radialMenuModule = ModuleManager.getModuleByName("RadialMenu");
        if (radialMenuModule == null || !radialMenuModule.isEnabled()) {
            if (isOpen) {
                forceClose(client);
            }
            wasKeyPressed = false;
            return;
        }

        int keybind = ConfigManager.radialMenuKeybind;
        if (keybind == 0) {
            if (isOpen) {
                forceClose(client);
            }
            wasKeyPressed = false;
            return;
        }

        long windowHandle = client.getWindow().handle();
        boolean isKeyPressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);

        if (isKeyPressed && !wasKeyPressed) {
            open(client, keybind);
        }
        wasKeyPressed = isKeyPressed;
    }

    private static void open(Minecraft client, int keybind) {
        if (isOpen) {
            return;
        }
        RadialMenuModels.RadialPreset preset = RadialPresetStore.getActivePreset();
        if (preset == null) {
            return;
        }
        isOpen = true;
        long windowHandle = client.getWindow().handle();
        double centerX = client.getWindow().getScreenWidth() / 2.0;
        double centerY = client.getWindow().getScreenHeight() / 2.0;
        SDLMouse.SDL_WarpMouseInWindow(windowHandle, (float) centerX, (float) centerY);
        client.gui.setScreen(new DynamicRadialMenuScreen(keybind, preset, preset.rootLayerId, null));
    }

    public static void forceClose(Minecraft client) {
        if (!isOpen) {
            return;
        }
        isOpen = false;
    }

    public static void executeCommand(Minecraft client, String raw) {
        if (client == null || client.player == null || raw == null || raw.isBlank()) {
            return;
        }
        String text = raw.trim();
        if (text.startsWith("/")) {
            String command = text.substring(1).trim();
            if (!command.isEmpty()) {
                client.player.connection.sendCommand(command);
            }
            return;
        }
        client.player.connection.sendChat(text);
    }
}
