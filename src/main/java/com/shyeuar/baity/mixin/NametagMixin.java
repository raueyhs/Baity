package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import com.shyeuar.baity.features.smolpeople.SmolPeopleNametag;
import com.shyeuar.baity.utils.NametagUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class NametagMixin {

    private static final String SUBMIT_NAME_DISPLAY_OFFSET = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V";

    @Mixin(EntityRenderer.class)
    public abstract static class ExtractMixin<T extends Entity, S extends EntityRenderState> {

        @Inject(method = "extractRenderState", at = @At("TAIL"))
        private void baity$ensureOwnNameTagOnExtract(T entity, S state, float tickDelta, CallbackInfo ci) {
            if (state instanceof EntityRenderStateInterface context) {
                context.baity$setEntityId(entity.getId());
            }
            if (RenderScope.isPaperDollRender()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || entity != mc.player) {
                return;
            }
            NametagUtils.ensureOwnNameTag(state);
        }
    }

    @Mixin(EntityRenderer.class)
    public static class SubmitMixin {

        @Inject(
            method = SUBMIT_NAME_DISPLAY_OFFSET,
            at = @At("HEAD")
        )
        private void baity$beginNameTagSubmit(
            EntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            int lineOffset,
            CallbackInfo ci
        ) {
            if (RenderScope.isPaperDollRender()) {
                return;
            }
            int entityId = baity$resolveEntityId(state);
            RenderScope.enterNameTagSubmit(entityId);
            RenderScope.bindNameTagEntity(cameraState, entityId);
            NametagUtils.ensureOwnNameTag(state);
        }

        @Inject(
            method = SUBMIT_NAME_DISPLAY_OFFSET,
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$hideOriginalNameTag(
            EntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            int lineOffset,
            CallbackInfo ci
        ) {
            if (!(state instanceof LivingEntityRenderState livingState)) {
                return;
            }
            if (RenderScope.isPaperDollRender()) {
                ci.cancel();
                return;
            }
            if (!NametagUtils.isNametagModuleActive()) {
                return;
            }

            if (NametagUtils.isHudEntityRender(cameraState)) {
                RenderScope.exitNameTagSubmit();
                ci.cancel();
                RenderScope.clearNameTagEntity(cameraState);
                return;
            }

            int entityId = baity$resolveEntityId(livingState);
            Minecraft mc = Minecraft.getInstance();

            if (NametagUtils.isDefaultNametagMode()
                && !com.shyeuar.baity.features.FocusPlayerNametag.isActive()) {
                return;
            }

            if (mc.player == null || mc.level == null) {
                return;
            }

            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof Player player && NametagUtils.shouldSuppressVanillaNametag(player)) {
                RenderScope.exitNameTagSubmit();
                ci.cancel();
            }
        }

        @Inject(
            method = SUBMIT_NAME_DISPLAY_OFFSET,
            at = @At("RETURN")
        )
        private void baity$endNameTagSubmit(CallbackInfo ci) {
            RenderScope.exitNameTagSubmit();
        }

        private static int baity$resolveEntityId(EntityRenderState state) {
            if (state instanceof LivingEntityRenderState livingState) {
                return RenderScope.resolveLivingEntityRenderStateId(livingState);
            }
            return -1;
        }
    }

    @Mixin(EntityRenderer.class)
    public static class EntitySubmitMixin {

        @Inject(
            method = SUBMIT_NAME_DISPLAY_OFFSET,
            at = @At("HEAD")
        )
        private void baity$offsetMirrorArmorStandNametag(
            EntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            int lineOffset,
            CallbackInfo ci
        ) {
            if (RenderScope.isPaperDollRender()) {
                return;
            }
            int entityId = baity$resolveEntityRenderStateId(state);
            float offset = SmolPeopleNametag.getMirrorNametagArmorStandOffset(entityId);
            if (offset != 0f) {
                matrices.translate(0, offset, 0);
            }
        }

        private static int baity$resolveEntityRenderStateId(EntityRenderState state) {
            if (state instanceof EntityRenderStateInterface context) {
                int entityId = context.baity$getEntityId();
                if (entityId >= 0) {
                    return entityId;
                }
            }
            return -1;
        }
    }

    @Mixin(AvatarRenderer.class)
    public static class HideMixin {

        @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
        private void baity$disableVanillaNameTag(Avatar avatar, double d, CallbackInfoReturnable<Boolean> cir) {
            if (NametagUtils.isQueryingVanillaVisibility()) {
                return;
            }

            Module m = ModuleManager.getModuleByName("Nametag");
            if (m == null || !m.isEnabled()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            if (NametagUtils.isDefaultNametagMode()
                && !com.shyeuar.baity.features.FocusPlayerNametag.isActive()) {
                if (avatar == mc.player) {
                    cir.setReturnValue(true);
                }
                return;
            }

            if (avatar instanceof Player player && NametagUtils.shouldSuppressVanillaNametag(player)) {
                cir.setReturnValue(false);
            }
        }

        @Redirect(
            method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"
            ),
            require = 0
        )
        private static boolean baity$ignoreInvisibilityForDefaultNametag(LivingEntity entity, Player viewer) {
            if (!(entity instanceof Avatar avatar)) {
                return entity.isInvisibleTo(viewer);
            }
            Module m = ModuleManager.getModuleByName("Nametag");
            if (m == null || !m.isEnabled()) {
                return avatar.isInvisibleTo(viewer);
            }
            if (NametagUtils.isQueryingVanillaVisibility()
                || !NametagUtils.isDefaultNametagEnabled()
                || com.shyeuar.baity.features.FocusPlayerNametag.isActive()) {
                return avatar.isInvisibleTo(viewer);
            }
            return false;
        }
    }

    @Mixin(SubmitNodeCollection.class)
    public static class SubmitNodeCollectionMixin {

        @Shadow
        @Final
        public SimpleFeatureRenderPhase afterTerrain;

        @Redirect(
            method = {"submitTextPart(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font$DisplayMode;ILnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content;)V",
                      "submitNameTagPart(Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Submit;)V"},
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"
            )
        )
        private void baity$routeCustomNametagText(
                SimpleFeatureRenderPhase textsPhase,
                SubmitNode submit
        ) {
            if (RenderScope.isCustomNametagText()) {
                this.afterTerrain.submit(submit);
            } else {
                textsPhase.submit(submit);
            }
        }

        @Inject(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
        )
        private void baity$beginNameTagAdd(
            PoseStack poseStack,
            net.minecraft.world.phys.Vec3 vec3,
            int lineOffset,
            Component component,
            boolean seeThrough,
            int light,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            RenderScope.enterNameTagAdd(cameraState);
        }

        @Inject(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
        )
        private void baity$endNameTagAdd(
            PoseStack poseStack,
            net.minecraft.world.phys.Vec3 vec3,
            int lineOffset,
            Component component,
            boolean seeThrough,
            int light,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            RenderScope.exitNameTagAdd();
        }

        @ModifyVariable(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            argsOnly = true
        )
        private Component baity$useProcessedTextForLayout(Component originalComponent) {
            if (!NametagUtils.shouldApplyNametagLayoutCompat()) {
                return originalComponent;
            }
            return NametagUtils.applyNickProcessedText(originalComponent);
        }

        @Redirect(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;getBackgroundOpacity(F)F"
            )
        )
        private float baity$transparentizeOtherTags(
            net.minecraft.client.renderer.state.OptionsRenderState optionsState,
            float opacity
        ) {
            if (NametagUtils.isNametagModuleActive() && ConfigManager.nametagTransparentizeOtherTags) {
                return 0f;
            }
            return opacity;
        }
    }
}
