package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class AntiBotUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Map<String, String> playerMap = new HashMap<>();
    private static int tickCount = 0;
    
    /**
     * 更新玩家映射，每40tick执行一次
     * 使用网络处理器获取所有玩家，包括隐藏的玩家
     */
    public static void updatePlayerMap() {
        if (mc.player == null || mc.world == null || mc.player.networkHandler == null) return;
        
        tickCount++;
        if (tickCount % 40 == 0) {
            playerMap.clear();
            
            // 使用网络处理器获取所有玩家UUID（包括隐藏的玩家）
            for (UUID uuid : mc.player.networkHandler.getPlayerUuids()) {
                try {
                    var playerListEntry = mc.player.networkHandler.getPlayerListEntry(uuid);
                    if (playerListEntry == null) continue;
                    
                    String playerName = playerListEntry.getProfile().getName();
                    
                    // 检测方法1：名称前缀检测（Hypixel NPC通常以!开头）
                    if (playerName.startsWith("!")) {
                        continue;
                    }
                    
                    // 检测方法2：状态效果检测（真实玩家通常有状态效果）
                    PlayerEntity worldPlayer = mc.world.getPlayerByUuid(uuid);
                    if (worldPlayer != null && worldPlayer.getStatusEffects().isEmpty()) {
                        continue;
                    }
                    
                    // 检测方法3：UUID格式检测
                    try {
                        UUID.fromString(uuid.toString());
                    } catch (IllegalArgumentException e) {
                        continue; 
                    }
                    
                    if (worldPlayer != null) {
                        playerMap.put(uuid.toString(), playerName);
                    }
                } catch (Exception e) {
                    // 忽略异常，继续处理下一个玩家
                    continue;
                }
            }
        }
    }

    public static boolean isRealPlayer(PlayerEntity player) {
        if (player == null || player == mc.player) return true; 
        
        String uuid = player.getUuid().toString();

        if (playerMap.isEmpty()) {
            return true;
        }
        
        return playerMap.containsKey(uuid);
    }

    public static boolean isBot(PlayerEntity player) {
        return !isRealPlayer(player);
    }
    
    public static void reset() {
        playerMap.clear();
        tickCount = 0;
    }
}

