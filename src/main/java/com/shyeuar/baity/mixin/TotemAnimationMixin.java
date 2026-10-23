package com.shyeuar.baity.mixin;

import com.shyeuar.baity.items.CustomTotemItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public class TotemAnimationMixin {
    
    @ModifyVariable(
        method = "displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private ItemStack replaceTotemWithCustom(ItemStack original) {
        if (original != null && original.getItem() == Items.TOTEM_OF_UNDYING && CustomTotemItem.CUSTOM_TOTEM != null) {
            return new ItemStack(CustomTotemItem.CUSTOM_TOTEM);
        }
        return original;
    }
}