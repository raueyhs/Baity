package com.shyeuar.baity.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {

    @Accessor("visible")
    boolean isVisible();

    @Accessor("footer")
    Text getFooter();
}

