package com.shyeuar.baity.utils;

import java.util.ArrayList;
import java.util.List;

public final class TextListCodec {
    private static final String SEPARATOR = ";";

    private TextListCodec() {
    }

    public static String encode(List<String> entries) {
        if (entries == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(sanitize(entries.get(i)));
        }
        return builder.toString();
    }

    public static List<String> decode(String raw) {
        List<String> entries = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return entries;
        }
        for (String part : raw.split(SEPARATOR, -1)) {
            String cleaned = sanitize(part);
            if (!cleaned.isEmpty() && containsIgnoreCase(entries, cleaned)) {
                entries.add("");
                continue;
            }
            entries.add(cleaned);
        }
        return entries;
    }

    public static String sanitize(String entry) {
        if (entry == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(entry.length());
        for (int i = 0; i < entry.length(); i++) {
            char c = entry.charAt(i);
            if (c == ';' || Character.isISOControl(c)) {
                continue;
            }
            builder.append(c);
        }
        return builder.toString().trim();
    }

    public static boolean containsIgnoreCase(List<String> entries, String candidate) {
        if (entries == null || candidate == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
