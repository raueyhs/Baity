package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.StatusBarHeightProvider;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class VanillaHudHider {
    private static final int ROW_HEIGHT = 10;
    private static final int HELD_ITEM_TOOLTIP_HEIGHT = 20;
    private static final int OVERLAY_MESSAGE_HEIGHT = 29;
    private static final int TEXT_HEIGHT_DELTA = 9;
    private static final String MODULE_NAME = "VanillaHudHider";
    private static final String EXPERIENCE_BAR_OPTION = "experience bar";

    private VanillaHudHider() {}

    public static void init() {
        registerVanillaHudStatusBar(VanillaHudElements.ARMOR_BAR, "armor bar", VanillaHudHider::armorBarHeight, true);
        registerVanillaHudStatusBar(VanillaHudElements.HEALTH_BAR, "health bar", VanillaHudHider::healthBarHeight, true);
        registerVanillaHudStatusBar(VanillaHudElements.FOOD_BAR, "food bar", VanillaHudHider::foodBarHeight, false);
        registerVanillaHudStatusBar(VanillaHudElements.AIR_BAR, "air bar", VanillaHudHider::airBarHeight, false);
        registerVanillaHudStatusBar(VanillaHudElements.MOUNT_HEALTH, "mount health", VanillaHudHider::mountHealthHeight, false);
        registerVanillaHudElement(VanillaHudElements.INFO_BAR, EXPERIENCE_BAR_OPTION);
        registerVanillaHudElement(VanillaHudElements.EXPERIENCE_LEVEL, EXPERIENCE_BAR_OPTION);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> client.execute(VanillaHudHider::cancelStatusBarTextOffsets));
    }

    private static void cancelStatusBarTextOffsets() {
        cancelFabricTextOffset(VanillaHudElements.HELD_ITEM_TOOLTIP, false);
        cancelFabricTextOffset(VanillaHudElements.OVERLAY_MESSAGE, true);
    }

    private static void cancelFabricTextOffset(Identifier id, boolean overlayMessage) {
        HudElementRegistry.replaceElement(id, original -> (graphics, tickDelta) -> {
            Player player = Minecraft.getInstance().player;
            int fabricOffset = 0;
            if (player != null) {
                int maxHeight = maxStatusBarHeight(player);
                if (overlayMessage) {
                    fabricOffset = OVERLAY_MESSAGE_HEIGHT - Math.max(OVERLAY_MESSAGE_HEIGHT, maxHeight + TEXT_HEIGHT_DELTA);
                } else {
                    fabricOffset = HELD_ITEM_TOOLTIP_HEIGHT - Math.max(HELD_ITEM_TOOLTIP_HEIGHT, maxHeight);
                }
            }
            if (fabricOffset != 0) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(0.0F, -fabricOffset);
                original.extractRenderState(graphics, tickDelta);
                graphics.pose().popMatrix();
                return;
            }
            original.extractRenderState(graphics, tickDelta);
        });
    }

    private static int maxStatusBarHeight(Player player) {
        int left = statusBarHeight("health bar", VanillaHudHider::healthBarHeight, player)
            + statusBarHeight("armor bar", VanillaHudHider::armorBarHeight, player);
        int right = statusBarHeight("mount health", VanillaHudHider::mountHealthHeight, player)
            + statusBarHeight("food bar", VanillaHudHider::foodBarHeight, player)
            + statusBarHeight("air bar", VanillaHudHider::airBarHeight, player);
        return Math.max(left, right);
    }

    private static int statusBarHeight(String optionName, StatusBarHeightProvider visibleHeight, Player player) {
        if (shouldHideVanillaHudElement(optionName)) {
            return 0;
        }
        return visibleHeight.getStatusBarHeight(player);
    }

    private static void registerVanillaHudElement(Identifier id, String optionName) {
        HudElementRegistry.replaceElement(id, original -> (graphics, tickDelta) -> {
            if (!shouldHideVanillaHudElement(optionName)) {
                original.extractRenderState(graphics, tickDelta);
            }
        });
    }

    private static void registerVanillaHudStatusBar(
            Identifier id,
            String optionName,
            StatusBarHeightProvider visibleHeight,
            boolean leftSide
    ) {
        registerVanillaHudElement(id, optionName);
        StatusBarHeightProvider provider = player -> {
            if (shouldHideVanillaHudElement(optionName) || player == null) {
                return 0;
            }
            return visibleHeight.getStatusBarHeight(player);
        };
        if (leftSide) {
            HudStatusBarHeightRegistry.addLeft(id, provider);
        } else {
            HudStatusBarHeightRegistry.addRight(id, provider);
        }
    }

    private static boolean shouldHideVanillaHudElement(String optionName) {
        Module module = ModuleManager.getModuleByName(MODULE_NAME);
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return ModuleUtils.getOptionBoolean(module, optionName, false);
    }

    private static int armorBarHeight(Player player) {
        return player.getArmorValue() > 0 ? ROW_HEIGHT : 0;
    }

    private static int healthBarHeight(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return 0;
        }
        int healthRows = Math.max(1, Mth.ceil(player.getMaxHealth() / 20.0F));
        int absorptionHearts = Mth.ceil(player.getAbsorptionAmount() / 2.0F);
        int absorptionRows = absorptionHearts > 0 ? Mth.ceil(absorptionHearts / 10.0F) : 0;
        return (healthRows + absorptionRows) * ROW_HEIGHT;
    }

    private static int foodBarHeight(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return 0;
        }
        return ROW_HEIGHT;
    }

    private static int airBarHeight(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return 0;
        }
        return player.getAirSupply() < player.getMaxAirSupply() ? ROW_HEIGHT : 0;
    }

    private static int mountHealthHeight(Player player) {
        if (!(player.getVehicle() instanceof LivingEntity living) || living.getMaxHealth() <= 0.0F) {
            return 0;
        }
        return ROW_HEIGHT;
    }
}
