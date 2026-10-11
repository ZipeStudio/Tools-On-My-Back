package me.zipestudio.tomb.client.screen.editor;

import com.mojang.blaze3d.platform.InputConstants;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class EntryGridPanel {

    public enum Press {
        IGNORED,
        HANDLED,
        REBUILD
    }

    private static final int COLOUR_SELECTED = 0x66FFFFFF;
    private static final int COLOUR_HOVERED = 0x33FFFFFF;
    private static final int COLOUR_TRACK = 0x66000000;
    private static final int COLOUR_THUMB = 0xFFA0A0A0;
    private static final int COLOUR_DISABLED_VEIL = 0x99101010;
    private static final int COLOUR_PLACEHOLDER = 0xFF808080;
    private static final int COLOUR_EMPTY = 0xFFB0B0B0;

    private final EditorSession session;

    private int left;
    private int top;
    private int right;
    private int bottom;
    private int cellsLeft;
    private int cellsTop;
    private int columns = 1;
    private int rows = 1;
    private int scroll;
    private boolean draggingScrollbar;
    private int dragFrom = -1;

    private boolean pickerOpen;
    private final List<ItemStack> pickerStacks = new ArrayList<>();

    private final Map<ToolEntry, ItemStack> iconCache = new IdentityHashMap<>();
    private final Map<ToolEntry, String> iconKey = new IdentityHashMap<>();

    public EntryGridPanel(EditorSession session) {
        this.session = session;
    }

    public boolean pickerOpen() {
        return pickerOpen;
    }

    public void layout(EditorLayout layout, int top, int buttonsTop, EditorPainter painter) {
        iconCache.clear();
        iconKey.clear();

        this.left = layout.leftColumnX;
        this.top = top;
        this.right = layout.leftColumnX + layout.leftWidth;

        int cellsWidth = layout.leftWidth - 2 - EditorLayout.GRID_GUTTER;
        this.columns = Math.max(1, cellsWidth / EditorLayout.CELL_SIZE);
        this.rows = Math.max(1, (buttonsTop - EditorLayout.GROUP_GAP - top - 2) / EditorLayout.CELL_SIZE);
        this.cellsLeft = left + 1 + (cellsWidth - columns * EditorLayout.CELL_SIZE) / 2;
        this.cellsTop = top + 1;
        this.bottom = top + rows * EditorLayout.CELL_SIZE + 2;
        painter.frame(left, top, right, bottom);
        if (!pickerOpen) scrollToSelected();
    }

    public void openPicker(Minecraft minecraft) {
        pickerStacks.clear();
        if (minecraft != null && minecraft.player != null) {
            var inventory = minecraft.player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) continue;
                boolean seen = false;
                for (ItemStack already : pickerStacks) {
                    if (ConfigHandler.areStacksEqual(already, stack)) {
                        seen = true;
                        break;
                    }
                }
                if (!seen) pickerStacks.add(stack);
            }
        }
        scroll = 0;
        pickerOpen = true;
    }

    public void closePicker() {
        pickerOpen = false;
        pickerStacks.clear();
        scroll = 0;
    }

    public Press mousePressed(double mouseX, double mouseY, int button) {
        dragFrom = -1;
        if (button == InputConstants.MOUSE_BUTTON_LEFT && overScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollFromMouse(mouseY);
            return Press.HANDLED;
        }
        draggingScrollbar = false;
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !over(mouseX, mouseY)) return Press.IGNORED;
        int cell = entryAt(mouseX, mouseY);
        if (cell < 0) return Press.IGNORED;
        if (pickerOpen) {
            ItemStack chosen = pickerStacks.get(cell);
            closePicker();
            session.addFromStack(chosen);
            return Press.REBUILD;
        }
        session.select(cell);
        dragFrom = session.index();
        return Press.REBUILD;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (draggingScrollbar) {
            scrollFromMouse(mouseY);
            return true;
        }
        if (dragFrom >= 0 && !pickerOpen) {
            int target = dropTargetAt(mouseX, mouseY);
            if (target >= 0 && target != dragFrom) {
                session.move(dragFrom, target);
                dragFrom = target;
                scrollToSelected();
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!over(mouseX, mouseY)) return false;
        int max = Math.max(0, rowCount() - rows);
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        return true;
    }

    public boolean over(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return rowCount() > rows
                && mouseX >= right - EditorLayout.GRID_GUTTER && mouseX <= right
                && mouseY >= top && mouseY <= bottom;
    }

    private int entryAt(double mouseX, double mouseY) {
        int cell = cellAt(mouseX, mouseY);
        return cell >= 0 && cell < cellCount() ? cell : -1;
    }

    private int dropTargetAt(double mouseX, double mouseY) {
        int cell = cellAt(mouseX, mouseY);
        return cell < 0 ? -1 : Math.min(cell, session.entries().size() - 1);
    }

    private int cellAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - cellsLeft) / EditorLayout.CELL_SIZE);
        int row = (int) Math.floor((mouseY - cellsTop) / EditorLayout.CELL_SIZE);
        if (column < 0 || column >= columns || row < 0 || row >= rows) return -1;
        return (scroll + row) * columns + column;
    }

    private void scrollFromMouse(double mouseY) {
        int total = rowCount();
        int max = Math.max(0, total - rows);
        if (max == 0) {
            scroll = 0;
            return;
        }
        int trackHeight = bottom - top - 2;
        int thumbHeight = Math.max(6, trackHeight * rows / total);
        double span = trackHeight - thumbHeight;
        double offset = mouseY - (top + 1) - thumbHeight / 2.0;
        scroll = span <= 0 ? 0 : Mth.clamp((int) Math.round(offset / span * max), 0, max);
    }

    private void scrollToSelected() {
        int max = Math.max(0, rowCount() - rows);
        int row = session.index() / columns;
        if (row < scroll) scroll = row;
        if (row >= scroll + rows) scroll = row - rows + 1;
        scroll = Mth.clamp(scroll, 0, max);
    }

    private int cellCount() {
        return pickerOpen ? pickerStacks.size() : session.entries().size();
    }

    private int rowCount() {
        return (cellCount() + columns - 1) / columns;
    }

    private ItemStack stackAt(int cell) {
        return pickerOpen ? pickerStacks.get(cell) : icon(session.entries().get(cell));
    }

    private ItemStack icon(ToolEntry entry) {
        String key = entry.getItemId() + "|" + entry.getComponents()
                + entry.isMatchName() + entry.isMatchModelData() + entry.isMatchItemModel();
        if (!key.equals(iconKey.get(entry))) {
            iconKey.put(entry, key);
            iconCache.put(entry, ConfigHandler.buildStack(entry));
        }
        return iconCache.get(entry);
    }

    public Component tooltipFor(int cell) {
        if (pickerOpen) {
            ItemStack stack = pickerStacks.get(cell);
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return itemTooltip(stack, key == null ? "" : key.toString());
        }
        ToolEntry entry = session.entries().get(cell);
        String id = entry.getItemId();
        if (id == null || id.isEmpty()) return TOMBServer.text("editor.entry.empty");
        return itemTooltip(icon(entry), id);
    }

    private static Component itemTooltip(ItemStack stack, String id) {
        MutableComponent line = Component.literal(id == null ? "" : id).withStyle(ChatFormatting.GRAY);
        return stack.isEmpty() ? line
                : stack.getHoverName().copy().append(Component.literal("\n")).append(line);
    }

    public int render(EditorPainter painter, int mouseX, int mouseY) {
        int total = rowCount();
        if (total > rows) {
            int trackX = right - 4;
            int trackHeight = bottom - top - 2;
            painter.fill(trackX, top + 1, right - 1, bottom - 1, COLOUR_TRACK);
            int thumbHeight = Math.max(6, trackHeight * rows / total);
            int thumbY = top + 1 + (trackHeight - thumbHeight) * scroll / (total - rows);
            painter.fill(trackX, thumbY, right - 1, thumbY + thumbHeight, COLOUR_THUMB);
        }
        int hovered = over(mouseX, mouseY) ? entryAt(mouseX, mouseY) : -1;
        if (pickerOpen && pickerStacks.isEmpty()) {
            painter.centeredText(TOMBServer.text("editor.picker.empty"), (left + right) / 2, top + 6, COLOUR_EMPTY);
            return -1;
        }
        painter.scissorOn(left + 1, top + 1, right - 1, bottom - 1);
        int count = cellCount();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int cell = (scroll + row) * columns + column;
                if (cell >= count) {
                    painter.scissorOff();
                    return hovered;
                }
                drawCell(painter, cell, cellsLeft + column * EditorLayout.CELL_SIZE,
                        cellsTop + row * EditorLayout.CELL_SIZE, hovered);
            }
        }
        painter.scissorOff();
        return hovered;
    }

    private void drawCell(EditorPainter painter, int cell, int x, int y, int hovered) {
        int size = EditorLayout.CELL_SIZE;
        if (!pickerOpen && cell == session.index()) painter.fill(x, y, x + size, y + size, COLOUR_SELECTED);
        else if (cell == hovered) painter.fill(x, y, x + size, y + size, COLOUR_HOVERED);

        ItemStack stack = stackAt(cell);
        if (stack.isEmpty()) {
            painter.centeredText(Component.literal("?"), x + size / 2, y + 5, COLOUR_PLACEHOLDER);
        } else {
            painter.item(stack, x + 1, y + 1);
        }
        if (!pickerOpen && !session.entries().get(cell).isEnabled()) {
            painter.overlayFill(x, y, x + size, y + size, COLOUR_DISABLED_VEIL);
        }
    }
}
