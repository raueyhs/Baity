package com.shyeuar.baity.features.sidepanel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.references.ItemIds;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class SidePanelEquipment {
    private static int previousPage = -1;
    private static boolean rescan = true;

    private SidePanelEquipment() {
    }

    public static void tick(Minecraft client) {
        if (!SidePanel.isSyncActive(client)) {
            reset();
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            reset();
            return;
        }
        if (!SidePanelMenus.isEquipmentSetsMenu(screen.getTitle())) {
            reset();
            return;
        }

        int page = SidePanelMenus.equipmentPageFromTitle(screen.getTitle());
        if (page < 1) {
            return;
        }
        if (page != previousPage) {
            rescan = true;
        }
        if (!rescan) {
            return;
        }

        AbstractContainerMenu menu = screen.getMenu();
        if (menu.slots.size() < 45) {
            return;
        }

        registerPageRows(menu, page);
        SidePanel.syncEquippedEquipmentFromMenu(menu, page);
        previousPage = page;
        rescan = false;
    }

    public static void requestRescan() {
        rescan = true;
    }

    public static void registerPageRows(AbstractContainerMenu menu, int page) {
        if (menu == null || page < 1 || menu.slots.size() < 45) {
            return;
        }
        for (int hotbar = 0; hotbar <= 8; hotbar++) {
            ItemStack necklace = normalizeEquipment(menu, hotbar);
            ItemStack cloak = normalizeEquipment(menu, hotbar + 9);
            ItemStack belt = normalizeEquipment(menu, hotbar + 18);
            ItemStack gloves = normalizeEquipment(menu, hotbar + 27);
            int setIndex = globalSetIndex(page, hotbar);
            SidePanelCache.registerSetRow(setIndex, necklace, cloak, belt, gloves);
            for (ItemStack stack : new ItemStack[] {necklace, cloak, belt, gloves}) {
                if (!stack.isEmpty()) {
                    SidePanelCache.registerLoadoutAliases(stack);
                }
            }
        }
    }

    public static void onMenuSlotClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen == null || slot == null || !SidePanelMenus.isEquipmentSetsMenu(screen.getTitle())) {
            return;
        }
        if (!slot.getItem().typeHolder().is(ItemIds.DYE.lime())) {
            return;
        }
        SidePanel.clearEquippedSlots();
    }

    private static void reset() {
        previousPage = -1;
        rescan = true;
    }

    public static int globalSetIndex(int page, int hotbarIndex) {
        if (page < 1 || hotbarIndex < 0 || hotbarIndex > 8) {
            return -1;
        }
        return (page - 1) * 9 + hotbarIndex;
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
