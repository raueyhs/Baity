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
        System.out.println("[Baity] 配置信息:");
        System.out.println("  - 版本类型: " + VERSION_TYPE);
        
        System.out.println("  - Fun模块: " + (ENABLE_FUN_MODULES ? "启用" : "禁用"));
        System.out.println("  - Render模块: " + (ENABLE_RENDER_MODULES ? "启用" : "禁用"));
        System.out.println("  - HUD模块: " + (ENABLE_HUD_MODULES ? "启用" : "禁用"));
    }
}
