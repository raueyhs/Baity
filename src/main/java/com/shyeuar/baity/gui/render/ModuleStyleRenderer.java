package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ModuleStyleRenderer {
    
    private static ClickGuiState currentState = null;
    
    public static void setState(ClickGuiState state) {
        currentState = state;
    }
    
    private static ClickGuiState.ShimmerAnimationState getShimmerState(String moduleName) {
        if (currentState == null) return null;
        return currentState.getModuleShimmerAnimations().get(moduleName);
    }
   
   public static void renderModule(GuiGraphicsExtractor context, Minecraft client, Module module, Theme theme,
                                  float x1, float y, float x2, float moduleHeight,
                                  float mouseX, float mouseY,
                                  boolean isListeningForKey, String currentKeyDisplay,
                                  java.util.function.Function<String, String> getTooltipText,
                                  java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                  TooltipInfo hoveredTooltipInfo) {

      boolean hovered = mouseX >= x1 && mouseY >= y && mouseX <= x2 && mouseY <= y + moduleHeight;
      
      com.shyeuar.baity.gui.theme.LinearTheme.applyToTheme(theme);
      
      if (module.isEnabled()) {
          int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
          int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
          GuiRenderUtil.draw3DGradientRect(context, x1, y, x2, y + moduleHeight, accentStart, accentEnd, 6f);
      } else {
          int cardBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
          GuiRenderUtil.drawFrostedGlass(context, x1, y, x2, y + moduleHeight, cardBg, 6f);
          GuiRenderUtil.draw3DRect(context, x1, y, x2, y + moduleHeight, cardBg, 6f);
      }

      if (!"ClickGUI".equals(module.getName())) {
          com.shyeuar.baity.gui.internal.ClickGuiState.ShimmerAnimationState shimmerState = 
              getShimmerState(module.getName());
          
          if (shimmerState != null && (shimmerState.isActive || shimmerState.isExiting || shimmerState.progress > 0f)) {
              boolean isEnabled = module.isEnabled();
              com.shyeuar.baity.gui.animation.ShimmerEffect.renderHoverShimmer(
                  context, x1, y, x2, y + moduleHeight,
                  shimmerState.mouseX, shimmerState.mouseY,
                  shimmerState.isActive, shimmerState.isExiting,
                  shimmerState.progress, shimmerState.direction,
                  isEnabled);
          }
      }

       String displayName = module.getName();
       if ("ClickGUI".equals(module.getName())) {
           displayName = "ClickGUI";
       }
       context.text(client.font, displayName, (int)(x1 + 10), (int)(y + 8), theme.FONT_C.getRGB(), false);

       if (hovered) {
           String tooltip = getTooltipText.apply(module.getName());
           if (tooltip != null) {
               hoveredTooltipInfo.tooltip = tooltip;
               hoveredTooltipInfo.tooltipText = getTooltipTextWithColors.apply(module.getName());
               float tooltipOffset = 5f;
               hoveredTooltipInfo.x = (int)(mouseX + tooltipOffset);
               hoveredTooltipInfo.y = (int)(mouseY + tooltipOffset);
           }
       }

       boolean hasChildren = false;
       for (com.shyeuar.baity.gui.value.Value v : module.getValues()) {
           if (!"enabled".equals(v.getName())) {
               hasChildren = true;
               break;
           }
       }
       if (hasChildren && !module.getName().equals("ClickGUI")) {
           String arrow = module.isExpanded() ? "▼" : "▶";
           context.text(client.font, arrow, (int)(x2 - 25), (int)(y + 8), theme.FONT_C.getRGB(), false);
       }

       if (module.getName().equals("ClickGUI")) {
           renderKeybindBox(context, client, theme, x1, y, x2, moduleHeight,
                          mouseX, mouseY, isListeningForKey, currentKeyDisplay);
       }
   }

   public static void renderKeybindBox(GuiGraphicsExtractor context, Minecraft client, Theme theme,
                                      float containerX1, float containerY, float containerX2, float containerHeight,
                                      float mouseX, float mouseY,
                                      boolean isListening, String keyDisplay) {
       String keyText = isListening ? "Press a key..." : keyDisplay;
       renderKeybindBoxContent(context, client, theme, containerX2, containerY, containerHeight, mouseX, mouseY, isListening, keyText);
   }

   public static void renderKeybindBoxContent(GuiGraphicsExtractor context, Minecraft client, Theme theme,
                                              float containerX2, float containerY, float containerHeight,
                                              float mouseX, float mouseY, boolean isListening, String displayText) {
       renderKeybindBoxContent(context, client, theme, containerX2, containerY, containerHeight, mouseX, mouseY,
               isListening, displayText, 0);
   }

   public static void renderKeybindBoxContent(GuiGraphicsExtractor context, Minecraft client, Theme theme,
                                              float containerX2, float containerY, float containerHeight,
                                              float mouseX, float mouseY, boolean isListening, String displayText,
                                              int textColorOverride) {
       if (displayText == null || displayText.isEmpty()) {
           displayText = "☄ NOTSET";
       }
       String plainText = displayText.replaceAll("§[0-9a-fklmnor]", "");
       int textWidth = client.font.width(plainText);
       int boxWidth = textWidth + 16;
       float boxCenterY = containerY + containerHeight / 2f;
       int boxHeight = 12;

       int boxX1 = (int)(containerX2 - boxWidth - 10);
       int boxY1 = (int)(boxCenterY - boxHeight / 2f);
       int boxX2 = (int)(containerX2 - 10);
       int boxY2 = (int)(boxCenterY + boxHeight / 2f);

       boolean boxHovered = GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, mouseX, mouseY);
      int boxBgColor = (boxHovered ? new java.awt.Color(60, 60, 60, 80).getRGB() : new java.awt.Color(40, 40, 40, 50).getRGB());
       GuiRenderUtil.draw3DRect(context, boxX1, boxY1, boxX2, boxY2, boxBgColor, 0f);

       int baseX = boxX1 + 8;
       int baseY = (int)(boxCenterY - 4);
       int valueTextColor = textColorOverride != 0 ? textColorOverride : theme.FONT.getRGB();

       if (isListening) {
           String hintText = "Press Backspace to reset";
           int hintColor = 0xFFFFFF00;
           int hintX = boxX1 - client.font.width(hintText) - 8;
           context.text(client.font, hintText, hintX, baseY, hintColor, false);
           
           net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
           context.text(client.font, textObj, baseX, baseY, theme.FONT_C.getRGB(), false);
       } else {
           String displayPlainText = displayText.replaceAll("§[0-9a-fklmnor]", "");

           if (displayPlainText.startsWith("✎")) {
               String prefix = "✎";
               String keyName = displayPlainText.substring(1);
               int prefixRGB = com.shyeuar.baity.utils.KeyMappingUtils.getModuleEnabledPurpleRGB();
               int keyNameRGB = valueTextColor;

               net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
               context.text(client.font, prefixText, baseX, baseY, prefixRGB, false);

               int prefixWidth = client.font.width(prefix);
               net.minecraft.network.chat.Component keyTextObj = net.minecraft.network.chat.Component.literal(keyName);
               context.text(client.font, keyTextObj, baseX + prefixWidth, baseY, keyNameRGB, false);
           } else if (displayPlainText.startsWith("☄") || 
                       displayPlainText.toUpperCase().contains("NOTSET") || 
                       displayPlainText.toUpperCase().contains("UNKNOWN")) {
               String prefix = "☄";
               String notsetText = displayPlainText.startsWith("☄") ? displayPlainText.substring(1) : (" " + displayPlainText);
               int prefixRGB = 0xFFFFFF00;  // 黄色
               int notsetRGB = 0xFFAAAAAA;  // 灰色

               net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
               context.text(client.font, prefixText, baseX, baseY, prefixRGB, false);

               int prefixWidth = client.font.width(prefix);
               net.minecraft.network.chat.Component notsetTextObj = net.minecraft.network.chat.Component.literal(notsetText);
               context.text(client.font, notsetTextObj, baseX + prefixWidth, baseY, notsetRGB, false);
           } else {
               net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
               context.text(client.font, textObj, baseX, baseY, valueTextColor, false);
           }
       }
   }

   public static class TooltipInfo {
       public String tooltip;
       public net.minecraft.network.chat.Component tooltipText;
       public int x, y;
   }
}

