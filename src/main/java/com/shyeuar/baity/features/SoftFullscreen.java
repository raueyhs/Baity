package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.WindowInvoker;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.MONITORINFOEX;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;

@Environment(EnvType.CLIENT)
public final class SoftFullscreen {

    private static final int WS_CAPTION = 0x00C00000;
    private static final int WS_THICKFRAME = 0x00040000;
    private static final int WS_SYSMENU = 0x00080000;
    private static final int WS_MAXIMIZEBOX = 0x00010000;
    private static final int WS_MINIMIZEBOX = 0x00020000;
    private static final int STYLE_CLEAR = WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;

    private static final int WS_EX_DLGMODALFRAME = 0x00000001;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_WINDOWEDGE = 0x00000100;
    private static final int WS_EX_CLIENTEDGE = 0x00000200;
    private static final int WS_EX_STATICEDGE = 0x00020000;
    private static final int WS_EX_APPWINDOW = 0x00040000;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int WS_EX_COMPOSITED = 0x02000000;
    private static final int EXSTYLE_CLEAR = WS_EX_DLGMODALFRAME | WS_EX_COMPOSITED | WS_EX_WINDOWEDGE
            | WS_EX_CLIENTEDGE | WS_EX_LAYERED | WS_EX_STATICEDGE | WS_EX_TOOLWINDOW | WS_EX_APPWINDOW;

    private static final int POS_FLAGS = User32.SWP_SHOWWINDOW | User32.SWP_NOOWNERZORDER | User32.SWP_NOSENDCHANGING;
    private static final int SETTLE_TICKS_AFTER_EXCLUSIVE = 3;
    private static final int SYNC_TICKS_AFTER_WIN32_RESIZE = 2;

    private static boolean applied;
    private static boolean wasExclusive;
    private static boolean pendingReapply;
    private static int settleTicks;
    private static long savedStyle;
    private static long savedExStyle;
    private static int savedLeft;
    private static int savedTop;
    private static int savedWidth;
    private static int savedHeight;
    private static boolean savedMaximized;
    private static int pendingWindowSync;
    private static boolean resumeFullscreenOnDisable;
    private static boolean leavingFullscreen;
    private static boolean wasEnabled;
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
        boolean justEnabled = want && !wasEnabled;
        wasEnabled = want;

        if (pendingFullscreenTicks > 0) {
            pendingFullscreenTicks--;
            if (pendingFullscreenTicks == 0) {
                client.options.fullscreen().set(true);
            }
        }

        if (!want) {
            pendingReapply = false;
            pendingWindowSync = 0;
            settleTicks = 0;
            leavingFullscreen = false;
            boolean enterFullscreen = resumeFullscreenOnDisable;
            resumeFullscreenOnDisable = false;
            if (applied) {
                restoreWindow(enterFullscreen);
            } else if (enterFullscreen) {
                client.options.fullscreen().set(true);
            }
            wasExclusive = client.getWindow().isFullscreen();
            return;
        }

        if (client.getWindow().isFullscreen()) {
            if (leavingFullscreen) {
                return;
            }
            if (justEnabled) {
                resumeFullscreenOnDisable = true;
                leavingFullscreen = true;
                client.options.fullscreen().set(false);
                settleTicks = SETTLE_TICKS_AFTER_EXCLUSIVE;
            }
            wasExclusive = true;
            return;
        }
        leavingFullscreen = false;

        if (wasExclusive) {
            wasExclusive = false;
            pendingReapply = true;
            settleTicks = SETTLE_TICKS_AFTER_EXCLUSIVE;
        }

        if (settleTicks > 0) {
            settleTicks--;
            return;
        }

