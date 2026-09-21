package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.shyeuar.baity.features.sidepanel.SidePanel;
import com.shyeuar.baity.features.sidepanel.SidePanelEquipment;
import com.shyeuar.baity.features.sidepanel.SidePanelPets;
import com.shyeuar.baity.features.sidepanel.SidePanelSlots;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class SidePanelMixin {

    @Mixin(ClientPacketListener.class)
    public static class PacketMixin {

        @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
        private void baity$sidePanelAfterContainerSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
            SidePanel.onContainerSlotPacket(packet.getContainerId(), packet.getSlot());
        }

        @Inject(method = "handleContainerContent", at = @At("TAIL"))
        private void baity$sidePanelAfterContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
            SidePanel.onContainerContentPacket(packet.containerId());
        }
    }

    @Mixin(InventoryScreen.class)
    public abstract static class InventoryScreenMixin {

        @Redirect(
            method = "extractBackground",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
                ordinal = 0
            )
        )
        private void baity$drawSidePanelBeforeInventoryBlit(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier atlas,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight
        ) {
            InventoryScreen screen = (InventoryScreen) (Object) this;
            if (atlas.equals(InventoryScreen.INVENTORY_LOCATION) && SidePanel.shouldRenderOn(screen)) {
                SidePanelSlots.renderBackground(graphics, x, y, SidePanel.isPetPanelEnabled());
                int omit = SidePanelSlots.INVENTORY_LEFT_EDGE_OMIT_WIDTH;
                graphics.blit(
                    pipeline,
                    atlas,
                    x + omit,
                    y,
                    u + omit,
                    v,
                    width - omit,
                    height,
                    textureWidth,
                    textureHeight
                );
                return;
            }
            graphics.blit(pipeline, atlas, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }

    @Mixin(AbstractContainerScreen.class)
    public abstract static class ContainerScreenMixin {

        @Shadow
        protected int leftPos;
        @Shadow
        protected int topPos;

        @Inject(method = "extractSlots", at = @At("TAIL"))
        private void baity$renderSidePanelSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            if (screen instanceof InventoryScreen inventoryScreen && SidePanel.shouldRenderOn(inventoryScreen)) {
                boolean petPanel = SidePanel.isPetPanelEnabled();
                SidePanelSlots.renderSlots(screen, graphics, mouseX, mouseY, leftPos, topPos);
                SidePanelSlots.renderTopBorderOverlay(graphics, 0, 0, petPanel);
            }
        }

        @Inject(method = "slotClicked", at = @At("HEAD"))
        private void baity$trackPetsMenuClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            SidePanelPets.trackMenuSlotClick(screen, slotId, button);
            SidePanelEquipment.onMenuSlotClick(screen, slot);
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        private void baity$consumeSidePanelClicks(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            if (!(screen instanceof InventoryScreen inventoryScreen) || !SidePanel.shouldRenderOn(inventoryScreen)) {
                return;
            }
            SidePanel.SlotKind slotKind = SidePanelSlots.hoveredSlotKindAt(leftPos, topPos, click.x(), click.y());
            if (slotKind != null) {
                SidePanel.onSlotClick(slotKind, click.button());
                cir.setReturnValue(true);
                return;
            }
            if (SidePanelSlots.isOverPanel(leftPos, topPos, click.x(), click.y())) {
                cir.setReturnValue(true);
            }
        }
    }
}
