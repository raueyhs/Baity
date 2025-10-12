package com.shyeuar.baity.features.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.shyeuar.baity.gui.modules.Module;
import com.shyeuar.baity.gui.modules.ModuleManager;
import com.shyeuar.baity.config.BaityConfig;
import com.shyeuar.baity.utils.MessageUtils;

@Environment(EnvType.CLIENT)
public class PepCat {
    private static boolean hasRegistered = false;
    private static float lastHealth = -1.0f; // 记录上次血量，初始化为-1表示未初始化
    private static boolean wasInWorld = false;
    private static long lastDeathTime = 0; // 记录上次死亡时间，防止重复触发
    
    public static void init() {
        if (!hasRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                Module pepCatModule = ModuleManager.getModuleByName("PepCat");
                if (pepCatModule != null && pepCatModule.isEnabled() && BaityConfig.pepCatEnabled) {
                    ClientPlayerEntity player = client.player;
                    if (player != null) {
                        float currentHealth = player.getHealth();
                        boolean isInWorld = client.world != null && client.player != null;
                        
                        // 血量变为0的瞬间检测（适用于原版非立即重生模式）
                        if (wasInWorld && isInWorld) {
                            // 首次进入世界时，初始化血量
                            if (lastHealth < 0) {
                                lastHealth = currentHealth;
                            }
                            // 检测生命值从正数变为0或负数的瞬间
                            else if (lastHealth > 0 && currentHealth <= 0) {
                                long currentTime = System.currentTimeMillis();
                                // 使用统一的防重复机制（5秒间隔）
                                if (currentTime - lastDeathTime > 5000) {
                                    lastDeathTime = currentTime;
                                    onPlayerDeath(player);
                                }
                            }
                        }
                        
                        // 更新状态
                        wasInWorld = isInWorld;
                        lastHealth = currentHealth;
                    } else {
                        // 玩家为空时重置状态
                        wasInWorld = false;
                        lastHealth = -1.0f; 
                    }
                }
            });
            
            // 注册聊天消息检测（适用于Skyblock和立即重生模式）
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                Module pepCatModule = ModuleManager.getModuleByName("PepCat");
                if (pepCatModule != null && pepCatModule.isEnabled() && BaityConfig.pepCatEnabled) {
                    if (isDeathMessage(message) && !overlay) {
                        long currentTime = System.currentTimeMillis();
                        // 防止5秒内重复触发
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
    
    /**
     * 检测是否为死亡消息
     * 支持中英文死亡消息格式
     */
    private static boolean isDeathMessage(Text message) {
        String messageText = message.getString().toLowerCase();
        
        // 中文死亡消息模式
        String[] chineseDeathPatterns = {
            "死亡", "摔死", "淹死", "烧死", "炸死", "饿死", "被杀死", 
            "被炸死", "被烧死", "被淹死", "被摔死", "被饿死",
            "被", "杀死了", "炸死了", "烧死了", "淹死了", "摔死了", "饿死了"
        };
        
        // 英文死亡消息模式
        String[] englishDeathPatterns = {
            "died", "death", "killed", "fell", "drowned", "burned", "exploded", 
            "starved", "suffocated", "was slain", "was shot", "was pricked",
            "was killed", "was blown up", "was burned", "was drowned", 
            "fell out of the world", "was squashed", "was killed by"
        };
        
        // 检查中文模式
        for (String pattern : chineseDeathPatterns) {
            if (messageText.contains(pattern)) {
                return true;
            }
        }
        
        // 检查英文模式
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
            try {
                // 使用ClientWorld.playSound播放音效
                if (client.world instanceof net.minecraft.client.world.ClientWorld) {
                    net.minecraft.client.world.ClientWorld clientWorld = (net.minecraft.client.world.ClientWorld) client.world;
                    clientWorld.playSound(
                        player, 
                        player.getX(), player.getY(), player.getZ(),
                        com.shyeuar.baity.client.Baity.LAUGHTER_SOUND,
                        net.minecraft.sound.SoundCategory.PLAYERS,
                        1.0f, // 音量
                        1.0f  // 音高
                    );
                } else {
                    // 备用方案：使用PositionedSoundInstance
                    net.minecraft.client.sound.PositionedSoundInstance soundInstance = net.minecraft.client.sound.PositionedSoundInstance.master(
                        com.shyeuar.baity.client.Baity.LAUGHTER_SOUND, 1.0f, 1.0f);
                    client.getSoundManager().play(soundInstance);
                }
            } catch (Exception e) {
                // 播放原版totem音效作为备用
                client.player.playSound(SoundEvents.ITEM_TOTEM_USE, 1.0f, 1.0f);
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
        MutableText prefix = MessageUtils.createBaityPrefix();
        MutableText mainText = MessageUtils.createColoredText("它张嘴大笑，似乎在笑你的失误，又或嘲笑死神的无能", Formatting.AQUA);
        MutableText emoji = MessageUtils.createColoredText("눈_눈", Formatting.LIGHT_PURPLE);

        MutableText fullMessage = MessageUtils.appendText(prefix, mainText);
        fullMessage = MessageUtils.appendText(fullMessage, emoji);

        MessageUtils.sendCustomMessage(fullMessage);
    }
}
