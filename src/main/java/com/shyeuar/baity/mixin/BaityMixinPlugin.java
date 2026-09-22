package com.shyeuar.baity.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BaityMixinPlugin implements IMixinConfigPlugin {

    private static final String MODERN_UI_MIXIN = "com.shyeuar.baity.mixin.ModernUiFloatingTextMixin";
    private static final String MOD_MENU_MIXIN = "com.shyeuar.baity.mixin.ModMenuNickTweaksMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            if (mixinClassName.startsWith(MODERN_UI_MIXIN)) {
                return FabricLoader.getInstance().isModLoaded("modernui");
            }
            if (mixinClassName.startsWith(MOD_MENU_MIXIN)) {
                return FabricLoader.getInstance().isModLoaded("modmenu");
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
