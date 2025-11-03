package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ValueStyleRenderer {
   
   public static void renderValue(DrawContext context, MinecraftClient client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       
       ValueStyle style = value.getStyle();
       
       if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
           renderButtonLikeValue(context, client, module, (ButtonValue) value, theme,
                              x1, y, x2, subOptionHeight,
                              mouseX, mouseY, localAlpha,
                              getDisplayTextFormatter, listeningButtonValueName);
       } else {
           renderDefaultValue(context, client, module, value, theme,
                          x1, y, x2, subOptionHeight,
                          mouseX, mouseY, localAlpha,
                          getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
       }
   }
   
   public static void renderDefaultValue(DrawContext context, MinecraftClient client, Module module, Value value, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       float mouseX, float mouseY, int localAlpha,
                                       java.util.function.Function<String, String> getTooltipText,
                                       java.util.function.Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                       ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       
       boolean subHovered = GuiRenderUtil.isHovered(x1, y, x2, y + subOptionHeight, mouseX, mouseY);
       int baseValueColor = subHovered ? new java.awt.Color(60, 60, 60, 80).getRGB() : 
                           new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + subOptionHeight, 6, valueColor);
       
       if (subHovered) {
           String tooltip = getTooltipText.apply(value.getName());
           if (tooltip != null) {
               hoveredTooltipInfo.tooltip = tooltip;
               hoveredTooltipInfo.tooltipText = getTooltipTextWithColors.apply(value.getName());
               float tooltipOffset = 5f;
               hoveredTooltipInfo.x = (int)(mouseX + tooltipOffset);
               hoveredTooltipInfo.y = (int)(mouseY + tooltipOffset);
           }
       }
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       String displayText = value.getDisplayName();
       int warningTextColor = textColor;
       if ("show own nametag".equals(value.getName())) {
           displayText = "⚠ " + displayText;
           warningTextColor = (com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR & 0x00FFFFFF) | (localAlpha << 24);
       }
       context.drawText(client.textRenderer, displayText, (int)(x1 + 8), (int)(y + 6), warningTextColor, false);
       
       String status;
       int statusColor;
       Object val = value.getValue();
       var handler = ValueTypeRegistry.getHandlerForValue(val);
       if (handler != null) {
           status = handler.formatValue(val);
           if (val instanceof Boolean) {
               boolean boolValue = (Boolean) val;
               statusColor = boolValue ? theme.BG_3.getRGB() : theme.FONT.getRGB();
           } else {
               statusColor = theme.FONT.getRGB();
           }
       } else {
           status = val != null ? val.toString() : "";
           statusColor = theme.FONT.getRGB();
       }
       if (!module.isEnabled()) {
           statusColor = theme.FONT.getRGB();
       }
       statusColor = (statusColor & 0x00FFFFFF) | (localAlpha << 24);
       
       int statusX = (int)(x2 - 40);
       if (val instanceof Double) {
           statusX = (int)(x2 - 60);
       } else if (val instanceof String) {
           statusX = (int)(x2 - 80);
       }
       context.drawText(client.textRenderer, status, statusX, (int)(y + 6), statusColor, false);
   }
   
   public static void renderButtonLikeValue(DrawContext context, MinecraftClient client, Module module, ButtonValue buttonValue, Theme theme,
                                             float x1, float y, float x2, float subOptionHeight,
                                             float mouseX, float mouseY, int localAlpha,
                                             java.util.function.Function<Object, String> getDisplayTextFormatter,
                                             String listeningButtonValueName) {
       
       int buttonEnabledBg = new java.awt.Color(54, 42, 150).getRGB();
       int valueColor = (buttonEnabledBg & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + subOptionHeight, 6, valueColor);
       
       int textColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawText(client.textRenderer, buttonValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       boolean isListeningThis = listeningButtonValueName != null && listeningButtonValueName.equals(buttonValue.getName());
       String boxText;
       if (isListeningThis) {
           boxText = "Press a key...";
       } else {
           boxText = buttonValue.getDisplayText(getDisplayTextFormatter);
       }
       
       ModuleStyleRenderer.renderKeybindBoxContent(context, client, theme, x2, y, subOptionHeight, mouseX, mouseY, isListeningThis, boxText);
   }
}


