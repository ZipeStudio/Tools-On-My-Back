package me.zipestudio.tomb.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.screen.editor.ActionRow;
import me.zipestudio.tomb.client.screen.editor.ListDropdown;
import me.zipestudio.tomb.client.screen.editor.EditorLayout;
import me.zipestudio.tomb.client.screen.editor.EditorLayout.Column;
import me.zipestudio.tomb.client.screen.editor.EditorLayout.Slot;
import me.zipestudio.tomb.client.screen.editor.EditorPainter;
import me.zipestudio.tomb.client.screen.editor.EditorSession;
import me.zipestudio.tomb.client.screen.editor.EditorWidgets;
import me.zipestudio.tomb.client.screen.editor.EntryGridPanel;
import me.zipestudio.tomb.client.screen.editor.EntryProperty;
import me.zipestudio.tomb.client.screen.editor.PreviewPanel;
import me.zipestudio.tomb.client.screen.editor.ProfileSharePanel;
import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.config.ItemDiscriminator;
import me.zipestudio.tomb.config.ToolEntry;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
 *///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public class ToolEditorScreen extends Screen implements EditorWidgets.Host, ProfileSharePanel.Host {

    private static final int FLIP_WIDTH = 90;
    private static final int COLOUR_TITLE = 0xFFFFFFFF;
    private static final List<String> DISPLAY_VALUES = java.util.Arrays.stream(ItemDisplayContext.values())
            .map(ItemDisplayContext::getSerializedName)
            .toList();
    private static final long EXIT_CONFIRM_MS = 3000L;

    private final Screen parent;
    private final EditorSession session;
    private final EditorLayout layout = new EditorLayout();
    private final EditorWidgets widgets = new EditorWidgets(this);
    private final ListDropdown dropdown = new ListDropdown();
    private final ListDropdown scopeDropdown = new ListDropdown();
    private final EntryGridPanel grid;
    private final ActionRow actionRow;
    private EditorPainter painter;
    private PreviewPanel preview;
    private ProfileSharePanel profiles;

    private Button displayButton;
    private Button scopeButton;
    private boolean merged;
    private long exitArmedUntil;

    public ToolEditorScreen(Screen parent) {
        super(TOMBServer.text("editor.title"));
        this.parent = parent;
        this.session = new EditorSession();
        this.grid = new EntryGridPanel(session);
        this.actionRow = new ActionRow(session, this::saveNow, this::onClose, this::cancelChanges);
    }

    public static ToolEditorScreen openWithImported(Screen parent, ToolEntry imported) {
        ToolEditorScreen screen = new ToolEditorScreen(parent);
        screen.addImported(imported);
        return screen;
    }

    public void addImported(ToolEntry imported) {
        this.merged = session.addImported(imported);
    }

    @Override
    protected void init() {
        if (painter == null) painter = new EditorPainter(this.minecraft, this.font);
        if (preview == null) preview = new PreviewPanel(this.minecraft);
        if (profiles == null) profiles = new ProfileSharePanel(this, this.minecraft, this.font);
        painter.clear();
        widgets.clear();
        profiles.clear();
        dropdown.close();
        scopeDropdown.close();
        displayButton = null;

        layout.measure(this.width, this.height, painter);
        layout.titleRules(painter, this.font, this.title);

        buildEntryColumn();
        buildPreview();
        buildPropertyColumn();
        buildBottomRow();

        updatePreview();
    }

    private void buildEntryColumn() {
        boolean picking = grid.pickerOpen();
        Column column = layout.leftColumn(painter);
        column.heading(TOMBServer.text(picking ? "editor.section.picker" : "editor.section.entries"), true);

        int blockHeight = picking
                ? EditorLayout.ROW_HEIGHT
                : 7 * EditorLayout.ROW_HEIGHT + 4 * EditorLayout.ROW_GAP + 2 * (2 * EditorLayout.GROUP_GAP + 1);
        int buttonsTop = layout.frameBottom - EditorLayout.FRAME_PAD - blockHeight;
        grid.layout(layout, column.cursorY(), buttonsTop, painter);
        column.moveTo(buttonsTop);

        if (picking) {
            widgets.button(column.full(), "editor.picker.cancel", this::closePicker);
            return;
        }

        widgets.button(column.full(), "editor.add_item", this::openPicker);
        widgets.button(column.full(), "editor.add_empty", this::addEmpty);

        column.divider();
        Slot enabledSlot = column.full();
        if (session.selected() != null) widgets.flag(enabledSlot, EntryProperty.ENABLED);
        widgets.button(column.full(), "editor.reset_all", this::resetEntry);
        widgets.button(column.full(), "editor.delete", this::removeSelected);

        column.divider();
        profiles.build(column, this::addRenderableWidget, widgets);
    }

    private void buildPreview() {
        preview.layout(layout);
        preview.buildControls(this::addRenderableWidget, this::updatePreview);
    }

    private void buildPropertyColumn() {
        ToolEntry entry = session.selected();
        if (entry == null) return;
        Column column = layout.rightColumn(painter);

        column.heading(TOMBServer.text("editor.section.item"));
        Slot idSlot = column.full();
        EditBox idBox = new EditBox(this.font, idSlot.x(), idSlot.y(), idSlot.width(), idSlot.height(),
                TOMBServer.text("editor.item_id"));
        idBox.setMaxLength(256);
        idBox.setValue(entry.getItemId());
        widgets.hint(idBox, this.font, "editor.item_id.hint");
        EditorWidgets.tip(idBox, TOMBServer.text("editor.item_id.tooltip"));
        idBox.setResponder(value -> {
            ToolEntry current = session.selected();
            if (current == null) return;
            current.setItemId(value);
            current.setComponents("");
            updatePreview();
        });
        addRenderableWidget(idBox);
        buildScopeButton(column.full(), entry);

        column.heading(TOMBServer.text("editor.section.match"));
        Slot[] match = column.thirds(3);
        widgets.flag(match[0], EntryProperty.MATCH_NAME);
        widgets.flag(match[1], EntryProperty.MATCH_MODEL_DATA);
        widgets.flag(match[2], EntryProperty.MATCH_ITEM_MODEL, ItemDiscriminator.supportsItemModel());

        column.splitRule(layout.rightFrameLeft, layout.rightFrameRight);

        column.heading(TOMBServer.text("editor.section.rendering"));
        Slot displaySlot = column.full();
        this.displayButton = Button.builder(displayLabel(entry), ignored -> dropdown.toggle())
                .bounds(displaySlot.x(), displaySlot.y(), displaySlot.width(), displaySlot.height()).build();
        dropdown.layout(this.displayButton, this.height, DISPLAY_VALUES,
                ConfigHandler.parseDisplayContext(entry.getDisplayContext()).getSerializedName(),
                Component::literal, this::pickDisplay);
        widgets.register(this.displayButton, TOMBServer.text("editor.display.tooltip"),
                (from, to) -> to.setDisplayContext(from.getDisplayContext()),
                (from, to) -> Objects.equals(from.getDisplayContext(), to.getDisplayContext()), true);

        Slot[] renderRow = column.split(Math.max(40, column.width() - EditorLayout.GAP - FLIP_WIDTH));
        widgets.slider(renderRow[0], EntryProperty.SCALE);
        widgets.flag(renderRow[1], EntryProperty.FLIP);

        column.heading(TOMBServer.text("editor.section.rotation"));
        Slot[] rotation = column.thirds();
        widgets.slider(rotation[0], EntryProperty.ANGLE_X);
        widgets.slider(rotation[1], EntryProperty.ANGLE_Y);
        widgets.slider(rotation[2], EntryProperty.ANGLE_Z);

        column.heading(TOMBServer.text("editor.section.position"));
        Slot[] position = column.thirds();
        widgets.slider(position[0], EntryProperty.OFFSET_X);
        widgets.slider(position[1], EntryProperty.OFFSET_Y);
        widgets.slider(position[2], EntryProperty.OFFSET_Z);
    }

    private void buildScopeButton(Slot slot, ToolEntry entry) {
        List<String> options = ConfigHandler.scopeOptions(entry);
        String current = options.contains(entry.getScope()) ? entry.getScope() : "";
        this.scopeButton = Button.builder(scopeButtonLabel(current), ignored -> scopeDropdown.toggle())
                .bounds(slot.x(), slot.y(), slot.width(), slot.height()).build();
        scopeDropdown.layout(this.scopeButton, this.height, options, current, ToolEditorScreen::scopeLabel,
                this::pickScope);
        widgets.register(this.scopeButton, TOMBServer.text("editor.apply_to_similar.tooltip"),
                (from, to) -> to.setScope(from.getScope()),
                (from, to) -> Objects.equals(from.getScope(), to.getScope()), true);
    }

    private void pickDisplay(String value) {
        ToolEntry current = session.selected();
        if (current == null) return;
        current.setDisplayContext(value);
        displayButton.setMessage(displayLabel(current));
        updatePreview();
    }

    private void pickScope(String value) {
        ToolEntry current = session.selected();
        if (current == null) return;
        current.setScope(value);
        rebuildWidgets();
    }

    private static Component displayLabel(ToolEntry entry) {
        return TOMBServer.text("editor.display").append(Component.literal(": "
                + ConfigHandler.parseDisplayContext(entry.getDisplayContext()).getSerializedName()));
    }

    private static Component scopeButtonLabel(String scope) {
        return TOMBServer.text("editor.apply_to_similar").append(Component.literal(": "))
                .append(scopeLabel(scope));
    }

    private static Component scopeLabel(String scope) {
        if (scope.isEmpty()) return TOMBServer.text("editor.scope.item");
        if (scope.equals(ConfigHandler.SCOPE_SIMILAR)) return TOMBServer.text("editor.scope.similar");
        return Component.literal(scope);
    }

    private void buildBottomRow() {
        widgets.button(new Slot(layout.leftFrameLeft, layout.bottomRowY,
                        layout.leftFrameRight - layout.leftFrameLeft, EditorLayout.ROW_HEIGHT),
                "editor.settings", () -> TOMBClient.setScreen(new ModSettingsScreen(this, session::saveGlobals)));
        actionRow.build(layout, this::addRenderableWidget);
        refreshShare();
    }

    @Override
    public ToolEntry selected() {
        return session.selected();
    }

    @Override
    public ToolEntry savedReference() {
        return session.savedOrDefaults(session.selected());
    }

    @Override
    public ToolEntry defaultsReference() {
        return session.defaultsFor(session.selected());
    }

    @Override
    public void previewChanged() {
        updatePreview();
    }

    @Override
    public void addWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    @Override
    public void rebuild() {
        rebuildWidgets();
    }

    @Override
    public void notice(Component message) {
        TOMBClient.toast(TOMBServer.text("name"), message);
    }

    @Override
    public void applyProfileAsEntry(ToolEntry profile) {
        addImported(profile);
        notice(merged ? TOMBServer.text("editor.paste.merged") : TOMBServer.text("editor.paste.added"));
    }

    @Override
    public void applyProfileSettings(ToolEntry profile) {
        ToolEntry current = session.selected();
        if (current == null) return;
        EntryProperty.copyLook(profile, current);
        updatePreview();
        notice(TOMBServer.text("editor.paste.applied"));
    }

    private void openPicker() {
        dropdown.close();
        grid.openPicker(this.minecraft);
        rebuildWidgets();
    }

    private void closePicker() {
        grid.closePicker();
        rebuildWidgets();
    }

    private void addEmpty() {
        session.addEmpty();
        rebuildWidgets();
    }

    private void removeSelected() {
        session.removeSelected();
        rebuildWidgets();
    }

    private void resetEntry() {
        widgets.resetEntry();
        rebuildWidgets();
    }

    private void saveNow() {
        session.save();
        notice(TOMBServer.text("editor.saved"));
        refreshActionRow();
    }

    private void cancelChanges() {
        session.cancel();
        notice(TOMBServer.text("editor.cancel_changes.done"));
        rebuildWidgets();
    }

    private void updatePreview() {
        if (minecraft == null || minecraft.player == null) return;
        ToolEntry entry = session.selected();
        if (entry == null) {
            TOMBClient.PREVIEW_PLAYER = null;
            TOMBClient.PREVIEW_STACK = ItemStack.EMPTY;
            TOMBClient.PREVIEW_ENTRY = null;
            return;
        }
        TOMBClient.PREVIEW_ENTRY = entry;
        TOMBClient.PREVIEW_PLAYER = preview.previewUuid();
        TOMBClient.PREVIEW_STACK = ConfigHandler.buildStack(entry);
    }

    private void refreshActionRow() {
        if (actionRow.refresh()) refreshShare();
    }

    private void refreshShare() {
        profiles.refreshShare(session.dirty(), session.selected() != null);
    }

    @Override
    public void tick() {
        super.tick();
        refreshActionRow();
        profiles.tick();
    }

    private boolean handleClick(double mouseX, double mouseY, int button) {
        setFocused(null);
        profiles.dismissIfClickedAway(mouseX, mouseY);
        if (dropdown.mouseClicked(mouseX, mouseY, button)) return true;
        if (scopeDropdown.mouseClicked(mouseX, mouseY, button)) return true;
        switch (grid.mousePressed(mouseX, mouseY, button)) {
            case HANDLED -> {
                return true;
            }
            case REBUILD -> {
                rebuildWidgets();
                return true;
            }
            default -> {
            }
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && preview.overModel(mouseX, mouseY)) {
            preview.resetCamera();
            return true;
        }
        if (actionRow.handleClick(mouseX, mouseY, button)) return true;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && widgets.restoreSaved(mouseX, mouseY)) {
            rebuildWidgets();
            return true;
        }
        return false;
    }

    private boolean handleScroll(double mouseX, double mouseY, double scrollY) {
        if (dropdown.over(mouseX, mouseY) || scopeDropdown.over(mouseX, mouseY)) return true;
        if (grid.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (preview.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return widgets.nudgeSlider(mouseX, mouseY, scrollY);
    }

    private boolean handleDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (grid.mouseDragged(mouseX, mouseY, button)) return true;
        return button == InputConstants.MOUSE_BUTTON_LEFT && preview.mouseDragged(mouseX, mouseY, dragX, dragY);
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (handleClick(event.x(), event.y(), event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (handleDrag(event.x(), event.y(), event.button(), dx, dy)) return true;
        return super.mouseDragged(event, dx, dy);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleClick(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handleDrag(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    *///?}

    //? if >=1.21 {
    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (handleScroll(x, y, scrollY)) return true;
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (handleScroll(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    *///?}

    private boolean hoverBlocked(int mouseX, int mouseY) {
        return dropdown.over(mouseX, mouseY) || scopeDropdown.over(mouseX, mouseY);
    }

    private void drawForeground(int mouseX, int mouseY, int hoverX, int hoverY, float partialTick) {
        painter.centeredText(this.title, layout.titleCenterX(), layout.titleY(), COLOUR_TITLE);
        preview.drawGlass(painter);
        painter.drawRules();
        painter.drawHeadings();
        widgets.drawHints(painter, this.font);
        int hoveredCell = grid.render(painter, hoverX, hoverY);
        preview.render(painter, partialTick);
        painter.drawFrames();
        if (preview.unavailable()) preview.drawNotice(painter);
        dropdown.render(painter, mouseX, mouseY);
        scopeDropdown.render(painter, mouseX, mouseY);

        if (preview.hintDue(hoverX, hoverY)) {
            painter.tooltip(TOMBServer.text("editor.hint.drag"), mouseX, mouseY);
        } else if (hoveredCell >= 0) {
            painter.tooltip(grid.tooltipFor(hoveredCell), mouseX, mouseY);
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean blocked = hoverBlocked(mouseX, mouseY);
        int hoverX = blocked ? -1 : mouseX;
        int hoverY = blocked ? -1 : mouseY;

        painter.target(graphics);
        painter.drawBackdrops();
        super.extractRenderState(graphics, hoverX, hoverY, a);
        painter.target(graphics);
        drawForeground(mouseX, mouseY, hoverX, hoverY, a);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean blocked = hoverBlocked(mouseX, mouseY);
        int hoverX = blocked ? -1 : mouseX;
        int hoverY = blocked ? -1 : mouseY;

        painter.target(guiGraphics);
        //? if <1.21 {
        /^renderBackground(guiGraphics);
        ^///?}
        super.render(guiGraphics, hoverX, hoverY, partialTick);
        painter.target(guiGraphics);
        drawForeground(mouseX, mouseY, hoverX, hoverY, partialTick);
    }

    //? if >=1.21 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        painter.target(guiGraphics);
        painter.drawBackdrops();
    }
    //?} else {
    /^@Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        painter.target(guiGraphics);
        painter.drawBackdrops();
    }
    ^///?}
    *///?}

    @Override
    public void onClose() {
        if (session.dirty() && System.currentTimeMillis() >= exitArmedUntil) {
            exitArmedUntil = System.currentTimeMillis() + EXIT_CONFIRM_MS;
            notice(TOMBServer.text("editor.exit.unsaved"));
            return;
        }
        exitArmedUntil = 0L;
        session.discardUnsaved();
        if (minecraft != null) TOMBClient.setScreen(parent);
        else super.onClose();
    }

    @Override
    public void removed() {
        TOMBClient.PREVIEW_PLAYER = null;
        TOMBClient.PREVIEW_STACK = ItemStack.EMPTY;
        TOMBClient.PREVIEW_ENTRY = null;
        TOMBClient.PREVIEW_DUMMY = null;
        if (preview != null) preview.forget();
    }
}
