package com.shyeuar.baity.mixin.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer$Draw")
public interface StagedVertexBufferDrawAccessor {

    @Accessor("indexCount")
    int baity$indexCount();
}
