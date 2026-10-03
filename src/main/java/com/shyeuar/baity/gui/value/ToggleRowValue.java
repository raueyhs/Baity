package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ToggleRowValue implements Value {

    public static final float TOGGLE_SIZE = 18f;
    public static final float TOGGLE_AREA_WIDTH = 28f;
    public static final float TOGGLE_RIGHT_PADDING = 4f;

    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final Value row;
    private boolean checked;

    public ToggleRowValue(String name, String displayName, ModuleCategory category, Value row, boolean checked) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.row = row;
        this.checked = checked;
    }

    public Value getRow() {
        return row;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Object getValue() {
        return checked;
    }

    @Override
    public void setValue(Object value) {
        this.checked = value instanceof Boolean && (Boolean) value;
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.TOGGLE_ROW;
    }

    public static float rowX2(float x2) {
        return x2 - TOGGLE_AREA_WIDTH;
    }

    public static float toggleX1(float x2) {
        return x2 - TOGGLE_RIGHT_PADDING - TOGGLE_SIZE;
    }

    public static float toggleX2(float x2) {
        return x2 - TOGGLE_RIGHT_PADDING;
    }

    public static float toggleY1(float y, float subOptionHeight) {
        return y + (subOptionHeight - TOGGLE_SIZE) * 0.5f;
    }

    public static float toggleY2(float y, float subOptionHeight) {
        return toggleY1(y, subOptionHeight) + TOGGLE_SIZE;
    }
}
