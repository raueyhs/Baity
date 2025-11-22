package com.shyeuar.baity.features;

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
                if (pepCatModule != null && pepCatModule.isEnabled() && ConfigManager.pepCatEnabled) {
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
        
        String messageText = message.getString();
        return isSecondPersonEnglishDeathMessage(messageText);
    }
    
    private static final String[] ENGLISH_SECOND_PERSON_DEATH_PREFIXES = {
        "you died",
        "you were killed",
        "you were slain",
        "you were shot",
        "you were blown up",
        "you were pricked",
        "you were squashed",
        "you were crushed",
        "you were impaled",
        "you were doomed to fall",
        "you were struck by lightning",
        "you were doomed to fall by",
        "you were slain by",
        "you were killed by",
        "you were slain as",
        "you were knocked into the void",
        "you were consumed",
        "you were incinerated",
        "you fell",
        "you hit the ground too hard",
        "you discovered the floor was lava",
        "you drowned",
        "you suffocated",
        "you suffocated in a wall",
        "you burned",
        "you burned to death",
        "you went up in flames",
        "you tried to swim in lava",
        "you blew up",
        "you froze to death",
    };
    
    private static boolean isSecondPersonEnglishDeathMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return false;
        }
        
        String normalized = rawMessage.toLowerCase();
        for (String fragment : ENGLISH_SECOND_PERSON_DEATH_PREFIXES) {
            if (normalized.contains(fragment)) {
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
        return new ItemStack(com.shyeuar.baity.items.CustomTotemItem.CUSTOM_TOTEM);
    }
    
    
    private static void sendEncouragementMessage(ClientPlayerEntity player) {
        MutableText fullMessage = MessageUtils.createBaityPrefix()
            .append(MessageUtils.createColoredText("它张嘴大笑，似乎在笑你的失误，又或嘲笑死神的无能", 0x00FFFF))
            .append(MessageUtils.createColoredText("눈_눈", 0xFF80FF));

        MessageUtils.sendCustomMessage(fullMessage);
    }
}
