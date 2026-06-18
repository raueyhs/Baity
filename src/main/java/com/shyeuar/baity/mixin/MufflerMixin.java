package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MufflerMixin {

    private static final Identifier ENDERMAN_SCREAM = Identifier.fromNamespaceAndPath("minecraft", "entity.enderman.scream");
    private static final Identifier ENDERMAN_STARE = Identifier.fromNamespaceAndPath("minecraft", "entity.enderman.stare");
    private static final Identifier PORTAL_AMBIENT = Identifier.fromNamespaceAndPath("minecraft", "block.portal.ambient");
    private static final Identifier ELDER_GUARDIAN_CURSE = Identifier.fromNamespaceAndPath("minecraft", "entity.elder_guardian.curse");
    private static final Identifier ELDER_GUARDIAN_AMBIENT = Identifier.fromNamespaceAndPath("minecraft", "entity.elder_guardian.ambient");
    private static final Identifier WITHER_SPAWN = Identifier.fromNamespaceAndPath("minecraft", "entity.wither.spawn");
    private static final Identifier TOTEM_USE = Identifier.fromNamespaceAndPath("minecraft", "item.totem.use");

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void baity$muteSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        Module m = ModuleManager.getModuleByName("Sounds");
        if (m == null || !m.isEnabled()) return;

        Identifier soundId = sound.getIdentifier();
        
        if (ModuleUtils.getOptionBoolean(m, "mute enderman scream", true)) {
            if (soundId.equals(ENDERMAN_SCREAM) || soundId.equals(ENDERMAN_STARE)) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        }
        
        if (ModuleUtils.getOptionBoolean(m, "mute phantom", true)) {
            if (isInGalatea() && soundId.getPath().startsWith("entity.phantom")) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        }
        
        if (ModuleUtils.getOptionBoolean(m, "mute portal", true)) {
            if (soundId.equals(PORTAL_AMBIENT)) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        }
        
        if (ModuleUtils.getOptionBoolean(m, "mute vampire", true)) {
            if (LocateUtils.inStillgoreChateau(Minecraft.getInstance())
                    && (soundId.equals(ELDER_GUARDIAN_CURSE) || soundId.equals(WITHER_SPAWN))) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        }
        
        if (ModuleUtils.getOptionBoolean(m, "mute drake", false)) {
            if (isInJerrysWorkshop() && soundId.equals(TOTEM_USE)) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        }

        if (ModuleUtils.getOptionBoolean(m, "mute wormhole", true)) {
            if (soundId.equals(ELDER_GUARDIAN_AMBIENT) && isWearingFroggles(Minecraft.getInstance())) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            }
        }
    }
    
    private static boolean isInGalatea() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        return LocateUtils.isGalatea(mc);
    }
    
    private static boolean isInJerrysWorkshop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        return "Jerry's Workshop".equals(LocateUtils.areaIslandName(mc));
    }

    private static boolean isWearingFroggles(Minecraft mc) {
        if (mc.player == null) return false;
        ItemStack head = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return false;
        String plainName = net.minecraft.ChatFormatting.stripFormatting(head.getHoverName().getString());
        return plainName.contains("Froggles");
    }
}
