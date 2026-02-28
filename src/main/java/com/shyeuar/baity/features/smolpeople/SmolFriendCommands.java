package com.shyeuar.baity.features.smolpeople;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.shyeuar.baity.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public final class SmolFriendCommands {
    private SmolFriendCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("baity")
            .executes(context -> {
                com.shyeuar.baity.client.Baity.openGuiNextTick = true;
                return 1;
            });

        attachSubCommands(root);
        dispatcher.register(root);
    }

    private static void attachSubCommands(LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(
            ClientCommandManager.literal("fadd")
                .executes(context -> handleMissingName())
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .executes(context -> executeAdd(StringArgumentType.getString(context, "name"))))
        );

        root.then(
            ClientCommandManager.literal("fremove")
                .executes(context -> handleMissingName())
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .suggests(SmolFriendCommands::suggestFriends)
                    .executes(context -> executeRemove(StringArgumentType.getString(context, "name"))))
        );
    }

    private static int executeAdd(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            MessageUtils.sendBaityMessage("Friend name cannot be empty.");
            return 0;
        }

        if (SmolFriendManager.addFriend(trimmed)) {
            MessageUtils.sendBaityMessage("Added SmolPeople friend: " + trimmed);
            return 1;
        }

        MessageUtils.sendBaityMessage(trimmed + " is already in SmolPeople friends.");
        return 0;
    }

    private static int executeRemove(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            MessageUtils.sendBaityMessage("Friend name cannot be empty.");
            return 0;
        }

        String storedName = SmolFriendManager.getStoredName(trimmed);
        if (SmolFriendManager.removeFriend(trimmed)) {
            MessageUtils.sendBaityMessage("Removed SmolPeople friend: " + (storedName == null ? trimmed : storedName));
            return 1;
        }

        MessageUtils.sendBaityMessage(trimmed + " is not in SmolPeople friends.");
        return 0;
    }

    private static int handleMissingName() {
        MessageUtils.sendBaityMessage("Friend name cannot be empty.");
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestFriends(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String friendName : SmolFriendManager.getFriends()) {
            if (friendName.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(friendName);
            }
        }
        return builder.buildFuture();
    }
}
