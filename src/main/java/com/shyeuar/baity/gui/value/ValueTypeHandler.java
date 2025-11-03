package com.shyeuar.baity.gui.value;

public interface ValueTypeHandler {
    String formatValue(Object value);
    
    Object updateValue(Object currentValue, double delta);
    
    Class<?> getValueType();
}

