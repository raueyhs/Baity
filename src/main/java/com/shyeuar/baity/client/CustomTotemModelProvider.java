package com.shyeuar.baity.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 自定义图腾模型提供器
 * 在1.21.5中，模型通过JSON文件自动加载，无需特殊注册
 */
@Environment(EnvType.CLIENT)
public class CustomTotemModelProvider {
    
    public static void register() {
        System.out.println("custom totem model provider registered");
    }
}

