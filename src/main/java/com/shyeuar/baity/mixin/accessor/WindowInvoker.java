package com.shyeuar.baity.mixin.accessor;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Window.class)
public interface WindowInvoker {
    @Invoker("onFramebufferResize")
    void baity$onFramebufferResize(int width, int height);

    @Invoker("onResize")
    void baity$onResize(int width, int height);

    @Invoker("syncWindow")
    void baity$syncWindow();
}
