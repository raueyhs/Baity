package com.shyeuar.baity.features.fishing;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ChromaFishingLineRenderStateExtension {
    int baity$getHookEntityId();

    void baity$setHookEntityId(int hookEntityId);

    boolean baity$isLocalPlayerHook();

    void baity$setLocalPlayerHook(boolean localPlayerHook);
}
