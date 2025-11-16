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
                if (!shouldExecuteSubModule(module, v)) {
                    return def;
                }
                Object val = v.getValue();
                return val instanceof Boolean ? (Boolean) val : def;
            }
        }
        return def;
    }
    
    public static boolean getOptionBooleanRaw(Module module, String name, boolean def) {
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
                if (!shouldExecuteSubModule(module, v)) {
                    return def;
                }
                Object val = v.getValue();
                return val != null ? val.toString() : def;
            }
        }
        return def;
    }
   
    public static String getOptionStringRaw(Module module, String name, String def) {
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
   
    public static boolean shouldExecuteSubModule(Module module, com.shyeuar.baity.gui.value.Value value) {
        if (value == null) {
            return false;
        }
        
        if (value.isIndependentOfParentModule()) {
            return true;
        }
        
        return module != null && module.isEnabled();
    }
    
    public static boolean shouldExecuteSubModule(String moduleName, String valueName) {
        Module module = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName(moduleName);
        if (module == null) {
            return false;
        }
        
        for (com.shyeuar.baity.gui.value.Value value : module.getValues()) {
            if (value.getName().equals(valueName)) {
                return shouldExecuteSubModule(module, value);
            }
        }
        
        return false;
    }
}

