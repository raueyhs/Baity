package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.BitSet;

@Environment(EnvType.CLIENT)
public class CrosshairPainterValue implements Value {
    public enum Layer {
        ACTIVE,
        STATIC
    }

    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final int size;

    private final BitSet staticLayer;
    private final BitSet activeLayer;
    private Layer selectedLayer = Layer.ACTIVE;

    private boolean resetArmed = false;

    public CrosshairPainterValue(String name, String displayName, ModuleCategory category, int size, String staticEncoded, String activeEncoded, boolean initializeDefaultWhenEmpty) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.size = size % 2 == 0 ? (size + 1) : size;
        this.staticLayer = new BitSet(this.size * this.size);
        this.activeLayer = new BitSet(this.size * this.size);

        if (staticEncoded != null && !staticEncoded.isBlank()) {
            decodeInto(staticEncoded, this.staticLayer, this.size);
        }
        if (activeEncoded != null && !activeEncoded.isBlank()) {
            decodeInto(activeEncoded, this.activeLayer, this.size);
        }

        if (initializeDefaultWhenEmpty && this.staticLayer.isEmpty() && this.activeLayer.isEmpty()) {
            seedDefaultStaticLayer(this.staticLayer, this.size);
            seedDefaultActiveLayer(this.activeLayer, this.size);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Object getValue() {
        return "v1|" + size + "|" + encode(staticLayer, size) + "|" + encode(activeLayer, size);
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof String raw)) return;
        Parsed parsed = parse(raw);
        if (parsed == null) return;
        if (parsed.size != this.size) {
            return;
        }
        staticLayer.clear();
        activeLayer.clear();
        decodeInto(parsed.staticEncoded, staticLayer, size);
        decodeInto(parsed.activeEncoded, activeLayer, size);
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.CROSSHAIR_PAINTER;
    }

    public int getSize() {
        return size;
    }

    public int getCenterIndex() {
        return (size / 2);
    }

    public boolean isStaticSet(int x, int y) {
        return staticLayer.get(index(x, y));
    }

    public boolean isActiveSet(int x, int y) {
        return activeLayer.get(index(x, y));
    }

    public Layer getSelectedLayer() {
        return selectedLayer;
    }

    public void selectLayer(Layer layer) {
        if (layer != null) {
            this.selectedLayer = layer;
        }
    }

    public boolean isResetArmed() {
        return resetArmed;
    }

    public void armReset() {
        this.resetArmed = true;
    }

    public void disarmReset() {
        this.resetArmed = false;
    }

    public void confirmResetSelectedLayer() {
        if (!resetArmed) return;
        if (selectedLayer == Layer.ACTIVE) {
            activeLayer.clear();
            seedDefaultActiveLayer(activeLayer, size);
        } else {
            staticLayer.clear();
            seedDefaultStaticLayer(staticLayer, size);
        }
        resetArmed = false;
    }

    public void togglePixel(int x, int y) {
        if (!isInside(x, y)) return;
        BitSet target = (selectedLayer == Layer.ACTIVE) ? activeLayer : staticLayer;
        int idx = index(x, y);
        target.flip(idx);
    }

    public void clearPixel(int x, int y) {
        if (!isInside(x, y)) return;
        BitSet target = (selectedLayer == Layer.ACTIVE) ? activeLayer : staticLayer;
        target.clear(index(x, y));
    }

    public BitSet copyStaticLayer() {
        return (BitSet) staticLayer.clone();
    }

    public BitSet copyActiveLayer() {
        return (BitSet) activeLayer.clone();
    }

    private boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size;
    }

    private int index(int x, int y) {
        return y * size + x;
    }

    private static void seedDefaultStaticLayer(BitSet layer, int size) {
        int c = size / 2;
        layer.set(c * size + c);
        if (c - 1 >= 0) layer.set((c - 1) * size + c);
        if (c + 1 < size) layer.set((c + 1) * size + c);
        if (c - 1 >= 0) layer.set(c * size + (c - 1));
        if (c + 1 < size) layer.set(c * size + (c + 1));
    }

    private static void seedDefaultActiveLayer(BitSet layer, int size) {
        int c = size / 2;
        for (int d = 2; d <= 4; d++) {
            int upY = c - d;
            int downY = c + d;
            int leftX = c - d;
            int rightX = c + d;
            if (upY >= 0) layer.set(upY * size + c);
            if (downY < size) layer.set(downY * size + c);
            if (leftX >= 0) layer.set(c * size + leftX);
            if (rightX < size) layer.set(c * size + rightX);
        }
    }

    private record Parsed(int size, String staticEncoded, String activeEncoded) {}

    private static Parsed parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        String[] parts = s.split("\\|", 4);
        if (parts.length != 4) return null;
        if (!"v1".equals(parts[0])) return null;
        int size;
        try {
            size = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        return new Parsed(size, parts[2], parts[3]);
    }

    private static String encode(BitSet bits, int size) {
        int total = size * size;
        if (bits.isEmpty()) return "";
        final char[] ALPH = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
        StringBuilder out = new StringBuilder((total + 5) / 6);
        int acc = 0;
        int accBits = 0;
        for (int i = 0; i < total; i++) {
            int bit = bits.get(i) ? 1 : 0;
            acc |= (bit << accBits);
            accBits++;
            if (accBits == 6) {
                out.append(ALPH[acc & 63]);
                acc = 0;
                accBits = 0;
            }
        }
        if (accBits > 0) {
            out.append(ALPH[acc & 63]);
        }
        return out.toString();
    }

    private static void decodeInto(String encoded, BitSet out, int size) {
        if (encoded == null || encoded.isEmpty()) return;
        int total = size * size;
        int idx = 0;
        for (int i = 0; i < encoded.length() && idx < total; i++) {
            int v = decode6(encoded.charAt(i));
            if (v < 0) continue;
            for (int b = 0; b < 6 && idx < total; b++) {
                if (((v >> b) & 1) != 0) {
                    out.set(idx);
                }
                idx++;
            }
        }
    }

    private static int decode6(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return 26 + (c - 'a');
        if (c >= '0' && c <= '9') return 52 + (c - '0');
        if (c == '-') return 62;
        if (c == '_') return 63;
        return -1;
    }
}

