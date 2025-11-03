package com.shyeuar.baity.gui.config;

import java.util.function.Supplier;
import java.util.function.Consumer;

public class ConfigLink<T> {
    private final String moduleName;
    private final String valueName;  
    private final Supplier<T> configGetter; 
    private final Consumer<T> configSetter; 
    private final Runnable onUpdate; 
    
    public ConfigLink(String moduleName, Supplier<T> configGetter, Consumer<T> configSetter) {
        this(moduleName, null, configGetter, configSetter, null);
    }
    
    public ConfigLink(String moduleName, String valueName,
                    Supplier<T> configGetter, Consumer<T> configSetter,
                    Runnable onUpdate) {
        this.moduleName = moduleName;
        this.valueName = valueName;
        this.configGetter = configGetter;
        this.configSetter = configSetter;
        this.onUpdate = onUpdate;
    }
    
    public String getModuleName() {
        return moduleName;
    }
    
    public String getValueName() {
        return valueName;
    }
    
    public boolean isModuleConfig() {
        return valueName == null;
    }
    
    public T getConfigValue() {
        return configGetter.get();
    }
    
    public void setConfigValue(T value) {
        configSetter.accept(value);
        if (onUpdate != null) {
            onUpdate.run();
        }
    }
}


