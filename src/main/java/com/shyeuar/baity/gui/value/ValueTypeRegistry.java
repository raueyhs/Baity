package com.shyeuar.baity.gui.value;

import java.util.HashMap;
import java.util.Map;

public class ValueTypeRegistry {
    private static final Map<Class<?>, ValueTypeHandler> typeHandlers = new HashMap<>();
    
    static {
        registerType(Boolean.class, new BooleanValueHandler());
        registerType(Double.class, new DoubleValueHandler());
        registerType(String.class, new StringValueHandler());
    }
    
    public static void registerType(Class<?> type, ValueTypeHandler handler) {
        typeHandlers.put(type, handler);
    }
   
    public static ValueTypeHandler getHandler(Class<?> type) {
        return typeHandlers.get(type);
    }
  
    public static ValueTypeHandler getHandlerForValue(Object value) {
        if (value == null) return null;
        return getHandler(value.getClass());
    }
 
    private static class BooleanValueHandler implements ValueTypeHandler {
        @Override
        public String formatValue(Object value) {
            if (value instanceof Boolean) {
                return (Boolean) value ? "ON" : "OFF";
            }
            return String.valueOf(value);
        }
        
        @Override
        public Object updateValue(Object currentValue, double delta) {
            return currentValue;
        }
        
        @Override
        public Class<?> getValueType() {
            return Boolean.class;
        }
    }
    
    private static class DoubleValueHandler implements ValueTypeHandler {
        @Override
        public String formatValue(Object value) {
            if (value instanceof Double) {
                return String.format("%.1f", (Double) value);
            }
            return String.valueOf(value);
        }
        
        @Override
        public Object updateValue(Object currentValue, double delta) {
            if (currentValue instanceof Double) {
                double increment = delta > 0 ? 0.1 : -0.1;
                double newValue = (Double) currentValue + increment;
                return Math.max(0.0, Math.min(100.0, newValue));
            }
            return currentValue;
        }
        
        @Override
        public Class<?> getValueType() {
            return Double.class;
        }
    }

    private static class StringValueHandler implements ValueTypeHandler {
        @Override
        public String formatValue(Object value) {
            return value != null ? value.toString() : "";
        }
        
        @Override
        public Object updateValue(Object currentValue, double delta) {
            return currentValue;
        }
        
        @Override
        public Class<?> getValueType() {
            return String.class;
        }
    }
}

