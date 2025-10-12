package com.shyeuar.baity.config;

/**
 * 开发者配置类
 * 此文件包含开发者相关的特殊配置，用于标识模组作者

 * 算是我自己的小小私心啦喵~
 */
public class DevConfig {
    public static final String DEV_USERNAME = "11YearCookieBuff";
    public static final String DEV_PREFIX = "[Dev]";
    public static final int DEV_PREFIX_COLOR = 0xFF6B6B; 
    
    /**
     * @param username 要检查的用户名
     * @return 如果是开发者返回true，否则返回false
     */
    public static boolean isDeveloper(String username) {
        return DEV_USERNAME.equals(username);
    }
}
