package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.features.inventorytweaks.MiddleClickGui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class MiddleClickGuiMixin {

    @Mixin(AbstractContainerScreen.class)
    public abstract static class ContainerScreenMixin {

        @WrapOperation(
                method = "slotClicked(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/client/input/MouseButtonEvent;Lnet/minecraft/world/inventory/ContainerInput;)V",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V"
                ),
                require = 1
        )
        private void baity$middleClickGui(
                AbstractContainerScreen<?> screen,
                Slot slot,
                int slotId,
                int button,
                ContainerInput actionType,
                Operation<Void> original
        ) {
            if (actionType != ContainerInput.PICKUP || button != 0
                    || !MiddleClickGui.shouldConvertLeftClick(screen, slot)) {
                original.call(screen, slot, slotId, button, actionType);
                return;
            }
            original.call(screen, slot, slotId, 2, ContainerInput.CLONE);
        }

        @ModifyExpressionValue(
                method = "mouseClicked",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"
                ),
                require = 1
        )
        private boolean baity$allowMiddleClickInSurvival(boolean original) {
            return MiddleClickGui.isActive() || original;
        }
    }
}
