package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.input.LineTextInput;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.CrosshairPainterValue;
import com.shyeuar.baity.gui.value.EnchantLoreColorEditorValue;
import com.shyeuar.baity.gui.value.ChromaFishingLineColorEditorValue;
import com.shyeuar.baity.gui.value.GradientEditorValue;
import com.shyeuar.baity.features.enchantlore.EnchantLore;
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

   public static class GradientEditorBottomLayout {
       public final float mapX1, mapY1, mapX2, mapY2;
       public final float syncX1, syncY1, syncX2, syncY2;
       public final float resetX1, resetY1, resetX2, resetY2;
       public final float inputX1, inputY, inputX2;
       public final float symbolInputX1, symbolInputY, symbolInputX2;
       public final boolean hasReset;
       public final boolean hasSymbolInput;

       public GradientEditorBottomLayout(float mapX1, float mapY1, float mapX2, float mapY2,
                                         float syncX1, float syncY1, float syncX2, float syncY2,
                                         float resetX1, float resetY1, float resetX2, float resetY2,
                                         float inputX1, float inputY, float inputX2,
                                         float symbolInputX1, float symbolInputY, float symbolInputX2,
                                         boolean hasReset, boolean hasSymbolInput) {
           this.mapX1 = mapX1;
           this.mapY1 = mapY1;
           this.mapX2 = mapX2;
           this.mapY2 = mapY2;
           this.syncX1 = syncX1;
           this.syncY1 = syncY1;
           this.syncX2 = syncX2;
           this.syncY2 = syncY2;
           this.resetX1 = resetX1;
           this.resetY1 = resetY1;
           this.resetX2 = resetX2;
           this.resetY2 = resetY2;
           this.inputX1 = inputX1;
           this.inputY = inputY;
           this.inputX2 = inputX2;
           this.symbolInputX1 = symbolInputX1;
           this.symbolInputY = symbolInputY;
           this.symbolInputX2 = symbolInputX2;
           this.hasReset = hasReset;
           this.hasSymbolInput = hasSymbolInput;
       }
   }

   public static GradientEditorBottomLayout computeGradientEditorBottomLayout(
           Minecraft client, float x1, float y, float x2, float blockHeight,
           String hexDisplay, boolean withReset) {
       return computeGradientEditorBottomLayout(client, x1, y, x2, blockHeight, hexDisplay, withReset, null);
   }

   public static final float FANCY_DMG_EDITOR_TOGGLE_SIZE = 18f;
   public static final float FANCY_DMG_EDITOR_TOGGLE_GAP = 6f;

   public static final class FancyDmgEditorBottomRowLayout {
       public final float previewX1, previewY1, previewX2, previewY2;
       public final float compactX1, compactY1, compactX2, compactY2;
       public final float boldX1, boldY1, boldX2, boldY2;
       public final float deleteX1, deleteY1, deleteX2, deleteY2;

       FancyDmgEditorBottomRowLayout(float previewX1, float previewY1, float previewX2, float previewY2,
                                     float compactX1, float compactY1, float compactX2, float compactY2,
                                     float boldX1, float boldY1, float boldX2, float boldY2,
                                     float deleteX1, float deleteY1, float deleteX2, float deleteY2) {
           this.previewX1 = previewX1;
           this.previewY1 = previewY1;
           this.previewX2 = previewX2;
           this.previewY2 = previewY2;
           this.compactX1 = compactX1;
           this.compactY1 = compactY1;
           this.compactX2 = compactX2;
           this.compactY2 = compactY2;
           this.boldX1 = boldX1;
           this.boldY1 = boldY1;
           this.boldX2 = boldX2;
           this.boldY2 = boldY2;
           this.deleteX1 = deleteX1;
           this.deleteY1 = deleteY1;
           this.deleteX2 = deleteX2;
           this.deleteY2 = deleteY2;
       }
   }

   public static FancyDmgEditorBottomRowLayout layoutFancyDmgEditorBottomRow(Minecraft client, float x1, float y,
                                                                             float blockHeight, float symbolInputX1) {
       float toggleSize = FANCY_DMG_EDITOR_TOGGLE_SIZE;
       float gap = FANCY_DMG_EDITOR_TOGGLE_GAP;
       float previewY1 = y + blockHeight - 22f;
       float previewY2 = previewY1 + com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.previewButtonHeight(client.font);
       float toggleY1 = previewY1 - 1f;
       float toggleY2 = toggleY1 + toggleSize;

       float deleteX2 = symbolInputX1 - gap;
       float deleteX1 = deleteX2 - toggleSize;
       float boldX2 = deleteX1 - gap;
       float boldX1 = boldX2 - toggleSize;
       float compactX2 = boldX1 - gap;
       float compactX1 = compactX2 - toggleSize;
       float previewX1 = x1 + 10f;
       float previewX2 = compactX1 - gap;

       return new FancyDmgEditorBottomRowLayout(
               previewX1, previewY1, previewX2, previewY2,
               compactX1, toggleY1, compactX2, toggleY2,
               boldX1, toggleY1, boldX2, toggleY2,
               deleteX1, toggleY1, deleteX2, toggleY2);
   }

   private static void drawCenteredClippedText(GuiGraphics context, net.minecraft.client.gui.Font font,
                                               String text, float lineX1, float lineY, float lineX2, int color) {
       LineTextInput.drawCenteredClippedWithBlinkCursor(
           context, font, text, 0, lineX1, lineY, lineX2, color, false);
   }

   public static GradientEditorBottomLayout computeGradientEditorBottomLayout(
           Minecraft client, float x1, float y, float x2, float blockHeight,
           String hexDisplay, boolean withReset, String symbolDisplay) {
       return computeGradientEditorBottomLayout(client, x1, y, x2, blockHeight, hexDisplay, withReset, symbolDisplay, false);
   }

   public static GradientEditorBottomLayout computeGradientEditorBottomLayout(
           Minecraft client, float x1, float y, float x2, float blockHeight,
           String hexDisplay, boolean withReset, String symbolDisplay, boolean editingSymbol) {
       float syncX2 = x2 - 8f;
       float syncY2 = y + blockHeight - 8f;
       float syncX1 = syncX2 - 48f;
       float syncY1 = syncY2 - 14f;
       float rowRight = syncX1 - 8f;
       float resetX1 = 0f;
       float resetY1 = syncY1;
       float resetX2 = 0f;
       float resetY2 = syncY2;
       if (withReset) {
           float resetW = 40f;
           resetX2 = rowRight;
           resetX1 = resetX2 - resetW;
           rowRight = resetX1 - 8f;
       }
       int hexW = client.font.width(hexDisplay);
       float inputX2 = rowRight;
       float inputX1 = inputX2 - hexW;
       float inputY = syncY2 - 3f;
       float symbolInputX1 = 0f;
       float symbolInputX2 = 0f;
       float symbolInputY = inputY;
       boolean hasSymbolInput = symbolDisplay != null;
       if (hasSymbolInput) {
           float symbolLineWidth = com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.symbolInputWidth(client.font);
           symbolInputX2 = inputX1 - 8f;
           symbolInputX1 = symbolInputX2 - symbolLineWidth;
           symbolInputY = inputY;
       }
       float mapX1 = x1 + 8f;
       float mapY1 = y + 22f;
       float mapX2 = Math.max(mapX1 + 40f, (x2 - 48f) - 8f);
       float mapY2 = y + blockHeight - 40f;
       return new GradientEditorBottomLayout(
               mapX1, mapY1, mapX2, mapY2,
               syncX1, syncY1, syncX2, syncY2,
               resetX1, resetY1, resetX2, resetY2,
               inputX1, inputY, inputX2,
               symbolInputX1, symbolInputY, symbolInputX2,
               withReset, hasSymbolInput);
   }

   private static void drawCenteredButtonLabel(GuiGraphics context, net.minecraft.client.gui.Font font,
                                               String text, float x1, float y1, float x2, float y2, int color) {
       int w = font.width(text);
       int h = font.lineHeight;
       int x = (int) (x1 + (x2 - x1 - w) * 0.5f);
       int y = (int) (y1 + (y2 - y1 - h) * 0.5f);
       context.drawString(font, text, x, y, color, false);
   }

   private static void drawThickLine(GuiGraphics context, float x0, float y0, float x1, float y1, int thickness, int color) {
       int steps = Math.max(1, (int) (Math.hypot(x1 - x0, y1 - y0) * 2f));
       for (int i = 0; i <= steps; i++) {
           float t = i / (float) steps;
           float px = x0 + (x1 - x0) * t;
           float py = y0 + (y1 - y0) * t;
           int half = thickness / 2;
           context.fill((int) px - half, (int) py - half, (int) px + half + 1, (int) py + half + 1, color);
       }
   }

   private static void drawToggleCheckmark(GuiGraphics context, float x1, float y1, float x2, float y2, int color) {
       float xL = x1 + 4f;
       float yL = y1 + (y2 - y1) * 0.58f;
       float xM = x1 + (x2 - x1) * 0.36f;
       float yM = y2 - 4f;
       float xR = x2 - 4f;
       float yR = y1 + 4f;
       drawThickLine(context, xL, yL, xM, yM, 2, color);
       drawThickLine(context, xM, yM, xR, yR, 2, color);
   }

   private static void renderGradientEditorBottomControls(GuiGraphics context, Minecraft client, Theme theme,
                                                          GradientEditorBottomLayout bottom, String selectedHex,
                                                          boolean editingHex, int localAlpha,
                                                          ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo,
                                                          float mouseX, float mouseY,
                                                          String syncTooltip) {
       renderGradientEditorBottomControls(context, client, theme, bottom, selectedHex, editingHex, null,
               null, false, null, localAlpha, hoveredTooltipInfo, mouseX, mouseY, syncTooltip);
   }

   private static void renderGradientEditorBottomControls(GuiGraphics context, Minecraft client, Theme theme,
                                                          GradientEditorBottomLayout bottom, String selectedHex,
                                                          boolean editingHex, Integer editingHexCaretCp,
                                                          String symbolDisplay,
                                                          boolean editingSymbol, Integer editingSymbolCaretCp,
                                                          int localAlpha,
                                                          ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo,
                                                          float mouseX, float mouseY,
                                                          String syncTooltip) {
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int hoverYellow = (new java.awt.Color(255, 255, 0, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       if (bottom.hasSymbolInput && symbolDisplay != null) {
           boolean symbolHovered = GuiRenderUtil.isHovered(bottom.symbolInputX1, bottom.symbolInputY - 12, bottom.symbolInputX2, bottom.symbolInputY + 6, mouseX, mouseY);
           int symbolTextColor = editingSymbol ? hoverYellow : (symbolHovered ? hoverYellow : textColor);
           LineTextInput.drawCenteredClippedWithBlinkCursor(
               context, client.font, symbolDisplay, editingSymbolCaretCp,
               bottom.symbolInputX1, bottom.symbolInputY, bottom.symbolInputX2, symbolTextColor, editingSymbol);
           int symbolLineColor = symbolHovered || editingSymbol
                   ? hoverYellow
                   : ((new java.awt.Color(120, 120, 120, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
           GuiRenderUtil.drawRoundedRect(context, bottom.symbolInputX1, bottom.symbolInputY, bottom.symbolInputX2, bottom.symbolInputY + 1, 0, symbolLineColor);
       }
       boolean inputHovered = GuiRenderUtil.isHovered(bottom.inputX1, bottom.inputY - 12, bottom.inputX2, bottom.inputY + 6, mouseX, mouseY);
       int inputTextColor = editingHex ? hoverYellow : (inputHovered ? hoverYellow : textColor);
       int hexTextY = (int) (bottom.inputY - 9);
       if (editingHex) {
           LineTextInput.drawTextWithBlinkCursor(
               context, client.font, selectedHex, editingHexCaretCp,
               (int) bottom.inputX1, hexTextY, inputTextColor, true,
               LineTextInput.shouldBlinkCursor());
       } else {
           context.drawString(client.font, selectedHex, (int) bottom.inputX1, hexTextY, inputTextColor, false);
       }
       int lineColor = inputHovered || editingHex
               ? hoverYellow
               : ((new java.awt.Color(120, 120, 120, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.drawRoundedRect(context, bottom.inputX1, bottom.inputY, bottom.inputX2, bottom.inputY + 1, 0, lineColor);
       if (bottom.hasReset) {
           boolean resetHovered = GuiRenderUtil.isHovered(bottom.resetX1, bottom.resetY1, bottom.resetX2, bottom.resetY2, mouseX, mouseY);
           int resetBg = resetHovered
                   ? ((new java.awt.Color(60, 60, 60, 80).getRGB() & 0x00FFFFFF) | (localAlpha << 24))
                   : ((new java.awt.Color(40, 40, 40, 50).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
           GuiRenderUtil.draw3DRect(context, bottom.resetX1, bottom.resetY1, bottom.resetX2, bottom.resetY2, resetBg, 0f);
           drawCenteredButtonLabel(context, client.font, "Reset", bottom.resetX1, bottom.resetY1, bottom.resetX2, bottom.resetY2, textColor);
       }
       boolean syncHovered = GuiRenderUtil.isHovered(bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, mouseX, mouseY);
       int syncBg = syncHovered
               ? ((new java.awt.Color(60, 60, 60, 80).getRGB() & 0x00FFFFFF) | (localAlpha << 24))
               : ((new java.awt.Color(40, 40, 40, 50).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.draw3DRect(context, bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, syncBg, 0f);
       drawCenteredButtonLabel(context, client.font, "Sync", bottom.syncX1, bottom.syncY1, bottom.syncX2, bottom.syncY2, textColor);
       if (syncHovered && hoveredTooltipInfo != null && syncTooltip != null) {
           hoveredTooltipInfo.tooltip = syncTooltip;
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(syncTooltip);
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }
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
                                 Integer gradientInputCaretCp,
                                 Integer sliderInputCaretCp,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.TextInputInfo editingTextInput,
                                 String textInputValue,
                                 Integer editingTextCaretCp) {

      ValueStyle style = value.getStyle();
       
      if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
           renderButtonLikeValue(context, client, module, (ButtonValue) value, theme,
                              x1, y, x2, subOptionHeight,
                              mouseX, mouseY, localAlpha,
                              getDisplayTextFormatter, listeningButtonValueName);
      } else if (style == ValueStyle.GROUP && value instanceof GroupValue) {
          renderGroupValue(context, client, (GroupValue) value, theme, x1, y, x2, subOptionHeight,
                  mouseX, mouseY, localAlpha, getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
       } else if (style == ValueStyle.SLIDER && value instanceof SliderValue) {
           renderSliderValue(context, client, module, (SliderValue) value, theme,
                            x1, y, x2, subOptionHeight,
                            mouseX, mouseY, localAlpha, editingSlider, sliderInputText, sliderInputCaretCp);
       } else if (style == ValueStyle.FANCY_DMG_PRESET && value instanceof com.shyeuar.baity.gui.value.FancyDmgSplashPresetValue presetPalette) {
           renderFancyDmgPresetValue(context, client, presetPalette, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, hoveredTooltipInfo);
       } else if (style == ValueStyle.COLOR_PALETTE && value instanceof com.shyeuar.baity.gui.value.ColorPaletteValue) {
           renderColorPaletteValue(context, client, module, (com.shyeuar.baity.gui.value.ColorPaletteValue) value, theme,
                                   x1, y, x2, subOptionHeight,
                                   mouseX, mouseY, localAlpha);
      } else if (style == ValueStyle.GRADIENT_EDITOR && value instanceof GradientEditorValue) {
          renderGradientEditorValue(context, client, (GradientEditorValue) value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingGradient, gradientInputText, gradientInputCaretCp, hoveredTooltipInfo);
      } else if (style == ValueStyle.FANCY_DMG_COLOR_EDITOR && value instanceof com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue fancyEditor) {
          renderFancyDmgColorEditorValue(context, client, fancyEditor, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingGradient, gradientInputText, gradientInputCaretCp, hoveredTooltipInfo);
      } else if (style == ValueStyle.ENCHANT_LORE_COLOR_EDITOR && value instanceof EnchantLoreColorEditorValue) {
          renderEnchantLoreColorEditorValue(context, client, (EnchantLoreColorEditorValue) value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingGradient, gradientInputText, gradientInputCaretCp, hoveredTooltipInfo);
      } else if (style == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR && value instanceof ChromaFishingLineColorEditorValue chromaEditor) {
          renderChromaFishingLineColorEditorValue(context, client, chromaEditor, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingGradient, gradientInputText, gradientInputCaretCp, hoveredTooltipInfo);
      } else if (style == ValueStyle.CROSSHAIR_PAINTER && value instanceof CrosshairPainterValue) {
          renderCrosshairPainterValue(context, client, (CrosshairPainterValue) value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, hoveredTooltipInfo);
      } else if (style == ValueStyle.TEXT_LINE_INPUT && value instanceof TextLineInputValue) {
          renderTextLineInputValue(context, client, module, (TextLineInputValue) value, theme,
              x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingTextInput, textInputValue,
              editingTextCaretCp,
              getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
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
                                               Integer editingTextCaretCp,
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
       preview = LineTextInput.limitByCodePoints(preview, 28);
       int valueTextColor = (hovered || editing) ? interactiveYellow : textColor;
       int textY = (int) (lineY - 9);
       if (editing) {
           LineTextInput.drawTextWithBlinkCursor(
               context, client.font, preview, editingTextCaretCp,
               (int) lineX1, textY, valueTextColor, true,
               LineTextInput.shouldBlinkCursor());
       } else {
           context.drawString(client.font, preview, (int) lineX1, textY, valueTextColor, false);
       }
   }

   public static void renderGroupValue(GuiGraphics context, Minecraft client, GroupValue groupValue, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       float mouseX, float mouseY, int localAlpha,
                                       java.util.function.Function<String, String> getTooltipText,
                                       java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                       ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
      boolean subHovered = GuiRenderUtil.isHovered(x1, y, x2, y + subOptionHeight, mouseX, mouseY);
      int baseValueColor = subHovered ? new java.awt.Color(60, 60, 60, 80).getRGB()
              : new java.awt.Color(40, 40, 40, 50).getRGB();
      int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);

      if (subHovered && hoveredTooltipInfo != null) {
          String tooltip = getTooltipText.apply(groupValue.getName());
          if (tooltip != null) {
              hoveredTooltipInfo.tooltip = tooltip;
              hoveredTooltipInfo.tooltipText = getTooltipTextWithColors.apply(groupValue.getName());
              hoveredTooltipInfo.x = (int) (mouseX + 5);
              hoveredTooltipInfo.y = (int) (mouseY + 5);
          }
      }

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
       
       if (subHovered && hoveredTooltipInfo != null) {
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
       renderSliderValue(context, client, module, sliderValue, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, null, "", 0);
   }
   
   public static void renderSliderValue(GuiGraphics context, Minecraft client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha,
                                        com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider,
                                        String inputText,
                                        Integer inputCaretCp) {
       
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
       
       String valueText = isEditing ? inputText : sliderValue.getFormattedValue();
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
       if (isEditing) {
           LineTextInput.drawTextWithBlinkCursor(
               context, client.font, valueText, inputCaretCp,
               textX, valueDisplayY, valueTextColor, true,
               LineTextInput.shouldBlinkCursor());
       } else {
           context.drawString(client.font, valueText, textX, valueDisplayY, valueTextColor, false);
       }
       
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
   
   public static final class FancyDmgPresetGridLayout {
       public final float boxSize;
       public final float spacing;
       public final float startX;
       public final float firstRowY;
       public final float rowStride;
       public final float editFramePad;

       FancyDmgPresetGridLayout(float boxSize, float spacing, float startX, float firstRowY, float rowStride, float editFramePad) {
           this.boxSize = boxSize;
           this.spacing = spacing;
           this.startX = startX;
           this.firstRowY = firstRowY;
           this.rowStride = rowStride;
           this.editFramePad = editFramePad;
       }

       public float boxX(int column) {
           return startX + column * (boxSize + spacing);
       }

       public float boxY(int row) {
           return firstRowY + row * rowStride + (rowStride - boxSize) * 0.5f;
       }
   }

   public static FancyDmgPresetGridLayout layoutFancyDmgPresetGrid(Minecraft client, float x1, float y, float x2,
                                                                  float subOptionHeight) {
       float totalWidth = x2 - x1 - 16f;
       float boxSize = com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.presetSwatchSize(client.font);
       float spacing = (totalWidth - 8f * boxSize) / 7f;
       if (spacing < 3f) {
           spacing = 3f;
           boxSize = (totalWidth - 7f * spacing) / 8f;
       }
       float editFramePad = com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.PRESET_EDIT_FRAME_PAD;
       float rowStride = com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.presetRowStride(subOptionHeight, client.font);
       return new FancyDmgPresetGridLayout(boxSize, spacing, x1 + 8f, y + subOptionHeight, rowStride, editFramePad);
   }

   public static void renderFancyDmgPresetValue(GuiGraphics context, Minecraft client,
                                               com.shyeuar.baity.gui.value.FancyDmgSplashPresetValue paletteValue,
                                               Theme theme, float x1, float y, float x2, float subOptionHeight,
                                               float mouseX, float mouseY, int localAlpha,
                                               ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       FancyDmgPresetGridLayout grid = layoutFancyDmgPresetGrid(client, x1, y, x2, subOptionHeight);
       int customRows = paletteValue.getCustomRowCount();
       float bottom = grid.boxY(customRows) + grid.boxSize + grid.editFramePad + 4f;
       float paletteHeight = bottom - y;
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + paletteHeight, 6, valueColor);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, paletteValue.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);
       String presetHint = "(Lclick to select,Rclick to edit)";
       int hintColor = (new java.awt.Color(160, 160, 160, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int hintW = client.font.width(presetHint);
       context.drawString(client.font, presetHint, (int) (x2 - 8 - hintW), (int) (y + 6), hintColor, false);

       int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
       int themePurpleBorder = theme.BG_3.getRGB();
       float boxY = grid.boxY(0);

       for (int i = 0; i < paletteValue.getBuiltinCount(); i++) {
           float boxX = grid.boxX(i);
           int color = paletteValue.getBuiltinColor(i);
           boolean hovered = GuiRenderUtil.isHovered(boxX, boxY, boxX + grid.boxSize, boxY + grid.boxSize, mouseX, mouseY);
           drawPresetSwatch(context, client, boxX, boxY, grid.boxSize, color, color, paletteValue.isBuiltinSelected(i),
                   paletteValue.isEditingBuiltin(i), hovered, grid.editFramePad, themeDarkBorder, themePurpleBorder, localAlpha);
           if (hovered && hoveredTooltipInfo != null) {
               String tooltip = paletteValue.getBuiltinTooltip(i);
               if (tooltip != null) {
                   hoveredTooltipInfo.tooltip = tooltip;
                   hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(tooltip);
                   hoveredTooltipInfo.x = (int) (mouseX + 5);
                   hoveredTooltipInfo.y = (int) (mouseY + 5);
               }
           }
       }

       int addIndex = paletteValue.getCustomCount();
       for (int row = 0; row < customRows; row++) {
           float rowBoxY = grid.boxY(row + 1);
           for (int col = 0; col < 8; col++) {
               int slot = row * 8 + col;
               if (slot > addIndex) {
                   break;
               }
               float boxX = grid.boxX(col);
               boolean hovered = GuiRenderUtil.isHovered(boxX, rowBoxY, boxX + grid.boxSize, rowBoxY + grid.boxSize, mouseX, mouseY);
               if (slot == addIndex) {
                   drawAddPresetButton(context, client, boxX, rowBoxY, grid.boxSize, hovered, themeDarkBorder, themePurpleBorder, localAlpha);
                   continue;
               }
               int start = paletteValue.getCustomGradientStart(slot);
               int end = paletteValue.getCustomGradientEnd(slot);
               drawPresetSwatch(context, client, boxX, rowBoxY, grid.boxSize, start, end, paletteValue.isCustomSelected(slot),
                       paletteValue.isEditingCustom(slot), hovered, grid.editFramePad, themeDarkBorder, themePurpleBorder, localAlpha);
           }
       }
   }

   private static void drawPresetSwatch(GuiGraphics context, Minecraft client, float boxX, float boxY, float boxSize,
                                        int start, int end, boolean selected, boolean editing, boolean hovered,
                                        float editFramePad, int themeDarkBorder, int themePurpleBorder, int localAlpha) {
       int inner = (int) (boxSize - 2);
       if ((start & 0xFFFFFF) == (end & 0xFFFFFF)) {
           int fillColor = (start & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawRoundedRect(context, boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, 2, fillColor);
       } else {
           for (int px = 0; px < inner; px++) {
               float ratio = inner <= 1 ? 0f : px / (float) (inner - 1);
               int color = com.shyeuar.baity.utils.ColorGradientUtils.blendColors(start & 0xFFFFFF, end & 0xFFFFFF, ratio);
               int fillColor = (color & 0x00FFFFFF) | (localAlpha << 24);
               context.fill((int) boxX + 1 + px, (int) boxY + 1, (int) boxX + 2 + px, (int) (boxY + boxSize - 1), fillColor);
           }
       }
       int borderColor = selected || hovered
               ? ((themePurpleBorder & 0x00FFFFFF) | (localAlpha << 24))
               : ((themeDarkBorder & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.drawRoundedRectOutline(context, boxX, boxY, boxX + boxSize, boxY + boxSize, 2, borderColor);
       if (selected) {
           GuiRenderUtil.drawRoundedRectOutline(context, boxX - 1, boxY - 1, boxX + boxSize + 1, boxY + boxSize + 1, 3, borderColor);
       }
       if (editing) {
           float frameX1 = boxX - editFramePad;
           float frameY1 = boxY - editFramePad;
           float frameX2 = boxX + boxSize + editFramePad;
           float frameY2 = boxY + boxSize + editFramePad;
           int skyBlue = (com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.PRESET_SKY_BLUE_RGB & 0xFFFFFF)
                   | (localAlpha << 24);
           GuiRenderUtil.drawRoundedRectOutline(context, frameX1, frameY1, frameX2, frameY2, 3, skyBlue);
           drawPresetEditingMarker(context, client, frameX2, frameY1, boxSize, localAlpha);
       }
   }

   private static void drawPresetDeleteButton(GuiGraphics context, float x1, float y1, float size, boolean armed,
                                              boolean hovered, boolean enabled, int localAlpha, int purple) {
       int greyBorder = (new java.awt.Color(220, 220, 220, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int greyBg = (new java.awt.Color(30, 30, 30, 80).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int iconColor;
       if (armed && enabled) {
           int bg3 = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB() & 0xFFFFFF;
           int hubFace = blendArgb(bg3, 0xFF6A5C, 0.35f);
           int hubRim = blendArgb(0xFF8A7A, 0xFFB0A8, 0.5f);
           int face = (hubFace & 0x00FFFFFF) | (localAlpha << 24);
           int rim = (hubRim & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawRoundedRect(context, x1, y1, x1 + size, y1 + size, 2, face);
           GuiRenderUtil.drawRoundedRectOutline(context, x1, y1, x1 + size, y1 + size, 2, rim);
           iconColor = (0xFFFFECEA & 0x00FFFFFF) | (localAlpha << 24);
       } else {
           int border = enabled && hovered ? purple : greyBorder;
           GuiRenderUtil.drawRoundedRect(context, x1, y1, x1 + size, y1 + size, 2, greyBg);
           GuiRenderUtil.drawRoundedRectOutline(context, x1, y1, x1 + size, y1 + size, 2, border);
           iconColor = enabled
                   ? ((0xFFCCCCCC & 0x00FFFFFF) | (localAlpha << 24))
                   : ((0xFF888888 & 0x00FFFFFF) | (localAlpha << 24));
       }
       drawRadialCloseIcon(context, x1 + size * 0.5f, y1 + size * 0.5f, size, iconColor);
   }

   private static void drawRadialCloseIcon(GuiGraphics context, float centerX, float centerY, float buttonSize, int color) {
       float half = buttonSize * 0.22f;
       int stroke = Math.max(2, Math.round(buttonSize * 0.11f));
       drawThickLine(context, centerX - half, centerY - half, centerX + half, centerY + half, stroke, color);
       drawThickLine(context, centerX - half, centerY + half, centerX + half, centerY - half, stroke, color);
   }

   private static void drawPresetEditingMarker(GuiGraphics context, Minecraft client, float frameX2, float frameY1,
                                               float boxSize, int localAlpha) {
       String mark = "✯";
       float scale = Math.min(0.9f, boxSize / client.font.lineHeight * 0.55f);
       int color = (com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.PRESET_SKY_BLUE_RGB & 0xFFFFFF)
               | (localAlpha << 24);
       float scaledW = client.font.width(mark) * scale;
       float scaledH = client.font.lineHeight * scale;
       float markX = frameX2 - scaledW;
       float markY = frameY1 - scaledH * 0.35f;
       drawScaledLabel(context, client, mark, markX, markY, color, scale);
   }

   private static void drawAddPresetButton(GuiGraphics context, Minecraft client, float boxX, float boxY, float boxSize,
                                           boolean hovered, int themeDarkBorder, int themePurpleBorder, int localAlpha) {
       int bg = ((new java.awt.Color(28, 28, 28, 200).getRGB()) & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, 2, bg);
       int borderColor = hovered
               ? ((themePurpleBorder & 0x00FFFFFF) | (localAlpha << 24))
               : ((themeDarkBorder & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.drawRoundedRectOutline(context, boxX, boxY, boxX + boxSize, boxY + boxSize, 2, borderColor);
       String plus = "+";
       int w = client.font.width(plus);
       int h = client.font.lineHeight;
       int tx = (int) (boxX + (boxSize - w) * 0.5f);
       int ty = (int) (boxY + (boxSize - h) * 0.5f);
       context.drawString(client.font, plus, tx, ty, 0xFFFFFFFF | (localAlpha << 24), false);
   }

   public static float getFancyDmgPresetHeight(float subOptionHeight) {
       net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
       if (client != null) {
           return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore
                   .computeRenderedPresetPanelHeight(subOptionHeight, client.font);
       }
       return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.estimatePresetPanelHeight(subOptionHeight);
   }

   private static int blendArgb(int a, int b, float ratio) {
       ratio = Math.max(0f, Math.min(1f, ratio));
       int ar = (a >> 16) & 0xFF;
       int ag = (a >> 8) & 0xFF;
       int ab = a & 0xFF;
       int br = (b >> 16) & 0xFF;
       int bg = (b >> 8) & 0xFF;
       int bb = b & 0xFF;
       int r = (int) (ar + (br - ar) * ratio);
       int g = (int) (ag + (bg - ag) * ratio);
       int bl = (int) (ab + (bb - ab) * ratio);
       return (r << 16) | (g << 8) | bl;
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
                                                Integer gradientInputCaretCp,
                                                ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       float blockHeight = getGradientEditorHeight(subOptionHeight);
       int bg = (new java.awt.Color(35, 35, 35, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + blockHeight, 6, bg);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       String selectedHex = value.getSelectedHex();
       boolean editingHex = editingGradient != null
               && editingGradient.valueName.equals(value.getName())
               && !editingGradient.symbolInput;
       if (editingHex) {
           selectedHex = "#" + gradientInputText.toUpperCase(java.util.Locale.ROOT);
       }
       GradientEditorBottomLayout bottom = computeGradientEditorBottomLayout(client, x1, y, x2, blockHeight, selectedHex, true);
       float mapX1 = bottom.mapX1;
       float mapY1 = bottom.mapY1;
       float mapX2 = bottom.mapX2;
       float mapY2 = bottom.mapY2;
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

       renderGradientEditorBottomControls(context, client, theme, bottom, selectedHex, editingHex, gradientInputCaretCp,
               null, false, null, localAlpha, hoveredTooltipInfo, mouseX, mouseY, "Set the selected color to the unselected color.");

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

   public static void renderFancyDmgColorEditorValue(GuiGraphics context, Minecraft client,
                                                     com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue value,
                                                     Theme theme, float x1, float y, float x2, float subOptionHeight,
                                                     float mouseX, float mouseY, int localAlpha,
                                                     com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                                     String gradientInputText,
                                                     Integer gradientInputCaretCp,
                                                     ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       GradientEditorValue gradient = value.gradient();
       float blockHeight = getGradientEditorHeight(subOptionHeight);
       int bg = (new java.awt.Color(35, 35, 35, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + blockHeight, 6, bg);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int purple = (theme.BG_3.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       String selectedHex = gradient.getSelectedHex();
       String symbolDisplay = value.getSymbols();
       boolean editingHex = editingGradient != null
               && editingGradient.valueName.equals(value.getName())
               && !editingGradient.symbolInput;
       boolean editingSymbol = editingGradient != null
               && editingGradient.valueName.equals(value.getName())
               && editingGradient.symbolInput;
       if (editingHex) {
           selectedHex = "#" + gradientInputText.toUpperCase(java.util.Locale.ROOT);
       }
       if (editingSymbol) {
           symbolDisplay = gradientInputText;
       }
       GradientEditorBottomLayout bottom = computeGradientEditorBottomLayout(
               client, x1, y, x2, blockHeight, selectedHex, true, symbolDisplay, editingSymbol);
       float mapX1 = bottom.mapX1;
       float mapY1 = bottom.mapY1;
       float mapX2 = bottom.mapX2;
       float mapY2 = bottom.mapY2;
       drawHueSatMap(context, mapX1, mapY1, mapX2, mapY2, localAlpha);

       float p1x = mapX1 + gradient.getSelectedHue() * (mapX2 - mapX1);
       float p1y = mapY1 + (1f - gradient.getSelectedSat()) * (mapY2 - mapY1);
       int pointFill = java.awt.Color.HSBtoRGB(gradient.getSelectedHue(), gradient.getSelectedSat(), GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
       int selectorColor = (pointFill & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, p1x - 2, p1y - 2, p1x + 3, p1y + 3, 0, selectorColor);
       float mapMidY = (mapY1 + mapY2) * 0.5f;
       int pointBorderBase = p1y >= mapMidY ? 0x000000 : 0xFFFFFF;
       int pointBorder = (pointBorderBase & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, p1x - 3, p1y - 3, p1x + 4, p1y + 4, 0, pointBorder);

       float sliderX1 = x2 - 48;
       float sliderX2 = x2 - 36;
       drawValueSlider(context, sliderX1, mapY1, sliderX2, mapY2, gradient.getSelectedHue(), gradient.getSelectedSat(), localAlpha);
       float knobY = mapY2 - gradient.getSelectedVal() * (mapY2 - mapY1);
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
       int color1 = (0xFF000000 | gradient.getStartColor() & 0xFFFFFF);
       int color2 = (0xFF000000 | gradient.getEndColor() & 0xFFFFFF);
       GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box1Y1 + 1, box1X2 - 1, box1Y2 - 1, 2, color1);
       GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box2Y1 + 1, box1X2 - 1, box2Y2 - 1, 2, color2);
       int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
       int themePurpleBorder = theme.BG_3.getRGB();
       boolean hoverL = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(box1X1, box1Y1, box1X2, box1Y2, mouseX, mouseY);
       boolean hoverR = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(box1X1, box2Y1, box1X2, box2Y2, mouseX, mouseY);
       int borderL = (gradient.getSelectedPoint() == 0 || hoverL ? themePurpleBorder : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
       int borderR = (gradient.getSelectedPoint() == 1 || hoverR ? themePurpleBorder : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box1Y1, box1X2, box1Y2, 2, borderL);
       GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box2Y1, box1X2, box2Y2, 2, borderR);
       if (gradient.getSelectedPoint() == 0) {
           GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box1Y1 - 1, box1X2 + 1, box1Y2 + 1, 3, borderL);
       } else {
           GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box2Y1 - 1, box1X2 + 1, box2Y2 + 1, 3, borderR);
       }
       drawScaledLabel(context, client, "L", box1X1 + 6, box1Y2 + 2, textColor, 0.85f);
       drawScaledLabel(context, client, "R", box1X1 + 6, box2Y1 - 8, textColor, 0.85f);

       renderGradientEditorBottomControls(context, client, theme, bottom, selectedHex, editingHex, gradientInputCaretCp,
               symbolDisplay, editingSymbol, gradientInputCaretCp, localAlpha, hoveredTooltipInfo, mouseX, mouseY,
               "Set the selected color to the unselected color.");

       FancyDmgEditorBottomRowLayout row = layoutFancyDmgEditorBottomRow(client, x1, y, blockHeight, bottom.symbolInputX1);
       boolean previewHovered = GuiRenderUtil.isHovered(row.previewX1, row.previewY1, row.previewX2, row.previewY2, mouseX, mouseY);
       int previewBg = previewHovered
               ? ((new java.awt.Color(60, 60, 60, 120).getRGB() & 0x00FFFFFF) | (localAlpha << 24))
               : ((new java.awt.Color(40, 40, 40, 90).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.draw3DRect(context, row.previewX1, row.previewY1, row.previewX2, row.previewY2, previewBg, 0f);
       int previewBorder = previewHovered ? purple : ((new java.awt.Color(90, 90, 90, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.drawRoundedRectOutline(context, row.previewX1, row.previewY1, row.previewX2, row.previewY2, 3, previewBorder);
       net.minecraft.network.chat.Component preview = com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.buildPreviewForEditor(value);
       int previewWidth = client.font.width(preview);
       int previewX = (int) (row.previewX1 + (row.previewX2 - row.previewX1 - previewWidth) * 0.5f);
       int previewY = (int) (row.previewY1 + (row.previewY2 - row.previewY1 - client.font.lineHeight) * 0.5f);
       context.enableScissor((int) row.previewX1 + 2, (int) row.previewY1 + 1, (int) row.previewX2 - 2, (int) row.previewY2 - 1);
       context.drawString(client.font, preview, previewX, previewY, 0xFFFFFFFF, false);
       context.disableScissor();
       if (previewHovered && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "click to change the editing preset";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(hoveredTooltipInfo.tooltip);
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }

       drawEnchantToggle(context, client, row.compactX1, row.compactY1, row.compactX2, row.compactY2, value.isCompact(), mouseX, mouseY, localAlpha, purple, textColor);
       if (GuiRenderUtil.isHovered(row.compactX1, row.compactY1, row.compactX2, row.compactY2, mouseX, mouseY) && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "compact";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal("compact");
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }

       drawEnchantToggle(context, client, row.boldX1, row.boldY1, row.boldX2, row.boldY2, value.isBold(), mouseX, mouseY, localAlpha, purple, textColor);
       if (GuiRenderUtil.isHovered(row.boldX1, row.boldY1, row.boldX2, row.boldY2, mouseX, mouseY) && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "bold";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal("bold");
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }

       boolean deleteHovered = GuiRenderUtil.isHovered(row.deleteX1, row.deleteY1, row.deleteX2, row.deleteY2, mouseX, mouseY);
       boolean deleteArmed = value.getDeleteArmedCustomIndex() == value.getEditingCustomIndex()
               && com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.canDeleteCurrentEditingPreset();
       boolean deleteEnabled = com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.canDeleteCurrentEditingPreset();
       drawPresetDeleteButton(context, row.deleteX1, row.deleteY1, FANCY_DMG_EDITOR_TOGGLE_SIZE, deleteArmed, deleteHovered, deleteEnabled, localAlpha, purple);
       if (deleteArmed && deleteHovered && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "reclick to confirm!";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(hoveredTooltipInfo.tooltip)
                   .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(com.shyeuar.baity.config.DevConfig.DEV_PREFIX_COLOR));
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }
   }

   public static void renderChromaFishingLineColorEditorValue(GuiGraphics context, Minecraft client,
                                                        ChromaFishingLineColorEditorValue value,
                                                        Theme theme, float x1, float y, float x2, float subOptionHeight,
                                                        float mouseX, float mouseY, int localAlpha,
                                                        com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                                        String gradientInputText,
                                                        Integer gradientInputCaretCp,
                                                        ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       GradientEditorValue gradient = value.gradient();
       float blockHeight = getGradientEditorHeight(subOptionHeight);
       int bg = (new java.awt.Color(35, 35, 35, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + blockHeight, 6, bg);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       String selectedHex = gradient.getSelectedHex();
       if (editingGradient != null && value.getName().equals(editingGradient.valueName)) {
           selectedHex = "#" + gradientInputText.toUpperCase(java.util.Locale.ROOT);
       }
       GradientEditorBottomLayout bottom = computeGradientEditorBottomLayout(client, x1, y, x2, blockHeight, selectedHex, true);
       float mapX1 = bottom.mapX1;
       float mapY1 = bottom.mapY1;
       float mapX2 = bottom.mapX2;
       float mapY2 = bottom.mapY2;
       drawHueSatMap(context, mapX1, mapY1, mapX2, mapY2, localAlpha);

       float p1x = mapX1 + gradient.getSelectedHue() * (mapX2 - mapX1);
       float p1y = mapY1 + (1f - gradient.getSelectedSat()) * (mapY2 - mapY1);
       int pointFill = java.awt.Color.HSBtoRGB(gradient.getSelectedHue(), gradient.getSelectedSat(), GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
       int selectorColor = (pointFill & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, p1x - 2, p1y - 2, p1x + 3, p1y + 3, 0, selectorColor);
       float mapMidY = (mapY1 + mapY2) * 0.5f;
       int pointBorderBase = p1y >= mapMidY ? 0x000000 : 0xFFFFFF;
       int pointBorder = (pointBorderBase & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, p1x - 3, p1y - 3, p1x + 4, p1y + 4, 0, pointBorder);

       float sliderX1 = x2 - 48;
       float sliderX2 = x2 - 36;
       drawValueSlider(context, sliderX1, mapY1, sliderX2, mapY2, gradient.getSelectedHue(), gradient.getSelectedSat(), localAlpha);
       float knobY = mapY2 - gradient.getSelectedVal() * (mapY2 - mapY1);
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
       int color1 = (0xFF000000 | gradient.getStartColor() & 0xFFFFFF);
       int color2 = (0xFF000000 | gradient.getEndColor() & 0xFFFFFF);
       GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box1Y1 + 1, box1X2 - 1, box1Y2 - 1, 2, color1);
       GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box2Y1 + 1, box1X2 - 1, box2Y2 - 1, 2, color2);
       int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
       boolean hoverL = GuiRenderUtil.isHovered(box1X1, box1Y1, box1X2, box1Y2, mouseX, mouseY);
       boolean hoverR = GuiRenderUtil.isHovered(box1X1, box2Y1, box1X2, box2Y2, mouseX, mouseY);
       int borderL = (gradient.getSelectedPoint() == 0 || hoverL ? theme.BG_3.getRGB() : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
       int borderR = (gradient.getSelectedPoint() == 1 || hoverR ? theme.BG_3.getRGB() : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box1Y1, box1X2, box1Y2, 2, borderL);
       GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box2Y1, box1X2, box2Y2, 2, borderR);
       if (gradient.getSelectedPoint() == 0) {
           GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box1Y1 - 1, box1X2 + 1, box1Y2 + 1, 3, borderL);
       } else {
           GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box2Y1 - 1, box1X2 + 1, box2Y2 + 1, 3, borderR);
       }
       drawScaledLabel(context, client, "L", box1X1 + 6, box1Y2 + 2, textColor, 0.85f);
       drawScaledLabel(context, client, "R", box1X1 + 6, box2Y1 - 8, textColor, 0.85f);

       boolean editingHex = editingGradient != null && value.getName().equals(editingGradient.valueName);
       renderGradientEditorBottomControls(context, client, theme, bottom, selectedHex, editingHex, gradientInputCaretCp,
               null, false, null, localAlpha, hoveredTooltipInfo, mouseX, mouseY, "Set the selected color to the unselected color.");

       int rodSize = 16;
       float previewY = y + blockHeight - rodSize - 4;
       float rodX = x1 + 10;
       context.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD), (int) rodX, (int) previewY);

       float lineX1 = rodX + rodSize + 4;
       float lineX2 = Math.min(x2 - 12, lineX1 + 72);
       float lineY = previewY + rodSize * 0.55f;
       int segments = 16;
       long nowMs = System.currentTimeMillis();
       for (int i = 0; i < segments; i++) {
           float t0 = i / (float) segments;
           float t1 = (i + 1) / (float) segments;
           float sx = lineX1 + (lineX2 - lineX1) * t0;
           float ex = lineX1 + (lineX2 - lineX1) * t1;
           float mid = (t0 + t1) * 0.5f;
           int argb = com.shyeuar.baity.features.fishing.ChromaFishingLine.previewColorArgb(
                   1.0f - mid, gradient.getStartColor(), gradient.getEndColor(), nowMs);
           int lineColor = (argb & 0x00FFFFFF) | (localAlpha << 24);
           drawThickLine(context, sx, lineY, ex, lineY, 2, lineColor);
       }
   }

   public static void renderEnchantLoreColorEditorValue(GuiGraphics context, Minecraft client, EnchantLoreColorEditorValue value, Theme theme,
                                                        float x1, float y, float x2, float subOptionHeight, float mouseX, float mouseY, int localAlpha,
                                                        com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                                        String gradientInputText,
                                                        Integer gradientInputCaretCp,
                                                        ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       GradientEditorValue gradient = value.gradient();
       float blockHeight = getGradientEditorHeight(subOptionHeight);
       int bg = (new java.awt.Color(35, 35, 35, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + blockHeight, 6, bg);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int purple = (theme.BG_3.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       String selectedHex = gradient.getSelectedHex();
       if (editingGradient != null && value.getName().equals(editingGradient.valueName)) {
           selectedHex = "#" + gradientInputText.toUpperCase(java.util.Locale.ROOT);
       }
       GradientEditorBottomLayout bottom = computeGradientEditorBottomLayout(client, x1, y, x2, blockHeight, selectedHex, true);
       float mapX1 = bottom.mapX1;
       float mapY1 = bottom.mapY1;
       float mapX2 = bottom.mapX2;
       float mapY2 = bottom.mapY2;
       drawHueSatMap(context, mapX1, mapY1, mapX2, mapY2, localAlpha);

       float p1x = mapX1 + gradient.getSelectedHue() * (mapX2 - mapX1);
       float p1y = mapY1 + (1f - gradient.getSelectedSat()) * (mapY2 - mapY1);
       int pointFill = java.awt.Color.HSBtoRGB(gradient.getSelectedHue(), gradient.getSelectedSat(), GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
       int selectorColor = (pointFill & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, p1x - 2, p1y - 2, p1x + 3, p1y + 3, 0, selectorColor);
       float mapMidY = (mapY1 + mapY2) * 0.5f;
       int pointBorderBase = p1y >= mapMidY ? 0x000000 : 0xFFFFFF;
       int pointBorder = (pointBorderBase & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, p1x - 3, p1y - 3, p1x + 4, p1y + 4, 0, pointBorder);

       float sliderX1 = x2 - 48;
       float sliderX2 = x2 - 36;
       drawValueSlider(context, sliderX1, mapY1, sliderX2, mapY2, gradient.getSelectedHue(), gradient.getSelectedSat(), localAlpha);
       float knobY = mapY2 - gradient.getSelectedVal() * (mapY2 - mapY1);
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
       int color1 = (0xFF000000 | gradient.getStartColor() & 0xFFFFFF);
       int color2 = (0xFF000000 | gradient.getEndColor() & 0xFFFFFF);
       GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box1Y1 + 1, box1X2 - 1, box1Y2 - 1, 2, color1);
       GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box2Y1 + 1, box1X2 - 1, box2Y2 - 1, 2, color2);
       int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
       boolean hoverL = GuiRenderUtil.isHovered(box1X1, box1Y1, box1X2, box1Y2, mouseX, mouseY);
       boolean hoverR = GuiRenderUtil.isHovered(box1X1, box2Y1, box1X2, box2Y2, mouseX, mouseY);
       int borderL = (gradient.getSelectedPoint() == 0 || hoverL ? theme.BG_3.getRGB() : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
       int borderR = (gradient.getSelectedPoint() == 1 || hoverR ? theme.BG_3.getRGB() : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box1Y1, box1X2, box1Y2, 2, borderL);
       GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box2Y1, box1X2, box2Y2, 2, borderR);
       if (gradient.getSelectedPoint() == 0) {
           GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box1Y1 - 1, box1X2 + 1, box1Y2 + 1, 3, borderL);
       } else {
           GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box2Y1 - 1, box1X2 + 1, box2Y2 + 1, 3, borderR);
       }
       drawScaledLabel(context, client, "L", box1X1 + 6, box1Y2 + 2, textColor, 0.85f);
       drawScaledLabel(context, client, "R", box1X1 + 6, box2Y1 - 8, textColor, 0.85f);

       boolean editingHex = editingGradient != null && value.getName().equals(editingGradient.valueName);
       renderGradientEditorBottomControls(context, client, theme, bottom, selectedHex, editingHex, gradientInputCaretCp,
               null, false, null, localAlpha, hoveredTooltipInfo, mouseX, mouseY, "Set the selected color to the unselected color.");

       float tierBtnX1 = x1 + 10;
       float tierBtnY1 = y + blockHeight - 22;
       float tierBtnX2 = tierBtnX1 + EnchantLore.tierButtonWidth(client.font);
       float tierBtnY2 = tierBtnY1 + EnchantLore.tierButtonHeight(client.font);
       boolean tierHovered = GuiRenderUtil.isHovered(tierBtnX1, tierBtnY1, tierBtnX2, tierBtnY2, mouseX, mouseY);
       int tierBg = tierHovered
           ? ((new java.awt.Color(60, 60, 60, 120).getRGB() & 0x00FFFFFF) | (localAlpha << 24))
           : ((new java.awt.Color(40, 40, 40, 90).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.draw3DRect(context, tierBtnX1, tierBtnY1, tierBtnX2, tierBtnY2, tierBg, 0f);
       int tierBorder = tierHovered ? purple : ((new java.awt.Color(90, 90, 90, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       GuiRenderUtil.drawRoundedRectOutline(context, tierBtnX1, tierBtnY1, tierBtnX2, tierBtnY2, 3, tierBorder);
       EnchantLore.Tier previewTier = EnchantLore.Tier.values()[value.getEditingTier()];
       net.minecraft.util.FormattedCharSequence tierPreview = EnchantLore.tierPreview(previewTier).getVisualOrderText();
       int previewWidth = client.font.width(tierPreview);
       int previewX = (int) (tierBtnX1 + (tierBtnX2 - tierBtnX1 - previewWidth) * 0.5f);
       int previewY = (int) (tierBtnY1 + (tierBtnY2 - tierBtnY1 - client.font.lineHeight) * 0.5f);
       context.drawString(client.font, tierPreview, previewX, previewY, 0xFFFFFFFF, false);
       if (tierHovered && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "click to change the editing rarity";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal(hoveredTooltipInfo.tooltip);
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }

       float toggleSize = 18;
       float boldX1 = tierBtnX2 + 6;
       float boldY1 = tierBtnY1 - 1;
       float boldX2 = boldX1 + toggleSize;
       float boldY2 = boldY1 + toggleSize;
       drawEnchantToggle(context, client, boldX1, boldY1, boldX2, boldY2, value.isBold(), mouseX, mouseY, localAlpha, purple, textColor);
       if (GuiRenderUtil.isHovered(boldX1, boldY1, boldX2, boldY2, mouseX, mouseY) && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "bold";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal("bold");
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }

       float rainbowX1 = boldX2 + 6;
       float rainbowY1 = boldY1;
       float rainbowX2 = rainbowX1 + toggleSize;
       float rainbowY2 = boldY2;
       drawEnchantToggle(context, client, rainbowX1, rainbowY1, rainbowX2, rainbowY2, value.isRainbow(), mouseX, mouseY, localAlpha, purple, textColor);
       if (GuiRenderUtil.isHovered(rainbowX1, rainbowY1, rainbowX2, rainbowY2, mouseX, mouseY) && hoveredTooltipInfo != null) {
           hoveredTooltipInfo.tooltip = "rainbow";
           hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal("rainbow");
           hoveredTooltipInfo.x = (int) (mouseX + 5);
           hoveredTooltipInfo.y = (int) (mouseY + 5);
       }
   }

   private static void drawEnchantToggle(GuiGraphics context, Minecraft client, float x1, float y1, float x2, float y2,
                                         boolean checked, float mouseX, float mouseY, int localAlpha, int purple, int textColor) {
       boolean hovered = GuiRenderUtil.isHovered(x1, y1, x2, y2, mouseX, mouseY);
       int border = hovered || checked ? purple : ((new java.awt.Color(220, 220, 220, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       int innerBg = ((new java.awt.Color(30, 30, 30, 80).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
       if (checked) {
           GuiRenderUtil.drawRoundedRect(context, x1, y1, x2, y2, 2, purple);
       } else {
           GuiRenderUtil.drawRoundedRect(context, x1, y1, x2, y2, 2, innerBg);
       }
       GuiRenderUtil.drawRoundedRectOutline(context, x1, y1, x2, y2, 2, border);
       if (checked) {
           drawToggleCheckmark(context, x1, y1, x2, y2, textColor);
       }
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
   
   public static int getHoveredFancyDmgPresetHit(com.shyeuar.baity.gui.value.FancyDmgSplashPresetValue paletteValue,
                                                 float x1, float y, float x2, float subOptionHeight,
                                                 float mouseX, float mouseY) {
       Minecraft client = Minecraft.getInstance();
       if (client == null) {
           return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_NONE;
       }
       FancyDmgPresetGridLayout grid = layoutFancyDmgPresetGrid(client, x1, y, x2, subOptionHeight);
       float boxY = grid.boxY(0);

       for (int i = 0; i < paletteValue.getBuiltinCount(); i++) {
           float boxX = grid.boxX(i);
           if (GuiRenderUtil.isHovered(boxX, boxY, boxX + grid.boxSize, boxY + grid.boxSize, mouseX, mouseY)) {
               return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_BUILTIN_BASE + i;
           }
       }

       int addIndex = paletteValue.getCustomCount();
       int customRows = paletteValue.getCustomRowCount();
       for (int row = 0; row < customRows; row++) {
           float rowBoxY = grid.boxY(row + 1);
           for (int col = 0; col < 8; col++) {
               int slot = row * 8 + col;
               if (slot > addIndex) {
                   return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_NONE;
               }
               float boxX = grid.boxX(col);
               if (GuiRenderUtil.isHovered(boxX, rowBoxY, boxX + grid.boxSize, rowBoxY + grid.boxSize, mouseX, mouseY)) {
                   if (slot == addIndex) {
                       return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_ADD;
                   }
                   return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_CUSTOM_BASE + slot;
               }
           }
       }
       return com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore.HIT_NONE;
   }

   public static int getHoveredColorIndex(com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue,
                                          float x1, float y, float x2, float subOptionHeight,
                                          float mouseX, float mouseY) {
       return getHoveredPaletteIndex(paletteValue.getColorCount(), x1, y, x2, subOptionHeight, mouseX, mouseY);
   }

   private static int getHoveredPaletteIndex(int colorCount, float x1, float y, float x2, float subOptionHeight,
                                             float mouseX, float mouseY) {
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
