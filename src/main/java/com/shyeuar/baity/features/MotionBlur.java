package com.shyeuar.baity.features;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.PostRenderAccessors.PostChainAccessor;
import com.shyeuar.baity.mixin.accessor.PostRenderAccessors.PostPassAccessor;
import com.shyeuar.baity.mixin.accessor.PostRenderAccessors.ShaderManagerAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class MotionBlur {

    private static final Identifier VELOCITY_PRE = Identifier.fromNamespaceAndPath("baity", "velocity_pre");
    private static final Identifier VELOCITY_F5 = Identifier.fromNamespaceAndPath("baity", "velocity_f5");
    private static final Identifier VELOCITY_POST = Identifier.fromNamespaceAndPath("baity", "velocity_post");

    private static final int UBO_SIZE = 304;
    private static final int UBO_USAGE = 130;
    private static Method createBufferMethod;

    private static final Matrix4f prevModelView = new Matrix4f();
    private static final Matrix4f prevProjection = new Matrix4f();
    private static final Matrix4f scratchModelView = new Matrix4f();
    private static final Matrix4f scratchProjection = new Matrix4f();
    private static final Matrix4f mvInverse = new Matrix4f();
    private static final Matrix4f projInverse = new Matrix4f();
    private static final Matrix4f uboPrevModelView = new Matrix4f();
    private static final Matrix4f uboPrevProjection = new Matrix4f();

    private static double prevCamX;
    private static double prevCamY;
    private static double prevCamZ;
    private static float cameraDx;
    private static float cameraDy;
    private static float cameraDz;
    private static boolean previousFrameReady;

    private static long frameLastNano;
    private static float frameFps;
    private static long monitorLastHandle;
    private static int monitorRefreshRate = 60;
    private static long monitorLastCheckNano;

    private static GraphicsResourceAllocator frameAllocator;
    private static final Set<String> loadErrorLogged = new HashSet<>();
    private static final UniformBuffer preEntityUbo = new UniformBuffer("PreEntityBlurUniforms", UBO_SIZE);
    private static final UniformBuffer postRenderUbo = new UniformBuffer("PostRenderBlurUniforms", UBO_SIZE);

    private MotionBlur() {
    }

    public static boolean isActive() {
        Module module = ModuleManager.getModuleByName("MotionBlur");
        return module != null
                && module.isEnabled()
                && ConfigManager.motionBlurEnabled
                && ConfigManager.motionBlurStrength != 0.0f;
    }

    public static void onRenderHead(GraphicsResourceAllocator allocator, Matrix4fc modelView, Matrix4fc projection,
                                    double camX, double camY, double camZ) {
        if (!isActive()) {
            clearFrameAllocator();
            rememberFrame(modelView, projection, camX, camY, camZ);
            return;
        }

        frameAllocator = allocator;
        tickFrameTimer();

        scratchModelView.set(modelView);
        scratchProjection.set(projection);

        if (!previousFrameReady) {
            updateCameraUniforms(scratchModelView, scratchModelView, scratchProjection, scratchProjection, 0.0f, 0.0f, 0.0f);
            rememberFrame(scratchModelView, scratchProjection, camX, camY, camZ);
            return;
        }

        updateCameraUniforms(
                scratchModelView, prevModelView, scratchProjection, prevProjection,
                (float) (camX - prevCamX), (float) (camY - prevCamY), (float) (camZ - prevCamZ));
        rememberFrame(scratchModelView, scratchProjection, camX, camY, camZ);
    }

    public static void onBeforeEntities() {
        applyBlur(useSinglePassBlur() ? VELOCITY_F5 : VELOCITY_PRE, preEntityUbo);
    }

    public static void onAfterLevel() {
        if (!useSinglePassBlur()) {
            applyBlur(VELOCITY_POST, postRenderUbo);
        }
    }

    public static void clearFrameAllocator() {
        frameAllocator = null;
    }

    private static void applyBlur(Identifier shaderId, UniformBuffer ubo) {
        if (frameAllocator == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        PostChain chain = loadChain(client, shaderId);
        if (chain == null) {
            return;
        }

        float strength = ConfigManager.motionBlurStrength;
        float fpsOverRefresh = monitorRefreshRate > 0 ? frameFps / monitorRefreshRate : 1.0f;
        if (fpsOverRefresh < 1.0f) {
            fpsOverRefresh = 1.0f;
        }
        float blendFactor = strength * fpsOverRefresh;
        int sampleAmount = fpsOverRefresh > 1.0f ? (int) (100 * fpsOverRefresh) : 100;
        float viewW = client.getMainRenderTarget().width;
        float viewH = client.getMainRenderTarget().height;

        List<PostPass> passes = ((PostChainAccessor) chain).baity$getPasses();
        if (passes.isEmpty()) {
            return;
        }

        Map<String, GpuBuffer> uniformBuffers = ((PostPassAccessor) passes.getFirst()).baity$getCustomUniforms();
        if (!uniformBuffers.containsKey(ubo.uniformName)) {
            return;
        }

        GpuBuffer gpuBuffer = ubo.bind(chain, uniformBuffers);
        try {
            try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(gpuBuffer, false, true)) {
                Std140Builder builder = Std140Builder.intoBuffer(view.data());
                builder.putMat4f(mvInverse);
                builder.putMat4f(projInverse);
                builder.putMat4f(uboPrevModelView);
                builder.putMat4f(uboPrevProjection);
                builder.putVec3(cameraDx, cameraDy, cameraDz);
                builder.putVec2(viewW, viewH);
                builder.putFloat(blendFactor);
                builder.putInt(sampleAmount);
                builder.putInt(0);
                builder.putInt(1);
            }
            chain.process(client.getMainRenderTarget(), frameAllocator);
        } catch (RuntimeException e) {
            if (ubo.resetIfClosed(e)) {
                return;
            }
            throw e;
        }
    }

    private static PostChain loadChain(Minecraft client, Identifier id) {
        try {
            ShaderManager.CompilationCache cache =
                    ((ShaderManagerAccessor) client.getShaderManager()).baity$getCompilationCache();
            if (cache == null) {
                return null;
            }
            PostChain chain = cache.getOrLoadPostChain(id, LevelTargetBundle.MAIN_TARGETS);
            loadErrorLogged.remove(id.getPath());
            return chain;
        } catch (Exception e) {
            if (loadErrorLogged.add(id.getPath())) {
                System.err.println("[Baity] Failed to load motion blur shader " + id.getPath() + ": " + e.getMessage());
            }
            return null;
        }
    }

    private static void rememberFrame(Matrix4fc modelView, Matrix4fc projection, double camX, double camY, double camZ) {
        prevModelView.set(modelView);
        prevProjection.set(projection);
        prevCamX = camX;
        prevCamY = camY;
        prevCamZ = camZ;
        previousFrameReady = true;
    }

    private static void updateCameraUniforms(Matrix4f modelView, Matrix4f previousModelView,
                                             Matrix4f projection, Matrix4f previousProjection,
                                             float dx, float dy, float dz) {
        modelView.invert(mvInverse);
        projection.invert(projInverse);
        uboPrevModelView.set(previousModelView);
        uboPrevProjection.set(previousProjection);
        cameraDx = dx;
        cameraDy = dy;
        cameraDz = dz;
    }

    private static boolean useSinglePassBlur() {
        Minecraft client = Minecraft.getInstance();
        if (client.options.getCameraType() != CameraType.FIRST_PERSON) {
            return true;
        }
        return client.player != null && client.player.isPassenger();
    }

    private static void tickFrameTimer() {
        long now = System.nanoTime();
        float delta = (now - frameLastNano) / 1_000_000_000.0f;
        frameLastNano = now;
        frameFps = delta > 0.0f && delta < 1.0f ? 1.0f / delta : 0.0f;

        if (now - monitorLastCheckNano < 1_000_000_000L) {
            return;
        }
        monitorLastCheckNano = now;

        Minecraft client = Minecraft.getInstance();
        long window = client.getWindow().handle();
        long monitor = GLFW.glfwGetWindowMonitor(window);
        if (monitor == 0) {
            int[] winX = new int[1];
            int[] winY = new int[1];
            GLFW.glfwGetWindowPos(window, winX, winY);
            int centerX = winX[0] + client.getWindow().getScreenWidth() / 2;
            int centerY = winY[0] + client.getWindow().getScreenHeight() / 2;
            monitor = GLFW.glfwGetPrimaryMonitor();
            PointerBuffer monitors = GLFW.glfwGetMonitors();
            if (monitors != null) {
                for (int i = 0; i < monitors.limit(); i++) {
                    long candidate = monitors.get(i);
                    int[] monitorX = new int[1];
                    int[] monitorY = new int[1];
                    GLFW.glfwGetMonitorPos(candidate, monitorX, monitorY);
                    GLFWVidMode mode = GLFW.glfwGetVideoMode(candidate);
                    if (mode == null) {
                        continue;
                    }
                    if (centerX >= monitorX[0] && centerX < monitorX[0] + mode.width()
                            && centerY >= monitorY[0] && centerY < monitorY[0] + mode.height()) {
                        monitor = candidate;
                        break;
                    }
                }
            }
        }
        if (monitor != monitorLastHandle) {
            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
            monitorRefreshRate = vidMode != null ? vidMode.refreshRate() : 60;
            monitorLastHandle = monitor;
        }
    }

    private static GpuBuffer createUbo(String name, int sizeBytes) {
        Object device = RenderSystem.getDevice();
        Supplier<String> label = () -> "baity:motion_blur:" + name;
        try {
            if (createBufferMethod == null) {
                createBufferMethod = device.getClass().getMethod("createBuffer", Supplier.class, int.class, long.class);
            }
            return (GpuBuffer) createBufferMethod.invoke(device, label, UBO_USAGE, (long) sizeBytes);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[Baity] MotionBlur UBO creation failed", e);
        }
    }

    private static void closeQuietly(GpuBuffer buffer) {
        if (buffer == null) {
            return;
        }
        try {
            buffer.close();
        } catch (RuntimeException ignored) {
        }
    }

    private static final class UniformBuffer {
        private final String uniformName;
        private final int sizeBytes;
        private PostChain owner;
        private GpuBuffer buffer;

        private UniformBuffer(String uniformName, int sizeBytes) {
            this.uniformName = uniformName;
            this.sizeBytes = sizeBytes;
        }

        private GpuBuffer bind(PostChain chain, Map<String, GpuBuffer> uniformBuffers) {
            GpuBuffer ubo = get(chain);
            GpuBuffer old = uniformBuffers.get(uniformName);
            if (old == ubo) {
                return ubo;
            }
            old = uniformBuffers.put(uniformName, ubo);
            if (old != null && old != ubo) {
                closeQuietly(old);
            }
            return ubo;
        }

        private GpuBuffer get(PostChain chain) {
            if (chain != owner) {
                reset();
                owner = chain;
            }
            if (buffer == null) {
                buffer = createUbo(uniformName, sizeBytes);
            }
            return buffer;
        }

        private void reset() {
            closeQuietly(buffer);
            owner = null;
            buffer = null;
        }

        private boolean resetIfClosed(RuntimeException e) {
            String message = e.getMessage();
            if (message == null || !message.toLowerCase().contains("closed")) {
                return false;
            }
            reset();
            return true;
        }
    }
}
