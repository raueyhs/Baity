package com.shyeuar.baity.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Config {

    public static final boolean ENABLE_FUN_MODULES = true;
    public static final boolean ENABLE_RENDER_MODULES = true;
    public static final boolean ENABLE_HUD_MODULES = true;
    
    public static final String VERSION_TYPE = 
        System.getProperty("baity.version.type", "open-source");
    
    public static void printConfig() {
        System.out.println("[Baity] 閰嶇疆淇℃伅:");
        System.out.println("  - 鐗堟湰绫诲瀷: " + VERSION_TYPE);
        
        System.out.println("  - Fun妯″潡: " + (ENABLE_FUN_MODULES ? "鍚敤" : "绂佺敤"));
        System.out.println("  - Render妯″潡: " + (ENABLE_RENDER_MODULES ? "鍚敤" : "绂佺敤"));
        System.out.println("  - HUD妯″潡: " + (ENABLE_HUD_MODULES ? "鍚敤" : "绂佺敤"));
    }
}

