package me.zipestudio.tomb.config;

/**
 * Turns one {@link ToolEntry} into a single line of text and back, so a tuned item can be handed to
 * another player through the clipboard or through chat without either of them opening the config file.
 *
 * <p>The format is deliberately compact and readable rather than JSON or base64: chat messages are capped
 * at {@link #CHAT_LIMIT} characters, and the JSON of one entry is already about that on its own before any
 * component discriminator is added. Only fields that differ from a fresh entry are written, and the
 * vanilla namespace is dropped from the item id, so an ordinary profile lands around 60–90 characters:
 *
 * <pre>[TOMB|diamond_sword|s=1.13|rx=15|oy=.28]</pre>
 *
 * The brackets are part of the format: a profile is usually written into a sentence of someone's own, and
 * they say where it starts and where it ends at a glance. They also match the {@code [TOMB | Import]} chip
 * a recognised profile turns into. Because they delimit, they must never occur inside the payload — which
 * is why {@link #escape} percent-encodes them along with everything else that would break the token.
 *
 * <p>Unknown keys are ignored on the way back in, so a profile written by a later version still imports
 * whatever this version does understand instead of failing outright.
 */
public final class ProfileCodec {

    /** What every profile line starts with, so chat can spot one without parsing it. */
    public static final String PREFIX = "[TOMB|";
    /** What every profile line ends with. Never appears inside one — {@link #escape} sees to that. */
    public static final char CLOSE = ']';
    /** The vanilla chat message limit. A profile longer than this can still go through the clipboard. */
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

    /** The profile with its brackets taken off — everything between {@link #PREFIX} and {@link #CLOSE}. */
    private static String body(String text) {
        String trimmed = text.trim();
        return trimmed.substring(PREFIX.length(), trimmed.length() - 1);
    }

    public static String encode(ToolEntry entry) {
        ToolEntry def = new ToolEntry();
        StringBuilder sb = new StringBuilder(PREFIX);

        String id = entry.getItemId() == null ? "" : entry.getItemId();
        // the namespace is noise for the 99% case; import puts it back when the id carries none
        if (id.startsWith(DEFAULT_NAMESPACE)) id = id.substring(DEFAULT_NAMESPACE.length());
        sb.append(id);

        // Only the components the entry actually matches on: the ones its filters ignore change nothing
        // on the other end and are the reason a plugin item's profile outgrew the chat limit — a single
        // custom_name from a server plugin is a couple of hundred characters of JSON on its own.
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

    /** @return the entry the line describes, or null when it is not a profile this version can read. */
    public static ToolEntry decode(String text) {
        if (!looksLikeProfile(text)) return null;
        String[] parts = body(text).split("\\" + SEP);
        if (parts.length == 0 || parts[0].isEmpty()) return null;

        ToolEntry entry = new ToolEntry();
        String id = parts[0].trim();
        // A namespace-less id already resolves as vanilla through Identifier.tryParse, but the config
        // and the editor's id box show it verbatim, so write the full one and keep both readable.
        entry.setItemId(id.indexOf(':') < 0 ? DEFAULT_NAMESPACE + id : id);
        entry.setComponents("");
        // A fresh entry has every filter off; a profile that carries a discriminator turns its own back on.
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
                default -> { /* written by a newer version — ignore it rather than refuse the profile */ }
            }
        }
        return entry;
    }

    /**
     * Finds where a profile sits inside a longer line, so it can be written into a sentence of one's own
     * ("look at this sword [TOMB|diamond_sword|s=1.2] nice eh?") rather than sent on a line of its own.
     *
     * @return the start index of a CLOSED profile token, or -1. An opening bracket with nothing closing it
     *         is somebody typing about the mod, not a profile, and is skipped rather than reported.
     */
    public static int findToken(String text, int from) {
        if (text == null) return -1;
        for (int at = text.indexOf(PREFIX, from); at >= 0; at = text.indexOf(PREFIX, at + 1)) {
            if (closeAt(text, at) >= 0) return at;
        }
        return -1;
    }

    /**
     * Index of the bracket closing the token that starts at {@code start}, or -1.
     *
     * <p>Scanning for the first {@link #CLOSE} is only sound because a payload can never contain one — see
     * {@link #escape}. The whitespace guard stops an unclosed opener from swallowing the rest of a sentence
     * and latching onto some unrelated bracket further along: a profile never contains a space either.
     */
    private static int closeAt(String text, int start) {
        for (int i = start + PREFIX.length(); i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == CLOSE) return i;
            if (Character.isWhitespace(c)) return -1;
        }
        return -1;
    }

    /**
     * The profile inside an arbitrary piece of text, or "" — so pasting a whole sentence copied out of
     * someone's chat works as well as pasting the bare profile.
     */
    public static String firstToken(String text) {
        if (text == null) return "";
        int start = findToken(text, 0);
        return start < 0 ? "" : text.substring(start, tokenEnd(text, start));
    }

    /**
     * Where the token starting at {@code start} ends, exclusive — just past its closing bracket. Callers
     * only ever reach this for a start {@link #findToken} handed them, which is always a closed one; the
     * fallback is there so a hand-rolled index cannot run off the end.
     */
    public static int tokenEnd(String text, int start) {
        int close = closeAt(text, start);
        return close < 0 ? text.length() : close + 1;
    }

    /**
     * Percent-encodes everything about a discriminator that does not survive the trip.
     *
     * <p>What has to go, and why. A space would cut the token in half when the profile is written into a
     * sentence (a custom name of "Cool Sword" puts one in the JSON). Braces and quotes are encoded because
     * chat plugins sanitise them: a server was seen delivering {@code c={"a":{"b":[1]}}} as
     * {@code c="a":"b":[1]}, which decodes to nothing. Square brackets travel through plugins fine, but
     * they are the format's own delimiters now, and a JSON array inside a discriminator would otherwise end
     * the token halfway through it. Colons, commas and digits still go as they are.
     *
     * <p><b>One character per escape, not two hex digits.</b> These lines are measured against the 256 of a
     * chat message and the discriminator is what pushes a profile over it, so {@code %7B} became {@code %b}
     * and so on. {@code %m} is the same trick taken further: it stands for the whole of {@code "minecraft:},
     * which occurs in every component key and in most id-valued components. On a real plugin sword
     * (custom_model_data) that took the profile from 102 characters to 82; on a coloured custom name, from
     * 106 to 79.
     *
     * <p>Both directions are a single left-to-right pass. Chained {@code String.replace} calls, which is
     * what this used to be, cannot be made safe once the codes are this short: decoding an escaped percent
     * followed by a code — {@code %%q} — would have the {@code %q} pass find the wrong two characters.
     */
    private static final String NAMESPACE_TOKEN = "\"minecraft:";
    /** Characters that must not travel literally, and the letter each is written as after a {@code %}. */
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
                // an unknown code was written by a later version: leave it be rather than lose the profile
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

    /** Two decimals at most, with every character that carries no information dropped: 0.28 → .28, 1.00 → 1. */
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
