package com.shyeuar.baity.features.sidepanel;

import com.mojang.serialization.Codec;
import com.shyeuar.baity.config.BaityConfigDir;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
final class SidePanelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/SidePanelCache");
    private static final Codec<Map<String, ItemStack>> EQUIPMENT_ITEMS_CODEC = Codec.unboundedMap(
            Codec.STRING,
            ItemStack.OPTIONAL_CODEC
    );
    private static final Codec<List<ItemStack>> EQUIPMENT_STACK_ROW_CODEC = ItemStack.OPTIONAL_CODEC
            .listOf(4, 4);

    private static final Map<String, ItemStack> EQUIPMENT_BY_NAME = new HashMap<>();
    private static final Map<Integer, ItemStack[]> SET_ROWS_BY_INDEX = new HashMap<>();

    private static String loadedProfileId;
    private static boolean diskDirty;

    private SidePanelCache() {
    }

    public static void register(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.is(Items.PLAYER_HEAD)) {
            return;
        }
        String key = SidePanelUtils.normalizeDisplayName(stack.getHoverName().getString());
        if (key.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        EQUIPMENT_BY_NAME.put(key, copy);
        String withoutSlot = SidePanelUtils.stripEquipmentSlotSuffix(key);
        if (!withoutSlot.isEmpty() && !withoutSlot.equals(key)) {
            EQUIPMENT_BY_NAME.putIfAbsent(withoutSlot, copy);
        }
        markLookupDirty();
    }

    public static void registerLoadoutAliases(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.is(Items.PLAYER_HEAD)) {
            return;
        }
        String base = SidePanelUtils.normalizeDisplayName(stack.getHoverName().getString());
        if (base.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        for (String suffix : new String[] {" Necklace", " Cloak", " Belt", " Gloves", " Bracelet"}) {
            EQUIPMENT_BY_NAME.put(base + suffix, copy);
        }
        markLookupDirty();
    }

    public static void registerAll(ItemStack... stacks) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            register(stack);
        }
    }

    public static void registerSet(ItemStack necklace, ItemStack cloak, ItemStack belt, ItemStack gloves) {
        registerAll(necklace, cloak, belt, gloves);
    }

    static void registerSetRow(
            int setIndex,
            ItemStack necklace,
            ItemStack cloak,
            ItemStack belt,
            ItemStack gloves
    ) {
        if (setIndex < 0) {
            return;
        }
        registerSet(necklace, cloak, belt, gloves);
        SET_ROWS_BY_INDEX.put(setIndex, copyRow(necklace, cloak, belt, gloves));
        markLookupDirty();
    }

    static ItemStack[] getSetRow(int setIndex) {
        if (setIndex < 0) {
            return copyRow(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        }
        ItemStack[] row = SET_ROWS_BY_INDEX.get(setIndex);
        if (row == null) {
            return copyRow(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        }
        return copyRow(row[0], row[1], row[2], row[3]);
    }

    static void registerPassivePreviewRow(
            ItemStack necklace,
            ItemStack cloak,
            ItemStack belt,
            ItemStack gloves
    ) {
        registerSet(necklace, cloak, belt, gloves);
        for (ItemStack stack : new ItemStack[] {necklace, cloak, belt, gloves}) {
            if (!stack.isEmpty()) {
                registerLoadoutAliases(stack);
            }
        }
    }

    static int findBestSetIndex(String necklace, String cloak, String belt, String gloves) {
        EquipmentSetKey query = keyFromEffectiveTooltipLines(necklace, cloak, belt, gloves);
        int required = countNamedFields(query);
        if (required == 0) {
            return -1;
        }
        int bestIndex = -1;
        int bestScore = 0;
        for (Map.Entry<Integer, ItemStack[]> entry : SET_ROWS_BY_INDEX.entrySet()) {
            EquipmentSetKey registered = keyFromStacks(
                    entry.getValue()[0],
                    entry.getValue()[1],
                    entry.getValue()[2],
                    entry.getValue()[3]
            );
            if (registered == null || registered.isEmpty()) {
                continue;
            }
            int score = scoreSetAgainstQuery(registered, query);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = entry.getKey();
            }
        }
        int minimum = required * 10;
        return bestScore >= minimum ? bestIndex : -1;
    }

    static boolean setIndexMatchesTooltip(int setIndex, String necklace, String cloak, String belt, String gloves) {
        if (setIndex < 0) {
            return false;
        }
        ItemStack[] row = SET_ROWS_BY_INDEX.get(setIndex);
        if (row == null) {
            return false;
        }
        EquipmentSetKey registered = keyFromStacks(row[0], row[1], row[2], row[3]);
        EquipmentSetKey query = keyFromEffectiveTooltipLines(necklace, cloak, belt, gloves);
        int required = countNamedFields(query);
        if (registered == null || required == 0 || registered.isEmpty()) {
            return false;
        }
        return scoreSetAgainstQuery(registered, query) >= required * 10;
    }

    static void markLookupDirty() {
        diskDirty = true;
    }

    static void loadIfProfileChanged(Minecraft client, String profileId) {
        if (profileId == null || profileId.isEmpty() || client.level == null) {
            return;
        }
        if (profileId.equals(loadedProfileId)) {
            return;
        }
        flush(client);
        clearLookupCache();
        SidePanelPets.clearLookupCache();
        loadedProfileId = profileId;
        diskDirty = false;
        loadFromDisk(client, profileId);
    }

    static void ensureLookupLoaded(Minecraft client, String profileId) {
        if (profileId == null || profileId.isEmpty() || client.level == null) {
            return;
        }
        if (!profileId.equals(loadedProfileId)) {
            loadIfProfileChanged(client, profileId);
        }
    }

    static void flush(Minecraft client) {
        if (!diskDirty || loadedProfileId == null || client.level == null) {
            return;
        }
        saveToDisk(client, loadedProfileId);
        diskDirty = false;
    }

    static void clearSession() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            flush(client);
        }
        loadedProfileId = null;
        diskDirty = false;
    }

    static void clearLookupCache() {
        EQUIPMENT_BY_NAME.clear();
        SET_ROWS_BY_INDEX.clear();
    }

    static void importItems(Map<String, ItemStack> items) {
        if (items == null) {
            return;
        }
        for (Map.Entry<String, ItemStack> entry : items.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            ItemStack stack = entry.getValue();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            EQUIPMENT_BY_NAME.put(entry.getKey(), stack.copy());
        }
    }

    static Map<String, ItemStack> exportItems() {
        Map<String, ItemStack> copy = new HashMap<>();
        for (Map.Entry<String, ItemStack> entry : EQUIPMENT_BY_NAME.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public static void registerCurrentPanelSlots() {
        registerSet(
                SidePanel.get(SidePanel.SlotKind.NECKLACE),
                SidePanel.get(SidePanel.SlotKind.CLOAK),
                SidePanel.get(SidePanel.SlotKind.BELT),
                SidePanel.get(SidePanel.SlotKind.GLOVES)
        );
    }

    private static int countNamedFields(EquipmentSetKey query) {
        int count = 0;
        if (query.necklace() != null && !query.necklace().isEmpty()) {
            count++;
        }
        if (query.cloak() != null && !query.cloak().isEmpty()) {
            count++;
        }
        if (query.belt() != null && !query.belt().isEmpty()) {
            count++;
        }
        if (query.gloves() != null && !query.gloves().isEmpty()) {
            count++;
        }
        return count;
    }

    private static int scoreSetAgainstQuery(EquipmentSetKey registered, EquipmentSetKey query) {
        return scoreEquipmentField(query.necklace(), registered.necklace())
                + scoreEquipmentField(query.cloak(), registered.cloak())
                + scoreEquipmentField(query.belt(), registered.belt())
                + scoreEquipmentField(query.gloves(), registered.gloves());
    }

    private static int scoreEquipmentField(String queryLine, String registeredKey) {
        if (queryLine == null || queryLine.isEmpty() || registeredKey == null || registeredKey.isEmpty()) {
            return 0;
        }
        String queryKey = canonicalEquipmentName(queryLine);
        if (queryKey.isEmpty()) {
            return 0;
        }
        String queryStripped = SidePanelUtils.stripEquipmentSlotSuffix(queryKey);
        String registeredStripped = SidePanelUtils.stripEquipmentSlotSuffix(registeredKey);
        if (namesEquivalent(queryKey, registeredKey)
                || namesEquivalent(queryStripped, registeredKey)
                || namesEquivalent(queryKey, registeredStripped)
                || namesEquivalent(queryStripped, registeredStripped)) {
            return 10;
        }
        return 0;
    }

    private static boolean namesEquivalent(String left, String right) {
        return left != null && right != null && !left.isEmpty() && left.equalsIgnoreCase(right);
    }

    private static EquipmentSetKey keyFromStacks(
            ItemStack necklace,
            ItemStack cloak,
            ItemStack belt,
            ItemStack gloves
    ) {
        return new EquipmentSetKey(
                nameKeyFromStack(necklace),
                nameKeyFromStack(cloak),
                nameKeyFromStack(belt),
                nameKeyFromStack(gloves)
        );
    }

    private static EquipmentSetKey keyFromEffectiveTooltipLines(
            String necklace,
            String cloak,
            String belt,
            String gloves
    ) {
        return new EquipmentSetKey(
                effectiveTooltipKey(necklace),
                effectiveTooltipKey(cloak),
                effectiveTooltipKey(belt),
                effectiveTooltipKey(gloves)
        );
    }

    private static String effectiveTooltipKey(String line) {
        if (!SidePanelLoadouts.NameSnapshot.isEffectiveEquipmentName(line)) {
            return "";
        }
        return nameKeyFromTooltip(line);
    }

    private static String nameKeyFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return canonicalEquipmentName(stack.getHoverName().getString());
    }

    private static String nameKeyFromTooltip(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        return canonicalEquipmentName(line);
    }

    private static String canonicalEquipmentName(String name) {
        return SidePanelUtils.normalizeDisplayName(name);
    }

    private static ItemStack[] copyRow(ItemStack necklace, ItemStack cloak, ItemStack belt, ItemStack gloves) {
        return new ItemStack[] {
                copyOrEmpty(necklace),
                copyOrEmpty(cloak),
                copyOrEmpty(belt),
                copyOrEmpty(gloves)
        };
    }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private record EquipmentSetKey(String necklace, String cloak, String belt, String gloves) {
        boolean isEmpty() {
            return necklace.isEmpty() && cloak.isEmpty() && belt.isEmpty() && gloves.isEmpty();
        }
    }

    private static void loadFromDisk(Minecraft client, String profileId) {
        Path file = cacheFile(profileId);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            RegistryAccess registries = client.level.registryAccess();
            Tag rootTag = NbtIo.read(file);
            if (!(rootTag instanceof CompoundTag root)) {
                return;
            }
            loadEquipmentSection(registries, root);
            SidePanelPets.importLookupSectionFromRoot(root);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to load loadout lookup cache {}: {}", file, exception.toString());
        }
    }

    private static void loadEquipmentSection(RegistryAccess registries, CompoundTag root) {
        root.getCompound("equipmentItems").ifPresent(itemsTag -> {
            Map<String, ItemStack> items = EQUIPMENT_ITEMS_CODEC.parse(
                    registries.createSerializationContext(NbtOps.INSTANCE),
                    itemsTag
            ).result().orElse(Map.of());
            importItems(items);
        });
        loadSetRowsSection(registries, root);
    }

    private static void loadSetRowsSection(RegistryAccess registries, CompoundTag root) {
        ListTag rowsTag = root.getList("equipmentSetRows").orElse(new ListTag());
        for (Tag entryTag : rowsTag) {
            if (!(entryTag instanceof CompoundTag entry)) {
                continue;
            }
            int index = entry.getIntOr("index", -1);
            if (index < 0) {
                continue;
            }
            List<ItemStack> stacks = entry.getList("stacks")
                    .flatMap(list -> EQUIPMENT_STACK_ROW_CODEC.parse(
                            registries.createSerializationContext(NbtOps.INSTANCE),
                            list
                    ).result())
                    .orElse(List.of());
            if (stacks.size() < 4) {
                continue;
            }
            registerSetRow(index, stacks.get(0), stacks.get(1), stacks.get(2), stacks.get(3));
        }
    }

    private static void appendSetRowsSection(RegistryAccess registries, CompoundTag root,
                                             Map<Integer, ItemStack[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        ListTag rowsTag = new ListTag();
        for (Map.Entry<Integer, ItemStack[]> entry : rows.entrySet()) {
            ItemStack[] row = entry.getValue();
            if (row == null) {
                continue;
            }
            Tag stacksTag = EQUIPMENT_STACK_ROW_CODEC.encodeStart(
                    registries.createSerializationContext(NbtOps.INSTANCE),
                    List.of(row[0], row[1], row[2], row[3])
            ).getOrThrow();
            if (!(stacksTag instanceof ListTag listTag)) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("index", entry.getKey());
            entryTag.put("stacks", listTag);
            rowsTag.add(entryTag);
        }
        if (!rowsTag.isEmpty()) {
            root.put("equipmentSetRows", rowsTag);
        }
    }

    private static void saveToDisk(Minecraft client, String profileId) {
        RegistryAccess registries = client.level.registryAccess();
        Map<String, ItemStack> items = exportItems();
        Map<Integer, ItemStack[]> setRows = snapshotSetRows();
        CompoundTag pets = SidePanelPets.exportLookupSection();
        com.shyeuar.baity.utils.AsyncFileWriter.writeLater("loadout lookup cache", cacheDir(),
                cacheFile(profileId), () -> buildCacheRoot(registries, items, setRows, pets));
    }

    private static Map<Integer, ItemStack[]> snapshotSetRows() {
        Map<Integer, ItemStack[]> snapshot = new HashMap<>();
        for (Map.Entry<Integer, ItemStack[]> entry : SET_ROWS_BY_INDEX.entrySet()) {
            ItemStack[] row = entry.getValue();
            if (row == null || row.length < 4) {
                continue;
            }
            ItemStack[] copy = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                copy[i] = row[i] == null ? ItemStack.EMPTY : row[i].copy();
            }
            snapshot.put(entry.getKey(), copy);
        }
        return snapshot;
    }

    private static Tag buildCacheRoot(RegistryAccess registries, Map<String, ItemStack> items,
                                      Map<Integer, ItemStack[]> setRows, CompoundTag pets) {
        CompoundTag root = new CompoundTag();
        if (!items.isEmpty()) {
            Tag itemsTag = EQUIPMENT_ITEMS_CODEC.encodeStart(
                    registries.createSerializationContext(NbtOps.INSTANCE),
                    items
            ).getOrThrow();
            if (itemsTag instanceof CompoundTag compound) {
                root.put("equipmentItems", compound);
            }
        }
        appendSetRowsSection(registries, root, setRows);
        root.put("pets", pets);
        return root;
    }

    private static Path cacheDir() {
        return BaityConfigDir.getBaityConfigDir().resolve("sidepanel");
    }

    private static Path cacheFile(String profileId) {
        return cacheDir().resolve(profileId + "-loadout-cache.nbt");
    }
}
