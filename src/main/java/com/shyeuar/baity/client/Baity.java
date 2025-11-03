package com.shyeuar.baity.client;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.item.CustomTotemItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class Baity implements ClientModInitializer {
    
    private static final Set<String> SMOLPEOPLE_OPTIONS = Set.of("crosshair");
    private static final Set<String> PLAYERESP_OPTIONS = Set.of("show distance", "show own nametag");
    private static final Set<String> REMINDER_OPTIONS = Set.of("cookie buff reminder", "god potion reminder", "meowalert");
    
    private static long lastKeyPressTime = 0;
    public static boolean openGuiNextTick = false;

    @Override
    public void onInitializeClient() {
        CustomTotemItem.register();
        
        ConfigManager.loadConfig();

        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        
        Module smolPeople = ModuleManager.getModuleByName("SmolPeople");
        if (smolPeople != null) {
            smolPeople.setEnabled(ConfigManager.smolpeopleMode);
            for (Value v : smolPeople.getValues()) {
                if (SMOLPEOPLE_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "crosshair" -> v.setValue(ConfigManager.crosshairMode);
                    }
                }
            }
        }
        
        Module blockAnimation = ModuleManager.getModuleByName("BlockAnimation");
        if (blockAnimation != null) {
            blockAnimation.setEnabled(ConfigManager.blockAnimationMode);
        }
        
        Module pepCat = ModuleManager.getModuleByName("PepCat");
        if (pepCat != null) {
            pepCat.setEnabled(ConfigManager.pepCatEnabled);
        }
        
        com.shyeuar.baity.features.PepCat.init();
        
        Module reminder = ModuleManager.getModuleByName("Reminder");
        if (reminder != null) {
            reminder.setEnabled(ConfigManager.reminderEnabled);
            for (Value v : reminder.getValues()) {
                if (REMINDER_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "cookie buff reminder" -> v.setValue(ConfigManager.cookieBuffReminderEnabled);
                        case "god potion reminder" -> v.setValue(ConfigManager.godPotionReminderEnabled);
                        case "meowalert" -> v.setValue(ConfigManager.meowAlertEnabled);
                    }
                }
            }
        }
        
        com.shyeuar.baity.features.Reminder.init();
        
        registerCustomSounds();
        Module playerEsp = ModuleManager.getModuleByName("PlayerESP");
        if (playerEsp != null) {
            playerEsp.setEnabled(ConfigManager.playerEspEnabled);
            for (Value v : playerEsp.getValues()) {
                if (PLAYERESP_OPTIONS.contains(v.getName())) {
                    switch (v.getName()) {
                        case "show distance" -> v.setValue(ConfigManager.playerEspShowDistance);
                        case "show own nametag" -> v.setValue(ConfigManager.playerEspShowOwnNametag);
                    }
                }
            }
        }
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiNextTick) {
                openGuiNextTick = false;
                MinecraftClient.getInstance().setScreen(new ClickGui());
                return;
            }
            if (client.currentScreen == null && GLFW.glfwGetKey(client.getWindow().getHandle(), ConfigManager.guiKeyCode) == GLFW.GLFW_PRESS) {
                if (System.currentTimeMillis() - lastKeyPressTime > 200) {
                    MinecraftClient.getInstance().setScreen(new ClickGui());
                    lastKeyPressTime = System.currentTimeMillis();
                }
            }
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("baity")
            .executes(context -> {
                openGuiNextTick = true;
                return 1;
            })));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(new com.shyeuar.baity.features.PlayerESPRenderer());
    }
    
    public static final net.minecraft.sound.SoundEvent LAUGHTER_SOUND = registerSoundEvent("sounds.laughter");
    
    private static net.minecraft.sound.SoundEvent registerSoundEvent(String name) {
        net.minecraft.util.Identifier identifier = net.minecraft.util.Identifier.of("baity", name);
        return net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.SOUND_EVENT, identifier, net.minecraft.sound.SoundEvent.of(identifier));
    }
    
    private void registerCustomSounds() {
        // 静态变量已在类加载时注册
    }

}


