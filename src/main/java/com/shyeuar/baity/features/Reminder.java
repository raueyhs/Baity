package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import com.shyeuar.baity.utils.TickScheduler;
import com.shyeuar.baity.utils.MessageUtils;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

@Environment(EnvType.CLIENT)
public class Reminder {
    
    private static final ItemStack COOKIE_ICON = new ItemStack(Items.COOKIE);
    private static final ItemStack GOD_POTION_ICON = new ItemStack(Items.POTION);
    
    private boolean cookieNotified = false;
    
    private boolean godPotionNotified = false;
    
    private boolean wasInSkyBlock = false;
    
    private boolean hasRegisteredMeowAlert = false;
    private long lastMeowTime = 0;
    private static final long MEOW_COOLDOWN_MS = 2000;
    private static final float MEOW_VOLUME = 1.5F;
    private static final float MEOW_PITCH = 1.0F;
    
    private int cookieTaskId = -1;
    private int godPotionTaskId = -1;
    
    private static final Pattern GOD_POTION_TIME_PATTERN = Pattern.compile(
        "You have a God Potion active! (\\d+) (Days?|Hours?|Minutes?|Mins?|Min) Use '/effects' to see the effects!"
    );
    
    public static void init() {
        Reminder instance = getInstance();
        if (instance != null) {
            instance.startScheduler();
            instance.registerWorldEvents();
            instance.initMeowAlert();
        }
    }
    
    private void registerWorldEvents() {
        godPotionTaskId = TickScheduler.getInstance().runRepeating(() -> {
            boolean currentlyInSkyBlock = isOnSkyBlock();
            if (currentlyInSkyBlock && !wasInSkyBlock) {
                cookieNotified = false;
                godPotionNotified = false;
            }
            wasInSkyBlock = currentlyInSkyBlock;
            
            if (isOnSkyBlock() && !godPotionNotified) {
                checkGodPotionBuff();
            }
        }, 10, TimeUnit.SECONDS);
    }
    
