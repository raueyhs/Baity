package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {

    @Mutable
    @Accessor("environmentAttributes")
    void baity$setEnvironmentAttributes(EnvironmentAttributeSystem system);

    @Invoker("addEnvironmentAttributeLayers")
    EnvironmentAttributeSystem.Builder baity$addEnvironmentAttributeLayers(EnvironmentAttributeSystem.Builder builder);
}
