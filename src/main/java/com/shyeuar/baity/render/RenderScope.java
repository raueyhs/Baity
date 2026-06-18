package com.shyeuar.baity.render;

import com.shyeuar.baity.render.interfaces.CameraRenderStateInterface;
import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RenderScope {

    private static final Map<CameraRenderState, Integer> NAMETAG_ENTITY_BY_CAMERA = new IdentityHashMap<>();

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> WORLD_ENTITY_RENDER = ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Integer> NAME_TAG_ADD_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> NAME_TAG_ADD_WORLD = ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Integer> NAME_TAG_SUBMIT_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> NAME_TAG_SUBMIT_ENTITY_ID = ThreadLocal.withInitial(() -> -1);

    private static final ThreadLocal<Integer> WORLD_RENDER_PHASE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> HUD_RENDER_PHASE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> PAPER_DOLL_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);

    private RenderScope() {
    }

    public static void enterWorldRenderPhase() {
        WORLD_RENDER_PHASE_DEPTH.set(WORLD_RENDER_PHASE_DEPTH.get() + 1);
    }

    public static void exitWorldRenderPhase() {
        int depth = WORLD_RENDER_PHASE_DEPTH.get() - 1;
        if (depth <= 0) {
            WORLD_RENDER_PHASE_DEPTH.remove();
        } else {
            WORLD_RENDER_PHASE_DEPTH.set(depth);
        }
    }

    public static boolean isWorldRenderPhase() {
        return WORLD_RENDER_PHASE_DEPTH.get() > 0;
    }

    public static void enterHudRenderPhase() {
        HUD_RENDER_PHASE_DEPTH.set(HUD_RENDER_PHASE_DEPTH.get() + 1);
    }

    public static void exitHudRenderPhase() {
        int depth = HUD_RENDER_PHASE_DEPTH.get() - 1;
        if (depth <= 0) {
            HUD_RENDER_PHASE_DEPTH.remove();
        } else {
            HUD_RENDER_PHASE_DEPTH.set(depth);
        }
    }

    public static boolean isHudRenderPhase() {
        return HUD_RENDER_PHASE_DEPTH.get() > 0;
    }

    public static void enterPaperDollRender() {
        PAPER_DOLL_RENDER_DEPTH.set(PAPER_DOLL_RENDER_DEPTH.get() + 1);
    }

    public static void exitPaperDollRender() {
        int depth = PAPER_DOLL_RENDER_DEPTH.get() - 1;
        if (depth <= 0) {
            PAPER_DOLL_RENDER_DEPTH.remove();
        } else {
            PAPER_DOLL_RENDER_DEPTH.set(depth);
        }
    }

    public static boolean isPaperDollRender() {
        return PAPER_DOLL_RENDER_DEPTH.get() > 0;
    }

    public static boolean isEntityRenderScope() {
        return DEPTH.get() > 0;
    }

    public static void enter(CameraRenderState cameraState) {
        int depth = DEPTH.get();
        if (depth == 0) {
            WORLD_ENTITY_RENDER.set(isWorldEntityRender(cameraState));
        }
        DEPTH.set(depth + 1);
    }

    public static void exit() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
            WORLD_ENTITY_RENDER.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean shouldApplyWorldEntityChanges() {
        return DEPTH.get() > 0 && WORLD_ENTITY_RENDER.get();
    }

    public static boolean isWorldEntityRender(CameraRenderState cameraState) {
        if (!isWorldRenderPhase()) {
            return false;
        }
        if (cameraState == null) {
            return false;
        }
        if (cameraState instanceof CameraRenderStateInterface camera) {
            return camera.baity$isWorldCamera();
        }
        return false;
    }

    public static void bindNameTagEntity(CameraRenderState cameraState, int entityId) {
        if (cameraState == null) {
            return;
        }
        NAMETAG_ENTITY_BY_CAMERA.put(cameraState, entityId);
        if (cameraState instanceof CameraRenderStateInterface camera) {
            camera.baity$setNameTagEntityId(entityId);
        }
    }

    public static void clearNameTagEntity(CameraRenderState cameraState) {
        if (cameraState == null) {
            return;
        }
        NAMETAG_ENTITY_BY_CAMERA.remove(cameraState);
        if (cameraState instanceof CameraRenderStateInterface camera) {
            camera.baity$setNameTagEntityId(-1);
        }
    }

    public static void enterNameTagAdd(CameraRenderState cameraState) {
        int depth = NAME_TAG_ADD_DEPTH.get();
        if (depth == 0) {
            NAME_TAG_ADD_WORLD.set(isWorldEntityRender(cameraState));
        }
        NAME_TAG_ADD_DEPTH.set(depth + 1);
    }

    public static void exitNameTagAdd() {
        int depth = NAME_TAG_ADD_DEPTH.get() - 1;
        if (depth <= 0) {
            NAME_TAG_ADD_DEPTH.remove();
            NAME_TAG_ADD_WORLD.remove();
        } else {
            NAME_TAG_ADD_DEPTH.set(depth);
        }
    }

    public static void enterNameTagSubmit(int entityId) {
        int depth = NAME_TAG_SUBMIT_DEPTH.get();
        if (depth == 0) {
            NAME_TAG_SUBMIT_ENTITY_ID.set(entityId);
        }
        NAME_TAG_SUBMIT_DEPTH.set(depth + 1);
    }

    public static void exitNameTagSubmit() {
        int depth = NAME_TAG_SUBMIT_DEPTH.get() - 1;
        if (depth <= 0) {
            NAME_TAG_SUBMIT_DEPTH.remove();
            NAME_TAG_SUBMIT_ENTITY_ID.remove();
        } else {
            NAME_TAG_SUBMIT_DEPTH.set(depth);
        }
    }

    public static int getNameTagSubmitEntityId() {
        return NAME_TAG_SUBMIT_ENTITY_ID.get();
    }

    public static boolean isWorldNameTagAdd() {
        if (NAME_TAG_ADD_DEPTH.get() == 0) {
            return true;
        }
        return NAME_TAG_ADD_WORLD.get();
    }

    public static boolean isWorldCameraContext(LivingEntityRenderState renderState) {
        if (renderState instanceof EntityRenderStateInterface context) {
            return context.baity$isWorldCameraContext();
        }
        return true;
    }
}