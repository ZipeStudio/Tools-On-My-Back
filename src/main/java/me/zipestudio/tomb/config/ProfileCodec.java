package me.zipestudio.tomb.config;

public final class ProfileCodec {

    public static final String PREFIX = "[TOMB|";
    public static final char CLOSE = ']';
    public static final int CHAT_LIMIT = 256;

    private static final String SEP = "|";
    private static final String DEFAULT_NAMESPACE = "minecraft:";

    private ProfileCodec() {
    }

    public static boolean looksLikeProfile(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return trimmed.length() > PREFIX.length()
                && trimmed.startsWith(PREFIX)
                && trimmed.charAt(trimmed.length() - 1) == CLOSE;
    }

    private static String body(String text) {
        String trimmed = text.trim();
        return trimmed.substring(PREFIX.length(), trimmed.length() - 1);
    }

    public static String encode(ToolEntry entry) {
        ToolEntry def = new ToolEntry();
        StringBuilder sb = new StringBuilder(PREFIX);

        String id = entry.getItemId() == null ? "" : entry.getItemId();
        if (id.startsWith(DEFAULT_NAMESPACE)) id = id.substring(DEFAULT_NAMESPACE.length());
        sb.append(id);

        String components = ConfigHandler.selectedComponentsJson(entry);
        if (!components.isEmpty()) put(sb, "c", escape(components));

        flag(sb, "n", entry.isMatchName(), def.isMatchName());
        flag(sb, "m", entry.isMatchModelData(), def.isMatchModelData());
        flag(sb, "i", entry.isMatchItemModel(), def.isMatchItemModel());
        flag(sb, "a", entry.isApplyToSimilar(), def.isApplyToSimilar());
        flag(sb, "e", entry.isEnabled(), def.isEnabled());
        flag(sb, "f", entry.isFlip(), def.isFlip());

        if (!equalsIgnoreCase(entry.getDisplayContext(), def.getDisplayContext())) {
            put(sb, "x", entry.getDisplayContext());
        }

        number(sb, "s", entry.getScale(), def.getScale());
        number(sb, "rx", entry.getAngleX(), def.getAngleX());
        number(sb, "ry", entry.getAngleY(), def.getAngleY());
        number(sb, "rz", entry.getAngleZ(), def.getAngleZ());
        number(sb, "ox", entry.getOffsetX(), def.getOffsetX());
        number(sb, "oy", entry.getOffsetY(), def.getOffsetY());
        number(sb, "oz", entry.getOffsetZ(), def.getOffsetZ());

        return sb.append(CLOSE).toString();
    }

    public static ToolEntry decode(String text) {
        if (!looksLikeProfile(text)) return null;
        String[] parts = body(text).split("\\" + SEP);
        if (parts.length == 0 || parts[0].isEmpty()) return null;

        ToolEntry entry = new ToolEntry();
        String id = parts[0].trim();
        entry.setItemId(id.indexOf(':') < 0 ? DEFAULT_NAMESPACE + id : id);
        entry.setComponents("");
        entry.setMatchName(false);
        entry.setMatchModelData(false);
        entry.setMatchItemModel(false);

        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq <= 0) continue;
            String key = parts[i].substring(0, eq).trim();
            String value = parts[i].substring(eq + 1);
            switch (key) {
                case "c" -> entry.setComponents(unescape(value));
                case "n" -> entry.setMatchName(bool(value));
                case "m" -> entry.setMatchModelData(bool(value));
                case "i" -> entry.setMatchItemModel(bool(value));
                case "a" -> entry.setApplyToSimilar(bool(value));
                case "e" -> entry.setEnabled(bool(value));
                case "f" -> entry.setFlip(bool(value));
                case "x" -> entry.setDisplayContext(value);
                case "s" -> entry.setScale(number(value, entry.getScale()));
                case "rx" -> entry.setAngleX(number(value, 0f));
                case "ry" -> entry.setAngleY(number(value, 0f));
                case "rz" -> entry.setAngleZ(number(value, 0f));
                case "ox" -> entry.setOffsetX(number(value, 0f));
                case "oy" -> entry.setOffsetY(number(value, 0f));
                case "oz" -> entry.setOffsetZ(number(value, 0f));
                default -> { }
            }
        }
        return entry;
    }

    public static int findToken(String text, int from) {
        if (text == null) return -1;
        for (int at = text.indexOf(PREFIX, from); at >= 0; at = text.indexOf(PREFIX, at + 1)) {
            if (closeAt(text, at) >= 0) return at;
        }
        return -1;
    }

    private static int closeAt(String text, int start) {
        for (int i = start + PREFIX.length(); i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == CLOSE) return i;
            if (Character.isWhitespace(c)) return -1;
        }
        return -1;
    }

    public static String firstToken(String text) {
        if (text == null) return "";
        int start = findToken(text, 0);
        return start < 0 ? "" : text.substring(start, tokenEnd(text, start));
    }

    public static int tokenEnd(String text, int start) {
        int close = closeAt(text, start);
        return close < 0 ? text.length() : close + 1;
    }

    private static final String NAMESPACE_TOKEN = "\"minecraft:";
    private static final String ESCAPED = " {}[]\"";
    private static final String CODES = "sbelrq";

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%') {
                out.append("%%");
            } else if (value.startsWith(NAMESPACE_TOKEN, i)) {
                out.append("%m");
                i += NAMESPACE_TOKEN.length() - 1;
            } else {
                int at = ESCAPED.indexOf(c);
                if (at < 0) out.append(c);
                else out.append('%').append(CODES.charAt(at));
            }
        }
        return out.toString();
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '%' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            char code = value.charAt(++i);
            if (code == '%') {
                out.append('%');
            } else if (code == 'm') {
                out.append(NAMESPACE_TOKEN);
            } else {
                int at = CODES.indexOf(code);
                if (at < 0) out.append('%').append(code);
                else out.append(ESCAPED.charAt(at));
            }
        }
        return out.toString();
    }

    private static void flag(StringBuilder sb, String key, boolean value, boolean def) {
        if (value != def) put(sb, key, value ? "1" : "0");
    }

    private static void number(StringBuilder sb, String key, float value, float def) {
        if (value != def) put(sb, key, format(value));
    }

    private static void put(StringBuilder sb, String key, String value) {
        sb.append(SEP).append(key).append('=').append(value);
    }

    private static String format(float value) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        if (s.startsWith("0.")) s = s.substring(1);
        else if (s.startsWith("-0.")) s = "-" + s.substring(2);
        return s.isEmpty() ? "0" : s;
    }

    private static boolean bool(String value) {
        return "1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim());
    }

    private static float number(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
