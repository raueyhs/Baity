package com.shyeuar.baity.features.inventorytweaks;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class MiddleClickGui {

    // 白名单界面整屏放行：这些界面的槽位左键会播拿起物品动画，高延迟下拖慢交互。
    // TODO(future): Replace this hardcoded whitelist with a user-configurable one.
    private static final List<String> ALLOW_GUIS = List.of(
            "Auctions Browser",
            "Stats & Equipment",
            "Accessory Bag Thaumaturgy",
            "Pets",
            "Community Shop",
            "Bits Shop"
    );

    private static final List<Pattern> ALLOW_GUI_PATTERNS = buildAllowGuiPatterns();

    private static List<Pattern> buildAllowGuiPatterns() {
        List<Pattern> patterns = new java.util.ArrayList<>(ALLOW_GUIS.size());
        for (String gui : ALLOW_GUIS) {
            patterns.add(Pattern.compile("\\(\\d+/\\d+\\) " + Pattern.quote(gui) + ".*"));
        }
        return List.copyOf(patterns);
    }

    private static final List<String> AVOID_GUIS = List.of(
            "Drill Anvil",
            "Anvil",
            "Storage",
            "The Hex",
            "Composter",
            "Auctions",
            "Abiphone",
            "Chest",
            "Large Chest",
            "Stats Tuning",
            "Exp Sharing",
            "Convert to Dungeon Item",
            "Upgrade Item",
            "Enchant Item",
            "Craft Item",
            "Brewing Stand",
            "Beacon",
            "Pet Sitter",
            "Offer Pets",
            "Pet Training"
    );

    private static final List<Pattern> AVOID_GUI_PATTERNS = List.of(
            Pattern.compile("^Loadout (\\d+)$"),
            Pattern.compile("^\\((\\d+)/(\\d+)\\) Abiphone([+\\w ]+): (\\w+)$"),
            Pattern.compile("^\\((\\d+)/(\\d+)\\) Armor Sets$"),
            Pattern.compile("^\\((\\d+)/(\\d+)\\) Equipment Sets$")
    );

    private static final List<Pattern> TERMINAL_GUIS = List.of(
            Pattern.compile("^Click in order!$"),
            Pattern.compile("^Select all the (.+?) items!$"),
            Pattern.compile("^What starts with: '(.+?)'\\?$"),
            Pattern.compile("^Change all to same color!$"),
            Pattern.compile("^Correct all the panes!$"),
            Pattern.compile("^Click the button on time!$")
    );

    private static final Set<String> AVOID_ITEMS = Set.of(
            "Reforge Item",
            "Salvage Items",
            "Experience Bottles"
    );

    private static final Set<String> TRADE_LORE_LINES = Set.of(
            "Cost",
            "Sell Price",
            "Bazaar Price"
    );

    private MiddleClickGui() {
    }

    public static boolean isActive() {
        if (!ConfigManager.inventoryTweaksEnabled || !ConfigManager.middleClickGuiEnabled) {
            return false;
        }
        if (isHoldToCancelHeld()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        return client != null && client.player != null && LocateUtils.inSkyBlock(client);
    }

    public static boolean convertLeftClickToMiddleClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (!isActive() || slot == null || !(screen instanceof ContainerScreen)) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.gameMode == null) {
            return false;
        }
        if (slot.container == client.player.getInventory()) {
            return false;
        }

        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return false;
        }

        Component title = screen.getTitle();
        String plainTitle = title != null ? title.getString() : null;
        if (!isAllowedGui(plainTitle) && !passesFilters(plainTitle, stack)) {
            return false;
        }

        client.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE, ContainerInput.CLONE, client.player);
        return true;
    }

    private static boolean passesFilters(String plainTitle, ItemStack stack) {
        if (isAvoidedGui(plainTitle)) {
            return false;
        }
        String itemName = customItemName(stack);
        if (itemName != null && AVOID_ITEMS.contains(itemName)) {
            return false;
        }
        return !hasSkyblockId(stack) || isTradeSlot(stack);
    }

    private static boolean isHoldToCancelHeld() {
        int keyCode = ConfigManager.middleClickGuiHoldToCancel;
        if (keyCode == 0) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        return KeyMappingUtils.isKeyPressed(client.getWindow().handle(), keyCode);
    }

    private static boolean isAllowedGui(String plainTitle) {
        if (plainTitle == null) {
            return false;
        }
        for (int i = 0; i < ALLOW_GUIS.size(); i++) {
            if (plainTitle.startsWith(ALLOW_GUIS.get(i))
                    || ALLOW_GUI_PATTERNS.get(i).matcher(plainTitle).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAvoidedGui(String plainTitle) {
        if (plainTitle == null || plainTitle.contains(" Minion ")) {
            return true;
        }
        for (String gui : AVOID_GUIS) {
            if (plainTitle.startsWith(gui)) {
                return true;
            }
        }
        for (Pattern pattern : AVOID_GUI_PATTERNS) {
            if (pattern.matcher(plainTitle).matches()) {
                return true;
            }
        }
        for (Pattern pattern : TERMINAL_GUIS) {
            if (pattern.matcher(plainTitle).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTradeSlot(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        for (Component line : lore.lines()) {
            if (TRADE_LORE_LINES.contains(LocateUtils.toPlainText(line.getString()))) {
                return true;
            }
        }
        return false;
    }

    private static String customItemName(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null ? name.getString() : null;
    }

    private static boolean hasSkyblockId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag root = customData.copyTag();
        return root != null && root.contains("id");
    }
}
