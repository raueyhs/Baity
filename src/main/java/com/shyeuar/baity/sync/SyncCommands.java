package com.shyeuar.baity.sync;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.shyeuar.baity.features.smolpeople.SmolFriendCommands;

@Environment(EnvType.CLIENT)
public final class SyncCommands {
    private SyncCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("baity")
            .executes(context -> {
                com.shyeuar.baity.client.Baity.openGuiNextTick = true;
                return 1;
            });

        SmolFriendCommands.attachSubCommands(root);
        com.shyeuar.baity.features.Reminder.attachSubCommands(root);

        root.then(
            ClientCommands.literal("notification")
                .executes(context -> showNotificationState())
                .then(ClientCommands.literal("on")
                    .executes(context -> setNotificationEnabled(true)))
                .then(ClientCommands.literal("off")
                    .executes(context -> setNotificationEnabled(false)))
        );

        root.then(
            ClientCommands.literal("sync")
                .executes(context -> syncNow())
                .then(ClientCommands.literal("on")
                    .executes(context -> setSyncEnabled(true)))
                .then(ClientCommands.literal("off")
                    .executes(context -> setSyncEnabled(false)))
                .then(ClientCommands.literal("help")
                    .executes(context -> {
                        com.shyeuar.baity.utils.MessageUtils.sendSyncHelpLinesInChat();
                        return 1;
                    }))
        );

        dispatcher.register(root);
    }

    private static int showNotificationState() {
        MessageUtils.sendBaityMessage("Sync notification is " + (ConfigManager.baityPresenceSyncNotificationEnabled ? "ON" : "OFF") + ".");
        return 1;
    }

    private static int setNotificationEnabled(boolean enabled) {
        ConfigManager.baityPresenceSyncNotificationEnabled = enabled;
        ConfigManager.requestSave();
        MessageUtils.sendBaityMessage("Sync notification " + (enabled ? "enabled." : "disabled."));
        return 1;
    }

    private static int syncNow() {
        MessageUtils.sendSyncStartForCommand();
        BaityPresenceSync.syncOnce();
        return 1;
    }

    private static int setSyncEnabled(boolean enabled) {
        ConfigManager.baityPresenceSyncEnabled = enabled;
        ConfigManager.requestSave();
        MessageUtils.sendBaityMessage("remote data autosync" + (enabled ? "enabled." : "disabled."));
        return 1;
    }
}