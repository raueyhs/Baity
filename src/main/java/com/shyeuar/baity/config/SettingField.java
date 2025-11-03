package com.shyeuar.baity.config;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SettingField {
    private final String key; 
    private final Function<ConfigManager, Object> getter; 
    private final BiConsumer<ConfigManager, Object> setter; 
    private final Class<?> type; 
    
    public SettingField(String key, Function<ConfigManager, Object> getter, 
                      BiConsumer<ConfigManager, Object> setter, Class<?> type) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
        this.type = type;
    }
    
    public String getKey() {
        return key;
    }
    
    public Object getValue(ConfigManager config) {
        return getter.apply(config);
    }
    
    public void setValue(ConfigManager config, Object value) {
        setter.accept(config, value);
    }
    
    public Class<?> getType() {
        return type;
    }
}


