package com.shyeuar.baity.config;

import net.minecraft.entity.player.PlayerEntity;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;

/**
 * 开发者配置类
 * 此文件包含开发者相关的特殊配置，用于标识模组作者
 * 使用 UUID 进行识别，永久有效且不受改名影响
 * 
 * 算是我自己的小小私心啦喵~
 */
public class DevConfig {
    private static final Set<String> DEV_UUIDS = new HashSet<>(Arrays.asList(
        "8b8e7203-bdda-489e-bc20-f226f5b59c62"
    ));
    
    public static final String DEV_PREFIX = "[Dev]";
    public static final int DEV_PREFIX_COLOR = 0xFF6B6B;
    
    public static boolean isDeveloper(PlayerEntity player) {
        if (player == null) return false;
        return DEV_UUIDS.contains(player.getUuid().toString());
    }
}
