package com.shyeuar.baity.mixin.accessor;

import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ArmedEntityRenderState.class)
public interface AvatarRenderStateAccessor extends EntityRenderStateInterface {
}
