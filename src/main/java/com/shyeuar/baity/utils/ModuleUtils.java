package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.modules.Module;
import com.shyeuar.baity.gui.values.Value;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 模块工具类
 * 提供模块相关的通用方法，避免代码重复
 */
@Environment(EnvType.CLIENT)
public class ModuleUtils {
    
    /**
     * 从模块中获取布尔选项值
     * @param module 模块
     * @param name 选项名称
     * @param def 默认值
     * @return 选项值
     */
    public static boolean getOptionBoolean(Module module, String name, boolean def) {
        if (module == null) {
            return def;
        }
        
        for (Value v : module.getValues()) {
            if (v instanceof com.shyeuar.baity.gui.values.Option && v.getName().equalsIgnoreCase(name)) {
                Object val = v.getValue();
                return val instanceof Boolean ? (Boolean) val : def;
            }
        }
        return def;
    }

    /**
     * 从模块中获取字符串选项值
     * @param module 模块
     * @param name 选项名称
     * @param def 默认值
     * @return 选项值
     */
    public static String getOptionString(Module module, String name, String def) {
        if (module == null) {
            return def;
        }
        
        for (Value v : module.getValues()) {
            if (v.getName().equalsIgnoreCase(name)) {
                Object val = v.getValue();
                return val != null ? val.toString() : def;
            }
        }
        return def;
    }
}

