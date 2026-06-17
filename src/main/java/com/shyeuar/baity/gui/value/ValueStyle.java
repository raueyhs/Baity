package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum ValueStyle {
    DEFAULT,
    BUTTON_LIKE,
    SLIDER,
    COLOR_PALETTE,
    GROUP,
    GRADIENT_EDITOR,
    FANCY_DMG_COLOR_EDITOR,
    FANCY_DMG_PRESET,
    ENCHANT_LORE_COLOR_EDITOR,
    CHROMA_FISHING_LINE_COLOR_EDITOR,
    TEXT_LINE_INPUT,
    CROSSHAIR_PAINTER
}

