package com.shyeuar.baity.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.DevConfig;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.features.smolpeople.SmolFriendManager;
import com.shyeuar.baity.utils.ModuleUtils;
import com.shyeuar.baity.utils.NickRenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class NametagRenderer implements WorldRenderEvents.AfterEntities {
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
        final String processed;
        final int width;
        CachedName(String processed, int width) {
            this.processed = processed;
            this.width = width;
        }
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) {
            return; 
        }
        boolean defaultNametag = ModuleUtils.getOptionBoolean(m, "default nametag", false);
        if (defaultNametag) {
            return;
        }

        refreshLayoutVersionIfNeeded(m);
        
        if (mc.level == null || mc.player == null) return;
        
        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.getMainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float cameraYaw = camera.getYRot();
        float cameraPitch = camera.getXRot();
        
        PoseStack matrices = context.matrices();
        if (matrices == null) {
            matrices = new PoseStack();
            matrices.mulPose(new org.joml.Quaternionf().rotationXYZ(
                (float) Math.toRadians(cameraPitch),
                (float) Math.toRadians(cameraYaw + 180.0f),
                0.0f
            ));
        }
        
        updateCache();
        
        com.shyeuar.baity.utils.AntiBotUtils.updatePlayerMap();
        for (Player player : mc.level.players()) {
            if (com.shyeuar.baity.utils.AntiBotUtils.isBot(player)) {
                continue;
            }
            
            boolean showOwnNametag = ModuleUtils.getOptionBoolean(m, "show own nametag", false);
            if (player == mc.player) {
                if (mc.options.getCameraType().isFirstPerson()) {
                    continue;
                }
                if (!showOwnNametag) {
                    continue;
                }
            }
            
            Vec3 lerpedPos = player.getPosition(tickDelta);
            double x = lerpedPos.x - cameraPos.x;
            double y = lerpedPos.y - cameraPos.y;
            double z = lerpedPos.z - cameraPos.z;

            matrices.pushPose();
            try {
                matrices.translate(x, y, z);
                renderPlayerName(matrices, player, cameraYaw, cameraPitch, m);
            } finally {
                matrices.popPose();
            }
        }
    }
    
    private void renderPlayerName(PoseStack matrices, Player player, float cameraYaw, float cameraPitch, Module module) {
        matrices.pushPose();
        try {
            float heightOffset = player.getBbHeight() + 0.5f;
            
            Module noSwimPoseModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("NoSwimPose");
            if (noSwimPoseModule != null && noSwimPoseModule.isEnabled()) {
                if (player == mc.player && player.getPose() == net.minecraft.world.entity.Pose.SWIMMING) {
                    heightOffset = 1.8f + 0.5f;
                }
            }
            
            Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule != null && smolPeopleModule.isEnabled()) {
                if (SmolFriendManager.shouldApplySmolTo(player.getId())) {
                    heightOffset -= 0.4f;
                }
            }
            
            matrices.translate(0, heightOffset, 0);
            matrices.mulPose(new org.joml.Quaternionf().rotationY(-cameraYaw * 0.017453292F));
            matrices.mulPose(new org.joml.Quaternionf().rotationX(cameraPitch * 0.017453292F));

            assert mc.player != null;
            boolean focusPlayerNametag = ModuleUtils.getOptionBoolean(module, "focus player nametag", false);
            
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

        String cacheKey = nametagLayoutVersion + "|" + baseName;
        CachedName cached;
        synchronized (NAME_CACHE_LOCK) {
            cached = NAME_CACHE.get(cacheKey);
        }
        String processedForWidth;
        int nameWidth;
        if (cached != null) {
            processedForWidth = cached.processed;
            nameWidth = cached.width;
        } else {
            processedForWidth = NickRenderUtils.handleString(baseName);
            nameWidth = textRenderer.width(processedForWidth);
            CachedName newEntry = new CachedName(processedForWidth, nameWidth);
            synchronized (NAME_CACHE_LOCK) {
                NAME_CACHE.put(cacheKey, newEntry);
            }
        }
        int totalWidth = nameWidth;
        if (isDeveloper) {
            totalWidth += textRenderer.width(DevConfig.DEV_PREFIX) + 2; 
        }
        
        int currentX;
        if (isDeveloper) {
            int prefixWidth = textRenderer.width(DevConfig.DEV_PREFIX) + 2;
            currentX = -nameWidth / 2 - prefixWidth; 
        } else {
            currentX = -totalWidth / 2;
        }
        
        MultiBufferSource.BufferSource immediate = mc.renderBuffers().bufferSource();
        
        int distanceColorWithAlpha = 0xFF00FFFF;
        int devColorWithAlpha = DevConfig.DEV_PREFIX_COLOR | 0xFF000000;
        
        if (isDeveloper) {
            textRenderer.drawInBatch(DevConfig.DEV_PREFIX, currentX, 0, devColorWithAlpha, false, matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            currentX += textRenderer.width(DevConfig.DEV_PREFIX) + 2;
        }
        
        textRenderer.drawInBatch(nameComponent, currentX, 0, 0xFFFFFFFF, false, matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        
        if (showDistance) {
            double dist = mc.player != null ? mc.player.distanceTo(player) : 0.0;
            String distanceText = " [" + (int)Math.round(dist) + "]";
            int distanceX;
            if (isDeveloper) {
                distanceX = currentX + nameWidth + 2;
            } else {
                distanceX = totalWidth / 2 + 2;
            }
            textRenderer.drawInBatch(distanceText, distanceX, 0, distanceColorWithAlpha, false, matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        }
        
            immediate.endBatch();
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
        boolean focusPlayerNametag = ModuleUtils.getOptionBoolean(nametagModule, "focus player nametag", false);
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
