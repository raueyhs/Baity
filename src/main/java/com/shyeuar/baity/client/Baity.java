package com.shyeuar.baity.client;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.smol.SmolFriendsScreen;
import com.shyeuar.baity.managers.KeybindManager;
import com.shyeuar.baity.managers.ModuleInitializer;
import com.shyeuar.baity.sync.BaityPresenceSync;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.items.CustomTotemItem;
import com.shyeuar.baity.features.FancyCreeperVeil;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplash;
import com.shyeuar.baity.features.fishing.ChromaFishingLine;
import com.shyeuar.baity.features.fishing.FishHookTimer;
import com.shyeuar.baity.features.PaperDoll;
import com.shyeuar.baity.features.ChatChannelSwitcher;
import com.shyeuar.baity.features.blockanimation.BlockAnimationSwordCatalog;
import com.shyeuar.baity.features.fishing.HypixelFishingRodCatalog;
import com.shyeuar.baity.features.smolpeople.SmolFriendManager;
import com.shyeuar.baity.features.highlights.PestEntityRegistry;
import com.shyeuar.baity.features.highlights.PestHighlights;
import com.shyeuar.baity.features.highlights.SafariHighlights;
import com.shyeuar.baity.features.moderntooltip.ModernTooltip;
import com.shyeuar.baity.features.VanillaHudHider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.shyeuar.baity.features.radialmenu.RadialMenu;
import com.shyeuar.baity.features.SoftFullscreen;
import com.shyeuar.baity.utils.RemoteFileFetcher;
import com.shyeuar.baity.utils.SoundUtils;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class Baity implements ClientModInitializer {
    
    private static long lastKeyPressTime = 0;
    public static boolean openGuiNextTick = false;
    public static boolean openSmolFriendsNextTick = false;

    @Override
    public void onInitializeClient() {
        com.shyeuar.baity.config.BaityConfigDir.init();
        com.shyeuar.baity.config.DevConfig.initWatermarkHandle();
        RemoteFileFetcher.init();
        BlockAnimationSwordCatalog.init();
        Objects.requireNonNull(CustomTotemItem.CUSTOM_TOTEM);
        com.shyeuar.baity.gui.render.BaityUiPipelines.register();

        ConfigManager.loadConfig();
        SmolFriendManager.reloadFromConfig();

        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        ModuleInitializer.initializeModules();
        BaityPresenceSync.init();

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> PestEntityRegistry.onClientEntityLoad(entity));
        
        FishHookTimer.init();
        PaperDoll.init();
        ChromaFishingLine.init();
        RadialMenu.init();
        ChatChannelSwitcher.init();
        com.shyeuar.baity.features.enchantlore.EnchantLore.init();
        com.shyeuar.baity.features.Keybinds.init();
        com.shyeuar.baity.features.sidepanel.SidePanel.init();
        ModernTooltip.init();
        com.shyeuar.baity.features.nucleusscanner.NucleusScanner.init();
        com.shyeuar.baity.utils.LocateUtils.registerClientEvents();
        
        registerCustomSounds();
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long windowHandle = client.getWindow().handle();
            ConfigManager.flushPendingSave();
            HypixelFishingRodCatalog.clientTickRefreshMainHandIfSkyblock(client);

            if (openGuiNextTick) {
                openGuiNextTick = false;
                Minecraft.getInstance().gui.setScreen(new ClickGui());
                return;
            }

            if (openSmolFriendsNextTick) {
                openSmolFriendsNextTick = false;
                Minecraft.getInstance().gui.setScreen(new SmolFriendsScreen(null));
                return;
            }
            
            if (client.gui.screen() == null && ConfigManager.guiKeyCode != 0) {
                boolean currentGuiKeyState = KeyMappingUtils.isKeyPressed(windowHandle, ConfigManager.guiKeyCode);
                if (currentGuiKeyState) {
                    if (System.currentTimeMillis() - lastKeyPressTime > 200) {
                        Minecraft.getInstance().gui.setScreen(new ClickGui());
                        lastKeyPressTime = System.currentTimeMillis();
                    }
                }
            }
            
            KeybindManager.handleModuleKeybinds(client, windowHandle);

            SoftFullscreen.tick(client);
            
            RadialMenu.tick(client);
            com.shyeuar.baity.features.FocusPlayerNametag.tick(client);
            BaityPresenceSync.tick();
            PestHighlights.tickPestCaches();
            
            FishHookTimer.getInstance().tick();
            PaperDoll.getInstance().clientTick();
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            com.shyeuar.baity.sync.SyncCommands.register(dispatcher)
        );

        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(ctx -> {
            if (com.shyeuar.baity.features.MotionBlur.isActive()) {
                com.shyeuar.baity.features.MotionBlur.onAfterOpaqueTerrain();
            }
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(new com.shyeuar.baity.features.NametagRenderer());
        LevelRenderEvents.END_MAIN.register(new FancyDmgSplash());
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(new com.shyeuar.baity.features.highlights.ShulkerHighlights());
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(new SafariHighlights());
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(new com.shyeuar.baity.features.highlights.PestHighlights());
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(new com.shyeuar.baity.features.highlights.InvisibugHighlights());

        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(new FancyCreeperVeil());
        
        VanillaHudHider.init();

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("baity", "fish_hook_timer"), (guiGraphics, tickDelta) -> {
            var timer = FishHookTimer.getInstance();
            if (timer.shouldRender()) timer.render(guiGraphics, 0.0f);
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("baity", "paper_doll"),
                (guiGraphics, tickDelta) -> {
                    var doll = PaperDoll.getInstance();
                    if (doll.shouldRender()) {
                        doll.render(guiGraphics, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                }
        );
    }
    
    public static final net.minecraft.sounds.SoundEvent LAUGHTER_SOUND = registerSoundEvent("sounds.laughter");
    
    private static net.minecraft.sounds.SoundEvent registerSoundEvent(String name) {
        net.minecraft.resources.Identifier identifier = net.minecraft.resources.Identifier.fromNamespaceAndPath("baity", name);
        return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT, identifier, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(identifier));
    }
    
    private void registerCustomSounds() {
        SoundUtils.registerSounds();
    }

}
