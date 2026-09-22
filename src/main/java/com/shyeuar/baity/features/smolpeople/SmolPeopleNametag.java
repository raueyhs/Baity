package com.shyeuar.baity.features.smolpeople;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SmolPeopleNametag {
    public static final float VERTICAL_OFFSET = -0.4f;

    private SmolPeopleNametag() {
    }

    public static boolean isSmolPeopleActive() {
        Module module = ModuleManager.getModuleByName("SmolPeople");
        return module != null && module.isEnabled();
    }

    public static boolean usesSmolNametag(int entityId) {
        return SmolFriendManager.shouldApplySmolTo(entityId);
    }

    public static float getMirrorNametagArmorStandOffset(int entityId) {
        if (!SmolFriendManager.isMirrorNametagArmorStand(entityId)) {
            return 0f;
        }
        return VERTICAL_OFFSET;
    }

    public static float getVerticalOffset(int entityId) {
        if (!usesSmolNametag(entityId)) {
            return 0f;
        }
        return VERTICAL_OFFSET;
    }

    public static float adjustNametagHeight(float baseHeight, int entityId) {
        return baseHeight + getVerticalOffset(entityId);
    }
}
