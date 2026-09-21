package com.shyeuar.baity.features.sidepanel;

import com.mojang.serialization.Codec;
import com.shyeuar.baity.config.BaityConfigDir;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class SidePanelLoadouts {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/SidePanelLoadouts");
    private static final int UNRESOLVED_INDEX = -1;
    private static final Pattern PET_LINE = Pattern.compile(
            "^Pet:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NECKLACE_LINE = Pattern.compile(
            "^Necklace:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CLOAK_LINE = Pattern.compile(
            "^Cloak:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BELT_LINE = Pattern.compile(
            "^Belt:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GLOVES_LINE = Pattern.compile(
            "^(?:Gloves|Bracelet):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ARMOR_LINE = Pattern.compile(
            "^(?:Helmet|Chestplate|Leggings|Boots):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Codec<Map<String, NameSnapshot>> PAGE_CODEC = Codec.unboundedMap(
            Codec.STRING,
            NameSnapshot.CODEC
    ).fieldOf("entries").codec();

    private static String loadedProfileId;
    private static final Map<Integer, Map<Integer, NameSnapshot>> namesByPage = new HashMap<>();
    private static int lastScannedPage = -1;
    private static int pendingAutoCloseIndex = -1;
    private static boolean namesDirty;

    private SidePanelLoadouts() {
    }

    public record NameSnapshot(
            String necklace,
            String cloak,
            String belt,
            String gloves,
            String petLine,
            int eqSetIndex,
            int petIndex
    ) {
        static final Codec<NameSnapshot> CODEC = Codec.STRING.listOf().xmap(
                NameSnapshot::fromParts,
                NameSnapshot::toParts
        );

        private static NameSnapshot fromParts(List<String> parts) {
            return new NameSnapshot(
                    part(parts, 0),
                    part(parts, 1),
                    part(parts, 2),
                    part(parts, 3),
                    part(parts, 4),
                    parseIndex(parts, 5),
                    parseIndex(parts, 6)
            );
        }

        private static List<String> toParts(NameSnapshot snapshot) {
            return List.of(
                    nullToEmpty(snapshot.necklace),
                    nullToEmpty(snapshot.cloak),
                    nullToEmpty(snapshot.belt),
                    nullToEmpty(snapshot.gloves),
                    nullToEmpty(snapshot.petLine),
                    indexToString(snapshot.eqSetIndex),
                    indexToString(snapshot.petIndex)
            );
        }

        private static String part(List<String> parts, int index) {
            return index < parts.size() ? parts.get(index) : "";
        }

        private static int parseIndex(List<String> parts, int index) {
            if (index >= parts.size()) {
                return UNRESOLVED_INDEX;
            }
            String value = parts.get(index);
            if (value == null || value.isEmpty()) {
                return UNRESOLVED_INDEX;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return UNRESOLVED_INDEX;
            }
        }

        private static String indexToString(int index) {
            return index < 0 ? "" : String.valueOf(index);
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        static boolean isEffectiveEquipmentName(String value) {
            if (value == null || value.isEmpty()) {
                return false;
            }
            String plain = SidePanelUtils.normalizeDisplayName(value);
            if (plain.isEmpty()) {
                return false;
            }
            if (plain.equalsIgnoreCase("none")) {
                return false;
            }
            return !plain.regionMatches(true, 0, "no ", 0, 3);
        }

        boolean hasNamedEquipment() {
            return isEffectiveEquipmentName(necklace)
                    || isEffectiveEquipmentName(cloak)
                    || isEffectiveEquipmentName(belt)
                    || isEffectiveEquipmentName(gloves);
        }

        boolean expectsPet() {
            if (petLine == null || petLine.isEmpty()) {
                return false;
            }
            String plain = SidePanelUtils.normalizeDisplayName(petLine);
            return !plain.equalsIgnoreCase("none") && !plain.equalsIgnoreCase("no pet");
        }
    }

    public static void load(Minecraft client, String profileId) {
        if (profileId == null || profileId.isEmpty() || client.level == null) {
            return;
        }
        if (profileId.equals(loadedProfileId)) {
            return;
        }
        save();
        namesByPage.clear();
        lastScannedPage = -1;
        loadedProfileId = profileId;
        Path file = cacheFile(profileId);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            RegistryAccess registries = client.level.registryAccess();
            Tag root = NbtIo.read(file);
            Map<String, NameSnapshot> flat = PAGE_CODEC.parse(
                    registries.createSerializationContext(NbtOps.INSTANCE),
                    root
            ).getOrThrow();
            for (Map.Entry<String, NameSnapshot> entry : flat.entrySet()) {
                int[] pageAndIndex = parsePageKey(entry.getKey());
                if (pageAndIndex == null) {
                    continue;
                }
                namesByPage.computeIfAbsent(pageAndIndex[0], _ -> new HashMap<>())
                        .put(pageAndIndex[1], entry.getValue());
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to load loadout cache {}: {}", file, exception.toString());
        }
    }

    public static void save() {
        flushNamesToDisk();
    }

    static void flushNamesIfDirty() {
        if (!namesDirty) {
            return;
        }
        flushNamesToDisk();
        namesDirty = false;
    }

    private static void flushNamesToDisk() {
        if (loadedProfileId == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Map<String, NameSnapshot> flat = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, NameSnapshot>> pageEntry : namesByPage.entrySet()) {
            for (Map.Entry<Integer, NameSnapshot> indexEntry : pageEntry.getValue().entrySet()) {
                flat.put(pageKey(pageEntry.getKey(), indexEntry.getKey()), indexEntry.getValue());
            }
        }
        try {
            Files.createDirectories(cacheDir());
            Tag root = PAGE_CODEC.encodeStart(
                    client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    flat
            ).getOrThrow();
            try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(cacheFile(loadedProfileId)))) {
                NbtIo.writeUnnamedTagWithFallback(root, output);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to save loadout cache: {}", exception.toString());
        }
    }

    public static void clearSession() {
        save();
        loadedProfileId = null;
        namesByPage.clear();
        lastScannedPage = -1;
        pendingAutoCloseIndex = -1;
    }

    static void armAutoCloseApply(int localIndex, AbstractContainerMenu menu, Component title) {
        if (localIndex < 0 || localIndex >= SidePanelMenus.LOADOUT_BUTTON_SLOTS.length) {
            pendingAutoCloseIndex = -1;
            return;
        }
        if (menu != null && title != null && SidePanelMenus.isLoadoutsMenu(title)) {
            refreshLoadoutEntry(menu, title, localIndex);
        }
        pendingAutoCloseIndex = localIndex;
    }

    static void applyPendingAutoCloseSnapshot() {
        if (pendingAutoCloseIndex < 0) {
            return;
        }
        int page = lastScannedPage > 0 ? lastScannedPage : 1;
        NameSnapshot snapshot = namesByPage.getOrDefault(page, Map.of()).get(pendingAutoCloseIndex);
        if (snapshot == null) {
            return;
        }
        applySnapshot(snapshot, page, pendingAutoCloseIndex);
    }

    static void clearPendingAutoCloseApply() {
        pendingAutoCloseIndex = -1;
    }

    static void scanMenu(AbstractContainerMenu menu, Component title) {
        scanMenuInternal(menu, title, true);
    }

    private static void scanMenuInternal(AbstractContainerMenu menu, Component title, boolean force) {
        if (menu == null || title == null || !SidePanelMenus.isLoadoutsMenu(title)) {
            return;
        }
        int page = SidePanelMenus.loadoutPageFromTitle(title);
        if (page < 1) {
            return;
        }
        if (!force && page == lastScannedPage) {
            return;
        }
        Map<Integer, NameSnapshot> pageCache = namesByPage.computeIfAbsent(page, _ -> new HashMap<>());
        for (int localIndex = 0; localIndex < SidePanelMenus.LOADOUT_BUTTON_SLOTS.length; localIndex++) {
            cacheLoadoutEntry(menu, pageCache, localIndex);
        }
        lastScannedPage = page;
        namesDirty = true;
    }

    private static void refreshLoadoutEntry(AbstractContainerMenu menu, Component title, int localIndex) {
        if (menu == null || title == null || !SidePanelMenus.isLoadoutsMenu(title)) {
            return;
        }
        int page = SidePanelMenus.loadoutPageFromTitle(title);
        if (page < 1 || localIndex < 0 || localIndex >= SidePanelMenus.LOADOUT_BUTTON_SLOTS.length) {
            return;
        }
        Map<Integer, NameSnapshot> pageCache = namesByPage.computeIfAbsent(page, _ -> new HashMap<>());
        cacheLoadoutEntry(menu, pageCache, localIndex);
        lastScannedPage = page;
        namesDirty = true;
    }

    private static void cacheLoadoutEntry(
            AbstractContainerMenu menu,
            Map<Integer, NameSnapshot> pageCache,
            int localIndex
    ) {
        int slotIndex = SidePanelMenus.LOADOUT_BUTTON_SLOTS[localIndex];
        if (slotIndex >= menu.slots.size()) {
            return;
        }
        ItemStack stack = menu.getSlot(slotIndex).getItem();
        if (!SidePanelMenus.isLoadoutEquipButton(stack)) {
            return;
        }
        NameSnapshot parsed = parseTooltip(stack);
        if (parsed == null) {
            return;
        }
        NameSnapshot existing = pageCache.get(localIndex);
        if (existing != null) {
            parsed = preserveIndices(parsed, existing);
        }
        pageCache.put(localIndex, reconcileAssociations(parsed));
    }

    static void tickIfOpen(Minecraft client) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Component title = screen.getTitle();
        if (!SidePanelMenus.isLoadoutsMenu(title)) {
            return;
        }
        int page = SidePanelMenus.loadoutPageFromTitle(title);
        if (page < 1) {
            return;
        }
        if (page == lastScannedPage) {
            return;
        }
        scanMenuInternal(screen.getMenu(), title, false);
    }

    private static NameSnapshot preserveIndices(NameSnapshot parsed, NameSnapshot existing) {
        int eqSetIndex = existing.eqSetIndex();
        int petIndex = existing.petIndex();
        if (parsed.hasNamedEquipment()
                && eqSetIndex >= 0
                && !SidePanelCache.setIndexMatchesTooltip(
                        eqSetIndex,
                        parsed.necklace(),
                        parsed.cloak(),
                        parsed.belt(),
                        parsed.gloves()
                )) {
            eqSetIndex = UNRESOLVED_INDEX;
        }
        if (parsed.expectsPet()
                && petIndex >= 0
                && !SidePanelPets.petIndexMatchesTooltip(petIndex, parsed.petLine())) {
            petIndex = UNRESOLVED_INDEX;
        }
        return new NameSnapshot(
                parsed.necklace(),
                parsed.cloak(),
                parsed.belt(),
                parsed.gloves(),
                parsed.petLine(),
                eqSetIndex,
                petIndex
        );
    }

    private static NameSnapshot reconcileAssociations(NameSnapshot snapshot) {
        int eqSetIndex = snapshot.eqSetIndex();
        if (snapshot.hasNamedEquipment()) {
            if (eqSetIndex < 0
                    || !SidePanelCache.setIndexMatchesTooltip(
                            eqSetIndex,
                            snapshot.necklace(),
                            snapshot.cloak(),
                            snapshot.belt(),
                            snapshot.gloves()
                    )) {
                eqSetIndex = SidePanelCache.findBestSetIndex(
                        snapshot.necklace(),
                        snapshot.cloak(),
                        snapshot.belt(),
                        snapshot.gloves()
                );
            }
        } else {
            eqSetIndex = UNRESOLVED_INDEX;
        }

        int petIndex = snapshot.petIndex();
        if (snapshot.expectsPet()) {
            if (petIndex < 0 || !SidePanelPets.petIndexMatchesTooltip(petIndex, snapshot.petLine())) {
                petIndex = SidePanelPets.findBestPetIndex(snapshot.petLine());
            }
        } else {
            petIndex = UNRESOLVED_INDEX;
        }

        if (eqSetIndex == snapshot.eqSetIndex() && petIndex == snapshot.petIndex()) {
            return snapshot;
        }
        return new NameSnapshot(
                snapshot.necklace(),
                snapshot.cloak(),
                snapshot.belt(),
                snapshot.gloves(),
                snapshot.petLine(),
                eqSetIndex,
                petIndex
        );
    }

    private static void applySnapshot(NameSnapshot snapshot, int page, int localIndex) {
        NameSnapshot resolved = reconcileAssociations(snapshot);
        if (resolved.eqSetIndex() < 0 && snapshot.eqSetIndex() >= 0 && resolved.hasNamedEquipment()) {
            resolved = withEquipmentIndex(resolved, snapshot.eqSetIndex());
        }
        if (!resolved.equals(snapshot)) {
            namesByPage.computeIfAbsent(page, _ -> new HashMap<>()).put(localIndex, resolved);
            namesDirty = true;
        }
        applyEquipmentFromBoundSet(resolved);
        applyBoundPet(resolved);
    }

    private static NameSnapshot withEquipmentIndex(NameSnapshot snapshot, int eqSetIndex) {
        return new NameSnapshot(
                snapshot.necklace(),
                snapshot.cloak(),
                snapshot.belt(),
                snapshot.gloves(),
                snapshot.petLine(),
                eqSetIndex,
                snapshot.petIndex()
        );
    }

    private static void applyEquipmentFromBoundSet(NameSnapshot snapshot) {
        if (!snapshot.hasNamedEquipment()) {
            return;
        }
        int setIndex = snapshot.eqSetIndex();
        if (setIndex < 0) {
            return;
        }
        ItemStack[] row = SidePanelCache.getSetRow(setIndex);
        SidePanel.set(SidePanel.SlotKind.NECKLACE, row[0]);
        SidePanel.set(SidePanel.SlotKind.CLOAK, row[1]);
        SidePanel.set(SidePanel.SlotKind.BELT, row[2]);
        SidePanel.set(SidePanel.SlotKind.GLOVES, row[3]);
        SidePanelCache.registerCurrentPanelSlots();
    }

    private static void applyBoundPet(NameSnapshot snapshot) {
        if (!snapshot.expectsPet()) {
            return;
        }
        int petIndex = snapshot.petIndex();
        if (petIndex < 0) {
            return;
        }
        ItemStack pet = SidePanelPets.getPetByIndex(petIndex);
        if (!pet.isEmpty()) {
            SidePanelPets.parsePetItemAndSave(pet);
        }
    }

    private static NameSnapshot parseTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return null;
        }

        String necklace = null;
        String cloak = null;
        String belt = null;
        String gloves = null;
        String petLine = null;

        for (Component line : lore.lines()) {
            String plain = SidePanelUtils.normalizeDisplayName(line.getString());
            if (plain.isEmpty()) {
                continue;
            }
            if (isSectionEnd(plain)) {
                break;
            }
            Matcher petMatcher = PET_LINE.matcher(plain);
            if (petMatcher.matches()) {
                petLine = petMatcher.group(1).trim();
                continue;
            }
            if (ARMOR_LINE.matcher(plain).matches()) {
                continue;
            }
            Matcher necklaceMatcher = NECKLACE_LINE.matcher(plain);
            if (necklaceMatcher.matches()) {
                necklace = necklaceMatcher.group(1).trim();
                continue;
            }
            Matcher cloakMatcher = CLOAK_LINE.matcher(plain);
            if (cloakMatcher.matches()) {
                cloak = cloakMatcher.group(1).trim();
                continue;
            }
            Matcher beltMatcher = BELT_LINE.matcher(plain);
            if (beltMatcher.matches()) {
                belt = beltMatcher.group(1).trim();
                continue;
            }
            Matcher glovesMatcher = GLOVES_LINE.matcher(plain);
            if (glovesMatcher.matches()) {
                gloves = glovesMatcher.group(1).trim();
            }
        }

        if (necklace == null && cloak == null && belt == null && gloves == null && petLine == null) {
            return null;
        }
        return new NameSnapshot(necklace, cloak, belt, gloves, petLine, UNRESOLVED_INDEX, UNRESOLVED_INDEX);
    }

    private static boolean isSectionEnd(String plain) {
        return plain.startsWith("HOTM:")
                || plain.startsWith("HOTF:")
                || plain.startsWith("Power Stone:")
                || plain.startsWith("Tuning Template")
                || plain.equals("Left-click to equip!")
                || plain.startsWith("Right-click to edit")
                || plain.startsWith("Lowest BIN")
                || plain.startsWith("Bazaar ")
                || plain.startsWith("Obtained:")
                || plain.startsWith("Item ID:")
                || plain.startsWith("Museum:");
    }

    private static String pageKey(int page, int localIndex) {
        return page + ":" + localIndex;
    }

    private static int[] parsePageKey(String key) {
        int split = key.indexOf(':');
        if (split <= 0 || split >= key.length() - 1) {
            return null;
        }
        try {
            return new int[] {
                    Integer.parseInt(key.substring(0, split)),
                    Integer.parseInt(key.substring(split + 1))
            };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Path cacheDir() {
        return BaityConfigDir.getBaityConfigDir().resolve("sidepanel");
    }

    private static Path cacheFile(String profileId) {
        return cacheDir().resolve(profileId + "-loadouts.nbt");
    }
}
