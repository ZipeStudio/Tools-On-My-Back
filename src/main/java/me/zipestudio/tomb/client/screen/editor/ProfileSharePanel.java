package me.zipestudio.tomb.client.screen.editor;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.ProfileCodec;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ProfileSharePanel {

    public interface Host {
        ToolEntry selected();

        void notice(Component message);

        void rebuild();

        void applyProfileAsEntry(ToolEntry profile);

        void applyProfileSettings(ToolEntry profile);
    }

    private final Host host;
    private final Minecraft minecraft;
    private final Font font;

    private Button shareButton;
    private Button asEntryButton;
    private Button settingsButton;
    private EditBox pasteBox;

    private String pendingImport;
    private ToolEntry pendingProfile;

    public ProfileSharePanel(Host host, Minecraft minecraft, Font font) {
        this.host = host;
        this.minecraft = minecraft;
        this.font = font;
    }

    public void clear() {
        shareButton = null;
        asEntryButton = null;
        settingsButton = null;
        pasteBox = null;
    }

    public void build(EditorLayout.Column column, Consumer<AbstractWidget> sink, EditorWidgets widgets) {
        if (pendingProfile != null) buildChoice(column, sink);
        else buildShareRows(column, sink, widgets);
    }

    private void buildChoice(EditorLayout.Column column, Consumer<AbstractWidget> sink) {
        EditorLayout.Slot entrySlot = column.full();
        this.asEntryButton = Button.builder(
                        TOMBServer.text("editor.paste.as_entry").withStyle(ChatFormatting.GOLD),
                        ignored -> applyAsEntry())
                .bounds(entrySlot.x(), entrySlot.y(), entrySlot.width(), entrySlot.height()).build();
        sink.accept(EditorWidgets.tip(asEntryButton, TOMBServer.text("editor.paste.as_entry.tooltip")));

        boolean canApply = host.selected() != null;
        EditorLayout.Slot settingsSlot = column.full();
        this.settingsButton = Button.builder(
                        TOMBServer.text("editor.paste.settings_only")
                                .withStyle(canApply ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY),
                        ignored -> applySettings())
                .bounds(settingsSlot.x(), settingsSlot.y(), settingsSlot.width(), settingsSlot.height()).build();
        this.settingsButton.active = canApply;
        sink.accept(EditorWidgets.tip(settingsButton, canApply
                ? TOMBServer.text("editor.paste.settings_only.tooltip")
                : TOMBServer.text("editor.paste.settings_only.tooltip").append(Component.literal("\n"))
                .append(TOMBServer.text("editor.paste.settings_only.no_entry"))));
    }

    private void buildShareRows(EditorLayout.Column column, Consumer<AbstractWidget> sink,
                                EditorWidgets widgets) {
        EditorLayout.Slot shareSlot = column.full();
        this.shareButton = Button.builder(TOMBServer.text("editor.share"), ignored -> copyToClipboard())
                .bounds(shareSlot.x(), shareSlot.y(), shareSlot.width(), shareSlot.height()).build();
        sink.accept(this.shareButton);

        EditorLayout.Slot pasteSlot = column.full();
        this.pasteBox = new EditBox(font, pasteSlot.x(), pasteSlot.y(), pasteSlot.width(), pasteSlot.height(),
                TOMBServer.text("editor.paste"));
        this.pasteBox.setMaxLength(512);
        widgets.hint(this.pasteBox, font, "editor.paste.hint");
        this.pasteBox.setResponder(value -> {
            if (!ProfileCodec.firstToken(value).isEmpty()) pendingImport = value;
        });
        EditorWidgets.tip(this.pasteBox, TOMBServer.text("editor.paste.tooltip"));
        sink.accept(this.pasteBox);
    }

    public void refreshShare(boolean dirty, boolean hasSelection) {
        if (shareButton == null) return;
        shareButton.active = !dirty && hasSelection;
        EditorWidgets.tip(shareButton, dirty
                ? TOMBServer.text("editor.share.tooltip").append(Component.literal("\n"))
                .append(TOMBServer.text("editor.share.needs_save"))
                : TOMBServer.text("editor.share.tooltip"));
    }

    public void tick() {
        if (pendingImport == null) return;
        String text = pendingImport;
        pendingImport = null;
        ToolEntry imported = ProfileCodec.decode(ProfileCodec.firstToken(text));
        if (imported == null) {
            host.notice(TOMBServer.text("editor.paste.invalid"));
            return;
        }
        pendingProfile = imported;
        host.rebuild();
    }

    public void dismissIfClickedAway(double mouseX, double mouseY) {
        if (pendingProfile == null) return;
        if (asEntryButton != null && asEntryButton.isMouseOver(mouseX, mouseY)) return;
        if (settingsButton != null && settingsButton.isMouseOver(mouseX, mouseY)) return;
        pendingProfile = null;
        host.rebuild();
    }

    private void applyAsEntry() {
        ToolEntry profile = pendingProfile;
        pendingProfile = null;
        if (profile != null) host.applyProfileAsEntry(profile);
        host.rebuild();
    }

    private void applySettings() {
        ToolEntry profile = pendingProfile;
        pendingProfile = null;
        if (profile != null) host.applyProfileSettings(profile);
        host.rebuild();
    }

    private void copyToClipboard() {
        ToolEntry current = host.selected();
        if (current == null || minecraft == null) return;
        String payload = ProfileCodec.encode(current);
        minecraft.keyboardHandler.setClipboard(payload);
        host.notice(payload.length() > ProfileCodec.CHAT_LIMIT
                ? TOMBServer.text("editor.share.copied_long", payload.length(), ProfileCodec.CHAT_LIMIT)
                : TOMBServer.text("editor.share.copied"));
    }
}
