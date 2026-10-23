package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.WindowInvoker;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
public final class SoftFullscreen {

    // TODO(26.2 backport): 26.3 manages the window through SDL, so the original windowed geometry
    // is captured before the border is removed and restored on disable. 26.2 drives the window
    // with Win32 directly and does not need this capture step.
    private static final int MIN_WINDOW_SIZE = 64;
    private static final int DEFAULT_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_WINDOW_HEIGHT = 720;

    private static boolean applied;
    private static int savedLeft;
    private static int savedTop;
    private static int savedWidth;
    private static int savedHeight;
    private static boolean savedMaximized;
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
            if (applied) {
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
                // Enabling while fullscreen: leave it first so the borderless window can take over.
                resumeFullscreenOnDisable = true;
                leavingFullscreen = true;
                client.options.fullscreen().set(false);
                retryTicks = 3;
            }
            // A manual F11 fullscreen wins while the feature is on, so keep vanilla fullscreen.
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
        // Remember where the manual toggle is heading so disabling later follows it.
        resumeFullscreenOnDisable = !isFullscreen(client);
        if (applied) {
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
            savedMaximized = (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_MAXIMIZED) != 0L;
            if (savedMaximized) {
                SDLVideo.SDL_RestoreWindow(handle);
                SDLVideo.SDL_SyncWindow(handle);
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer positionX = stack.mallocInt(1);
                IntBuffer positionY = stack.mallocInt(1);
                IntBuffer sizeWidth = stack.mallocInt(1);
                IntBuffer sizeHeight = stack.mallocInt(1);
                SDLVideo.SDL_GetWindowPosition(handle, positionX, positionY);
                SDLVideo.SDL_GetWindowSize(handle, sizeWidth, sizeHeight);
                savedLeft = positionX.get(0);
                savedTop = positionY.get(0);
                savedWidth = sizeWidth.get(0);
                savedHeight = sizeHeight.get(0);
            }
            if (savedWidth < MIN_WINDOW_SIZE || savedHeight < MIN_WINDOW_SIZE) {
                savedWidth = 0;
                savedHeight = 0;
                return false;
            }
            // A maximised window reports the display size; fall back to the default size but keep
            // the maximised state so the window is maximised again on disable.
            int captureDisplay = SDLVideo.SDL_GetDisplayForWindow(handle);
            if (captureDisplay >= 0) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    SDL_Rect bounds = SDL_Rect.malloc(stack);
                    if (SDLVideo.SDL_GetDisplayBounds(captureDisplay, bounds)
                            && savedWidth >= bounds.w() - 2 && savedHeight >= bounds.h() - 2) {
                        savedWidth = DEFAULT_WINDOW_WIDTH;
                        savedHeight = DEFAULT_WINDOW_HEIGHT;
                        savedMaximized = true;
                    }
                }
            }
        }

        int x = 0;
        int y = 0;
        int width = Math.max(1, window.getScreenWidth());
        int height = Math.max(1, window.getScreenHeight());
        int display = SDLVideo.SDL_GetDisplayForWindow(handle);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            if (display >= 0 && SDLVideo.SDL_GetDisplayBounds(display, bounds)) {
                x = bounds.x();
                y = bounds.y();
                width = Math.max(1, bounds.w());
                height = Math.max(1, bounds.h());
            }
        }

        SDLVideo.SDL_SetWindowBordered(handle, false);
        SDLVideo.SDL_SetWindowPosition(handle, x, y);
        SDLVideo.SDL_SetWindowSize(handle, width, height);
        SDLVideo.SDL_SyncWindow(handle);
        syncWindow(window);
        syncMinecraftWindow(client);

        int actualWidth;
        int actualHeight;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer sizeWidth = stack.mallocInt(1);
            IntBuffer sizeHeight = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSize(handle, sizeWidth, sizeHeight);
            actualWidth = sizeWidth.get(0);
            actualHeight = sizeHeight.get(0);
        }
        applied = actualWidth == width && actualHeight == height;
        return applied;
    }

    private static void restoreWindow(Minecraft client, boolean enterFullscreen) {
        if (client == null || client.getWindow() == null) {
            applied = false;
            return;
        }

        Window window = client.getWindow();
        long handle = window.handle();
        if (handle != 0L) {
            SDLVideo.SDL_RestoreWindow(handle);
            SDLVideo.SDL_SetWindowBordered(handle, true);

            boolean hasSavedGeometry = savedWidth >= MIN_WINDOW_SIZE && savedHeight >= MIN_WINDOW_SIZE;
            int targetWidth = hasSavedGeometry ? savedWidth : DEFAULT_WINDOW_WIDTH;
            int targetHeight = hasSavedGeometry ? savedHeight : DEFAULT_WINDOW_HEIGHT;
            int targetX = savedLeft;
            int targetY = savedTop;
            if (!hasSavedGeometry) {
                int display = SDLVideo.SDL_GetDisplayForWindow(handle);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    SDL_Rect bounds = SDL_Rect.malloc(stack);
                    if (display >= 0 && SDLVideo.SDL_GetDisplayBounds(display, bounds)) {
                        targetX = bounds.x() + Math.max(0, (bounds.w() - targetWidth) / 2);
                        targetY = bounds.y() + Math.max(0, (bounds.h() - targetHeight) / 2);
                    }
                }
            }
            SDLVideo.SDL_SetWindowSize(handle, targetWidth, targetHeight);
            SDLVideo.SDL_SetWindowPosition(handle, targetX, targetY);
            SDLVideo.SDL_SyncWindow(handle);
            if (savedMaximized) {
                SDLVideo.SDL_MaximizeWindow(handle);
                SDLVideo.SDL_SyncWindow(handle);
            }
            syncWindow(window);
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
            // Let SDL settle the restored window geometry before going back to fullscreen.
            pendingFullscreenTicks = 2;
        }
    }

    private static void recoverZeroSizedWindow(Minecraft client) {
        Window window = client.getWindow();
        if (window == null || window.handle() == 0L) {
            return;
        }
        int width = window.getScreenWidth() > 0 ? window.getScreenWidth() : DEFAULT_WINDOW_WIDTH;
        int height = window.getScreenHeight() > 0 ? window.getScreenHeight() : DEFAULT_WINDOW_HEIGHT;
        SDLVideo.SDL_SetWindowSize(window.handle(), width, height);
        SDLVideo.SDL_SyncWindow(window.handle());
        syncMinecraftWindow(client);
    }

    private static void syncMinecraftWindow(Minecraft client) {
        Window window = client.getWindow();
        long handle = window.handle();
        if (handle == 0L) {
            return;
        }

        SDLEvents.SDL_PumpEvents();

        int framebufferWidth;
        int framebufferHeight;
        int windowWidth;
        int windowHeight;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fbWidth = stack.mallocInt(1);
            IntBuffer fbHeight = stack.mallocInt(1);
            IntBuffer winWidth = stack.mallocInt(1);
            IntBuffer winHeight = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSizeInPixels(handle, fbWidth, fbHeight);
            SDLVideo.SDL_GetWindowSize(handle, winWidth, winHeight);
            framebufferWidth = fbWidth.get(0);
            framebufferHeight = fbHeight.get(0);
            windowWidth = winWidth.get(0);
            windowHeight = winHeight.get(0);
        }
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        WindowInvoker invoker = (WindowInvoker) (Object) window;
        invoker.baity$onResize(windowWidth, windowHeight);
        invoker.baity$onFramebufferResize(framebufferWidth, framebufferHeight);
    }

    private static void syncWindow(Window window) {
        if (window == null) {
            return;
        }
        try {
            ((WindowInvoker) (Object) window).baity$syncWindow();
        } catch (Throwable ignored) {
        }
    }

    private static boolean isFullscreen(Minecraft client) {
        return client.options != null && Boolean.TRUE.equals(client.options.fullscreen().get());
    }

    private static boolean isWindows() {
        return Platform.get() == Platform.WINDOWS;
    }
}
