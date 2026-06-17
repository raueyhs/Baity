package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.fishing.ChromaFishingLineRenderStateExtension;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FishingHookRenderState.class)
public class FishingHookRenderStateMixin implements ChromaFishingLineRenderStateExtension {
    @Unique
    private int baity$hookEntityId = -1;

    @Unique
    private boolean baity$localPlayerHook;

    @Override
    public int baity$getHookEntityId() {
        return baity$hookEntityId;
    }

    @Override
    public void baity$setHookEntityId(int hookEntityId) {
        this.baity$hookEntityId = hookEntityId;
    }

    @Override
    public boolean baity$isLocalPlayerHook() {
        return baity$localPlayerHook;
    }

    @Override
    public void baity$setLocalPlayerHook(boolean localPlayerHook) {
        this.baity$localPlayerHook = localPlayerHook;
    }
}
