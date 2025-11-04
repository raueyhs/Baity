package com.shyeuar.baity.managers;

import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.utils.KeyMappingUtils;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class KeybindManager {
    
    private static final List<KeybindInfo> keybindCache = new ArrayList<>();
    private static boolean cacheDirty = true;
    
    private static class KeybindInfo {
        final Module module;
        final int keyCode;
        boolean lastState = false;
        long lastPressTime = 0;
        
        KeybindInfo(Module module, int keyCode) {
            this.module = module;
            this.keyCode = keyCode;
        }
    }
   
    public static void markCacheDirty() {
        cacheDirty = true;
    }
    
    private static void updateKeybindCache() {
        if (!cacheDirty) return;
        
        keybindCache.clear();
        
        for (Module module : ModuleManager.getModules()) {
            for (Value value : module.getValues()) {
                if (value instanceof ButtonValue) {
                    ButtonValue buttonValue = (ButtonValue) value;
                    if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.KEYBIND) {
                        Object keybindValue = buttonValue.getValue();
                        if (keybindValue instanceof Number) {
                            int keyCode = ((Number) keybindValue).intValue();
                            if (keyCode != 0) {
                                keybindCache.add(new KeybindInfo(module, keyCode));
                            }
                        }
                    }
                }
            }
        }
        
        cacheDirty = false;
    }
    
    public static void handleModuleKeybinds(MinecraftClient client, long windowHandle) {
        if (client.currentScreen != null) return;
        
        updateKeybindCache();
        
        if (keybindCache.isEmpty()) return;
        
        for (KeybindInfo info : keybindCache) {
            boolean currentKeyState = KeyMappingUtils.isKeyPressed(windowHandle, info.keyCode);
            
            if (currentKeyState && !info.lastState) {
                long currentTime = System.currentTimeMillis();
                
                if (currentTime - info.lastPressTime > 200) {
                    boolean newEnabled = !info.module.isEnabled();
                    info.module.setEnabled(newEnabled);
                    
                    if (ConfigSynchronizer.hasModuleConfig(info.module.getName())) {
                        ConfigSynchronizer.handleModuleToggle(info.module.getName(), newEnabled);
                    }
                    
                    info.lastPressTime = currentTime;
                }
            }
            
            info.lastState = currentKeyState;
        }
    }
}


