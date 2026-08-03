package me.zipestudio.tomb.client;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.screen.ToolEditorScreen;
import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.config.ProfileCodec;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

/**
 * The chat half of profile sharing: turns a received profile line into one clickable message, and opens
 * the editor on it when that message is clicked.
 *
 * <p>The click rides on a plain RUN_COMMAND click event carrying {@link #CLICK_COMMAND}. Nothing is ever
 * sent to the server: the click is caught client-side before vanilla would run it, which is what lets this
 * work on a vanilla server with no command registered on either side.
 */
public final class ProfileShare {

    /** Marks a click event as ours. Never reaches a server — the Screen mixin swallows it first. */
    public static final String CLICK_COMMAND = "/tomb-profile ";

    private ProfileShare() {
    }

    /**
     * Rewrites a chat line, swapping every profile written into it for a short clickable chip and leaving
     * the player's own words around it alone — the profile is copied to the clipboard and pasted into
     * whatever sentence they like, so it can sit anywhere in the message.
     *
     * @return the rewritten content, or null when the line carries no profile and should be left as is.
     */
    public static MutableComponent rewrite(String raw) {
        int at = ProfileCodec.findToken(raw, 0);
        if (at < 0) return null;

        MutableComponent out = Component.empty();
        int cursor = 0;
        while (at >= 0) {
            if (at > cursor) out.append(Component.literal(raw.substring(cursor, at)));
            int end = ProfileCodec.tokenEnd(raw, at);
            out.append(chip(raw.substring(at, end)));
            cursor = end;
            at = ProfileCodec.findToken(raw, cursor);
        }
        if (cursor < raw.length()) out.append(Component.literal(raw.substring(cursor)));
        return out;
    }

    /**
     * Whether the flag {@code ChatListener.handleSystemMessage} carries means "this is the action bar".
     *
     * <p>It did up to 26.1, which split the action bar out into its own {@code handleOverlay} and left this
     * flag meaning something else — the packet path now passes a constant {@code true}. Reading it as
     * "overlay" there threw away every system message, which is exactly the chat a plugin server sends.
     */
    public static boolean isActionBar(boolean flag) {
        //? if >=26.1 {
        return false;
        //?} else {
        /*return flag;
        *///?}
    }

    /**
     * The same rewrite for a line that arrives already built as a component rather than as raw text —
     * which is what happens on any server whose chat goes through a plugin: it cancels the player chat and
     * broadcasts its own system message, so nothing ever reaches the player-chat path.
     *
     * <p>Walks the tree rather than flattening it, so the server's own formatting around the profile
     * survives: every node keeps its style and its siblings, and only the text that carries a profile is
     * swapped for the chip. Literals and translation arguments are both walked — a plugin that formats
     * through a translation hides the player's words in an argument, which is why one server showed the
     * chip and another did not. Anything the walk cannot reach is caught by the flattened fallback.
     *
     * @return the rewritten line, or null when it carries no profile and must be left alone.
     */
    public static Component rewriteComponent(Component source) {
        boolean[] changed = {false};
        MutableComponent out = rewriteNode(source, changed);
        if (changed[0]) return out;

        // Last resort. Servers build a chat line in whatever shape they like, and one of them will always
        // be a shape this cannot walk into — a profile split across two nodes because the plugin coloured
        // it in pieces, or an argument that is neither text nor a component. Flattening loses the server's
        // own colours, which is a fair price for the profile being clickable at all.
        return rewrite(source.getString());
    }

    private static MutableComponent rewriteNode(Component node, boolean[] changed) {
        MutableComponent result = rewriteContents(node.getContents(), changed);
        if (result == null) result = node.plainCopy();
        // the node's own style stays on the container, so the words around the chip keep the colour the
        // server gave them; the chip sets its colour and underline itself and so wins over it
        result.setStyle(node.getStyle());
        for (Component sibling : node.getSiblings()) result.append(rewriteNode(sibling, changed));
        return result;
    }

