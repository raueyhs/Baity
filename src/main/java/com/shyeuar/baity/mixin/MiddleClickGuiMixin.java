package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.shyeuar.baity.features.inventorytweaks.MiddleClickGui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class MiddleClickGuiMixin {

    @Mixin(AbstractContainerScreen.class)
    public abstract static class ContainerScreenMixin {

        @Inject(
                method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
                at = @At("HEAD"),
                cancellable = true,
                require = 1
        )
        private void baity$middleClickGui(
                Slot slot,
                int slotId,
                int button,
                ContainerInput actionType,
                CallbackInfo ci
        ) {
            if (actionType != ContainerInput.PICKUP || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return;
            }
            if (MiddleClickGui.convertLeftClickToMiddleClick((AbstractContainerScreen<?>) (Object) this, slot)) {
                ci.cancel();
            }
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
