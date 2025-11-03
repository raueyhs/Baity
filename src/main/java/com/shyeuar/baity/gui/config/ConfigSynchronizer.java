package com.shyeuar.baity.gui.config;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Consumer;

public class ConfigSynchronizer {
    private static final Map<String, ConfigLink<Boolean>> moduleConfigMap = new HashMap<>();
    private static final Map<String, ConfigLink<Object>> valueConfigMap = new HashMap<>();
    
    public static void registerModuleConfig(String moduleName, 
                                           Supplier<Boolean> configGetter,
                                           Consumer<Boolean> configSetter) {
        moduleConfigMap.put(moduleName, new ConfigLink<>(moduleName, configGetter, configSetter));
    }
   
    public static void registerValueConfig(String moduleName, String valueName,
                                          Supplier<Object> configGetter,
                                          Consumer<Object> configSetter) {
        registerValueConfig(moduleName, valueName, configGetter, configSetter, null);
    }
    
    public static void registerValueConfig(String moduleName, String valueName,
                                          Supplier<Object> configGetter,
                                          Consumer<Object> configSetter,
                                          Runnable onUpdate) {
        String key = moduleName + "::" + valueName;
        valueConfigMap.put(key, new ConfigLink<>(moduleName, valueName, configGetter, configSetter, onUpdate));
    }
    
    public static void syncModuleStates() {
        for (ConfigLink<Boolean> config : moduleConfigMap.values()) {
            Module module = ModuleManager.getModuleByName(config.getModuleName());
            if (module != null) {
                module.setEnabled(config.getConfigValue());
            }
        }
        
        for (ConfigLink<Object> config : valueConfigMap.values()) {
            Module module = ModuleManager.getModuleByName(config.getModuleName());
            if (module != null) {
                for (Value value : module.getValues()) {
                    if (value.getName().equals(config.getValueName())) {
                        value.setValue(config.getConfigValue());
                        break;
                    }
                }
            }
        }
    }
    
    public static void handleModuleToggle(String moduleName, boolean enabled) {
        ConfigLink<Boolean> config = moduleConfigMap.get(moduleName);
        if (config != null) {
            config.setConfigValue(enabled);
            ConfigManager.saveConfig();
        }
    }
    
    public static void handleValueUpdate(String moduleName, String valueName, Object value) {
        String key = moduleName + "::" + valueName;
        ConfigLink<Object> config = valueConfigMap.get(key);
        if (config != null) {
            config.setConfigValue(value);
            ConfigManager.saveConfig();
        }
    }
   
    public static boolean hasModuleConfig(String moduleName) {
        return moduleConfigMap.containsKey(moduleName);
    }
   
    public static boolean hasValueConfig(String moduleName, String valueName) {
        String key = moduleName + "::" + valueName;
        return valueConfigMap.containsKey(key);
    }
}


