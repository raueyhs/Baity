package com.shyeuar.baity.features.moderntooltip;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

@Environment(EnvType.CLIENT)
public final class ModernTooltip {

    private static Module cachedModule;

    private ModernTooltip() {
    }

    public static boolean isModuleActive() {
        if (!ConfigManager.modernTooltipEnabled) {
            return false;
        }
        if (cachedModule == null) {
            cachedModule = ModuleManager.getModuleByName("ModernTooltip");
        }
        return cachedModule == null || cachedModule.isEnabled();
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() == null) {
                TooltipAnimation.reset();
                ScrollableTooltip.reset();
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ClickGui) {
                return;
            }
            ScreenEvents.remove(screen).register(removed -> {
                TooltipAnimation.reset();
                ScrollableTooltip.reset();
            });
            ScreenMouseEvents.beforeMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                    ScrollableTooltip.onMouseScroll(verticalAmount)
            );
        });
    }
}
