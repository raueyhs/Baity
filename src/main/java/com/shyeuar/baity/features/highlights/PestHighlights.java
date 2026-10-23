package com.shyeuar.baity.features.highlights;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.EntityDrawUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class PestHighlights implements LevelRenderEvents.AfterTranslucentFeatures {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final int PEST_COLOR_ARGB = 0xFFEE82EE;
    private static final float FILL_FACE_ALPHA = 0.25f;

    private static final double WORM_LINK_INFLATE_XZ = 0.85;
    private static final double WORM_LINK_INFLATE_Y = 0.75;
    private static final double LINE_MERGE_MAX_CENTER_DISTANCE = 1.0;
    private static final double BOX_VERTICAL_OFFSET_BLOCKS = 1.0;
    private static final double EARTHWORM_HULL_SHRINK = 0.8;

    private static final RenderPipeline BAITY_PEST_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/baity_pest_lines")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .build()
    );

    private static final RenderType NO_DEPTH_LINES = RenderType.create(
            "baity_pest_lines",
            RenderSetup.builder(BAITY_PEST_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup()
    );

    private static final RenderPipeline BAITY_PEST_FILL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("baity", "pipeline/baity_pest_fill"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build()
    );

    private static final RenderType NO_DEPTH_FILL = RenderType.create(
            "baity_pest_fill",
            RenderSetup.builder(BAITY_PEST_FILL)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup()
    );

    private record HeadBoxDims(double centerYFromFeet, double halfXz, double halfY) {}

    private record WormPiece(AABB box) {}

    private record NonWormPiece(AABB box, String texture) {}

    private record LineEndpoint(Vec3 target) {}

    private record TaggedHull(AABB box) {}

    private record PestHighlightLayout(List<TaggedHull> drawHulls, List<LineEndpoint> lineEndpoints) {}

    public static void tickPestCaches() {
        if (MC.level == null || MC.player == null) {
            return;
        }
        if (!pestHighlightEnabled()) {
            return;
        }
        PestEntityRegistry.pruneStale();
        PestEntityRegistry.tickThrottledRescanGardenPests();
    }

    private static boolean pestHighlightEnabled() {
        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) {
            return false;
        }
        if (!ConfigManager.highlightsPestEnabled) {
            return false;
        }
        return LocateUtils.isOwnGarden(MC);
    }

    private static boolean slimPestTexture(String texture) {
        return texture.equals(PestEntityRegistry.TEXTURE_DRAGONFLY)
                || texture.equals(PestEntityRegistry.TEXTURE_FIELD_MOUSE)
                || texture.equals(PestEntityRegistry.TEXTURE_FIREFLY)
                || texture.equals(PestEntityRegistry.TEXTURE_FIREFLY_FLASH)
                || texture.equals(PestEntityRegistry.TEXTURE_LUNAR_MOTH)
                || texture.equals(PestEntityRegistry.TEXTURE_PRAYING_MANTIS)
                || texture.equals(PestEntityRegistry.TEXTURE_RAT);
    }

    private static HeadBoxDims headDimsFor(String texture, boolean wormSegment) {
        if (wormSegment) {
            return new HeadBoxDims(1.38, 0.37, 0.36);
        }
        if (slimPestTexture(texture)) {
            return new HeadBoxDims(1.41, 0.235, 0.325);
        }
        return new HeadBoxDims(1.40, 0.27, 0.31);
    }

    private static Vec3 pestHeadBoxCenter(ArmorStand stand, float partialTick, HeadBoxDims d) {
        Vec3 p = stand.getPosition(partialTick);
        double cy = p.y + d.centerYFromFeet();
        return new Vec3(p.x, cy, p.z);
    }

    private static AABB pestHighlightBox(ArmorStand stand, float partialTick, HeadBoxDims d) {
        Vec3 c = pestHeadBoxCenter(stand, partialTick, d);
        return headBoxAroundCenter(c, d.halfXz(), d.halfY());
    }

    @Override
    public void afterTranslucentFeatures(LevelRenderContext context) {
        if (!pestHighlightEnabled()) {
            return;
        }
        if (MC.level == null || MC.player == null) {
            return;
        }

        Map<Integer, String> pests = PestEntityRegistry.snapshotTrackedPests();
        if (pests.isEmpty()) {
            return;
        }

        float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PestHighlightLayout layout = buildHighlightLayout(pests, partialTick);
        List<TaggedHull> hulls = layout.drawHulls();
        if (hulls.isEmpty()) {
            return;
        }

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();
        var submits = context.submitNodeCollector();
        if (matrices == null || submits == null) {
            return;
        }

        float[] rgba = pestColorRgba();

        submits.submitCustomGeometry(matrices, NO_DEPTH_FILL, (pose, fill) -> {
            for (TaggedHull th : hulls) {
                drawHullFaces(pose, fill, th.box(), cameraPos, rgba, FILL_FACE_ALPHA);
            }
        });

        Vec3 lineStart = null;
        if (ConfigManager.highlightsPestDrawLineEnabled) {
            Player player = MC.player;
            if (player != null) {
                Vec3 eye = player.getEyePosition(partialTick);
                Vec3 look = player.getViewVector(partialTick);
                lineStart = eye.add(look.scale(0.12));
            }
        }
        Vec3 finalLineStart = lineStart;
        submits.submitCustomGeometry(matrices, NO_DEPTH_LINES, (pose, lines) -> {
            for (TaggedHull th : hulls) {
                EntityDrawUtils.drawWireBoxAtWorld(pose, lines, th.box(), cameraPos, rgba[0], rgba[1], rgba[2], rgba[3]);
            }
            if (finalLineStart != null) {
                for (LineEndpoint le : layout.lineEndpoints()) {
                    EntityDrawUtils.drawLineAtWorld(
                            pose,
                            lines,
                            finalLineStart,
                            le.target(),
                            cameraPos,
                            rgba[0],
                            rgba[1],
                            rgba[2],
                            rgba[3]
                    );
                }
            }
        });
    }

    private static PestHighlightLayout buildHighlightLayout(Map<Integer, String> pests, float partialTick) {
        List<WormPiece> wormPieces = new ArrayList<>();
        List<NonWormPiece> nonWorm = new ArrayList<>();

        for (Map.Entry<Integer, String> e : pests.entrySet()) {
            Entity entity = MC.level.getEntity(e.getKey());
            if (!(entity instanceof ArmorStand stand) || !stand.isAlive()) {
                continue;
            }
            String texture = e.getValue();
            boolean wormPiece = isEarthwormPieceTexture(texture);
            HeadBoxDims dims = headDimsFor(texture, wormPiece);
            AABB box = pestHighlightBox(stand, partialTick, dims);
            if (wormPiece) {
                wormPieces.add(new WormPiece(box));
            } else {
                nonWorm.add(new NonWormPiece(box, texture));
            }
        }

        List<TaggedHull> display = new ArrayList<>();
        List<LineEndpoint> lineCandidates = new ArrayList<>();

        for (NonWormPiece p : nonWorm) {
            AABB hull = cubeMaxEdgeYOffset(p.box(), BOX_VERTICAL_OFFSET_BLOCKS);
            hull = shiftBoxDownByOwnHeight(hull);
            display.add(new TaggedHull(hull));
            lineCandidates.add(new LineEndpoint(hull.getCenter()));
        }

        List<List<WormPiece>> wormClusters = clusterWormPieces(wormPieces);
        for (List<WormPiece> cluster : wormClusters) {
            double maxHalf = 0.0;
            for (WormPiece w : cluster) {
                AABB b = w.box();
                double half = Math.max(Math.max(b.getXsize(), b.getYsize()), b.getZsize()) / 2.0;
                maxHalf = Math.max(maxHalf, half);
            }
            for (WormPiece w : cluster) {
                Vec3 c = w.box().getCenter().add(0.0, BOX_VERTICAL_OFFSET_BLOCKS, 0.0);
                AABB hull = shiftBoxDownByOwnHeight(cubeAroundCenterHalf(c, maxHalf));
                hull = hull.move(0.0, -hull.getYsize(), 0.0);
                hull = shrinkAabbFromCenter(hull, EARTHWORM_HULL_SHRINK);
                display.add(new TaggedHull(hull));
                lineCandidates.add(new LineEndpoint(hull.getCenter()));
            }
        }

        return new PestHighlightLayout(display, collapseLineEndpointsWithinOneBlock(lineCandidates));
    }

    private static AABB shiftBoxDownByOwnHeight(AABB box) {
        double h = box.getYsize();
        return box.move(0.0, -h, 0.0);
    }

    private static AABB shrinkAabbFromCenter(AABB box, double scale) {
        Vec3 center = box.getCenter();
        double hx = box.getXsize() * 0.5 * scale;
        double hy = box.getYsize() * 0.5 * scale;
        double hz = box.getZsize() * 0.5 * scale;
        return new AABB(
                center.x - hx,
                center.y - hy,
                center.z - hz,
                center.x + hx,
                center.y + hy,
                center.z + hz
        );
    }

    private static AABB cubeMaxEdgeYOffset(AABB box, double yOffset) {
        double dx = box.maxX - box.minX;
        double dy = box.maxY - box.minY;
        double dz = box.maxZ - box.minZ;
        double s = Math.max(dx, Math.max(dy, dz));
        double half = s / 2.0;
        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5 + yOffset;
        double cz = (box.minZ + box.maxZ) * 0.5;
        return new AABB(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half);
    }

    private static AABB cubeAroundCenterHalf(Vec3 c, double half) {
        return new AABB(c.x - half, c.y - half, c.z - half, c.x + half, c.y + half, c.z + half);
    }

    private static List<LineEndpoint> collapseLineEndpointsWithinOneBlock(List<LineEndpoint> endpoints) {
        int n = endpoints.size();
        if (n <= 1) {
            return n == 0 ? List.of() : List.copyOf(endpoints);
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            Vec3 vi = endpoints.get(i).target();
            for (int j = i + 1; j < n; j++) {
                if (vi.distanceTo(endpoints.get(j).target()) <= LINE_MERGE_MAX_CENTER_DISTANCE + 1e-9) {
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int r = find(parent, i);
            groups.computeIfAbsent(r, k -> new ArrayList<>()).add(i);
        }
        List<LineEndpoint> out = new ArrayList<>();
        for (List<Integer> idx : groups.values()) {
            double cx = 0.0;
            double cy = 0.0;
            double cz = 0.0;
            for (int i : idx) {
                Vec3 t = endpoints.get(i).target();
                cx += t.x;
                cy += t.y;
                cz += t.z;
            }
            int m = idx.size();
            Vec3 centroid = new Vec3(cx / m, cy / m, cz / m);
            int bestIdx = idx.getFirst();
            double bestD2 = endpoints.get(bestIdx).target().distanceToSqr(centroid);
            for (int k = 1; k < idx.size(); k++) {
                int ii = idx.get(k);
                double d2 = endpoints.get(ii).target().distanceToSqr(centroid);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    bestIdx = ii;
                }
            }
            LineEndpoint pick = endpoints.get(bestIdx);
            out.add(new LineEndpoint(pick.target()));
        }
        return out;
    }

    private static boolean isEarthwormPieceTexture(String texture) {
        return PestEntityRegistry.EARTHWORM_SEGMENT_TEXTURE.equals(texture)
                || PestEntityRegistry.TEXTURE_EARTHWORM_TAIL.equals(texture);
    }

    private static List<List<WormPiece>> clusterWormPieces(List<WormPiece> pieces) {
        int n = pieces.size();
        if (n == 0) {
            return List.of();
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (wormPiecesLinked(pieces.get(i).box, pieces.get(j).box)) {
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, List<WormPiece>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int r = find(parent, i);
            clusters.computeIfAbsent(r, k -> new ArrayList<>()).add(pieces.get(i));
        }
        return new ArrayList<>(clusters.values());
    }

    private static int find(int[] parent, int i) {
        if (parent[i] != i) {
            parent[i] = find(parent, parent[i]);
        }
        return parent[i];
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[rb] = ra;
        }
    }

    private static boolean wormPiecesLinked(AABB a, AABB b) {
        AABB expanded = new AABB(
                a.minX - WORM_LINK_INFLATE_XZ,
                a.minY - WORM_LINK_INFLATE_Y,
                a.minZ - WORM_LINK_INFLATE_XZ,
                a.maxX + WORM_LINK_INFLATE_XZ,
                a.maxY + WORM_LINK_INFLATE_Y,
                a.maxZ + WORM_LINK_INFLATE_XZ
        );
        return expanded.intersects(b);
    }

    private static AABB headBoxAroundCenter(Vec3 c, double halfXz, double halfY) {
        return new AABB(
                c.x - halfXz,
                c.y - halfY,
                c.z - halfXz,
                c.x + halfXz,
                c.y + halfY,
                c.z + halfXz
        );
    }

    private static void drawHullFaces(
            PoseStack.Pose pose,
            VertexConsumer fill,
            AABB hull,
            Vec3 cameraPos,
            float[] rgba,
            float alpha
    ) {
        double x1 = hull.minX - cameraPos.x;
        double y1 = hull.minY - cameraPos.y;
        double z1 = hull.minZ - cameraPos.z;
        double x2 = hull.maxX - cameraPos.x;
        double y2 = hull.maxY - cameraPos.y;
        double z2 = hull.maxZ - cameraPos.z;
        drawFilledBoxFaces(pose, fill, x1, y1, z1, x2, y2, z2, rgba[0], rgba[1], rgba[2], alpha);
    }

    private static void drawFilledBoxFaces(
            PoseStack.Pose pose,
            VertexConsumer vc,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float r,
            float g,
            float b,
            float a
    ) {
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z1, (float) x2, (float) y1, (float) z1, (float) x2, (float) y1, (float) z2, (float) x1, (float) y1, (float) z2,
                r, g, b, a, 0f, -1f, 0f);
        drawQuad(pose, vc, (float) x1, (float) y2, (float) z1, (float) x1, (float) y2, (float) z2, (float) x2, (float) y2, (float) z2, (float) x2, (float) y2, (float) z1,
                r, g, b, a, 0f, 1f, 0f);
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z1, (float) x1, (float) y1, (float) z2, (float) x1, (float) y2, (float) z2, (float) x1, (float) y2, (float) z1,
                r, g, b, a, -1f, 0f, 0f);
        drawQuad(pose, vc, (float) x2, (float) y1, (float) z2, (float) x2, (float) y1, (float) z1, (float) x2, (float) y2, (float) z1, (float) x2, (float) y2, (float) z2,
                r, g, b, a, 1f, 0f, 0f);
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z1, (float) x2, (float) y1, (float) z1, (float) x2, (float) y2, (float) z1, (float) x1, (float) y2, (float) z1,
                r, g, b, a, 0f, 0f, -1f);
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z2, (float) x1, (float) y2, (float) z2, (float) x2, (float) y2, (float) z2, (float) x2, (float) y1, (float) z2,
                r, g, b, a, 0f, 0f, 1f);
    }

    private static float[] pestColorRgba() {
        int c = PEST_COLOR_ARGB;
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;
        return new float[]{r, g, b, 0.9f};
    }

    private static void drawQuad(
            PoseStack.Pose pose,
            VertexConsumer vc,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            float r,
            float g,
            float b,
            float a,
            float nx,
            float ny,
            float nz
    ) {
        vc.addVertex(pose.pose(), ax, ay, az).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), bx, by, bz).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), cx, cy, cz).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), dx, dy, dz).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }
}
