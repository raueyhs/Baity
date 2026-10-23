package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FirstPersonHandsAndItems.class)
public interface FirstPersonHandsAndItemsAccessor {

    @Accessor("mainHandHeight")
    float baity$getMainHandHeight();

    @Accessor("mainHandHeight")
    void baity$setMainHandHeight(float height);

    @Accessor("offHandHeight")
    float baity$getOffHandHeight();

    @Accessor("offHandHeight")
    void baity$setOffHandHeight(float height);

    @Accessor("mainHandItem")
    ItemStack baity$getMainHandItem();

    @Accessor("mainHandItem")
    void baity$setMainHandItem(ItemStack item);

    @Accessor("offHandItem")
    ItemStack baity$getOffHandItem();

    @Accessor("offHandItem")
    void baity$setOffHandItem(ItemStack item);
}