        if (pendingReapply || !applied) {
            if (tryApply(client)) {
                pendingReapply = false;
            }
        }
        if (pendingWindowSync > 0) {
            pendingWindowSync--;
            if (pendingWindowSync == 0) {
                syncMinecraftWindow(client);
            }
        }
    }

    public static void onBeforeToggleFullScreen(Minecraft client) {
        if (!isWindows() || client == null || client.getWindow() == null) {
            return;
        }

        resumeFullscreenOnDisable = !client.getWindow().isFullscreen();
        if (applied) {
            restoreWindow(false);
        }
        pendingReapply = true;
        settleTicks = SETTLE_TICKS_AFTER_EXCLUSIVE;
    }

    private static boolean tryApply(Minecraft client) {
        if (client.getWindow().isFullscreen()) {
            return false;
        }
        long hwnd = hwnd(client.getWindow().handle());
        if (hwnd == 0L) {
            return false;
        }
        if (!captureAndBorderless(client, hwnd)) {
            return false;
        }
        applied = true;
        return true;
    }

    private static void restoreWindow(boolean enterFullscreen) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            applied = false;
            clearSaved();
            return;
        }

        long hwnd = hwnd(client.getWindow().handle());
        if (hwnd != 0L && savedStyle != 0L) {
            if (User32.IsZoomed(hwnd)) {
                User32.ShowWindow(hwnd, User32.SW_RESTORE);
            }
            User32.SetWindowLongPtr(null, hwnd, User32.GWL_STYLE, savedStyle);
            User32.SetWindowLongPtr(null, hwnd, User32.GWL_EXSTYLE, savedExStyle);
            User32.SetWindowPos(
                    null,
                    hwnd,
                    User32.HWND_TOP,
                    savedLeft,
                    savedTop,
                    savedWidth,
                    savedHeight,
                    POS_FLAGS | User32.SWP_NOZORDER
            );
            if (savedMaximized) {
                User32.ShowWindow(hwnd, User32.SW_MAXIMIZE);
            } else {
                Window window = client.getWindow();
                window.setWindowed(savedWidth, savedHeight);
                GLFW.glfwSetWindowPos(window.handle(), savedLeft, savedTop);
            }
            scheduleWindowSync();
        }
        applied = false;
        clearSaved();
        if (enterFullscreen) {
            pendingFullscreenTicks = 2;
        }
    }

    private static void clearSaved() {
        savedStyle = 0L;
        savedExStyle = 0L;
        savedLeft = 0;
        savedTop = 0;
        savedWidth = 0;
        savedHeight = 0;
        savedMaximized = false;
    }

    private static void scheduleWindowSync() {
        pendingWindowSync = SYNC_TICKS_AFTER_WIN32_RESIZE;
    }

    private static void syncMinecraftWindow(Minecraft client) {
        Window window = client.getWindow();
        long handle = window.handle();
        if (handle == 0L) {
            return;
        }

        GLFW.glfwPollEvents();

        int[] framebufferWidth = new int[1];
        int[] framebufferHeight = new int[1];
        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        GLFW.glfwGetFramebufferSize(handle, framebufferWidth, framebufferHeight);
        GLFW.glfwGetWindowSize(handle, windowWidth, windowHeight);
        if (framebufferWidth[0] <= 0 || framebufferHeight[0] <= 0) {
            return;
        }

        WindowInvoker invoker = (WindowInvoker) (Object) window;
        invoker.baity$onResize(handle, windowWidth[0], windowHeight[0]);
        invoker.baity$onFramebufferResize(handle, framebufferWidth[0], framebufferHeight[0]);
    }

    private static void captureWindowState(Minecraft client, long hwnd) {
        Window window = client.getWindow();
        savedMaximized = hwnd != 0L && User32.IsZoomed(hwnd);
        savedWidth = Math.max(1, window.getWidth());
        savedHeight = Math.max(1, window.getHeight());
        if (savedMaximized) {
            savedWidth = 1280;
            savedHeight = 720;
        }
        savedLeft = window.getX();
        savedTop = window.getY();
    }

    private static boolean captureAndBorderless(Minecraft client, long hwnd) {
        long style = User32.GetWindowLongPtr(hwnd, User32.GWL_STYLE);
        long exStyle = User32.GetWindowLongPtr(hwnd, User32.GWL_EXSTYLE);
        captureWindowState(client, hwnd);

        savedStyle = style;
        savedExStyle = exStyle;

        long newStyle = style & ~STYLE_CLEAR;
        long newExStyle = exStyle & ~EXSTYLE_CLEAR;
        User32.SetWindowLongPtr(null, hwnd, User32.GWL_STYLE, newStyle);
        User32.SetWindowLongPtr(null, hwnd, User32.GWL_EXSTYLE, newExStyle);

        int x;
        int y;
        int w;
        int h;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            MONITORINFOEX info = MONITORINFOEX.calloc(stack);
            info.cbSize(MONITORINFOEX.SIZEOF);
            long monitor = User32.MonitorFromWindow(hwnd, User32.MONITOR_DEFAULTTONEAREST);
            if (monitor == 0L || !User32.GetMonitorInfo(monitor, info)) {
                User32.SetWindowLongPtr(null, hwnd, User32.GWL_STYLE, savedStyle);
                User32.SetWindowLongPtr(null, hwnd, User32.GWL_EXSTYLE, savedExStyle);
                clearSaved();
                return false;
            }
            RECT monitorRect = info.rcMonitor();
            x = monitorRect.left();
            y = monitorRect.top();
            w = Math.max(1, monitorRect.right() - monitorRect.left());
            h = Math.max(1, monitorRect.bottom() - monitorRect.top());
        }

        User32.SetWindowPos(null, hwnd, User32.HWND_TOP, x, y, w, h, POS_FLAGS);
        User32.ShowWindow(hwnd, User32.SW_MAXIMIZE);
        scheduleWindowSync();
        return true;
    }

    private static long hwnd(long glfwWindow) {
        if (glfwWindow == 0L) {
            return 0L;
        }
        try {
            return GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static boolean isWindows() {
        return Platform.get() == Platform.WINDOWS;
    }
}
