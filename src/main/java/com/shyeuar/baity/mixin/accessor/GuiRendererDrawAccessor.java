package com.shyeuar.baity.mixin.accessor;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.render.GuiRenderer$Draw")
public interface GuiRendererDrawAccessor {

    @Accessor("pipeline")
    RenderPipeline baity$pipeline();

    @Accessor("draw")
    StagedVertexBuffer.Draw baity$draw();
}
