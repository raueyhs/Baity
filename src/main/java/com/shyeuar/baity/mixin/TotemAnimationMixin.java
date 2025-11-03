package com.shyeuar.baity.mixin;

import com.shyeuar.baity.item.CustomTotemItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class TotemAnimationMixin {
    
    @ModifyVariable(
        method = "showFloatingItem",
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

