package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ButtonValue implements Value {
    private final String name;
    private final String displayName;
    private Object value;  // 存储任意类型的值
    private final ModuleCategory category;
    private final String defaultDisplayText;  // 默认显示的文本（如"🚨NOTSET"）
  
    public ButtonValue(String name, String displayName, Object defaultValue, String defaultDisplayText, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.value = defaultValue;
        this.defaultDisplayText = defaultDisplayText;
        this.category = category;
    }

    public ButtonValue(String name, String displayName, Object defaultValue, ModuleCategory category) {
        this(name, displayName, defaultValue, "NOTSET", category);
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDisplayName() { return displayName; }
    
    @Override
    public Object getValue() { return value; }
    
    @Override
    public void setValue(Object value) { this.value = value; }
    
    @Override
    public ModuleCategory getCategory() { return category; }
    
    @Override
    public ValueStyle getStyle() {
        return ValueStyle.BUTTON_LIKE;
    }
    
    public String getDefaultDisplayText() {
        return defaultDisplayText;
    }
    
    public String getDisplayText(java.util.function.Function<Object, String> formatter) {
        if (formatter != null) {
            String formatted = formatter.apply(value);
            if (formatted != null && !formatted.isEmpty()) {
                return formatted;
            }
        }
        if (value == null || (value instanceof Number && ((Number) value).intValue() == 0)) {
            return defaultDisplayText;
        }
        return value != null ? value.toString() : defaultDisplayText;
    }
}


