package me.zipestudio.tomb.client.screen.editor;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.screen.editor.EditorLayout.Slot;
import me.zipestudio.tomb.client.screen.editor.EntryProperty.FlagProperty;
import me.zipestudio.tomb.client.screen.editor.EntryProperty.FloatProperty;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EditorWidgets {

    public static final long HOVER_DELAY_MS = 500L;
    private static final Duration TOOLTIP_DELAY = Duration.ofMillis(HOVER_DELAY_MS);
    private static final String REVERT_KEY = "editor.hint.revert";
    private static final String UNSUPPORTED_KEY = "editor.hint.unsupported";
    private static final int COLOUR_HINT = 0xFF555555;

    public interface Host {
        ToolEntry selected();

        ToolEntry savedReference();

        ToolEntry defaultsReference();

        void previewChanged();

        void addWidget(AbstractWidget widget);
    }

    private final Host host;
    private final List<ResetTarget> resetTargets = new ArrayList<>();
    private final List<FloatSlider> sliders = new ArrayList<>();
    private final List<Hint> hints = new ArrayList<>();

    public EditorWidgets(Host host) {
        this.host = host;
    }

    public void clear() {
        resetTargets.clear();
        sliders.clear();
        hints.clear();
    }

    public void hint(EditBox box, Font font, String key) {
        Component text = TOMBServer.text(key).withStyle(ChatFormatting.DARK_GRAY);
        if (font.width(text) <= box.getInnerWidth()) {
            box.setHint(text);
            return;
        }
        hints.add(new Hint(box, text));
    }

    public void drawHints(EditorPainter painter, Font font) {
        for (Hint hint : hints) {
            EditBox box = hint.box();
            if (!box.getValue().isEmpty() || !box.visible) continue;
            int left = box.getX() + 4;
            int right = left + box.getInnerWidth();
            int top = box.getY() + (box.getHeight() - 8) / 2;
            painter.scrollingText(hint.text(), font, left, box.getY(), right,
                    box.getY() + box.getHeight(), top, COLOUR_HINT);
        }
    }

    public static <T extends AbstractWidget> T tip(T widget, Component text) {
        widget.setTooltip(Tooltip.create(text));
        //? if >=1.21 {
        widget.setTooltipDelay(TOOLTIP_DELAY);
        //?} else {
        /*widget.setTooltipDelay((int) HOVER_DELAY_MS);
        *///?}
        return widget;
    }

    public Button button(Slot slot, String key, Runnable action) {
        Button widget = Button.builder(TOMBServer.text(key), ignored -> action.run())
                .bounds(slot.x(), slot.y(), slot.width(), slot.height())
                .build();
        tip(widget, TOMBServer.text(key + ".tooltip"));
        host.addWidget(widget);
        return widget;
    }

    public AbstractWidget flag(Slot slot, FlagProperty property) {
        return flag(slot, property, true);
    }

    public AbstractWidget flag(Slot slot, FlagProperty property, boolean available) {
        ToolEntry entry = host.selected();
        AbstractWidget widget = CycleButton.onOffBuilder(entry != null && property.getter().get(entry))
                .create(slot.x(), slot.y(), slot.width(), slot.height(), property.label(),
                        (button, value) -> {
                            ToolEntry current = host.selected();
                            if (current == null) return;
                            property.setter().set(current, value);
                            host.previewChanged();
                        });
        if (available) {
            register(widget, property.tooltip(), property::copy, property::same, property.inResetEntry());
        } else {
            widget.active = false;
            tip(widget, property.tooltip().append(Component.literal("\n"))
                    .append(TOMBServer.text(UNSUPPORTED_KEY)));
            host.addWidget(widget);
        }
        return widget;
    }

    public FloatSlider slider(Slot slot, FloatProperty property) {
        FloatSlider widget = new FloatSlider(slot, property, host::selected, host::previewChanged);
        register(widget, property.tooltip(), property::copy, property::same, true);
        sliders.add(widget);
        return widget;
    }

    public static AbstractWidget plainFlag(Slot slot, String key, boolean initial, BoolWriter writer) {
        AbstractWidget widget = CycleButton.onOffBuilder(initial)
                .create(slot.x(), slot.y(), slot.width(), slot.height(), TOMBServer.text(key),
                        (button, value) -> writer.write(value));
        return tip(widget, TOMBServer.text(key + ".tooltip"));
    }

    public static <T> CycleButton<T> cycle(Slot slot, String key, T[] values, T initial,
                                           Function<T, Component> name, Consumer<T> onChange) {
        return
                //? if >=1.21.11 {
                CycleButton.builder(name, initial)
                        .withValues(values)
                        //?} else {
                        /*CycleButton.<T>builder(name)
                                .withValues(values)
                                .withInitialValue(initial)
                        *///?}
                        .create(slot.x(), slot.y(), slot.width(), slot.height(), TOMBServer.text(key),
                                (button, value) -> onChange.accept(value));
    }

    public void register(AbstractWidget widget, MutableComponent tooltip,
                         BiConsumer<ToolEntry, ToolEntry> restore,
                         BiPredicate<ToolEntry, ToolEntry> same, boolean inResetEntry) {
        tip(widget, tooltip.append(Component.literal("\n")).append(TOMBServer.text(REVERT_KEY)));
        host.addWidget(widget);
        resetTargets.add(new ResetTarget(widget, restore, same, inResetEntry));
    }

    public boolean restoreSaved(double mouseX, double mouseY) {
        for (ResetTarget target : resetTargets) {
            if (!target.widget().isMouseOver(mouseX, mouseY)) continue;
            ToolEntry saved = host.savedReference();
            ToolEntry current = host.selected();
            boolean atSaved = saved != null && current != null && target.same().test(saved, current);
            apply(target, atSaved ? host.defaultsReference() : saved);
            return true;
        }
        return false;
    }

    public void resetEntry() {
        for (ResetTarget target : resetTargets) {
            if (target.inResetEntry()) apply(target, host.defaultsReference());
        }
    }

    public boolean nudgeSlider(double mouseX, double mouseY, double scrollY) {
        for (FloatSlider slider : sliders) {
            if (slider.isMouseOver(mouseX, mouseY)) {
                slider.nudge(scrollY);
                return true;
            }
        }
        return false;
    }

    private void apply(ResetTarget target, ToolEntry reference) {
        ToolEntry current = host.selected();
        if (current == null || reference == null) return;
        target.restore().accept(reference, current);
        host.previewChanged();
    }

    public interface BoolWriter {
        void write(boolean value);
    }

    private record Hint(EditBox box, Component text) {
    }

    private record ResetTarget(AbstractWidget widget, BiConsumer<ToolEntry, ToolEntry> restore,
                               BiPredicate<ToolEntry, ToolEntry> same, boolean inResetEntry) {
    }
}
