package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.MessageUtils;

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
                if (pepCatModule != null && pepCatModule.isEnabled() && ConfigManager.pepCatEnabled) {
                    LocalPlayer player = client.player;
                    if (player != null) {
                        float currentHealth = player.getHealth();
                        boolean isInWorld = client.level != null && client.player != null;
                        
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
                if (pepCatModule != null && pepCatModule.isEnabled() && ConfigManager.pepCatEnabled) {
                    if (isCurrentPlayerDeathMessage(message) && !overlay) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastDeathTime > 5000) {
                            lastDeathTime = currentTime;
                            LocalPlayer player = Minecraft.getInstance().player;
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
    
    private static final String[] ENGLISH_SECOND_PERSON_DEATH_SUBSTRINGS = new String[]{
        "you died",
        "you were killed by",
        "you were slain by",
        "you were blown up by",
        "you fell into the void",
        "you fell to your death",
        "you starved to death",
        "you burned to death",
        "you were burnt to a crisp",
        "you drowned",
        "you hit the ground too hard",
        "you were shot by",
        "you were slain",
        "you were killed"
    };

    private static boolean isCurrentPlayerDeathMessage(Component message) {
        String messageText = message.getString();

        if (messageText.indexOf(':') >= 0) {
            return false;
        }

        String lower = messageText.toLowerCase(java.util.Locale.ROOT);
        for (String pattern : ENGLISH_SECOND_PERSON_DEATH_SUBSTRINGS) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private static void onPlayerDeath(LocalPlayer player) {
        playTotemAnimation(player);
        sendEncouragementMessage(player);
    }
    
    private static void playTotemAnimation(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            if (client.level instanceof net.minecraft.client.multiplayer.ClientLevel) {
                net.minecraft.client.multiplayer.ClientLevel clientWorld = (net.minecraft.client.multiplayer.ClientLevel) client.level;
                clientWorld.playSound(
                    player, 
                    player.getX(), player.getY(), player.getZ(),
                    com.shyeuar.baity.client.Baity.LAUGHTER_SOUND,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f  
                );
            } else {
                net.minecraft.client.resources.sounds.SimpleSoundInstance soundInstance = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    com.shyeuar.baity.client.Baity.LAUGHTER_SOUND, 1.0f, 1.0f);
                client.getSoundManager().play(soundInstance);
            }
            
            ItemStack catItem = createCustomCatItem();
            client.gameRenderer.displayItemActivation(catItem);
            client.particleEngine.createTrackingEmitter(client.player, ParticleTypes.OMINOUS_SPAWNING, 10);
        }
    }
    
    private static ItemStack createCustomCatItem() {
        return new ItemStack(com.shyeuar.baity.items.CustomTotemItem.CUSTOM_TOTEM);
    }
    
    
    private static void sendEncouragementMessage(LocalPlayer player) {
        MutableComponent fullMessage = MessageUtils.createBaityPrefix()
            .append(MessageUtils.createColoredText("它张嘴大笑，似乎在笑你的失误，又或嘲笑死神的无能", 0x00FFFF))
            .append(MessageUtils.createColoredText("눈_눈", 0xFF80FF));

        MessageUtils.sendCustomMessage(fullMessage);
    }
}
