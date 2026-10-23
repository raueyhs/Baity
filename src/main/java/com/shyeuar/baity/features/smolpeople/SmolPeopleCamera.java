package com.shyeuar.baity.features.smolpeople;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class SmolPeopleCamera {
    public static final float THIRD_PERSON_FRONT_Y_OFFSET = -0.65f;

    private SmolPeopleCamera() {
    }

    public static boolean isThirdPersonFrontActive() {
        if (!SmolPeopleNametag.isSmolPeopleActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        // TODO(26.2 backport): swimming already lowers the third person camera, so the smol offset
        // would push it too far down. Only exists on this branch.
        boolean swimming = mc.player.isVisuallySwimming() || mc.player.isSwimming();
        if (swimming && !com.shyeuar.baity.utils.NoSwimPoseUtils.shouldForceStandingModelAppearance()) {
            return false;
        }
        return mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT;
    }

    public static Vec3 applyThirdPersonFrontOffset(Vec3 position) {
        if (!isThirdPersonFrontActive()) {
            return position;
        }
        return new Vec3(position.x, position.y + THIRD_PERSON_FRONT_Y_OFFSET, position.z);
    }
}