    /**
     * @return the node's contents rewritten, or null when this kind of content holds no profile.
     *
     * <p>Translatable contents matter as much as literals here: a chat plugin that formats through a
     * translation puts the player's words in an <em>argument</em>, where walking siblings alone never
     * reaches them — which is why one server showed the chip and another showed the raw line.
     */
    private static MutableComponent rewriteContents(ComponentContents contents, boolean[] changed) {
        if (contents instanceof PlainTextContents plain) {
            MutableComponent replaced = rewrite(plain.text());
            if (replaced == null) return null;
            changed[0] = true;
            return replaced;
        }
        if (contents instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] rewritten = new Object[args.length];
            boolean[] inArgs = {false};
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component component) {
                    rewritten[i] = rewriteNode(component, inArgs);
                } else if (args[i] instanceof String text) {
                    MutableComponent replaced = rewrite(text);
                    if (replaced != null) inArgs[0] = true;
                    rewritten[i] = replaced != null ? replaced : args[i];
                } else {
                    rewritten[i] = args[i];
                }
            }
            if (!inArgs[0]) return null;
            changed[0] = true;
            return MutableComponent.create(
                    new TranslatableContents(translatable.getKey(), translatable.getFallback(), rewritten));
        }
        return null;
    }

    /**
     * What a profile looks like once the mod has recognised it: one {@code [TOMB | Import]} chip. The mod's
     * name stays in the text on purpose — someone seeing it in a screenshot should be able to tell which
     * mod it belongs to. Hovering anywhere on it shows the item itself, so the profile can be judged
     * before importing, and clicking anywhere on it opens the editor.
     */
    private static MutableComponent chip(String payload) {
        ToolEntry entry = ProfileCodec.decode(payload);
        ItemStack stack = entry == null ? ItemStack.EMPTY : ConfigHandler.buildStack(entry);

        return TOMBServer.text("chat.chip").withStyle(style -> {
            Style styled = chipStyle(style)
                    //? if >=1.21.11 {
                    .withClickEvent(new ClickEvent.RunCommand(CLICK_COMMAND + payload))
                    //?} else {
                    /*.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, CLICK_COMMAND + payload))
                    *///?}
                    ;
            // an id nothing is registered under builds no stack, and there is nothing to show for it
            return stack.isEmpty() ? textHover(styled, TOMBServer.text("chat.chip.unknown")) : itemHover(styled, stack);
        });
    }

    /** The look of a recognised profile: underlined, in the parchment tone the mod's own text uses. */
    private static Style chipStyle(Style style) {
        return style.withColor(TextColor.fromRgb(0xF0F0D8)).withUnderlined(true);
    }

    private static Style textHover(Style style, Component text) {
        //? if >=1.21.11 {
        return style.withHoverEvent(new HoverEvent.ShowText(text));
        //?} else {
        /*return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
        *///?}
    }

    /** Hovering the link shows the item the way vanilla shows any item in chat. */
    private static Style itemHover(Style style, ItemStack stack) {
        //? if >=26.1 {
        return style.withHoverEvent(new HoverEvent.ShowItem(
                net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack)));
        //?} else if >=1.21.11 {
        /*return style.withHoverEvent(new HoverEvent.ShowItem(stack));
        *///?} else {
        /*return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)));
        *///?}
    }

    /**
     * @return true when the command was one of our chat links and has been dealt with, so the caller must
     *         not let vanilla send it on to the server.
     */
    public static boolean handleClick(String command) {
        if (command == null || !command.startsWith(CLICK_COMMAND)) return false;
        String payload = command.substring(CLICK_COMMAND.length());
        ToolEntry imported = ProfileCodec.decode(payload);
        if (imported == null) {
            TOMBClient.tell(TOMBServer.text("chat.invalid"));
            return true;
        }
        // Straight into the editor rather than into the config: the profile is someone else's idea of how
        // the item should sit, and it is only written to disk if Save is pressed. The chat screen the click
        // came from becomes the parent, so closing the editor lands back where the player was.
        TOMBClient.setScreen(ToolEditorScreen.openWithImported(TOMBClient.currentScreen(), imported));
        return true;
    }
}
