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
//? if >=1.21 {
import net.minecraft.network.chat.contents.PlainTextContents;
//?} else {
/*import net.minecraft.network.chat.contents.LiteralContents;
*///?}
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

public final class ProfileShare {

    public static final String CLICK_COMMAND = "/tomb-profile ";

    private ProfileShare() {
    }

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

    public static Component rewriteComponent(Component source) {
        boolean[] changed = {false};
        MutableComponent out = rewriteNode(source, changed);
        if (changed[0]) return out;

        return rewrite(source.getString());
    }

    private static MutableComponent rewriteNode(Component node, boolean[] changed) {
        MutableComponent result = rewriteContents(node.getContents(), changed);
        if (result == null) result = node.plainCopy();
        result.setStyle(node.getStyle());
        for (Component sibling : node.getSiblings()) result.append(rewriteNode(sibling, changed));
        return result;
    }

    private static String plainText(ComponentContents contents) {
        //? if >=1.21 {
        return contents instanceof PlainTextContents plain ? plain.text() : null;
        //?} else {
        /*return contents instanceof LiteralContents plain ? plain.text() : null;
        *///?}
    }

    private static MutableComponent rewriteContents(ComponentContents contents, boolean[] changed) {
        String plain = plainText(contents);
        if (plain != null) {
            MutableComponent replaced = rewrite(plain);
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

    private static MutableComponent chip(String payload) {
        ToolEntry entry = ProfileCodec.decode(payload);
        ItemStack stack = entry == null ? ItemStack.EMPTY : ConfigHandler.buildStack(entry);

        return TOMBServer.text("chat.chip").withStyle(style -> {
            Style styled = chipStyle(style)
                    //? if >=1.21.5 {
                    .withClickEvent(new ClickEvent.RunCommand(CLICK_COMMAND + payload))
                    //?} else {
                    /*.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, CLICK_COMMAND + payload))
                    *///?}
                    ;
            return stack.isEmpty() ? textHover(styled, TOMBServer.text("chat.chip.unknown")) : itemHover(styled, stack);
        });
    }

    private static Style chipStyle(Style style) {
        return style.withColor(TextColor.fromRgb(0xF0F0D8)).withUnderlined(true);
    }

    private static Style textHover(Style style, Component text) {
        //? if >=1.21.5 {
        return style.withHoverEvent(new HoverEvent.ShowText(text));
        //?} else {
        /*return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
        *///?}
    }

    private static Style itemHover(Style style, ItemStack stack) {
        //? if >=26.1 {
        return style.withHoverEvent(new HoverEvent.ShowItem(
                net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack)));
        //?} else if >=1.21.5 {
        /*return style.withHoverEvent(new HoverEvent.ShowItem(stack));
        *///?} else {
        /*return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)));
        *///?}
    }

    public static boolean handleClick(String command) {
        if (command == null || !command.startsWith(CLICK_COMMAND)) return false;
        String payload = command.substring(CLICK_COMMAND.length());
        ToolEntry imported = ProfileCodec.decode(payload);
        if (imported == null) {
            TOMBClient.tell(TOMBServer.text("chat.invalid"));
            return true;
        }
        TOMBClient.setScreen(ToolEditorScreen.openWithImported(TOMBClient.currentScreen(), imported));
        return true;
    }
}
