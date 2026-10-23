package com.shyeuar.baity.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.fancydmgsplash.ElementalReactionDetector;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class FancyCreeperVeil implements LevelRenderEvents.AfterTranslucentFeatures {
    
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Identifier WITHER_CLOAK_SHIELD = Identifier.fromNamespaceAndPath("baity", "textures/item/wither_cloak_shield.png");
    
    public static long lastCreeperRender = 0;
    public static long lastDeactivate = System.currentTimeMillis();
    
    
    private static final double SHIELD_WIDTH = 0.8;
    private static final double SHIELD_HEIGHT = 2.0;
    private static final double ACCURACY = 4.0;
    private static final int SHIELD_COUNT = 6;
    private static final double SHIELD_SPEED = 2.0;
    private static final double SHIELD_DISTANCE = 1.2;
    
    
    @Override
    public void afterTranslucentFeatures(LevelRenderContext context) {
        Module module = ModuleManager.getModuleByName("FancyCreeperVeil");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.fancyCreeperVeilEnabled) return;
        
        if (MC.level == null || MC.player == null) return;
        
        boolean isCloakActive = ElementalReactionDetector.isUsingWitherCloak();
        
        if (isCloakActive) {
            long timeSinceLastCreeper = System.currentTimeMillis() - lastCreeperRender;
            if (timeSinceLastCreeper >= 2000 && lastCreeperRender > 0) {
                isCloakActive = false;
                lastDeactivate = System.currentTimeMillis();
                lastCreeperRender = 0;
                return;
            }
        }
        
        if (!isCloakActive) return;
        
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) return;
        
        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        float tickDelta = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 playerPos = MC.player.getPosition(tickDelta);
        
        matrices.pushPose();
        matrices.translate(playerPos.x - cameraPos.x, playerPos.y - cameraPos.y, playerPos.z - cameraPos.z);
        
        for (int i = 0; i < SHIELD_COUNT; i++) {
            double angle = (int) (((System.currentTimeMillis() / 30.0 * SHIELD_SPEED * -0.5 * ACCURACY) % (360 * ACCURACY)) / ACCURACY);
            angle += (360.0 / SHIELD_COUNT) * i;
            angle %= 360;
            
            double angleRad = Math.toRadians(angle);
            double posX = Math.cos(angleRad) * SHIELD_DISTANCE;
            double posZ = Math.sin(angleRad) * SHIELD_DISTANCE;
            
            matrices.pushPose();
            matrices.translate(posX, 0, posZ);
            double yawToPlayerRad = Math.atan2(-posX, -posZ);
            float yawToPlayerDeg = (float) Math.toDegrees(yawToPlayerRad);
            matrices.rotate(Axis.YP.rotationDegrees(yawToPlayerDeg));
            
            submits.submitCustomGeometry(
                    matrices,
                    RenderTypes.entityTranslucent(WITHER_CLOAK_SHIELD),
                    FancyCreeperVeil::renderShield
            );
            
            matrices.popPose();
        }
        
        matrices.popPose();
    }
    
    private static void renderShield(PoseStack.Pose pose, VertexConsumer consumer) {
        Matrix4f matrix = pose.pose();
        
        float width = (float) SHIELD_WIDTH;
        float height = (float) SHIELD_HEIGHT;
        float halfWidth = width / 2.0f;
        
        consumer.addVertex(matrix, -halfWidth, height, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(0, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
        
        consumer.addVertex(matrix, halfWidth, height, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(1, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
        
        consumer.addVertex(matrix, halfWidth, 0, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(1, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
        
        consumer.addVertex(matrix, -halfWidth, 0, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(0, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);

        consumer.addVertex(matrix, -halfWidth, height, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(1, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, -1);

        consumer.addVertex(matrix, -halfWidth, 0, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(1, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, -1);

        consumer.addVertex(matrix, halfWidth, 0, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(0, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, -1);

        consumer.addVertex(matrix, halfWidth, height, 0)
            .setColor(1.0f, 1.0f, 1.0f, 0.5f)
            .setUv(0, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, -1);
    }
    
    public static void onWorldUnload() {
        lastCreeperRender = 0;
        lastDeactivate = System.currentTimeMillis();
    }
  
}
