package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(ChatComponent.class)
public class ChromaOwnNameMixin {
    @ModifyArg(
        method = "method_71991(ILnet/minecraft/client/gui/GuiGraphics;FIIILnet/minecraft/client/GuiMessage$Line;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"
        ),
        index = 1
    )
    private FormattedCharSequence baity$animateOwnName(FormattedCharSequence original) {
        if (original == null) return null;

        Module module = ModuleManager.getModuleByName("ChromaOwnName");
        if (module == null || !module.isEnabled()) return original;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return original;

        String playerName = client.player.getName().getString();
        if (playerName == null || playerName.isEmpty()) return original;

        return recolorWithChroma(original, playerName);
    }

    private static FormattedCharSequence recolorWithChroma(FormattedCharSequence original, String playerName) {
        List<Glyph> glyphs = new ArrayList<>();
        original.accept((index, style, codepoint) -> {
            glyphs.add(new Glyph(codepoint, style));
            return true;
        });
        if (glyphs.isEmpty()) return original;

        int[] target = playerName.codePoints().toArray();
        if (target.length == 0 || target.length > glyphs.size()) return original;

        int[] matchStartByIndex = new int[glyphs.size()];
        int[] matchLenByIndex = new int[glyphs.size()];
        Arrays.fill(matchStartByIndex, -1);
        boolean found = false;

        for (int i = 0; i <= glyphs.size() - target.length; i++) {
            if (!matchesAtIgnoreCase(glyphs, target, i)) continue;

            int end = i + target.length;
            boolean leftBoundary = i == 0 || !isNameCodepoint(glyphs.get(i - 1).codepoint());
            boolean rightBoundary = end >= glyphs.size() || !isNameCodepoint(glyphs.get(end).codepoint());
            if (!leftBoundary || !rightBoundary) continue;

            for (int j = 0; j < target.length; j++) {
                matchStartByIndex[i + j] = i;
                matchLenByIndex[i + j] = target.length;
            }
            found = true;
        }
        if (!found) return original;

        double lightness = clamp(ConfigManager.chromaOwnNameChromaLightness, 0.2, 1.0);
        double chroma = clamp(ConfigManager.chromaOwnNameChromaChroma, 0.0, 0.4);
        double size = Math.max(0.1, ConfigManager.chromaOwnNameChromaSize);
        double speed = clamp(ConfigManager.chromaOwnNameChromaSpeed, 0.0, 8.0) * 0.5;
        double phase = (System.currentTimeMillis() / 1000.0) * speed;
        float saturation = (float) (chroma / 0.4);

        List<FormattedCharSequence> out = new ArrayList<>(glyphs.size());
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            Style style = glyph.style();
            int matchStart = matchStartByIndex[i];
            if (matchStart >= 0) {
                int matchLen = Math.max(1, matchLenByIndex[i]);
                double localProgress = matchLen == 1 ? 0.0 : (double) (i - matchStart) / (matchLen - 1);
                float hue = (float) positiveModulo((localProgress / size) - phase, 1.0);
                int rgb = Mth.hsvToRgb(hue, saturation, (float) lightness);
                style = style.withColor(rgb);
            }
            out.add(FormattedCharSequence.codepoint(glyph.codepoint(), style));
        }

        return FormattedCharSequence.composite(out);
    }

    private static boolean matchesAtIgnoreCase(List<Glyph> glyphs, int[] target, int offset) {
        for (int i = 0; i < target.length; i++) {
            int a = Character.toLowerCase(glyphs.get(offset + i).codepoint());
            int b = Character.toLowerCase(target[i]);
            if (a != b) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNameCodepoint(int codepoint) {
        return Character.isLetterOrDigit(codepoint) || codepoint == '_';
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }

    private record Glyph(int codepoint, Style style) {}
}
