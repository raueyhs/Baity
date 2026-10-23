package com.shyeuar.baity.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO(ModernUI-26.2): Verify pipeline/shader compatibility when ModernUI ships for 26.2.
public final class BaityModernUiFloatingTextTypes {

    private static final RenderPipeline.Snippet SDF_SNIPPET = RenderPipelines.WORLD_TEXT_SNIPPET;

    private static final RenderPipeline FLOATING_WORLD_TEXT_SDF = RenderPipelines.register(
            RenderPipeline.builder(SDF_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("baity", "pipeline/floating_world_text_sdf"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("modernui", "core/rendertype_modern_text_sdf_fill"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    private static final Map<Identifier, RenderType> BY_TEXTURE = new HashMap<>();

    private BaityModernUiFloatingTextTypes() {
    }

    public static RenderType getOrCreate(Identifier texture) {
        return BY_TEXTURE.computeIfAbsent(texture, BaityModernUiFloatingTextTypes::create);
    }

    private static RenderType create(Identifier texture) {
        return RenderType.create(
                "baity_floating_world_text_sdf/" + texture.getPath(),
                RenderSetup.builder(FLOATING_WORLD_TEXT_SDF)
                        .withTexture("Sampler0", texture, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                        .useLightmap()
                        .sortOnUpload()
                        .createRenderSetup()
        );
    }
}
