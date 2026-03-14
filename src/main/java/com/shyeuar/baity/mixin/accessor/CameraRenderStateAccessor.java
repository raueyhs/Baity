package com.shyeuar.baity.mixin.accessor;

import com.shyeuar.baity.api.CameraRenderStateInterface;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CameraRenderState.class)
public interface CameraRenderStateAccessor extends CameraRenderStateInterface {
}
