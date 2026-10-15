package me.zipestudio.tomb.client.screen.editor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ListDropdown {

    private static final int COLOUR_BACKDROP = 0xF0100010;
    private static final int COLOUR_BORDER = 0xFF808080;
    private static final int COLOUR_HOVERED = 0x66FFFFFF;
    private static final int COLOUR_CHOSEN = 0x33FFFFFF;
    private static final int COLOUR_LABEL = 0xFFB0B0B0;
    private static final int COLOUR_LABEL_CHOSEN = 0xFFFFFFFF;

    private boolean open;
    private int left;
    private int top;
    private int right;
    private int bottom;

    private List<String> values = List.of();
    private String chosen = "";
    private Function<String, Component> labels = Component::literal;
    private Consumer<String> onPick = ignored -> {};
    private AbstractWidget owner;

    public boolean open() {
        return open;
    }

    public void toggle() {
        open = !open;
    }

    public void close() {
        open = false;
    }

    public void layout(AbstractWidget owner, int screenHeight, List<String> values, String chosen,
                       Function<String, Component> labels, Consumer<String> onPick) {
        this.owner = owner;
        this.values = values;
        this.chosen = chosen;
        this.labels = labels;
        this.onPick = onPick;

        int height = values.size() * EditorLayout.DROPDOWN_ROW_HEIGHT + 2;
        this.left = owner.getX();
        this.right = owner.getX() + owner.getWidth();
        int below = owner.getY() + owner.getHeight();
        if (below + height <= screenHeight) this.top = below;
        else if (owner.getY() - height >= 0) this.top = owner.getY() - height;
        else this.top = Math.max(0, screenHeight - height);
        this.bottom = top + height;
    }

    public boolean over(double mouseX, double mouseY) {
        return open && mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return false;
        boolean onList = over(mouseX, mouseY);
        open = false;
        if (button == InputConstants.MOUSE_BUTTON_LEFT && onList) {
            String picked = valueAt(mouseY);
            if (picked != null) {
                chosen = picked;
                onPick.accept(picked);
            }
            return true;
        }
        return owner != null && owner.isMouseOver(mouseX, mouseY);
    }

    private String valueAt(double mouseY) {
        int row = (int) ((mouseY - top - 1) / EditorLayout.DROPDOWN_ROW_HEIGHT);
        return (row >= 0 && row < values.size()) ? values.get(row) : null;
    }

    public void render(EditorPainter painter, int mouseX, int mouseY) {
        if (!open || values.isEmpty()) return;
        painter.overlayFill(left, top, right, bottom, COLOUR_BACKDROP);
        for (int i = 0; i < values.size(); i++) {
            int rowY = top + 1 + i * EditorLayout.DROPDOWN_ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < right
                    && mouseY >= rowY && mouseY < rowY + EditorLayout.DROPDOWN_ROW_HEIGHT;
            boolean picked = values.get(i).equals(chosen);
            if (hovered) {
                painter.overlayFill(left + 1, rowY, right - 1, rowY + EditorLayout.DROPDOWN_ROW_HEIGHT, COLOUR_HOVERED);
            } else if (picked) {
                painter.overlayFill(left + 1, rowY, right - 1, rowY + EditorLayout.DROPDOWN_ROW_HEIGHT, COLOUR_CHOSEN);
            }
            painter.scrollingText(labels.apply(values.get(i)), left + 5, rowY, right - 2,
                    rowY + EditorLayout.DROPDOWN_ROW_HEIGHT, rowY + 2,
                    picked ? COLOUR_LABEL_CHOSEN : COLOUR_LABEL);
        }
        painter.drawBorder(left, top, right, bottom, COLOUR_BORDER);
    }
}
