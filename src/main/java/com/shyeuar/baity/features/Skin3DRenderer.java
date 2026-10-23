package com.shyeuar.baity.features;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;

public class Skin3DRenderer {

    public static final float HEAD_VOXEL_SIZE = 1.15f;
    public static final float BASE_VOXEL_SIZE = 1.1f;
    public static final float BODY_VOXEL_WIDTH = 1.05f;
    public static final boolean FAST_RENDER = false; 

    private static final double MAX_RENDER_DIST_SQ = 64.0 * 64.0;
    private static final Map<Identifier, CachedSkinData> skinCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_MS = 60000;

    private static final Set<Object> registeredMainModels = Collections.newSetFromMap(new WeakHashMap<>());


    public interface MeshInjector {
        void baity$setInjectedMesh(VoxelMesh mesh, OffsetProvider offset);
        VoxelMesh baity$getInjectedMesh();
        OffsetProvider baity$getOffsetProvider();
    }

    public interface PlayerModelMarker {
        void baity$setIgnored(boolean ignored);
        boolean baity$isIgnored();
    }

    public static void injectMesh(ModelPart part, VoxelMesh mesh, OffsetProvider offset) {
        if ((Object) part instanceof MeshInjector injector) {
            if (injector.baity$getInjectedMesh() == mesh && injector.baity$getOffsetProvider() == offset) {
                return;
            }
            injector.baity$setInjectedMesh(mesh, offset);
        }
    }

    public static void clearInjectedMesh(ModelPart part) {
        if ((Object) part instanceof MeshInjector injector) {
            injector.baity$setInjectedMesh(null, null);
        }
    }

    public static void registerMainModel(Object model) {
        registeredMainModels.add(model);
    }

    public static boolean isRegisteredMainModel(Object model) {
        return registeredMainModels.contains(model);
    }

