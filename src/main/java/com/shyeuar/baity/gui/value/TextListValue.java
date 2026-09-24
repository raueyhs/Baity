package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.utils.TextListCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
public class TextListValue implements Value {
    public static final float VERTICAL_PADDING = 4f;
    public static final float STATUS_STRIP_HEIGHT = 12f;
    public static final float ICON_SIZE = 12f;
    public static final float ADD_BUTTON_WIDTH = 30f;
    public static final float ADD_BUTTON_HEIGHT = 12f;
    public static final float TEXT_Y_OFFSET = 4f;
    public static final float FONT_LINE_HEIGHT = 9f;
    public static final float LEFT_PADDING = 6f;
    public static final float COLUMN_GAP = 4f;
    public static final float TEXT_RIGHT_PADDING = 8f;

    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final BooleanSupplier blockedSupplier;
    private final java.util.function.Predicate<String> matchedSupplier;
    private final List<String> entries = new ArrayList<>();
    private int armedRowIndex = -1;

    public TextListValue(String name, String displayName, ModuleCategory category,
                         BooleanSupplier blockedSupplier, java.util.function.Predicate<String> matchedSupplier) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.blockedSupplier = blockedSupplier;
        this.matchedSupplier = matchedSupplier;
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
        return TextListCodec.encode(entries);
    }

    @Override
    public void setValue(Object value) {
        entries.clear();
        entries.addAll(TextListCodec.decode(value == null ? "" : String.valueOf(value)));
        armedRowIndex = -1;
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.TEXT_LIST;
    }

    public boolean isBlocked() {
        return blockedSupplier != null && blockedSupplier.getAsBoolean();
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int getRowCount() {
        return entries.size() + 1;
    }

    public int getInputRowIndex() {
        return entries.size();
    }

    public boolean isInputRow(int row) {
        return row == entries.size();
    }

    public String getEntry(int row) {
        return isInputRow(row) || row < 0 || row >= entries.size() ? "" : entries.get(row);
    }

    public int getArmedRowIndex() {
        return armedRowIndex;
    }

    public boolean isArmed(int row) {
        return row >= 0 && row == armedRowIndex;
    }

    public void arm(int row) {
        this.armedRowIndex = row;
    }

    public void disarm() {
        this.armedRowIndex = -1;
    }

    public boolean matchesCurrentArea(int row) {
        if (matchedSupplier == null || isInputRow(row) || row < 0 || row >= entries.size()) {
            return false;
        }
        return matchedSupplier.test(entries.get(row));
    }

    public boolean addInputRow() {
        entries.add("");
        return true;
    }

    public boolean removeRow(int row) {
        if (row < 0 || row >= entries.size()) {
            return false;
        }
        entries.remove(row);
        disarm();
        return true;
    }

    public boolean commitRow(int row, String raw) {
        if (row < 0 || row > entries.size()) {
            return false;
        }
        disarm();
        if (row == entries.size()) {
            entries.add("");
        }
        String cleaned = TextListCodec.sanitize(raw);
        String previous = entries.get(row);
        if (cleaned.isEmpty() || isDuplicate(row, cleaned)) {
            entries.set(row, "");
            return !previous.isEmpty();
        }
        entries.set(row, cleaned);
        return !previous.equals(cleaned);
    }

    private boolean isDuplicate(int row, String candidate) {
        for (int i = 0; i < entries.size(); i++) {
            if (i != row && entries.get(i).equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static float getHeight(TextListValue value, float subOptionHeight) {
        if (value.getRowCount() <= 1) {
            return subOptionHeight;
        }
        return value.getRowCount() * subOptionHeight + VERTICAL_PADDING * 2f + STATUS_STRIP_HEIGHT;
    }

    public static Layout layout(TextListValue value, float x1, float y, float x2, float subOptionHeight) {
        return new Layout(value, x1, y, x2, subOptionHeight);
    }

    public static final class Layout {
        private final boolean blocked;
        private final int rowCount;
        private final float x1;
        private final float y;
        private final float x2;
        private final float subOptionHeight;
        private final float rowTop;
        private final float height;

        private Layout(TextListValue value, float x1, float y, float x2, float subOptionHeight) {
            this.blocked = value.isBlocked();
            this.rowCount = value.getRowCount();
            this.x1 = x1;
            this.y = y;
            this.x2 = x2;
            this.subOptionHeight = subOptionHeight;
            this.rowTop = rowCount <= 1 ? y : y + VERTICAL_PADDING + STATUS_STRIP_HEIGHT;
            this.height = TextListValue.getHeight(value, subOptionHeight);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public int getRowCount() {
            return rowCount;
        }

        public float getHeight() {
            return height;
        }

        public float getStatusY() {
            return y + VERTICAL_PADDING;
        }

        public float rowY(int row) {
            return rowTop + row * subOptionHeight;
        }

        public float iconX1() {
            return x1 + LEFT_PADDING;
        }

        public float markerX1() {
            return iconX1() + ICON_SIZE + COLUMN_GAP;
        }

        public float textX1() {
            return markerX1() + ICON_SIZE + COLUMN_GAP;
        }

        public float textX2() {
            return x2 - TEXT_RIGHT_PADDING;
        }

        public float iconY(int row) {
            return rowY(row) + TEXT_Y_OFFSET + (FONT_LINE_HEIGHT - ICON_SIZE) * 0.5f;
        }

        public float addButtonX1() {
            return iconX1();
        }

        public float addButtonX2() {
            return iconX1() + ADD_BUTTON_WIDTH;
        }

        public float addButtonY(int row) {
            return rowY(row) + (subOptionHeight - ADD_BUTTON_HEIGHT) * 0.5f;
        }

        public float addButtonY2(int row) {
            return addButtonY(row) + ADD_BUTTON_HEIGHT;
        }

        public float textY(int row) {
            return rowY(row) + TEXT_Y_OFFSET;
        }

        public float lineY(int row) {
            return rowY(row) + subOptionHeight - 4f;
        }
    }
}
