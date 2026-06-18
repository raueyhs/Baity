package com.shyeuar.baity.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public interface PlayerListHudMixin {

    @Accessor("visible")
    boolean isVisible();

    @Accessor("footer")
    Component getFooter();

    @Accessor("header")
    Component getHeader();

    @Invoker("getPlayerInfos")
    List<PlayerInfo> baity$getPlayerInfos();
}
