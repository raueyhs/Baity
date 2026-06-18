package com.shyeuar.baity.features.paperdoll;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.hud.HudElement;
import com.shyeuar.baity.gui.hud.HudManager;
import com.shyeuar.baity.gui.hud.HudPositionEditor;
import com.shyeuar.baity.mixin.accessor.LivingEntityAccessor;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class PaperDoll implements HudElement {

    private static final int BASE_WIDTH = 30;
    private static final int BASE_HEIGHT = 52;
    private static final float ENTITY_SCALE_PER_BOX_HEIGHT = 64.0f / 192.0f;
    private static final float ENTITY_SIZE_MULTIPLIER = 1.2f;
    private static final float DISPLAY_FACING_YAW = 180.0f;
    private static final float YAW_CHANGE_SPEED = 0.5f;
    private static final float YAW_RESTORE_SPEED = 20.0f;
    private static final float YAW_CHANGE_EPSILON = 1.0f;

    private static PaperDoll instance;

    private boolean selected;
    private boolean clicked;

    private float displayYaw = DISPLAY_FACING_YAW;
    private float displayYawSmooth = DISPLAY_FACING_YAW;
    private float lastRealtimeYaw = DISPLAY_FACING_YAW;
    private boolean yawChanged;
    private float displayPitch;

    private PaperDoll() {
    }

    public static PaperDoll getInstance() {
        if (instance == null) {
            instance = new PaperDoll();
            HudManager.getInstance().register(instance);
        }
        return instance;
    }

    public static void init() {
        getInstance();
    }

    public void clientTick() {
        if (!ConfigManager.paperDollEnabled || !ConfigManager.paperDollHeadRestore) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        tickRotationRestore();
    }

    @Override
    public String getId() {
        return "paperDoll";
    }

    @Override
    public String getDisplayName() {
        return "PaperDoll";
    }

    @Override
    public double getX() {
        return ConfigManager.paperDollX;
    }

    @Override
    public void setX(double x) {
        ConfigManager.paperDollX = x;
    }

    @Override
    public double getY() {
        return ConfigManager.paperDollY;
    }

    @Override
    public void setY(double y) {
        ConfigManager.paperDollY = y;
    }

    @Override
    public float getScale() {
        return ConfigManager.paperDollScale;
    }

    @Override
    public void setScale(float scale) {
        ConfigManager.paperDollScale = Math.max(0.1f, Math.min(10.0f, scale));
    }

    @Override
    public double getDefaultX() {
        return 0.10;
    }

    @Override
    public double getDefaultY() {
        return 0.35;
    }

    @Override
    public float getDefaultScale() {
        return 3.0f;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isClicked() {
        return clicked;
    }

    @Override
    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    @Override
    public int getWidth() {
        return BASE_WIDTH;
    }

    @Override
    public int getHeight() {
        return BASE_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTicks) {
        if (!shouldRender()) {
            return;
        }
        extractPaperDoll(guiGraphics, partialTicks);
    }

    @Override
    public boolean shouldRender() {
        if (!ConfigManager.paperDollEnabled) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (mc.screen instanceof HudPositionEditor) {
            return true;
        }
        return !mc.options.hideGui;
    }

    private void extractPaperDoll(GuiGraphics graphics, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity entity = mc.player;
        if (entity == null) {
            return;
        }

        EntityPoseBackup backup = EntityPoseBackup.capture(entity);
        RenderScope.enterPaperDollRender();
        try {
            if (ConfigManager.paperDollHeadRestore) {
                updateRotation(entity.getYRot(), entity.getXRot());
            }
            transformEntity(entity, partialTicks);
            if (NoSwimPoseUtils.isFeatureActive()) {
                clearSwimFields(entity);
            }

            EntityRenderState entityState = extractRenderState(entity, partialTicks);
            if (entityState instanceof AvatarRenderState avatar && NoSwimPoseUtils.isFeatureActive()) {
                NoSwimPoseUtils.clearSwimRenderState(avatar);
            }
            entityState.nameTag = null;
            Vector3f offset = new Vector3f(0.0f, getPaperDollOffsetY(entity), 0.0f);
            renderPaperDoll(graphics, entityState, offset);
        } finally {
            RenderScope.exitPaperDollRender();
            backup.restore();
        }
    }

    private void renderPaperDoll(
            GuiGraphics graphics,
            EntityRenderState entityState,
            Vector3f offset
    ) {
        int boxW = getDummyWidth(false);
        int boxH = getDummyHeight(false);
        int boxLeft = getAbsX(boxW);
        int boxTop = getAbsY(boxH);
        int centerX = boxLeft + boxW / 2;
        int centerY = boxTop + boxH / 2;

        int renderW = Math.round(boxW * ENTITY_SIZE_MULTIPLIER);
        int renderH = Math.round(boxH * ENTITY_SIZE_MULTIPLIER);
        int renderLeft = centerX - renderW / 2;
        int renderTop = centerY - renderH / 2;

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.PI);
        rotation.rotateY((float) Math.toRadians(ConfigManager.paperDollFacingAngle));

        graphics.submitEntityRenderState(
                entityState,
                boxH * ENTITY_SCALE_PER_BOX_HEIGHT * ENTITY_SIZE_MULTIPLIER,
                offset,
                rotation,
                null,
                renderLeft,
                renderTop,
                renderLeft + renderW,
                renderTop + renderH
        );
    }

    private void tickRotationRestore() {
        if (yawChanged) {
            return;
        }
        displayYawSmooth = displayYaw;
        float yawLimit = ConfigManager.paperDollHeadYawRange;
        float delta = (1.0f + Mth.sin((float) (Math.PI / 2.0 * (Math.abs(DISPLAY_FACING_YAW - displayYaw) / yawLimit))))
                * YAW_RESTORE_SPEED;
        if (displayYaw > DISPLAY_FACING_YAW) {
            displayYaw = Math.max(displayYaw - delta, DISPLAY_FACING_YAW);
        } else if (displayYaw < DISPLAY_FACING_YAW) {
            displayYaw = Math.min(displayYaw + delta, DISPLAY_FACING_YAW);
        }
    }

    private void updateRotation(float realtimeYaw, float realtimePitch) {
        float yawLimit = ConfigManager.paperDollHeadYawRange;
        float pitchLimit = ConfigManager.paperDollHeadPitchRange;

        yawChanged = Math.abs(lastRealtimeYaw - realtimeYaw) > YAW_CHANGE_EPSILON;
        if (yawChanged) {
            float deltaYaw = Mth.wrapDegrees(realtimeYaw - lastRealtimeYaw) * YAW_CHANGE_SPEED;
            displayYaw += deltaYaw;
            displayYaw = Mth.clamp(displayYaw, DISPLAY_FACING_YAW - yawLimit, DISPLAY_FACING_YAW + yawLimit);
            displayYawSmooth = displayYaw;
        }
        lastRealtimeYaw = realtimeYaw;
        displayPitch = Mth.clamp(realtimePitch, -pitchLimit, pitchLimit);
    }

    private void transformEntity(LivingEntity entity, float partialTicks) {
        if (NoSwimPoseUtils.isFeatureActive()) {
            entity.setPose(entity.isCrouching() ? Pose.CROUCHING : Pose.STANDING);
        } else if (!entity.isSwimming() && !entity.isFallFlying() && !entity.isVisuallyCrawling()) {
            entity.setPose(entity.isCrouching() ? Pose.CROUCHING : Pose.STANDING);
        }

        float headYawRange = ConfigManager.paperDollHeadYawRange;
        float pitchRange = ConfigManager.paperDollHeadPitchRange;
        float dollBodyYaw = DISPLAY_FACING_YAW;
        float dollHeadYaw;

        if (ConfigManager.paperDollHeadRestore) {
            dollHeadYaw = Mth.lerp(partialTicks, displayYawSmooth, displayYaw);
            entity.setXRot(entity.xRotO = displayPitch);
        } else {
            float bodyLerp = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            float headLerp = Mth.lerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
            float headDelta = Mth.clamp(Mth.wrapDegrees(headLerp - bodyLerp), -headYawRange, headYawRange);
            dollHeadYaw = dollBodyYaw + headDelta;

            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            entity.setXRot(entity.xRotO = Mth.clamp(pitch, -pitchRange, pitchRange));
        }

        entity.yBodyRot = entity.yBodyRotO = dollBodyYaw;
        entity.yHeadRot = entity.yHeadRotO = dollHeadYaw;
        entity.setYRot(entity.yRotO = dollHeadYaw);
    }

    private static float getPaperDollOffsetY(LivingEntity entity) {
        float standingHeight = entity.getDimensions(Pose.STANDING).height() * entity.getScale();
        return standingHeight * 0.5f + 0.0625f;
    }

    private static void clearSwimFields(LivingEntity entity) {
        LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
        accessor.baity$setSwimAmountField(0.0f);
        accessor.baity$setSwimAmountOField(0.0f);
    }

    private EntityRenderState extractRenderState(Entity entity, float partialTicks) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super Entity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, partialTicks);
        state.lightCoords = LightTexture.pack(15, 15);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        return state;
    }

    private static final class EntityPoseBackup {
        private final LivingEntity entity;
        private Pose pose;
        private float yRot;
        private float yRotO;
        private float yBodyRot;
        private float yBodyRotO;
        private float yHeadRot;
        private float yHeadRotO;
        private float xRot;
        private float xRotO;
        private float swimAmount;
        private float swimAmountO;

        private EntityPoseBackup(LivingEntity entity) {
            this.entity = entity;
        }

        static EntityPoseBackup capture(LivingEntity entity) {
            LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
            EntityPoseBackup backup = new EntityPoseBackup(entity);
            backup.pose = entity.getPose();
            backup.yRot = entity.getYRot();
            backup.yRotO = entity.yRotO;
            backup.yBodyRot = entity.yBodyRot;
            backup.yBodyRotO = entity.yBodyRotO;
            backup.yHeadRot = entity.yHeadRot;
            backup.yHeadRotO = entity.yHeadRotO;
            backup.xRot = entity.getXRot();
            backup.xRotO = entity.xRotO;
            backup.swimAmount = accessor.baity$getSwimAmountField();
            backup.swimAmountO = accessor.baity$getSwimAmountOField();
            return backup;
        }

        void restore() {
            LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
            entity.setPose(pose);
            entity.setYRot(yRot);
            entity.yRotO = yRotO;
            entity.yBodyRot = yBodyRot;
            entity.yBodyRotO = yBodyRotO;
            entity.yHeadRot = yHeadRot;
            entity.yHeadRotO = yHeadRotO;
            entity.setXRot(xRot);
            entity.xRotO = xRotO;
            accessor.baity$setSwimAmountField(swimAmount);
            accessor.baity$setSwimAmountOField(swimAmountO);
        }
    }
}