    private void initMeowAlert() {
        if (!hasRegisteredMeowAlert) {
            ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
                boolean meowAlertEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "meowalert", false);
                if (!meowAlertEnabled) {
                    return;
                }
                
                if (sender != null) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        String currentPlayerName = client.player.getGameProfile().getName();
                        String senderName = sender.getName();
                        if (!currentPlayerName.equals(senderName)) {
                            String messageText = message.getString();
                            if (containsPlayerName(messageText, currentPlayerName)) {
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastMeowTime > MEOW_COOLDOWN_MS) {
                                    lastMeowTime = currentTime;
                                    playMeowSound(client.player);
                                }
                            }
                        }
                    }
                }
            });
            hasRegisteredMeowAlert = true;
        }
    }
    
    private boolean containsPlayerName(String message, String playerName) {
        if (message == null || playerName == null) return false;
        
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
    
    private void playMeowSound(net.minecraft.client.network.ClientPlayerEntity player) {
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_CAT_AMBIENT, MEOW_VOLUME * 5.0f, MEOW_PITCH);
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, MEOW_VOLUME * 5.0f, 5.0f);
    }
    
    private static Reminder instance;
    
    public static Reminder getInstance() {
        if (instance == null) {
            instance = new Reminder();
        }
        return instance;
    }
    
    private void startScheduler() {
        cookieTaskId = TickScheduler.getInstance().runRepeating(this::update, 5, TimeUnit.SECONDS);
    }
    
    private boolean isCookieEnabled() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        boolean cookieReminderEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "cookie buff reminder", false);
        return isOnSkyBlock() && cookieReminderEnabled;
    }
    
    private boolean isGodPotionEnabled() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        boolean godPotionReminderEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "god potion reminder", false);
        return isOnSkyBlock() && godPotionReminderEnabled;
    }
    
    private void update() {
        boolean currentlyInSkyBlock = isOnSkyBlock();
        if (currentlyInSkyBlock && !wasInSkyBlock) {
            cookieNotified = false;
            godPotionNotified = false;
        }
        wasInSkyBlock = currentlyInSkyBlock;
        
        checkCookieBuff();
    }
    
    private void checkCookieBuff() {
        if (!isCookieEnabled()) return;
        if (cookieNotified) return;

        String footer = getTabListFooter();
        if (footer == null || !footer.contains("Cookie Buff")) return;

        if (footer.contains("Not active! Obtain booster cookies from the community")) {
            cookieNotified = true;

            MutableText prefix = MessageUtils.createBaityPrefix();
            MutableText message = Text.literal("You don't have a").formatted(Formatting.RED, Formatting.BOLD)
                    .append(Text.literal(" Booster Cookie ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                    .append(Text.literal("active!").formatted(Formatting.RED, Formatting.BOLD));
            MutableText fullMessage = prefix.append(message);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                MessageUtils.sendCustomMessage(fullMessage);
                
                client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_DEATH, 1.0f, 0.75f);
                
                playCookieAnimation(client, client.player);
            }
        }
    }
    
    private void checkGodPotionBuff() {
        if (!isGodPotionEnabled()) return;
        if (godPotionNotified) return;

        String footer = getTabListFooter();
        if (footer == null || !footer.contains("God Potion")) return;

        Matcher matcher = GOD_POTION_TIME_PATTERN.matcher(footer);
        if (matcher.find()) {
            int timeValue = Integer.parseInt(matcher.group(1));
            String timeUnit = matcher.group(2).toLowerCase();
            
            int remainingMinutes = parseTimeToMinutes(timeValue, timeUnit);
            
            if (remainingMinutes <= 30) {
                godPotionNotified = true;
                
                MutableText prefix = MessageUtils.createBaityPrefix();
                MutableText message = Text.literal("Your ").formatted(Formatting.YELLOW, Formatting.BOLD)
                        .append(Text.literal("God Potion ").formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal("will expire in ").formatted(Formatting.YELLOW, Formatting.BOLD))
                        .append(Text.literal(formatTime(remainingMinutes)).formatted(Formatting.RED, Formatting.BOLD))
                        .append(Text.literal("!").formatted(Formatting.YELLOW, Formatting.BOLD));
                MutableText fullMessage = prefix.append(message);

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    MessageUtils.sendCustomMessage(fullMessage);
                    
                    client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_DEATH, 1.0f, 0.75f);
                    
                    playGodPotionAnimation(client, client.player);
                }
            }
        }
    }
    
    private int parseTimeToMinutes(int value, String unit) {
        switch (unit) {
            case "day":
            case "days":
                return value * 24 * 60;
            case "hour":
            case "hours":
                return value * 60;
            case "minute":
            case "minutes":
            case "min":
            case "mins":
                return value;
            default:
                return Integer.MAX_VALUE;
        }
    }
    
    private String formatTime(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        } else {
            return minutes + "m";
        }
    }
    
    private void playCookieAnimation(MinecraftClient client, net.minecraft.client.network.ClientPlayerEntity player) {
        if (client.world != null) {
            client.gameRenderer.showFloatingItem(COOKIE_ICON);
            
            client.particleManager.addEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
        }
    }
    
    private void playGodPotionAnimation(MinecraftClient client, net.minecraft.client.network.ClientPlayerEntity player) {
        if (client.world != null) {
            client.gameRenderer.showFloatingItem(GOD_POTION_ICON);
            
            client.particleManager.addEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
        }
    }
    
    private boolean isOnSkyBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return false;
        
        if (client.isInSingleplayer()) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        }
        
        if (client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address.toLowerCase();
            return serverAddress.contains("hypixel");
        }
        
        return false;
    }
    
    private String getTabListFooter() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null || client.inGameHud.getPlayerListHud() == null) return null;
        
        try {
            net.minecraft.text.Text footer = ((com.shyeuar.baity.mixin.PlayerListHudAccessor) client.inGameHud.getPlayerListHud()).getFooter();
            return footer != null ? footer.getString() : null;
        } catch (Exception e) {
            try {
                if (client.getNetworkHandler() != null) {
                    var playerList = client.getNetworkHandler().getPlayerList();
                    for (var entry : playerList) {
                        if (entry.getDisplayName() != null) {
                            String name = entry.getDisplayName().getString();
                            if (name.contains("Cookie Buff")) {
                                return name;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // 忽略错误
            }
        }
        
        return null;
    }
    
    public static void updateSettings() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null) return;
        
        Reminder instance = getInstance();
        if (instance == null) return;
        
        boolean cookieReminderEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBooleanRaw(
            reminderModule, "cookie buff reminder", false);
        boolean godPotionReminderEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBooleanRaw(
            reminderModule, "god potion reminder", false);
        boolean meowAlertEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBooleanRaw(
            reminderModule, "meowalert", false);
        
        instance.setCookieReminderEnabled(cookieReminderEnabled);
        instance.setGodPotionReminderEnabled(godPotionReminderEnabled);
        instance.setMeowAlertEnabled(meowAlertEnabled);
    }
    
    public boolean isCookieReminderEnabled() {
        return com.shyeuar.baity.config.ConfigManager.cookieBuffReminderEnabled;
    }
    
    public void setCookieReminderEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.cookieBuffReminderEnabled = enabled;
        if (!enabled) {
            cookieNotified = false;
            if (cookieTaskId != -1) {
                TickScheduler.getInstance().cancelTask(cookieTaskId);
                cookieTaskId = -1;
            }
        } else {
            if (cookieTaskId == -1) {
                cookieTaskId = TickScheduler.getInstance().runRepeating(this::update, 5, TimeUnit.SECONDS);
            }
        }
    }
    
    public boolean isGodPotionReminderEnabled() {
        return com.shyeuar.baity.config.ConfigManager.godPotionReminderEnabled;
    }
    
    public void setGodPotionReminderEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.godPotionReminderEnabled = enabled;
        if (!enabled) {
            godPotionNotified = false;
            if (godPotionTaskId != -1) {
                TickScheduler.getInstance().cancelTask(godPotionTaskId);
                godPotionTaskId = -1;
            }
        } else {
            if (godPotionTaskId == -1) {
                godPotionTaskId = TickScheduler.getInstance().runRepeating(() -> {
                    if (isOnSkyBlock() && !godPotionNotified) {
                        checkGodPotionBuff();
                    }
                }, 10, TimeUnit.SECONDS);
            }
        }
    }
    
    public boolean isMeowAlertEnabled() {
        return com.shyeuar.baity.config.ConfigManager.meowAlertEnabled;
    }
    
    public void setMeowAlertEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.meowAlertEnabled = enabled;
    }
}


