package com.shyeuar.baity.gui.values;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum ModuleCategory {
    FUN("Fun"),
    RENDER("Render"),
    QOL("Qol"),
    
    HUD("Hud");
    
    private final String displayName;
    
    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

