package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.utils.AntiBotUtils;
import com.shyeuar.baity.utils.ModuleUtils;
import com.shyeuar.baity.utils.NametagUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class NametagMixin {

    @Mixin(EntityRenderer.class)
    public abstract static class ExtractMixin<T extends Entity, S extends EntityRenderState> {

        @Inject(method = "extractRenderState", at = @At("TAIL"))
        private void baity$ensureOwnNameTagOnExtract(T entity, S state, float tickDelta, CallbackInfo ci) {
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

    @Mixin(LivingEntityRenderer.class)
    public static class SubmitMixin {

        @Inject(
            method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD")
        )
        private void baity$beginNameTagSubmit(
            LivingEntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
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
            method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$hideOriginalNameTag(
            LivingEntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            if (RenderScope.isPaperDollRender()) {
                ci.cancel();
                return;
            }
            if (!NametagUtils.isNametagModuleActive()) {
                return;
            }

            if (NametagUtils.isHudEntityRender(cameraState)) {
                ci.cancel();
                RenderScope.clearNameTagEntity(cameraState);
                return;
            }

            int entityId = baity$resolveEntityId(state);
            Minecraft mc = Minecraft.getInstance();

            if (NametagUtils.isDefaultNametagMode()) {
                return;
            }

            if (mc.player == null || mc.level == null) {
                return;
            }

            boolean isSelf = mc.player.getId() == entityId;
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof Player player && AntiBotUtils.isBot(player)) {
                ci.cancel();
                return;
            }

            Module m = ModuleManager.getModuleByName("Nametag");
            if (isSelf && ModuleUtils.getOptionBoolean(m, "show own nametag", false)) {
                ci.cancel();
            } else if (!isSelf) {
                ci.cancel();
            }
        }

        @ModifyVariable(
            method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("STORE"),
            ordinal = 0
        )
        private Component baity$applyNickTweaksBeforeLayout(Component originalComponent, LivingEntityRenderState state) {
            if (!NametagUtils.shouldProcessNametagText()) {
                return originalComponent;
            }
            return NametagUtils.applyNickProcessedText(originalComponent);
        }

        @Inject(
            method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("RETURN")
        )
        private void baity$endNameTagSubmit(CallbackInfo ci) {
            RenderScope.exitNameTagSubmit();
        }

        private static int baity$resolveEntityId(LivingEntityRenderState state) {
            if (state instanceof AvatarRenderState avatarRenderState) {
                return avatarRenderState.id;
            }
            return RenderScope.getNameTagSubmitEntityId();
        }
    }

    @Mixin(AvatarRenderer.class)
    public static class HideMixin {

        @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
        private void baity$disableVanillaNameTag(Avatar avatar, double d, CallbackInfoReturnable<Boolean> cir) {
            Module m = ModuleManager.getModuleByName("Nametag");
            if (m == null || !m.isEnabled()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            if (NametagUtils.isDefaultNametagMode()) {
                if (avatar == mc.player) {
                    cir.setReturnValue(true);
                }
                return;
            }

            if (avatar == mc.player) {
                if (ModuleUtils.getOptionBoolean(m, "show own nametag", false)) {
                    cir.setReturnValue(false);
                }
                return;
            }

            cir.setReturnValue(false);
        }

        @Redirect(
            method = "shouldShowName",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/Avatar;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"
            ),
            require = 0
        )
        private boolean baity$ignoreInvisibilityForDefaultNametag(Avatar avatar, Player viewer) {
            Module m = ModuleManager.getModuleByName("Nametag");
            if (m == null || !m.isEnabled()) {
                return avatar.isInvisibleTo(viewer);
            }
            if (!NametagUtils.isDefaultNametagEnabled()) {
                return avatar.isInvisibleTo(viewer);
            }
            return false;
        }
    }

    @Mixin(NameTagFeatureRenderer.Storage.class)
    public static class StorageMixin {

        @Inject(
            method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD")
        )
        private void baity$beginNameTagAdd(
            PoseStack poseStack,
            net.minecraft.world.phys.Vec3 vec3,
            int lineOffset,
            Component component,
            boolean seeThrough,
            int light,
            double distanceSq,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            RenderScope.enterNameTagAdd(cameraState);
        }

        @Inject(
            method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("RETURN")
        )
        private void baity$endNameTagAdd(
            PoseStack poseStack,
            net.minecraft.world.phys.Vec3 vec3,
            int lineOffset,
            Component component,
            boolean seeThrough,
            int light,
            double distanceSq,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            RenderScope.exitNameTagAdd();
        }

        @ModifyVariable(
            method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"),
            argsOnly = true
        )
        private Component baity$useProcessedTextForLayout(Component originalComponent) {
            if (!NametagUtils.shouldApplyNametagLayoutCompat()) {
                return originalComponent;
            }
            return NametagUtils.applyNickProcessedText(originalComponent);
        }

        @ModifyVariable(
            method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("STORE"),
            ordinal = 0
        )
        private float baity$recalculateCenteredOffset(
            float originalOffset,
            PoseStack poseStack,
            net.minecraft.world.phys.Vec3 vec3,
            int lineOffset,
            Component component,
            boolean seeThrough,
            int light,
            double distanceSq,
            CameraRenderState cameraState
        ) {
            if (!NametagUtils.shouldApplyNametagLayoutCompat() || component == null) {
                return originalOffset;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.font == null) {
                return originalOffset;
            }
            Component target = NametagUtils.applyNickProcessedText(component);
            return -mc.font.width(target.getVisualOrderText()) / 2.0F;
        }

        @Redirect(
            method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(
                value = "NEW",
                target = "net/minecraft/client/renderer/SubmitNodeStorage$NameTagSubmit"
            )
        )
        private SubmitNodeStorage.NameTagSubmit baity$createNameTagSubmit(
            Matrix4f pose,
            float x,
            float y,
            Component text,
            int lightCoords,
            int color,
            int backgroundColor,
            double distanceToCameraSq
        ) {
            int finalBackgroundColor = backgroundColor;
            if (NametagUtils.isNametagModuleActive() && ConfigManager.nametagTransparentizeOtherTags) {
                finalBackgroundColor = 0;
            }

            return new SubmitNodeStorage.NameTagSubmit(
                pose,
                x,
                y,
                text,
                lightCoords,
                color,
                finalBackgroundColor,
                distanceToCameraSq
            );
        }
    }
}
