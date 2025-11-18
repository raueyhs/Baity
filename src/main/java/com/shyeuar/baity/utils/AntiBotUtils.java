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
    
    public static void updatePlayerMap() {
        if (mc.player == null || mc.world == null || mc.player.networkHandler == null) return;
        
        tickCount++;
        if (tickCount % 40 == 0) {
            playerMap.clear();
            
            for (UUID uuid : mc.player.networkHandler.getPlayerUuids()) {
                try {
                    var playerListEntry = mc.player.networkHandler.getPlayerListEntry(uuid);
                    if (playerListEntry == null) continue;
                    
                    String playerName = playerListEntry.getProfile().getName();
                    
                    if (playerName.startsWith("!")) {
                        continue;
                    }
                    
                    PlayerEntity worldPlayer = mc.world.getPlayerByUuid(uuid);
                    // 1.21.5: if (worldPlayer != null && worldPlayer.getStatusEffects().isEmpty()) { continue; }
                    // 1.21.8 版本后客户端不再可靠同步其他玩家的状态效果，继续使用该判断会把所有玩家标记为 Bot。
                    // 暂时跳过此过滤，确保 PlayerESP 等功能正常工作。TODO: 引入更可靠的识别方式。
                    
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

