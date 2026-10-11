package me.zipestudio.tomb.client.screen.editor;

import com.mojang.blaze3d.platform.InputConstants;
import me.zipestudio.tomb.TOMBServer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ActionRow {

    private static final long CANCEL_CONFIRM_MS = 2000L;

    private final EditorSession session;
    private final Runnable onSave;
    private final Runnable onDone;
    private final Runnable onCancelChanges;

    private Button saveButton;
    private Button cancelButton;
    private boolean lastDirty;
    private boolean lastArmed;
    private long armedUntil;

    public ActionRow(EditorSession session, Runnable onSave, Runnable onDone, Runnable onCancelChanges) {
        this.session = session;
        this.onSave = onSave;
        this.onDone = onDone;
        this.onCancelChanges = onCancelChanges;
    }

    public void build(EditorLayout layout, Consumer<AbstractWidget> sink) {
        int width = (layout.rightFrameRight - layout.rightFrameLeft - EditorLayout.GAP) / 2;
        this.saveButton = Button.builder(TOMBServer.text("editor.save"), ignored -> {
            if (session.dirty()) onSave.run();
            else onDone.run();
        }).bounds(layout.rightFrameLeft, layout.bottomRowY, width, EditorLayout.ROW_HEIGHT).build();
        sink.accept(this.saveButton);

        this.cancelButton = Button.builder(TOMBServer.text("editor.cancel_changes"), ignored -> {
            if (armed()) {
                armedUntil = 0L;
                onCancelChanges.run();
            } else {
                armedUntil = System.currentTimeMillis() + CANCEL_CONFIRM_MS;
                refresh();
            }
        }).bounds(layout.rightFrameRight - width, layout.bottomRowY, width, EditorLayout.ROW_HEIGHT).build();
        sink.accept(this.cancelButton);
        forceRefresh();
    }

    private boolean armed() {
        return System.currentTimeMillis() < armedUntil;
    }

    private void forceRefresh() {
        lastDirty = !session.dirty();
        lastArmed = !armed();
        refresh();
    }

    public boolean refresh() {
        if (saveButton == null || cancelButton == null) return false;
        boolean dirty = session.dirty();
        boolean armed = armed();
        if (dirty == lastDirty && armed == lastArmed) return false;
        lastDirty = dirty;
        lastArmed = armed;
        saveButton.setMessage(dirty
                ? TOMBServer.text("editor.save").withStyle(ChatFormatting.GREEN)
                : TOMBServer.text("editor.done"));
        EditorWidgets.tip(saveButton, TOMBServer.text(dirty ? "editor.save.tooltip" : "editor.done.tooltip"));
        cancelButton.active = dirty;
        cancelButton.setMessage(armed
                ? TOMBServer.text("editor.cancel_changes.confirm").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                : TOMBServer.text("editor.cancel_changes").withStyle(ChatFormatting.RED));
        EditorWidgets.tip(cancelButton,
                TOMBServer.text(armed ? "editor.cancel_changes.confirm.tooltip" : "editor.cancel_changes.tooltip")
                        .append(Component.literal("\n")).append(TOMBServer.text("editor.cancel_changes.instant")));
        return true;
    }

    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (cancelButton != null && cancelButton.isMouseOver(mouseX, mouseY)) {
            if (button == InputConstants.MOUSE_BUTTON_RIGHT && cancelButton.active) {
                armedUntil = 0L;
                onCancelChanges.run();
                return true;
            }
            return false;
        }
        if (armed()) {
            armedUntil = 0L;
            refresh();
        }
        return false;
    }
}
