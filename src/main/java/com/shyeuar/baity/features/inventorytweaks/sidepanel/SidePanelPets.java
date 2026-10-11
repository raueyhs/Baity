package com.shyeuar.baity.features.inventorytweaks.sidepanel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shyeuar.baity.features.droppeditem.SkyblockItemRarity;
import com.shyeuar.baity.utils.ComponentTextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class SidePanelPets {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/SidePanelPets");
    private static final Pattern PETS_TITLE = Pattern.compile("(?:\\((\\d+)/\\d+\\) +)?Pets *");
    private static final Pattern FAVORITE_PATTERN = Pattern.compile("(?i)(§r)?§e⭐ ");
    private static final Pattern PET_LEVEL_PATTERN = Pattern.compile(
            "(§7\\[Lvl )(?<level>\\d+)(] )(§8\\[§.)?(?<cosmeticLevel>\\d+)?(.*)"
    );
    private static final Pattern STRIP_PET_NAME = Pattern.compile("]\\s*(?:§.)*([\\w- ]+(?:§. ✦)?)");
    private static final Pattern AUTOPET_PATTERN = Pattern.compile(
            "§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)](?: §8\\[§6\\d+§8§.✦§8])? §(?<rarityColor>.)(?<name>.*)§e!(?: §a§lVIEW RULE§r)?"
    );
    private static final Pattern PET_LEVELED_UP_PATTERN = Pattern.compile(
            "§aYour §r§(?<rarityColor>.)(?<name>.*?)(?<cosmetic>§r§. ✦)? §r§aleveled up to level §r(?:§.)*(?<newLevel>\\d+)§r§a!§r"
    );
    private static final Pattern PET_ITEM_PATTERN = Pattern.compile(
            "§aYour pet is now holding §r§(?<rarityColor>.)(?<petItem>.*)§r§a.§r"
    );

    private static final Map<Integer, CachedPet> petMap = new HashMap<>();
    private static int currentPetIdx = -1;
    private static int previousPage = -1;
    private static boolean updatePetCache;
    private static boolean cacheDirty;
    private static Integer lastClickedSlot;
    private static Integer lastClickedButton;

    private SidePanelPets() {
    }

    public static void tick(Minecraft client) {
        if (!SidePanel.isPetMenuPassiveActive(client)) {
            updatePetCache = true;
            previousPage = -1;
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            updatePetCache = true;
            previousPage = -1;
            return;
        }
        if (!isPetsMenu(screen.getTitle())) {
            updatePetCache = true;
            previousPage = -1;
            return;
        }

        int page = pageNum(screen.getTitle());
        if (page != previousPage) {
            updatePetCache = true;
        }
        if (!updatePetCache) {
            return;
        }

        var menu = screen.getMenu();
        if (menu.slots.size() < 54) {
            return;
        }

        previousPage = page;
        int pageOffset = 45 * (page == 0 ? 0 : page - 1);

        for (int i = 10; i < 44; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            if (item.isEmpty() || !item.is(Items.PLAYER_HEAD)) {
                continue;
            }

            ItemStack itemCopy = stripTimestamp(item.copy());
            CachedPet newPet = petFromStack(itemCopy);
            if (newPet == null) {
                continue;
            }

            int petIndex = i + pageOffset;
            CachedPet oldPet = petMap.get(petIndex);

            if (oldPet != null && oldPet.matches(newPet)) {
                if (newPet.info.active && syncActivePetEquipment(petIndex, itemCopy)) {
                    cacheDirty = true;
                }
                continue;
            }

            if (newPet.info.active) {
                syncActivePetEquipment(petIndex, itemCopy);
            }

            petMap.put(petIndex, newPet);
            cacheDirty = true;
        }
        updatePetCache = false;
    }

    public static void onPetsMenuInventoryLoaded(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        syncSelectedPetFromMenu(menu);
        updatePetCache = true;
    }

    private static void syncSelectedPetFromMenu(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (menu.slots.size() <= 4) {
            return;
        }
        ItemStack bone = menu.getSlot(4).getItem();
        if (!bone.is(Items.BONE)) {
            return;
        }
        ItemLore lore = bone.get(DataComponents.LORE);
        if (lore == null) {
            return;
        }

        String selectedPetLine = null;
        for (Component line : lore.lines()) {
            String text = line.getString();
            if (text.contains("Selected pet:")) {
                int colon = text.indexOf(':');
                if (colon != -1) {
                    selectedPetLine = text.substring(colon + 2).trim();
                }
                break;
            }
        }
        if (selectedPetLine == null) {
            return;
        }
        if (selectedPetLine.contains("None")) {
            setCurrentPetIndex(-1);
            return;
        }
        if (isCurrentPetValid(selectedPetLine)) {
            return;
        }
        int bestIndex = findBestPetIndexForSelectedLine(selectedPetLine);
        if (bestIndex >= 0) {
            setCurrentPetIndex(bestIndex);
        }
    }

    private static boolean isCurrentPetValid(String selectedPetLine) {
        if (currentPetIdx < 0) {
            return selectedPetLine.contains("None");
        }
        CachedPet currentPet = petMap.get(currentPetIdx);
        if (currentPet == null) {
            return false;
        }
        String resolved = resolveAncientGoldenDragonException(currentPet, selectedPetLine);
        String display = SidePanelUtils.stripFormatting(currentPet.displayName);
        return display.endsWith(SidePanelUtils.stripFormatting(resolved));
    }

    private static int findBestPetIndexForSelectedLine(String selectedPetLine) {
        int bestIndex = Integer.MIN_VALUE;
        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            CachedPet pet = entry.getValue();
            String resolved = resolveAncientGoldenDragonException(pet, selectedPetLine);
            String display = SidePanelUtils.stripFormatting(pet.displayName);
            if (display.endsWith(SidePanelUtils.stripFormatting(resolved))) {
                if (bestIndex == Integer.MIN_VALUE) {
                    bestIndex = entry.getKey();
                } else {
                    bestIndex = Integer.MIN_VALUE;
                    break;
                }
            }
        }
        return bestIndex == Integer.MIN_VALUE ? -1 : bestIndex;
    }

    private static String resolveAncientGoldenDragonException(CachedPet pet, String selectedPetLine) {
        if (pet.info.skin != null && "GOLDEN_DRAGON_ANCIENT".equals(pet.info.skin)) {
            return selectedPetLine.replace(" ✦", "");
        }
        return selectedPetLine;
    }

    private static boolean isPetGridSlot(int slotId) {
        return slotId >= 10 && slotId <= 43;
    }

    static int findBestPetIndex(String petLine) {
        if (petLine == null || petLine.isEmpty()) {
            return -1;
        }
        String plain = SidePanelUtils.stripFormatting(petLine).trim();
        if (plain.equalsIgnoreCase("none") || plain.equalsIgnoreCase("no pet")) {
            return -1;
        }
        int bestIndex = -1;
        int bestScore = 0;
        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            int score = scorePetForLoadoutLine(entry.getValue(), plain);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = entry.getKey();
            }
        }
        return bestScore > 0 ? bestIndex : -1;
    }

    static boolean petIndexMatchesTooltip(int petIndex, String petLine) {
        if (petIndex < 0 || petLine == null || petLine.isEmpty()) {
            return false;
        }
        CachedPet pet = petMap.get(petIndex);
        if (pet == null) {
            return false;
        }
        String plain = SidePanelUtils.stripFormatting(petLine).trim();
        return scorePetForLoadoutLine(pet, plain) >= 100;
    }

    static ItemStack getPetByIndex(int petIndex) {
        if (petIndex < 0) {
            return ItemStack.EMPTY;
        }
        CachedPet pet = petMap.get(petIndex);
        if (pet == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = pet.decodeItem(Minecraft.getInstance());
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static int scorePetForLoadoutLine(CachedPet pet, String plainPetLine) {
        String lineName = SidePanelUtils.stripFormatting(stripPetName(plainPetLine)).trim();
        String petName = SidePanelUtils.stripFormatting(stripPetName(pet.displayName)).trim();
        if (lineName.isEmpty() || petName.isEmpty()) {
            return 0;
        }

        int score = 0;
        if (lineName.equalsIgnoreCase(petName)) {
            score += 100;
        } else if (lineName.contains(petName) || petName.contains(lineName)) {
            score += 60;
        } else {
            return 0;
        }

        int lineLevel = petLevelFromDisplayName(plainPetLine);
        if (lineLevel >= 0) {
            if (lineLevel == pet.petLevel) {
                score += 20;
            } else {
                score += 5;
            }
        }
        if (pet.info.active) {
            score += 1;
        }
        return score;
    }

    public static void handleChat(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!SidePanel.isPetChatPassiveActive(client)) {
            return;
        }
        SidePanelCache.ensureLookupLoaded(client, SidePanel.profileId(client));
        String formatted = ComponentTextUtils.getFormattedText(message);
        String autopetFormatted = ComponentTextUtils.stripLegacyResets(formatted);
        Matcher matcher;

        if ((matcher = AUTOPET_PATTERN.matcher(autopetFormatted)).find()) {
            findCurrentPetFromAutopet(matcher.group("level"), matcher.group("rarityColor"), matcher.group("name"));
        } else if ((matcher = PET_LEVELED_UP_PATTERN.matcher(formatted)).find()) {
            int newLevel = Integer.parseInt(matcher.group("newLevel"));
            String petName = matcher.group("name");
            String cosmetic = matcher.group("cosmetic");
            if (!StringUtil.isNullOrEmpty(cosmetic) && newLevel <= 200) {
                petName += cosmetic.replace("§r", "");
            }
            updateAndSetCurrentLevelledPet(newLevel, matcher.group("rarityColor"), petName);
        } else if ((matcher = PET_ITEM_PATTERN.matcher(formatted)).find()) {
            updateCurrentPetHeldItem(matcher.group("petItem"));
        }
    }

    public static void trackMenuSlotClick(AbstractContainerScreen<?> screen, int slotId, int button) {
        if (!SidePanel.isSyncActive(Minecraft.getInstance())
                || !SidePanel.usesGlobalPetCache(Minecraft.getInstance())
                || !isPetsMenu(screen.getTitle())) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.hasShiftDown()) {
            if (button == 0) {
                updatePetCache = true;
                clearLastClick();
            }
            return;
        }
        if (slotId < 54 && button == 1 && isPetGridSlot(slotId)) {
            int index = slotId + pageOffset(screen.getTitle());
            if (petMap.remove(index) != null) {
                updatePetCache = true;
                cacheDirty = true;
            }
            clearLastClick();
            return;
        }
        if (!isPetGridSlot(slotId)) {
            clearLastClick();
            return;
        }
        lastClickedSlot = slotId;
        lastClickedButton = button;
    }

    public static void onPetsMenuClose(Component title) {
        if (!SidePanel.usesGlobalPetCache(Minecraft.getInstance())) {
            clearLastClick();
            return;
        }
        if (!isPetsMenu(title)) {
            return;
        }
        if (lastClickedSlot == null || lastClickedSlot >= 54) {
            clearLastClick();
            return;
        }

        int index = lastClickedSlot + pageOffset(title);
        Integer clickedButton = lastClickedButton;
        clearLastClick();

        CachedPet cached = petMap.get(index);
        if (cached == null) {
            return;
        }
        if (cached.info.active) {
            setCurrentPetIndex(-1);
            return;
        }
        if (clickedButton != null && clickedButton == 1) {
            return;
        }
        setCurrentPetIndex(index);
    }

    private static void clearLastClick() {
        lastClickedSlot = null;
        lastClickedButton = null;
    }

    public static void flushCacheIfDirty() {
        if (!cacheDirty) {
            return;
        }
        cacheDirty = false;
        SidePanelCache.markLookupDirty();
    }

    static void clearLookupCache() {
        petMap.clear();
        currentPetIdx = -1;
        updatePetCache = true;
        previousPage = -1;
    }

    static CompoundTag exportLookupSection() {
        CompoundTag root = new CompoundTag();
        root.putInt("currentPetIdx", currentPetIdx);
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            mapTag.put(String.valueOf(entry.getKey()), entry.getValue().encode());
        }
        root.put("petMap", mapTag);
        return root;
    }

    static void importLookupSection(CompoundTag root) {
        if (root == null) {
            return;
        }
        currentPetIdx = root.getIntOr("currentPetIdx", -1);
        petMap.clear();
        root.getCompound("petMap").ifPresent(mapTag -> {
            for (String key : mapTag.keySet()) {
                try {
                    int index = Integer.parseInt(key);
                    CompoundTag entry = mapTag.getCompound(key).orElseThrow();
                    CachedPet pet = CachedPet.decode(entry);
                    if (pet != null) {
                        petMap.put(index, pet);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid pet cache entry {}: {}", key, exception.toString());
                }
            }
        });
    }

    static void importLookupSectionFromRoot(CompoundTag fileRoot) {
        if (fileRoot == null) {
            return;
        }
        fileRoot.getCompound("pets").ifPresent(SidePanelPets::importLookupSection);
    }

    public static void parsePetItemAndSave(ItemStack petItem) {
        if (petItem == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (SidePanelUtils.isPlaceholderPane(petItem)) {
            SidePanel.set(SidePanel.SlotKind.PET, ItemStack.EMPTY);
            SidePanel.persistNow();
            if (SidePanel.usesGlobalPetCache(client)) {
                setCurrentPetIndex(-1, false);
            }
            return;
        }

        if (!petItem.is(Items.PLAYER_HEAD)) {
            return;
        }

        ItemStack itemCopy = stripTimestamp(petItem.copy());

        if (!SidePanel.usesGlobalPetCache(client)) {
            SidePanel.set(SidePanel.SlotKind.PET, itemCopy);
            SidePanel.persistNow();
            return;
        }

        CachedPet newPet = petFromStack(itemCopy);
        if (newPet == null) {
            return;
        }

        int index = upsertPetInMap(newPet, itemCopy);
        setCurrentPetIndex(index);
    }

    private static int upsertPetInMap(CachedPet newPet, ItemStack itemCopy) {
        UUID newId = newPet.info.uniqueId;
        if (newId != null) {
            for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
                CachedPet existing = entry.getValue();
                if (newId.equals(existing.info.uniqueId)) {
                    int index = entry.getKey();
                    petMap.put(index, newPet);
                    ItemStack oldItem = existing.decodeItem(Minecraft.getInstance());
                    if (oldItem == null || !ItemStack.matches(oldItem, itemCopy)) {
                        cacheDirty = true;
                    }
                    return index;
                }
            }
        }

        int byLine = findBestPetIndex(newPet.displayName);
        if (byLine >= 0) {
            petMap.put(byLine, newPet);
            cacheDirty = true;
            return byLine;
        }

        int index = petMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(9) + 1;
        petMap.put(index, newPet);
        cacheDirty = true;
        return index;
    }

    static void applyLoadedCurrentPet() {
        if (currentPetIdx < 0 || !petMap.containsKey(currentPetIdx)) {
            return;
        }
        setCurrentPetIndex(currentPetIdx, true);
    }

    public static boolean isPetsMenu(Component title) {
        return title != null && PETS_TITLE.matcher(SidePanelUtils.stripFormatting(title.getString())).matches();
    }

    private static boolean syncActivePetEquipment(int petIndex, ItemStack itemCopy) {
        Minecraft client = Minecraft.getInstance();
        if (!SidePanel.usesGlobalPetCache(client)) {
            return false;
        }
        currentPetIdx = petIndex;
        ItemStack equipped = SidePanel.get(SidePanel.SlotKind.PET);
        if (!ItemStack.matches(itemCopy, equipped)) {
            SidePanel.set(SidePanel.SlotKind.PET, itemCopy);
            SidePanel.persistNow();
            return true;
        }
        return false;
    }

    private static void setCurrentPetIndex(int index) {
        setCurrentPetIndex(index, true);
    }

    private static void setCurrentPetIndex(int index, boolean updateEquipment) {
        if (currentPetIdx != index) {
            currentPetIdx = index;
            cacheDirty = true;
        }
        syncActiveFlagsInCache(index);

        if (!updateEquipment) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (!SidePanel.usesGlobalPetCache(client)) {
            return;
        }

        if (index == -1) {
            if (!SidePanel.get(SidePanel.SlotKind.PET).isEmpty()) {
                SidePanel.set(SidePanel.SlotKind.PET, ItemStack.EMPTY);
                SidePanel.persistNow();
            }
            return;
        }

        CachedPet pet = petMap.get(index);
        if (pet == null) {
            return;
        }
        ItemStack stack = pet.decodeItem(Minecraft.getInstance());
        if (stack != null && !ItemStack.matches(stack, SidePanel.get(SidePanel.SlotKind.PET))) {
            SidePanel.set(SidePanel.SlotKind.PET, stack);
            SidePanel.persistNow();
        }
    }

    private static void syncActiveFlagsInCache(int equippedIndex) {
        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            boolean shouldBeActive = equippedIndex >= 0 && entry.getKey() == equippedIndex;
            if (entry.getValue().info.active != shouldBeActive) {
                entry.getValue().info.active = shouldBeActive;
                cacheDirty = true;
            }
        }
    }

    private static void updateCurrentPetHeldItem(String petItemDisplay) {
        if (petItemDisplay == null || petItemDisplay.isEmpty()) {
            return;
        }
        CachedPet currentPet = currentPetIdx >= 0 ? petMap.get(currentPetIdx) : null;
        if (currentPet == null) {
            return;
        }
        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            CachedPet pet = entry.getValue();
            if (pet.info.uniqueId != null && pet.info.uniqueId.equals(currentPet.info.uniqueId)) {
                pet.info.heldItemId = SidePanelUtils.stripFormatting(petItemDisplay);
                petMap.put(entry.getKey(), pet);
                cacheDirty = true;
                if (entry.getKey() == currentPetIdx) {
                    setCurrentPetIndex(currentPetIdx);
                }
            }
        }
    }

    private static void findCurrentPetFromAutopet(String levelString, String rarityColor, String petName) {
        int level = Integer.parseInt(levelString);
        if (rarityColor == null || rarityColor.isEmpty()) {
            return;
        }
        SkyblockItemRarity rarity = rarityFromColor(rarityColor.charAt(0));
        String targetName = autopetComparableName(petName);

        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            CachedPet pet = entry.getValue();
            if (!autopetComparableName(stripPetName(pet.displayName)).equalsIgnoreCase(targetName)) {
                continue;
            }
            if (pet.petLevel != level) {
                continue;
            }
            if (pet.info.petRarity != rarity && rarity != SkyblockItemRarity.UNKNOWN) {
                continue;
            }
            setCurrentPetIndex(entry.getKey());
            return;
        }

        String autopetLine = "§7[Lvl " + level + "] §" + rarityColor + petName;
        int bestIndex = findBestPetIndex(autopetLine);
        if (bestIndex >= 0) {
            CachedPet best = petMap.get(bestIndex);
            if (best != null && best.petLevel == level) {
                setCurrentPetIndex(bestIndex);
            }
        }
    }

    private static String autopetComparableName(String rawName) {
        return SidePanelUtils.stripFormatting(stripPetName(rawName != null ? rawName : "")).trim();
    }

    private static void updateAndSetCurrentLevelledPet(int newLevel, String rarityColor, String petName) {
        SkyblockItemRarity rarity = rarityFromColor(rarityColor.charAt(0));
        CachedPet currentPet = currentPetIdx >= 0 ? petMap.get(currentPetIdx) : null;

        for (Map.Entry<Integer, CachedPet> entry : petMap.entrySet()) {
            CachedPet pet = entry.getValue();
            if (!stripPetName(pet.displayName).equals(petName)
                    || pet.info.petRarity != rarity) {
                continue;
            }

            Matcher matcher = PET_LEVEL_PATTERN.matcher(pet.displayName);
            if (!matcher.matches()) {
                continue;
            }

            boolean isCurrent = currentPet != null
                    && currentPet.info.uniqueId != null
                    && currentPet.info.uniqueId.equals(pet.info.uniqueId);
            String cosmeticLevelGroup = matcher.group("cosmeticLevel");

            if (pet.petLevel >= newLevel) {
                continue;
            }

            if (cosmeticLevelGroup != null) {
                int cosmeticLevel = newLevel - pet.petLevel;
                pet.displayName = matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4) + cosmeticLevel + matcher.group(6);
            } else {
                pet.petLevel = newLevel;
                pet.displayName = matcher.group(1) + newLevel + matcher.group(3) + matcher.group(6);
            }

            ItemStack stack = pet.decodeItem(Minecraft.getInstance());
            if (stack != null) {
                pet.encodedItem = encodeItem(Minecraft.getInstance(), stack);
            }

            petMap.put(entry.getKey(), pet);
            cacheDirty = true;

            if (isCurrent) {
                setCurrentPetIndex(entry.getKey());
            }
        }
    }

    private static CachedPet petFromStack(ItemStack itemCopy) {
        Component name = itemCopy.getCustomName();
        if (name == null) {
            name = itemCopy.getHoverName();
        }
        if (name == null) {
            return null;
        }
        String displayName = FAVORITE_PATTERN.matcher(ComponentTextUtils.getFormattedText(name, true)).replaceAll("");
        int petLevel = petLevelFromDisplayName(displayName);
        if (petLevel < 0) {
            return null;
        }
        PetInfo info = petInfoFromStack(itemCopy);
        return info == null ? null : new CachedPet(displayName, petLevel, info, encodeItem(Minecraft.getInstance(), itemCopy));
    }

    private static PetInfo petInfoFromStack(ItemStack stack) {
        CompoundTag extra = extraAttributes(stack);
        if (extra == null) {
            return null;
        }
        String json = extra.getString("petInfo").orElse("");
        if (json.isEmpty()) {
            return null;
        }
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            PetInfo info = new PetInfo();
            info.active = object.has("active") && object.get("active").getAsBoolean();
            info.petRarity = parsePetRarity(object);
            if (object.has("uniqueId") && !object.get("uniqueId").isJsonNull()) {
                try {
                    info.uniqueId = UUID.fromString(object.get("uniqueId").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (object.has("uuid") && !object.get("uuid").isJsonNull()) {
                try {
                    info.uuid = UUID.fromString(object.get("uuid").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (object.has("heldItem") && !object.get("heldItem").isJsonNull()) {
                info.heldItemId = object.get("heldItem").getAsString();
            }
            if (object.has("type")) {
                info.petSkyblockId = object.get("type").getAsString();
            }
            if (object.has("skin") && !object.get("skin").isJsonNull()) {
                info.skin = object.get("skin").getAsString();
            }
            return info;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static SkyblockItemRarity parsePetRarity(JsonObject object) {
        if (!object.has("tier") || object.get("tier").isJsonNull()) {
            return SkyblockItemRarity.COMMON;
        }
        return petRarityFromTierName(object.get("tier").getAsString());
    }

    private static SkyblockItemRarity petRarityFromTierName(String tier) {
        if (tier == null || tier.isEmpty()) {
            return SkyblockItemRarity.COMMON;
        }
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "SUPREME" -> SkyblockItemRarity.DIVINE;
            case "UNOBTAINABLE" -> SkyblockItemRarity.ADMIN;
            default -> SkyblockItemRarity.fromTierName(tier);
        };
    }

    private static ItemStack stripTimestamp(ItemStack stack) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data
                .update(tag -> tag.remove("timestamp"))
        );
        return stack;
    }

    private static String encodeItem(Minecraft client, ItemStack stack) {
        if (client.level == null || stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            Tag tag = ItemStack.CODEC.encodeStart(
                    client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    stack
            ).getOrThrow();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeUnnamedTagWithFallback(tag, new DataOutputStream(output));
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException | RuntimeException exception) {
            return "";
        }
    }

    private static ItemStack decodeItem(Minecraft client, String encoded) {
        if (client.level == null || encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            Tag tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
            return ItemStack.CODEC.parse(
                    client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    tag
            ).getOrThrow();
        } catch (RuntimeException | IOException exception) {
            return null;
        }
    }

    private static CompoundTag extraAttributes(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static int petLevelFromDisplayName(String displayName) {
        int start = displayName.indexOf("[Lvl ");
        if (start < 0) {
            return -1;
        }
        start += 5;
        int end = displayName.indexOf(']', start);
        if (end <= start || end - start > 9) {
            return -1;
        }
        int level = 0;
        for (int i = start; i < end; i++) {
            char c = displayName.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            level = level * 10 + (c - '0');
        }
        return level;
    }

    private static String stripPetName(String displayName) {
        Matcher matcher = STRIP_PET_NAME.matcher(displayName);
        return matcher.find() ? matcher.group(1) : displayName;
    }

    private static SkyblockItemRarity rarityFromColor(char colorCode) {
        return switch (colorCode) {
            case 'f' -> SkyblockItemRarity.COMMON;
            case 'a' -> SkyblockItemRarity.UNCOMMON;
            case '9' -> SkyblockItemRarity.RARE;
            case '5' -> SkyblockItemRarity.EPIC;
            case '6' -> SkyblockItemRarity.LEGENDARY;
            case 'd' -> SkyblockItemRarity.MYTHIC;
            case 'b' -> SkyblockItemRarity.DIVINE;
            case 'c' -> SkyblockItemRarity.SPECIAL;
            case '4' -> SkyblockItemRarity.ULTIMATE;
            default -> SkyblockItemRarity.UNKNOWN;
        };
    }

    private static int pageNum(Component title) {
        Matcher matcher = PETS_TITLE.matcher(SidePanelUtils.stripFormatting(title.getString()));
        if (!matcher.matches() || matcher.group(1) == null) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int pageOffset(Component title) {
        int page = pageNum(title);
        return 45 * (page == 0 ? 0 : page - 1);
    }

    private static final class PetInfo {
        private String petSkyblockId;
        private boolean active;
        private SkyblockItemRarity petRarity = SkyblockItemRarity.COMMON;
        private String heldItemId;
        private String skin;
        private UUID uuid;
        private UUID uniqueId;
    }

    private static final class CachedPet {
        private String displayName;
        private int petLevel;
        private final PetInfo info;
        private String encodedItem;

        private CachedPet(String displayName, int petLevel, PetInfo info, String encodedItem) {
            this.displayName = displayName;
            this.petLevel = petLevel;
            this.info = info;
            this.encodedItem = encodedItem;
        }

        private boolean matches(CachedPet other) {
            return petLevel == other.petLevel
                    && displayName.equals(other.displayName)
                    && info.uniqueId != null
                    && info.uniqueId.equals(other.info.uniqueId);
        }

        private ItemStack decodeItem(Minecraft client) {
            return SidePanelPets.decodeItem(client, encodedItem);
        }

        private CompoundTag encode() {
            CompoundTag tag = new CompoundTag();
            tag.putString("displayName", displayName);
            tag.putInt("petLevel", petLevel);
            CompoundTag infoTag = new CompoundTag();
            infoTag.putBoolean("active", info.active);
            infoTag.putString("tier", info.petRarity.name());
            if (info.petSkyblockId != null) {
                infoTag.putString("type", info.petSkyblockId);
            }
            if (info.heldItemId != null) {
                infoTag.putString("heldItem", info.heldItemId);
            }
            if (info.skin != null) {
                infoTag.putString("skin", info.skin);
            }
            if (info.uuid != null) {
                infoTag.putString("uuid", info.uuid.toString());
            }
            if (info.uniqueId != null) {
                infoTag.putString("uniqueId", info.uniqueId.toString());
            }
            tag.put("info", infoTag);
            if (encodedItem != null && !encodedItem.isEmpty()) {
                tag.putString("item", encodedItem);
            }
            return tag;
        }

        private static CachedPet decode(CompoundTag tag) {
            PetInfo info = new PetInfo();
            CompoundTag infoTag = tag.getCompound("info").orElse(new CompoundTag());
            info.active = infoTag.getBooleanOr("active", false);
            info.petRarity = petRarityFromTierName(infoTag.getStringOr("tier", "COMMON"));
            if (infoTag.contains("type")) {
                info.petSkyblockId = infoTag.getString("type").orElse(null);
            }
            if (infoTag.contains("heldItem")) {
                info.heldItemId = infoTag.getString("heldItem").orElse(null);
            }
            if (infoTag.contains("skin")) {
                info.skin = infoTag.getString("skin").orElse(null);
            }
            if (infoTag.contains("uuid")) {
                info.uuid = UUID.fromString(infoTag.getString("uuid").orElse(""));
            }
            if (infoTag.contains("uniqueId")) {
                info.uniqueId = UUID.fromString(infoTag.getString("uniqueId").orElse(""));
            }
            return new CachedPet(
                    tag.getString("displayName").orElse(""),
                    tag.getIntOr("petLevel", 1),
                    info,
                    tag.getString("item").orElse("")
            );
        }
    }
}
