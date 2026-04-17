package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.shyeuar.baity.utils.TickSchedulerUtils;
import com.shyeuar.baity.utils.MessageUtils;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public class Reminder {
    
    private static Reminder instance;
    
    private static final ItemStack COOKIE_DISPLAY_ICON = new ItemStack(Items.COOKIE);
    private static final ItemStack GOD_POTION_DISPLAY_ICON = new ItemStack(Items.POTION);
    
    private static final Pattern GOD_POTION_PATTERN = Pattern.compile(
        "You have a God Potion active! (\\d+) (Days?|Hours?|Minutes?|Mins?|Min) Use '/effects' to see the effects!"
    );
    
    private boolean cookieAlreadyNotified = false;
    private boolean godPotionAlreadyNotified = false;
    private boolean previouslyInSkyBlock = false;
    
    private int cookieSchedulerId = -1;
    private int godPotionSchedulerId = -1;
    
    public static Reminder getInstance() {
        if (instance == null) {
            instance = new Reminder();
        }
        return instance;
    }
    
    public static void init() {
        Reminder reminder = getInstance();
        if (reminder != null) {
            reminder.startCookieScheduler();
            reminder.startGodPotionScheduler();
        }
    }
    
    private void startCookieScheduler() {
        cookieSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickCookieReminder, 5, TimeUnit.SECONDS);
    }
    
    private void startGodPotionScheduler() {
        godPotionSchedulerId = TickSchedulerUtils.getInstance().runRepeating(() -> {
            boolean currentlyInSkyBlock = isInSkyBlock();
            if (currentlyInSkyBlock && !previouslyInSkyBlock) {
                cookieAlreadyNotified = false;
                godPotionAlreadyNotified = false;
            }
            previouslyInSkyBlock = currentlyInSkyBlock;
            
            if (isInSkyBlock() && !godPotionAlreadyNotified) {
                tickGodPotionReminder();
            }
        }, 10, TimeUnit.SECONDS);
    }
    
    private void tickCookieReminder() {
        boolean currentlyInSkyBlock = isInSkyBlock();
        if (currentlyInSkyBlock && !previouslyInSkyBlock) {
            cookieAlreadyNotified = false;
            godPotionAlreadyNotified = false;
        }
        previouslyInSkyBlock = currentlyInSkyBlock;
        
        if (!isCookieReminderActive()) return;
        if (cookieAlreadyNotified) return;

        String tabFooter = getTabFooterText();
        if (tabFooter == null || !tabFooter.contains("Cookie Buff")) return;

        if (tabFooter.contains("Not active! Obtain booster cookies from the community")) {
            cookieAlreadyNotified = true;
            sendCookieNotification();
        }
    }
    
    private void tickGodPotionReminder() {
        if (!isGodPotionReminderActive()) return;
        if (godPotionAlreadyNotified) return;

        String tabFooter = getTabFooterText();
        if (tabFooter == null || !tabFooter.contains("God Potion")) return;

        Matcher matcher = GOD_POTION_PATTERN.matcher(tabFooter);
        if (matcher.find()) {
            int timeValue = Integer.parseInt(matcher.group(1));
            String timeUnit = matcher.group(2).toLowerCase();
            int remainingMinutes = convertToMinutes(timeValue, timeUnit);
            
            if (remainingMinutes <= 30) {
                godPotionAlreadyNotified = true;
                sendGodPotionNotification(remainingMinutes);
            }
        }
    }
    
    private boolean isCookieReminderActive() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null || !reminderModule.isEnabled()) return false;
        boolean subEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "cookie buff reminder", false);
        return isInSkyBlock() && subEnabled;
    }
    
    private boolean isGodPotionReminderActive() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null || !reminderModule.isEnabled()) return false;
        boolean subEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "god potion reminder", false);
        return isInSkyBlock() && subEnabled;
    }
    
    private void sendCookieNotification() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        MutableComponent prefix = MessageUtils.createBaityPrefix();
        MutableComponent message = Component.literal("You don't have a").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(" Booster Cookie ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal("active!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        
        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sounds.SoundEvents.BLAZE_DEATH, 1.0f, 0.75f);
        showCookieAnimation(client, client.player);
    }
    
    private void sendGodPotionNotification(int remainingMinutes) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        MutableComponent prefix = MessageUtils.createBaityPrefix();
        MutableComponent message = Component.literal("Your ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                .append(Component.literal("God Potion ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("will expire in ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(formatMinutes(remainingMinutes)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        
        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sounds.SoundEvents.BLAZE_DEATH, 1.0f, 0.75f);
        showGodPotionAnimation(client, client.player);
    }
    
    private int convertToMinutes(int value, String unit) {
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
    
    private String formatMinutes(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remaining = minutes % 60;
            return remaining == 0 ? hours + "h" : hours + "h " + remaining + "m";
        }
        return minutes + "m";
    }
    
    private void showCookieAnimation(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        if (client.level == null) return;
        client.gameRenderer.displayItemActivation(COOKIE_DISPLAY_ICON);
        client.particleEngine.createTrackingEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }
    
    private void showGodPotionAnimation(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        if (client.level == null) return;
        client.gameRenderer.displayItemActivation(GOD_POTION_DISPLAY_ICON);
        client.particleEngine.createTrackingEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }
    
    private boolean isInSkyBlock() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return false;
        
        if (client.isLocalServer()) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        }
        
        if (client.getCurrentServer() != null) {
            String serverAddress = client.getCurrentServer().ip.toLowerCase();
            return serverAddress.contains("hypixel");
        }
        
        return false;
    }
    
    private String getTabFooterText() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null || client.gui.getTabList() == null) return null;
        
        try {
            net.minecraft.network.chat.Component footer = ((com.shyeuar.baity.mixin.PlayerListHudMixin) client.gui.getTabList()).getFooter();
            return footer != null ? footer.getString() : null;
        } catch (Exception e) {
            try {
                if (client.getConnection() != null) {
                    var playerList = client.getConnection().getOnlinePlayers();
                    for (var entry : playerList) {
                        if (entry.getTabListDisplayName() != null) {
                            String name = entry.getTabListDisplayName().getString();
                            if (name.contains("Cookie Buff")) {
                                return name;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        return null;
    }
    
    public static void updateSettings() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null) return;
        
        Reminder reminder = getInstance();
        if (reminder == null) return;
        
        boolean cookieEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "cookie buff reminder", false);
        boolean godPotionEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "god potion reminder", false);
        
        reminder.setCookieReminderEnabled(cookieEnabled);
        reminder.setGodPotionReminderEnabled(godPotionEnabled);
    }
    
    public boolean isCookieReminderEnabled() {
        return com.shyeuar.baity.config.ConfigManager.reminderCookieBuffEnabled;
    }
    
    public void setCookieReminderEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.reminderCookieBuffEnabled = enabled;
        if (!enabled) {
            cookieAlreadyNotified = false;
            if (cookieSchedulerId != -1) {
                TickSchedulerUtils.getInstance().cancelTask(cookieSchedulerId);
                cookieSchedulerId = -1;
            }
        } else {
            if (cookieSchedulerId == -1) {
                cookieSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickCookieReminder, 5, TimeUnit.SECONDS);
            }
        }
    }
    
    public boolean isGodPotionReminderEnabled() {
        return com.shyeuar.baity.config.ConfigManager.reminderGodPotionEnabled;
    }
    
    public void setGodPotionReminderEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.reminderGodPotionEnabled = enabled;
        if (!enabled) {
            godPotionAlreadyNotified = false;
            if (godPotionSchedulerId != -1) {
                TickSchedulerUtils.getInstance().cancelTask(godPotionSchedulerId);
                godPotionSchedulerId = -1;
            }
        } else {
            if (godPotionSchedulerId == -1) {
                godPotionSchedulerId = TickSchedulerUtils.getInstance().runRepeating(() -> {
                    if (isInSkyBlock() && !godPotionAlreadyNotified) {
                        tickGodPotionReminder();
                    }
                }, 10, TimeUnit.SECONDS);
            }
        }
    }
    
}
