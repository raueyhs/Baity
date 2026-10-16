package com.shyeuar.baity.gui.input;

import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings;
import com.shyeuar.baity.gui.value.SliderValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputQuirks;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiPredicate;

public final class LineTextInput {

    private static final int PASTE_CHAR_LIMIT = 2024;

    public enum KeyResult {
        NOT_HANDLED,
        HANDLED,
        CANCEL,
        COMMIT
    }

    private Policy policy;
    private String text = "";
    private Integer caretCp;

    public LineTextInput(Policy policy) {
        this.policy = policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getText() {
        return text;
    }

    public Integer getCaretCp() {
        return caretCp;
    }

    public void setText(String value) {
        text = sanitize(value == null ? "" : value);
        clampCaret();
    }

    public void setTextAndCaretAtEnd(String value) {
        text = sanitize(value == null ? "" : value);
        caretCp = null;
    }

    public void clear() {
        text = "";
        caretCp = null;
    }

    public KeyResult handleKey(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return KeyResult.CANCEL;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return KeyResult.COMMIT;
        }
        if (isCopyShortcut(keyCode)) {
            setClipboard(text);
            return KeyResult.HANDLED;
        }
        if (isPasteShortcut(keyCode)) {
            pasteFromClipboard();
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveCaretLeft();
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveCaretRight();
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || (isMac() && keyCode == GLFW.GLFW_KEY_DELETE)) {
            deleteBehindCaret(isPrimaryModifierDown());
            return KeyResult.HANDLED;
        }
        return KeyResult.NOT_HANDLED;
    }

    public boolean handleCodePoint(int codePoint) {
        if (codePoint == 0) {
            return false;
        }
        if (codePoint == '\b') {
            deleteBehindCaret(isPrimaryModifierDown());
            return true;
        }
        if (codePoint == 127) {
            if (isMac()) {
                deleteBehindCaret(isPrimaryModifierDown());
                return true;
            }
            return false;
        }
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        if (!policy.isAllowed(text, codePoint)) {
            return true;
        }
        insertAtCaret(new String(Character.toChars(codePoint)));
        return true;
    }

    public void onMousePressed(Font font, float localX) {
        onMousePressed(font, localX, -1);
    }

    public void onMousePressed(Font font, float localX, int maxWidth) {
        int cpCount = codePointCount(text);
        if (cpCount == 0) {
            caretCp = null;
            return;
        }
        if (maxWidth <= 0) {
            int bestCp = 0;
            int bestDist = Integer.MAX_VALUE;
            for (int cp = 0; cp <= cpCount; cp++) {
                int width = font.width(text.substring(0, cpIndexToCharIndex(text, cp)));
                int dist = Math.abs(width - Math.round(localX));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestCp = cp;
                }
            }
            caretCp = bestCp >= cpCount ? null : bestCp;
            return;
        }

