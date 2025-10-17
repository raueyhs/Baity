package com.shyeuar.baity.features.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import com.shyeuar.baity.gui.modules.Module;
import com.shyeuar.baity.gui.modules.ModuleManager;

@Environment(EnvType.CLIENT)
public class MeowAlert {
    private static boolean hasRegistered = false;
    private static long lastMeowTime = 0; 
    private static final long COOLDOWN_MS = 2000;
    private static final float VOLUME = 1.5F;
    private static final float PITCH = 1.0F;
    
    public static void init() {
        if (!hasRegistered) {
            ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                Module meowAlertModule = ModuleManager.getModuleByName("MeowAlert");
                if (meowAlertModule != null && meowAlertModule.isEnabled()) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null && sender != null) {
                        String currentPlayerName = client.player.getGameProfile().getName();
                        String senderName = sender.getName();
                        if (!currentPlayerName.equals(senderName)) {
                            checkForMention(message);
                        }
                    }
                }
            });
            
            hasRegistered = true;
        }
    }
    
    private static void checkForMention(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) return;
        
        String playerName = client.player.getGameProfile().getName();
        String messageText = message.getString();
        
        if (containsMention(messageText, playerName)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMeowTime >= COOLDOWN_MS) {
                lastMeowTime = currentTime;
                playMeowSound(client.player);
            }
        }
    }
    
    private static boolean containsMention(String message, String playerName) {
        String lowerMessage = message.toLowerCase();
        String lowerPlayerName = playerName.toLowerCase();
        
        String regex = "\\b" + java.util.regex.Pattern.quote(lowerPlayerName) + "\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(lowerMessage);
        
        if (matcher.find()) {
            return true;
        }
        
        String looseRegex = "\\b" + java.util.regex.Pattern.quote(lowerPlayerName) + "[a-zA-Z]";
        java.util.regex.Pattern loosePattern = java.util.regex.Pattern.compile(looseRegex);
        java.util.regex.Matcher looseMatcher = loosePattern.matcher(lowerMessage);
        
        return looseMatcher.find();
    }
    
    private static boolean isValidPlayerNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static void playMeowSound(ClientPlayerEntity player) {
        player.playSound(SoundEvents.ENTITY_CAT_AMBIENT, VOLUME, PITCH);
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, VOLUME, 1.5f);
    }
}
