package com.shyeuar.baity.features.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.shyeuar.baity.gui.modules.Module;
import com.shyeuar.baity.gui.modules.ModuleManager;
import com.shyeuar.baity.config.BaityConfig;
import com.shyeuar.baity.utils.MessageUtils;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class PepCat {
    private static boolean hasRegistered = false;
    private static float lastHealth = -1.0f; 
    private static boolean wasInWorld = false;
    private static long lastDeathTime = 0; 
    public static void init() {
        if (!hasRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                Module pepCatModule = ModuleManager.getModuleByName("PepCat");
                if (pepCatModule != null && pepCatModule.isEnabled() && BaityConfig.pepCatEnabled) {
                    ClientPlayerEntity player = client.player;
                    if (player != null) {
                        float currentHealth = player.getHealth();
                        boolean isInWorld = client.world != null && client.player != null;
                        
                        if (wasInWorld && isInWorld) {
                            if (lastHealth < 0) {
                                lastHealth = currentHealth;
                            }
                            else if (lastHealth > 0 && currentHealth <= 0) {
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastDeathTime > 5000) {
                                    lastDeathTime = currentTime;
                                    onPlayerDeath(player);
                                }
                            }
                        }
                        
                        wasInWorld = isInWorld;
                        lastHealth = currentHealth;
                    } else {
                        wasInWorld = false;
                        lastHealth = -1.0f; 
                    }
                }
            });
            
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                Module pepCatModule = ModuleManager.getModuleByName("PepCat");
                if (pepCatModule != null && pepCatModule.isEnabled() && BaityConfig.pepCatEnabled) {
                    if (isCurrentPlayerDeathMessage(message) && !overlay) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastDeathTime > 5000) {
                            lastDeathTime = currentTime;
                            ClientPlayerEntity player = MinecraftClient.getInstance().player;
                            if (player != null) {
                                onPlayerDeath(player);
                            }
                        }
                    }
                }
            });
            
            hasRegistered = true;
        }
    }
    
    private static boolean isCurrentPlayerDeathMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        String currentPlayerName = client.player.getGameProfile().getName();
        String messageText = message.getString();
        
        if (!isDeathMessage(message)) {
            return false;
        }
        
        return containsPlayerName(messageText, currentPlayerName);
    }
    
    private static boolean containsPlayerName(String message, String playerName) {
        if (message == null || playerName == null) return false;
        
        String lowerMessage = message.toLowerCase();
        String lowerPlayerName = playerName.toLowerCase();
        
        String pattern = "\\b" + Pattern.quote(lowerPlayerName) + "\\b";
        return lowerMessage.matches(".*" + pattern + ".*");
    }
    
    private static boolean isDeathMessage(Text message) {
        String messageText = message.getString().toLowerCase();
        
        String[] chineseDeathPatterns = {
            "死亡", "摔死", "淹死", "烧死", "炸死", "饿死", "被杀死", 
            "被炸死", "被烧死", "被淹死", "被摔死", "被饿死",
            "被", "杀死了", "炸死了", "烧死了", "淹死了", "摔死了", "饿死了"
        };
        
        String[] englishDeathPatterns = {
            "died", "death", "killed", "fell", "drowned", "burned", "exploded", 
            "starved", "suffocated", "was slain", "was shot", "was pricked",
            "was killed", "was blown up", "was burned", "was drowned", 
            "fell out of the world", "was squashed", "was killed by"
        };
        
        for (String pattern : chineseDeathPatterns) {
            if (messageText.contains(pattern)) {
                return true;
            }
        }
        
        for (String pattern : englishDeathPatterns) {
            if (messageText.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void onPlayerDeath(ClientPlayerEntity player) {
        playTotemAnimation(player);
        sendEncouragementMessage(player);
    }
    
    private static void playTotemAnimation(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            if (client.world instanceof net.minecraft.client.world.ClientWorld) {
                net.minecraft.client.world.ClientWorld clientWorld = (net.minecraft.client.world.ClientWorld) client.world;
                clientWorld.playSound(
                    player, 
                    player.getX(), player.getY(), player.getZ(),
                    com.shyeuar.baity.client.Baity.LAUGHTER_SOUND,
                    net.minecraft.sound.SoundCategory.PLAYERS,
                    1.0f,
                    1.0f  
                );
            } else {
                // 备用
                net.minecraft.client.sound.PositionedSoundInstance soundInstance = net.minecraft.client.sound.PositionedSoundInstance.master(
                    com.shyeuar.baity.client.Baity.LAUGHTER_SOUND, 1.0f, 1.0f);
                client.getSoundManager().play(soundInstance);
            }
            
            ItemStack catItem = createCustomCatItem();
            client.gameRenderer.showFloatingItem(catItem);
            client.particleManager.addEmitter(client.player, ParticleTypes.OMINOUS_SPAWNING, 10);
        }
    }
    
    private static ItemStack createCustomCatItem() {
        return new ItemStack(com.shyeuar.baity.item.CustomTotemItem.CUSTOM_TOTEM);
    }

    private static void sendEncouragementMessage(ClientPlayerEntity player) {
        MutableText fullMessage = MessageUtils.createBaityPrefix()
            .append(MessageUtils.createColoredText("它张嘴大笑，似乎在笑你的失误，又或嘲笑死神的无能", 0x00FFFF))
            .append(MessageUtils.createColoredText("눈_눈", 0xFF80FF));

        MessageUtils.sendCustomMessage(fullMessage);
    }
}

