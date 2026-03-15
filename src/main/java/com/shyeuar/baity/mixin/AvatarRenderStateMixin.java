package com.shyeuar.baity.mixin;

import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmedEntityRenderState.class)
public abstract class AvatarRenderStateMixin implements EntityRenderStateInterface {
	@Unique
	private EntityDimensions baity$standingDimensions = null;
	
	@Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
	private static void baity$storeStandingDimensions(LivingEntity livingEntity, ArmedEntityRenderState armedEntityRenderState, ItemModelResolver itemModelResolver, CallbackInfo ci) {
		EntityRenderStateInterface interfaceImpl = (EntityRenderStateInterface) armedEntityRenderState;
		if (livingEntity instanceof Avatar avatar) {
			interfaceImpl.baity$setStandingDimensions(avatar.getDefaultDimensions(Pose.STANDING));
		}
	}
	
	@Override
	public EntityDimensions baity$getStandingDimensions() {
		return this.baity$standingDimensions;
	}
	
	@Override
	public void baity$setStandingDimensions(EntityDimensions dimensions) {
		this.baity$standingDimensions = dimensions;
	}
}
