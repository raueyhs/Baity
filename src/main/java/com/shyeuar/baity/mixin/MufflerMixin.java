package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MufflerMixin {

    private static final ResourceLocation ENDERMAN_SCREAM = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.enderman.scream");
    private static final ResourceLocation ENDERMAN_STARE = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.enderman.stare");
    private static final ResourceLocation PORTAL_AMBIENT = ResourceLocation.fromNamespaceAndPath("minecraft", "block.portal.ambient");
    private static final ResourceLocation ELDER_GUARDIAN_CURSE = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.elder_guardian.curse");
    private static final ResourceLocation WITHER_SPAWN = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.wither.spawn");
    private static final ResourceLocation TOTEM_USE = ResourceLocation.fromNamespaceAndPath("minecraft", "item.totem.use");

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void baity$muteSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        Module m = ModuleManager.getModuleByName("Muffler");
        if (m == null || !m.isEnabled()) return;

        ResourceLocation soundId = sound.getLocation();
        
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
            if (isInChateau() && (soundId.equals(ELDER_GUARDIAN_CURSE) || soundId.equals(WITHER_SPAWN))) {
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
    }
    
    private static boolean isInGalatea() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        return true;
    }
    
    private static boolean isInChateau() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        
        String location = getLocation();
        if (location == null) return false;
        
        return location.contains("Stillgore Château") || location.contains("Oubliette");
    }
    
    private static boolean isInJerrysWorkshop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        
        String area = getArea();
        if (area == null) return false;
        
        return area.equals("Jerry's Workshop");
    }
    
    private static String getLocation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        
        try {
            if (mc.gui != null && mc.gui.getTabList() != null) {
                net.minecraft.network.chat.Component footer = ((com.shyeuar.baity.mixin.PlayerListHudMixin) mc.gui.getTabList()).getFooter();
                if (footer != null) {
                    String footerText = footer.getString();
                    if (footerText.contains("⏣")) {
                        return footerText;
                    }
                }
            }
            
            if (mc.getConnection() != null) {
                var playerList = mc.getConnection().getOnlinePlayers();
                for (var entry : playerList) {
                    if (entry.getTabListDisplayName() != null) {
                        String name = entry.getTabListDisplayName().getString();
                        if (name.contains("⏣")) {
                            return name;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        return null;
    }
    
    private static String getArea() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null || mc.gui.getTabList() == null) return null;
        
        try {
            net.minecraft.network.chat.Component footer = ((com.shyeuar.baity.mixin.PlayerListHudMixin) mc.gui.getTabList()).getFooter();
            if (footer != null) {
                String footerText = footer.getString();
                if (footerText.contains("Area: ")) {
                    return footerText.substring(footerText.indexOf("Area: ") + 6).trim();
                }
            }
            
            if (mc.getConnection() != null) {
                var playerList = mc.getConnection().getOnlinePlayers();
                for (var entry : playerList) {
                    if (entry.getTabListDisplayName() != null) {
                        String name = entry.getTabListDisplayName().getString();
                        if (name.startsWith("Area: ")) {
                            return name.substring(6).trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        return null;
    }
}
