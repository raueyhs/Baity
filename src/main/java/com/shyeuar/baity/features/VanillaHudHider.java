package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.StatusBarHeightProvider;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class VanillaHudHider {
    private static final int ROW_HEIGHT = 10;
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
    }

    private static void registerVanillaHudElement(Identifier id, String optionName) {
        HudElementRegistry.replaceElement(id, original -> (graphics, tickDelta) -> {
            if (!shouldHideVanillaHudElement(optionName)) {
                original.render(graphics, tickDelta);
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
