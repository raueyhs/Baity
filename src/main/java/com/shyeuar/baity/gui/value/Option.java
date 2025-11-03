package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Option implements Value {
    private final String name;
    private final String displayName;
    private boolean value;
    private final ModuleCategory category;
    
    public Option(String name, String displayName, boolean defaultValue, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.value = defaultValue;
        this.category = category;
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDisplayName() { return displayName; }
    
    @Override
    public Object getValue() { return value; }
    
    @Override
    public void setValue(Object value) { this.value = (Boolean) value; }
    
    @Override
    public ModuleCategory getCategory() { return category; }
}

