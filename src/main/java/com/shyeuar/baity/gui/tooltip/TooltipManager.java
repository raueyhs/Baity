package com.shyeuar.baity.gui.tooltip;

import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;

public class TooltipManager {
    private static final Map<String, TooltipInfo> tooltipMap = new HashMap<>();
    
    public static void registerTooltip(String name, String text, int color) {
        tooltipMap.put(name, new TooltipInfo(text, color));
    }
    
    public static void registerTooltip(String name, Text coloredText) {
        tooltipMap.put(name, new TooltipInfo(coloredText));
    }
    
    public static String getTooltipText(String name) {
        TooltipInfo info = tooltipMap.get(name);
        return info != null ? info.getText() : null;
    }
    
    public static Text getTooltipTextWithColors(String name) {
        TooltipInfo info = tooltipMap.get(name);
        return info != null ? info.getColoredText() : null;
    }
    
    public static boolean hasTooltip(String name) {
        return tooltipMap.containsKey(name);
    }
    
    private static class TooltipInfo {
        private final String text;
        private final Text coloredText;
        
        TooltipInfo(String text, int color) {
            this.text = text;
            this.coloredText = Text.literal(text).styled(style -> style.withColor(color));
        }
        
        TooltipInfo(Text coloredText) {
            this.text = coloredText.getString();
            this.coloredText = coloredText;
        }
        
        String getText() {
            return text;
        }
        
        Text getColoredText() {
            return coloredText;
        }
    }
}

