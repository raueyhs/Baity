package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.CrosshairPainterValue;
import com.shyeuar.baity.gui.value.GradientEditorValue;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.SliderValue;
import com.shyeuar.baity.gui.value.TextLineInputValue;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import com.shyeuar.baity.utils.NickRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ValueStyleRenderer {

   public static class CrosshairPainterLayout {
       public final float blockHeight;
       public final float canvasX1, canvasY1, canvasX2, canvasY2;
       public final int gridX1, gridY1, cellPx, gridSizePx;
       public final float activeBtnX1, activeBtnY1, activeBtnX2, activeBtnY2;
       public final float staticBtnX1, staticBtnY1, staticBtnX2, staticBtnY2;
       public final float resetX1, resetY1, resetX2, resetY2;
       public final float previewX1, previewY1, previewX2, previewY2;
       public final String resetText;

       public CrosshairPainterLayout(float blockHeight,
                                     float canvasX1, float canvasY1, float canvasX2, float canvasY2,
                                     int gridX1, int gridY1, int cellPx, int gridSizePx,
                                     float activeBtnX1, float activeBtnY1, float activeBtnX2, float activeBtnY2,
                                     float staticBtnX1, float staticBtnY1, float staticBtnX2, float staticBtnY2,
                                     float resetX1, float resetY1, float resetX2, float resetY2,
                                     float previewX1, float previewY1, float previewX2, float previewY2,
                                     String resetText) {
           this.blockHeight = blockHeight;
           this.canvasX1 = canvasX1; this.canvasY1 = canvasY1; this.canvasX2 = canvasX2; this.canvasY2 = canvasY2;
           this.gridX1 = gridX1; this.gridY1 = gridY1; this.cellPx = cellPx; this.gridSizePx = gridSizePx;
           this.activeBtnX1 = activeBtnX1; this.activeBtnY1 = activeBtnY1; this.activeBtnX2 = activeBtnX2; this.activeBtnY2 = activeBtnY2;
           this.staticBtnX1 = staticBtnX1; this.staticBtnY1 = staticBtnY1; this.staticBtnX2 = staticBtnX2; this.staticBtnY2 = staticBtnY2;
           this.resetX1 = resetX1; this.resetY1 = resetY1; this.resetX2 = resetX2; this.resetY2 = resetY2;
           this.previewX1 = previewX1; this.previewY1 = previewY1; this.previewX2 = previewX2; this.previewY2 = previewY2;
           this.resetText = resetText;
       }
   }

   public static CrosshairPainterLayout computeCrosshairPainterLayout(Minecraft client, CrosshairPainterValue value,
                                                                      float x1, float y, float x2, float subOptionHeight) {
       float blockHeight = getCrosshairPainterHeight(subOptionHeight);
       float pad = 8f;
       float contentY1 = y + 22f;
       float contentY2 = y + blockHeight - 8f;

       float canvasH = Math.max(60f, contentY2 - contentY1);
       float canvasW = Math.max(60f, (x2 - x1) * 0.56f - pad * 2f);
       float canvasSize = Math.min(canvasH, canvasW);
       canvasSize = (float) Math.floor(canvasSize);
       float canvasX1 = x1 + pad;
       float canvasY1 = contentY1 + ((contentY2 - contentY1) - canvasSize) * 0.5f;
       float canvasX2 = canvasX1 + canvasSize;
       float canvasY2 = canvasY1 + canvasSize;

       int n = value.getSize();
       int cellPx = Math.max(1, (int) Math.floor(canvasSize / n));
       int gridSizePx = cellPx * n;
       int gridX1 = (int) Math.floor(canvasX1 + (canvasSize - gridSizePx) * 0.5f);
       int gridY1 = (int) Math.floor(canvasY1 + (canvasSize - gridSizePx) * 0.5f);

       float rightX1 = canvasX2 + 10f;
       float rightX2 = x2 - pad;

       String resetText = value.isResetArmed() ? "reclick to confirm" : "Reset";
       float btnH = 16f;
       float activeW = Math.max(26f, client.font.width("AL") + 12f);
       float staticW = Math.max(26f, client.font.width("SL") + 12f);
       float btnW = Math.max(activeW, staticW);
       float maxResetW = Math.max(30f, rightX2 - rightX1);
       float resetW = Math.min(maxResetW, Math.max(30f, client.font.width(resetText) + 12f));
       float stackY = contentY1 + 4f;
       float gap = 6f;

       float pairGap = 6f;
       float pairW = btnW * 2f + pairGap;
       float pairX1 = rightX1 + Math.max(0f, (rightX2 - rightX1 - pairW) * 0.5f);

       float activeBtnX1 = pairX1;
       float activeBtnY1 = stackY;
       float activeBtnX2 = activeBtnX1 + btnW;
       float activeBtnY2 = activeBtnY1 + btnH;

       float staticBtnX1 = activeBtnX2 + pairGap;
       float staticBtnY1 = activeBtnY1;
       float staticBtnX2 = staticBtnX1 + btnW;
       float staticBtnY2 = activeBtnY2;

       float resetX1 = rightX1 + Math.max(0f, (rightX2 - rightX1 - resetW) * 0.5f);
       float resetY1 = staticBtnY2 + gap;
       float resetX2 = Math.min(rightX2, resetX1 + resetW);
       float resetY2 = resetY1 + btnH;

       float previewX1 = rightX1;
       float previewY1 = resetY2 + 8f;
       float previewX2 = rightX2;
       float previewY2 = contentY2;
       if (previewY2 < previewY1) previewY2 = previewY1 + 1f;

       return new CrosshairPainterLayout(
           blockHeight,
           canvasX1, canvasY1, canvasX2, canvasY2,
           gridX1, gridY1, cellPx, gridSizePx,
           activeBtnX1, activeBtnY1, activeBtnX2, activeBtnY2,
           staticBtnX1, staticBtnY1, staticBtnX2, staticBtnY2,
           resetX1, resetY1, resetX2, resetY2,
           previewX1, previewY1, previewX2, previewY2,
           resetText
       );
   }
   
   public static void renderValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
      renderValue(context, client, module, value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha,
                 getTooltipText, getTooltipTextWithColors, getDisplayTextFormatter, listeningButtonValueName, hoveredTooltipInfo,
                   null, "", null, "", null, "", 0);
   }
   
   public static void renderValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider,
                                 String sliderInputText,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                 String gradientInputText,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.TextInputInfo editingTextInput,
                                 String textInputValue) {
       int endCp = textInputValue == null ? 0 : textInputValue.codePointCount(0, textInputValue.length());
       renderValue(context, client, module, value, theme,
           x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha,
           getTooltipText, getTooltipTextWithColors, getDisplayTextFormatter, listeningButtonValueName, hoveredTooltipInfo,
           editingSlider, sliderInputText, editingGradient, gradientInputText, editingTextInput, textInputValue, endCp);
   }
                                 
   public static void renderValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider,
                                 String sliderInputText,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                 String gradientInputText,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.TextInputInfo editingTextInput,
                                 String textInputValue,
                                 int editingTextCursorCpIndex) {
       
      ValueStyle style = value.getStyle();
       
      if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
           renderButtonLikeValue(context, client, module, (ButtonValue) value, theme,
                              x1, y, x2, subOptionHeight,
                              mouseX, mouseY, localAlpha,
                              getDisplayTextFormatter, listeningButtonValueName);
      } else if (style == ValueStyle.GROUP && value instanceof GroupValue) {
          renderGroupValue(context, client, (GroupValue) value, theme, x1, y, x2, subOptionHeight, localAlpha);
       } else if (style == ValueStyle.SLIDER && value instanceof SliderValue) {
           renderSliderValue(context, client, module, (SliderValue) value, theme,
                            x1, y, x2, subOptionHeight,
                            mouseX, mouseY, localAlpha, editingSlider, sliderInputText);
       } else if (style == ValueStyle.COLOR_PALETTE && value instanceof com.shyeuar.baity.gui.value.ColorPaletteValue) {
           renderColorPaletteValue(context, client, module, (com.shyeuar.baity.gui.value.ColorPaletteValue) value, theme,
                                   x1, y, x2, subOptionHeight,
                                   mouseX, mouseY, localAlpha);
      } else if (style == ValueStyle.GRADIENT_EDITOR && value instanceof GradientEditorValue) {
          renderGradientEditorValue(context, client, (GradientEditorValue) value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingGradient, gradientInputText, hoveredTooltipInfo);
      } else if (style == ValueStyle.CROSSHAIR_PAINTER && value instanceof CrosshairPainterValue) {
          renderCrosshairPainterValue(context, client, (CrosshairPainterValue) value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, hoveredTooltipInfo);
      } else if (style == ValueStyle.TEXT_LINE_INPUT && value instanceof TextLineInputValue) {
          renderTextLineInputValue(context, client, module, (TextLineInputValue) value, theme,
              x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingTextInput, textInputValue,
              editingTextCursorCpIndex, getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
      } else {
           renderDefaultValue(context, client, module, value, theme,
                          x1, y, x2, subOptionHeight,
                          mouseX, mouseY, localAlpha,
                          getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
       }
   }

   public static void renderTextLineInputValue(GuiGraphics context, Minecraft client, Module module, TextLineInputValue value, Theme theme,
                                               float x1, float y, float x2, float subOptionHeight,
                                               float mouseX, float mouseY, int localAlpha,
                                               com.shyeuar.baity.gui.internal.ClickGuiState.TextInputInfo editingTextInput,
                                               String textInputValue,
                                               int editingTextCursorCpIndex,
                                               java.util.function.Function<String, String> getTooltipText,
                                               java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                               ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       float lineX1 = x1 + (x2 - x1) * 0.52f;
       float lineX2 = x2 - 10;
       float lineY = y + subOptionHeight - 4;
      float hoverY1 = lineY - 10;
      float hoverY2 = lineY + 5;
       boolean hovered = GuiRenderUtil.isHovered(lineX1, hoverY1, lineX2, hoverY2, mouseX, mouseY);

       if (hoveredTooltipInfo != null) {
           float tooltipX1 = x1 + 4;
           float tooltipX2 = Math.max(tooltipX1, lineX1 - 3);
           boolean tooltipHovered = GuiRenderUtil.isHovered(tooltipX1, y + 3, tooltipX2, y + subOptionHeight - 3, mouseX, mouseY);
           if (tooltipHovered) {
               String tooltip = getTooltipText.apply(value.getName());
               if (tooltip != null) {
                   hoveredTooltipInfo.tooltip = tooltip;
                   hoveredTooltipInfo.tooltipText = getTooltipTextWithColors.apply(value.getName());
                   hoveredTooltipInfo.x = (int) (mouseX + 5);
                   hoveredTooltipInfo.y = (int) (mouseY + 5);
               }
           }
       }

       int interactiveYellow = (new java.awt.Color(255, 255, 0, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       boolean editing = editingTextInput != null
           && editingTextInput.moduleName.equals(module.getName())
           && editingTextInput.valueName.equals(value.getName());
       int lineColor = (hovered || editing)
           ? interactiveYellow
           : ((new java.awt.Color(120, 120, 120, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.drawRoundedRect(context, lineX1, lineY, lineX2, lineY + 1, 0, lineColor);

       String raw = editing ? textInputValue : String.valueOf(value.getValue());
       String preview = raw == null ? "" : raw;
       if (editing) {
           int cpCursor = Math.max(0, editingTextCursorCpIndex);
           int charPos = cpIndexToCharIndex(preview, cpCursor);
           preview = preview.substring(0, charPos) + "_" + preview.substring(charPos);
       }
       preview = limitByCodePoints(preview, 28);
       int valueTextColor = (hovered || editing) ? interactiveYellow : textColor;
       context.drawString(client.font, preview, (int) lineX1, (int) (lineY - 9), valueTextColor, false);
   }

   private static int cpIndexToCharIndex(String s, int cpIndex) {
       if (s == null) return 0;
       int cpCount = s.codePointCount(0, s.length());
       int target = Math.max(0, Math.min(cpIndex, cpCount));
       int curCp = 0;
       for (int charIdx = 0; charIdx < s.length(); ) {
           if (curCp == target) return charIdx;
           int cp = s.codePointAt(charIdx);
           charIdx += Character.charCount(cp);
           curCp++;
       }
       return s.length();
   }

   private static String limitByCodePoints(String s, int maxCp) {
       if (s == null) return "";
       int cpCount = s.codePointCount(0, s.length());
       if (cpCount <= maxCp) return s;
       int cpSeen = 0;
       int charIdx = 0;
       while (charIdx < s.length()) {
           int cp = s.codePointAt(charIdx);
           if (cpSeen >= maxCp) break;
           charIdx += Character.charCount(cp);
           cpSeen++;
       }
       return s.substring(0, charIdx);
   }

   public static void renderGroupValue(GuiGraphics context, Minecraft client, GroupValue groupValue, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       int localAlpha) {
      int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
      int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);

      int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      context.drawString(client.font, groupValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);

      String arrow = groupValue.isExpanded() ? "▼" : "▶";
      int arrowColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      context.drawString(client.font, arrow, (int)(x2 - 16), (int)(y + 6), arrowColor, false);
   }

   
   public static void renderDefaultValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       float mouseX, float mouseY, int localAlpha,
                                       java.util.function.Function<String, String> getTooltipText,
                                       java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                       ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       
       boolean subHovered = GuiRenderUtil.isHovered(x1, y, x2, y + subOptionHeight, mouseX, mouseY);
       int baseValueColor = subHovered ? new java.awt.Color(60, 60, 60, 80).getRGB() : 
                           new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
       
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
       context.drawString(client.font, displayText, (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       String status;
       int statusColor;
       Object val = value.getValue();
       var handler = ValueTypeRegistry.getHandlerForValue(val);
       if (handler != null) {
           status = handler.formatValue(val);
          if (val instanceof Boolean) {
              boolean boolValue = (Boolean) val;
              statusColor = boolValue ? com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB() : theme.FONT.getRGB();
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
       context.drawString(client.font, status, statusX, (int)(y + 6), statusColor, false);
   }
   
   public static void renderButtonLikeValue(GuiGraphics context, Minecraft client, Module module, ButtonValue buttonValue, Theme theme,
                                             float x1, float y, float x2, float subOptionHeight,
                                             float mouseX, float mouseY, int localAlpha,
                                             java.util.function.Function<Object, String> getDisplayTextFormatter,
                                             String listeningButtonValueName) {
       
       Object buttonVal = buttonValue.getValue();
       boolean isEnabled = buttonVal instanceof Boolean && (Boolean)buttonVal;
       
       if (isEnabled) {
           int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
           int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
           int aStart = ((accentStart >> 24) & 0xFF) * localAlpha / 255;
           int aEnd = ((accentEnd >> 24) & 0xFF) * localAlpha / 255;
           accentStart = (aStart << 24) | (accentStart & 0x00FFFFFF);
           accentEnd = (aEnd << 24) | (accentEnd & 0x00FFFFFF);
           GuiRenderUtil.draw3DGradientRect(context, x1, y, x2, y + subOptionHeight, accentStart, accentEnd, 6f);
       } else {
           int cardBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
           int valueColor = (cardBg & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawFrostedGlass(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
           GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
       }
       
       int textColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, buttonValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       boolean isListeningThis = listeningButtonValueName != null && listeningButtonValueName.equals(buttonValue.getName());
       String boxText;
       if (isListeningThis) {
           boxText = "Press a key...";
       } else {
           boxText = buttonValue.getDisplayText(getDisplayTextFormatter);
       }
       
       ModuleStyleRenderer.renderKeybindBoxContent(context, client, theme, x2, y, subOptionHeight, mouseX, mouseY, isListeningThis, boxText);
   }

   
   public static void renderSliderValue(GuiGraphics context, Minecraft client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha) {
       renderSliderValue(context, client, module, sliderValue, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, null, "");
   }
   
   public static void renderSliderValue(GuiGraphics context, Minecraft client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha,
                                        com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider, String inputText) {
       
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, sliderValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       int subX2 = (int)(x2 - 4);
       
       int resetBoxWidth = 30;
       int resetBoxHeight = 12;
       int resetBoxX = subX2 - resetBoxWidth - 6;
       int resetBoxY = (int)(y + (subOptionHeight - resetBoxHeight) / 2);
       
       boolean resetHovered = GuiRenderUtil.isHovered(resetBoxX, resetBoxY, resetBoxX + resetBoxWidth, resetBoxY + resetBoxHeight, mouseX, mouseY);
       int resetBgColor = resetHovered ? 
           (new java.awt.Color(70, 70, 70, 200).getRGB() & 0x00FFFFFF) | (localAlpha << 24) :
           (new java.awt.Color(40, 40, 40, 200).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int resetBorderColor = (new java.awt.Color(80, 80, 80, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, resetBoxX, resetBoxY, resetBoxX + resetBoxWidth, resetBoxY + resetBoxHeight, 3, resetBgColor);
       GuiRenderUtil.drawRoundedRectOutline(context, resetBoxX, resetBoxY, resetBoxX + resetBoxWidth, resetBoxY + resetBoxHeight, 3, resetBorderColor);
       
       int resetTextColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       String resetText = "Reset";
       int resetTextWidth = client.font.width(resetText);
       context.drawString(client.font, resetText, resetBoxX + (resetBoxWidth - resetTextWidth) / 2, resetBoxY + 2, resetTextColor, false);
       
       boolean isEditing = editingSlider != null && 
                          editingSlider.moduleName.equals(module.getName()) && 
                          editingSlider.valueName.equals(sliderValue.getName());
       
       String valueText = isEditing ? inputText + "_" : sliderValue.getFormattedValue();
       int valueTextWidth = client.font.width(valueText);
       int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
       int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
       int valueDisplayY = (int)(y + 4);
       
       int lineY = (int)(y + subOptionHeight - 6);
       int lineColor = (new java.awt.Color(100, 100, 100, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, valueDisplayX, lineY, valueDisplayX + valueDisplayWidth, lineY + 1, 0, lineColor);
       
       int valueTextColor = isEditing ? 
           (new java.awt.Color(255, 255, 100, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24) :
           (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int textX = valueDisplayX + (valueDisplayWidth - client.font.width(valueText)) / 2;
       context.drawString(client.font, valueText, textX, valueDisplayY, valueTextColor, false);
       
       int sliderWidth = 80;
       int sliderHeight = 4;
       int sliderX = valueDisplayX - sliderWidth - 10;
       int sliderY = (int)(y + (subOptionHeight - sliderHeight) / 2);
       
       int sliderBgColor = (new java.awt.Color(20, 20, 20, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 2, sliderBgColor);
       
       int sliderBorderColor = (new java.awt.Color(60, 60, 60, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 2, sliderBorderColor);
       
       double percentage = sliderValue.getPercentage();
       int filledWidth = (int)(sliderWidth * percentage);
       if (filledWidth > 0) {
           int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
           int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
           int aStart = ((accentStart >> 24) & 0xFF) * localAlpha / 255;
           int aEnd = ((accentEnd >> 24) & 0xFF) * localAlpha / 255;
           accentStart = (aStart << 24) | (accentStart & 0x00FFFFFF);
           accentEnd = (aEnd << 24) | (accentEnd & 0x00FFFFFF);
           GuiRenderUtil.drawGradientRect(context, sliderX, sliderY, sliderX + filledWidth, sliderY + sliderHeight, accentStart, accentEnd, 2f);
       }
       
       int handleRadius = 5;
       int handleX = sliderX + filledWidth;
       int handleY = sliderY + sliderHeight / 2;
       int handleColor = (new java.awt.Color(255, 255, 255, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawCircle(context, handleX, handleY, handleRadius, handleColor);
   }

   public static void renderColorPaletteValue(GuiGraphics context, Minecraft client, Module module,
                                              com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue, Theme theme,
                                              float x1, float y, float x2, float subOptionHeight,
                                              float mouseX, float mouseY, int localAlpha) {
       float paletteHeight = subOptionHeight * 2;
       
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + paletteHeight, 6, valueColor);
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       String displayText = paletteValue.getDisplayName();
       float textY = y + 6;
       context.drawString(client.font, displayText, (int)(x1 + 8), (int) textY, textColor, false);
       
       int colorCount = paletteValue.getColorCount();
       float colorAreaY = y + subOptionHeight; 
       float colorAreaHeight = subOptionHeight - 8; 
       
       float totalWidth = x2 - x1 - 16;
       float boxSize = Math.min(colorAreaHeight - 2, (totalWidth - (colorCount - 1) * 3) / colorCount);
       float spacing = (totalWidth - boxSize * colorCount) / (colorCount - 1);
       float startX = x1 + 8;
       float boxY = colorAreaY + (colorAreaHeight - boxSize) / 2;
       
       int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
       int themePurpleBorder = theme.BG_3.getRGB();
       
       for (int i = 0; i < colorCount; i++) {
           float boxX = startX + i * (boxSize + spacing);
           int color = paletteValue.getColor(i);
           boolean isSelected = paletteValue.isColorSelected(i);
           boolean isHovered = GuiRenderUtil.isHovered(boxX, boxY, boxX + boxSize, boxY + boxSize, mouseX, mouseY);
           
           int fillColor = (color & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawRoundedRect(context, boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, 2, fillColor);
           
           int borderColor;
           if (isSelected || isHovered) {
               borderColor = (themePurpleBorder & 0x00FFFFFF) | (localAlpha << 24);
           } else {
               borderColor = (themeDarkBorder & 0x00FFFFFF) | (localAlpha << 24);
           }
           GuiRenderUtil.drawRoundedRectOutline(context, boxX, boxY, boxX + boxSize, boxY + boxSize, 2, borderColor);
           
           if (isSelected) {
               GuiRenderUtil.drawRoundedRectOutline(context, boxX - 1, boxY - 1, boxX + boxSize + 1, boxY + boxSize + 1, 3, borderColor);
           }
       }
   }
   
   public static float getColorPaletteHeight(float subOptionHeight) {
       return subOptionHeight * 2;
   }

   public static float getGradientEditorHeight(float subOptionHeight) {
       return subOptionHeight * 6;
   }

   public static float getCrosshairPainterHeight(float subOptionHeight) {
       return subOptionHeight * 8;
   }

   public static void renderCrosshairPainterValue(GuiGraphics context, Minecraft client, CrosshairPainterValue value, Theme theme,
                                                  float x1, float y, float x2, float subOptionHeight,
                                                  float mouseX, float mouseY, int localAlpha,
                                                  ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       CrosshairPainterLayout l = computeCrosshairPainterLayout(client, value, x1, y, x2, subOptionHeight);

       int bg = (new java.awt.Color(30, 31, 36, 165).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int border = (new java.awt.Color(65, 68, 78, 220).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + l.blockHeight, 6, bg);
       GuiRenderUtil.drawRoundedRectOutline(context, x1, y, x2, y + l.blockHeight, 6, border);
       GuiRenderUtil.drawRoundedRectOutline(context, l.previewX1, l.previewY1, l.previewX2, l.previewY2, 0, border);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       int canvasBg = (new java.awt.Color(22, 23, 27, 210).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, l.canvasX1, l.canvasY1, l.canvasX2, l.canvasY2, 4, canvasBg);
       GuiRenderUtil.drawRoundedRectOutline(context, l.canvasX1, l.canvasY1, l.canvasX2, l.canvasY2, 4, border);

       int emptyA = localAlpha;
       int empty0 = (emptyA << 24) | 0x1F2228;
       int empty1 = (emptyA << 24) | 0x232730;
       int center = (Math.min(255, (int) (localAlpha * 0.85f)) << 24) | 0xB061FF;
       int staticColor = (localAlpha << 24) | 0xE6E6E6;
       int activeColor = (localAlpha << 24) | 0xBEBEBE;
       int bothColor = (localAlpha << 24) | 0x7FD4FF;

       int n = value.getSize();
       int cx = value.getCenterIndex();
       int cy = value.getCenterIndex();

       for (int py = 0; py < n; py++) {
           for (int px = 0; px < n; px++) {
               int rx1 = l.gridX1 + px * l.cellPx;
               int ry1 = l.gridY1 + py * l.cellPx;
               int rx2 = rx1 + l.cellPx;
               int ry2 = ry1 + l.cellPx;
               int fill;
               boolean s = value.isStaticSet(px, py);
               boolean a = value.isActiveSet(px, py);
               if (s && a) {
                   fill = bothColor;
               } else if (s) {
                   fill = staticColor;
               } else if (a) {
                   fill = activeColor;
               } else if (px == cx && py == cy) {
                   fill = center;
               } else {
                   fill = (((px + py) & 1) == 0) ? empty0 : empty1;
               }
               context.fill(rx1, ry1, rx2, ry2, fill);
           }
       }

       boolean activeSelected = value.getSelectedLayer() == CrosshairPainterValue.Layer.ACTIVE;
       boolean staticSelected = value.getSelectedLayer() == CrosshairPainterValue.Layer.STATIC;
       int btnBg = (new java.awt.Color(40, 40, 40, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int btnBgSel = (theme.BG_3.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, l.activeBtnX1, l.activeBtnY1, l.activeBtnX2, l.activeBtnY2, 3, activeSelected ? btnBgSel : btnBg);
       GuiRenderUtil.drawRoundedRectOutline(context, l.activeBtnX1, l.activeBtnY1, l.activeBtnX2, l.activeBtnY2, 3, border);
       GuiRenderUtil.drawRoundedRect(context, l.staticBtnX1, l.staticBtnY1, l.staticBtnX2, l.staticBtnY2, 3, staticSelected ? btnBgSel : btnBg);
       GuiRenderUtil.drawRoundedRectOutline(context, l.staticBtnX1, l.staticBtnY1, l.staticBtnX2, l.staticBtnY2, 3, border);

       int labelColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, "AL", (int) (l.activeBtnX1 + (l.activeBtnX2 - l.activeBtnX1 - client.font.width("AL")) * 0.5f), (int) (l.activeBtnY1 + 4), labelColor, false);
       context.drawString(client.font, "SL", (int) (l.staticBtnX1 + (l.staticBtnX2 - l.staticBtnX1 - client.font.width("SL")) * 0.5f), (int) (l.staticBtnY1 + 4), labelColor, false);

       if (hoveredTooltipInfo != null) {
           if (GuiRenderUtil.isHovered(l.activeBtnX1, l.activeBtnY1, l.activeBtnX2, l.activeBtnY2, mouseX, mouseY)) {
               hoveredTooltipInfo.tooltip = "The crosshair in active layer will be given fancy animations.";
               hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(hoveredTooltipInfo.tooltip);
               hoveredTooltipInfo.x = (int) (mouseX + 5);
               hoveredTooltipInfo.y = (int) (mouseY + 5);
           } else if (GuiRenderUtil.isHovered(l.staticBtnX1, l.staticBtnY1, l.staticBtnX2, l.staticBtnY2, mouseX, mouseY)) {
               hoveredTooltipInfo.tooltip = "The crosshair in static layer is just a normal custom one.";
               hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(hoveredTooltipInfo.tooltip);
               hoveredTooltipInfo.x = (int) (mouseX + 5);
               hoveredTooltipInfo.y = (int) (mouseY + 5);
           }
       }

       int resetBg = ((theme.BG_2.getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.draw3DRect(context, l.resetX1, l.resetY1, l.resetX2, l.resetY2, resetBg, 0f);
       int resetTextColor = value.isResetArmed()
           ? ((com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR & 0x00FFFFFF) | (localAlpha << 24))
           : textColor;
       context.drawString(client.font, l.resetText, (int) (l.resetX1 + (l.resetX2 - l.resetX1 - client.font.width(l.resetText)) * 0.5f), (int) (l.resetY1 + 4), resetTextColor, false);

       drawPreviewCrosshair(context, l.previewX1, l.previewY1, l.previewX2 - l.previewX1, l.previewY2 - l.previewY1, value, localAlpha);
   }

   private static void drawPreviewCrosshair(GuiGraphics g, float x, float y, float w, float h, CrosshairPainterValue value, int localAlpha) {
       int n = value.getSize();
       int center = n / 2;
       int pxSize = 1;
       int cx = (int) (x + w * 0.5f);
       int cy = (int) (y + h * 0.5f);

       int staticColor = (localAlpha << 24) | 0xE6E6E6;
       int activeColor = (localAlpha << 24) | 0xBEBEBE;
       boolean chroma = com.shyeuar.baity.config.ConfigManager.crosshairChromaEnabled;
       long nowMs = System.currentTimeMillis();
       float triggerPeriodTicks = 10f;
       float fallTicks = 3.5f;
       float maxExtra = 20f;
       float previewTick = (nowMs % 100000L) / 50f;
       float sinceTrigger = previewTick % triggerPeriodTicks;
       float previewExtra = sinceTrigger >= fallTicks ? 0f : (maxExtra * (1f - sinceTrigger / fallTicks));

       for (int py = 0; py < n; py++) {
           for (int px = 0; px < n; px++) {
               boolean s = value.isStaticSet(px, py);
               if (!s) continue;
               int dx = px - center;
               int dy = py - center;
               int rx = cx + dx * pxSize;
               int ry = cy + dy * pxSize;
               int color = chroma ? (0xFF000000 | chromaColor(nowMs, py * n + px, n * n)) : staticColor;
               g.fill(rx, ry, rx + pxSize, ry + pxSize, color);
           }
       }

       for (int py = 0; py < n; py++) {
           for (int px = 0; px < n; px++) {
               boolean a = value.isActiveSet(px, py);
               if (!a) continue;
               int dx = px - center;
               int dy = py - center;
               int drawDx = dx;
               int drawDy = dy;

               if (a) {
                   double len = Math.sqrt((double) dx * dx + (double) dy * dy);
                   if (len > 0.0) {
                       drawDx = (int) Math.round(dx + (dx / len) * previewExtra);
                       drawDy = (int) Math.round(dy + (dy / len) * previewExtra);
                   }
               }

               int rx = cx + drawDx * pxSize;
               int ry = cy + drawDy * pxSize;
               int color = chroma ? (0xFF000000 | chromaColor(nowMs, py * n + px, n * n)) : activeColor;
               g.fill(rx, ry, rx + pxSize, ry + pxSize, color);
           }
       }
   }

   private static int chromaColor(long nowMs, int idx, int len) {
       double lightness = Math.max(0.2, Math.min(1.0, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaLightness));
       double chroma = Math.max(0.0, Math.min(0.4, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaChroma));
       double size = Math.max(0.1, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaSize);
       double speed = Math.max(0.0, Math.min(8.0, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaSpeed));
       double phase = (nowMs / 1000.0) * (speed * 0.5);
       double progress = len <= 1 ? 0.0 : (double) idx / (double) (len - 1);
       float saturation = (float) (chroma / 0.4);
       float hue = (float) (((progress / size) - phase) % 1.0);
       if (hue < 0f) hue += 1f;
       return net.minecraft.util.Mth.hsvToRgb(hue, saturation, (float) lightness);
   }

   public static void renderGradientEditorValue(GuiGraphics context, Minecraft client, GradientEditorValue value, Theme theme,
                                                float x1, float y, float x2, float subOptionHeight, float mouseX, float mouseY, int localAlpha,
                                                com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                                String gradientInputText,
                                                ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       float blockHeight = getGradientEditorHeight(subOptionHeight);
       int bg = (new java.awt.Color(35, 35, 35, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + blockHeight, 6, bg);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       float mapX1 = x1 + 8;
       float mapY1 = y + 22;
       float mapX2 = x2 - 60;
       float mapY2 = y + blockHeight - 40;
       drawHueSatMap(context, mapX1, mapY1, mapX2, mapY2, localAlpha);

      float p1x = mapX1 + value.getSelectedHue() * (mapX2 - mapX1);
      float p1y = mapY1 + (1f - value.getSelectedSat()) * (mapY2 - mapY1);
      int pointFill = java.awt.Color.HSBtoRGB(value.getSelectedHue(), value.getSelectedSat(), GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
      int selectorColor = (pointFill & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRect(context, p1x - 2, p1y - 2, p1x + 3, p1y + 3, 0, selectorColor);
      float mapMidY = (mapY1 + mapY2) * 0.5f;
      int pointBorderBase = p1y >= mapMidY ? 0x000000 : 0xFFFFFF;
      int pointBorder = (pointBorderBase & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRectOutline(context, p1x - 3, p1y - 3, p1x + 4, p1y + 4, 0, pointBorder);

       float sliderX1 = x2 - 48;
       float sliderX2 = x2 - 36;
       drawValueSlider(context, sliderX1, mapY1, sliderX2, mapY2, value.getSelectedHue(), value.getSelectedSat(), localAlpha);
       float knobY = mapY2 - value.getSelectedVal() * (mapY2 - mapY1);
      int knobColor = (new java.awt.Color(0, 0, 0, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      int knobBorder = (new java.awt.Color(255, 255, 255, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRect(context, sliderX1 - 2, knobY - 2, sliderX2 + 2, knobY + 2, 1, knobColor);
      GuiRenderUtil.drawRoundedRectOutline(context, sliderX1 - 3, knobY - 3, sliderX2 + 3, knobY + 3, 1, knobBorder);

       float box1X1 = x2 - 30;
       float box1X2 = x2 - 12;
       float box1Y1 = y + 24;
       float box1Y2 = y + 42;
      float box2Y2 = mapY2;
      float box2Y1 = box2Y2 - 18;
      int color1 = (0xFF000000 | value.getStartColor() & 0xFFFFFF);
      int color2 = (0xFF000000 | value.getEndColor() & 0xFFFFFF);
      GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box1Y1 + 1, box1X2 - 1, box1Y2 - 1, 2, color1);
      GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box2Y1 + 1, box1X2 - 1, box2Y2 - 1, 2, color2);
      int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
      int themePurpleBorder = theme.BG_3.getRGB();
      boolean hoverL = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(box1X1, box1Y1, box1X2, box1Y2, mouseX, mouseY);
      boolean hoverR = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(box1X1, box2Y1, box1X2, box2Y2, mouseX, mouseY);
      int borderL = (value.getSelectedPoint() == 0 || hoverL ? themePurpleBorder : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
      int borderR = (value.getSelectedPoint() == 1 || hoverR ? themePurpleBorder : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box1Y1, box1X2, box1Y2, 2, borderL);
      GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box2Y1, box1X2, box2Y2, 2, borderR);
      if (value.getSelectedPoint() == 0) {
          GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box1Y1 - 1, box1X2 + 1, box1Y2 + 1, 3, borderL);
      } else {
          GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box2Y1 - 1, box1X2 + 1, box2Y2 + 1, 3, borderR);
      }
      drawScaledLabel(context, client, "L", box1X1 + 6, box1Y2 + 2, textColor, 0.85f);
      drawScaledLabel(context, client, "R", box1X1 + 6, box2Y1 - 8, textColor, 0.85f);

       String selectedHex = value.getSelectedHex();
       if (editingGradient != null && "gradient editor".equals(editingGradient.valueName)) {
           selectedHex = "#" + gradientInputText.toUpperCase(java.util.Locale.ROOT);
       }
      int inputWidth = client.font.width("#FFFFFF");
       float inputX2 = mapX2;
       float inputX1 = inputX2 - inputWidth;
      float syncX2 = x2 - 8;
      float syncY2 = y + blockHeight - 8;
      float syncX1 = syncX2 - 48;
      float syncY1 = syncY2 - 14;
      float inputY = syncY2 - 3;
      boolean inputHovered = GuiRenderUtil.isHovered(inputX1, inputY - 12, inputX2, inputY + 6, mouseX, mouseY);
      int hoverYellow = (new java.awt.Color(255, 255, 0, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      int inputTextColor = (editingGradient != null) ? hoverYellow : (inputHovered ? hoverYellow : textColor);
       context.drawString(client.font, selectedHex, (int) inputX1, (int) (inputY - 9), inputTextColor, false);
      int lineColor = inputHovered || editingGradient != null
          ? hoverYellow
          : ((new java.awt.Color(120, 120, 120, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
      GuiRenderUtil.drawRoundedRect(context, inputX1, inputY, inputX2, inputY + 1, 0, lineColor);
      boolean syncHovered = GuiRenderUtil.isHovered(syncX1, syncY1, syncX2, syncY2, mouseX, mouseY);
      int syncBg = syncHovered
          ? ((new java.awt.Color(60, 60, 60, 80).getRGB() & 0x00FFFFFF) | (localAlpha << 24))
          : ((new java.awt.Color(40, 40, 40, 50).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
      GuiRenderUtil.draw3DRect(context, syncX1, syncY1, syncX2, syncY2, syncBg, 0f);
       context.drawString(client.font, "Sync", (int) (syncX1 + 12), (int) (syncY1 + 3), textColor, false);
      if (syncHovered && hoveredTooltipInfo != null) {
          hoveredTooltipInfo.tooltip = "Set the selected color to the unselected color.";
          hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal("Set the selected color to the unselected color.");
          hoveredTooltipInfo.x = (int) (mouseX + 5);
          hoveredTooltipInfo.y = (int) (mouseY + 5);
      }

      String raw = com.shyeuar.baity.config.ConfigManager.nickTweaksNickChanger;
      if (raw == null || raw.isBlank()) {
          raw = (client.player != null ? client.player.getName().getString() : "NickTweaks");
      }
      String baseName = (client.player != null ? client.player.getName().getString() : "NickTweaks");
      NickRenderUtils.invalidateLocalTargetsCache();
      NickRenderUtils.beginPreviewOverride();
      net.minecraft.util.FormattedCharSequence seq;
      try {
          net.minecraft.util.FormattedCharSequence base = net.minecraft.util.FormattedCharSequence.forward(baseName, net.minecraft.network.chat.Style.EMPTY);
          seq = NickRenderUtils.handleCharSequence(base);
      } finally {
          NickRenderUtils.endPreviewOverride();
      }
      context.drawString(client.font, seq, (int) (x1 + 10), (int) (y + blockHeight - 18), 0xFFFFFFFF, false);
   }

   private static void drawHueSatMap(GuiGraphics context, float x1, float y1, float x2, float y2, int alpha) {
       int width = Math.max(1, (int) (x2 - x1));
       int height = Math.max(1, (int) (y2 - y1));
       for (int px = 0; px < width; px += 8) {
           float hue = px / (float) width;
           for (int py = 0; py < height; py += 8) {
               float sat = 1f - py / (float) height;
               int rgb = java.awt.Color.HSBtoRGB(hue, sat, GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
               int color = (alpha << 24) | rgb;
               context.fill((int) x1 + px, (int) y1 + py, (int) x1 + px + 8, (int) y1 + py + 8, color);
           }
       }
   }



   private static void drawValueSlider(GuiGraphics context, float x1, float y1, float x2, float y2, float hue, float sat, int alpha) {
       int height = Math.max(1, (int) (y2 - y1));
       for (int py = 0; py < height; py += 4) {
           float val = 1f - py / (float) height;
           int rgb = java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
           int color = (alpha << 24) | rgb;
           context.fill((int) x1, (int) y1 + py, (int) x2, (int) y1 + py + 4, color);
       }
   }


   private static void drawScaledLabel(GuiGraphics context, Minecraft client, String text, float x, float y, int color, float scale) {
      var pose = context.pose();
      pose.pushMatrix();
      pose.translate(x, y);
      pose.scale(scale, scale);
      context.drawString(client.font, text, 0, 0, color, false);
      pose.popMatrix();
   }
   
   public static int getHoveredColorIndex(com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue,
                                          float x1, float y, float x2, float subOptionHeight,
                                          float mouseX, float mouseY) {
       int colorCount = paletteValue.getColorCount();
       float colorAreaY = y + subOptionHeight;
       float colorAreaHeight = subOptionHeight - 8;
       
       float totalWidth = x2 - x1 - 16;
       float boxSize = Math.min(colorAreaHeight - 2, (totalWidth - (colorCount - 1) * 3) / colorCount);
       float spacing = (totalWidth - boxSize * colorCount) / (colorCount - 1);
       float startX = x1 + 8;
       float boxY = colorAreaY + (colorAreaHeight - boxSize) / 2;
       
       for (int i = 0; i < colorCount; i++) {
           float boxX = startX + i * (boxSize + spacing);
           if (GuiRenderUtil.isHovered(boxX, boxY, boxX + boxSize, boxY + boxSize, mouseX, mouseY)) {
               return i;
           }
       }
       return -1;
   }
}
