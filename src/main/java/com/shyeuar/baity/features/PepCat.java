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
    private static int lastRandomMessageIndex = -1;
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
        int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(7);
        if (idx == lastRandomMessageIndex && 7 > 1) {
            idx = (idx + 1) % 7;
        }
        lastRandomMessageIndex = idx;

        String text;
        String emoji;
        switch (idx) {
            case 0 -> {
                text = "它张嘴大笑，似乎在嘲笑你的失误，又或嘲笑死神的无能...";
                emoji = "(´-ι_-｀)";
            }
            case 1 -> {
                text = "死神能带走你的命，但带不走你的倔强...";
                emoji = "(°∀°)ﾉ";
            }
            case 2 -> {
                text = "身体重新拼凑的感觉并不好受，像是把一堆零件强行塞进太小的盒子里，顺便还弄丢了几枚硬币...";
                emoji = "(ﾟ∀。)";
            }
            case 3 -> {
                text = "不是因为怜悯，是因为你老死在这儿...";
                emoji = "(￣^￣)ゞ";
            }
            case 4 -> {
                text = "用一种比较激烈的方式，测试一下这个世界的硬度。结论：挺硬...";
                emoji = "(´;ω;)";
            }
            case 5 -> {
                text = "屏幕前的你喝了一口水，屏幕里的你也站了起来。你们都有光明的未来...";
                emoji = "(・∀・)";
            }
            default -> {
                text = "我其实想在死亡后自动打开千恋万花的...没有就启动原神... Ciallo~";
                emoji = "(∠・ω< )⌒☆";
            }
        }

        MutableComponent fullMessage = MessageUtils.createBaityPrefix()
            .append(MessageUtils.createColoredText(text, 0x00FFFF))
            .append(MessageUtils.createColoredText(emoji, 0xFF80FF));

        MessageUtils.sendCustomMessage(fullMessage);
    }
}
