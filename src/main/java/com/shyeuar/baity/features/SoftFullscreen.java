package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.WindowInvoker;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

@Environment(EnvType.CLIENT)
public final class SoftFullscreen {

    private static final int MIN_WINDOW_SIZE = 64;
    private static final int DEFAULT_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_WINDOW_HEIGHT = 720;

    private static boolean applied;
    private static int savedLeft;
    private static int savedTop;
    private static int savedWidth;
    private static int savedHeight;
    private static boolean savedMaximized;
    private static boolean decorationsRemoved;
    private static int retryTicks;
    private static boolean resumeFullscreenOnDisable;
    private static boolean leavingFullscreen;
    private static boolean wasEnabled;
    private static int pendingRestoreSync;
    private static int pendingFullscreenTicks;

    private SoftFullscreen() {
    }

    public static boolean isApplied() {
        return applied;
    }

    public static void tick(Minecraft client) {
        if (!isWindows() || client == null || client.getWindow() == null) {
            return;
        }

        Module module = ModuleManager.getModuleByName("SoftFullscreen");
        boolean want = module != null && module.isEnabled();

        Window window = client.getWindow();
        if (window.getWidth() <= 0 || window.getHeight() <= 0) {
            recoverZeroSizedWindow(client);
        }

        if (pendingRestoreSync > 0) {
            pendingRestoreSync--;
            syncMinecraftWindow(client);
        }
        if (pendingFullscreenTicks > 0) {
            pendingFullscreenTicks--;
            if (pendingFullscreenTicks == 0) {
                client.options.fullscreen().set(true);
            }
        }

        boolean justEnabled = want && !wasEnabled;
        wasEnabled = want;

        if (!want) {
            retryTicks = 0;
            leavingFullscreen = false;
            boolean enterFullscreen = resumeFullscreenOnDisable;
            resumeFullscreenOnDisable = false;
            if (applied || decorationsRemoved || savedWidth > 0) {
                restoreWindow(client, enterFullscreen);
            } else if (enterFullscreen) {
                client.options.fullscreen().set(true);
            }
            return;
        }

        if (isFullscreen(client)) {
            if (leavingFullscreen) {
                return;
            }
            if (justEnabled) {
                resumeFullscreenOnDisable = true;
                leavingFullscreen = true;
                client.options.fullscreen().set(false);
                retryTicks = 3;
            }
            return;
        }
        leavingFullscreen = false;

        if (applied) {
            return;
        }
        if (retryTicks > 0) {
            retryTicks--;
            return;
        }
        if (!applyBorderless(client)) {
            retryTicks = 2;
        }
    }

    public static void onBeforeToggleFullScreen(Minecraft client) {
        if (client == null) {
            return;
        }
        resumeFullscreenOnDisable = !isFullscreen(client);
        if (applied || decorationsRemoved || savedWidth > 0) {
            restoreWindow(client, false);
        }
    }

    private static boolean applyBorderless(Minecraft client) {
        Window window = client.getWindow();
        long handle = window.handle();
        if (handle == 0L) {
            return false;
        }

        if (savedWidth <= 0) {
            captureWindowState(client, handle);
            if (savedWidth < MIN_WINDOW_SIZE || savedHeight < MIN_WINDOW_SIZE) {
                savedWidth = 0;
                savedHeight = 0;
                return false;
            }
        }

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        decorationsRemoved = true;

        int x = 0;
        int y = 0;
        int width = Math.max(1, window.getScreenWidth());
        int height = Math.max(1, window.getScreenHeight());
        int[] monitor = monitorRect();
        if (monitor != null) {
            x = monitor[0];
            y = monitor[1];
            width = Math.max(1, monitor[2]);
            height = Math.max(1, monitor[3]);
        }

        GLFW.glfwSetWindowSize(handle, width, height);
        GLFW.glfwSetWindowPos(handle, x, y);
        GLFW.glfwShowWindow(handle);
        syncMinecraftWindow(client);

        int[] size = windowSize(handle);
        applied = size != null && Math.abs(size[0] - width) <= 2 && Math.abs(size[1] - height) <= 2;
        return applied;
    }

