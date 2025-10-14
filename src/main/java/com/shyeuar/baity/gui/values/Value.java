package com.shyeuar.baity.gui.values;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Value {
    String getName();
    String getDisplayName();
    Object getValue();
    void setValue(Object value);
    ModuleCategory getCategory();
}

