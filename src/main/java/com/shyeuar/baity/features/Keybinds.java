package com.shyeuar.baity.features;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.sidepanel.SidePanel;
import com.shyeuar.baity.features.sidepanel.SidePanelEquipment;
import com.shyeuar.baity.features.sidepanel.SidePanelMenus;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.references.ItemIds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class Keybinds {
    private static final int CLICK_COOLDOWN_MS = 300;
    private static final long AUTO_CLOSE_TIMEOUT_MS = 3000L;

    private static long lastClickAt;
    private static MenuType pendingAutoClose = MenuType.NONE;
    private static long pendingAutoCloseExpiresAt;

    private Keybinds() {
    }

    public enum MenuType {
        WARDROBE(SidePanelMenus.WARDROBE_TITLE),
        EQUIPMENT(SidePanelMenus.EQUIPMENT_TITLE),
        LOADOUT(SidePanelMenus.LOADOUTS_TITLE),
        NONE(null);

        private final Pattern titlePattern;

        MenuType(Pattern titlePattern) {
            this.titlePattern = titlePattern;
        }

        public static MenuType fromTitle(Component title) {
            if (title == null) {
                return NONE;
            }
            String plain = LocateUtils.toPlainText(title.getString());
            for (MenuType type : values()) {
                if (type.titlePattern != null && type.titlePattern.matcher(plain).matches()) {
                    return type;
                }
            }
            return NONE;
        }
    }

    private enum NavButton {
        NEXT,
        PREVIOUS
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, _, _) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || client.gameMode == null) {
                return;
            }

            onAutoCloseScreenOpened(containerScreen);

            ScreenKeyboardEvents.allowKeyPress(containerScreen).register((_, keyInput) ->
                    handleKeyboard(client, containerScreen, keyInput)
            );
            ScreenMouseEvents.allowMouseClick(containerScreen).register((_, click) ->
                    handleMouseClick(client, containerScreen, click)
            );
        });
    }

    public static boolean isActive() {
        var module = ModuleManager.getModuleByName("Keybinds");
        return module != null && module.isEnabled() && ConfigManager.keybindsEnabled;
    }

    public static boolean isUnequipBlockedAction(ContainerInput actionType) {
        return actionType == ContainerInput.PICKUP
                || actionType == ContainerInput.QUICK_MOVE
                || actionType == ContainerInput.SWAP
                || actionType == ContainerInput.THROW
                || actionType == ContainerInput.PICKUP_ALL
                || actionType == ContainerInput.CLONE
                || actionType == ContainerInput.QUICK_CRAFT;
    }

    public static boolean shouldBlockUnequipContainerInput(int containerId, int slotIndex) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == null || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        if (screen.getMenu().containerId != containerId) {
            return false;
        }
        MenuType menuType = MenuType.fromTitle(screen.getTitle());
        if (menuType != MenuType.WARDROBE && menuType != MenuType.EQUIPMENT) {
            return false;
        }
        if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return false;
        }
        ItemStack stack = screen.getMenu().getSlot(slotIndex).getItem();
        if (!isEquippedSetButton(stack)) {
            return false;
        }
        return shouldPreventUnequip(menuType, stack);
    }

    public static boolean shouldPreventUnequip(MenuType menuType, ItemStack stack) {
        if (!isActive() || stack == null || !isEquippedSetButton(stack)) {
            return false;
        }
        boolean prevent;
        int holdKey;
        if (menuType == MenuType.WARDROBE) {
            prevent = ConfigManager.keybindsWardrobePreventUnequip;
            holdKey = ConfigManager.keybindsWardrobeHoldToUnequip;
        } else if (menuType == MenuType.EQUIPMENT) {
            prevent = ConfigManager.keybindsEquipmentPreventUnequip;
            holdKey = ConfigManager.keybindsEquipmentHoldToUnequip;
        } else {
            return false;
        }
        if (!prevent) {
            return false;
        }
        return !isHoldToUnequipModifierHeld(holdKey);
    }

    public static boolean isEquippedSetButton(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.typeHolder().is(ItemIds.DYE.lime());
    }

    private static boolean handleKeyboard(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent keyInput) {
        if (!handlePaginationKeyPress(screen, keyInput)) {
            return false;
        }
        if (!isActive()) {
            return true;
        }

        MenuType menuType = MenuType.fromTitle(screen.getTitle());

        return switch (menuType) {
            case WARDROBE -> handleSetMenu(client, screen, binding -> binding.matches(keyInput), menuType);
            case EQUIPMENT -> handleSetMenu(client, screen, binding -> binding.matches(keyInput), menuType);
            case LOADOUT -> {
                if (!tryConsumeClickCooldown()) {
                    yield false;
                }
                yield handleLoadoutMenu(client, screen, keyInput);
            }
            case NONE -> true;
        };
    }

    private static boolean handleMouseClick(Minecraft client, AbstractContainerScreen<?> screen, MouseButtonEvent click) {
        if (!isActive()) {
            return true;
        }

        MenuType menuType = MenuType.fromTitle(screen.getTitle());

        if (menuType == MenuType.WARDROBE && ConfigManager.keybindsWardrobeEnabled) {
            return handleSetMenu(client, screen, binding -> binding.matchesMouse(click), menuType);
        }
        if (menuType == MenuType.EQUIPMENT && ConfigManager.keybindsEquipmentEnabled) {
            return handleSetMenu(client, screen, binding -> binding.matchesMouse(click), menuType);
        }
        if (menuType == MenuType.LOADOUT && ConfigManager.keybindsLoadoutEnabled) {
            if (!tryConsumeClickCooldown()) {
                return false;
            }
            return handleLoadoutMenu(client, screen, binding -> binding.matchesMouse(click));
        }
        return true;
    }

    private static boolean handlePaginationKeyPress(AbstractContainerScreen<?> screen, KeyEvent keyInput) {
        if (!isActive()) {
            return true;
        }
        MenuType menuType = MenuType.fromTitle(screen.getTitle());
        if (menuType == MenuType.NONE) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        NavButton nav = resolveNavButton(client, keyInput);
        if (nav == null) {
            return true;
        }
        if (!tryConsumeClickCooldown()) {
            return false;
        }

        Slot target = findNavSlot(screen, nav);
        if (target == null) {
            return true;
        }

        clickNavigationSlot(screen.getMenu().containerId, target.index, target.getItem());
        return false;
    }

    private static boolean handleSetMenu(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            Predicate<KeyMapping> matcher,
            MenuType menuType
    ) {
        if (!isSetMenuEnabled(menuType)) {
            return true;
        }

        int hotbarIndex = resolveHotbarIndex(client, matcher);
        if (hotbarIndex < 0) {
            return true;
        }

        int slotIndex = wardrobeHotbarIndexToSlot(hotbarIndex);
        Slot slot = screen.getMenu().getSlot(slotIndex);
        ItemStack stack = slot.getItem();
        if (!isWardrobeButton(stack)) {
            return true;
        }

        if (shouldPreventUnequip(menuType, stack)) {
            return false;
        }

        if (!tryConsumeClickCooldown()) {
            return false;
        }

        boolean unequipping = isEquippedSetButton(stack);
        boolean willAutoClose = isAutoCloseEnabled(menuType);
        if (willAutoClose && menuType == MenuType.EQUIPMENT) {
            int page = SidePanelMenus.equipmentPageFromTitle(screen.getTitle());
            int setIndex = SidePanelEquipment.globalSetIndex(page, hotbarIndex);
            SidePanel.prepareEquipmentAutoClose(screen.getMenu(), setIndex, hotbarIndex, unequipping);
        }
        leftClickSlot(screen.getMenu().containerId, slotIndex);
        if (willAutoClose) {
            scheduleAutoClose(menuType);
        }
        return false;
    }

    private static boolean handleLoadoutMenu(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent keyInput) {
        if (!ConfigManager.keybindsLoadoutEnabled) {
            return true;
        }
        int loadoutIndex = resolveLoadoutIndexFromKey(client, keyInput);
        return executeLoadoutIndex(client, screen, loadoutIndex);
    }

    private static boolean handleLoadoutMenu(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            Predicate<KeyMapping> matcher
    ) {
        int loadoutIndex = resolveHotbarIndex(client, matcher);
        if (loadoutIndex < 0) {
            loadoutIndex = resolveExtraLoadoutIndexFromMouse(matcher);
        }
        return executeLoadoutIndex(client, screen, loadoutIndex);
    }

    private static boolean executeLoadoutIndex(Minecraft client, AbstractContainerScreen<?> screen, int loadoutIndex) {
        if (loadoutIndex < 0) {
            return true;
        }

        int slotIndex = loadoutIndexToSlot(loadoutIndex);
        ItemStack stack = screen.getMenu().getSlot(slotIndex).getItem();
        if (!SidePanelMenus.isLoadoutEquipButton(stack)) {
            return true;
        }

        boolean willAutoClose = ConfigManager.keybindsLoadoutAutoCloseOnUse;
        if (willAutoClose) {
            SidePanel.prepareLoadoutAutoClose(screen.getMenu(), screen.getTitle(), loadoutIndex);
        }
        leftClickSlot(screen.getMenu().containerId, slotIndex);
        if (willAutoClose) {
            scheduleAutoClose(MenuType.LOADOUT);
        }
        playLoadoutEquipSound(client);
        return false;
    }

    private static NavButton resolveNavButton(Minecraft client, KeyEvent keyInput) {
        if (client.options.keyLeft.matches(keyInput)) {
            return NavButton.PREVIOUS;
        }
        if (client.options.keyRight.matches(keyInput)) {
            return NavButton.NEXT;
        }
        return null;
    }

    private static Slot findNavSlot(AbstractContainerScreen<?> screen, NavButton nav) {
        for (Slot slot : screen.getMenu().slots) {
            NavButton type = getNavButtonType(slot.getItem());
            if (type == nav) {
                return slot;
            }
        }
        return null;
    }

    private static NavButton getNavButtonType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (!item.equals(Items.ARROW) && !item.equals(Items.PLAYER_HEAD)) {
            return null;
        }
        String name = stack.getHoverName().getString();
        if (name.contains("Next Page") || name.contains("Scroll Right")) {
            return NavButton.NEXT;
        }
        if (name.contains("Previous Page") || name.contains("Scroll Left")) {
            return NavButton.PREVIOUS;
        }
        return null;
    }

    private static int resolveHotbarIndex(Minecraft client, Predicate<KeyMapping> matcher) {
        KeyMapping[] slots = client.options.keyHotbarSlots;
        for (int i = 0; i < slots.length; i++) {
            if (matcher.test(slots[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int resolveLoadoutIndexFromKey(Minecraft client, KeyEvent keyInput) {
        int hotbarIndex = resolveHotbarIndex(client, binding -> binding.matches(keyInput));
        if (hotbarIndex >= 0) {
            return hotbarIndex;
        }
        int key = keyInput.input();
        int[] extraKeys = {
                ConfigManager.keybindsLoadoutSlot10Key,
                ConfigManager.keybindsLoadoutSlot11Key,
                ConfigManager.keybindsLoadoutSlot12Key
        };
        for (int i = 0; i < extraKeys.length; i++) {
            if (extraKeys[i] > 0 && extraKeys[i] == key) {
                return 9 + i;
            }
        }
        return -1;
    }

    private static int resolveExtraLoadoutIndexFromMouse(Predicate<KeyMapping> matcher) {
        int[] extraKeys = {
                ConfigManager.keybindsLoadoutSlot10Key,
                ConfigManager.keybindsLoadoutSlot11Key,
                ConfigManager.keybindsLoadoutSlot12Key
        };
        for (int i = 0; i < extraKeys.length; i++) {
            int keyCode = extraKeys[i];
            if (keyCode <= 0) {
                continue;
            }
            if (matcher.test(new KeyMapping("baity.loadout.extra", keyCode, KeyMapping.Category.MISC))) {
                return 9 + i;
            }
        }
        return -1;
    }

    private static boolean isAutoCloseEnabled(MenuType menuType) {
        return switch (menuType) {
            case WARDROBE -> ConfigManager.keybindsWardrobeAutoCloseOnUse;
            case EQUIPMENT -> ConfigManager.keybindsEquipmentAutoCloseOnUse;
            default -> false;
        };
    }

    private static boolean isSetMenuEnabled(MenuType menuType) {
        return switch (menuType) {
            case WARDROBE -> ConfigManager.keybindsWardrobeEnabled;
            case EQUIPMENT -> ConfigManager.keybindsEquipmentEnabled;
            default -> false;
        };
    }

    private static void playLoadoutEquipSound(Minecraft client) {
        if (client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 0.0f);
        }
    }

    private static void scheduleAutoClose(MenuType menuType) {
        pendingAutoClose = menuType;
        pendingAutoCloseExpiresAt = System.currentTimeMillis() + AUTO_CLOSE_TIMEOUT_MS;
        SidePanel.onAutoCloseScheduled(menuType);
        closeNow();
    }

    private static void onAutoCloseScreenOpened(AbstractContainerScreen<?> screen) {
        if (pendingAutoClose == MenuType.NONE) {
            return;
        }
        if (System.currentTimeMillis() > pendingAutoCloseExpiresAt) {
            clearAutoClose();
            return;
        }
        MenuType opened = MenuType.fromTitle(screen.getTitle());
        if (opened != pendingAutoClose) {
            return;
        }
        clearAutoClose();
        closeNow();
    }

    private static void clearAutoClose() {
        pendingAutoClose = MenuType.NONE;
        pendingAutoCloseExpiresAt = 0L;
        SidePanel.clearAutoCloseSuppress();
    }

    private static void closeNow() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.closeContainer();
        }
    }

    private static boolean isHoldToUnequipModifierHeld(int keyCode) {
        if (keyCode == 0) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        return KeyMappingUtils.isKeyPressed(client.getWindow().handle(), keyCode);
    }

    private static boolean tryConsumeClickCooldown() {
        long now = System.currentTimeMillis();
        if (now - lastClickAt < CLICK_COOLDOWN_MS) {
            return false;
        }
        lastClickAt = now;
        return true;
    }

    private static void clickSlot(int containerId, int slotIndex, int mouseButton, ContainerInput input) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null || client.player == null) {
            return;
        }
        client.gameMode.handleContainerInput(containerId, slotIndex, mouseButton, input, client.player);
    }

    private static void leftClickSlot(int containerId, int slotIndex) {
        clickSlot(containerId, slotIndex, GLFW.GLFW_MOUSE_BUTTON_LEFT, ContainerInput.PICKUP);
    }

    private static void clickNavigationSlot(int containerId, int slotIndex, ItemStack stack) {
        boolean extraLines = getLoreLineCount(stack) > 1;
        if (extraLines) {
            clickSlot(containerId, slotIndex, GLFW.GLFW_MOUSE_BUTTON_LEFT, ContainerInput.PICKUP);
        } else {
            clickSlot(containerId, slotIndex, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, ContainerInput.CLONE);
        }
    }

    private static int getLoreLineCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? 0 : lore.lines().size();
    }

    private static boolean isWardrobeButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.typeHolder().is(ItemIds.DYE.lime()) || stack.typeHolder().is(ItemIds.DYE.gray()) || stack.typeHolder().is(ItemIds.DYE.pink());
    }

    private static int wardrobeHotbarIndexToSlot(int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex > 8) {
            return -1;
        }
        return 36 + hotbarIndex;
    }

    private static int loadoutIndexToSlot(int loadoutIndex) {
        if (loadoutIndex < 0 || loadoutIndex >= SidePanelMenus.LOADOUT_BUTTON_SLOTS.length) {
            return -1;
        }
        return SidePanelMenus.LOADOUT_BUTTON_SLOTS[loadoutIndex];
    }
}
