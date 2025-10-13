package com.shyeuar.baity.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Config {
    // 123
    // MACRO_START - 私有功能配置
    // 从系统属性读取配置，默认为开源版本
    public static final boolean ENABLE_MACRO_MODULES = 
        Boolean.parseBoolean(System.getProperty("baity.macro.enabled", "false"));
    // MACRO_END
    
    public static final boolean ENABLE_FUN_MODULES = true;
    public static final boolean ENABLE_RENDER_MODULES = true;
    public static final boolean ENABLE_HUD_MODULES = true;
    
    public static final String VERSION_TYPE = 
        System.getProperty("baity.version.type", "open-source");
    
    public static void printConfig() {
        System.out.println("[Baity] 配置信息:");
        System.out.println("  - 版本类型: " + VERSION_TYPE);
        // MACRO_START
        System.out.println("  - Macro模块: " + (ENABLE_MACRO_MODULES ? "启用" : "禁用"));
        // MACRO_END
        System.out.println("  - Fun模块: " + (ENABLE_FUN_MODULES ? "启用" : "禁用"));
        System.out.println("  - Render模块: " + (ENABLE_RENDER_MODULES ? "启用" : "禁用"));
        System.out.println("  - HUD模块: " + (ENABLE_HUD_MODULES ? "启用" : "禁用"));
    }
}
