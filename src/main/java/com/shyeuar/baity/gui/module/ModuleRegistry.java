package com.shyeuar.baity.gui.module;

import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.config.ConfigSynchronizer;
import java.util.function.Supplier;
import java.util.function.Consumer;

public class ModuleRegistry {
    
    public static Module registerSimpleModule(String name, String description, ModuleCategory category,
                                             Supplier<Boolean> configGetter, Consumer<Boolean> configSetter) {
        Module module = new Module(name, description, category);
        ModuleManager.registerModule(module);
        ConfigSynchronizer.registerModuleConfig(name, configGetter, configSetter);
        return module;
    }
    
    public static Module registerModuleWithValues(String name, String description, ModuleCategory category,
                                                  Supplier<Boolean> configGetter, Consumer<Boolean> configSetter,
                                                  Value[] values, ValueConfigInfo[] valueConfigs) {
        Module module = new Module(name, description, category);
        for (Value value : values) {
            module.addValue(value);
        }
        ModuleManager.registerModule(module);
        ConfigSynchronizer.registerModuleConfig(name, configGetter, configSetter);
        
        if (valueConfigs != null) {
            for (ValueConfigInfo vci : valueConfigs) {
                ConfigSynchronizer.registerValueConfig(
                    name, vci.valueName, 
                    vci.configGetter, vci.configSetter, vci.onUpdate
                );
            }
        }
        
        return module;
    }
    
    public static class ValueConfigInfo {
        public final String valueName;
        public final Supplier<Object> configGetter;
        public final Consumer<Object> configSetter;
        public final Runnable onUpdate;
        
        public ValueConfigInfo(String valueName, Supplier<Object> configGetter, Consumer<Object> configSetter) {
            this(valueName, configGetter, configSetter, null);
        }
        
        public ValueConfigInfo(String valueName, Supplier<Object> configGetter, Consumer<Object> configSetter, Runnable onUpdate) {
            this.valueName = valueName;
            this.configGetter = configGetter;
            this.configSetter = configSetter;
            this.onUpdate = onUpdate;
        }
    }
}

