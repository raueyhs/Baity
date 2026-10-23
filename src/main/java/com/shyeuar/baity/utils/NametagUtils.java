package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.FocusPlayerNametag;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.AvatarRendererInvoker;
import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class NametagUtils {

    private static final ThreadLocal<Boolean> QUERYING_VANILLA_VISIBILITY = ThreadLocal.withInitial(() -> false);

    private NametagUtils() {
    }

    public static boolean isQueryingVanillaVisibility() {
        return QUERYING_VANILLA_VISIBILITY.get();
    }

    public static boolean wouldVanillaShowName(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || player == null) {
            return false;
        }

        EntityRenderer<? super Player, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof AvatarRenderer<?>)) {
            return false;
        }

        double distSq = mc.gameRenderer.mainCamera().position().distanceToSqr(
            player.getX(), player.getY(), player.getZ());
        QUERYING_VANILLA_VISIBILITY.set(true);
        try {
            return ((AvatarRendererInvoker) renderer).baity$invokeShouldShowName((Avatar) player, distSq);
        } catch (Throwable ignored) {
            return true;
        } finally {
            QUERYING_VANILLA_VISIBILITY.set(false);
        }
    }

    public static boolean wouldEntityRender(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || player == null) {
            return false;
        }

        Camera camera = mc.gameRenderer.mainCamera();
        Frustum frustum = camera.getCullFrustum();
        if (frustum == null) {
            return true;
        }

        Vec3 camPos = camera.position();
        try {
            return mc.getEntityRenderDispatcher().shouldRender(player, frustum, camPos.x, camPos.y, camPos.z, 0.0f);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean shouldDrawCustomNametag(Player player) {
        if (!wouldVanillaShowName(player)) {
            return false;
        }
        if (FocusPlayerNametag.isActive()) {
            return true;
        }
        return wouldEntityRender(player);
    }

    public static boolean shouldRenderCustomNametag(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || player == null) {
            return false;
        }
        if (AntiBotUtils.isBot(player)) {
            return false;
        }
        if (player == mc.player) {
            if (mc.options.getCameraType().isFirstPerson()) {
                return false;
            }
            Module nametag = ModuleManager.getModuleByName("Nametag");
            return nametag != null && nametag.isEnabled()
                && ModuleUtils.getOptionBoolean(nametag, "show own nametag", false);
        }
        return shouldDrawCustomNametag(player);
    }

    public static boolean shouldSuppressVanillaNametag(Player player) {
        if (!FocusPlayerNametag.shouldUseCustomNametagPath()) {
            return false;
        }
        if (player == null) {
            return false;
        }
        if (AntiBotUtils.isBot(player)) {
            return false;
        }
        return shouldRenderCustomNametag(player);
    }

    public static boolean isNametagModuleActive() {
        return ConfigManager.nametagEnabled;
    }

    public static boolean isDefaultNametagMode() {
        return ConfigManager.nametagEnabled && ConfigManager.nametagDefaultNametag;
    }

    public static boolean isOwnNametagDisplayEnabled() {
        return ConfigManager.nametagEnabled
            && ConfigManager.nametagDefaultNametag
            && ConfigManager.nametagShowOwnNametag;
    }

    public static boolean isDefaultNametagEnabled() {
        Module nametag = ModuleManager.getModuleByName("Nametag");
        return nametag != null && nametag.isEnabled()
            && ModuleUtils.getOptionBoolean(nametag, "default nametag", false);
    }

    public static boolean isHudEntityRender(CameraRenderState cameraState) {
        return !RenderScope.isWorldEntityRender(cameraState);
    }

    public static void ensureOwnNameTag(EntityRenderState state) {
        if (state == null || state.nameTag != null || !isOwnNametagDisplayEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        state.nameTag = mc.player.getDisplayName();
    }

    public static boolean shouldProcessNametagText() {
        return isDefaultNametagEnabled();
    }

    public static boolean shouldApplyNametagLayoutCompat() {
        return isDefaultNametagEnabled() && RenderScope.isWorldNameTagAdd();
    }

    public static Component applyNickProcessedText(Component original) {
        FormattedText processed = NickRenderUtils.handleFormattedText(original);
        if (processed instanceof Component processedComponent) {
            return processedComponent;
        }
        return original;
    }
}
