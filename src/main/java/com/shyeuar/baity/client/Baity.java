package com.shyeuar.baity.client;

import com.shyeuar.baity.config.BaityConfig;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.modules.ModuleManager;
import com.shyeuar.baity.gui.modules.Module;
import com.shyeuar.baity.gui.values.Value;
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

@Environment(EnvType.CLIENT)
public class Baity implements ClientModInitializer {
    
    private static long lastKeyPressTime = 0;
    public static boolean openGuiNextTick = false;

    @Override
    public void onInitializeClient() {
        // 注册自定义图腾
        CustomTotemItem.register();
        CustomTotemModelProvider.register();
        
        BaityConfig.loadConfig();

        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        
        // 初始化SmolPeople模块状态
        Module smolPeople = ModuleManager.getModuleByName("SmolPeople");
        if (smolPeople != null) {
            smolPeople.setEnabled(BaityConfig.smolpeopleMode);
            for (Value v : smolPeople.getValues()) {
                if ("crosshair".equals(v.getName())) {
                    v.setValue(BaityConfig.crosshairMode);
                }
            }
        }
        
        // 初始化BlockAnimation模块状态
        Module blockAnimation = ModuleManager.getModuleByName("BlockAnimation");
        if (blockAnimation != null) {
            blockAnimation.setEnabled(BaityConfig.blockAnimationMode);
        }
        
        // 初始化PepCat模块状态
        Module pepCat = ModuleManager.getModuleByName("PepCat");
        if (pepCat != null) {
            pepCat.setEnabled(BaityConfig.pepCatEnabled);
        }
        
        com.shyeuar.baity.features.game.PepCat.init();
        
        // 注册自定义音效事件
        registerCustomSounds();
        Module playerEsp = ModuleManager.getModuleByName("PlayerESP");
        if (playerEsp != null) {
            playerEsp.setEnabled(BaityConfig.playerEspEnabled);
            for (Value v : playerEsp.getValues()) {
                if ("show distance".equals(v.getName())) {
                    v.setValue(BaityConfig.playerEspShowDistance);
                } else if ("show own nametag".equals(v.getName())) {
                    v.setValue(BaityConfig.playerEspShowOwnNametag);
                }
            }
        }
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiNextTick) {
                openGuiNextTick = false;
                MinecraftClient.getInstance().setScreen(new ClickGui());
                return;
            }
            if (client.currentScreen == null && GLFW.glfwGetKey(client.getWindow().getHandle(), BaityConfig.guiKeyCode) == GLFW.GLFW_PRESS) {
                // 防重复触发
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

        WorldRenderEvents.AFTER_TRANSLUCENT.register(new com.shyeuar.baity.features.render.PlayerESPRenderer());
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
