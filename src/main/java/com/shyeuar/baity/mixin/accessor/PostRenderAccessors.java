package com.shyeuar.baity.mixin.accessor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

public final class PostRenderAccessors {
    private PostRenderAccessors() {
    }

    @Mixin(PostChain.class)
    public interface PostChainAccessor {
        @Accessor("passes")
        List<PostPass> baity$getPasses();
    }

    @Mixin(PostPass.class)
    public interface PostPassAccessor {
        @Accessor("customUniforms")
        Map<String, GpuBuffer> baity$getCustomUniforms();
    }

    @Mixin(ShaderManager.class)
    public interface ShaderManagerAccessor {
        @Accessor("compilationCache")
        ShaderManager.CompilationCache baity$getCompilationCache();
    }
}
