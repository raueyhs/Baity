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
                    
                    try {
                        UUID.fromString(uuid.toString());
                    } catch (IllegalArgumentException e) {
                        continue; 
                    }
                    
                    PlayerEntity worldPlayer = mc.world.getPlayerByUuid(uuid);
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
