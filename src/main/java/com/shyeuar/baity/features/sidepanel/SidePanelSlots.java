package com.shyeuar.baity.features.sidepanel;

import com.shyeuar.baity.mixin.accessor.AbstractContainerScreenInvoker;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class SidePanelSlots {
    public static final int PANEL_X = -23;
    public static final int WING_INTERIOR_WIDTH = -PANEL_X;
    public static final int INVENTORY_LEFT_EDGE_OMIT_WIDTH = 1;
    public static final int PANEL_HEIGHT = 86;
    public static final int PET_PANEL_Y = 83;
    public static final int PET_PANEL_HEIGHT = 25;

    private static final int BORDER = 7;
    private static final int ARMOR_SLOT_U = 8;
    private static final int INTERIOR_U = 4;
    private static final int TEX_SIZE = 256;
    private static final int JUNCTION_INNER_U = BORDER;
    private static final int OPEN_INVENTORY_BOTTOM_EDGE_V = 159;
    private static final int ARMOR_SLOT_TEXTURE_V = 8;
    private static final int EQ_TO_PET_GAP_START = 79;
    private static final int EQ_TO_PET_GAP_END = 83;
    private static final int PET_SLOT_SPRITE_START = 83;
    private static final int PET_SLOT_SPRITE_END = 101;
    private static final Identifier INVENTORY_BACKGROUND = InventoryScreen.INVENTORY_LOCATION;
    private static final RenderPipeline PANEL_PIPELINE = RenderPipelines.GUI_TEXTURED;

    private static final Identifier SLOT_HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Identifier[] EMPTY_SLOT_TEXTURES = {
            Identifier.fromNamespaceAndPath("baity", "textures/gui/sidepanel/empty_necklace.png"),
            Identifier.fromNamespaceAndPath("baity", "textures/gui/sidepanel/empty_cloak.png"),
            Identifier.fromNamespaceAndPath("baity", "textures/gui/sidepanel/empty_belt.png"),
            Identifier.fromNamespaceAndPath("baity", "textures/gui/sidepanel/empty_gloves.png"),
            Identifier.fromNamespaceAndPath("baity", "textures/gui/sidepanel/empty_pet.png"),
    };

    private static final SidePanel.SlotKind[] KINDS = SidePanel.SlotKind.values();
    private static final Container BACKING = new SlotContainer();
    private static final List<DisplaySlot> SLOTS = Arrays.stream(KINDS).map(DisplaySlot::new).toList();

    private static DisplaySlot hoveredSlot;

    private SidePanelSlots() {
    }

    public static void renderBackground(GuiGraphicsExtractor graphics, int leftPos, int topPos, boolean petPanel) {
        int wingX = leftPos + PANEL_X;
        int height = panelPixelHeight(petPanel);
        drawWingRows(graphics, wingX, topPos, BORDER, height, height, petPanel);
        if (petPanel) {
            flattenEqPetGapJunction(graphics, leftPos, topPos);
        }
        flattenBottomLeftJunction(graphics, leftPos, topPos, height);
    }

    public static void renderTopBorderOverlay(GuiGraphicsExtractor graphics, int leftPos, int topPos, boolean petPanel) {
        int wingX = leftPos + PANEL_X;
        int height = panelPixelHeight(petPanel);
        drawWingRows(graphics, wingX, topPos, 0, BORDER, height, petPanel);
        flattenTopLeftJunction(graphics, leftPos, topPos);
    }

    public static int panelBottomY(int topPos, boolean petPanel) {
        return topPos + panelPixelHeight(petPanel);
    }

    public static void renderSlots(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int leftPos,
            int topPos
    ) {
        hoveredSlot = null;
        int slotMouseX = mouseX - leftPos;
        int slotMouseY = mouseY - topPos;

        for (DisplaySlot slot : visibleSlots()) {
            drawSlotBase(graphics, slot);
        }

        for (DisplaySlot slot : visibleSlots()) {
            if (isHovering(slot, slotMouseX, slotMouseY)) {
                hoveredSlot = slot;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK, slot.x - 4, slot.y - 4, 24, 24);
                break;
            }
        }

        for (DisplaySlot slot : visibleSlots()) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                drawEmptySlotIcon(graphics, slot);
            } else {
                ((AbstractContainerScreenInvoker) screen).baity$invokeExtractSlot(graphics, slot, mouseX, mouseY);
            }
        }

        if (hoveredSlot != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT, hoveredSlot.x - 4, hoveredSlot.y - 4, 24, 24);
            ItemStack stack = hoveredSlot.getItem();
            if (!stack.isEmpty()) {
                graphics.setTooltipForNextFrame(MC.font, stack, mouseX, mouseY);
            }
        }
    }

    public static SidePanel.SlotKind hoveredSlotKindAt(int leftPos, int topPos, double mouseX, double mouseY) {
        DisplaySlot slot = hoveredSlot;
        if (slot == null) {
            return null;
        }
        int slotMouseX = (int) mouseX - leftPos;
        int slotMouseY = (int) mouseY - topPos;
        return isHovering(slot, slotMouseX, slotMouseY) ? slot.kind : null;
    }

    public static boolean isOverPanel(int leftPos, int topPos, double mouseX, double mouseY) {
        if (mouseX >= leftPos || mouseX < leftPos + PANEL_X) {
            return false;
        }
        int slotMouseX = (int) mouseX - leftPos;
        int slotMouseY = (int) mouseY - topPos;
        for (DisplaySlot slot : visibleSlots()) {
            if (isHovering(slot, slotMouseX, slotMouseY)) {
                return true;
            }
        }
        return isOverPanelBackground(leftPos, topPos, slotMouseX, slotMouseY);
    }

    private static boolean isOverPanelBackground(int leftPos, int topPos, int slotMouseX, int slotMouseY) {
        return slotMouseX >= PANEL_X
                && slotMouseX < 0
                && slotMouseY >= 0
                && slotMouseY < panelBottomY(topPos, SidePanel.isPetPanelEnabled()) - topPos;
    }

    private static List<DisplaySlot> visibleSlots() {
        if (SidePanel.isPetPanelEnabled()) {
            return SLOTS;
        }
        return SLOTS.subList(0, SLOTS.size() - 1);
    }

    private static boolean isHovering(DisplaySlot slot, int slotMouseX, int slotMouseY) {
        return slotMouseX >= slot.x - 1 && slotMouseX < slot.x + 17
                && slotMouseY >= slot.y - 1 && slotMouseY < slot.y + 17;
    }

    private static void drawSlotBase(GuiGraphicsExtractor graphics, DisplaySlot slot) {
        boolean petPanel = SidePanel.isPetPanelEnabled();
        blitInventory(
                graphics,
                slot.x - 1,
                slot.y - 1,
                18,
                18,
                slot.x + 23,
                mapSrcV(slot.y - 1, panelPixelHeight(petPanel), petPanel)
        );
    }

    private static void drawEmptySlotIcon(GuiGraphicsExtractor graphics, DisplaySlot slot) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                EMPTY_SLOT_TEXTURES[slot.kind.ordinal()],
                slot.x,
                slot.y,
                0.0f,
                0.0f,
                16,
                16,
                16,
                16
        );
    }

    private static int slotX() {
        return -15;
    }

    private static int slotY(SidePanel.SlotKind kind) {
        int base = 8 + kind.ordinal() * 18;
        return kind == SidePanel.SlotKind.PET ? base + 4 : base;
    }

    private static void drawWingRows(
            GuiGraphicsExtractor graphics,
            int wingX,
            int topY,
            int startRow,
            int endRow,
            int height,
            boolean petPanel
    ) {
        for (int row = startRow; row < endRow; row++) {
            int srcV = mapSrcV(row, height, petPanel);
            blitInventory(graphics, wingX, topY + row, BORDER, 1, 0, srcV);
            blitInventory(
                    graphics,
                    wingX + BORDER,
                    topY + row,
                    WING_INTERIOR_WIDTH - BORDER,
                    1,
                    wingInteriorU(row, height, petPanel),
                    srcV
            );
            blitInventory(
                    graphics,
                    wingX + WING_INTERIOR_WIDTH,
                    topY + row,
                    INVENTORY_LEFT_EDGE_OMIT_WIDTH,
                    1,
                    0,
                    srcV
            );
        }
    }

    private static int mapSrcV(int row, int height, boolean petPanel) {
        int bottomCapStart = height - BORDER;

        if (row >= bottomCapStart) {
            return OPEN_INVENTORY_BOTTOM_EDGE_V + (row - bottomCapStart);
        }
        if (petPanel && row >= EQ_TO_PET_GAP_START && row < EQ_TO_PET_GAP_END) {
            return OPEN_INVENTORY_BOTTOM_EDGE_V;
        }
        if (petPanel && row >= PET_SLOT_SPRITE_START && row < PET_SLOT_SPRITE_END) {
            return ARMOR_SLOT_TEXTURE_V + (row - PET_SLOT_SPRITE_START);
        }
        return row;
    }

    private static int wingInteriorU(int row, int height, boolean petPanel) {
        if (row < ARMOR_SLOT_TEXTURE_V) {
            return INTERIOR_U;
        }
        if (row >= height - BORDER) {
            return ARMOR_SLOT_U;
        }
        if (petPanel && row >= EQ_TO_PET_GAP_START && row < PET_SLOT_SPRITE_END) {
            return ARMOR_SLOT_U;
        }
        if (row >= ARMOR_SLOT_TEXTURE_V && row < EQ_TO_PET_GAP_START) {
            return ARMOR_SLOT_U;
        }
        return INTERIOR_U;
    }

    private static void flattenTopLeftJunction(GuiGraphicsExtractor graphics, int leftPos, int topPos) {
        blitInventory(graphics, leftPos, topPos, BORDER, BORDER, JUNCTION_INNER_U, 0);
    }

    private static void flattenEqPetGapJunction(GuiGraphicsExtractor graphics, int leftPos, int topPos) {
        for (int row = EQ_TO_PET_GAP_START; row < EQ_TO_PET_GAP_END; row++) {
            blitInventory(graphics, leftPos, topPos + row, BORDER, 1, JUNCTION_INNER_U, OPEN_INVENTORY_BOTTOM_EDGE_V);
        }
    }

    private static void flattenBottomLeftJunction(GuiGraphicsExtractor graphics, int leftPos, int topPos, int height) {
        blitInventory(
                graphics,
                leftPos,
                topPos + height - BORDER,
                BORDER,
                BORDER,
                JUNCTION_INNER_U,
                OPEN_INVENTORY_BOTTOM_EDGE_V
        );
    }

    private static void blitInventory(
            GuiGraphicsExtractor graphics,
            int destX,
            int destY,
            int destW,
            int destH,
            int srcU,
            int srcV
    ) {
        graphics.blit(
                PANEL_PIPELINE,
                INVENTORY_BACKGROUND,
                destX,
                destY,
                (float) srcU,
                (float) srcV,
                destW,
                destH,
                TEX_SIZE,
                TEX_SIZE
        );
    }

    private static int panelPixelHeight(boolean petPanel) {
        return petPanel ? PET_PANEL_Y + PET_PANEL_HEIGHT : PANEL_HEIGHT;
    }

    private static final class DisplaySlot extends Slot {
        private final SidePanel.SlotKind kind;

        private DisplaySlot(SidePanel.SlotKind kind) {
            super(BACKING, kind.ordinal(), slotX(), slotY(kind));
            this.kind = kind;
            this.index = -1;
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return false;
        }

        @Override
        public @NonNull ItemStack getItem() {
            return SidePanel.get(kind);
        }
    }

    private static final class SlotContainer implements Container {
        @Override
        public int getContainerSize() {
            return KINDS.length;
        }

        @Override
        public boolean isEmpty() {
            for (SidePanel.SlotKind kind : KINDS) {
                if (!SidePanel.get(kind).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NonNull ItemStack getItem(int slot) {
            return slot >= 0 && slot < KINDS.length ? SidePanel.get(KINDS[slot]) : ItemStack.EMPTY;
        }

        @Override
        public @NonNull ItemStack removeItem(int slot, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NonNull ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, @NonNull ItemStack stack) {
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(@NonNull Player player) {
            return false;
        }

        @Override
        public void clearContent() {
        }
    }
}
