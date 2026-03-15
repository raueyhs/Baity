package com.shyeuar.baity.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.DevConfig;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class NametagRenderer implements WorldRenderEvents.AfterEntities {
    private static final Minecraft mc = Minecraft.getInstance();
    
    private static long lastTimeUpdate = 0;
    private static double cachedSinValue = 0.0;
    
    @Override
    public void afterEntities(WorldRenderContext context) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) {
            return; 
        }
        
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
                boolean showOwnNametag = ModuleUtils.getOptionBoolean(module, "show own nametag", false);
                if (player == mc.player && showOwnNametag) {
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
        
        Component nameComponent;
        String baseName;
        if (forcePinkColor) {
            baseName = originalNameComponent.getString();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < baseName.length(); i++) {
                char c = baseName.charAt(i);
                if (c == '\u00A7' && i + 1 < baseName.length()) {
                    i++;
                    continue;
                }
                sb.append(c);
            }
            baseName = sb.toString();
            nameComponent = Component.literal(baseName).withStyle(Style.EMPTY.withColor(0xFFFF69B4));
        } else {
            nameComponent = originalNameComponent;
            baseName = nameComponent.getString();
        }
        
        Font textRenderer = mc.font;
        
        int totalWidth = textRenderer.width(nameComponent);
        if (isDeveloper) {
            totalWidth += textRenderer.width(DevConfig.DEV_PREFIX) + 2; 
        }
        
        int currentX;
        if (isDeveloper) {
            int nameWidth = textRenderer.width(nameComponent);
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
                distanceX = currentX + textRenderer.width(nameComponent) + 2;
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
}
