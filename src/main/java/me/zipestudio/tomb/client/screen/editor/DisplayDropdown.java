package me.zipestudio.tomb.client.screen.editor;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;

public final class DisplayDropdown {

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

    public boolean open() {
        return open;
    }

    public void toggle() {
        open = !open;
    }

    public void close() {
        open = false;
    }

    public static Component buttonLabel(ToolEntry entry) {
        ItemDisplayContext context = ConfigHandler.parseDisplayContext(entry.getDisplayContext());
        return TOMBServer.text("editor.display").append(Component.literal(": " + context.getSerializedName()));
    }

    public void layout(EditorLayout.Slot button, int screenHeight) {
        int height = ItemDisplayContext.values().length * EditorLayout.DROPDOWN_ROW_HEIGHT + 2;
        this.left = button.x();
        this.right = button.x() + button.width();
        int below = button.y() + button.height();
        if (below + height <= screenHeight) this.top = below;
        else if (button.y() - height >= 0) this.top = button.y() - height;
        else this.top = Math.max(0, screenHeight - height);
        this.bottom = top + height;
    }

    public boolean over(double mouseX, double mouseY) {
        return open && mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    public ItemDisplayContext valueAt(double mouseY) {
        int row = (int) ((mouseY - top - 1) / EditorLayout.DROPDOWN_ROW_HEIGHT);
        ItemDisplayContext[] values = ItemDisplayContext.values();
        return (row >= 0 && row < values.length) ? values[row] : null;
    }

    public void render(EditorPainter painter, ToolEntry entry, int mouseX, int mouseY) {
        if (!open || entry == null) return;
        String current = ConfigHandler.parseDisplayContext(entry.getDisplayContext()).getSerializedName();
        painter.overlayFill(left, top, right, bottom, COLOUR_BACKDROP);
        ItemDisplayContext[] values = ItemDisplayContext.values();
        for (int i = 0; i < values.length; i++) {
            int rowY = top + 1 + i * EditorLayout.DROPDOWN_ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < right
                    && mouseY >= rowY && mouseY < rowY + EditorLayout.DROPDOWN_ROW_HEIGHT;
            boolean chosen = values[i].getSerializedName().equals(current);
            if (hovered) {
                painter.overlayFill(left + 1, rowY, right - 1, rowY + EditorLayout.DROPDOWN_ROW_HEIGHT, COLOUR_HOVERED);
            } else if (chosen) {
                painter.overlayFill(left + 1, rowY, right - 1, rowY + EditorLayout.DROPDOWN_ROW_HEIGHT, COLOUR_CHOSEN);
            }
            painter.text(Component.literal(values[i].getSerializedName()), left + 5, rowY + 2,
                    chosen ? COLOUR_LABEL_CHOSEN : COLOUR_LABEL);
        }
        painter.drawBorder(left, top, right, bottom, COLOUR_BORDER);
    }
}
