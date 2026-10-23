package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The value is the hue (0..1) the text starts at.
 */
@Environment(EnvType.CLIENT)
public class HueBandValue implements Value {

    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final float defaultHue;
    private final java.util.function.BooleanSupplier ownerEnabled;
    private float hue;

    public HueBandValue(String name, String displayName, ModuleCategory category, float hue,
                        java.util.function.BooleanSupplier ownerEnabled) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.hue = wrap(hue);
        this.defaultHue = this.hue;
        this.ownerEnabled = ownerEnabled;
    }

    public boolean isEditAllowed() {
        return ownerEnabled == null || ownerEnabled.getAsBoolean();
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = Math.max(0.0f, Math.min(1.0f, hue));
    }

    public void resetToDefault() {
        this.hue = defaultHue;
    }

    public static int hueToRgb(float hue) {
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(wrap(hue), 1.0f, 1.0f) & 0xFFFFFF);
    }

    private static float wrap(float value) {
        float wrapped = value % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
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
        return (double) hue;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Number number) {
            setHue(number.floatValue());
        }
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.HUE_BAND;
    }
}
