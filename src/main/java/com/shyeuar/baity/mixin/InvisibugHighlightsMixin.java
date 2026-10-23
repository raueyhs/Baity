package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.highlights.InvisibugHighlights;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.LocateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class InvisibugHighlightsMixin {

    @Mixin(ClientPacketListener.class)
    public static class ParticlePacketMixin {

        private static final Minecraft MC = Minecraft.getInstance();

        @Inject(
                method = "handleParticleEvent",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                        shift = At.Shift.AFTER
                )
        )
        private void baity$onCritParticleForInvisibug(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
            Module module = ModuleManager.getModuleByName("Highlights");
            if (module == null || !module.isEnabled()) return;
            if (!ConfigManager.highlightsInvisibugEnabled) return;

            if (MC.level == null || MC.player == null) return;

            if (!LocateUtils.isGalatea(MC)) return;

            if (packet.particle().getType() != ParticleTypes.CRIT) return;

            if (packet.count() != 1) return;

            try {
                java.lang.reflect.Method getSpeedMethod = null;
                java.lang.reflect.Method getOffsetXMethod = null;
                java.lang.reflect.Method getOffsetYMethod = null;
                java.lang.reflect.Method getOffsetZMethod = null;
                java.lang.reflect.Method isImportantMethod = null;
                java.lang.reflect.Method shouldForceSpawnMethod = null;

                for (java.lang.reflect.Method method : packet.getClass().getMethods()) {
                    String name = method.getName();
                    if (getSpeedMethod == null && (name.equals("getSpeed") || name.equals("speed"))) {
                        getSpeedMethod = method;
                    }
                    if (getOffsetXMethod == null && (name.equals("getOffsetX") || name.equals("offsetX") || name.equals("getXOffset"))) {
                        getOffsetXMethod = method;
                    }
                    if (getOffsetYMethod == null && (name.equals("getOffsetY") || name.equals("offsetY") || name.equals("getYOffset"))) {
                        getOffsetYMethod = method;
                    }
                    if (getOffsetZMethod == null && (name.equals("getOffsetZ") || name.equals("offsetZ") || name.equals("getZOffset"))) {
                        getOffsetZMethod = method;
                    }
                    if (isImportantMethod == null && (name.equals("isImportant") || name.equals("important"))) {
                        isImportantMethod = method;
                    }
                    if (shouldForceSpawnMethod == null && (name.equals("shouldForceSpawn") || name.equals("forceSpawn"))) {
                        shouldForceSpawnMethod = method;
                    }
                }

                if (getSpeedMethod != null) {
                    float speed = ((Number) getSpeedMethod.invoke(packet)).floatValue();
                    if (speed != 0.0f) return;
                }

                if (getOffsetXMethod != null && getOffsetYMethod != null && getOffsetZMethod != null) {
                    float offsetX = ((Number) getOffsetXMethod.invoke(packet)).floatValue();
                    float offsetY = ((Number) getOffsetYMethod.invoke(packet)).floatValue();
                    float offsetZ = ((Number) getOffsetZMethod.invoke(packet)).floatValue();
                    if (offsetX != 0.0f || offsetY != 0.0f || offsetZ != 0.0f) return;
                }

                if (isImportantMethod != null) {
                    boolean important = (Boolean) isImportantMethod.invoke(packet);
                    if (!important) return;
                }

                if (shouldForceSpawnMethod != null) {
                    boolean forceSpawn = (Boolean) shouldForceSpawnMethod.invoke(packet);
                    if (!forceSpawn) return;
                }
            } catch (Exception e) {
                return;
            }

            Vec3 particleLocation = new Vec3(packet.x(), packet.y(), packet.z());

            InvisibugHighlights.onParticleAt(particleLocation);
        }
    }

    @Mixin(Entity.class)
    public static class EntityRemovalMixin {

        @Inject(method = "remove", at = @At("HEAD"))
        private void baity$removeInvisibugTrackingOnRemove(CallbackInfo ci) {
            Entity self = (Entity) (Object) this;
            if (self instanceof ArmorStand armorStand) {
                InvisibugHighlights.removeTrackedMarker(armorStand);
            }
        }
    }
}