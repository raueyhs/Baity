package com.shyeuar.baity.api;

import net.minecraft.world.entity.EntityDimensions;

public interface EntityRenderStateInterface {
	EntityDimensions baity$getStandingDimensions();
	void baity$setStandingDimensions(EntityDimensions dimensions);
}
