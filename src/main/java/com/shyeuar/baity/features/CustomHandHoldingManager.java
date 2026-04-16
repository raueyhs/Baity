package com.shyeuar.baity.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.world.entity.HumanoidArm;

public class CustomHandHoldingManager {

    private abstract static class Animation<T> {
        private final long duration;
        private AnimationState state;

        private static class AnimationState {
            long startTime;
            boolean reversed;

            AnimationState(long startTime, boolean reversed) {
                this.startTime = startTime;
                this.reversed = reversed;
            }
        }

        public Animation(long duration) {
            this.duration = duration;
        }

        public void start() {
            long currentTime = System.currentTimeMillis();
            if (state == null) {
                state = new AnimationState(currentTime, false);
                return;
            }

            float percent = Math.max(0f, Math.min(1f, (currentTime - state.startTime) / (float) duration));
            state.reversed = !state.reversed;
            state.startTime = currentTime - (long) ((1f - percent) * duration);
        }

        public float getPercent() {
            if (state == null) return 100f;

            float percent = ((System.currentTimeMillis() - state.startTime) / (float) duration * 100f);
            if (percent >= 100f) {
                state = null;
                return 100f;
            }
            return Math.min(percent, 100f);
        }

        public boolean isAnimating() {
            return state != null;
        }

        public abstract T get(T start, T end, boolean reverse);
    }

    @SuppressWarnings("unchecked")
    private static class LinearAnimation<E extends Number & Comparable<E>> extends Animation<E> {
        
        public LinearAnimation(long duration) {
            super(duration);
        }

        @Override
        public E get(E start, E end, boolean reverse) {
            E startVal = reverse ? end : start;
            E endVal = reverse ? start : end;

            if (!isAnimating()) {
                return reverse ? start : end;
            }

            float startFloat = startVal.floatValue();
            float endFloat = endVal.floatValue();
            float percent = getPercent() / 100f;
            float result = startFloat + (endFloat - startFloat) * percent;

            if (start instanceof Double) {
                return (E) Double.valueOf(result);
            } else if (start instanceof Float) {
                return (E) Float.valueOf(result);
            } else if (start instanceof Integer) {
                return (E) Integer.valueOf(Math.round(result));
            } else if (start instanceof Long) {
                return (E) Long.valueOf(Math.round(result));
            } else {
                return (E) Float.valueOf(result);
            }
        }
    }
    private static final CustomHandHoldingManager INSTANCE = new CustomHandHoldingManager();
    
    private final LinearAnimation<Float> positionXAnim = new LinearAnimation<>(200);
    private final LinearAnimation<Float> positionYAnim = new LinearAnimation<>(200);
    private final LinearAnimation<Float> positionZAnim = new LinearAnimation<>(200);
    private final LinearAnimation<Float> rotationXAnim = new LinearAnimation<>(200);
    private final LinearAnimation<Float> rotationYAnim = new LinearAnimation<>(200);
    private final LinearAnimation<Float> rotationZAnim = new LinearAnimation<>(200);
    private final LinearAnimation<Float> scaleAnim = new LinearAnimation<>(200);
    
    private float currentPosX = 0f;
    private float currentPosY = 0f;
    private float currentPosZ = 0f;
    private float currentRotX = 0f;
    private float currentRotY = 0f;
    private float currentRotZ = 0f;
    private float currentScale = 1.0f; 
    private float targetPosX = 0f;
    private float targetPosY = 0f;
    private float targetPosZ = 0f;
    private float targetRotX = 0f;
    private float targetRotY = 0f;
    private float targetRotZ = 0f;
    private float targetScale = 1.0f; 
    
    private CustomHandHoldingManager() {}
    
    public static CustomHandHoldingManager getInstance() {
        return INSTANCE;
    }

    public void update() {
        updateTargetValues();
        
        updateAnimations();
    }
    
    private void updateTargetValues() {
        targetPosX = (float) ConfigManager.customHandHoldingPosX;
        targetPosY = (float) ConfigManager.customHandHoldingPosY;
        targetPosZ = (float) ConfigManager.customHandHoldingPosZ;
        targetRotX = (float) ConfigManager.customHandHoldingRotX;
        targetRotY = (float) ConfigManager.customHandHoldingRotY;
        targetRotZ = (float) ConfigManager.customHandHoldingRotZ;
        float scaleValue = (float) ConfigManager.customHandHoldingScale;
 
        targetScale = scaleValue;
    }
    
    private void updateAnimations() {
        if (Math.abs(currentPosX - targetPosX) > 0.001f) {
            if (!positionXAnim.isAnimating()) positionXAnim.start();
            currentPosX = positionXAnim.get(currentPosX, targetPosX, false);
        }
        
        if (Math.abs(currentPosY - targetPosY) > 0.001f) {
            if (!positionYAnim.isAnimating()) positionYAnim.start();
            currentPosY = positionYAnim.get(currentPosY, targetPosY, false);
        }
        
        if (Math.abs(currentPosZ - targetPosZ) > 0.001f) {
            if (!positionZAnim.isAnimating()) positionZAnim.start();
            currentPosZ = positionZAnim.get(currentPosZ, targetPosZ, false);
        }
        
        if (Math.abs(currentRotX - targetRotX) > 0.001f) {
            if (!rotationXAnim.isAnimating()) rotationXAnim.start();
            currentRotX = rotationXAnim.get(currentRotX, targetRotX, false);
        }
        
        if (Math.abs(currentRotY - targetRotY) > 0.001f) {
            if (!rotationYAnim.isAnimating()) rotationYAnim.start();
            currentRotY = rotationYAnim.get(currentRotY, targetRotY, false);
        }
        
        if (Math.abs(currentRotZ - targetRotZ) > 0.001f) {
            if (!rotationZAnim.isAnimating()) rotationZAnim.start();
            currentRotZ = rotationZAnim.get(currentRotZ, targetRotZ, false);
        }
        
        if (Math.abs(currentScale - targetScale) > 0.001f) {
            if (!scaleAnim.isAnimating()) scaleAnim.start();
            currentScale = scaleAnim.get(currentScale, targetScale, false);
        }
    }

    public void applyTransform(PoseStack poseStack, HumanoidArm hand) {
        applyPosition(poseStack, hand);
        applyRotation(poseStack, hand);
        applyScale(poseStack);
    }

    public void applyPositionAndRotation(PoseStack poseStack, HumanoidArm hand) {
        applyPosition(poseStack, hand);
        applyRotation(poseStack, hand);
    }

    public void applyPosition(PoseStack poseStack, HumanoidArm hand) {
        int handDirection = hand == HumanoidArm.RIGHT ? 1 : -1;
        
        poseStack.translate(
            currentPosX * handDirection,
            currentPosY,
            currentPosZ
        );
    }

    public void applyRotation(PoseStack poseStack, HumanoidArm hand) {
        int handDirection = hand == HumanoidArm.RIGHT ? 1 : -1;

        poseStack.mulPose(Axis.XP.rotationDegrees(currentRotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(currentRotY * handDirection));
        poseStack.mulPose(Axis.ZP.rotationDegrees(currentRotZ * handDirection));
    }

    public void applyScale(PoseStack poseStack) {
        poseStack.scale(currentScale, currentScale, currentScale);
    }

    public int getSwingDuration() {
        return (int) ConfigManager.swingDuration;
    }

    public boolean isNoSwingEnabled() {
        return ConfigManager.customHandHoldingNoSwing;
    }
}