    public static boolean isEnabled() {
        try {
            Module m = ModuleManager.getModuleByName("3DSkins");
            return m != null && m.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean inRange(AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        var cameraPos = ((com.shyeuar.baity.mixin.accessor.CameraAccessor) mc.gameRenderer.mainCamera()).baity$getPosition();
        double dx = state.x - cameraPos.x;
        double dy = state.y - cameraPos.y;
        double dz = state.z - cameraPos.z;
        return dx * dx + dy * dy + dz * dz <= MAX_RENDER_DIST_SQ;
    }

    public static SkinData getOrCreateSkinData(AvatarRenderState state) {
        if (!isEnabled()) return null;

        PlayerSkin skinTextures = state.skin;
        if (skinTextures == null || skinTextures.body() == null) return null;

        Identifier skinId = skinTextures.body().texturePath();
        if (skinId == null) return null;

        boolean slim = skinTextures.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;

        CachedSkinData cached = skinCache.get(skinId);
        if (cached != null && !cached.isExpired() && cached.slim == slim) {
            SkinData data = new SkinData();
            data.skinId = skinId;
            data.slim = slim;
            data.head = cached.head;
            data.body = cached.body;
            data.leftArm = cached.leftArm;
            data.rightArm = cached.rightArm;
            data.leftLeg = cached.leftLeg;
            data.rightLeg = cached.rightLeg;
            return data;
        }

        NativeImage img = getSkinImage(skinId);
        if (img == null || img.getWidth() != 64 || img.getHeight() != 64) {
            return null;
        }

        SkinData data = new SkinData();
        data.skinId = skinId;
        data.slim = slim;

        data.head = SolidPixelWrapper.create3DMesh(img, 8, 8, 8, 32, 0, false, 0.6f);
        data.body = SolidPixelWrapper.create3DMesh(img, 8, 12, 4, 16, 32, true, 0f);

        int armW = slim ? 3 : 4;
        data.leftArm = SolidPixelWrapper.create3DMesh(img, armW, 12, 4, 48, 48, true, -2f);
        data.rightArm = SolidPixelWrapper.create3DMesh(img, armW, 12, 4, 40, 32, true, -2f);

        data.leftLeg = SolidPixelWrapper.create3DMesh(img, 4, 12, 4, 0, 48, true, 0f);
        data.rightLeg = SolidPixelWrapper.create3DMesh(img, 4, 12, 4, 0, 32, true, 0f);

        skinCache.put(skinId, new CachedSkinData(
                data.head, data.body, data.leftArm, data.rightArm, data.leftLeg, data.rightLeg, slim));

        return data;
    }

    public static void clearCache() {
        skinCache.clear();
    }

    private static NativeImage getSkinImage(Identifier id) {
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture tex = mc.getTextureManager().getTexture(id);

        if (tex instanceof DynamicTexture nativeTexture) {
            return nativeTexture.getPixels();
        }

        return null;
    }

    public static AbstractClientPlayer findPlayerById(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof AbstractClientPlayer player) {
            return player;
        }
        return null;
    }

    public static SkinData getOrCreateSkinDataForPlayer(AbstractClientPlayer player) {
        if (!isEnabled()) return null;

        PlayerSkin skinTextures = player.getSkin();
        if (skinTextures == null || skinTextures.body() == null) return null;

        Identifier skinId = skinTextures.body().texturePath();
        if (skinId == null) return null;

        boolean slim = skinTextures.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;

        CachedSkinData cached = skinCache.get(skinId);
        if (cached != null && !cached.isExpired() && cached.slim == slim) {
            SkinData data = new SkinData();
            data.skinId = skinId;
            data.slim = slim;
            data.head = cached.head;
            data.body = cached.body;
            data.leftArm = cached.leftArm;
            data.rightArm = cached.rightArm;
            data.leftLeg = cached.leftLeg;
            data.rightLeg = cached.rightLeg;
            return data;
        }

        NativeImage img = getSkinImage(skinId);
        if (img == null || img.getWidth() != 64 || img.getHeight() != 64) {
            return null;
        }

        SkinData data = new SkinData();
        data.skinId = skinId;
        data.slim = slim;

        data.head = SolidPixelWrapper.create3DMesh(img, 8, 8, 8, 32, 0, false, 0.6f);
        data.body = SolidPixelWrapper.create3DMesh(img, 8, 12, 4, 16, 32, true, 0f);

        int armW = slim ? 3 : 4;
        data.leftArm = SolidPixelWrapper.create3DMesh(img, armW, 12, 4, 48, 48, true, -2f);
        data.rightArm = SolidPixelWrapper.create3DMesh(img, armW, 12, 4, 40, 32, true, -2f);

        data.leftLeg = SolidPixelWrapper.create3DMesh(img, 4, 12, 4, 0, 48, true, 0f);
        data.rightLeg = SolidPixelWrapper.create3DMesh(img, 4, 12, 4, 0, 32, true, 0f);

        skinCache.put(skinId, new CachedSkinData(
                data.head, data.body, data.leftArm, data.rightArm, data.leftLeg, data.rightLeg, slim));

        return data;
    }

    public enum OffsetProvider {
        HEAD {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.translate(0, -0.25, 0);
                stack.scale(HEAD_VOXEL_SIZE, HEAD_VOXEL_SIZE, HEAD_VOXEL_SIZE);
                stack.translate(0, 0.25, 0);
                stack.translate(0, -0.04, 0);
                mesh.setPosition(0, 0, 0);
            }
        },
        BODY {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BODY_VOXEL_WIDTH, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0, -0.7f, 0);
            }
        },
        LEFT_ARM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0.998f, -0.1f, 0);
            }
        },
        LEFT_ARM_SLIM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0.499f, -0.1f, 0);
            }
        },
        RIGHT_ARM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(-0.998f, -0.1f, 0);
            }
        },
        RIGHT_ARM_SLIM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(-0.499f, -0.1f, 0);
            }
        },
        LEFT_LEG {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0, -0.1f, 0);
            }
        },
        RIGHT_LEG {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0, -0.1f, 0);
            }
        },
        FIRSTPERSON_LEFT_ARM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0.998f, -0.1f, 0);
            }
        },
        FIRSTPERSON_LEFT_ARM_SLIM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(0.499f, -0.1f, 0);
            }
        },
        FIRSTPERSON_RIGHT_ARM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(-0.998f, -0.1f, 0);
            }
        },
        FIRSTPERSON_RIGHT_ARM_SLIM {
            @Override
            public void applyOffset(PoseStack stack, VoxelMesh mesh) {
                stack.scale(BASE_VOXEL_SIZE, 1.035f, BASE_VOXEL_SIZE);
                mesh.setPosition(-0.499f, -0.1f, 0);
            }
        };

        public abstract void applyOffset(PoseStack stack, VoxelMesh mesh);
    }

    public static class SkinData {
        public Identifier skinId;
        public boolean slim;
        public VoxelMesh head, body, leftArm, rightArm, leftLeg, rightLeg;
    }

    private static class CachedSkinData {
        final VoxelMesh head, body, leftArm, rightArm, leftLeg, rightLeg;
        final long timestamp;
        final boolean slim;

        CachedSkinData(VoxelMesh head, VoxelMesh body, VoxelMesh leftArm, VoxelMesh rightArm,
                       VoxelMesh leftLeg, VoxelMesh rightLeg, boolean slim) {
            this.head = head;
            this.body = body;
            this.leftArm = leftArm;
            this.rightArm = rightArm;
            this.leftLeg = leftLeg;
            this.rightLeg = rightLeg;
            this.slim = slim;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }

    public static class VoxelMesh {
        private final float[] polygonData;
        private final int polygonCount;
        private static final int FLOATS_PER_POLYGON = 23;

        public boolean visible = true;
        public float x, y, z;
        public float xRot, yRot, zRot;

        public VoxelMesh(float[] data, int count) {
            this.polygonData = data;
            this.polygonCount = count;
        }

        public void setPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void setRotation(float xRot, float yRot, float zRot) {
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
        }

        public void reset() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.xRot = 0;
            this.yRot = 0;
            this.zRot = 0;
        }

        public void render(PoseStack matrices, VertexConsumer vc, int light, int overlay, int color) {
            if (!visible || polygonCount == 0) return;

            matrices.pushPose();
            translateAndRotate(matrices);
            compile(matrices.last(), vc, light, overlay, color);
            matrices.popPose();
        }

        private void translateAndRotate(PoseStack matrices) {
            if (x != 0 || y != 0 || z != 0) {
                matrices.translate(x / 16.0f, y / 16.0f, z / 16.0f);
            }
            if (xRot != 0 || yRot != 0 || zRot != 0) {
                matrices.rotate(new Quaternionf().rotationZYX(zRot, yRot, xRot));
            }
        }

        private final Vector4f[] tempVec4 = new Vector4f[]{
                new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()
        };
        private final Vector3f tempNormal = new Vector3f();

        private void compile(PoseStack.Pose entry, VertexConsumer vc, int light, int overlay, int color) {
            Matrix4f posMatrix = entry.pose();
            Matrix3f normMatrix = entry.normal();

            for (int i = 0; i < polygonCount; i++) {
                int base = i * FLOATS_PER_POLYGON;

                tempNormal.set(polygonData[base], polygonData[base + 1], polygonData[base + 2]);
                normMatrix.transform(tempNormal);

                for (int v = 0; v < 4; v++) {
                    int off = base + 3 + v * 5;
                    tempVec4[v].set(polygonData[off], polygonData[off + 1], polygonData[off + 2], 1.0f);
                    posMatrix.transform(tempVec4[v]);

                    vc.addVertex(tempVec4[v].x(), tempVec4[v].y(), tempVec4[v].z())
                            .setColor(color)
                            .setUv(polygonData[off + 3], polygonData[off + 4])
                            .setOverlay(overlay)
                            .setLight(light)
                            .setNormal(tempNormal.x(), tempNormal.y(), tempNormal.z());
                }
            }
        }
    }

    public enum Direction {
        DOWN(Axis.Y, 0, -1, 0),
        UP(Axis.Y, 0, 1, 0),
        NORTH(Axis.Z, 0, 0, -1),
        SOUTH(Axis.Z, 0, 0, 1),
        WEST(Axis.X, -1, 0, 0),
        EAST(Axis.X, 1, 0, 0);

        private static final Direction[] OPPOSITES = new Direction[]{UP, DOWN, SOUTH, NORTH, EAST, WEST};

        private final Axis axis;
        public final int nx, ny, nz;

        Direction(Axis axis, int x, int y, int z) {
            this.axis = axis;
            this.nx = x;
            this.ny = y;
            this.nz = z;
        }

        public Direction opposite() {
            return OPPOSITES[this.ordinal()];
        }

        public Axis getAxis() {
            return axis;
        }

        public int getStepX() {
            return nx;
        }

        public int getStepY() {
            return ny;
        }

        public int getStepZ() {
            return nz;
        }

        public int getDirStep() {
            return nx + ny + nz;
        }

        public enum Axis {
            X {
                public int choose(int i, int j, int k) { return i; }
                public double choose(double d, double e, double f) { return d; }
            },
            Y {
                public int choose(int i, int j, int k) { return j; }
                public double choose(double d, double e, double f) { return e; }
            },
            Z {
                public int choose(int i, int j, int k) { return k; }
                public double choose(double d, double e, double f) { return f; }
            };

            public abstract int choose(int i, int j, int k);
            public abstract double choose(double d, double e, double f);
        }
    }

    public static class SolidPixelWrapper {

        private static final float PIXEL_SIZE = 1f;

        public record UV(int u, int v) {}
        public record Dimensions(int width, int height, int depth) {}
        public record Position(float x, float y, float z) {}
        public record VoxelPosition(int x, int y, int z) {}

        public static VoxelMesh create3DMesh(NativeImage img, int width, int height, int depth,
                                              int textureU, int textureV, boolean topPivot, float rotationOffset) {
            List<float[]> polygons = new ArrayList<>();

            float staticXOffset = -width / 2f;
            float staticYOffset = topPivot ? rotationOffset : -height + rotationOffset;
            float staticZOffset = -depth / 2f;
            Position staticOffset = new Position(staticXOffset, staticYOffset, staticZOffset);
            Dimensions dimensions = new Dimensions(width, height, depth);
            UV textureUV = new UV(textureU, textureV);

            try {
                for (Direction face : Direction.values()) {
                    UV sizeUV = getSizeUV(dimensions, face);
                    for (int u = 0; u < sizeUV.u; u++) {
                        for (int v = 0; v < sizeUV.v; v++) {
                            addPixel(img, polygons, staticOffset, face, dimensions, new UV(u, v), textureUV, sizeUV);
                        }
                    }
                }
            } catch (Exception ex) {
                return new VoxelMesh(new float[0], 0);
            }

            float[] data = new float[polygons.size() * 23];
            int idx = 0;
            for (float[] poly : polygons) {
                System.arraycopy(poly, 0, data, idx, 23);
                idx += 23;
            }

            return new VoxelMesh(data, polygons.size());
        }

        private static UV getSizeUV(Dimensions dimensions, Direction face) {
            if (face == Direction.DOWN || face == Direction.UP) {
                return new UV(dimensions.width, dimensions.depth);
            } else if (face == Direction.NORTH || face == Direction.SOUTH) {
                return new UV(dimensions.width, dimensions.height);
            } else {
                return new UV(dimensions.depth, dimensions.height);
            }
        }

        private static UV getOnTextureUV(UV textureUV, UV onFaceUV, Dimensions dimensions, Direction face) {
            if (face == Direction.DOWN) {
                return new UV(textureUV.u + dimensions.depth + onFaceUV.u, textureUV.v + onFaceUV.v);
            } else if (face == Direction.UP) {
                return new UV(textureUV.u + dimensions.width + dimensions.depth + onFaceUV.u, textureUV.v + onFaceUV.v);
            } else if (face == Direction.NORTH) {
                return new UV(textureUV.u + dimensions.depth + onFaceUV.u, textureUV.v + dimensions.depth + onFaceUV.v);
            } else if (face == Direction.SOUTH) {
                return new UV(textureUV.u + dimensions.depth + dimensions.width + dimensions.depth + onFaceUV.u,
                        textureUV.v + dimensions.depth + onFaceUV.v);
            } else if (face == Direction.WEST) {
                return new UV(textureUV.u + onFaceUV.u, textureUV.v + dimensions.depth + onFaceUV.v);
            } else {
                return new UV(textureUV.u + dimensions.depth + dimensions.width + onFaceUV.u,
                        textureUV.v + dimensions.depth + onFaceUV.v);
            }
        }

        private static VoxelPosition UVtoXYZ(UV onFaceUV, Dimensions dimensions, Direction face) {
            if (face == Direction.DOWN) {
                return new VoxelPosition(onFaceUV.u, 0, dimensions.depth - 1 - onFaceUV.v);
            } else if (face == Direction.UP) {
                return new VoxelPosition(onFaceUV.u, dimensions.height - 1, dimensions.depth - 1 - onFaceUV.v);
            } else if (face == Direction.NORTH) {
                return new VoxelPosition(onFaceUV.u, onFaceUV.v, 0);
            } else if (face == Direction.SOUTH) {
                return new VoxelPosition(dimensions.width - 1 - onFaceUV.u, onFaceUV.v, dimensions.depth - 1);
            } else if (face == Direction.WEST) {
                return new VoxelPosition(0, onFaceUV.v, dimensions.depth - 1 - onFaceUV.u);
            } else {
                return new VoxelPosition(dimensions.width - 1, onFaceUV.v, onFaceUV.u);
            }
        }

        private static UV XYZtoUV(VoxelPosition voxelPosition, Dimensions dimensions, Direction face) {
            if (face == Direction.DOWN || face == Direction.UP) {
                return new UV(voxelPosition.x, dimensions.depth - 1 - voxelPosition.z);
            } else if (face == Direction.NORTH) {
                return new UV(voxelPosition.x, voxelPosition.y);
            } else if (face == Direction.SOUTH) {
                return new UV(dimensions.width - 1 - voxelPosition.x, voxelPosition.y);
            } else if (face == Direction.WEST) {
                return new UV(dimensions.depth - 1 - voxelPosition.z, voxelPosition.y);
            } else {
                return new UV(voxelPosition.z, voxelPosition.y);
            }
        }


        private static void addPixel(NativeImage img, List<float[]> polygons, Position staticOffset, Direction face,
                                     Dimensions dimensions, UV onFaceUV, UV textureUV, UV sizeUV) {
            UV onTextureUV = getOnTextureUV(textureUV, onFaceUV, dimensions, face);
            if (!isPresent(img, onTextureUV)) return;

            VoxelPosition voxelPosition = UVtoXYZ(onFaceUV, dimensions, face);
            Position position = new Position(
                    staticOffset.x + voxelPosition.x,
                    staticOffset.y + voxelPosition.y,
                    staticOffset.z + voxelPosition.z
            );
            boolean solidPixel = isSolid(img, onTextureUV);

            Set<Direction> hide = new HashSet<>();
            Set<Direction[]> corners = new HashSet<>();

            boolean isOnBorder = false;
            boolean backsideOverlaps = false;

            for (Direction neighbourFace : Direction.values()) {
                if (neighbourFace.getAxis() == face.getAxis()) continue;

                VoxelPosition neighbourVoxelPosition = new VoxelPosition(
                        voxelPosition.x + neighbourFace.getStepX(),
                        voxelPosition.y + neighbourFace.getStepY(),
                        voxelPosition.z + neighbourFace.getStepZ()
                );
                UV neighbourOnFaceUV = XYZtoUV(neighbourVoxelPosition, dimensions, face);

                if (isOnFace(neighbourOnFaceUV, sizeUV)) {
                    if (isPresent(img, getOnTextureUV(textureUV, neighbourOnFaceUV, dimensions, face))) {
                        if (!(solidPixel && !isSolid(img, getOnTextureUV(textureUV, neighbourOnFaceUV, dimensions, face)))) {
                            hide.add(neighbourFace);
                        }
                    } else {
                        VoxelPosition farNeighbourVoxelPosition = new VoxelPosition(
                                neighbourVoxelPosition.x + neighbourFace.getStepX(),
                                neighbourVoxelPosition.y + neighbourFace.getStepY(),
                                neighbourVoxelPosition.z + neighbourFace.getStepZ()
                        );
                        UV farNeighbourOnFaceUV = XYZtoUV(farNeighbourVoxelPosition, dimensions, face);
                        if (!isOnFace(farNeighbourOnFaceUV, sizeUV)) {
                            farNeighbourOnFaceUV = XYZtoUV(farNeighbourVoxelPosition, dimensions, neighbourFace);
                            if (isPresent(img, getOnTextureUV(textureUV, farNeighbourOnFaceUV, dimensions, neighbourFace))) {
                                if (!(solidPixel && !isSolid(img, getOnTextureUV(textureUV, farNeighbourOnFaceUV, dimensions, neighbourFace)))) {
                                    hide.add(neighbourFace);
                                }
                            }
                        }
                    }
                } else {
                    isOnBorder = true;
                    neighbourOnFaceUV = XYZtoUV(voxelPosition, dimensions, neighbourFace);
                    if (isPresent(img, getOnTextureUV(textureUV, neighbourOnFaceUV, dimensions, neighbourFace))) {
                        backsideOverlaps = true;
                        hide.add(neighbourFace);
                        corners.add(new Direction[]{face.opposite(), neighbourFace});
                    } else {
                        UV downNeighbourOnFaceUV = XYZtoUV(new VoxelPosition(
                                voxelPosition.x - face.getStepX(),
                                voxelPosition.y - face.getStepY(),
                                voxelPosition.z - face.getStepZ()
                        ), dimensions, neighbourFace);
                        if (isPresent(img, getOnTextureUV(textureUV, downNeighbourOnFaceUV, dimensions, neighbourFace))) {
                            backsideOverlaps = true;
                        }
                    }
                }
            }

            if (!isOnBorder || backsideOverlaps) {
                hide.add(face.opposite());
            }
            if (FAST_RENDER) {
                hide.add(face);
            }

            addBox(polygons, position.x, position.y, position.z, PIXEL_SIZE,
                    onTextureUV.u, onTextureUV.v, img.getWidth(), img.getHeight(),
                    hide, corners);
        }

        private static boolean isOnFace(UV onFaceUV, UV sizeUV) {
            return onFaceUV.u >= 0 && onFaceUV.u < sizeUV.u && onFaceUV.v >= 0 && onFaceUV.v < sizeUV.v;
        }

        private static boolean isPresent(NativeImage img, UV uv) {
            if (uv.u < 0 || uv.u >= img.getWidth() || uv.v < 0 || uv.v >= img.getHeight()) return false;
            
            int alpha = img.getLuminanceOrAlpha(uv.u, uv.v);
            return alpha != 0;
        }

        private static boolean isSolid(NativeImage img, UV uv) {
            if (uv.u < 0 || uv.u >= img.getWidth() || uv.v < 0 || uv.v >= img.getHeight()) return false;
            int alpha = img.getLuminanceOrAlpha(uv.u, uv.v);
            return alpha == 255;
        }


        private static void addBox(List<float[]> polygons, float x, float y, float z, float pixelSize,
                                   int texU, int texV, int texW, int texH,
                                   Set<Direction> hide, Set<Direction[]> corners) {
            float pX = x + pixelSize;
            float pY = y + pixelSize;
            float pZ = z + pixelSize;

            float[][] vertices = {
                    {x, y, z},      
                    {pX, y, z},     
                    {pX, pY, z},   
                    {x, pY, z},     
                    {x, y, pZ},    
                    {pX, y, pZ},  
                    {pX, pY, pZ},  
                    {x, pY, pZ}   
            };

            float minU = (float) texU / texW;
            float maxU = (float) (texU + 1) / texW;
            float minV = (float) texV / texH;
            float maxV = (float) (texV + 1) / texH;

            Map<Direction.Axis, Direction[]> axisToCorner = new HashMap<>();
            for (Direction[] corner : corners) {
                nextAxis:
                for (Direction.Axis axis : Direction.Axis.values()) {
                    for (Direction dir : corner) {
                        if (dir.getAxis() == axis) continue nextAxis;
                    }
                    axisToCorner.put(axis, corner);
                    break;
                }
            }

            if (!hide.contains(Direction.DOWN)) {
                int[] indices = removeCornerVertex(new int[]{5, 4, 0, 1}, vertices, axisToCorner.get(Direction.Axis.Y));
                polygons.add(createPolygon(vertices, indices, Direction.DOWN, minU, minV, maxU, maxV));
            }
            if (!hide.contains(Direction.UP)) {
                int[] indices = removeCornerVertex(new int[]{2, 3, 7, 6}, vertices, axisToCorner.get(Direction.Axis.Y));
                polygons.add(createPolygon(vertices, indices, Direction.UP, minU, minV, maxU, maxV));
            }
            if (!hide.contains(Direction.NORTH)) {
                int[] indices = removeCornerVertex(new int[]{1, 0, 3, 2}, vertices, axisToCorner.get(Direction.Axis.Z));
                polygons.add(createPolygon(vertices, indices, Direction.NORTH, minU, minV, maxU, maxV));
            }
            if (!hide.contains(Direction.SOUTH)) {
                int[] indices = removeCornerVertex(new int[]{4, 5, 6, 7}, vertices, axisToCorner.get(Direction.Axis.Z));
                polygons.add(createPolygon(vertices, indices, Direction.SOUTH, minU, minV, maxU, maxV));
            }
            if (!hide.contains(Direction.WEST)) {
                int[] indices = removeCornerVertex(new int[]{0, 4, 7, 3}, vertices, axisToCorner.get(Direction.Axis.X));
                polygons.add(createPolygon(vertices, indices, Direction.WEST, minU, minV, maxU, maxV));
            }
            if (!hide.contains(Direction.EAST)) {
                int[] indices = removeCornerVertex(new int[]{5, 1, 2, 6}, vertices, axisToCorner.get(Direction.Axis.X));
                polygons.add(createPolygon(vertices, indices, Direction.EAST, minU, minV, maxU, maxV));
            }
        }

        private static int[] removeCornerVertex(int[] indices, float[][] vertices, Direction[] corner) {
            if (corner == null) return indices;

            int exceptIdx = indices[0];
            for (int i = 1; i < 4; i++) {
                exceptIdx = compareVertices(exceptIdx, indices[i], vertices, corner);
            }

            int[] result = new int[4];
            int idx = 0;
            for (int i = 0; i < 4; i++) {
                if (indices[i] != exceptIdx) {
                    result[idx++] = indices[i];
                }
            }
            result[3] = result[2]; 
            return result;
        }

        private static int compareVertices(int idx1, int idx2, float[][] vertices, Direction[] corner) {
            float[] v1 = vertices[idx1];
            float[] v2 = vertices[idx2];
            for (Direction dir : corner) {
                double d = dir.getAxis().choose(v1[0] - v2[0], v1[1] - v2[1], v1[2] - v2[2]) * dir.getDirStep();
                if (d > 0) return idx1;
                else if (d < 0) return idx2;
            }
            return idx1;
        }

        private static float[] createPolygon(float[][] vertices, int[] indices, Direction dir,
                                             float minU, float minV, float maxU, float maxV) {
            float[] data = new float[23];

            data[0] = dir.nx;
            data[1] = dir.ny;
            data[2] = dir.nz;

            float[] us = {maxU, minU, minU, maxU};
            float[] vs = {minV, minV, maxV, maxV};

            for (int i = 0; i < 4; i++) {
                float[] v = vertices[indices[i]];
                int off = 3 + i * 5;
                data[off] = v[0] / 16f;
                data[off + 1] = v[1] / 16f;
                data[off + 2] = v[2] / 16f;
                data[off + 3] = us[i];
                data[off + 4] = vs[i];
            }

            return data;
        }
    }
}
