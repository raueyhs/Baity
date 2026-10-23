package com.shyeuar.baity.features.highlights;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.config.DevConfig;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.EntityDrawUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class SafariHighlights implements LevelRenderEvents.AfterSolidFeatures {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String HIDEYHO_NAME = "Hideyho";
    private static final String EXCLUDED_HUNTER_TOKEN = "\"hunter\"";
    private static final String SPARKLING_LABEL = "SPARKLING";

    private static final float SPARKLING_R = 1.0f;
    private static final float SPARKLING_G = 215.0f / 255.0f;
    private static final float SPARKLING_B = 0.0f;

    private static final float MOB_R = ((DevConfig.DEV_PREFIX_COLOR >> 16) & 0xFF) / 255.0f;
    private static final float MOB_G = ((DevConfig.DEV_PREFIX_COLOR >> 8) & 0xFF) / 255.0f;
    private static final float MOB_B = (DevConfig.DEV_PREFIX_COLOR & 0xFF) / 255.0f;

    private static final float HIDEYHO_R = 1.0f;
    private static final float HIDEYHO_G = 105.0f / 255.0f;
    private static final float HIDEYHO_B = 180.0f / 255.0f;

    private static final float NPC_R = 1.0f;
    private static final float NPC_G = 1.0f;
    private static final float NPC_B = 1.0f;

    private static final float FLOOR_FILL_R = 0.7f;
    private static final float FLOOR_FILL_G = 1.0f;
    private static final float FLOOR_FILL_B = 0.0f;
    private static final float FLOOR_FILL_ALPHA = 0.25f;

    private static ClientLevel cachedTargetLevel;
    private static long cachedTargetGameTime = Long.MIN_VALUE;
    private static SafariTargets cachedTargets = new SafariTargets(List.of(), List.of());
    private static ClientLevel beeNestLevel;
    private static final Set<BlockPos> beeNestPositions = new HashSet<>();

    private static final RenderPipeline BAITY_SAFARI_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/baity_safari_lines")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .build()
    );

    private static final RenderType THROUGH_WALLS_LINE = RenderType.create(
            "baity_safari_lines",
            RenderSetup.builder(BAITY_SAFARI_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup()
    );

    private static final RenderPipeline BAITY_SAFARI_FILL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("baity", "pipeline/baity_safari_fill"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build()
    );

    private static final RenderType THROUGH_WALLS_FILL = RenderType.create(
            "baity_safari_fill",
            RenderSetup.builder(BAITY_SAFARI_FILL)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup()
    );

    public SafariHighlights() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(SafariHighlights::onBlockEntityLoad);
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(SafariHighlights::onBlockEntityUnload);
    }

    private static void onBlockEntityLoad(BlockEntity blockEntity, ClientLevel level) {
        ensureBeeNestLevel(level);
        if (blockEntity.getBlockState().is(Blocks.BEE_NEST)) {
            beeNestPositions.add(blockEntity.getBlockPos().immutable());
        }
    }

    private static void onBlockEntityUnload(BlockEntity blockEntity, ClientLevel level) {
        if (beeNestLevel == level) {
            beeNestPositions.remove(blockEntity.getBlockPos());
        }
    }

    private static void ensureBeeNestLevel(ClientLevel level) {
        if (beeNestLevel != level) {
            beeNestLevel = level;
            beeNestPositions.clear();
        }
    }

    @Override
    public void afterSolidFeatures(LevelRenderContext context) {
        if (!ConfigManager.safariRenderTargetESP) return;
        if (MC.level == null) return;
        if (!LocateUtils.isInSafari(MC)) return;
        ensureBeeNestLevel(MC.level);

        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;

        CameraRenderState cameraRenderState = context.levelState().cameraRenderState;
        Vec3 cameraPos = cameraRenderState.pos;
        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        Frustum frustum = MC.gameRenderer.mainCamera().getCullFrustum();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) return;

        SafariZoneUtils.Zone playerZone = SafariZoneUtils.playerZone(MC);
        float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        SafariTargets targets = safariTargets(playerZone);

        Vec3 rayStart = MC.player == null
                ? null
                : MC.player.getEyePosition(partialTick).add(MC.player.getViewVector(partialTick).scale(0.12));

        if (ConfigManager.safariFloorDropEnabled) {
            for (Entity entity : MC.level.entitiesForRendering()) {
                if (!(entity instanceof Display.ItemDisplay itemDisplay) || !isFloorDrop(itemDisplay)) continue;
                BlockPos position = itemDisplay.blockPosition();
                if (!SafariZoneUtils.matchesPlayerZone(playerZone, position.getX(), position.getZ())) continue;
                AABB box = new AABB(
                        position.getX(),
                        position.getY() + 0.5,
                        position.getZ(),
                        position.getX() + 1.0,
                        position.getY() + 1.0,
                        position.getZ() + 1.0
                );
                drawFilledBox(
                        matrices,
                        submits,
                        box,
                        cameraPos,
                        FLOOR_FILL_R,
                        FLOOR_FILL_G,
                        FLOOR_FILL_B,
                        FLOOR_FILL_ALPHA
                );
            }
        }

        if (ConfigManager.safariMobEnabled) {
            for (BlockPos position : beeNestPositions) {
                if (!SafariZoneUtils.matchesPlayerZone(playerZone, position.getX(), position.getZ())) continue;
                AABB box = new AABB(
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        position.getX() + 1.0,
                        position.getY() + 1.0,
                        position.getZ() + 1.0
                );
                if (!frustum.isVisible(box)) continue;
                drawWireBox(matrices, submits, box, cameraPos);
            }
        }

        for (SafariPlayerTarget target : targets.players()) {
            Player player = target.player();
            if (!player.isAlive()) continue;
            if (!dispatcher.shouldRender(player, frustum, cameraPos.x, cameraPos.y, cameraPos.z, partialTick)) continue;
            drawEntityModel(
                    player,
                    partialTick,
                    cameraPos,
                    matrices,
                    submits,
                    cameraRenderState,
                    target.outlineColor()
            );
        }

        for (SafariMobTarget target : targets.mobs()) {
            Mob mob = target.mob();
            if (!mob.isAlive()) continue;
            if (!dispatcher.shouldRender(mob, frustum, cameraPos.x, cameraPos.y, cameraPos.z, partialTick)) continue;
            drawEntityModel(
                    mob,
                    partialTick,
                    cameraPos,
                    matrices,
                    submits,
                    cameraRenderState,
                    mobOutlineColor(target.sparkling())
            );
            if (rayStart != null && target.sparkling()) {
                drawLine(
                        matrices,
                        submits,
                        rayStart,
                        EntityDrawUtils.interpolatedEntityBox(mob, partialTick, 0.01).getCenter(),
                        cameraPos,
                        SPARKLING_R,
                        SPARKLING_G,
                        SPARKLING_B,
                        0.9f
                );
            }
        }
    }

    private static SafariTargets safariTargets(SafariZoneUtils.Zone playerZone) {
        long gameTime = MC.level.getGameTime();
        if (cachedTargetLevel == MC.level && cachedTargetGameTime == gameTime) {
            return cachedTargets;
        }

        List<NamedArmorStand> namedArmorStands = namedArmorStands();
        List<SafariPlayerTarget> players = new ArrayList<>();
        List<SafariMobTarget> mobs = new ArrayList<>();

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != MC.player) {
                if (!player.isAlive()) continue;
                if (!SafariZoneUtils.matchesPlayerZone(playerZone, player.getX(), player.getZ())) continue;

                String name = LocateUtils.toPlainText(
                        player.getDisplayName() != null
                                ? player.getDisplayName().getString()
                                : player.getName().getString()
                );
                boolean isHideyho = ConfigManager.safariHideyhoEnabled && HIDEYHO_NAME.equals(name);
                boolean isOtherNpc = ConfigManager.safariNpcEnabled
                        && (isSafariNpc(name) || hasAssociatedSafariNpcLabel(player, namedArmorStands));
                if (isHideyho || isOtherNpc) {
                    int outlineColor = isHideyho
                            ? ARGB.colorFromFloat(1.0f, HIDEYHO_R, HIDEYHO_G, HIDEYHO_B)
                            : ARGB.colorFromFloat(1.0f, NPC_R, NPC_G, NPC_B);
                    players.add(new SafariPlayerTarget(player, outlineColor));
                }
                continue;
            }

            if (!ConfigManager.safariMobEnabled
                    || !(entity instanceof Mob mob)
                    || !mob.isAlive()
                    || mob instanceof HappyGhast) {
                continue;
            }
            if (!SafariZoneUtils.matchesPlayerZone(playerZone, mob.getX(), mob.getZ())) continue;
            NamedArmorStand associatedArmorStand = findAssociatedArmorStand(mob, namedArmorStands);
            mobs.add(new SafariMobTarget(mob, associatedArmorStand != null && associatedArmorStand.sparkling()));
        }

        cachedTargetLevel = MC.level;
        cachedTargetGameTime = gameTime;
        cachedTargets = new SafariTargets(players, mobs);
        return cachedTargets;
    }

    private static boolean isFloorDrop(Display.ItemDisplay itemDisplay) {
        if (!itemDisplay.getItemStack().is(Items.STRING)) return false;
        BlockPos position = itemDisplay.blockPosition();
        BlockPos above = position.above();
        return !MC.level.getBlockState(above).isCollisionShapeFullBlock(MC.level, above);
    }

    private static boolean isSafariNpc(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return !lowerName.contains(EXCLUDED_HUNTER_TOKEN)
                && (lowerName.contains("hunter") || lowerName.contains("huntress"));
    }

    private static boolean hasAssociatedSafariNpcLabel(Player player, List<NamedArmorStand> armorStands) {
        for (NamedArmorStand namedArmorStand : armorStands) {
            ArmorStand armorStand = namedArmorStand.armorStand();
            double dx = player.getX() - armorStand.getX();
            double dz = player.getZ() - armorStand.getZ();
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > 1.0 || armorStand.getY() + 2.0 < player.getY()) continue;
            if (namedArmorStand.safariNpc()) return true;
        }
        return false;
    }

    private static NamedArmorStand findAssociatedArmorStand(Mob mob, List<NamedArmorStand> armorStands) {
        NamedArmorStand closest = null;
        double closestDistanceSquared = Double.MAX_VALUE;
        for (NamedArmorStand namedArmorStand : armorStands) {
            ArmorStand armorStand = namedArmorStand.armorStand();
            double dx = mob.getX() - armorStand.getX();
            double dz = mob.getZ() - armorStand.getZ();
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > 1.0 || armorStand.getY() + 2.0 < mob.getY()) continue;
            if (distanceSquared < closestDistanceSquared) {
                closest = namedArmorStand;
                closestDistanceSquared = distanceSquared;
            }
        }
        return closest;
    }

    private static int mobOutlineColor(boolean sparkling) {
        if (sparkling) {
            return ARGB.colorFromFloat(1.0f, SPARKLING_R, SPARKLING_G, SPARKLING_B);
        }
        return ARGB.colorFromFloat(1.0f, MOB_R, MOB_G, MOB_B);
    }

    public static boolean isSafariMobOutlineActive() {
        if (!ConfigManager.safariRenderTargetESP
                || MC.level == null
                || !LocateUtils.isInSafari(MC)) {
            return false;
        }
        if (!ConfigManager.safariMobEnabled
                && !ConfigManager.safariHideyhoEnabled
                && !ConfigManager.safariNpcEnabled) {
            return false;
        }
        Module module = ModuleManager.getModuleByName("Highlights");
        return module != null && module.isEnabled();
    }

    private static void drawEntityModel(
            Entity entity,
            float partialTick,
            Vec3 cameraPos,
            PoseStack matrices,
            SubmitNodeCollector submits,
            CameraRenderState cameraRenderState,
            int color
    ) {
        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
        state.isInvisible = false;
        dispatcher.submit(
                state,
                cameraRenderState,
                state.x - cameraPos.x,
                state.y - cameraPos.y,
                state.z - cameraPos.z,
                matrices,
                new SafariMobModelCollector(submits, color)
        );
    }

    private static List<NamedArmorStand> namedArmorStands() {
        List<NamedArmorStand> armorStands = new ArrayList<>();
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand && armorStand.isAlive() && armorStand.hasCustomName()) {
                String name = LocateUtils.toPlainText(armorStand.getName().getString());
                armorStands.add(new NamedArmorStand(
                        armorStand,
                        isSafariNpc(name),
                        name.contains(SPARKLING_LABEL)
                ));
            }
        }
        return armorStands;
    }

    private record SafariPlayerTarget(Player player, int outlineColor) {
    }

    private record SafariMobTarget(Mob mob, boolean sparkling) {
    }

    private record SafariTargets(List<SafariPlayerTarget> players, List<SafariMobTarget> mobs) {
    }

    private record NamedArmorStand(ArmorStand armorStand, boolean safariNpc, boolean sparkling) {
    }

    private static final class SafariMobModelCollector extends SubmitNodeStorage {
        private final SubmitNodeCollector target;
        private final int color;

        private SafariMobModelCollector(SubmitNodeCollector target, int color) {
            this.target = target;
            this.color = color;
        }

        @Override
        public SubmitNodeCollection order(int order) {
            return new SafariMobOrderedCollector(this);
        }

        @Override
        public <S> void submitModel(
                Model<? super S> model,
                S state,
                PoseStack poseStack,
                RenderType renderType,
                int lightCoords,
                int overlayCoords,
                int tintedColor,
                UvMapping uvMapping,
                int ignoredOutlineColor
        ) {
            // TODO(26.3): renderType.outline() targets the second (outline) colour attachment and
            target.submitModel(
                    model,
                    state,
                    poseStack,
                    renderType,
                    lightCoords,
                    overlayCoords,
                    tintedColor,
                    uvMapping,
                    color
            );
        }

        @Override
        public void submitModelPart(
                ModelPart modelPart,
                PoseStack poseStack,
                RenderType renderType,
                int lightCoords,
                int overlayCoords,
                UvMapping uvMapping,
                int tintedColor,
                int ignoredOutlineColor
        ) {
            target.submitModelPart(
                    modelPart,
                    poseStack,
                    renderType,
                    lightCoords,
                    overlayCoords,
                    uvMapping,
                    tintedColor,
                    color
            );
        }

        private static final class SafariMobOrderedCollector extends SubmitNodeCollection {
            private final SafariMobModelCollector parent;

            private SafariMobOrderedCollector(SafariMobModelCollector parent) {
                super(false, new net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase());
                this.parent = parent;
            }

            @Override
            public <S> void submitModel(
                    Model<? super S> model,
                    S state,
                    PoseStack poseStack,
                    RenderType renderType,
                    int lightCoords,
                    int overlayCoords,
                    int tintedColor,
                    UvMapping uvMapping,
                    int ignoredOutlineColor
            ) {
                parent.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, ignoredOutlineColor);
            }

            @Override
            public void submitModelPart(
                    ModelPart modelPart,
                    PoseStack poseStack,
                    RenderType renderType,
                    int lightCoords,
                    int overlayCoords,
                    UvMapping uvMapping,
                    int tintedColor,
                    int ignoredOutlineColor
            ) {
                parent.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, tintedColor, ignoredOutlineColor);
            }
        }
    }
    private static void drawFilledBox(
            PoseStack matrices,
            SubmitNodeCollector submits,
            AABB box,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        submits.submitCustomGeometry(
                matrices,
                THROUGH_WALLS_FILL,
                (pose, fill) -> EntityDrawUtils.drawFilledBoxAtWorld(pose, fill, box, cameraPos, r, g, b, a)
        );
    }

    private static void drawWireBox(
            PoseStack matrices,
            SubmitNodeCollector submits,
            AABB box,
            Vec3 cameraPos
    ) {
        submits.submitCustomGeometry(
                matrices,
                THROUGH_WALLS_LINE,
                (pose, lines) -> EntityDrawUtils.drawWireBoxAtWorld(
                        pose,
                        lines,
                        box,
                        cameraPos,
                        MOB_R,
                        MOB_G,
                        MOB_B,
                        0.9f
                )
        );
    }

    private static void drawLine(
            PoseStack matrices,
            SubmitNodeCollector submits,
            Vec3 start,
            Vec3 end,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        submits.submitCustomGeometry(
                matrices,
                THROUGH_WALLS_LINE,
                (pose, lines) -> EntityDrawUtils.drawLineAtWorld(pose, lines, start, end, cameraPos, r, g, b, a)
        );
    }
}
