package com.shyeuar.baity.features.sidepanel;

import com.mojang.serialization.Codec;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.Keybinds;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.references.ItemIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public final class SidePanel {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/SidePanel");
    private static final int[] LOADOUT_PREVIEW_SLOTS = {10, 19, 28, 37, 21};
    private static final int[] EQUIPMENT_MENU_SLOTS = {10, 19, 28, 37, 47};
    private static final Codec<ItemStack[]> CACHE_CODEC = ItemStack.OPTIONAL_CODEC
            .listOf(SlotKind.values().length, SlotKind.values().length)
            .xmap(stacks -> stacks.toArray(new ItemStack[0]), List::of)
            .fieldOf("items")
            .codec();

    private static final ItemStack[] SLOTS = new ItemStack[SlotKind.values().length];
    private static final SlotKind[] EQUIPMENT_KINDS = {
            SlotKind.NECKLACE,
            SlotKind.CLOAK,
            SlotKind.BELT,
            SlotKind.GLOVES
    };
    private static final Set<AbstractContainerScreen<?>> syncedOnOpen =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static String loadedProfileId;
    private static Island loadedIsland;

    static {
        for (int i = 0; i < SLOTS.length; i++) {
            SLOTS[i] = ItemStack.EMPTY;
        }
    }

    private SidePanel() {
    }

    public enum SlotKind {
        NECKLACE,
        CLOAK,
        BELT,
        GLOVES,
        PET
    }

    public enum Island {
        MAIN("main"),
        RIFT("rift"),
        SAFARI("safari");

        private final String cacheKey;

        Island(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        public String cacheKey() {
            return cacheKey;
        }

        public boolean usesSeparateCache() {
            return this != MAIN;
        }
    }

    private static final int ISLAND_SWITCH_DEBOUNCE_TICKS = 3;

    private static Island pendingIsland;
    private static int pendingIslandTicks;
    private static Island cachedPanelIsland = Island.MAIN;
    private static Keybinds.MenuType suppressCloseResync = Keybinds.MenuType.NONE;
    private static int pendingAutoCloseSetIndex = -1;
    private static boolean pendingAutoCloseUnequip;
    private static int activeEquipmentSetIndex = -1;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, _, _) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }
            ScreenEvents.remove(screen).register(removed -> onContainerClose(containerScreen));
            waitForContainerLoad(containerScreen);
        });

        ClientTickEvents.END_CLIENT_TICK.register(SidePanel::tick);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!isModuleEnabled() || client.player == null || !LocateUtils.onHypixel(client)) {
                return;
            }
            SidePanelCache.ensureLookupLoaded(client, profileId(client));
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            SidePanelPets.handleChat(message, overlay);
            return true;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ensureSessionReady(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> {
            SidePanelPets.flushCacheIfDirty();
            SidePanelLoadouts.save();
            SidePanelCache.flush(Minecraft.getInstance());
            SidePanelCache.clearSession();
            clearSession();
        });
    }

    private static int loadoutDiskFlushCooldown;

    private static void tick(Minecraft client) {
        if (!isModuleEnabled() || client.player == null || !isSyncActive(client)) {
            return;
        }
        cachedPanelIsland = LocateUtils.panelIsland(client);
        updateIslandCache(client);
        SidePanelPets.tick(client);
        SidePanelEquipment.tick(client);
        SidePanelLoadouts.tickIfOpen(client);
        if (++loadoutDiskFlushCooldown >= 40) {
            loadoutDiskFlushCooldown = 0;
            SidePanelLoadouts.flushNamesIfDirty();
            SidePanelCache.flush(client);
        }
    }

    private static void updateIslandCache(Minecraft client) {
        String profileId = profileId(client);
        Island detected = cachedPanelIsland;

        if (loadedIsland == null || loadedProfileId == null || !profileId.equals(loadedProfileId)) {
            resetIslandDebounce();
            ensureLoaded(client, profileId, detected);
            return;
        }

        if (detected == loadedIsland) {
            resetIslandDebounce();
            return;
        }

        if (detected != pendingIsland) {
            pendingIsland = detected;
            pendingIslandTicks = 1;
            return;
        }

        if (++pendingIslandTicks >= ISLAND_SWITCH_DEBOUNCE_TICKS) {
            ensureLoaded(client, profileId, pendingIsland);
            resetIslandDebounce();
        }
    }

    private static void resetIslandDebounce() {
        pendingIsland = null;
        pendingIslandTicks = 0;
    }

    public static boolean isModuleEnabled() {
        var module = ModuleManager.getModuleByName("SidePanel");
        return module != null && module.isEnabled() && ConfigManager.sidePanelEnabled;
    }

    public static boolean isPetPanelEnabled() {
        return isModuleEnabled() && ConfigManager.sidePanelPetEnabled;
    }

    public static boolean isSyncActive(Minecraft client) {
        return isModuleEnabled() && LocateUtils.inSkyBlock(client);
    }

    public static boolean isPetChatPassiveActive(Minecraft client) {
        return isModuleEnabled() && LocateUtils.onHypixel(client);
    }

    public static boolean isPetMenuPassiveActive(Minecraft client) {
        return isModuleEnabled() && LocateUtils.inSkyBlock(client);
    }

    public static boolean shouldRenderOn(InventoryScreen screen) {
        if (screen == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        if (!isModuleEnabled()) {
            return false;
        }
        return LocateUtils.inSkyBlock(client);
    }

    public static String profileId(Minecraft client) {
        return client.player.getGameProfile().id().toString();
    }

    public static void onSlotClick(SlotKind kind, int button) {
        if (button != 0) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !LocateUtils.inSkyBlock(client)) {
            return;
        }
        String command = slotCommand(kind, cachedPanelIsland);
        if (command != null) {
            client.player.connection.sendCommand(command);
        }
    }

    private static String slotCommand(SlotKind kind, Island island) {
        if (kind == SlotKind.PET) {
            return switch (island) {
                case RIFT -> "sbmenu";
                case SAFARI -> "stats";
                default -> "petsmenu";
            };
        }
        return switch (island) {
            case RIFT, SAFARI -> "stats";
            default -> "equipment";
        };
    }

    static ItemStack get(SlotKind kind) {
        return SLOTS[kind.ordinal()];
    }

    static void set(SlotKind kind, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.is(Items.AIR)) {
            SLOTS[kind.ordinal()] = ItemStack.EMPTY;
            return;
        }
        SLOTS[kind.ordinal()] = stack.copy();
    }

    static void clearSlots() {
        for (int i = 0; i < SLOTS.length; i++) {
            SLOTS[i] = ItemStack.EMPTY;
        }
    }

    static void persistNow() {
        persistIfNeeded();
    }

    private static void persistIfNeeded() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null && isSyncActive(client)) {
            saveCurrent(client);
        }
    }

    private static void ensureLoaded(Minecraft client, String profileId, Island island) {
        if (client.level == null || profileId == null || profileId.isEmpty()) {
            return;
        }
        if (profileId.equals(loadedProfileId) && island == loadedIsland) {
            return;
        }
        boolean switchingProfile = loadedProfileId != null;
        if (loadedProfileId != null && loadedIsland != null) {
            saveCurrent(client, loadedProfileId, loadedIsland);
        }
        load(client, profileId, island, switchingProfile);
        SidePanelCache.loadIfProfileChanged(client, profileId);
        SidePanelLoadouts.load(client, profileId);
        SidePanelCache.registerCurrentPanelSlots();
        afterIslandCacheLoad(client, island);
        loadedProfileId = profileId;
        loadedIsland = island;
    }

    static void ensureSessionReady(Minecraft client) {
        if (client.player == null || !isModuleEnabled()) {
            return;
        }
        if (LocateUtils.onHypixel(client)) {
            SidePanelCache.ensureLookupLoaded(client, profileId(client));
        }
        if (!isSyncActive(client)) {
            return;
        }
        ensureLoaded(client, profileId(client), LocateUtils.panelIsland(client));
    }

    static Island currentPanelIsland() {
        return cachedPanelIsland;
    }

    static boolean usesGlobalPetCache(Minecraft client) {
        return !LocateUtils.panelIsland(client).usesSeparateCache();
    }

    private static void afterIslandCacheLoad(Minecraft client, Island island) {
        if (island == Island.MAIN && get(SlotKind.PET).isEmpty()) {
            SidePanelPets.applyLoadedCurrentPet();
        }
    }

    private static void saveCurrent(Minecraft client) {
        if (loadedProfileId == null || loadedIsland == null) {
            return;
        }
        saveCurrent(client, loadedProfileId, loadedIsland);
    }

    private static void clearSession() {
        Minecraft client = Minecraft.getInstance();
        if (loadedProfileId != null && loadedIsland != null && client.level != null) {
            saveCurrent(client, loadedProfileId, loadedIsland);
        }
        SidePanelLoadouts.clearSession();
        loadedProfileId = null;
        loadedIsland = null;
        resetIslandDebounce();
    }

    private static void saveCurrent(Minecraft client, String profileId, Island island) {
        if (client.level == null) {
            return;
        }
        if (shouldSkipEmptyIslandSave(client, profileId, island)) {
            return;
        }
        ItemStack[] stacks = new ItemStack[SLOTS.length];
        for (SlotKind kind : SlotKind.values()) {
            stacks[kind.ordinal()] = get(kind);
        }
        save(client.level.registryAccess(), profileId, island, stacks);
    }

    private static void load(Minecraft client, String profileId, Island island, boolean switchingProfile) {
        Path file = cacheFile(profileId, island);
        if (!Files.isRegularFile(file)) {
            if (switchingProfile) {
                clearSlots();
            }
            return;
        }
        try {
            ItemStack[] stacks = readCacheFile(client, file);
            if (stacks == null) {
                clearSlots();
                return;
            }
            SlotKind[] kinds = SlotKind.values();
            for (int i = 0; i < kinds.length; i++) {
                ItemStack stack = i < stacks.length ? stacks[i] : ItemStack.EMPTY;
                set(kinds[i], stack);
            }
            SidePanelCache.registerCurrentPanelSlots();
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to load SidePanel cache {}: {}", file, exception.toString());
            clearSlots();
        }
    }

    private static ItemStack[] readCacheFile(Minecraft client, Path file) throws IOException {
        RegistryAccess registries = client.level.registryAccess();
        Tag root = NbtIo.read(file);
        return CACHE_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), root)
                .getOrThrow();
    }

    private static boolean shouldSkipEmptyIslandSave(Minecraft client, String profileId, Island island) {
        if (!island.usesSeparateCache() || !isAllSlotsEmpty()) {
            return false;
        }
        Path file = cacheFile(profileId, island);
        if (!Files.isRegularFile(file)) {
            return true;
        }
        try {
            ItemStack[] cached = readCacheFile(client, file);
            return cached != null && hasAnyCachedItem(cached);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean isAllSlotsEmpty() {
        for (SlotKind kind : SlotKind.values()) {
            if (!get(kind).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAnyCachedItem(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void save(RegistryAccess registries, String profileId, Island island, ItemStack[] stacks) {
        try {
            Files.createDirectories(cacheDir());
            Tag root = CACHE_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stacks)
                    .getOrThrow();
            try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(cacheFile(profileId, island)))) {
                NbtIo.writeUnnamedTagWithFallback(root, output);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to save SidePanel cache: {}", exception.toString());
        }
    }

    private static Path cacheDir() {
        return BaityConfigDir.getBaityConfigDir().resolve("sidepanel");
    }

    private static Path cacheFile(String profileId, Island island) {
        return cacheDir().resolve(profileId + "-" + island.cacheKey() + ".nbt");
    }

    public static void onContainerClose(AbstractContainerScreen<?> screen) {
        syncRecognizedContainer(screen, true);
    }

    public static void prepareLoadoutAutoClose(AbstractContainerMenu menu, Component title, int index) {
        Minecraft client = Minecraft.getInstance();
        if (!isSyncActive(client) || menu == null) {
            return;
        }
        if (client.player != null) {
            ensureLoaded(client, profileId(client), LocateUtils.panelIsland(client));
        }
        if (!isModuleEnabled()) {
            return;
        }
        SidePanelLoadouts.armAutoCloseApply(index, menu, title);
    }

    public static void prepareEquipmentAutoClose(
            AbstractContainerMenu menu,
            int setIndex,
            int hotbarIndex,
            boolean unequipping
    ) {
        Minecraft client = Minecraft.getInstance();
        if (!isSyncActive(client) || menu == null) {
            return;
        }
        if (client.player != null) {
            ensureLoaded(client, profileId(client), LocateUtils.panelIsland(client));
        }
        if (!isModuleEnabled()) {
            return;
        }
        refreshEquipmentSetRowCache(menu, hotbarIndex, setIndex);
        pendingAutoCloseSetIndex = setIndex;
        pendingAutoCloseUnequip = unequipping;
    }

    private static void applyEquipmentAutoClose(int setIndex, boolean unequipping) {
        if (unequipping) {
            set(SlotKind.NECKLACE, ItemStack.EMPTY);
            set(SlotKind.CLOAK, ItemStack.EMPTY);
            set(SlotKind.BELT, ItemStack.EMPTY);
            set(SlotKind.GLOVES, ItemStack.EMPTY);
            SidePanelCache.registerCurrentPanelSlots();
        } else {
            applyEquipmentSetFromCache(setIndex);
        }
    }

    private static void clearPendingAutoCloseEquipment() {
        pendingAutoCloseSetIndex = -1;
        pendingAutoCloseUnequip = false;
    }

    private static void refreshEquipmentSetRowCache(AbstractContainerMenu menu, int hotbarIndex, int setIndex) {
        if (menu == null || hotbarIndex < 0 || hotbarIndex > 8 || setIndex < 0) {
            return;
        }
        ItemStack necklace = normalizeEquipment(menu, hotbarIndex);
        ItemStack cloak = normalizeEquipment(menu, hotbarIndex + 9);
        ItemStack belt = normalizeEquipment(menu, hotbarIndex + 18);
        ItemStack gloves = normalizeEquipment(menu, hotbarIndex + 27);
        SidePanelCache.registerSetRow(setIndex, necklace, cloak, belt, gloves);
    }

    private static void applyEquipmentSetFromCache(int setIndex) {
        ItemStack[] row = SidePanelCache.getSetRow(setIndex);
        set(SlotKind.NECKLACE, row[0]);
        set(SlotKind.CLOAK, row[1]);
        set(SlotKind.BELT, row[2]);
        set(SlotKind.GLOVES, row[3]);
        SidePanelCache.registerCurrentPanelSlots();
    }

    public static void onAutoCloseScheduled(Keybinds.MenuType menuType) {
        if (menuType == Keybinds.MenuType.EQUIPMENT) {
            suppressCloseResync = menuType;
            applyEquipmentAutoClose(pendingAutoCloseSetIndex, pendingAutoCloseUnequip);
            clearPendingAutoCloseEquipment();
            persistNow();
        } else if (menuType == Keybinds.MenuType.LOADOUT) {
            suppressCloseResync = menuType;
            SidePanelLoadouts.applyPendingAutoCloseSnapshot();
            SidePanelLoadouts.clearPendingAutoCloseApply();
            persistNow();
        }
    }

    public static void clearAutoCloseSuppress() {
        suppressCloseResync = Keybinds.MenuType.NONE;
        clearPendingAutoCloseEquipment();
        SidePanelLoadouts.clearPendingAutoCloseApply();
    }

    private static void waitForContainerLoad(AbstractContainerScreen<?> screen) {
        if (syncedOnOpen.contains(screen)) {
            return;
        }
        int[] ticks = {0};
        ScreenEvents.afterTick(screen).register(_ -> {
            if (Minecraft.getInstance().gui.screen() != screen || syncedOnOpen.contains(screen)) {
                return;
            }
            ticks[0]++;
            if (ticks[0] < 2) {
                return;
            }
            if (!isSidePanelSyncReady(screen) && ticks[0] < 80) {
                return;
            }
            syncedOnOpen.add(screen);
            ensureSessionReady(Minecraft.getInstance());
            syncRecognizedContainer(screen, false);
        });
    }

    private static boolean isSidePanelSyncReady(AbstractContainerScreen<?> screen) {
        Minecraft client = Minecraft.getInstance();
        String title = screen.getTitle().getString().trim();
        AbstractContainerMenu menu = screen.getMenu();
        if (SidePanelMenus.isEquipmentStatsMenu(title)) {
            return isEquipmentStatsReady(menu);
        }
        if (SidePanelMenus.SKYBLOCK_MENU_TITLE.matcher(title).matches() && LocateUtils.isInRift(client)) {
            return menu.slots.size() > 30 && !menu.getSlot(30).getItem().isEmpty();
        }
        return isContainerPopulated(screen);
    }

    private static boolean isEquipmentStatsReady(AbstractContainerMenu menu) {
        if (menu.slots.size() <= EQUIPMENT_MENU_SLOTS[4]) {
            return false;
        }
        boolean anyReal = false;
        boolean allPopulated = true;
        for (int idx : EQUIPMENT_MENU_SLOTS) {
            ItemStack stack = menu.getSlot(idx).getItem();
            if (stack.isEmpty()) {
                allPopulated = false;
                continue;
            }
            if (!SidePanelUtils.isPlaceholderPane(stack)) {
                anyReal = true;
            }
        }
        return anyReal || allPopulated;
    }

    private static boolean isEquipmentStatsSlot(int slotIndex) {
        for (int idx : EQUIPMENT_MENU_SLOTS) {
            if (idx == slotIndex) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContainerPopulated(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        if (slots.isEmpty()) {
            return false;
        }
        return !slots.get(slots.size() - 1).getItem().isEmpty();
    }

    private static void syncRecognizedContainer(AbstractContainerScreen<?> screen, boolean onClose) {
        Minecraft client = Minecraft.getInstance();
        if (!isSyncActive(client)) {
            return;
        }
        ensureSessionReady(client);
        AbstractContainerMenu menu = screen.getMenu();
        String title = screen.getTitle().getString().trim();

        if (SidePanelPets.isPetsMenu(screen.getTitle())) {
            if (onClose) {
                SidePanelPets.onPetsMenuClose(screen.getTitle());
                SidePanelPets.flushCacheIfDirty();
            } else {
                SidePanelPets.onPetsMenuInventoryLoaded(menu);
            }
            return;
        }

        if (SidePanelMenus.SKYBLOCK_MENU_TITLE.matcher(title).matches() && LocateUtils.isInRift(client)) {
            if (onClose || (menu.slots.size() > 30 && !menu.getSlot(30).getItem().isEmpty())) {
                applyRiftPet(menu);
            }
            return;
        }

        if (SidePanelMenus.isEquipmentStatsMenu(title)) {
            if (onClose || isEquipmentStatsReady(menu)) {
                applyFixedSlots(menu, EQUIPMENT_MENU_SLOTS);
            }
            return;
        }

        Keybinds.MenuType menuType = Keybinds.MenuType.fromTitle(screen.getTitle());
        boolean skipPassiveApply = onClose && suppressCloseResync == menuType;
        if (onClose && skipPassiveApply) {
            suppressCloseResync = Keybinds.MenuType.NONE;
        }
        if (menuType == Keybinds.MenuType.EQUIPMENT) {
            SidePanelEquipment.requestRescan();
            if (onClose && !skipPassiveApply) {
                syncEquippedEquipmentFromMenu(menu, SidePanelMenus.equipmentPageFromTitle(screen.getTitle()));
            }
            return;
        }
        if (menuType == Keybinds.MenuType.LOADOUT) {
            if (!onClose || !skipPassiveApply) {
                SidePanelLoadouts.scanMenu(menu, screen.getTitle());
                if (onClose) {
                    applyLoadoutPreviewSlots(menu);
                }
            } else {
                persistIfNeeded();
            }
        }
    }

    static void applyLoadoutPreviewSlots(AbstractContainerMenu menu) {
        if (menu == null || menu.slots.size() <= LOADOUT_PREVIEW_SLOTS[4]) {
            return;
        }
        readRowIntoState(
                menu,
                LOADOUT_PREVIEW_SLOTS[0],
                LOADOUT_PREVIEW_SLOTS[1],
                LOADOUT_PREVIEW_SLOTS[2],
                LOADOUT_PREVIEW_SLOTS[3],
                LOADOUT_PREVIEW_SLOTS[4]
        );
    }

    private static boolean isLoadoutPreviewSlot(int slotIndex) {
        for (int idx : LOADOUT_PREVIEW_SLOTS) {
            if (idx == slotIndex) {
                return true;
            }
        }
        return false;
    }

    public static void onContainerSlotPacket(int containerId, int slotIndex) {
        var client = Minecraft.getInstance();
        if (!isSyncActive(client)) {
            return;
        }
        ensureSessionReady(client);

        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (screen.getMenu().containerId != containerId) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < screen.getMenu().slots.size()) {
            refreshKnownItem(screen.getMenu().getSlot(slotIndex).getItem());
        }

        String title = screen.getTitle().getString().trim();
        if (SidePanelMenus.isEquipmentStatsMenu(title) && isEquipmentStatsSlot(slotIndex)) {
            applyFixedSlots(screen.getMenu(), EQUIPMENT_MENU_SLOTS);
            return;
        }
        if (SidePanelMenus.isLoadoutsMenu(screen.getTitle()) && isLoadoutPreviewSlot(slotIndex)) {
            if (suppressCloseResync == Keybinds.MenuType.LOADOUT) {
                return;
            }
            if (slotIndex == LOADOUT_PREVIEW_SLOTS[3] || slotIndex == LOADOUT_PREVIEW_SLOTS[4]) {
                applyLoadoutPreviewSlots(screen.getMenu());
            }
            return;
        }
        if (Keybinds.MenuType.fromTitle(screen.getTitle()) == Keybinds.MenuType.EQUIPMENT) {
            int page = SidePanelMenus.equipmentPageFromTitle(screen.getTitle());
            if (suppressCloseResync == Keybinds.MenuType.EQUIPMENT) {
                if (slotIndex >= 0 && slotIndex <= 35) {
                    SidePanelEquipment.requestRescan();
                    SidePanelEquipment.registerPageRows(screen.getMenu(), page);
                }
                return;
            }
            if (slotIndex >= 36 && slotIndex <= 44) {
                syncEquippedEquipmentFromMenu(screen.getMenu(), page);
            } else if (slotIndex >= 0 && slotIndex <= 35) {
                SidePanelEquipment.requestRescan();
                SidePanelEquipment.registerPageRows(screen.getMenu(), page);
            }
        }
        if (SidePanelMenus.SKYBLOCK_MENU_TITLE.matcher(title).matches()
                && LocateUtils.isInRift(client)
                && slotIndex == 30) {
            applyRiftPet(screen.getMenu());
        }
    }

    public static void onContainerContentPacket(int containerId) {
        var client = Minecraft.getInstance();
        if (!isSyncActive(client)) {
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (screen.getMenu().containerId != containerId) {
            return;
        }
        ensureSessionReady(client);

        if (SidePanelMenus.isEquipmentStatsMenu(screen.getTitle().getString().trim())) {
            applyFixedSlots(screen.getMenu(), EQUIPMENT_MENU_SLOTS);
        } else if (Keybinds.MenuType.fromTitle(screen.getTitle()) == Keybinds.MenuType.EQUIPMENT) {
            int page = SidePanelMenus.equipmentPageFromTitle(screen.getTitle());
            SidePanelEquipment.registerPageRows(screen.getMenu(), page);
            syncEquippedEquipmentFromMenu(screen.getMenu(), page);
        } else if (SidePanelMenus.isLoadoutsMenu(screen.getTitle())) {
            applyLoadoutPreviewSlots(screen.getMenu());
        }
        refreshKnownItems(screen.getMenu());
    }

    private static void refreshKnownItems(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            refreshKnownItem(slot.getItem(), false);
        }
    }

    private static void refreshKnownItem(ItemStack stack) {
        refreshKnownItem(stack, true);
    }

    private static void refreshKnownItem(ItemStack stack, boolean allowIdFallback) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int setIndex = -1;
        boolean changed = false;
        for (SlotKind kind : EQUIPMENT_KINDS) {
            ItemStack cached = get(kind);
            if (cached.isEmpty()
                    || !cached.getHoverName().getString().equals(stack.getHoverName().getString())) {
                continue;
            }
            String uuid = itemUuid(stack);
            String cachedUuid = itemUuid(cached);
            boolean identified = uuid != null && uuid.equals(cachedUuid);
            if (!identified && allowIdFallback && uuid == null && cachedUuid == null) {
                String id = itemId(stack);
                identified = id != null && id.equals(itemId(cached));
            }
            if (!identified || ItemStack.matches(cached, stack)) {
                continue;
            }
            if (setIndex < 0) {
                setIndex = resolveActiveEquipmentSetIndex();
            }
            set(kind, stack);
            changed = true;
        }
        if (changed) {
            registerActiveEquipmentSetRow(setIndex);
            persistNow();
        }
    }

    private static String itemUuid(ItemStack stack) {
        return itemAttribute(stack, "uuid");
    }

    private static String itemId(ItemStack stack) {
        return itemAttribute(stack, "id");
    }

    private static String itemAttribute(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag().getString(key).orElse(null);
    }

    static void syncEquippedEquipmentFromMenu(AbstractContainerMenu menu, int page) {
        if (menu == null || menu.slots.size() < 45) {
            return;
        }
        for (int slotIndex = 36; slotIndex <= 44; slotIndex++) {
            if (menu.getSlot(slotIndex).getItem().typeHolder().is(ItemIds.DYE.lime())) {
                activeEquipmentSetIndex = SidePanelEquipment.globalSetIndex(page, slotIndex - 36);
                readRowIntoState(menu, slotIndex - 36, slotIndex - 27, slotIndex - 18, slotIndex - 9, -1);
                return;
            }
        }
        if (activeEquipmentSetIndex >= 0 && activeEquipmentSetIndex / 9 + 1 == page) {
            clearEquippedSlots();
        }
    }

    static void clearEquippedSlots() {
        for (SlotKind kind : EQUIPMENT_KINDS) {
            set(kind, ItemStack.EMPTY);
        }
        SidePanelCache.registerCurrentPanelSlots();
        persistNow();
    }

    private static void applyRiftPet(AbstractContainerMenu menu) {
        if (menu.slots.size() <= 30) {
            return;
        }
        ItemStack riftPet = menu.getSlot(30).getItem();
        if (!riftPet.is(Items.PLAYER_HEAD) || SidePanelUtils.isPlaceholderPane(riftPet)) {
            return;
        }
        SidePanelPets.parsePetItemAndSave(riftPet);
    }

    private static void applyFixedSlots(AbstractContainerMenu menu, int[] indices) {
        if (menu.slots.size() <= indices[4]) {
            return;
        }
        int setIndex = resolveActiveEquipmentSetIndex();
        readRowIntoState(menu, indices[0], indices[1], indices[2], indices[3], indices[4]);
        registerActiveEquipmentSetRow(setIndex);
    }

    private static int resolveActiveEquipmentSetIndex() {
        return SidePanelCache.findBestSetIndex(
                panelEquipmentName(SlotKind.NECKLACE),
                panelEquipmentName(SlotKind.CLOAK),
                panelEquipmentName(SlotKind.BELT),
                panelEquipmentName(SlotKind.GLOVES)
        );
    }

    private static void registerActiveEquipmentSetRow(int setIndex) {
        if (setIndex < 0) {
            return;
        }
        SidePanelCache.registerSetRow(
                setIndex,
                get(SlotKind.NECKLACE),
                get(SlotKind.CLOAK),
                get(SlotKind.BELT),
                get(SlotKind.GLOVES)
        );
    }

    private static String panelEquipmentName(SlotKind kind) {
        ItemStack stack = get(kind);
        return stack.isEmpty() ? "" : SidePanelUtils.normalizeDisplayName(stack.getHoverName().getString());
    }

    private static void readRowIntoState(
            AbstractContainerMenu menu,
            int necklaceIdx,
            int cloakIdx,
            int beltIdx,
            int glovesIdx,
            int petIdx
    ) {
        ItemStack necklace = normalizeEquipment(menu, necklaceIdx);
        ItemStack cloak = normalizeEquipment(menu, cloakIdx);
        ItemStack belt = normalizeEquipment(menu, beltIdx);
        ItemStack gloves = normalizeEquipment(menu, glovesIdx);

        set(SlotKind.NECKLACE, necklace);
        set(SlotKind.CLOAK, cloak);
        set(SlotKind.BELT, belt);
        set(SlotKind.GLOVES, gloves);
        SidePanelCache.registerPassivePreviewRow(necklace, cloak, belt, gloves);
        if (petIdx >= 0) {
            ItemStack pet = menu.getSlot(petIdx).getItem();
            if (!pet.isEmpty() && !SidePanelUtils.isPlaceholderPane(pet)) {
                SidePanelPets.parsePetItemAndSave(pet);
            }
        }
        persistIfNeeded();
    }

    private static ItemStack normalizeEquipment(AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = menu.getSlot(slotIndex).getItem();
        if (stack.isEmpty() || SidePanelUtils.isPlaceholderPane(stack)) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }
}
