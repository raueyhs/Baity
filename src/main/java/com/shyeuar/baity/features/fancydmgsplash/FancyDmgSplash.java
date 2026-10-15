package com.shyeuar.baity.features.fancydmgsplash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.FloatingWorldTextCompat;
import com.shyeuar.baity.utils.LocateUtils;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class FancyDmgSplash implements LevelRenderEvents.EndMain {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final List<DamageNumber> damageNumbers = new ArrayList<>();
    private static final List<ReactionText> reactionTexts = new ArrayList<>();
    private static final Random random = new Random();
    
    private static final long ANIMATION_DURATION_MS = 1200; 
    private static final float PHASE1_RATIO = 0.4f;
    private static final float BASE_SCALE = 0.0325f; 
    private static final float PHASE2_FLOAT_UP = 0.18f; 
    private static final float PHASE2_DISPLAY_RATIO = 0.25f;
    private static final float ARC_DRIFT_SCALE = 0.8f;
    private static final float ARC_PEAK_HEIGHT = 0.25f;
    private static final float ARC_RISE_DRIFT = 0.12f;
    private static final float SLAM_DROP_HEIGHT = 0.35f;
    private static final float SLAM_DROP_RATIO = 0.2f;
    private static final float SLAM_BOUNCE_RATIO = 0.15f;
    private static final float BOUNCE_ACTIVE_RATIO = 0.42f;
    private static final float BOUNCE_PEAK_HEIGHT = 0.14f;
    private static final float BOUNCE_HEIGHT_DECAY = 0.55f;
    private static final int BOUNCE_COUNT = 3;
    private static final float SHAKE_CRIT_AMPLITUDE = 0.045f;
    private static final float SHAKE_FREQUENCY = 18f;
    private static final float SHAKE_ACTIVE_RATIO = 0.35f;
    private static final float FALL_DISTANCE = 0.38f;
    private static final float SPIRAL_RADIUS = 0.18f;
    private static final float SPIRAL_RISE = 0.22f;
    private static final float SPIRAL_TURNS = 1.25f;

    private record AnimationFrame(Vec3 pos, float alpha, float scaleAnimation) {
    }
    
    private static long lastWetHealCheckTime = 0;
    private static final long WET_HEAL_CHECK_INTERVAL_MS = 500;

    public static void addDamageNumber(double damage, Vec3 targetPos, Component originalText) {
        Module moduleRef = ModuleManager.getModuleByName("FancyDmgSplash");
        if (moduleRef == null || !moduleRef.isEnabled()) return;
        if (mc.player == null || mc.gameRenderer == null) return;
        if (originalText == null) return;

        float targetRandomX = (random.nextFloat() - 0.5f) * 1.8f;
        float targetRandomY = (random.nextFloat() - 0.2f) * 1.4f;
        float targetRandomZ = (random.nextFloat() - 0.5f) * 1.8f;
        Vec3 finalTargetPos = targetPos.add(targetRandomX, targetRandomY, targetRandomZ);

        FancyDmgSplashPresetStore.PresetData style = FancyDmgSplashPresetStore.pickRandomForDamage();
        FancyDmgSplashSettings.DamageKind kind = FancyDmgSplashSettings.classifyDamage(originalText);
        boolean syncEnabled = FancyDmgSplashSettings.isSyncNonCriticalEnabled();

        Component formattedText;
        int color;
        boolean preserveComponentColors;

        if (kind == FancyDmgSplashSettings.DamageKind.CRITICAL) {
            formattedText = FancyDmgSplashSettings.formatCriticalDamage(originalText, damage, style);
            color = style.primaryColor();
            preserveComponentColors = true;
        } else if (kind == FancyDmgSplashSettings.DamageKind.PLAIN_NORMAL && syncEnabled) {
            formattedText = FancyDmgSplashSettings.formatSyncNonCrit(originalText, damage, style);
            color = style.primaryColor();
            preserveComponentColors = true;
        } else {
            formattedText = FancyDmgSplashSettings.formatLegacyNonCrit(originalText, damage);
            if (formattedText == null) {
                formattedText = originalText;
            }
            color = FancyDmgSplashSettings.extractColorFromText(formattedText);
            preserveComponentColors = false;
        }

        float arcDriftX = (random.nextFloat() - 0.5f) * 1.2f;
        float arcDriftZ = (random.nextFloat() - 0.5f) * 1.2f;
        float motionPhase = random.nextFloat() * ((float) Math.PI * 2f);

        damageNumbers.add(new DamageNumber(damage, finalTargetPos, formattedText, color,
                System.currentTimeMillis(), preserveComponentColors, arcDriftX, arcDriftZ, kind, motionPhase));

        if (FancyDmgSplashSettings.isGenshinReactionEnabled()) {
            Integer reactionColor = resolveReactionColor(kind, style, syncEnabled);
            if (reactionColor != null) {
                ElementalReactionDetector.ReactionResult reaction = kind == FancyDmgSplashSettings.DamageKind.BURN
                        ? ElementalReactionDetector.recordForcedElementDamage(reactionColor, targetPos)
                        : ElementalReactionDetector.recordDamageAndCheckReaction(reactionColor, targetPos);
                if (reaction != null) {
                    Vec3 reactionPos = finalTargetPos.add(0.3, 0.15, 0);
                    reactionTexts.add(new ReactionText(reaction.name, reactionPos, reaction.color, System.currentTimeMillis()));
                }
            }
        }
    }

    private static Integer resolveReactionColor(FancyDmgSplashSettings.DamageKind kind,
                                                FancyDmgSplashPresetStore.PresetData style,
                                                boolean syncEnabled) {
        if (kind == FancyDmgSplashSettings.DamageKind.BURN) {
            return ElementalReactionDetector.PYRO;
        }
        if (kind == FancyDmgSplashSettings.DamageKind.CRITICAL) {
            if (!FancyDmgSplashPresetStore.isReactionEligible(style)) {
                return null;
            }
            return FancyDmgSplashPresetStore.resolveReactionElementColor(style);
        }
        if (kind == FancyDmgSplashSettings.DamageKind.PLAIN_NORMAL && syncEnabled) {
            if (!FancyDmgSplashPresetStore.isReactionEligible(style)) {
                return null;
            }
            return FancyDmgSplashPresetStore.resolveReactionElementColor(style);
        }
        return null;
    }

    public static void addImmuneReaction(Vec3 targetPos) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (!FancyDmgSplashSettings.isGenshinReactionEnabled()) return;
        
        ElementalReactionDetector.ReactionResult result = 
            ElementalReactionDetector.checkImmuneReaction(targetPos, false);
        if (result != null) {
            reactionTexts.add(new ReactionText(result.name, targetPos, result.color, System.currentTimeMillis()));
        }
    }
    
    public static void addPlayerImmuneReaction() {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.player == null) return;
        if (!FancyDmgSplashSettings.isGenshinReactionEnabled()) return;
        
        Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getBbHeight() * 0.5, mc.player.getZ());
        ElementalReactionDetector.ReactionResult result = 
            ElementalReactionDetector.checkPlayerImmuneItem(playerPos);
        if (result != null) {
            reactionTexts.add(new ReactionText(result.name, playerPos, result.color, System.currentTimeMillis()));
        }
    }
    
    public static void addWitherCloakImmuneReaction(ElementalReactionDetector.ReactionResult result) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (!FancyDmgSplashSettings.isGenshinReactionEnabled()) return;
        if (result == null || result.position == null) return;

        reactionTexts.add(new ReactionText(result.name, result.position, result.color, System.currentTimeMillis()));
    }
    
    @Override
    public void endMain(LevelRenderContext context) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.level == null || mc.player == null) return;

        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) return;
        
        long currentTime = System.currentTimeMillis();
        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.mainCamera();
        float cameraYaw = camera.yRot();
        float cameraPitch = camera.xRot();

        // TODO(ModernUI-26.2): ModernUI font compat may need deferred submit tracking once ModernUI 26.2 is out.
        FloatingWorldTextCompat.beginFrame();
        try {
            if (currentTime - lastWetHealCheckTime >= WET_HEAL_CHECK_INTERVAL_MS) {
                lastWetHealCheckTime = currentTime;
                checkWetAndHealReactions();
            }

            Iterator<DamageNumber> iterator = damageNumbers.iterator();
            while (iterator.hasNext()) {
                DamageNumber dn = iterator.next();
                if (currentTime - dn.startTime > ANIMATION_DURATION_MS) {
                    iterator.remove();
                }
            }

            Iterator<ReactionText> reactionIterator = reactionTexts.iterator();
            while (reactionIterator.hasNext()) {
                ReactionText rt = reactionIterator.next();
                if (currentTime - rt.startTime > ANIMATION_DURATION_MS) {
                    reactionIterator.remove();
                }
            }

            if (!shouldHideInDungeon()) {
                for (DamageNumber dn : damageNumbers) {
                    renderDamageNumber(matrices, submits, dn, cameraPos, cameraYaw, cameraPitch, currentTime);
                }

                for (ReactionText rt : reactionTexts) {
                    renderReactionText(matrices, submits, rt, cameraPos, cameraYaw, cameraPitch, currentTime);
                }
            }
        } finally {
            FloatingWorldTextCompat.endFrame();
        }
    }

    private static boolean shouldHideInDungeon() {
        if (LocateUtils.isInDungeonBossRoom(mc)) {
            return ConfigManager.fancyDmgSplashHideInDungeonBoss;
        }
        if (LocateUtils.isInDungeonRun(mc)) {
            return ConfigManager.fancyDmgSplashHideInDungeonClear;
        }
        return false;
    }

    private static void checkWetAndHealReactions() {
        if (mc.level == null || mc.player == null) return;
        if (!FancyDmgSplashSettings.isGenshinReactionEnabled()) return;
        
        Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double range = 32.0;
        
        AABB searchBox = new AABB(
            playerPos.x - range, playerPos.y - range, playerPos.z - range,
            playerPos.x + range, playerPos.y + range, playerPos.z + range
        );
        
        List<Entity> entities = mc.level.getEntities(null, searchBox);
        
        checkEntityReactions(mc.player, playerPos);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                checkEntityReactions(livingEntity, playerPos);
            }
        }
    }
    
    private static void checkEntityReactions(LivingEntity entity, Vec3 playerPos) {
        ElementalReactionDetector.ReactionResult wetResult = 
            ElementalReactionDetector.checkEntityWetState(entity, playerPos);
        if (wetResult != null && wetResult.position != null) {
            reactionTexts.add(new ReactionText(wetResult.name, wetResult.position, 
                wetResult.color, System.currentTimeMillis()));
        }
        
        ElementalReactionDetector.ReactionResult burningResult = 
            ElementalReactionDetector.checkEntityBurningState(entity, playerPos);
        if (burningResult != null && burningResult.position != null) {
            reactionTexts.add(new ReactionText(burningResult.name, burningResult.position, 
                burningResult.color, System.currentTimeMillis()));
        }
        
        ElementalReactionDetector.ReactionResult healResult = 
            ElementalReactionDetector.checkHealReaction(entity);
        if (healResult != null && healResult.position != null) {
            reactionTexts.add(new ReactionText(healResult.name, healResult.position, 
                healResult.color, System.currentTimeMillis()));
        }
    }
    
    private void renderDamageNumber(
            PoseStack matrices,
            SubmitNodeCollector submits,
            DamageNumber dn,
            Vec3 cameraPos,
            float cameraYaw,
            float cameraPitch,
            long currentTime
    ) {
        float totalProgress = (float)(currentTime - dn.startTime) / ANIMATION_DURATION_MS;
        totalProgress = Math.min(1.0f, Math.max(0.0f, totalProgress));

        AnimationFrame frame = computeDamageAnimationFrame(dn, totalProgress);
        Vec3 currentPos = frame.pos;
        float alpha = frame.alpha;
        float scaleAnimation = frame.scaleAnimation;
        
        double x = currentPos.x - cameraPos.x;
        double y = currentPos.y - cameraPos.y;
        double z = currentPos.z - cameraPos.z;
        
        float damageScale;
        if (dn.damage < 1000) {
            damageScale = 0.65f; 
        } else if (dn.damage < 10000) {
            damageScale = 0.7f;
        } else if (dn.damage < 100000) {
            damageScale = 0.72f;
        } else if (dn.damage < 500000) {
            damageScale = 0.78f;
        } else if (dn.damage < 900000) {
            damageScale = 0.88f;
        } else if (dn.damage < 1200000) {
            float progress = (float)(dn.damage - 900000) / 300000f;
            damageScale = 0.88f + progress * 0.27f;
        } else {
            damageScale = 1.15f;
        }
        
        float finalScale = BASE_SCALE * scaleAnimation * damageScale;
        
        matrices.pushPose();
        try {
            matrices.translate(x, y, z);
            
            matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.mulPose(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            
            matrices.scale(-finalScale, -finalScale, finalScale);
            
            Component styledText;
            int color;
            
            if (dn.originalText != null) {
                styledText = dn.originalText;
            } else {
                String damageText = String.valueOf((long) dn.damage);
                styledText = Component.literal(damageText);
            }
            color = dn.color;
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = dn.preserveComponentColors
                    ? alphaInt | 0xFFFFFF
                    : (color & 0x00FFFFFF) | alphaInt;
            
            Font textRenderer = mc.font;
            int textWidth = textRenderer.width(styledText);

            FloatingWorldTextCompat.drawInBatch(
                    textRenderer,
                    styledText,
                    -textWidth / 2.0f,
                    0,
                    finalColor,
                    matrices,
                    submits,
                    15728880
            );
        } finally {
            matrices.popPose();
        }
    }
    
    
    private void renderReactionText(
            PoseStack matrices,
            SubmitNodeCollector submits,
            ReactionText rt,
            Vec3 cameraPos,
            float cameraYaw,
            float cameraPitch,
            long currentTime
    ) {
        float totalProgress = (float)(currentTime - rt.startTime) / ANIMATION_DURATION_MS;
        totalProgress = Math.min(1.0f, Math.max(0.0f, totalProgress));

        AnimationFrame frame = computeReactionAnimationFrame(rt.pos, totalProgress);
        Vec3 currentPos = frame.pos;
        float alpha = frame.alpha;
        float scaleAnimation = frame.scaleAnimation;
        
        double x = currentPos.x - cameraPos.x;
        double y = currentPos.y - cameraPos.y;
        double z = currentPos.z - cameraPos.z;
        
        float finalScale = BASE_SCALE * scaleAnimation * 1.15f * 0.5f;
        
        matrices.pushPose();
        try {
            matrices.translate(x, y, z);
            
            matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.mulPose(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            
            matrices.scale(-finalScale, -finalScale, finalScale);
            
            Component styledText = Component.literal(rt.text);
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = (rt.color & 0x00FFFFFF) | alphaInt;
            
            Font textRenderer = mc.font;
            int textWidth = textRenderer.width(styledText);

            FloatingWorldTextCompat.drawInBatch(
                    textRenderer,
                    styledText,
                    -textWidth / 2.0f,
                    0,
                    finalColor,
                    matrices,
                    submits,
                    15728880
            );
        } finally {
            matrices.popPose();
        }
    }

    private static AnimationFrame computeDamageAnimationFrame(DamageNumber dn, float totalProgress) {
        String style = FancyDmgSplashSettings.getAnimationStyle();
        if (FancyDmgSplashSettings.STYLE_GENSHIN.equalsIgnoreCase(style)) {
            return computeGenshinAnimationFrame(dn.targetPos, totalProgress, 1.0f, 0.5f, 1.0f, 1.5f, 0.5f);
        }
        if (FancyDmgSplashSettings.STYLE_ARC.equalsIgnoreCase(style)) {
            return computeArcAnimationFrame(dn, totalProgress);
        }
        if (FancyDmgSplashSettings.STYLE_SLAM.equalsIgnoreCase(style)) {
            return computeSlamAnimationFrame(dn, totalProgress);
        }
        if (FancyDmgSplashSettings.STYLE_BOUNCE.equalsIgnoreCase(style)) {
            return computeBounceAnimationFrame(dn, totalProgress);
        }
        if (FancyDmgSplashSettings.STYLE_SHAKE.equalsIgnoreCase(style)) {
            return computeShakeAnimationFrame(dn, totalProgress);
        }
        if (FancyDmgSplashSettings.STYLE_FALL.equalsIgnoreCase(style)) {
            return computeFallAnimationFrame(dn, totalProgress);
        }
        if (FancyDmgSplashSettings.STYLE_SPIRAL.equalsIgnoreCase(style)) {
            return computeSpiralAnimationFrame(dn, totalProgress);
        }
        return new AnimationFrame(dn.targetPos, 1.0f, 1.0f);
    }

    private static AnimationFrame computeReactionAnimationFrame(Vec3 basePos, float totalProgress) {
        return computeGenshinAnimationFrame(basePos, totalProgress, 0.8f, 0.5f, 0.8f, 1.3f, 0.3f);
    }

    private static AnimationFrame computeArcAnimationFrame(DamageNumber dn, float totalProgress) {
        float eased = easeOutQuad(totalProgress);
        float horizontal = eased * ARC_DRIFT_SCALE;
        double offsetX = dn.arcDriftX * horizontal;
        double offsetZ = dn.arcDriftZ * horizontal;
        double offsetY = Math.sin(totalProgress * Math.PI) * ARC_PEAK_HEIGHT + totalProgress * ARC_RISE_DRIFT;

        float scaleAnimation = 1.0f;
        if (totalProgress < 0.15f) {
            scaleAnimation = computePopScale(totalProgress / 0.15f, 0.5f, 1.0f, 1.5f, 0.5f);
        }

        float alpha = computeTailFadeAlpha(totalProgress, 0.75f);
        Vec3 pos = dn.targetPos.add(offsetX, offsetY, offsetZ);
        return new AnimationFrame(pos, alpha, scaleAnimation);
    }

    private static AnimationFrame computeSlamAnimationFrame(DamageNumber dn, float totalProgress) {
        if (dn.kind != FancyDmgSplashSettings.DamageKind.CRITICAL) {
            return computeSimpleFloatFadeFrame(dn.targetPos, totalProgress, 1.0f);
        }

        float dropEnd = SLAM_DROP_RATIO;
        float bounceEnd = dropEnd + SLAM_BOUNCE_RATIO;
        double offsetY;
        float scaleAnimation;

        if (totalProgress < dropEnd) {
            float dropProgress = totalProgress / dropEnd;
            float easedDrop = dropProgress * dropProgress * dropProgress;
            offsetY = SLAM_DROP_HEIGHT * (1.0f - easedDrop);
            scaleAnimation = 1.2f - easedDrop * 0.1f;
        } else if (totalProgress < bounceEnd) {
            float bounceProgress = (totalProgress - dropEnd) / SLAM_BOUNCE_RATIO;
            offsetY = -0.02f * (1.0f - bounceProgress);
            scaleAnimation = 1.1f + (float) Math.sin(bounceProgress * Math.PI) * 0.25f;
        } else {
            float settleProgress = (totalProgress - bounceEnd) / (1.0f - bounceEnd);
            float easedFloat = easeOutQuad(settleProgress);
            offsetY = easedFloat * 0.08f;
            scaleAnimation = 1.0f;
        }

        float alpha = computeTailFadeAlpha(totalProgress, 0.7f);
        Vec3 pos = dn.targetPos.add(0, offsetY, 0);
        return new AnimationFrame(pos, alpha, scaleAnimation);
    }

    private static AnimationFrame computeBounceAnimationFrame(DamageNumber dn, float totalProgress) {
        double offsetY;
        float scaleAnimation = 1.0f;

        if (totalProgress < BOUNCE_ACTIVE_RATIO) {
            float local = totalProgress / BOUNCE_ACTIVE_RATIO;
            float bounceSlot = local * BOUNCE_COUNT;
            int bounceIndex = Math.min(BOUNCE_COUNT - 1, (int) bounceSlot);
            float inBounce = bounceSlot - bounceIndex;
            float peak = BOUNCE_PEAK_HEIGHT * (float) Math.pow(BOUNCE_HEIGHT_DECAY, bounceIndex);
            offsetY = peak * (4.0 * inBounce * (1.0 - inBounce));
            scaleAnimation = 1.0f + (float) Math.sin(inBounce * Math.PI) * 0.1f * (peak / BOUNCE_PEAK_HEIGHT);
        } else {
            float settleProgress = (totalProgress - BOUNCE_ACTIVE_RATIO) / (1.0f - BOUNCE_ACTIVE_RATIO);
            offsetY = easeOutQuad(settleProgress) * 0.06f;
        }

        float alpha = computeTailFadeAlpha(totalProgress, 0.68f);
        Vec3 pos = dn.targetPos.add(0, offsetY, 0);
        return new AnimationFrame(pos, alpha, scaleAnimation);
    }

    private static AnimationFrame computeShakeAnimationFrame(DamageNumber dn, float totalProgress) {
        if (dn.kind != FancyDmgSplashSettings.DamageKind.CRITICAL) {
            return new AnimationFrame(dn.targetPos, 1.0f, 1.0f);
        }

        float shakeProgress = Math.min(1.0f, totalProgress / SHAKE_ACTIVE_RATIO);
        float decay = 1.0f - easeOutQuad(shakeProgress);
        float amplitude = SHAKE_CRIT_AMPLITUDE * decay;

        float angle = dn.motionPhase + shakeProgress * SHAKE_FREQUENCY * ((float) Math.PI * 2f);
        double offsetX = Math.sin(angle) * amplitude;
        double offsetZ = Math.cos(angle * 1.31f) * amplitude * 0.5f;

        float alpha = computeTailFadeAlpha(totalProgress, 0.75f);
        Vec3 pos = dn.targetPos.add(offsetX, 0, offsetZ);
        return new AnimationFrame(pos, alpha, 1.0f);
    }

    private static AnimationFrame computeFallAnimationFrame(DamageNumber dn, float totalProgress) {
        float easedFall = totalProgress * totalProgress;
        double offsetY = -FALL_DISTANCE * easedFall;
        float alpha = computeTailFadeAlpha(totalProgress, 0.55f);
        Vec3 pos = dn.targetPos.add(0, offsetY, 0);
        return new AnimationFrame(pos, alpha, 1.0f);
    }

    private static AnimationFrame computeSpiralAnimationFrame(DamageNumber dn, float totalProgress) {
        float radius = SPIRAL_RADIUS * (1.0f - totalProgress * 0.35f);
        float angle = dn.motionPhase + totalProgress * SPIRAL_TURNS * ((float) Math.PI * 2f);
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = easeOutQuad(totalProgress) * SPIRAL_RISE;

        float scaleAnimation = 1.0f;
        if (totalProgress < 0.12f) {
            scaleAnimation = computePopScale(totalProgress / 0.12f, 0.85f, 0.3f, 1.15f, 0.15f);
        }

        float alpha = computeTailFadeAlpha(totalProgress, 0.72f);
        Vec3 pos = dn.targetPos.add(offsetX, offsetY, offsetZ);
        return new AnimationFrame(pos, alpha, scaleAnimation);
    }

    private static AnimationFrame computeSimpleFloatFadeFrame(Vec3 basePos, float totalProgress, float floatUpScale) {
        float easedFloat = easeOutQuad(totalProgress);
        Vec3 pos = basePos.add(0, easedFloat * PHASE2_FLOAT_UP * floatUpScale, 0);
        float alpha = computeTailFadeAlpha(totalProgress, 0.6f);
        return new AnimationFrame(pos, alpha, 1.0f);
    }

    private static float computePopScale(float phaseProgress, float scaleStart, float scaleMidBoost, float scalePeak, float scaleEndReduction) {
        if (phaseProgress < 0.3f) {
            return scaleStart + phaseProgress / 0.3f * scaleMidBoost;
        }
        if (phaseProgress < 0.6f) {
            return scalePeak - (phaseProgress - 0.3f) / 0.3f * scaleEndReduction;
        }
        return 1.0f;
    }

    private static float computeTailFadeAlpha(float totalProgress, float fadeStart) {
        if (totalProgress < fadeStart) {
            return 1.0f;
        }
        float fadeProgress = (totalProgress - fadeStart) / (1.0f - fadeStart);
        float acceleratedFade = Math.min(1.0f, fadeProgress * 2.0f);
        return 1.0f - acceleratedFade * acceleratedFade;
    }

    private static float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private static AnimationFrame computeGenshinAnimationFrame(
            Vec3 basePos,
            float totalProgress,
            float floatUpScale,
            float scaleStart,
            float scaleMidBoost,
            float scalePeak,
            float scaleEndReduction
    ) {
        if (totalProgress < PHASE1_RATIO) {
            float phase1Progress = totalProgress / PHASE1_RATIO;
            float scaleAnimation = computePopScale(phase1Progress, scaleStart, scaleMidBoost, scalePeak, scaleEndReduction);
            return new AnimationFrame(basePos, 1.0f, scaleAnimation);
        }

        float phase2Progress = (totalProgress - PHASE1_RATIO) / (1.0f - PHASE1_RATIO);
        float easedFloatProgress = 1.0f - (1.0f - phase2Progress) * (1.0f - phase2Progress);
        float floatUp = easedFloatProgress * PHASE2_FLOAT_UP * floatUpScale;
        Vec3 currentPos = basePos.add(0, floatUp, 0);

        float alpha = 1.0f;
        if (phase2Progress >= PHASE2_DISPLAY_RATIO) {
            float fadeProgress = (phase2Progress - PHASE2_DISPLAY_RATIO) / (1.0f - PHASE2_DISPLAY_RATIO);
            float acceleratedFade = Math.min(1.0f, fadeProgress * 2.0f);
            alpha = 1.0f - acceleratedFade * acceleratedFade;
        }
        return new AnimationFrame(currentPos, alpha, 1.0f);
    }
    
    private static class DamageNumber {
        final double damage;
        final Vec3 targetPos;
        final Component originalText;
        final int color;
        final long startTime;
        final boolean preserveComponentColors;
        final float arcDriftX;
        final float arcDriftZ;
        final FancyDmgSplashSettings.DamageKind kind;
        final float motionPhase;

        DamageNumber(double damage, Vec3 targetPos, Component originalText, int color, long startTime,
                       boolean preserveComponentColors, float arcDriftX, float arcDriftZ,
                       FancyDmgSplashSettings.DamageKind kind, float motionPhase) {
            this.damage = damage;
            this.targetPos = targetPos;
            this.originalText = originalText;
            this.color = color;
            this.startTime = startTime;
            this.preserveComponentColors = preserveComponentColors;
            this.arcDriftX = arcDriftX;
            this.arcDriftZ = arcDriftZ;
            this.kind = kind;
            this.motionPhase = motionPhase;
        }
    }
    
    private static class ReactionText {
        final String text;
        final Vec3 pos;
        final int color;
        final long startTime;
        
        ReactionText(String text, Vec3 pos, int color, long startTime) {
            this.text = text;
            this.pos = pos;
            this.color = color;
            this.startTime = startTime;
        }
    }
}
