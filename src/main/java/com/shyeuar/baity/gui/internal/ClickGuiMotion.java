package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.animation.ScalarTransition;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ValueTreeUtils;

import java.util.List;
import java.util.Map;

final class ClickGuiMotion {

    private ClickGuiMotion() {
    }

    static List<ValueTreeUtils.ValueEntry> getVisibleEntries(Module module, ClickGuiState state) {
        return ValueTreeUtils.getVisibleEntries(module, (m, group) -> getGroupExpandProgress(state, m, group));
    }

    static String groupKey(String moduleName, String groupName) {
        return moduleName + '\0' + groupName;
    }

    static void updateAnimations(ClickGuiState state, List<Module> modules, ScalarTransition transition) {
        float dt = transition.beginFrame();
        for (Module module : modules) {
            updateExpandAnimation(
                    state.getModuleExpandAnimations(),
                    module.getName(),
                    module.isExpanded(),
                    dt
            );
            for (ValueTreeUtils.ValueEntry entry : ValueTreeUtils.getAllEntries(module)) {
                if (entry.value() instanceof GroupValue group) {
                    updateExpandAnimation(
                            state.getGroupExpandAnimations(),
                            groupKey(module.getName(), group.getName()),
                            group.isExpanded(),
                            dt
                    );
                }
            }
        }
        float target = state.getTargetScrollOffset();
        float animated = ScalarTransition.moveLinear(
                state.getScrollOffset(),
                target,
                dt,
                ScalarTransition.SCROLL_TRANSITION_SECONDS
        );
        state.setAnimatedScrollOffset(animated);
    }

    static float getModuleExpandProgress(ClickGuiState state, Module module) {
        return state.getModuleExpandAnimations().getOrDefault(module.getName(), module.isExpanded() ? 1.0f : 0.0f);
    }

    static float getGroupExpandProgress(ClickGuiState state, Module module, GroupValue group) {
        return state.getGroupExpandAnimations().getOrDefault(
                groupKey(module.getName(), group.getName()),
                group.isExpanded() ? 1.0f : 0.0f
        );
    }

    static float getEntryGroupFactor(ClickGuiState state, Module module, ValueTreeUtils.ValueEntry entry) {
        float factor = 1.0f;
        String groupName = entry.enclosingGroupName();
        while (groupName != null) {
            Value groupValue = ValueTreeUtils.findByName(module, groupName);
            if (!(groupValue instanceof GroupValue group)) {
                break;
            }
            factor *= getGroupExpandProgress(state, module, group);
            ValueTreeUtils.ValueEntry groupEntry = findEntry(module, groupName);
            groupName = groupEntry != null ? groupEntry.enclosingGroupName() : null;
        }
        return factor;
    }

    static float calculateEntriesHeight(
            List<ValueTreeUtils.ValueEntry> entries,
            ClickGuiState state,
            Module module,
            float visibleHeight
    ) {
        if (entries.isEmpty()) {
            return 0.0f;
        }

        int extraHeight = ClickGuiLayout.calculateExtraHeight(entries);
        ClickGuiLayout.ContainerDimensions dims =
                ClickGuiLayout.calculateSubOptionContainer(entries.size(), visibleHeight, extraHeight);

        float height = dims.padding * 2.0f;
        Value previousValue = null;
        for (ValueTreeUtils.ValueEntry entry : entries) {
            Value value = entry.value();
            if (value.needsSeparatorBefore(previousValue)) {
                height += 12.0f * getEntryGroupFactor(state, module, entry);
            }
            height += entryHeight(value, dims.subOptionHeight) * getEntryGroupFactor(state, module, entry);
            previousValue = value;
        }
        return height;
    }

    static float calculateModuleSubOptionsHeight(Module module, ClickGuiState state, float visibleHeight) {
        float expandProgress = getModuleExpandProgress(state, module);
        if (expandProgress <= 0.0f) {
            return 0.0f;
        }
        List<ValueTreeUtils.ValueEntry> entries = getVisibleEntries(module, state);
        if (entries.isEmpty()) {
            return 0.0f;
        }
        return (calculateEntriesHeight(entries, state, module, visibleHeight) + 5.0f) * expandProgress;
    }

    static float calculateContentHeightForModules(List<Module> modules, ClickGuiState state, float visibleHeight) {
        float contentHeight = 0.0f;
        for (Module module : modules) {
            contentHeight += ClickGuiState.ITEM_HEIGHT;
            contentHeight += calculateModuleSubOptionsHeight(module, state, visibleHeight);
        }
        return contentHeight;
    }

    private static void updateExpandAnimation(
            Map<String, Float> animations,
            String key,
            boolean expanded,
            float dt
    ) {
        float target = expanded ? 1.0f : 0.0f;
        float current = animations.getOrDefault(key, target);
        animations.put(key, ScalarTransition.moveLinear(current, target, dt, ScalarTransition.EXPAND_TRANSITION_SECONDS));
    }

    private static ValueTreeUtils.ValueEntry findEntry(Module module, String valueName) {
        for (ValueTreeUtils.ValueEntry entry : ValueTreeUtils.getAllEntries(module)) {
            if (valueName.equals(entry.value().getName())) {
                return entry;
            }
        }
        return null;
    }

    private static float entryHeight(Value value, int baseHeight) {
        if (value.getStyle() == ValueStyle.COLOR_PALETTE) {
            return baseHeight * 2.0f;
        }
        if (value.getStyle() == ValueStyle.FANCY_DMG_PRESET) {
            return com.shyeuar.baity.gui.render.ValueStyleRenderer.getFancyDmgPresetHeight(baseHeight);
        }
        if (value.getStyle() == ValueStyle.GRADIENT_EDITOR
                || value.getStyle() == ValueStyle.FANCY_DMG_COLOR_EDITOR
                || value.getStyle() == ValueStyle.ENCHANT_LORE_COLOR_EDITOR
                || value.getStyle() == ValueStyle.CHROMA_FISHING_LINE_COLOR_EDITOR) {
            return baseHeight * 6.0f;
        }
        if (value.getStyle() == ValueStyle.CROSSHAIR_PAINTER) {
            return baseHeight * 8.0f;
        }
        if (value.getStyle() == ValueStyle.TEXT_LIST
                && value instanceof com.shyeuar.baity.gui.value.TextListValue listValue) {
            return com.shyeuar.baity.gui.render.ValueStyleRenderer.getTextListHeight(listValue, baseHeight);
        }
        return baseHeight;
    }
}