    private static void restoreWindow(Minecraft client, boolean enterFullscreen) {
        if (client == null || client.getWindow() == null) {
            applied = false;
            decorationsRemoved = false;
            return;
        }

        Window window = client.getWindow();
        long handle = window.handle();
        if (handle != 0L) {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            decorationsRemoved = false;

            boolean hasSavedGeometry = savedWidth >= MIN_WINDOW_SIZE && savedHeight >= MIN_WINDOW_SIZE;
            int targetWidth = hasSavedGeometry ? savedWidth : DEFAULT_WINDOW_WIDTH;
            int targetHeight = hasSavedGeometry ? savedHeight : DEFAULT_WINDOW_HEIGHT;
            int targetX = savedLeft;
            int targetY = savedTop;
            if (!hasSavedGeometry) {
                int[] monitor = monitorRect();
                if (monitor != null) {
                    targetX = monitor[0] + Math.max(0, (monitor[2] - targetWidth) / 2);
                    targetY = monitor[1] + Math.max(0, (monitor[3] - targetHeight) / 2);
                }
            }

            window.setWindowed(targetWidth, targetHeight);
            GLFW.glfwSetWindowSize(handle, targetWidth, targetHeight);
            GLFW.glfwSetWindowPos(handle, targetX, targetY);
            if (savedMaximized) {
                GLFW.glfwMaximizeWindow(handle);
            }
            syncMinecraftWindow(client);
            if (window.getWidth() <= 0 || window.getHeight() <= 0) {
                recoverZeroSizedWindow(client);
            }
            pendingRestoreSync = 3;
        }
        savedWidth = 0;
        savedHeight = 0;
        savedMaximized = false;
        applied = false;

        if (enterFullscreen) {
            pendingFullscreenTicks = 2;
        }
    }

    private static void captureWindowState(Minecraft client, long handle) {
        savedMaximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
        if (savedMaximized) {
            GLFW.glfwRestoreWindow(handle);
        }

        int[] position = windowPosition(handle);
        int[] size = windowSize(handle);
        if (position != null && size != null) {
            savedLeft = position[0];
            savedTop = position[1];
            savedWidth = Math.max(1, size[0]);
            savedHeight = Math.max(1, size[1]);
        } else {
            savedWidth = Math.max(1, client.getWindow().getWidth());
            savedHeight = Math.max(1, client.getWindow().getHeight());
            savedLeft = client.getWindow().getX();
            savedTop = client.getWindow().getY();
        }

        int[] monitor = monitorRect();
        if (monitor != null && savedWidth >= monitor[2] - 2 && savedHeight >= monitor[3] - 2) {
            savedWidth = DEFAULT_WINDOW_WIDTH;
            savedHeight = DEFAULT_WINDOW_HEIGHT;
            savedMaximized = true;
        }
    }

    private static int[] monitorRect() {
        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (monitor == 0L) {
            return null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer x = stack.mallocInt(1);
            java.nio.IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetMonitorPos(monitor, x, y);
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            if (mode == null) {
                return null;
            }
            return new int[]{x.get(0), y.get(0), mode.width(), mode.height()};
        }
    }

    private static int[] windowPosition(long handle) {
        if (handle == 0L) {
            return null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer x = stack.mallocInt(1);
            java.nio.IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(handle, x, y);
            return new int[]{x.get(0), y.get(0)};
        }
    }

    private static int[] windowSize(long handle) {
        if (handle == 0L) {
            return null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer w = stack.mallocInt(1);
            java.nio.IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(handle, w, h);
            return new int[]{w.get(0), h.get(0)};
        }
    }

    private static void recoverZeroSizedWindow(Minecraft client) {
        Window window = client.getWindow();
        if (window == null || window.handle() == 0L) {
            return;
        }
        int width = window.getScreenWidth() > 0 ? window.getScreenWidth() : DEFAULT_WINDOW_WIDTH;
        int height = window.getScreenHeight() > 0 ? window.getScreenHeight() : DEFAULT_WINDOW_HEIGHT;
        GLFW.glfwSetWindowSize(window.handle(), width, height);
        syncMinecraftWindow(client);
    }

    private static void syncMinecraftWindow(Minecraft client) {
        Window window = client.getWindow();
        long handle = window.handle();
        if (handle == 0L) {
            return;
        }

        GLFW.glfwPollEvents();

        int[] windowSize = windowSize(handle);
        int[] framebufferSize;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer fbWidth = stack.mallocInt(1);
            java.nio.IntBuffer fbHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(handle, fbWidth, fbHeight);
            framebufferSize = new int[]{fbWidth.get(0), fbHeight.get(0)};
        }
        if (windowSize == null || framebufferSize[0] <= 0 || framebufferSize[1] <= 0) {
            return;
        }

        WindowInvoker invoker = (WindowInvoker) (Object) window;
        invoker.baity$onResize(handle, windowSize[0], windowSize[1]);
        invoker.baity$onFramebufferResize(handle, framebufferSize[0], framebufferSize[1]);
    }

    private static boolean isFullscreen(Minecraft client) {
        return client.options != null && Boolean.TRUE.equals(client.options.fullscreen().get());
    }

    private static boolean isWindows() {
        return Platform.get() == Platform.WINDOWS;
    }
}