        int windowCaret = caretCp == null ? cpCount : caretCp;
        VisibleWindow win = computeVisibleWindow(font, text, windowCaret, maxWidth);
        float x = localX;
        if (win.leftEllipsis()) {
            int dotsW = font.width(ELLIPSIS);
            if (x <= dotsW * 0.5f) {
                caretCp = win.startCp();
                return;
            }
            x -= dotsW;
        }
        String mid = sliceByCodePoints(text, win.startCp(), win.endCp());
        int midCpCount = codePointCount(mid);
        int bestRel = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int rel = 0; rel <= midCpCount; rel++) {
            int width = font.width(mid.substring(0, cpIndexToCharIndex(mid, rel)));
            int dist = Math.abs(width - Math.round(x));
            if (dist < bestDist) {
                bestDist = dist;
                bestRel = rel;
            }
        }
        int bestCp = win.startCp() + bestRel;
        caretCp = bestCp >= cpCount ? null : bestCp;
    }

    public static boolean tryCommitSlider(SliderValue slider, String inputText) {
        if (inputText == null || inputText.isEmpty() || inputText.equals("-") || inputText.equals(".")) {
            return false;
        }
        if (!isValidSliderInputText(slider, inputText)) {
            return false;
        }
        try {
            return slider.trySetValue(Double.parseDouble(inputText));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static int cpIndexToCharIndex(String s, int cpIndex) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int cpCount = s.codePointCount(0, s.length());
        int target = Math.max(0, Math.min(cpIndex, cpCount));
        int curCp = 0;
        for (int charIdx = 0; charIdx < s.length(); ) {
            if (curCp == target) {
                return charIdx;
            }
            int cp = s.codePointAt(charIdx);
            charIdx += Character.charCount(cp);
            curCp++;
        }
        return s.length();
    }

    public static String limitByCodePoints(String s, int maxCp) {
        if (s == null) {
            return "";
        }
        if (maxCp < 0) {
            return s;
        }
        int cpCount = s.codePointCount(0, s.length());
        if (cpCount <= maxCp) {
            return s;
        }
        int cpSeen = 0;
        int charIdx = 0;
        while (charIdx < s.length()) {
            int cp = s.codePointAt(charIdx);
            if (cpSeen >= maxCp) {
                break;
            }
            charIdx += Character.charCount(cp);
            cpSeen++;
        }
        return s.substring(0, charIdx);
    }

    public static boolean shouldBlinkCursor() {
        return System.currentTimeMillis() % 1000 < 500;
    }

    public static final String ELLIPSIS = "...";

    public record VisibleWindow(int startCp, int endCp, boolean leftEllipsis, boolean rightEllipsis) {
    }

    public static String sliceByCodePoints(String text, int startCp, int endCp) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int from = cpIndexToCharIndex(text, startCp);
        int to = cpIndexToCharIndex(text, endCp);
        if (from >= to) {
            return "";
        }
        return text.substring(from, to);
    }

    public static VisibleWindow computeVisibleWindow(Font font, String text, Integer caretCp, int maxWidth) {
        if (text == null) {
            text = "";
        }
        int cpCount = codePointCount(text);
        int caret = caretCp == null ? cpCount : Math.max(0, Math.min(caretCp, cpCount));
        if (maxWidth <= 0 || font.width(text) <= maxWidth) {
            return new VisibleWindow(0, cpCount, false, false);
        }
        int dotsW = font.width(ELLIPSIS);

        int start = 0;
        while (start < caret) {
            boolean leftEll = start > 0;
            boolean rightEll = caret < cpCount;
            int budget = maxWidth - (leftEll ? dotsW : 0) - (rightEll ? dotsW : 0);
            if (font.width(sliceByCodePoints(text, start, caret)) <= Math.max(0, budget)) {
                break;
            }
            start++;
        }

        int end = caret;
        while (end < cpCount) {
            boolean leftEll = start > 0;
            boolean rightEll = (end + 1) < cpCount;
            int budget = maxWidth - (leftEll ? dotsW : 0) - (rightEll ? dotsW : 0);
            if (font.width(sliceByCodePoints(text, start, end + 1)) > Math.max(0, budget)) {
                break;
            }
            end++;
        }

        while (start > 0) {
            boolean leftEll = (start - 1) > 0;
            boolean rightEll = end < cpCount;
            int budget = maxWidth - (leftEll ? dotsW : 0) - (rightEll ? dotsW : 0);
            if (font.width(sliceByCodePoints(text, start - 1, end)) > Math.max(0, budget)) {
                break;
            }
            start--;
        }

        return new VisibleWindow(start, end, start > 0, end < cpCount);
    }

    public static void drawTextWithBlinkCursor(
        GuiGraphicsExtractor context,
        Font font,
        String text,
        Integer caretCp,
        int x,
        int y,
        int color,
        boolean focused,
        boolean blinkOn
    ) {
        drawTextWithBlinkCursor(context, font, text, caretCp, x, y, color, focused, blinkOn, -1);
    }

    public static void drawTextWithBlinkCursor(
        GuiGraphicsExtractor context,
        Font font,
        String text,
        Integer caretCp,
        int x,
        int y,
        int color,
        boolean focused,
        boolean blinkOn,
        int maxWidth
    ) {
        drawTextWithBlinkCursor(context, font, text, caretCp, x, y, color, focused, blinkOn, maxWidth, false);
    }

    public static void drawTextWithBlinkCursor(
        GuiGraphicsExtractor context,
        Font font,
        String text,
        Integer caretCp,
        int x,
        int y,
        int color,
        boolean focused,
        boolean blinkOn,
        int maxWidth,
        boolean legacyColors
    ) {
        if (text == null) {
            text = "";
        }
        int windowCaret = focused
                ? (caretCp != null ? caretCp : codePointCount(text))
                : 0;
        VisibleWindow win = computeVisibleWindow(font, text, windowCaret, maxWidth);
        int drawX = x;
        if (win.leftEllipsis()) {
            context.text(font, ELLIPSIS, drawX, y, color, false);
            drawX += font.width(ELLIPSIS);
        }
        String mid = sliceByCodePoints(text, win.startCp(), win.endCp());
        if (!focused || !blinkOn) {
            if (legacyColors) {
                drawLegacyColoredText(context, font, mid, drawX, y, color);
            } else {
                context.text(font, mid, drawX, y, color, false);
            }
        } else {
            int cpCount = codePointCount(text);
            int absCaret = caretCp != null ? caretCp : cpCount;
            absCaret = Math.max(win.startCp(), Math.min(absCaret, win.endCp()));
            int relCaret = absCaret - win.startCp();
            int midCharPos = cpIndexToCharIndex(mid, relCaret);
            String before = mid.substring(0, midCharPos);
            String after = mid.substring(midCharPos);
            int caretX;
            if (legacyColors) {
                caretX = drawLegacyColoredText(context, font, before, drawX, y, color);
            } else {
                context.text(font, before, drawX, y, color, false);
                caretX = drawX + font.width(before);
            }
            if (!after.isEmpty()) {
                if (legacyColors) {
                    drawLegacyColoredText(context, font, after, caretX, y, color);
                } else {
                    context.text(font, after, caretX, y, color, false);
                }
            }
            context.fill(caretX, y, caretX + 1, y + 9, color);
        }
        if (win.rightEllipsis()) {
            context.text(font, ELLIPSIS, drawX + font.width(mid), y, color, false);
        }
    }

    public static int drawLegacyColoredText(
        GuiGraphicsExtractor context,
        Font font,
        String text,
        int x,
        int y,
        int defaultColor
    ) {
        if (text == null || text.isEmpty()) {
            return x;
        }
        int alpha = defaultColor & 0xFF000000;
        int currentColor = defaultColor;
        int drawX = x;
        int segmentStart = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int cpLength = Character.charCount(cp);
            Integer nextColor = null;
            int consumed = cpLength;
            if (cp == '&' && i + cpLength < text.length()) {
                int nextCp = text.codePointAt(i + cpLength);
                char code = Character.toLowerCase((char) nextCp);
                Integer rgb = com.shyeuar.baity.utils.NickRenderUtils.legacyColorRgb(code);
                if (rgb != null) {
                    nextColor = alpha | rgb;
                    consumed = cpLength + Character.charCount(nextCp);
                } else if (code == 'r') {
                    nextColor = defaultColor;
                    consumed = cpLength + Character.charCount(nextCp);
                }
            }
            if (nextColor != null) {
                if (i > segmentStart) {
                    String segment = text.substring(segmentStart, i);
                    context.text(font, segment, drawX, y, currentColor, false);
                    drawX += font.width(segment);
                }
                currentColor = nextColor;
                segmentStart = i;
                i += consumed;
                continue;
            }
            i += cpLength;
        }
        if (segmentStart < text.length()) {
            String segment = text.substring(segmentStart);
            context.text(font, segment, drawX, y, currentColor, false);
            drawX += font.width(segment);
        }
        return drawX;
    }

    public static void drawCenteredClippedWithBlinkCursor(
        GuiGraphicsExtractor context,
        Font font,
        String text,
        Integer caretCp,
        float lineX1,
        float lineY,
        float lineX2,
        int color,
        boolean focused
    ) {
        float lineWidth = lineX2 - lineX1;
        float textAreaTop = lineY - 11f;
        float drawY = textAreaTop + (11f - font.lineHeight) * 0.5f;
        int drawYInt = Math.round(drawY);
        if (text == null) {
            text = "";
        }
        int maxWidth = Math.max(0, Math.round(lineWidth));
        int textWidth = font.width(text);
        int drawX;
        if (textWidth <= maxWidth) {
            drawX = Math.round(lineX1 + (lineWidth - textWidth) * 0.5f);
        } else {
            drawX = Math.round(lineX1);
        }
        drawTextWithBlinkCursor(
            context,
            font,
            text,
            caretCp,
            drawX,
            drawYInt,
            color,
            focused,
            focused && shouldBlinkCursor(),
            maxWidth
        );
    }

    private void moveCaretLeft() {
        int cpCount = codePointCount(text);
        if (caretCp == null) {
            caretCp = Math.max(0, cpCount - 1);
        } else {
            caretCp = Math.max(0, caretCp - 1);
        }
    }

    private void moveCaretRight() {
        if (caretCp == null) {
            return;
        }
        int cpCount = codePointCount(text);
        if (caretCp >= cpCount - 1) {
            caretCp = null;
        } else {
            caretCp++;
        }
    }

    private void deleteBehindCaret(boolean deleteWord) {
        if (text.isEmpty()) {
            return;
        }
        if (caretCp != null) {
            if (caretCp == 0) {
                return;
            }
            int leftCp = caretCp - 1;
            int leftChar = cpIndexToCharIndex(text, leftCp);
            int rightChar = cpIndexToCharIndex(text, caretCp);
            text = text.substring(0, leftChar) + text.substring(rightChar);
            caretCp = leftCp;
            return;
        }
        if (deleteWord) {
            text = dropLastWord(text);
        } else {
            int cpCount = codePointCount(text);
            if (cpCount <= 1) {
                text = "";
            } else {
                int cutFrom = cpIndexToCharIndex(text, cpCount - 1);
                text = text.substring(0, cutFrom);
            }
        }
    }

    private void insertAtCaret(String insert) {
        if (insert == null || insert.isEmpty()) {
            return;
        }
        StringBuilder filtered = new StringBuilder();
        String base = text;
        for (int i = 0; i < insert.length(); ) {
            int cp = insert.codePointAt(i);
            if (policy.isAllowed(base, cp)) {
                filtered.appendCodePoint(cp);
                base += new String(Character.toChars(cp));
            }
            i += Character.charCount(cp);
        }
        if (filtered.isEmpty()) {
            return;
        }
        String addition = filtered.toString();
        if (caretCp == null) {
            text = sanitize(text + addition);
        } else {
            int charPos = cpIndexToCharIndex(text, caretCp);
            text = sanitize(text.substring(0, charPos) + addition + text.substring(charPos));
            caretCp = Math.min(caretCp + addition.codePointCount(0, addition.length()), codePointCount(text));
        }
    }

    private void pasteFromClipboard() {
        String clip = getClipboard();
        if (clip == null || clip.isEmpty()) {
            return;
        }
        if (clip.length() > PASTE_CHAR_LIMIT) {
            clip = clip.substring(0, PASTE_CHAR_LIMIT);
        }
        insertAtCaret(clip);
    }

    private static String dropLastWord(String value) {
        int space = value.lastIndexOf(' ');
        if (space < 0) {
            return "";
        }
        return value.substring(0, space);
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private static long windowHandle() {
        Minecraft client = Minecraft.getInstance();
        return client.getWindow().handle();
    }

    private static boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(windowHandle(), key) == GLFW.GLFW_PRESS;
    }

    private static boolean isPrimaryModifierDown() {
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            return isKeyDown(GLFW.GLFW_KEY_LEFT_SUPER) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SUPER);
        }
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isCopyShortcut(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_C && isPrimaryModifierDown();
    }

    private static boolean isPasteShortcut(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_V && isPrimaryModifierDown();
    }

    private static String getClipboard() {
        Minecraft client = Minecraft.getInstance();
        return client.keyboardHandler.getClipboard();
    }

    private static void setClipboard(String value) {
        Minecraft.getInstance().keyboardHandler.setClipboard(value);
    }

    private String sanitize(String raw) {
        StringBuilder out = new StringBuilder();
        String building = "";
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            if (policy.isAllowed(building, cp)) {
                out.appendCodePoint(cp);
                building += new String(Character.toChars(cp));
            }
            i += Character.charCount(cp);
        }
        if (policy.maxCodePoints() >= 0) {
            return limitByCodePoints(out.toString(), policy.maxCodePoints());
        }
        return out.toString();
    }

    private void clampCaret() {
        if (caretCp == null) {
            return;
        }
        int cpCount = codePointCount(text);
        if (caretCp >= cpCount) {
            caretCp = null;
        } else {
            caretCp = Math.max(0, caretCp);
        }
    }

    private static int codePointCount(String s) {
        return s == null || s.isEmpty() ? 0 : s.codePointCount(0, s.length());
    }

    private static boolean isSliderCharAllowed(SliderValue slider, String textBefore, int codePoint) {
        if (codePoint > 0xFFFF) {
            return false;
        }
        char ch = (char) codePoint;
        if (ch != '.' && ch != '-' && !Character.isDigit(codePoint)) {
            return false;
        }
        return isValidSliderInputText(slider, textBefore + ch);
    }

    private static boolean isValidSliderInputText(SliderValue slider, String raw) {
        if (raw == null || raw.isEmpty()) {
            return true;
        }
        String s = raw;
        boolean negative = false;
        if (s.startsWith("-")) {
            if (slider.getMinValue() >= 0) {
                return false;
            }
            negative = true;
            s = s.substring(1);
            if (s.isEmpty()) {
                return true;
            }
        }

        int dotCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                dotCount++;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        if (dotCount > 1) {
            return false;
        }
        if (dotCount > 0 && slider.getDecimalPlaces() <= 0) {
            return false;
        }

        int dotIndex = s.indexOf('.');
        if (dotIndex >= 0) {
            String fractional = s.substring(dotIndex + 1);
            if (fractional.length() > slider.getDecimalPlaces()) {
                return false;
            }
        }

        if (s.endsWith(".")) {
            if (slider.getDecimalPlaces() <= 0) {
                return false;
            }
            s = s.substring(0, s.length() - 1);
            if (s.isEmpty()) {
                return true;
            }
        }

        try {
            double v = Double.parseDouble((negative ? "-" : "") + s);
            return v <= slider.getMaxValue();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static final class Policy {
        private final int maxCodePoints;
        private final BiPredicate<String, Integer> allowed;

        private Policy(int maxCodePoints, BiPredicate<String, Integer> allowed) {
            this.maxCodePoints = maxCodePoints;
            this.allowed = allowed;
        }

        public int maxCodePoints() {
            return maxCodePoints;
        }

        public boolean isAllowed(String currentText, int codePoint) {
            return allowed.test(currentText == null ? "" : currentText, codePoint);
        }

        public static Policy search() {
            return new Policy(-1, (text, cp) -> cp >= 32 && cp < 127);
        }

        public static Policy hexColor() {
            return new Policy(6, (text, cp) -> {
                if (cp > 0xFFFF) {
                    return false;
                }
                char ch = (char) (int) cp;
                return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
            });
        }

        public static Policy damageSymbols() {
            return new Policy(
                FancyDmgSplashSettings.MAX_DAMAGE_SYMBOL_CODE_POINTS,
                (text, cp) -> !Character.isISOControl(cp)
            );
        }

        public static Policy freeText(int maxCodePoints) {
            return new Policy(maxCodePoints, (text, cp) -> !Character.isISOControl(cp));
        }

        public static Policy radialIconId(int maxCodePoints) {
            return new Policy(maxCodePoints, (text, cp) -> {
                if (Character.isISOControl(cp)) {
                    return false;
                }
                if (cp >= 'A' && cp <= 'Z') {
                    return true;
                }
                if (cp >= 'a' && cp <= 'z') {
                    return true;
                }
                if (cp >= '0' && cp <= '9') {
                    return true;
                }
                return cp == '_' || cp == '-' || cp == '.' || cp == '/' || cp == ':' || cp == ' ';
            });
        }

        public static Policy forSlider(SliderValue slider) {
            return new Policy(-1, (text, cp) -> isSliderCharAllowed(slider, text, cp));
        }
    }
}
