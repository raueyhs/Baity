package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.shyeuar.baity.mixin.accessor.GuiRendererDrawAccessor;
import com.shyeuar.baity.mixin.accessor.StagedVertexBufferDrawAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Environment(EnvType.CLIENT)
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @WrapOperation(
            method = "executeDraw(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setIndexBuffer(Lcom/mojang/renderpearl/api/buffers/GpuBuffer;Lcom/mojang/renderpearl/api/pipeline/IndexType;)V"
            ),
            require = 1
    )
    private void baity$fixNonQuadIndexing(
            RenderPass pass,
            GpuBuffer buffer,
            IndexType indexType,
            Operation<Void> original,
            @Coerce Object draw,
            RenderPass drawPass
    ) {
        GuiRendererDrawAccessor drawAccessor = (GuiRendererDrawAccessor) draw;
        RenderPipeline pipeline = drawAccessor.baity$pipeline();
        if (!"baity".equals(pipeline.getLocation().getNamespace())
                || pipeline.getPrimitiveTopology() == PrimitiveTopology.QUADS) {
            original.call(pass, buffer, indexType);
            return;
        }
        StagedVertexBufferDrawAccessor stagedAccessor =
                (StagedVertexBufferDrawAccessor) drawAccessor.baity$draw();
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                RenderSystem.getSequentialBuffer(pipeline.getPrimitiveTopology());
        pass.setIndexBuffer(shapeIndexBuffer.getBuffer(stagedAccessor.baity$indexCount()), shapeIndexBuffer.type());
    }
}
