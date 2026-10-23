package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.legacytweaks.legacymusic.LegacyMusicPriority;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class SpadeUseMixin {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void baity$trackSpadeAbility(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        LegacyMusicPriority.onItemUsed(player, hand);
    }
}
