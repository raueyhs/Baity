package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.value.Value;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModuleUtils {
    
    public static boolean getOptionBoolean(Module module, String name, boolean def) {
        if (module == null) {
            return def;
        }
        
        for (Value v : module.getValues()) {
            if (v instanceof com.shyeuar.baity.gui.value.Option && v.getName().equalsIgnoreCase(name)) {
                Object val = v.getValue();
                return val instanceof Boolean ? (Boolean) val : def;
            }
        }
        return def;
    }
    
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
    
    public static Module getEnabledModule(String moduleName) {
        Module module = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName(moduleName);
        if (module == null || !module.isEnabled()) {
            return null;
        }
        return module;
    }
}

