package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.Option;
import com.shyeuar.baity.gui.value.Value;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModuleUtils {

    public static boolean shouldRemoveCullingWeatherFilter() {
        return ConfigManager.cullingEnabled
                && ConfigManager.cullingRemoveRainSnow
                && ConfigManager.cullingRemoveWeatherFilter;
    }

    public static boolean getOptionBoolean(Module module, String name, boolean def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (!(v instanceof Option)) {
            return def;
        }
        if (!shouldExecuteSubModule(module, v)) {
            if (module.isEnabled()) {
                return false;
            }
            return def;
        }
        Object val = v.getValue();
        return val instanceof Boolean ? (Boolean) val : def;
    }
    
    public static boolean getOptionBooleanRaw(Module module, String name, boolean def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (!(v instanceof Option)) {
            return def;
        }
        Object val = v.getValue();
        return val instanceof Boolean ? (Boolean) val : def;
    }
    
    public static String getOptionString(Module module, String name, String def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (v == null) {
            return def;
        }
        if (!shouldExecuteSubModule(module, v)) {
            return def;
        }
        Object val = v.getValue();
        return val != null ? val.toString() : def;
    }
   
    public static String getOptionStringRaw(Module module, String name, String def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (v == null) {
            return def;
        }
        Object val = v.getValue();
        return val != null ? val.toString() : def;
    }
    
    public static Module getEnabledModule(String moduleName) {
        Module module = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName(moduleName);
        if (module == null || !module.isEnabled()) {
            return null;
        }
        return module;
    }
   
    public static boolean shouldExecuteSubModule(Module module, Value value) {
        if (value == null) {
            return false;
        }
        
        if (value.isIndependentOfParentModule()) {
            return true;
        }
        
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return passesAllAncestorGroupSwitches(module, value);
    }
    
    public static boolean shouldExecuteSubModule(String moduleName, String valueName) {
        Module module = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName(moduleName);
        if (module == null) {
            return false;
        }
        
        Value value = findValueByName(module.getValues(), valueName);
        return value != null && shouldExecuteSubModule(module, value);
    }

    private static Value findValueByName(Iterable<Value> values, String name) {
        if (values == null || name == null) {
            return null;
        }
        for (Value v : values) {
            if (v == null) continue;
            if (v.getName() != null && v.getName().equalsIgnoreCase(name)) {
                return v;
            }
            if (v instanceof GroupValue group) {
                Value nested = findValueByName(group.getChildren(), name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean passesAllAncestorGroupSwitches(Module module, Value target) {
        Value cursor = target;
        while (cursor != null) {
            GroupValue parent = findImmediateParentGroup(module.getValues(), cursor);
            if (parent == null) {
                break;
            }
            Option groupSwitch = parent.getSubModuleSwitchChild();
            if (groupSwitch != null) {
                Object sv = groupSwitch.getValue();
                boolean switchOn = sv instanceof Boolean && (Boolean) sv;
                if (!switchOn && cursor != groupSwitch) {
                    return false;
                }
            }
            cursor = parent;
        }
        return true;
    }

    private static GroupValue findImmediateParentGroup(Iterable<Value> roots, Value target) {
        if (roots == null || target == null) {
            return null;
        }
        for (Value root : roots) {
            GroupValue p = findImmediateParentUnderNode(root, target);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    private static GroupValue findImmediateParentUnderNode(Value node, Value target) {
        if (node instanceof GroupValue g) {
            for (Value c : g.getChildren()) {
                if (c == target) {
                    return g;
                }
                GroupValue deeper = findImmediateParentUnderNode(c, target);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return null;
    }
}
