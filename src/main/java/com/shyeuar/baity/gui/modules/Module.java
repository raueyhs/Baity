package com.shyeuar.baity.gui.modules;

import com.shyeuar.baity.gui.values.ModuleCategory;
import com.shyeuar.baity.gui.values.Value;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class Module {
    private final String name;
    private final String description;
    private boolean enabled;
    private final ModuleCategory category;
    private final ArrayList<Value> values;
    private boolean expanded;
    
    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
        this.values = new ArrayList<>();
        this.expanded = false;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle() { this.enabled = !this.enabled; }
    public ModuleCategory getCategory() { return category; }
    public ArrayList<Value> getValues() { return values; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public void toggleExpanded() { this.expanded = !this.expanded; }
    
    public void addValue(Value value) {
        values.add(value);
    }
}

