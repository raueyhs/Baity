package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ModuleStyleRenderer {
   
   public static void renderModule(DrawContext context, MinecraftClient client, Module module, Theme theme,
                                  float x1, float y, float x2, float moduleHeight,
                                  float mouseX, float mouseY,
                                  boolean isListeningForKey, String currentKeyDisplay,
                                  java.util.function.Function<String, String> getTooltipText,
                                  java.util.function.Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                  TooltipInfo hoveredTooltipInfo) {

       boolean hovered = GuiRenderUtil.isHovered(x1, y, x2, y + moduleHeight, mouseX, mouseY);
       int enabledBg = new java.awt.Color(54, 42, 150).getRGB();
       int cardBg = module.isEnabled() ? enabledBg : theme.Modules.getRGB();
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + moduleHeight, 6, cardBg);

       if (hovered && !"ClickGUI".equals(module.getName())) {
           int hi = new java.awt.Color(255, 255, 255, 24).getRGB();
           int lx = (int)(x1 + 1);
           int ty = (int)(y + 1);
           int rx = (int)(x2 - 1);
           int by = (int)(y + moduleHeight - 1);
           context.fill(lx, ty, rx, by, hi);
       }

       String displayName = module.getName();
       if ("ClickGUI".equals(module.getName())) {
           displayName = "ClickGUI";
       }
       context.drawText(client.textRenderer, displayName, (int)(x1 + 10), (int)(y + 8), theme.FONT_C.getRGB(), false);

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
           context.drawText(client.textRenderer, arrow, (int)(x2 - 25), (int)(y + 8), theme.FONT_C.getRGB(), false);
       }

       if (module.getName().equals("ClickGUI")) {
           renderKeybindBox(context, client, theme, x1, y, x2, moduleHeight,
                          mouseX, mouseY, isListeningForKey, currentKeyDisplay);
       }
   }

   public static void renderKeybindBox(DrawContext context, MinecraftClient client, Theme theme,
                                      float containerX1, float containerY, float containerX2, float containerHeight,
                                      float mouseX, float mouseY,
                                      boolean isListening, String keyDisplay) {
       String keyText = isListening ? "Press a key..." : keyDisplay;
       renderKeybindBoxContent(context, client, theme, containerX2, containerY, containerHeight, mouseX, mouseY, isListening, keyText);
   }

   public static void renderKeybindBoxContent(DrawContext context, MinecraftClient client, Theme theme,
                                              float containerX2, float containerY, float containerHeight,
                                              float mouseX, float mouseY, boolean isListening, String displayText) {
       String plainText = displayText.replaceAll("§[0-9a-fklmnor]", "");
       int textWidth = client.textRenderer.getWidth(plainText);
       int boxWidth = textWidth + 16;
       float boxCenterY = containerY + containerHeight / 2f;
       int boxHeight = 12;

       int boxX1 = (int)(containerX2 - boxWidth - 10);
       int boxY1 = (int)(boxCenterY - boxHeight / 2f);
       int boxX2 = (int)(containerX2 - 10);
       int boxY2 = (int)(boxCenterY + boxHeight / 2f);

       boolean boxHovered = GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, mouseX, mouseY);
       int boxBgColor = isListening ? theme.BG_3.getRGB() :
                       (boxHovered ? new java.awt.Color(255, 255, 255, 24).getRGB() : theme.BG_2.getRGB());
       context.fill(boxX1, boxY1, boxX2, boxY2, boxBgColor);

       int baseX = boxX1 + 8;
       int baseY = (int)(boxCenterY - 4);

       if (isListening) {
           net.minecraft.text.Text textObj = net.minecraft.text.Text.literal(displayText);
           context.drawText(client.textRenderer, textObj, baseX, baseY, theme.FONT_C.getRGB(), false);
       } else {
           String displayPlainText = displayText.replaceAll("§[0-9a-fklmnor]", "");

           if (displayPlainText.startsWith("✎")) {
               String prefix = "✎";
               String keyName = displayPlainText.substring(1);
               int prefixRGB = com.shyeuar.baity.utils.KeyMappingUtils.getModuleEnabledPurpleRGB();
               int keyNameRGB = theme.FONT.getRGB();

               net.minecraft.text.Text prefixText = net.minecraft.text.Text.literal(prefix);
               context.drawText(client.textRenderer, prefixText, baseX, baseY, prefixRGB, false);

               int prefixWidth = client.textRenderer.getWidth(prefix);
               net.minecraft.text.Text keyTextObj = net.minecraft.text.Text.literal(keyName);
               context.drawText(client.textRenderer, keyTextObj, baseX + prefixWidth, baseY, keyNameRGB, false);
           } else if (displayPlainText.startsWith("☄")) {
               String prefix = "☄";
               String notsetText = displayPlainText.substring(1);
               int prefixRGB = 0xFFFF00;
               int notsetRGB = 0xAAAAAA;

               net.minecraft.text.Text prefixText = net.minecraft.text.Text.literal(prefix);
               context.drawText(client.textRenderer, prefixText, baseX, baseY, prefixRGB, false);

               int prefixWidth = client.textRenderer.getWidth(prefix);
               net.minecraft.text.Text notsetTextObj = net.minecraft.text.Text.literal(notsetText);
               context.drawText(client.textRenderer, notsetTextObj, baseX + prefixWidth, baseY, notsetRGB, false);
           } else {
               net.minecraft.text.Text textObj = net.minecraft.text.Text.literal(displayText);
               context.drawText(client.textRenderer, textObj, baseX, baseY, theme.FONT.getRGB(), false);
           }
       }
   }

   public static class TooltipInfo {
       public String tooltip;
       public net.minecraft.text.Text tooltipText;
       public int x, y;
   }
}


