package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class GroupValue implements Value {
    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final List<Value> children = new ArrayList<>();
    private boolean expanded;
    private boolean needsSeparator;
    private String subModuleSwitchChildName;
    private Option subModuleSwitch;

    public GroupValue(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.expanded = false;
    }

    public GroupValue setSubModuleSwitchChildName(String childName) {
        this.subModuleSwitchChildName = childName;
        return this;
    }

    public boolean hasSubModuleSwitch() {
        return subModuleSwitchChildName != null && !subModuleSwitchChildName.isEmpty();
    }

    public Option getSubModuleSwitchChild() {
        return subModuleSwitch;
    }

    public GroupValue setNeedsSeparator(boolean needsSeparator) {
        this.needsSeparator = needsSeparator;
        return this;
    }

    public GroupValue setExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public GroupValue addChild(Value value) {
        if (value instanceof Option option && hasSubModuleSwitch()
                && subModuleSwitchChildName.equals(value.getName())) {
            this.subModuleSwitch = option;
            return this;
        }
        this.children.add(value);
        return this;
    }

    public List<Value> getChildren() {
        return children;
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
        return expanded;
    }

    @Override
    public void setValue(Object value) {
        this.expanded = (Boolean) value;
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.GROUP;
    }

    @Override
    public boolean needsSeparatorBefore(Value previousValue) {
        return needsSeparator;
    }
}
