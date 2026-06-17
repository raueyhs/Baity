package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ChromaFishingLineColorEditorValue implements Value {
    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final GradientEditorValue gradient;

    public ChromaFishingLineColorEditorValue(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.gradient = new GradientEditorValue(
                name,
                displayName,
                category,
                ConfigManager.chromaFishingLineGradientStart,
                ConfigManager.chromaFishingLineGradientEnd
        );
        loadFromConfig();
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
        persistToConfig();
        return String.format("#%06X,#%06X", gradient.getStartColor() & 0xFFFFFF, gradient.getEndColor() & 0xFFFFFF);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String raw) {
            String[] parts = raw.split(",", 2);
            if (parts.length != 2) {
                return;
            }
            gradient.setValue(raw);
            persistToConfig();
        }
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR;
    }

    public GradientEditorValue gradient() {
        return gradient;
    }

    public void loadFromConfig() {
        gradient.setValue(String.format("#%06X,#%06X",
                ConfigManager.chromaFishingLineGradientStart & 0xFFFFFF,
                ConfigManager.chromaFishingLineGradientEnd & 0xFFFFFF));
    }

    public void persistToConfig() {
        ConfigManager.chromaFishingLineGradientStart = gradient.getStartColor();
        ConfigManager.chromaFishingLineGradientEnd = gradient.getEndColor();
    }

    public void resetToDefault() {
        ConfigManager.chromaFishingLineGradientStart = 0x000000;
        ConfigManager.chromaFishingLineGradientEnd = 0x000000;
        loadFromConfig();
    }
}
