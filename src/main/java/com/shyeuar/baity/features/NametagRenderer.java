package com.shyeuar.baity.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.DevConfig;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.features.smolpeople.SmolPeopleNametag;
import com.shyeuar.baity.utils.ClientPickUtils;
import com.shyeuar.baity.utils.FloatingWorldTextCompat;
import com.shyeuar.baity.utils.ModuleUtils;
import com.shyeuar.baity.utils.NickRenderUtils;
import com.shyeuar.baity.render.RenderScope;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class NametagRenderer implements LevelRenderEvents.CollectSubmits {
    private static final Minecraft mc = Minecraft.getInstance();

    private static long lastTimeUpdate = 0;
    private static double cachedSinValue = 0.0;
    private static final Object NAME_CACHE_LOCK = new Object();
    private static volatile long nametagLayoutVersion = 0L;
    private static volatile String lastLayoutSignature = "";
    private static final java.util.LinkedHashMap<String, CachedName> NAME_CACHE = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, CachedName> eldest) {
            return size() > 512;
        }
    };

    private static final class CachedName {
        final int width;

        CachedName(int width) {
            this.width = width;
        }
    }

    @Override
    public void collectSubmits(LevelRenderContext context) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) {
            return;
        }
        if (!FocusPlayerNametag.shouldUseCustomNametagPath()) {
            return;
        }

        refreshLayoutVersionIfNeeded(m);

        if (mc.level == null || mc.player == null) {
            return;
        }

        SubmitNodeCollector submits = context.submitNodeCollector();
        if (submits == null) {
            return;
        }

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.mainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float cameraYaw = camera.yRot();
        float cameraPitch = camera.xRot();

        PoseStack matrices = context.poseStack();
        if (matrices == null) {
            matrices = new PoseStack();
            matrices.rotate(new org.joml.Quaternionf().rotationXYZ(
                    (float) Math.toRadians(cameraPitch),
                    (float) Math.toRadians(cameraYaw + 180.0f),
                    0.0f
            ));
        }

        updateCache();

        FloatingWorldTextCompat.beginFrame();
        RenderScope.enterCustomNametagText();
        try {
            for (Player player : mc.level.players()) {
                if (!com.shyeuar.baity.utils.NametagUtils.shouldRenderCustomNametag(player)) {
                    continue;
                }

                Vec3 lerpedPos = player.getPosition(tickDelta);
                double x = lerpedPos.x - cameraPos.x;
                double y = lerpedPos.y - cameraPos.y;
                double z = lerpedPos.z - cameraPos.z;

                matrices.pushPose();
                try {
                    matrices.translate(x, y, z);
                    renderPlayerName(matrices, submits, player, cameraYaw, cameraPitch, m);
                } finally {
                    matrices.popPose();
                }
            }
        } finally {
            RenderScope.exitCustomNametagText();
            FloatingWorldTextCompat.endFrame();
        }
    }

    private void renderPlayerName(
            PoseStack matrices,
            SubmitNodeCollector submits,
            Player player,
            float cameraYaw,
            float cameraPitch,
            Module module
    ) {
        matrices.pushPose();
        try {
            float heightOffset = player.getBbHeight() + 0.5f;
            if (player == mc.player) {
                float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                heightOffset = ClientPickUtils.getSelfNametagHeightOffset(player, partialTick);
            }
            heightOffset = SmolPeopleNametag.adjustNametagHeight(heightOffset, player.getId());

            matrices.translate(0, heightOffset, 0);
            matrices.rotate(new org.joml.Quaternionf().rotationY(-cameraYaw * 0.017453292F));
            matrices.rotate(new org.joml.Quaternionf().rotationX(cameraPitch * 0.017453292F));

            assert mc.player != null;
            boolean focusPlayerNametag = FocusPlayerNametag.isActive();

            double distance = mc.player.distanceTo(player);
            float dynamicScale = (float) Math.max(0.03, Math.min(distance * 0.0025, 0.12));
            float baseScale = focusPlayerNametag ? dynamicScale : 0.03f;
            float animatedScale = (float) (baseScale * (1.0 + cachedSinValue * 0.3));
            matrices.scale(-animatedScale, -animatedScale, animatedScale);

            Component originalNameComponent = player.getDisplayName() != null ? player.getDisplayName() : player.getName();
            boolean isDeveloper = DevConfig.isDeveloper(player);
            boolean showDistance = ModuleUtils.getOptionBoolean(module, "show distance", true);
            boolean forcePinkColor = ModuleUtils.getOptionBoolean(module, "force pink color", true);

            FormattedText nickProcessed = NickRenderUtils.handleFormattedText(originalNameComponent);
            boolean nickTweaksModifiesDisplay = nickProcessed != originalNameComponent;

            Component nameComponent;
            String baseName;
            if (nickTweaksModifiesDisplay) {
                nameComponent = (Component) nickProcessed;
                baseName = originalNameComponent.getString();
            } else if (forcePinkColor) {
                baseName = originalNameComponent.getString();
                if (baseName.indexOf('\u00A7') >= 0) {
                    StringBuilder sb = new StringBuilder(baseName.length());
                    for (int i = 0; i < baseName.length(); i++) {
                        char c = baseName.charAt(i);
                        if (c == '\u00A7' && i + 1 < baseName.length()) {
                            i++;
                            continue;
                        }
                        sb.append(c);
                    }
                    baseName = sb.toString();
                }
                nameComponent = Component.literal(baseName).withStyle(Style.EMPTY.withColor(0xFFFF69B4));
            } else {
                nameComponent = originalNameComponent;
                baseName = nameComponent.getString();
            }
            Font textRenderer = mc.font;

            String renderedForWidth = nameComponent.getString();
            String cacheKey = nametagLayoutVersion + "|" + renderedForWidth;
            CachedName cached;
            synchronized (NAME_CACHE_LOCK) {
                cached = NAME_CACHE.get(cacheKey);
            }
            int nameWidth;
            if (cached != null) {
                nameWidth = cached.width;
            } else {
                nameWidth = textRenderer.width(nameComponent.getVisualOrderText());
                CachedName newEntry = new CachedName(nameWidth);
                synchronized (NAME_CACHE_LOCK) {
                    NAME_CACHE.put(cacheKey, newEntry);
                }
            }
            int prefixWidth = 0;
            if (isDeveloper) {
                prefixWidth = textRenderer.width("[") + textRenderer.width("Dev") + textRenderer.width("]") + 2;
            }

            String distanceText = null;
            int distanceWidth = 0;
            if (showDistance) {
                double dist = mc.player != null ? mc.player.distanceTo(player) : 0.0;
                distanceText = " [" + (int) Math.round(dist) + "]";
                distanceWidth = textRenderer.width(distanceText);
            }

            int totalWidth = nameWidth + prefixWidth + distanceWidth + (showDistance ? 2 : 0);
            int currentX = -totalWidth / 2;

            int distanceColorWithAlpha = 0xFF00FFFF;
            int bracketColorWithAlpha = DevConfig.DEV_PREFIX_COLOR | 0xFF000000;
            int devTextColorWithAlpha = DevConfig.DEV_TEXT_COLOR | 0xFF000000;

            if (isDeveloper) {
                FloatingWorldTextCompat.drawInBatch(textRenderer, "[", currentX, 0, bracketColorWithAlpha, matrices, submits, 15728880);
                currentX += textRenderer.width("[");
                FloatingWorldTextCompat.drawInBatch(textRenderer, "Dev", currentX, 0, devTextColorWithAlpha, matrices, submits, 15728880);
                currentX += textRenderer.width("Dev");
                FloatingWorldTextCompat.drawInBatch(textRenderer, "]", currentX, 0, bracketColorWithAlpha, matrices, submits, 15728880);
                currentX += textRenderer.width("]") + 2;
            }

            FloatingWorldTextCompat.drawInBatch(textRenderer, nameComponent, currentX, 0, 0xFFFFFFFF, matrices, submits, 15728880);
            currentX += nameWidth;

            if (showDistance && distanceText != null) {
                currentX += 2;
                FloatingWorldTextCompat.drawInBatch(textRenderer, distanceText, currentX, 0, distanceColorWithAlpha, matrices, submits, 15728880);
            }
        } finally {
            matrices.popPose();
        }
    }

    private static void updateCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime != lastTimeUpdate) {
            cachedSinValue = Math.sin(currentTime * 0.001) * 0.08;
            lastTimeUpdate = currentTime;
        }
    }

    private static void refreshLayoutVersionIfNeeded(Module nametagModule) {
        String signature = buildLayoutSignature(nametagModule);
        if (signature.equals(lastLayoutSignature)) {
            return;
        }

        synchronized (NAME_CACHE_LOCK) {
            if (signature.equals(lastLayoutSignature)) {
                return;
            }
            lastLayoutSignature = signature;
            nametagLayoutVersion++;
            NAME_CACHE.clear();
        }
    }

    private static String buildLayoutSignature(Module nametagModule) {
        boolean showDistance = ModuleUtils.getOptionBoolean(nametagModule, "show distance", true);
        boolean forcePinkColor = ModuleUtils.getOptionBoolean(nametagModule, "force pink color", true);
        boolean focusPlayerNametag = FocusPlayerNametag.isActive();
        boolean showOwnNametag = ModuleUtils.getOptionBoolean(nametagModule, "show own nametag", false);
        boolean nickTweaksEnabled = com.shyeuar.baity.config.ConfigManager.nickTweaksEnabled;
        boolean nickTweaksChromaEnabled = com.shyeuar.baity.config.ConfigManager.nickTweaksChromaEnabled;
        boolean nickTweaksCustomNickColorEnabled = com.shyeuar.baity.config.ConfigManager.nickTweaksCustomNickColorEnabled;
        boolean nickTweaksBoldSelf = com.shyeuar.baity.config.ConfigManager.nickTweaksBoldSelf;
        String nickChanger = com.shyeuar.baity.config.ConfigManager.nickTweaksNickChanger;
        if (nickChanger == null) {
            nickChanger = "";
        }

        return showDistance
                + "|" + forcePinkColor
                + "|" + focusPlayerNametag
                + "|" + showOwnNametag
                + "|" + nickTweaksEnabled
                + "|" + nickTweaksChromaEnabled
                + "|" + nickTweaksCustomNickColorEnabled
                + "|" + nickTweaksBoldSelf
                + "|" + nickChanger
                + "|" + NickRenderUtils.getTargetsCacheAt();
    }
}
