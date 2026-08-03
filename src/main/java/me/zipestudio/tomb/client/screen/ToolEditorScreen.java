package me.zipestudio.tomb.client.screen;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.config.ConfigHandler;
import me.zipestudio.tomb.config.LeafyConfig;
import me.zipestudio.tomb.config.ProfileCodec;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.ChatFormatting;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
 *///?}
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.authlib.GameProfile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * In-game editor for per-item back-tool overrides. Edits the live config so the 3D preview (local
 * player rendered with the back-tool layer forced to the edited item) reflects changes instantly.
 * Done saves; Cancel/Esc restore the snapshot taken on open. Controls are explained by hover tooltips.
 */
public class ToolEditorScreen extends Screen {

    private static final int LEFT_X = 10;
    private static final int LEFT_W = 110;
    private static final int RIGHT_W = 190;
    private static final int ROW_H = 18;
    private static final int GAP = 4;
    private static final int STEP = ROW_H + 2;
    /**
     * Breathing room between a column's groups: what it targets, how it draws, where it sits.
     */
    private static final int GROUP_GAP = 5;
    /**
     * A group heading plus the air under it, before the first row of that group.
     */
    private static final int HEAD_H = 11;
    /**
     * Air between a framed panel's border and the widgets inside it.
     */
    private static final int FRAME_PAD = 5;
    /**
     * Air either side of the rule cutting the right panel in two.
     */
    private static final int SPLIT_GAP = 7;
    /**
     * One cell of the entry grid: a 16px item icon with a pixel of air on each side.
     */
    private static final int ENTRY_CELL = 18;
    /**
     * How wide the entry grid keeps its scrollbar gutter — reserved always, so the columns never shift.
     */
    private static final int LIST_GUTTER = 6;
    /**
     * One line of the Display dropdown: the vanilla font plus a little air.
     */
    private static final int DROP_ROW_H = 12;
    /**
     * How wide a tooltip this screen draws itself may run before it wraps.
     */
    private static final int TOOLTIP_W = 200;
    /**
     * The radius inside which vanilla draws a name tag, taken straight from
     * {@code LivingEntityRenderer.shouldShowName}: it compares the SQUARED distance against
     * {@code (isDiscrete() ? 32 : 64)} squared. Only the pre-1.21.9 render path ever asks.
     */
    private static final double NAME_TAG_RANGE = 64.0;
    /**
     * One row of the small controls: the preview trio over the doll, the mod switches under it.
     */
    private static final int OVERLAY_H = 12;
    /**
     * Width of the preview trio, which floats over the model rather than spanning the panel.
     */
    private static final int OVERLAY_W = 66;
    /**
     * How far the trio sits from the corner of the model box it is tucked into: one pixel, i.e. flush
     * against the inside of the panel's 1px border.
     */
    private static final int OVERLAY_PAD = 1;

    // Widgets have fixed pixel sizes, so a layout spread over the whole window keeps growing its gaps and its
    // preview panel as the GUI area grows: at GUI Scale Auto (~480×270 units) that looks right, but at a low
    // fixed scale (e.g. 2 on a 1080p screen → 960×526 units) it balloons. So the editor is laid out inside a
    // content box capped to these and centred. The caps sit just above the Auto-sized GUI area on purpose —
    // at Auto nothing is capped and the layout is exactly the full-window one, only lower scales get reined in.
    private static final int MAX_W = 560;
    private static final int MAX_H = 280;

    private static final String REVERT_KEY = "editor.hint.revert";
    private static final String NO_WORLD_KEY = "editor.hint.no_world";
    /**
     * How long the cursor has to rest before an explanation appears — on a widget, and on the preview.
     */
    private static final long HOVER_DELAY_MS = 500L;
    private static final Duration TOOLTIP_DELAY = Duration.ofMillis(HOVER_DELAY_MS);
    /**
     * How long the cancel button stays armed after the first click before it goes back to asking.
     */
    private static final long CANCEL_CONFIRM_MS = 2000L;

    private final Screen parent;
    private final LeafyConfig config;
    /**
     * The entries as they are on disk. Replaced on every save, so it is always "the last saved state".
     */
    private List<ToolEntry> snapshot;
    /**
     * The same for the three mod-wide options, which the editor now owns as well.
     */
    private boolean savedEnableMod, savedRenderWithCape, savedRenderWithElytra;

    private int index = 0;
    private EditBox idBox;
    private Button saveButton;
    private Button cancelButton;
    private Button shareButton;
    /**
     * What the bottom row was last told to say, so its labels are only rebuilt when they change.
     */
    private boolean lastDirty;
    private boolean lastCancelArmed;
    /**
     * While this is in the future, cancelling is one click away; otherwise the first click only arms it.
     */
    private long cancelArmedUntil;
    /**
     * Whether the last import landed on an existing entry rather than making a new one.
     */
    private boolean merged = false;
    /**
     * Text waiting to be imported, handed over by the paste box for the next tick to deal with.
     */
    private String pendingImport;
    private EditBox pasteBox;
    /**
     * A profile that has been read and is waiting to be told what it is for: a whole entry, or just the
     * numbers that say how the item is drawn. While this is set the share/paste rows show the two choices
     * instead. Only the editor asks — a profile clicked in chat still lands as an entry, as it always did.
     */
    private ToolEntry pendingProfile;
    private Button importAsEntryButton;
    private Button importSettingsButton;

    // centred content box (see MAX_W/MAX_H); every widget and the preview panel are placed relative to it
    private int guiLeft, guiTop, contentW, contentH;

    private Button poseButton;
    /**
     * The three controls that float over the doll. They sit inside the preview panel, so without knowing
     * where they are the panel's own hover hint comes up over them — which is not what the cursor is
     * pointing at.
     */
    private final List<AbstractWidget> overlayControls = new ArrayList<>();

    // the entry grid: its box, how many cells fit in it, and the first row of cells drawn in it
    private int lsx1, lsy1, lsx2, lsy2;
    /**
     * Left edge of the cells themselves — the box inset by its border and the leftover half-column.
     */
    private int gridX, gridY;
    private int listCols = 1;
    private int listRows = 1;
    /**
     * First visible ROW of cells, not the first entry: the grid scrolls a whole row at a time.
     */
    private int listScroll = 0;
    /**
     * Set while the scrollbar is being dragged; cleared by the next press that starts anywhere else.
     */
    private boolean draggingScroll;
    /**
     * Which entry a left-press landed on, so dragging can carry it to another cell. -1 when no press is in
     * flight. Cleared by the next press, the same trick {@link #draggingScroll} uses — committing on
     * release would need a {@code mouseReleased} override, and that one splits at 1.21.11 too.
     */
    private int dragEntryFrom = -1;

    // The Display value is picked from a drop-down rather than cycled: with ten contexts, clicking through
    // them to reach the one you want is the whole interaction. The list is not a widget — it has to paint
    // over the widgets under it, so it is drawn at the end of the screen's own render and hit-tested first.
    private Button displayButton;
    private boolean displayOpen;
    private int ddx1, ddy1, ddx2, ddy2;

    // The item picker: everything the player is carrying, as a grid, so configuring an item no longer means
    // juggling it into the hotbar first. Drawn and hit-tested exactly like the Display list.
    private boolean pickerOpen;
    /**
     * What the grid shows while picking: one cell per distinct stack carried, in inventory order.
     */
    private final List<ItemStack> pickerStacks = new ArrayList<>();

    /**
     * The stack each entry stands for, so the grid does not rebuild (and re-parse) one per cell per frame.
     */
    private final Map<ToolEntry, ItemStack> iconCache = new IdentityHashMap<>();
    /**
     * What each cached icon was built from, so editing an id in place still refreshes it.
     */
    private final Map<ToolEntry, String> iconKey = new IdentityHashMap<>();

    // the middle panel: pv* is the whole framed box, mvy2 the bottom of the part the model is drawn in —
    // the pose/skin/cape controls sit under it, inside the same frame
    private int pvx1, pvy1, pvx2, pvy2;
    private int mvy2;
    private float previewYaw = 0f;    // full 360° turntable; 0 = back to camera
    private float previewPitch = 0f;  // clamped to a small tilt so the model never clips
    /**
     * Wheel zoom over the panel, multiplying the fitted scale. 1 = the doll as the box sizes it.
     */
    private float previewZoom = 1f;
    private static final float ZOOM_MIN = 0.6f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.1f;

    // the preview panel is not a widget, so its hover delay is timed by hand (0 = not hovered)
    private long previewHoverSince = 0L;

    private PreviewPose previewPose = PreviewPose.STANDING;
    private PreviewSubject previewSubject = PreviewSubject.YOU;
    private boolean showCape = true;

    // a throw-away stand-in rendered instead of the real player, so poses can be forced freely
    private RemotePlayer dummy;

    /**
     * Live entry → the copy of it that is on disk, so right-click can put back what was saved.
     */
    private final Map<ToolEntry, ToolEntry> savedByEntry = new IdentityHashMap<>();

    private final List<ResetTarget> resettables = new ArrayList<>();
    private final List<FloatSlider> sliders = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();
    private final List<Frame> frames = new ArrayList<>();
    private final List<Rule> rules = new ArrayList<>();

    public ToolEditorScreen(Screen parent) {
        super(TOMBServer.text("editor.title"));
        this.parent = parent;
        this.config = LeafyConfig.getInstance();
        this.snapshot = deepCopy(config.getTools());
        // Pair every live entry with its saved copy, by identity rather than by position: right-click
        // restores what is on disk, and the two lists drift apart as soon as anything is added or removed.
        // The live objects survive the list swap below, so they stay valid keys for the whole session.
        // codec-decoded list is immutable; swap in a mutable copy so add/remove work while editing
        config.setTools(new ArrayList<>(config.getTools()));
        pairWithSaved();
        snapshotGlobals();
        if (index >= config.getTools().size()) index = config.getTools().size() - 1;
    }

    private List<ToolEntry> tools() {
        return config.getTools();
    }

    private ToolEntry selected() {
        List<ToolEntry> t = tools();
        return (index >= 0 && index < t.size()) ? t.get(index) : null;
    }

    @Override
    protected void init() {
        resettables.clear();
        sliders.clear();
        sections.clear();
        frames.clear();
        rules.clear();
        overlayControls.clear();
        // the icons are keyed by entry identity, and entries come and go with every add, delete or cancel
        iconCache.clear();
        iconKey.clear();
        // a list left hanging open over widgets that are about to be rebuilt has nothing to point at, and
        // with no entry selected there is no button below to rebuild it either
        displayOpen = false;
        displayButton = null;
        // pickerOpen is deliberately NOT reset here: it is a mode of the panel, not a pop-up, and this
        // method is what draws that mode — openPicker sets the flag and then rebuilds into it. Leaving the
        // mode is closePicker's job, or picking an item.
        // rebuilt below, one pair or the other — never both, and never a stale reference to a dead widget
        shareButton = null;
        pasteBox = null;
        importAsEntryButton = null;
        importSettingsButton = null;

        this.contentW = Math.min(this.width, MAX_W);
        this.contentH = Math.min(this.height, MAX_H);
        this.guiLeft = (this.width - contentW) / 2;
        this.guiTop = (this.height - contentH) / 2;

        int lx = guiLeft + LEFT_X;
        int right = guiLeft + contentW - RIGHT_W - LEFT_X;
        int subW = (RIGHT_W - 2 * GAP) / 3;
        // integer division loses a pixel or two, so the last of a three-across row takes the remainder —
        // otherwise these rows end up narrower than the full-width ones above them
        int lastW = RIGHT_W - 2 * (subW + GAP);

        // ---- three framed panels, filling the box between the title and the bottom row ----
        int by = guiTop + contentH - 20;
        int frameTop = guiTop + 20;
        int frameBottom = by - 5;
        panel(lx - FRAME_PAD, frameTop, lx + LEFT_W + FRAME_PAD, frameBottom);
        int rightFrameX1 = right - FRAME_PAD;
        int rightFrameX2 = right + RIGHT_W + FRAME_PAD;
        panel(rightFrameX1, frameTop, rightFrameX2, frameBottom);
        // the middle panel takes whatever is left between the two columns
        this.pvx1 = lx + LEFT_W + FRAME_PAD + 8;
        this.pvy1 = frameTop;
        this.pvx2 = right - FRAME_PAD - 8;
        this.pvy2 = frameBottom;
        // Border only here. This one panel paints its backdrop AFTER its widgets — see the note at that
        // call — so it must not also get one from the shared pass.
        frame(pvx1, pvy1, pvx2, pvy2);

        // the title sits on a rule spanning the full width, broken around the text
        titleRules();

        // ---- left column: the entry grid, with what makes entries and what changes the selected one.
        // While an item is being picked the same panel shows the inventory instead, under its own heading
        // and over a single Cancel button — a mode of the panel rather than a window floating over the
        // screen. That is what keeps it out of the preview: a pop-up centred on the doll was drawing its
        // backdrop behind her on 1.21.1, because there the GUI is immediate-mode and the entity goes down
        // at z=150. Nothing here overlaps anything, so the question cannot come back.
        int y = head(lx, frameTop + FRAME_PAD, LEFT_W,
                TOMBServer.text(pickerOpen ? "editor.section.picker" : "editor.section.entries"), true);

        // The buttons are anchored to the bottom of the panel and the grid takes whatever is left, so a
        // taller window grows the grid rather than the gaps. Three groups: what makes entries, what
        // happens to the selected one, and what carries it to another player. Picking collapses all of
        // that to one row, which is also what gives the inventory more of the panel to spread into.
        int blockH = pickerOpen ? ROW_H : 7 * ROW_H + 4 * (STEP - ROW_H) + 2 * (2 * GROUP_GAP + 1);
        int buttonsY = frameBottom - FRAME_PAD - blockH;

        this.lsx1 = lx;
        this.lsy1 = y;
        this.lsx2 = lx + LEFT_W;
        // The gutter is reserved whether or not the scrollbar is showing, so adding the entry that makes the
        // grid overflow does not shunt every icon a column to the left.
        int cellsW = LEFT_W - 2 - LIST_GUTTER;
        this.listCols = Math.max(1, cellsW / ENTRY_CELL);
        this.listRows = Math.max(1, (buttonsY - GROUP_GAP - lsy1 - 2) / ENTRY_CELL);
        this.gridX = lsx1 + 1 + (cellsW - listCols * ENTRY_CELL) / 2;
        this.gridY = lsy1 + 1;
        this.lsy2 = lsy1 + listRows * ENTRY_CELL + 2;
        frame(lsx1, lsy1, lsx2, lsy2);
        if (!pickerOpen) scrollToSelected();

        y = buttonsY;
        ToolEntry e = selected();
        if (pickerOpen) {
            addRenderableWidget(tip(Button.builder(TOMBServer.text("editor.picker.cancel"), b -> closePicker())
                    .bounds(lx, y, LEFT_W, ROW_H).build(), TOMBServer.text("editor.picker.cancel.tooltip")));
        } else {
            addRenderableWidget(tip(Button.builder(TOMBServer.text("editor.add_item"), b -> openPicker())
                    .bounds(lx, y, LEFT_W, ROW_H).build(), TOMBServer.text("editor.add_item.tooltip")));
            y += STEP;
            addRenderableWidget(tip(Button.builder(TOMBServer.text("editor.add_empty"), b -> addEmpty())
                    .bounds(lx, y, LEFT_W, ROW_H).build(), TOMBServer.text("editor.add_empty.tooltip")));

            // below the rule: what happens to the entry that is selected up there
            y = divider(lx, y + ROW_H, LEFT_W);
            if (e != null) {
                AbstractWidget wEnabled = CycleButton.onOffBuilder(e.isEnabled())
                        .create(lx, y, LEFT_W, ROW_H, TOMBServer.text("editor.enabled"),
                                (btn, val) -> {
                                    ToolEntry c = selected();
                                    if (c != null) {
                                        c.setEnabled(val);
                                        updatePreview();
                                    }
                                });
                addProp(wEnabled, TOMBServer.text("editor.enabled.tooltip"),
                        (c, ref) -> c.setEnabled(ref.isEnabled()));
            }
            y += STEP;
            addRenderableWidget(tip(Button.builder(TOMBServer.text("editor.reset_all"), b -> resetToDefaultsAll())
                    .bounds(lx, y, LEFT_W, ROW_H).build(), TOMBServer.text("editor.reset_all.tooltip")));
            y += STEP;
            addRenderableWidget(tip(Button.builder(TOMBServer.text("editor.delete"), b -> removeSelected())
                    .bounds(lx, y, LEFT_W, ROW_H).build(), TOMBServer.text("editor.delete.tooltip")));

            // handing the selected entry to someone else, and taking one back
            y = divider(lx, y + ROW_H, LEFT_W);
            if (pendingProfile != null) {
                // A pasted profile is two different wishes wearing one code, so the group asks which before it
                // does anything. It borrows the two rows Copy and Paste normally hold rather than adding a row
                // of its own: the labels have to say what is COPIED, not where it lands ("into the current one"
                // reads as "replace this entry with the pasted one"), and that does not fit half a panel.
                // Ordinary buttons on the panel, not a pop-up — the choice belongs where the paste happened.
                // Both labels in the SAME colour: the pair is one question, so telling them apart by colour was
                // wrong — green and aqua read as "good one / other one". Gold is the palette's dark yellow
                // (#FFAA00; YELLOW is the bright #FFFF55). Colour goes on the message, the way the Cancel
                // button already does it: darkening the button face itself is not available here —
                // AbstractWidget.setAlpha reaches only the sprite from 1.21.11 on and leaves the text bright,
                // which reads as a yellow button rather than a dark one, tried and rejected on the preview trio.
                this.importAsEntryButton = Button.builder(
                                TOMBServer.text("editor.paste.as_entry").withStyle(ChatFormatting.GOLD),
                                b -> applyPendingAsEntry())
                        .bounds(lx, y, LEFT_W, ROW_H).build();
                addRenderableWidget(tip(this.importAsEntryButton, TOMBServer.text("editor.paste.as_entry.tooltip")));
                y += STEP;
                // nothing selected means there is no entry to pour the numbers into
                boolean canApply = selected() != null;
                this.importSettingsButton = Button.builder(
                                // an explicit colour would otherwise survive into the disabled state and make a
                                // dead button look live, so the grey is spelled out here
                                TOMBServer.text("editor.paste.settings_only")
                                        .withStyle(canApply ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY),
                                b -> applyPendingSettings())
                        .bounds(lx, y, LEFT_W, ROW_H).build();
                this.importSettingsButton.active = canApply;
                addRenderableWidget(tip(this.importSettingsButton,
                        canApply
                                ? TOMBServer.text("editor.paste.settings_only.tooltip")
                                : TOMBServer.text("editor.paste.settings_only.tooltip").append(Component.literal("\n"))
                                .append(TOMBServer.text("editor.paste.settings_only.no_entry"))));
            } else {
                // Only a saved entry can be handed out: a profile is what the config says, and someone importing
                // half-finished numbers you have not committed to yourself is nobody's idea of sharing.
                this.shareButton = Button.builder(TOMBServer.text("editor.share"), b -> copyToClipboard())
                        .bounds(lx, y, LEFT_W, ROW_H).build();
                addRenderableWidget(this.shareButton);
                // A box rather than a "paste from clipboard" button: by the time the profile is wanted the
                // clipboard may well hold something else entirely, and a button would silently do nothing. Here
                // the text is visible, can be pasted, typed or corrected, and the choice of what to do with it
                // comes up as soon as it reads as a profile.
                y += STEP;
                this.pasteBox = new EditBox(this.font, lx, y, LEFT_W, ROW_H, TOMBServer.text("editor.paste"));
                this.pasteBox.setMaxLength(512);
                // grey, so it reads as a placeholder rather than as text already in the box. The colour rides on
                // the component itself: vanilla draws the hint with the same call it draws a value with.
                this.pasteBox.setHint(TOMBServer.text("editor.paste.hint").withStyle(ChatFormatting.DARK_GRAY));
                this.pasteBox.setResponder(v -> {
                    // Applied from tick() rather than here: reading the profile rebuilds every widget, and this
                    // box is one of them — it must not be torn down inside its own responder.
                    if (!ProfileCodec.firstToken(v).isEmpty()) pendingImport = v;
                });
                tip(this.pasteBox, TOMBServer.text("editor.paste.tooltip"));
                addRenderableWidget(this.pasteBox);
            }
        }

        // ---- middle column: nothing but the doll, with the preview controls tucked into its corner ----
        this.mvy2 = pvy2;

        // Pose, skin and cape are about the doll, so they go back on top of it, in the bottom-left corner
        // of its box. Out in a strip of their own they cost the model a third of the panel's height for
        // three switches that are read once and left alone.
        int overlayX = pvx1 + OVERLAY_PAD;
        int overlayY = mvy2 - OVERLAY_PAD - (3 * OVERLAY_H + 2 * 2);
        // Plain button rather than a CycleButton: with only two poses the doll itself shows which one is on,
        // so the button carries just the word and stays narrow — the value goes into the tooltip.
        this.poseButton = Button.builder(TOMBServer.text("editor.pose"), b -> {
            PreviewPose[] poses = PreviewPose.values();
            previewPose = poses[(previewPose.ordinal() + 1) % poses.length];
            // Build a fresh stand-in, the same as the skin and cape switches do.
            //
            // Reported 2026-08-02: in single player WITHOUT an open LAN world, switching the pose moved
            // everything except the ARMS — and they then caught up the moment the skin was switched, i.e.
            // the moment the dummy was rebuilt. Toggling back left them stuck at the sneaking angle the
            // same way, always one pose behind. On a server or an open-to-LAN world it never happens.
            //
            // What was ruled out by reading the 26.2 bytecode: the whole sneak pose hangs off the single
            // flag HumanoidRenderState.isCrouching (body xRot, BOTH arms +0.4, legs, head), there is only
            // one such field in the hierarchy, AvatarRenderState extends HumanoidRenderState so
            // applyCrouchState's write lands on it, extractEntity allocates a fresh state every call so
            // nothing pools or reuses it, and no Player/Avatar class overrides isCrouching(). By the code
            // alone the path is identical in both cases, so the real cause is something about a paused
            // single-player world that the class files do not show. Rebuilding is the fix the report
            // itself points at, it costs one object per click, and it needs no version branch.
            dummy = null;
            updatePreview();
        }).bounds(overlayX, overlayY, OVERLAY_W, OVERLAY_H).build();
        // The tooltip does not name the current pose: the doll in front of it already does, and repeating
        // it meant re-attaching the tooltip on every click for a line nobody had to read.
        addRenderableWidget(overlayControl(tip(this.poseButton, TOMBServer.text("editor.pose.tooltip"))));

        overlayY += OVERLAY_H + 2;
        addRenderableWidget(overlayControl(tip(
                //? if >=1.21.11 {
                CycleButton.<PreviewSubject>builder(p -> TOMBServer.text(p.key), previewSubject)
                        .withValues(PreviewSubject.values())
                        //?} else {
                        /*CycleButton.<PreviewSubject>builder(p -> TOMBServer.text(p.key))
                                .withValues(PreviewSubject.values())
                                .withInitialValue(previewSubject)
                        *///?}
                        .create(overlayX, overlayY, OVERLAY_W, OVERLAY_H, TOMBServer.text("editor.subject"),
                                // the stand-in carries the UUID the preview is keyed on, so re-register it
                                (btn, val) -> {
                                    previewSubject = val;
                                    dummy = null;
                                    updatePreview();
                                }),
                TOMBServer.text("editor.subject.tooltip"))));

        overlayY += OVERLAY_H + 2;
        addRenderableWidget(overlayControl(tip(CycleButton.onOffBuilder(showCape)
                        .create(overlayX, overlayY, OVERLAY_W, OVERLAY_H, TOMBServer.text("editor.cape"),
                                (btn, val) -> {
                                    showCape = val;
                                    dummy = null;
                                }),
                TOMBServer.text("editor.cape.tooltip"))));

        // ---- right column: properties of the selected entry, in labelled sections ----
        if (e != null) {
            y = head(right, frameTop + FRAME_PAD, RIGHT_W, TOMBServer.text("editor.section.item"), false);
            this.idBox = new EditBox(this.font, right, y, RIGHT_W, ROW_H, TOMBServer.text("editor.item_id"));
            this.idBox.setMaxLength(256);
            this.idBox.setValue(e.getItemId());
            // "Add empty" leaves the id blank on purpose, so the box is routinely empty and has to say what
            // belongs in it. Same grey hint the paste box uses — the colour rides on the component, because
            // vanilla draws a hint with the same call it draws a value with.
            this.idBox.setHint(TOMBServer.text("editor.item_id.hint").withStyle(ChatFormatting.DARK_GRAY));
            tip(this.idBox, TOMBServer.text("editor.item_id.tooltip"));
            this.idBox.setResponder(v -> {
                ToolEntry cur = selected();
                if (cur != null) {
                    cur.setItemId(v);
                    // The component discriminator belongs to the item it was captured from, and it is
                    // applied on top of the built stack — vanilla dyed bundles, for one, are plain
                    // item_model overrides, so a leftover one repaints the new item as the old one. Typing
                    // a different id means a different item, so the old discriminator cannot survive it.
                    cur.setComponents("");
                    updatePreview();
                }
            });
            addRenderableWidget(this.idBox);

            y += STEP;
            AbstractWidget wSimilar = CycleButton.onOffBuilder(e.isApplyToSimilar())
                    .create(right, y, RIGHT_W, ROW_H, TOMBServer.text("editor.apply_to_similar"),
                            (btn, val) -> {
                                ToolEntry c = selected();
                                if (c != null) {
                                    c.setApplyToSimilar(val);
                                    updatePreview();
                                }
                            });
            addProp(wSimilar, TOMBServer.text("editor.apply_to_similar.tooltip"),
                    (c, ref) -> c.setApplyToSimilar(ref.isApplyToSimilar()));

            // ---- section: which captured components narrow this entry down. Uneven widths on purpose:
            // the last label is the longest one in both languages, and an ON/OFF value on top of it does
            // not fit the even third the other two are happy with.
            y = head(right, y + ROW_H + GROUP_GAP, RIGHT_W, TOMBServer.text("editor.section.match"), false);
            int narrowW = (RIGHT_W - 2 * GAP) * 3 / 10;
            int wideW = RIGHT_W - 2 * GAP - 2 * narrowW;
            // A filter is live if the entry captured that component, or if the item in hand is one this
            // entry covers and carries it — switching the filter on then captures it again. That is what
            // keeps a saved entry editable: saving drops whatever no filter used, and the item itself is
            // where it came from in the first place.
            ItemStack hand = handStackFor(e);
            boolean handName = !hand.isEmpty() && hand.get(DataComponents.CUSTOM_NAME) != null;
            boolean handModelData = !hand.isEmpty() && hand.get(DataComponents.CUSTOM_MODEL_DATA) != null;
            boolean handItemModel = false;
            //? if >=1.21.4 {
            handItemModel = !hand.isEmpty() && hand.get(DataComponents.ITEM_MODEL) != null;
            //?}
            boolean storedName = ConfigHandler.captured(e, DataComponents.CUSTOM_NAME);
            boolean storedModelData = ConfigHandler.captured(e, DataComponents.CUSTOM_MODEL_DATA);
            boolean storedItemModel = false;
            //? if >=1.21.4 {
            storedItemModel = ConfigHandler.captured(e, DataComponents.ITEM_MODEL);
            //?}
            // false: "Reset Entry" leaves the filters alone, the same way it leaves the item id and the
            // captured components alone — all three answer "which item", not "how is it drawn"
            addProp(matchToggle(right, y, narrowW, TOMBServer.text("editor.match_name"), e.isMatchName(),
                            (c, val) -> c.setMatchName(val)),
                    matchTooltip("editor.match_name.tooltip", storedName, handName),
                    (c, ref) -> c.setMatchName(ref.isMatchName()), false);
            addProp(matchToggle(right + narrowW + GAP, y, narrowW, TOMBServer.text("editor.match_model_data"),
                            e.isMatchModelData(), (c, val) -> c.setMatchModelData(val)),
                    matchTooltip("editor.match_model_data.tooltip", storedModelData, handModelData),
                    (c, ref) -> c.setMatchModelData(ref.isMatchModelData()), false);
            addProp(matchToggle(right + 2 * (narrowW + GAP), y, wideW, TOMBServer.text("editor.match_item_model"),
                            e.isMatchItemModel(), (c, val) -> c.setMatchItemModel(val)),
                    matchTooltip("editor.match_item_model.tooltip", storedItemModel, handItemModel),
                    (c, ref) -> c.setMatchItemModel(ref.isMatchItemModel()), false);

            // ---- the panel's halves: everything above says WHICH item the entry is about, everything
            // below says how that item is drawn. The rule runs border to border so it reads as a split of
            // the panel rather than as one more divider between groups.
            y = y + ROW_H + SPLIT_GAP;
            rules.add(new Rule(rightFrameX1, y, rightFrameX2 - rightFrameX1));
            y += 1 + SPLIT_GAP;

            // ---- section: how it is drawn ----
            y = head(right, y, RIGHT_W, TOMBServer.text("editor.section.rendering"), false);
            // Display takes a row to itself: its value is a long one ("firstperson_lefthand") that used to
            // run past the button. The flip toggle moves down next to Scale, which needs far less room.
            int flagW = 90;
            int scaleW = RIGHT_W - GAP - flagW;
            // A plain button that opens a list, not a CycleButton: there are ten contexts, and the one that
            // matters is rarely the next one along. The list is painted by the screen (see renderDropdown),
            // so the button itself stays an ordinary widget and needs no version branch.
            this.displayButton = Button.builder(displayLabel(e), b -> displayOpen = !displayOpen)
                    .bounds(right, y, RIGHT_W, ROW_H).build();
            layoutDropdown(right, y, RIGHT_W);
            addProp(this.displayButton, TOMBServer.text("editor.display.tooltip"),
                    (c, ref) -> c.setDisplayContext(ref.getDisplayContext()));
            y += STEP;
            addProp(new FloatSlider(right, y, scaleW, ROW_H, TOMBServer.text("editor.scale"), 0.1f, 3f, false,
                            () -> selected().getScale(), v -> {
                        selected().setScale(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.scale.tooltip"),
                    (c, ref) -> c.setScale(ref.getScale()));
            AbstractWidget wFlip = CycleButton.onOffBuilder(e.isFlip())
                    .create(right + scaleW + GAP, y, flagW, ROW_H, TOMBServer.text("editor.flip"),
                            (btn, val) -> {
                                ToolEntry c = selected();
                                if (c != null) {
                                    c.setFlip(val);
                                    updatePreview();
                                }
                            });
            addProp(wFlip, TOMBServer.text("editor.flip.tooltip"),
                    (c, ref) -> c.setFlip(ref.isFlip()));

            // ---- section: rotation ----
            y = head(right, y + ROW_H + GROUP_GAP, RIGHT_W, TOMBServer.text("editor.section.rotation"), false);
            addProp(new FloatSlider(right, y, subW, ROW_H, TOMBServer.text("editor.rot_x"), -180f, 180f, true,
                            () -> selected().getAngleX(), v -> {
                        selected().setAngleX(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.rot_x.tooltip"),
                    (c, ref) -> c.setAngleX(ref.getAngleX()));
            addProp(new FloatSlider(right + subW + GAP, y, subW, ROW_H, TOMBServer.text("editor.rot_y"), -180f, 180f, true,
                            () -> selected().getAngleY(), v -> {
                        selected().setAngleY(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.rot_y.tooltip"),
                    (c, ref) -> c.setAngleY(ref.getAngleY()));
            addProp(new FloatSlider(right + 2 * (subW + GAP), y, lastW, ROW_H, TOMBServer.text("editor.rot_z"), -180f, 180f, true,
                            () -> selected().getAngleZ(), v -> {
                        selected().setAngleZ(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.rot_z.tooltip"),
                    (c, ref) -> c.setAngleZ(ref.getAngleZ()));

            // ---- section: position ----
            y = head(right, y + ROW_H + GROUP_GAP, RIGHT_W, TOMBServer.text("editor.section.position"), false);
            addProp(new FloatSlider(right, y, subW, ROW_H, TOMBServer.text("editor.off_x"), -2f, 2f, false,
                            () -> selected().getOffsetX(), v -> {
                        selected().setOffsetX(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.off_x.tooltip"),
                    (c, ref) -> c.setOffsetX(ref.getOffsetX()));
            addProp(new FloatSlider(right + subW + GAP, y, subW, ROW_H, TOMBServer.text("editor.off_y"), -2f, 2f, false,
                            () -> selected().getOffsetY(), v -> {
                        selected().setOffsetY(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.off_y.tooltip"),
                    (c, ref) -> c.setOffsetY(ref.getOffsetY()));
            addProp(new FloatSlider(right + 2 * (subW + GAP), y, lastW, ROW_H, TOMBServer.text("editor.off_z"), -2f, 2f, false,
                            () -> selected().getOffsetZ(), v -> {
                        selected().setOffsetZ(v);
                        updatePreview();
                    }),
                    TOMBServer.text("editor.off_z.tooltip"),
                    (c, ref) -> c.setOffsetZ(ref.getOffsetZ()));
        }

        // ---- bottom row, each button under the frame it belongs to ----
        // Mod settings sit under the entry panel: they are the other half of "what this mod draws", and the
        // strip under the preview is better left to the preview. Save and Cancel stay under the panel whose
        // entries they are about.
        addRenderableWidget(tip(Button.builder(TOMBServer.text("editor.settings"),
                                b -> TOMBClient.setScreen(new ModSettingsScreen(this)))
                        .bounds(lx - FRAME_PAD, by, LEFT_W + 2 * FRAME_PAD, ROW_H).build(),
                TOMBServer.text("editor.settings.tooltip")));

        int actionW = (rightFrameX2 - rightFrameX1 - GAP) / 2;
        // Save while there is something to save, Done once there is not — so leaving is never the same
        // keystroke as committing, and the row says which of the two it is about to do.
        this.saveButton = Button.builder(TOMBServer.text("editor.save"), b -> {
            if (dirty()) saveNow();
            else onClose();
        }).bounds(rightFrameX1, by, actionW, ROW_H).build();
        addRenderableWidget(this.saveButton);
        // "Cancel changes" throws away work and sits where half the world expects Save to be, so it takes
        // two clicks: the first only arms it, and it disarms itself again after a moment. Named for what it
        // does — it is not an undo of the last action, and it does not close the screen either.
        this.cancelButton = Button.builder(TOMBServer.text("editor.cancel_changes"), b -> {   // "Cancel changes"
            if (cancelArmed()) {
                cancelArmedUntil = 0L;
                cancelChanges();
            } else {
                cancelArmedUntil = System.currentTimeMillis() + CANCEL_CONFIRM_MS;
                refreshBottomRow();
            }
        }).bounds(rightFrameX2 - actionW, by, actionW, ROW_H).build();
        addRenderableWidget(this.cancelButton);
        forceBottomRowRefresh();

        updatePreview();
    }

    /**
     * Whether anything differs from what is on disk. Drives both bottom buttons.
     */
    private void snapshotGlobals() {
        savedEnableMod = config.isEnableMod();
        savedRenderWithCape = config.isRenderWithCape();
        savedRenderWithElytra = config.isRenderWithElytra();
    }

    private boolean dirty() {
        if (config.isEnableMod() != savedEnableMod
                || config.isRenderWithCape() != savedRenderWithCape
                || config.isRenderWithElytra() != savedRenderWithElytra) return true;
        List<ToolEntry> live = tools();
        if (live.size() != snapshot.size()) return true;
        for (int i = 0; i < live.size(); i++) {
            if (!live.get(i).equals(snapshot.get(i))) return true;
        }
        return false;
    }

    /**
     * Writes the config and makes what was just written the new "saved state" for cancel and right-click.
     */
    private void saveNow() {
        config.save();
        this.snapshot = deepCopy(tools());
        pairWithSaved();
        snapshotGlobals();
        notice(TOMBServer.text("editor.saved"));
        refreshBottomRow();
    }

    /**
     * Puts every entry back to the last saved state — the same restore right-click does to one widget,
     * applied to the lot. Entries added since the last save disappear with it.
     */
    private void cancelChanges() {
        cancelArmedUntil = 0L;
        config.setEnableMod(savedEnableMod);
        config.setRenderWithCape(savedRenderWithCape);
        config.setRenderWithElytra(savedRenderWithElytra);
        config.setTools(deepCopy(snapshot));
        pairWithSaved();
        if (index >= tools().size()) index = tools().size() - 1;
        notice(TOMBServer.text("editor.cancel_changes.done"));
        rebuildWidgets();
    }

    /**
     * Pairs every live entry with its saved copy by identity: right-click restores what is on disk, and
     * the two lists drift apart as soon as anything is added or removed, so position is no use as a key.
     */
    private void pairWithSaved() {
        savedByEntry.clear();
        List<ToolEntry> live = tools();
        for (int i = 0; i < live.size() && i < snapshot.size(); i++) savedByEntry.put(live.get(i), snapshot.get(i));
    }

    private boolean cancelArmed() {
        return System.currentTimeMillis() < cancelArmedUntil;
    }

    /**
     * Makes the next refresh write every label, whatever it thinks the previous state was.
     */
    private void forceBottomRowRefresh() {
        lastDirty = !dirty();
        lastCancelArmed = !cancelArmed();
        refreshBottomRow();
    }

    /**
     * The bottom row follows two things: whether anything is unsaved, and whether the cancel button is armed. Called
     * every tick, but it only rebuilds labels when one of those actually flips — which is also what lets
     * the arming time out on its own.
     */
    private void refreshBottomRow() {
        if (saveButton == null || cancelButton == null) return;
        boolean dirty = dirty();
        boolean armed = cancelArmed();
        if (dirty == lastDirty && armed == lastCancelArmed) return;
        lastDirty = dirty;
        lastCancelArmed = armed;
        // Green only while there is something to save, as the opposite number of the red Cancel changes
        // beside it. Once it says Done there is nothing to urge, so it goes back to an ordinary label.
        saveButton.setMessage(dirty
                ? TOMBServer.text("editor.save").withStyle(ChatFormatting.GREEN)
                : TOMBServer.text("editor.done"));
        tip(saveButton, TOMBServer.text(dirty ? "editor.save.tooltip" : "editor.done.tooltip"));
        cancelButton.active = dirty;
        // red throughout, so it is never mistaken for the save button by anyone used to finding one there
        cancelButton.setMessage(armed
                ? TOMBServer.text("editor.cancel_changes.confirm").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                : TOMBServer.text("editor.cancel_changes").withStyle(ChatFormatting.RED));
        tip(cancelButton, TOMBServer.text(armed ? "editor.cancel_changes.confirm.tooltip" : "editor.cancel_changes.tooltip")
                .append(Component.literal("\n")).append(TOMBServer.text("editor.cancel_changes.instant")));
        if (shareButton != null) {
            shareButton.active = !dirty && selected() != null;
            tip(shareButton, dirty ? TOMBServer.text("editor.share.tooltip").append(Component.literal("\n"))
                    .append(TOMBServer.text("editor.share.needs_save")) : TOMBServer.text("editor.share.tooltip"));
        }
    }

    /**
     * Puts the whole entry back to how a fresh one for this item would look. Deliberately different from
     * right-click, which restores what is saved: this is the "start over" escape hatch, so it has to ignore
     * the saved state too. The item id and the captured components survive — they say which item this entry
     * is about, not how it is drawn.
     */
    private void resetToDefaultsAll() {
        for (ResetTarget rt : resettables) {
            if (rt.inResetEntry()) rt.restoreDefaults().run();
        }
        rebuildWidgets();
    }

    /**
     * The entry as last saved, or defaults when it was added during this session and never saved.
     */
    private ToolEntry savedOrDefaults(ToolEntry entry) {
        if (entry == null) return null;
        ToolEntry saved = savedByEntry.get(entry);
        return saved != null ? saved : defaultsFor(entry);
    }

    /**
     * A stand-in entry holding the built-in defaults for whatever item the given entry points at.
     */
    private ToolEntry defaultsFor(ToolEntry entry) {
        if (entry == null) return null;
        ToolEntry defaults = new ToolEntry();
        defaults.setItemId(entry.getItemId());
        var item = ConfigHandler.buildStack(entry).getItem();
        defaults.setScale(ConfigHandler.defaultScale(item));
        defaults.setAngleZ(ConfigHandler.defaultAngleZ(item));
        defaults.setFlip(ConfigHandler.defaultFlip(item));
        defaults.setDisplayContext(ConfigHandler.defaultDisplayContext(item).getSerializedName());
        float[] off = ConfigHandler.defaultOffset(item);
        defaults.setOffsetX(off[0]);
        defaults.setOffsetY(off[1]);
        defaults.setOffsetZ(off[2]);
        return defaults;
    }

    /**
     * Places a section heading and returns the y the first row under it should use. The headings are drawn
     * during rendering, not as widgets, so they are collected here.
     */
    private int head(int x, int y, int w, Component label, boolean centered) {
        sections.add(new Section(label, x, y, w, centered));
        return y + HEAD_H;
    }

    /**
     * A 1px divider between two groups of a panel; returns the y the next row starts at.
     */
    private int divider(int x, int y, int w) {
        rules.add(new Rule(x, y + GROUP_GAP, w));
        return y + 2 * GROUP_GAP + 1;
    }

    /**
     * A border-only frame: a box drawn around something that already sits on a backdrop.
     */
    private void frame(int x1, int y1, int x2, int y2) {
        frames.add(new Frame(x1, y1, x2, y2, false));
    }

    /**
     * One of the three panels: the same border, over a backdrop dark enough for it to read against.
     */
    private void panel(int x1, int y1, int x2, int y2) {
        frames.add(new Frame(x1, y1, x2, y2, true));
    }

    /**
     * The rule the title sits on, drawn as the two stretches either side of the text.
     */
    private void titleRules() {
        int mid = guiLeft + contentW / 2;
        int ty = guiTop + 12;
        int half = this.font.width(this.title) / 2 + 8;
        int x1 = guiLeft + LEFT_X - FRAME_PAD;
        int x2 = guiLeft + contentW - LEFT_X + FRAME_PAD;
        if (mid - half > x1) rules.add(new Rule(x1, ty, mid - half - x1));
        if (x2 > mid + half) rules.add(new Rule(mid + half, ty, x2 - mid - half));
    }

    /**
     * What the Display button reads as: the property, then the context it is currently set to.
     */
    private static Component displayLabel(ToolEntry entry) {
        ItemDisplayContext ctx = ConfigHandler.parseDisplayContext(entry.getDisplayContext());
        return TOMBServer.text("editor.display").append(Component.literal(": " + ctx.getSerializedName()));
    }

    /**
     * Works out where the open Display list goes. It hangs under its button, but the button sits low enough
     * in a full panel that ten rows run off the bottom, so it flips above when there is no room below —
     * and, failing both, is pinned inside the window. It deliberately overhangs the panel frame: it is a
     * pop-up over the screen, not another row that has to be paid for out of the vertical budget.
     */
    private void layoutDropdown(int x, int y, int w) {
        int h = ItemDisplayContext.values().length * DROP_ROW_H + 2;
        this.ddx1 = x;
        this.ddx2 = x + w;
        int below = y + ROW_H;
        if (below + h <= this.height) this.ddy1 = below;
        else if (y - h >= 0) this.ddy1 = y - h;
        else this.ddy1 = Math.max(0, this.height - h);
        this.ddy2 = ddy1 + h;
    }

    /**
     * Collects what the player is carrying and puts the grid up. Iterates the whole {@code Container} view
     * of the inventory rather than its compartments by hand, so the hotbar, the backpack and whatever else
     * the version keeps in there all come along without a branch.
     *
     * <p>De-duplicated with {@code areStacksEqual}, which compares item and components while ignoring what
     * changes in the hand (damage, charged projectiles, bundle contents) — five of the same pickaxe at
     * different durabilities are one entry's worth of item, and five cells of it would just be noise.
     */
    private void openPicker() {
        displayOpen = false;
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
        listScroll = 0;
        pickerOpen = true;
        rebuildWidgets();
    }

    /**
     * Leaves the picking mode and gives the entry grid back.
     */
    private void closePicker() {
        pickerOpen = false;
        pickerStacks.clear();
        listScroll = 0;
        rebuildWidgets();
    }

    /**
     * How many cells the left grid is showing: carried stacks while picking, entries otherwise.
     */
    private int gridCount() {
        return pickerOpen ? pickerStacks.size() : tools().size();
    }

    /**
     * The stack drawn in cell {@code i} of whichever list the grid is showing.
     */
    private ItemStack gridStack(int i) {
        return pickerOpen ? pickerStacks.get(i) : icon(tools().get(i));
    }

    /**
     * What cell {@code i} says on hover: item name over its id, for both lists.
     */
    private Component gridTooltip(int i) {
        if (!pickerOpen) return entryLabel(tools().get(i));
        ItemStack stack = pickerStacks.get(i);
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemTooltip(stack, key == null ? "" : key.toString());
    }

    private boolean overDropdown(double mx, double my) {
        return displayOpen && mx >= ddx1 && mx < ddx2 && my >= ddy1 && my < ddy2;
    }

    /**
     * Which context the given point is over, or null when it is on the list's border rather than a row.
     */
    private ItemDisplayContext dropdownValueAt(double my) {
        int row = (int) ((my - ddy1 - 1) / DROP_ROW_H);
        ItemDisplayContext[] values = ItemDisplayContext.values();
        return (row >= 0 && row < values.length) ? values[row] : null;
    }

    /**
     * The stack an entry stands for, cached. The grid asks for one per visible cell per frame and building
     * it decodes the entry's component patch out of JSON, so it is kept until the entry's id, its captured
     * components or the filters over them change — an id typed into the box does not rebuild the screen.
     */
    private ItemStack icon(ToolEntry entry) {
        String key = entry.getItemId() + "|" + entry.getComponents()
                + entry.isMatchName() + entry.isMatchModelData() + entry.isMatchItemModel();
        if (!key.equals(iconKey.get(entry))) {
            iconKey.put(entry, key);
            iconCache.put(entry, ConfigHandler.buildStack(entry));
        }
        return iconCache.get(entry);
    }

    /**
     * One of the NBT filters, as an ON/OFF toggle so it reads and sits like every other switch on the
     * screen. The tooltip is attached later by addProp, which adds the reset hint to it.
     *
     * <p>Always clickable. It used to grey out when neither the entry nor the held item carried that
     * component, which made a saved-then-reopened entry look broken: saving drops the components no filter
     * used, so coming back the toggles were both OFF and dead. Leaving them live is harmless — {@code
     * ConfigHandler.componentsMatch} walks the *stored* discriminator, so a filter switched on with nothing
     * captured for it never enters the comparison. The tooltip still says which of the two cases it is in.
     */
    private AbstractWidget matchToggle(int x, int y, int w, Component label, boolean initial,
                                       BiConsumer<ToolEntry, Boolean> setter) {
        return CycleButton.onOffBuilder(initial)
                .create(x, y, w, ROW_H, label, (btn, value) -> {
                    ToolEntry c = selected();
                    if (c == null) return;
                    // switching a filter on needs a value to match against: take it off the item in hand
                    // when the entry no longer carries one
                    if (value) recaptureFromHand(c);
                    setter.accept(c, value);
                    updatePreview();
                });
    }

    /**
     * The filter's own explanation, plus why it is greyed out when the entry captured nothing for it.
     */
    private MutableComponent matchTooltip(String key, boolean captured, boolean fromHand) {
        MutableComponent tooltip = TOMBServer.text(key)
                .append(Component.literal("\n")).append(TOMBServer.text("editor.match.pruned"));
        if (captured) return tooltip;
        return tooltip.append(Component.literal("\n"))
                .append(TOMBServer.text(fromHand ? "editor.match.from_hand" : "editor.match.absent"));
    }

    /**
     * Registers one of the controls floating over the doll, so the panel knows not to claim its hover.
     */
    private <T extends AbstractWidget> T overlayControl(T widget) {
        overlayControls.add(widget);
        return widget;
    }

    /**
     * Whether the cursor is on one of the three controls floating over the doll.
     */
    private boolean overOverlayControls(double mx, double my) {
        for (AbstractWidget w : overlayControls) {
            if (w.isMouseOver(mx, my)) return true;
        }
        return false;
    }

    /**
     * Attaches a hover tooltip that only appears once the cursor has rested on the widget.
     */
    private static <T extends AbstractWidget> T tip(T widget, Component text) {
        widget.setTooltip(Tooltip.create(text));
        widget.setTooltipDelay(TOOLTIP_DELAY);
        return widget;
    }

    /**
     * Registers a property widget along with how to put its value back. The same action serves both
     * restores — right-click reads from the entry as saved, the Reset button reads from a freshly
     * initialised one — so the two can never disagree about which field they touch.
     *
     * @param restore receives the entry to write into and the one to read the old value from
     */
    private void addProp(AbstractWidget widget, MutableComponent tooltip,
                         BiConsumer<ToolEntry, ToolEntry> restore) {
        addProp(widget, tooltip, restore, true);
    }

    /**
     * @param inResetEntry whether "Reset Entry" touches this property. The NBT filters pass false: they say
     *                     WHICH item the entry is about, exactly like the item id and the captured
     *                     components that button already leaves alone, so wiping them would quietly widen
     *                     the entry from one plugin variant to every item sharing its base id. Right-click
     *                     still restores them one at a time, and Cancel changes still puts them all back.
     */
    private void addProp(AbstractWidget widget, MutableComponent tooltip,
                         BiConsumer<ToolEntry, ToolEntry> restore, boolean inResetEntry) {
        // the reset hint goes on a line of its own rather than trailing the description, so every one of
        // these tooltips breaks in the same place instead of wherever the sentence happens to run out
        tip(widget, tooltip.append(Component.literal("\n")).append(TOMBServer.text(REVERT_KEY)));
        addRenderableWidget(widget);
        resettables.add(new ResetTarget(widget,
                () -> applyRestore(restore, savedOrDefaults(selected())),
                () -> applyRestore(restore, defaultsFor(selected())),
                inResetEntry));
        if (widget instanceof FloatSlider fs) sliders.add(fs);
    }

    private void applyRestore(BiConsumer<ToolEntry, ToolEntry> restore, ToolEntry reference) {
        ToolEntry current = selected();
        if (current == null || reference == null) return;
        restore.accept(current, reference);
        updatePreview();
    }

    /**
     * How many rows of cells the entries fill, however few are on the last one.
     */
    private int gridRowCount() {
        return (gridCount() + listCols - 1) / listCols;
    }

    /**
     * Keeps the selected entry inside the visible rows, and the scroll inside the grid.
     */
    private void scrollToSelected() {
        int max = Math.max(0, gridRowCount() - listRows);
        int row = index / listCols;
        if (row < listScroll) listScroll = row;
        if (row >= listScroll + listRows) listScroll = row - listRows + 1;
        listScroll = Mth.clamp(listScroll, 0, max);
    }

    private boolean overList(double mx, double my) {
        return mx >= lsx1 && mx < lsx2 && my >= lsy1 && my < lsy2;
    }

    /**
     * The entry the cursor is over, or -1 — including the empty tail of the last row.
     */
    private int entryAt(double mx, double my) {
        int col = (int) Math.floor((mx - gridX) / ENTRY_CELL);
        int row = (int) Math.floor((my - gridY) / ENTRY_CELL);
        if (col < 0 || col >= listCols || row < 0 || row >= listRows) return -1;
        int i = (listScroll + row) * listCols + col;
        return i < gridCount() ? i : -1;
    }

    /**
     * Where a dragged entry would land, or -1. Same cells {@link #entryAt} answers for, plus the empty tail
     * of the last row: dropping past the last icon means "put it at the end", which is the only way to move
     * something down to the bottom. The padding either side of the columns is NOT the tail — landing there
     * must do nothing rather than fling the entry to the end.
     */
    private int dropTargetAt(double mx, double my) {
        int col = (int) Math.floor((mx - gridX) / ENTRY_CELL);
        int row = (int) Math.floor((my - gridY) / ENTRY_CELL);
        if (col < 0 || col >= listCols || row < 0 || row >= listRows) return -1;
        int i = (listScroll + row) * listCols + col;
        return Math.min(i, tools().size() - 1);
    }

    /**
     * The scrollbar only exists while the grid overflows; its grab area is a little wider than it looks.
     */
    private boolean overScrollbar(double mx, double my) {
        return gridRowCount() > listRows && mx >= lsx2 - LIST_GUTTER && mx <= lsx2 && my >= lsy1 && my <= lsy2;
    }

    /**
     * Puts the thumb under the cursor: the middle of the thumb follows the mouse, as vanilla lists do.
     */
    private void scrollFromMouse(double my) {
        int total = gridRowCount();
        int max = Math.max(0, total - listRows);
        if (max == 0) {
            listScroll = 0;
            return;
        }
        int trackH = lsy2 - lsy1 - 2;
        int thumbH = Math.max(6, trackH * listRows / total);
        double span = trackH - thumbH;
        double offset = my - (lsy1 + 1) - thumbH / 2.0;
        listScroll = span <= 0 ? 0 : Mth.clamp((int) Math.round(offset / span * max), 0, max);
    }

    /**
     * What a cell says when the cursor rests on it: the whole item id, namespace and all — the icon already
     * says which item it is, so the hover is there for the exact id. A blank id is what "Add empty" leaves
     * behind, and it needs to say so rather than hover as an empty line.
     */
    private Component entryLabel(ToolEntry entry) {
        String id = entry.getItemId();
        if (id == null || id.isEmpty()) return TOMBServer.text("editor.entry.empty");
        return itemTooltip(icon(entry), id);
    }

    /**
     * What a hovered item cell says: the item's own name, then the id underneath it in grey. Shared by the
     * entry grid and the picker so the two read the same — the grid used to give the bare id, which says
     * little for a plugin item whose whole point is that it does not look like its id.
     */
    private static Component itemTooltip(ItemStack stack, String id) {
        MutableComponent line = Component.literal(id == null ? "" : id).withStyle(ChatFormatting.GRAY);
        return stack.isEmpty() ? line
                : stack.getHoverName().copy().append(Component.literal("\n")).append(line);
    }

    private void select(int newIndex) {
        int size = tools().size();
        if (size == 0) {
            index = 0;
            return;
        }
        index = Math.floorMod(newIndex, size);
        rebuildWidgets();
    }

    /**
     * Adds an entry for a stack the player is carrying, or opens the one that already covers it.
     *
     * <p>Used to read the main hand and be the only way in, which meant juggling the hotbar to configure
     * anything. It now takes whatever was picked out of the inventory grid — the held item included, since
     * that is in the inventory too.
     */
    private void addFromStack(ItemStack held) {
        if (held == null || held.isEmpty()) return;

        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        String components = ConfigHandler.encodeComponents(held);

        // An entry already covering this exact item decides where it sits, so a second one would be a dead
        // twin — open the existing one instead. Identity here is the item and the filters it requires,
        // nothing else: how the entry draws or places the item has no bearing on which item it is about.
        List<ToolEntry> existing = tools();
        for (int i = 0; i < existing.size(); i++) {
            ToolEntry candidate = existing.get(i);
            if (itemId.equals(candidate.getItemId()) && ConfigHandler.componentsMatch(held, candidate)) {
                select(i);
                return;
            }
        }

        ToolEntry e = new ToolEntry();
        e.setItemId(itemId);
        e.setComponents(components);
        // Only custom_model_data is switched on from the capture. That one is a deliberate marker — an item
        // carrying it is almost always a plugin or pack item that wants telling apart. The other two are
        // left for the user to switch on:
        //   * the NAME, because items get renamed in an anvil constantly;
        //   * the ITEM MODEL, because from 1.21.4 on practically every item carries item_model and it is
        //     normally just the vanilla model, so matching on it would pin the entry to nothing useful
        //     while looking like it had captured something meaningful.
        e.setMatchModelData(held.get(DataComponents.CUSTOM_MODEL_DATA) != null);
        // Seed from the item's built-in defaults, so items the mod already handles (trident, shield, ...)
        // start looking the way they normally render. These must be the default* helpers, not the getTool*
        // ones: those resolve through getToolEntry first, so a new entry would silently inherit the tuned
        // values of whatever existing entry already matches this item instead of starting clean.
        var item = held.getItem();
        e.setScale(ConfigHandler.defaultScale(item));
        e.setAngleZ(ConfigHandler.defaultAngleZ(item));
        e.setFlip(ConfigHandler.defaultFlip(item));
        e.setDisplayContext(ConfigHandler.defaultDisplayContext(item).getSerializedName());
        float[] off = ConfigHandler.defaultOffset(item);
        e.setOffsetX(off[0]);
        e.setOffsetY(off[1]);
        e.setOffsetZ(off[2]);
        tools().add(e);
        index = tools().size() - 1;
        rebuildWidgets();
    }

    private void addEmpty() {
        ToolEntry e = new ToolEntry();
        // Blank on purpose. "Add from hand" now opens an existing entry rather than duplicating it, so this
        // button is the only way left to get a fresh one — it must never collide with what is already
        // there. The constructor's diamond_sword default would both collide and, being a specific entry,
        // shadow a tuned "apply to all similar" sword entry until the id was filled in.
        e.setItemId("");
        tools().add(e);
        index = tools().size() - 1;
        rebuildWidgets();
    }

    /**
     * Opens the editor on a profile that arrived from someone else. The entry is added after the
     * constructor has taken its snapshot, so Cancel drops it again and only Done writes it to disk.
     */
    public static ToolEditorScreen openWithImported(Screen parent, ToolEntry imported) {
        ToolEditorScreen screen = new ToolEditorScreen(parent);
        screen.addImported(imported);
        return screen;
    }

    /**
     * Takes in an entry from outside the editor and selects it.
     *
     * <p>An entry already covering the same item is written over rather than joined by a twin: lookup
     * stops at the first match, so a second entry for the same item could never take effect — importing
     * one would look like nothing happened. Nothing is lost by it either, since right-click still puts any
     * single value back to what is saved and Cancel drops the whole import.
     */
    public void addImported(ToolEntry imported) {
        List<ToolEntry> t = tools();
        for (int i = 0; i < t.size(); i++) {
            ToolEntry existing = t.get(i);
            if (sameTarget(existing, imported)) {
                copySettings(imported, existing);
                index = i;
                merged = true;
                return;
            }
        }
        t.add(imported);
        index = t.size() - 1;
        merged = false;
    }

    /**
     * Same item, and pinned to the same components — decided on what the two entries match on, never on
     * the stored strings. Those drift apart legitimately: an entry that still carries the whole capture
     * and its own profile, which carries only the filtered part, mean exactly the same item. Comparing
     * text called them different, and importing your own profile produced a second entry.
     *
     * <p>This is the same notion "Add from hand" uses, which asks {@code componentsMatch} — so both ways
     * of bringing an item in land on the entry that already covers it.
     */
    private static boolean sameTarget(ToolEntry a, ToolEntry b) {
        return Objects.equals(a.getItemId(), b.getItemId())
                && ConfigHandler.sameDiscriminator(a, b);
    }

    /**
     * Only how the item is drawn: display context, flip, scale, rotation and position. Deliberately not
     * the item id, the captured components, the NBT filters, "apply to all similar" or the enabled flag —
     * those say WHICH item the entry is about, and this is for pouring one entry's look into another
     * entry that is about something else entirely.
     */
    private static void copyDisplaySettings(ToolEntry from, ToolEntry to) {
        to.setDisplayContext(from.getDisplayContext());
        to.setFlip(from.isFlip());
        to.setScale(from.getScale());
        to.setAngleX(from.getAngleX());
        to.setAngleY(from.getAngleY());
        to.setAngleZ(from.getAngleZ());
        to.setOffsetX(from.getOffsetX());
        to.setOffsetY(from.getOffsetY());
        to.setOffsetZ(from.getOffsetZ());
    }

    /**
     * Everything except which item the entry is about — that is what the two already agree on.
     */
    private static void copySettings(ToolEntry from, ToolEntry to) {
        to.setMatchName(from.isMatchName());
        to.setMatchModelData(from.isMatchModelData());
        to.setMatchItemModel(from.isMatchItemModel());
        to.setApplyToSimilar(from.isApplyToSimilar());
        to.setEnabled(from.isEnabled());
        to.setFlip(from.isFlip());
        to.setDisplayContext(from.getDisplayContext());
        to.setScale(from.getScale());
        to.setAngleX(from.getAngleX());
        to.setAngleY(from.getAngleY());
        to.setAngleZ(from.getAngleZ());
        to.setOffsetX(from.getOffsetX());
        to.setOffsetY(from.getOffsetY());
        to.setOffsetZ(from.getOffsetZ());
    }

    /**
     * The main-hand item when it is one this entry already covers — the only item an entry may take
     * components from. Requiring the entry's own filters to match keeps a second, differently named sword
     * from overwriting the name this entry is pinned to.
     */
    private ItemStack handStackFor(ToolEntry entry) {
        if (minecraft == null || minecraft.player == null || entry == null) return ItemStack.EMPTY;
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) return ItemStack.EMPTY;
        var key = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (key == null || !key.toString().equals(entry.getItemId())) return ItemStack.EMPTY;
        return ConfigHandler.componentsMatch(held, entry) ? held : ItemStack.EMPTY;
    }

    /**
     * Refills the entry's discriminator from the item in hand. Safe whenever the hand qualifies: that item
     * has already been checked against the filters that are on, so everything they go by keeps its value —
     * all that changes is that what a save dropped is back.
     */
    private void recaptureFromHand(ToolEntry entry) {
        ItemStack hand = handStackFor(entry);
        if (hand.isEmpty()) return;
        String captured = ConfigHandler.encodeComponents(hand);
        if (!captured.isEmpty() && !captured.equals(entry.getComponents())) entry.setComponents(captured);
    }

    private void copyToClipboard() {
        ToolEntry current = selected();
        if (current == null || minecraft == null) return;
        String payload = ProfileCodec.encode(current);
        minecraft.keyboardHandler.setClipboard(payload);
        // The line is meant to be pasted wherever the player likes, chat included, so warn when it has
        // outgrown what a chat message may carry rather than let it be cut off there.
        notice(payload.length() > ProfileCodec.CHAT_LIMIT
                ? TOMBServer.text("editor.share.copied_long", payload.length(), ProfileCodec.CHAT_LIMIT)
                : TOMBServer.text("editor.share.copied"));
    }

    @Override
    public void tick() {
        super.tick();
        refreshBottomRow();
        if (pendingImport == null) return;
        String text = pendingImport;
        pendingImport = null;
        ToolEntry imported = ProfileCodec.decode(ProfileCodec.firstToken(text));
        if (imported == null) {
            notice(TOMBServer.text("editor.paste.invalid"));
            return;
        }
        // Nothing is applied yet — the profile waits while the panel asks what it is for. Rebuilding here
        // both puts up the two choices and disposes of the box, so the same text is never read twice.
        pendingProfile = imported;
        rebuildWidgets();
    }

    /**
     * The pasted profile taken as a whole entry: exactly what a profile clicked in chat does.
     */
    private void applyPendingAsEntry() {
        ToolEntry profile = pendingProfile;
        pendingProfile = null;
        if (profile != null) {
            addImported(profile);
            notice(merged ? TOMBServer.text("editor.paste.merged") : TOMBServer.text("editor.paste.added"));
        }
        rebuildWidgets();
    }

    /**
     * The pasted profile taken as numbers only, written over the entry that is selected. This is the answer
     * to "I tuned one entry until it sat right on the belt and now I want a modded item to sit there too":
     * the tuned entry's own profile cannot be imported for that, because it names an item that already has
     * an entry, so importing it just selects the entry it came from and nothing appears to happen.
     */
    private void applyPendingSettings() {
        ToolEntry profile = pendingProfile;
        pendingProfile = null;
        ToolEntry current = selected();
        if (profile != null && current != null) {
            copyDisplaySettings(profile, current);
            updatePreview();
            notice(TOMBServer.text("editor.paste.applied"));
        }
        rebuildWidgets();
    }

    /**
     * Puts the pending profile away without applying it, and gives the paste box back.
     */
    private void cancelPendingProfile() {
        pendingProfile = null;
        rebuildWidgets();
    }

    /**
     * Says something short to the player, as a toast. Deliberately not chat: the HUD is not drawn behind an
     * open screen, so a chat notice would only surface once the editor is closed, by which time it means
     * nothing. A toast is: every version renders the toast queue after the screen (verified in bytecode on
     * all five), so it lands on top of the editor rather than in the corner of it.
     *
     * <p>It used to be a line drawn along the bottom of the preview panel, which is exactly where the
     * pose/skin/cape controls sit — it covered them for its four seconds every time anything was saved.
     * Toasts have their own corner of the screen and cover nothing.
     */
    private void notice(Component message) {
        TOMBClient.toast(TOMBServer.text("name"), message);
    }

    private void removeSelected() {
        List<ToolEntry> t = tools();
        if (index >= 0 && index < t.size()) {
            t.remove(index);
            if (index >= t.size()) index = t.size() - 1;
            rebuildWidgets();
        }
    }

    private void updatePreview() {
        if (minecraft == null || minecraft.player == null) return;
        ToolEntry e = selected();
        if (e == null) {
            TOMBClient.PREVIEW_PLAYER = null;
            TOMBClient.PREVIEW_STACK = ItemStack.EMPTY;
            TOMBClient.PREVIEW_ENTRY = null;
            return;
        }
        // pin the lookup to the entry being edited, so the doll shows this config even when another entry
        // targets the same item and would otherwise win
        TOMBClient.PREVIEW_ENTRY = e;
        // key the forced preview on the model that is actually drawn: the Steve/Alex stand-ins carry a
        // synthetic profile UUID, so pointing this at the real player would leave them without the item
        RemotePlayer shown = dummy();
        TOMBClient.PREVIEW_PLAYER = (shown != null ? shown : minecraft.player).getUUID();
        TOMBClient.PREVIEW_STACK = ConfigHandler.buildStack(e);
    }

    /**
     * The preview needs a player entity, and an entity needs a level — in the main menu there is neither,
     * so the panel would just sit empty and look broken. Building a throw-away ClientLevel to work around
     * that would mean fabricating a connection and packet listener, whose shape differs across every
     * version this mod targets, so the panel says what it needs instead.
     */
    private boolean previewUnavailable() {
        return minecraft == null || minecraft.level == null || minecraft.player == null;
    }

    private boolean overPreview(double mx, double my) {
        return mx >= pvx1 && mx <= pvx2 && my >= pvy1 && my <= mvy2;
    }

    //? if >=1.21.11 {
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        //?} else {
    /*@Override
    public boolean mouseClicked(double mx, double my, int button) {
    *///?}
        // Vanilla only ever moves focus TO whatever was clicked — clicking past every widget leaves the last
        // one focused, and a focused EditBox keeps its cursor and hides its hint until something else is
        // clicked. Dropping focus up front makes any click behave like a click on another widget: what was
        // actually hit gets focus straight back from super.mouseClicked below, and everything else is left
        // unfocused. Safe before the early returns too — the widget drags that read getFocused() (sliders)
        // all go through super, which re-focuses them in the same call.
        setFocused(null);
        // A profile waiting to be told what it is for must never trap the panel: clicking past both choices
        // puts it away and gives the paste box back. Checked before anything else consumes the click, and
        // it only cancels — the click then carries on to whatever it was actually aimed at.
        if (pendingProfile != null
                && !(importAsEntryButton != null && importAsEntryButton.isMouseOver(mx, my))
                && !(importSettingsButton != null && importSettingsButton.isMouseOver(mx, my))) {
            cancelPendingProfile();
        }
        // The open Display list is painted over the widgets, so it has to be hit-tested before them too —
        // otherwise a click meant for a row lands on whatever the list happens to be covering.
        if (displayOpen) {
            boolean onButton = displayButton != null && displayButton.isMouseOver(mx, my);
            if (button == 0 && overDropdown(mx, my)) {
                ItemDisplayContext picked = dropdownValueAt(my);
                displayOpen = false;
                ToolEntry c = selected();
                if (picked != null && c != null) {
                    c.setDisplayContext(picked.getSerializedName());
                    displayButton.setMessage(displayLabel(c));
                    updatePreview();
                }
                return true;
            }
            // a click anywhere else just puts the list away; on the button itself it is swallowed as well,
            // or the button's own handler would open it straight back up
            displayOpen = false;
            if (onButton) return true;
        }
        // Cleared before anything can return early: the scrollbar press below does return early, and a
        // stale entry-drag left armed there would turn dragging the scrollbar into reordering the grid.
        dragEntryFrom = -1;
        // the scrollbar sits inside the grid box, so it has to be asked first
        if (button == 0 && overScrollbar(mx, my)) {
            draggingScroll = true;
            scrollFromMouse(my);
            return true;
        }
        draggingScroll = false;
        if (button == 0 && overList(mx, my)) {
            int i = entryAt(mx, my);
            if (i >= 0) {
                if (pickerOpen) {
                    // addFromStack rebuilds the widgets; leaving the mode first means it rebuilds into the
                    // entry grid, with the entry it just made selected
                    ItemStack chosen = pickerStacks.get(i);
                    pickerOpen = false;
                    pickerStacks.clear();
                    listScroll = 0;
                    addFromStack(chosen);
                    return true;
                }
                select(i);
                // select() rebuilt the widgets, so arm the drag afterwards or init() would be reading a
                // field that is about to be set anyway
                dragEntryFrom = index;
                return true;
            }
        }
        // Right-click on the panel puts the camera back where it started. Right-click already means "put
        // this back" on every widget in this screen, and the doll is the one thing here you can knock out
        // of shape by dragging and scrolling — with a 360° turntable there is no obvious way home.
        // The Pose/Skin/Cape trio is excluded: it sits inside the panel, so right-clicking a button was
        // yanking the camera instead of doing nothing, which is not what a right-click on a button means.
        if (button == 1 && overPreview(mx, my) && !overOverlayControls(mx, my)) {
            previewYaw = 0f;
            previewPitch = 0f;
            previewZoom = 1f;
            return true;
        }
        // Right-click cancels the changes at once. Everywhere else in this screen right-click already means
        // "put back what is saved", so on the button that does exactly that for the whole config it needs
        // no confirming — nobody right-clicks a button by accident.
        if (cancelButton != null && cancelButton.isMouseOver(mx, my)) {
            if (button == 1 && cancelButton.active) {
                cancelChanges();
                return true;
            }
        } else if (cancelArmed()) {
            // armed it by mistake? clicking anywhere else calls it off, rather than making you wait it out
            cancelArmedUntil = 0L;
            refreshBottomRow();
        }
        if (button == 1) {
            for (ResetTarget rt : resettables) {
                if (rt.widget().isMouseOver(mx, my)) {
                    rt.restoreSaved().run();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        //? if >=1.21.11 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(mx, my, button);
         *///?}
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        // an open pop-up eats the wheel, so a scroll aimed at it never nudges a slider underneath
        if (overDropdown(mx, my)) return true;
        if (overList(mx, my)) {
            int max = Math.max(0, gridRowCount() - listRows);
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY), 0, max);
            return true;
        }
        // the wheel over the panel zooms the doll, the same gesture that turns it when dragged
        if (overPreview(mx, my)) {
            previewZoom = Mth.clamp(previewZoom + (float) Math.signum(scrollY) * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
            return true;
        }
        for (FloatSlider s : sliders) {
            if (s.isMouseOver(mx, my)) {
                s.nudge(scrollY);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    //? if >=1.21.11 {
    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        //?} else {
    /*@Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
    *///?}
        if (button == 0 && draggingScroll) {
            scrollFromMouse(my);
            return true;
        }
        // Reordering the grid. The entry is moved as the cursor passes over another cell rather than on
        // release, so the icons under the cursor ARE the feedback and no mouseReleased override is needed.
        // Nothing is rebuilt: the same entry object stays selected, so every widget on the right still
        // shows the right values, and the grid is drawn from the live list every frame anyway.
        if (button == 0 && dragEntryFrom >= 0 && !pickerOpen) {
            int target = dropTargetAt(mx, my);
            if (target >= 0 && target != dragEntryFrom) {
                List<ToolEntry> t = tools();
                t.add(target, t.remove(dragEntryFrom));
                index = target;
                dragEntryFrom = target;
                scrollToSelected();
            }
            return true;
        }
        if (button == 0 && overPreview(mx, my)) {
            previewYaw += (float) dragX;                                          // free 360° spin
            previewPitch = Mth.clamp(previewPitch + (float) dragY, -25f, 25f);    // small tilt only
            return true;
        }
        //? if >=1.21.11 {
        return super.mouseDragged(event, dragX, dragY);
        //?} else {
        /*return super.mouseDragged(mx, my, button, dragX, dragY);
         *///?}
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // While the Display list is open over them, the widgets underneath must not think they are hovered:
        // they would light up through it and, worse, put their own tooltip on screen over the list. Vanilla
        // decides hover from the mouse position it is handed, so hand it one that is over nothing at all.
        boolean overPopup = overDropdown(mouseX, mouseY);
        int hoverX = overPopup ? -1 : mouseX;
        int hoverY = overPopup ? -1 : mouseY;

        // The panel backdrops go down BEFORE the widgets, and that placement is not cosmetic: on the
        // retained pipeline every element is layered above the ones whose bounds it intersects
        // (GuiRenderState.findAppropriateNode → navigateToAboveHighestElementWithIntersectingBounds), so a
        // fill drawn after super would sit on top of every button inside the panel and tint it. Below super
        // it simply sits behind them, on all five versions — see the note in the other branch for why
        // 1.21.1/1.21.4, which draw vanilla's background dim from inside super.render, come out the same.
        renderBackdrops(g);

        // NB: do NOT call extractBackground here — on 26.x the vanilla
        // extractRenderStateWithTooltipAndSubtitles already extracts the (blurred) background
        // on its own stratum before this runs; a second call triggers "Can only blur once per frame".
        super.extractRenderState(g, hoverX, hoverY, partialTick);

        // colours must carry alpha: 1.21.2+ dropped Font's "alpha 0 means opaque" fallback, so a plain
        // 0xFFFFFF is fully transparent there — that is why the title and the counter were invisible on 26.x
        g.centeredText(this.font, this.title, guiLeft + contentW / 2, guiTop + 8, 0xFFFFFFFF);

        // The middle panel is the one backdrop that goes ON TOP of its widgets, and it is deliberate: the
        // tint over the Pose/Skin/Cape trio is what makes them read as sitting under the preview's glass.
        // Dimming those three widgets instead (AbstractWidget.setAlpha) was tried and reverted — from
        // 1.21.11 on the alpha reaches only the button sprite, never the label, so the face went dark while
        // the text stayed bright and the buttons came out looking yellow rather than recessed. Dimming the
        // label too would mean overriding renderWidget, whose signature splits at 26.1. The fill still goes
        // down before the doll, which is drawn later and so stays in front of it.
        g.fill(pvx1, pvy1, pvx2, pvy2, 0x66000000);
        for (Rule r : rules) g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + 1, 0xFF555555);
        for (Section s : sections) {
            if (s.centered()) g.centeredText(this.font, s.label(), s.x() + s.w() / 2, s.y(), 0xFFB0B0B0);
            else g.text(this.font, s.label(), s.x(), s.y(), 0xFFB0B0B0);
        }
        int hoveredEntry = renderEntryList(g, hoverX, hoverY);
        renderPreviewEntity(g, partialTick);
        // after the doll, so a zoomed-in model never sits on top of the box that is supposed to contain it
        renderFrames(g);
        if (previewUnavailable()) {
            g.centeredText(this.font, TOMBServer.text(NO_WORLD_KEY), (pvx1 + pvx2) / 2, (pvy1 + mvy2) / 2 - 4, 0xFFAAAAAA);
        }
        // last, so it covers the widgets it hangs over: super.extractRenderState above put those on an
        // earlier stratum, and everything drawn here goes on top of them. It gets the REAL cursor —
        // highlighting its own rows is the one thing that still has to follow the mouse in here.
        renderDropdown(g, mouseX, mouseY);

        // The trio floats inside the panel, so the panel's own hint would come up over it and describe
        // dragging and zooming while the cursor is pointing at a button. Same reasoning as the open
        // Display list: whatever is nearest the cursor owns the hover.
        boolean overPanel = overPreview(hoverX, hoverY) && !overOverlayControls(hoverX, hoverY);
        if (!overPanel) {
            previewHoverSince = 0L;
        } else if (previewHoverSince == 0L) {
            previewHoverSince = System.currentTimeMillis();
        }
        if (overPanel && System.currentTimeMillis() - previewHoverSince >= HOVER_DELAY_MS) {
            tooltip(g, TOMBServer.text("editor.hint.drag"), mouseX, mouseY);
        } else if (hoveredEntry >= 0) {
            // the grid shows what the item looks like; the name and id are what the hover is for
            tooltip(g, gridTooltip(hoveredEntry), mouseX, mouseY);
        }
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // While the Display list is open over them, the widgets underneath must not think they are hovered:
        // they would light up through it and, worse, put their own tooltip on screen over the list. Vanilla
        // decides hover from the mouse position it is handed, so hand it one that is over nothing at all.
        boolean overPopup = overDropdown(mouseX, mouseY);
        int hoverX = overPopup ? -1 : mouseX;
        int hoverY = overPopup ? -1 : mouseY;

        // The backdrops are NOT drawn here — see renderBackground below. Drawing them before super.render
        // put them in the framebuffer that the blur then sampled, and the blur smeared their edges.

        // NB: do NOT draw the background here. Where vanilla does it moved: up to 1.21.4 Screen.render()
        // starts with renderBackground, and from 1.21.11 the wrapper renderWithTooltipAndSubtitles does it
        // on its own stratum before render() is called at all. Either way it is already handled, and on
        // 1.21.11+ a second one is a hard error — "Can only blur once per frame", which is what crashed the
        // editor there while 1.21.1 (no such guard) and 26.x (other branch) were fine.
        super.render(g, hoverX, hoverY, partialTick);

        // opaque alpha kept explicit here too: 1.21.4/1.21.11 also need it, only 1.21.1 forces it for us
        g.drawCenteredString(this.font, this.title, guiLeft + contentW / 2, guiTop + 8, 0xFFFFFFFF);

        // the one backdrop that goes above its widgets — see the note in the 26.1 branch
        g.fill(pvx1, pvy1, pvx2, pvy2, 0x66000000);
        for (Rule r : rules) g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + 1, 0xFF555555);
        for (Section s : sections) {
            if (s.centered()) g.drawCenteredString(this.font, s.label(), s.x() + s.w() / 2, s.y(), 0xFFB0B0B0);
            else g.drawString(this.font, s.label(), s.x(), s.y(), 0xFFB0B0B0);
        }
        int hoveredEntry = renderEntryList(g, hoverX, hoverY);
        renderPreviewEntity(g, partialTick);
        // after the doll, so a zoomed-in model never sits on top of the box that is supposed to contain it
        renderFrames(g);
        if (previewUnavailable()) {
            g.drawCenteredString(this.font, TOMBServer.text(NO_WORLD_KEY), (pvx1 + pvx2) / 2, (pvy1 + mvy2) / 2 - 4, 0xFFAAAAAA);
        }
        // last, so it covers the widgets it hangs over — super.render above has already drawn those. It
        // gets the REAL cursor: highlighting its own rows is the one thing that still has to follow it.
        renderDropdown(g, mouseX, mouseY);

        // hover delay for the panel hint, matching vanilla widgets: the timer starts when the cursor
        // enters the panel and only resets when it leaves, so moving around inside does not cancel it
        // The trio floats inside the panel, so the panel's own hint would come up over it and describe
        // dragging and zooming while the cursor is pointing at a button. Same reasoning as the open
        // Display list: whatever is nearest the cursor owns the hover.
        boolean overPanel = overPreview(hoverX, hoverY) && !overOverlayControls(hoverX, hoverY);
        if (!overPanel) {
            previewHoverSince = 0L;
        } else if (previewHoverSince == 0L) {
            previewHoverSince = System.currentTimeMillis();
        }
        if (overPanel && System.currentTimeMillis() - previewHoverSince >= HOVER_DELAY_MS) {
            tooltip(g, TOMBServer.text("editor.hint.drag"), mouseX, mouseY);
        } else if (hoveredEntry >= 0) {
            // the grid shows what the item looks like; the name and id are what the hover is for
            tooltip(g, gridTooltip(hoveredEntry), mouseX, mouseY);
        }
    }
    *///?}

    //? if >=26.1 {
    //?} else {
    /*// Vanilla's background, then this screen's panel backdrops — the one spot that is after the blur and
    // still before the widgets.
    //
    // They used to go down at the top of render(), which is before super.render() calls this. That put them
    // into the framebuffer that renderBlurredBackground then sampled, so the blur smeared the panels
    // outwards and their borders came out fuzzy, while the preview frame — whose fill is drawn later —
    // stayed crisp. Appending them here keeps them out of the blur's input and still under every widget,
    // which is the whole reason they could not simply move after super.render().
    //
    // 26.1+ needs none of this: there the background is extracted on its own stratum before the screen is
    // asked to render at all, so the fills go down at the top of extractRenderState as before.
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        renderBackdrops(g);
    }
    *///?}

    /**
     * Draws the entries as a grid of item icons rather than a column of ids: which item an entry is about
     * is exactly what an icon says at a glance, whether it is a plain diamond sword, a server item with its
     * own texture or a resourcepack override — where a line of text says only {@code diamond_sword} three
     * times over. The id is still there, on hover.
     *
     * <p>Drawn by hand rather than through a vanilla list widget: those were rewritten for the retained-mode
     * GUI, so one would need a branch per version, while a fill and an item are the same call everywhere.
     * Cells outside the box are simply not drawn — with the scroll clamped to the row count there is
     * nothing to clip.
     *
     * @return the entry under the cursor, or -1
     */
    //? if >=26.1 {
    private int renderEntryList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        //?} else {
        /*private int renderEntryList(GuiGraphics g, int mouseX, int mouseY) {
         *///?}
        // The grid is short enough that "there is more below" has to be visible rather than discovered, so
        // it carries a scrollbar whenever it is not showing everything.
        int total = gridRowCount();
        if (total > listRows) {
            int trackX = lsx2 - 4;
            int trackH = lsy2 - lsy1 - 2;
            g.fill(trackX, lsy1 + 1, lsx2 - 1, lsy2 - 1, 0x66000000);
            int thumbH = Math.max(6, trackH * listRows / total);
            int thumbY = lsy1 + 1 + (trackH - thumbH) * listScroll / (total - listRows);
            g.fill(trackX, thumbY, lsx2 - 1, thumbY + thumbH, 0xFFA0A0A0);
        }
        int hovered = overList(mouseX, mouseY) ? entryAt(mouseX, mouseY) : -1;
        if (pickerOpen && pickerStacks.isEmpty()) {
            //? if >=26.1 {
            g.centeredText(this.font, TOMBServer.text("editor.picker.empty"),
                    (lsx1 + lsx2) / 2, lsy1 + 6, 0xFFB0B0B0);
            //?} else {
            /*g.drawCenteredString(this.font, TOMBServer.text("editor.picker.empty"),
                    (lsx1 + lsx2) / 2, lsy1 + 6, 0xFFB0B0B0);
            *///?}
            return -1;
        }
        // An item is drawn through its own model's GUI display transform, and plenty of them — block models,
        // and anything a resourcepack or plugin scales up — come out bigger than the 16px a slot allows.
        // Left alone they bleed into the neighbouring cells and out over the panel border. Scissor keeps
        // every cell inside the box; the same two calls exist unchanged on every version, and the retained
        // pipeline honours them for items too (GuiItemRenderState carries a scissorArea).
        g.enableScissor(lsx1 + 1, lsy1 + 1, lsx2 - 1, lsy2 - 1);
        for (int row = 0; row < listRows; row++) {
            for (int col = 0; col < listCols; col++) {
                int i = (listScroll + row) * listCols + col;
                if (i >= gridCount()) {
                    g.disableScissor();
                    return hovered;
                }
                int cx = gridX + col * ENTRY_CELL;
                int cy = gridY + row * ENTRY_CELL;
                // nothing is "selected" while picking, so only the cell under the cursor lights up
                if (!pickerOpen && i == index) g.fill(cx, cy, cx + ENTRY_CELL, cy + ENTRY_CELL, 0x66FFFFFF);
                else if (i == hovered) g.fill(cx, cy, cx + ENTRY_CELL, cy + ENTRY_CELL, 0x33FFFFFF);
                ItemStack stack = gridStack(i);
                if (stack.isEmpty()) {
                    // a blank or unregistered id has no icon to draw; the cell still has to be there to be
                    // selected and filled in, so it holds a placeholder
                    //? if >=26.1 {
                    g.centeredText(this.font, Component.literal("?"), cx + ENTRY_CELL / 2, cy + 5, 0xFF808080);
                    //?} else {
                    /*g.drawCenteredString(this.font, Component.literal("?"), cx + ENTRY_CELL / 2, cy + 5, 0xFF808080);
                     *///?}
                } else {
                    //? if >=26.1 {
                    g.item(stack, cx + 1, cy + 1);
                    //?} else {
                    /*g.renderItem(stack, cx + 1, cy + 1);
                     *///?}
                }
                // A switched-off entry is still in the grid, it just does nothing — say so by greying the
                // icon out behind a veil, which is the only way to dim an item that is drawn, not tinted.
                // Getting the veil ON TOP of the item splits with the GUI rewrite: from the retained-mode
                // pipeline on, an element overlapping one already submitted is auto-layered above it (see
                // GuiRenderState.findAppropriateNode), so a plain fill is enough. Before that the GUI is
                // immediate-mode and renderItem draws at z=150, which a z=0 fill loses the depth test to —
                // hence RenderType.guiOverlay(), the same one vanilla's slot highlight uses. That overload
                // only exists on the old side, so it cannot be the shared call.
                if (!pickerOpen && !tools().get(i).isEnabled()) {
                    //? if >=1.21.9 {
                    g.fill(cx, cy, cx + ENTRY_CELL, cy + ENTRY_CELL, 0x99101010);
                    //?} else {
                    /*g.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), cx, cy, cx + ENTRY_CELL, cy + ENTRY_CELL, 0x99101010);
                     *///?}
                }
            }
        }
        g.disableScissor();
        return hovered;
    }

    /**
     * The 1px border of every panel, drawn AFTER the doll so a zoomed-in model cannot sit on top of the box
     * that is meant to contain it.
     *
     * <p>Drawing order alone is not enough below 1.21.9. There the GUI is immediate-mode and
     * {@code renderEntityInInventory} puts the model at z=50 and deeper, so a border at z=0 loses the depth
     * test to it however late it is drawn — hence {@link #popupFill}, whose old arm goes through
     * {@code RenderType.guiOverlay()} and ignores depth entirely, the same layer vanilla's slot highlight
     * uses. From 1.21.9 on the doll is pre-rendered to a texture and blitted into the layer it was
     * submitted in, so an overlapping element added afterwards is auto-layered above it and a plain fill
     * does. Four fills rather than {@code renderOutline}, which does not exist from 26.1 and would draw at
     * z=0 on the old side anyway.
     */
    //? if >=26.1 {
    private void renderFrames(GuiGraphicsExtractor g) {
    //?} else {
    /*private void renderFrames(GuiGraphics g) {
    *///?}
        for (Frame f : frames) {
            popupFill(g, f.x1(), f.y1(), f.x2(), f.y1() + 1, 0xFF555555);
            popupFill(g, f.x1(), f.y2() - 1, f.x2(), f.y2(), 0xFF555555);
            popupFill(g, f.x1(), f.y1(), f.x1() + 1, f.y2(), 0xFF555555);
            popupFill(g, f.x2() - 1, f.y1(), f.x2(), f.y2(), 0xFF555555);
        }
    }

    /**
     * The backdrop behind the two SIDE panels. They used to be border-only so the world showed through, and
     * against a bright scene their 1px grey border was not visible at all.
     *
     * <p>Called before the widgets in both branches, so it never tints the buttons and sliders standing on
     * it. The middle panel is not in here: it paints its backdrop after its widgets on purpose, because the
     * tint over the preview trio is the look — see the note at that call. The nested entry-grid box is left
     * out because it already sits on the left panel's backdrop and a second fill would darken it twice.
     */
    //? if >=26.1 {
    private void renderBackdrops(GuiGraphicsExtractor g) {
        //?} else {
        /*private void renderBackdrops(GuiGraphics g) {
         *///?}
        for (Frame f : frames) {
            if (f.filled()) g.fill(f.x1(), f.y1(), f.x2(), f.y2(), 0x66000000);
        }
    }

    /**
     * A tooltip this screen puts up itself, split into lines first.
     *
     * <p>The single-Component overloads take the text as ONE line — they never split it — so a {@code \n}
     * coming out of a lang file was drawn as a missing-glyph box instead of a line break. Every tooltip
     * attached to a widget escapes this because {@code Tooltip.create} splits for us; these two (the
     * preview hint and the entry hover) do not go through a widget, so they have to split for themselves.
     * {@code Font.split} returns the same type on all five targets, so only the draw call is branched.
     */
    //? if >=26.1 {
    private void tooltip(GuiGraphicsExtractor g, Component text, int mouseX, int mouseY) {
        g.setTooltipForNextFrame(this.font, this.font.split(text, TOOLTIP_W), mouseX, mouseY);
    }
    //?} else {
    /*private void tooltip(GuiGraphics g, Component text, int mouseX, int mouseY) {
        //? if >=1.21.11 {
        g.setTooltipForNextFrame(this.font, this.font.split(text, TOOLTIP_W), mouseX, mouseY);
        //?} else {
        /^g.renderTooltip(this.font, this.font.split(text, TOOLTIP_W), mouseX, mouseY);
        ^///?}
    }
    *///?}

    /**
     * A fill belonging to a pop-up, i.e. one that has to stay above everything including the doll.
     *
     * <p>Plain {@code fill} is enough from the retained pipeline on, where an element is auto-layered above
     * whatever it intersects. Below that the GUI is immediate-mode and the preview entity is drawn at
     * z=150, so a z=0 fill loses the depth test to it — which is exactly what was reported on 1.21.1: the
     * picker's items showed (items are drawn at z=150 too) while its backdrop and border vanished behind
     * the doll. {@code RenderType.guiOverlay()} is the layer vanilla's own slot highlight uses for this,
     * and the overload taking one only exists on that side of the split.
     */
    //? if >=26.1 {
    private void popupFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int colour) {
        g.fill(x1, y1, x2, y2, colour);
    }
    //?} else if >=1.21.9 {
    /*private void popupFill(GuiGraphics g, int x1, int y1, int x2, int y2, int colour) {
        g.fill(x1, y1, x2, y2, colour);
    }
    *///?} else {
    /*private void popupFill(GuiGraphics g, int x1, int y1, int x2, int y2, int colour) {
        g.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), x1, y1, x2, y2, colour);
    }
    *///?}

    /**
     * The open Display list. Not a widget: it has to paint over the rows under it, and both render branches
     * call this after the widgets have gone down, so it lands on top of them without a stratum of its own.
     */
    //? if >=26.1 {
    private void renderDropdown(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        //?} else {
        /*private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
         *///?}
        if (!displayOpen) return;
        ToolEntry entry = selected();
        if (entry == null) return;
        String current = ConfigHandler.parseDisplayContext(entry.getDisplayContext()).getSerializedName();
        popupFill(g, ddx1, ddy1, ddx2, ddy2, 0xF0100010);
        ItemDisplayContext[] values = ItemDisplayContext.values();
        for (int i = 0; i < values.length; i++) {
            int ry = ddy1 + 1 + i * DROP_ROW_H;
            boolean over = mouseX >= ddx1 && mouseX < ddx2 && mouseY >= ry && mouseY < ry + DROP_ROW_H;
            boolean chosen = values[i].getSerializedName().equals(current);
            if (over) popupFill(g, ddx1 + 1, ry, ddx2 - 1, ry + DROP_ROW_H, 0x66FFFFFF);
            else if (chosen) popupFill(g, ddx1 + 1, ry, ddx2 - 1, ry + DROP_ROW_H, 0x33FFFFFF);
            Component label = Component.literal(values[i].getSerializedName());
            //? if >=26.1 {
            g.text(this.font, label, ddx1 + 5, ry + 2, chosen ? 0xFFFFFFFF : 0xFFB0B0B0);
            //?} else {
            /*g.drawString(this.font, label, ddx1 + 5, ry + 2, chosen ? 0xFFFFFFFF : 0xFFB0B0B0);
             *///?}
        }
        // border last, so it is not painted over by the highlighted row
        popupFill(g, ddx1, ddy1, ddx2, ddy1 + 1, 0xFF808080);
        popupFill(g, ddx1, ddy2 - 1, ddx2, ddy2, 0xFF808080);
        popupFill(g, ddx1, ddy1, ddx1 + 1, ddy2, 0xFF808080);
        popupFill(g, ddx2 - 1, ddy1, ddx2, ddy2, 0xFF808080);
    }

    /**
     * Lazily builds the stand-in. It is its own player, not a mirror of the real one: the only things it
     * borrows are the profile (so the skin and cape resolve through the tab list) and the enabled skin
     * layers, each read on demand rather than copied. Everything else — pose, flags, game type — is set
     * here or overridden, and it never ticks, so nothing about the real player leaks into the preview.
     */
    private RemotePlayer dummy() {
        if (dummy == null && minecraft != null && minecraft.level != null && minecraft.player != null) {
            // Profile picks the skin: with no PlayerInfo behind it (the dummy is not in the tab list)
            // AbstractClientPlayer.getSkin() falls back to DefaultPlayerSkin.get(uuid), so a synthetic
            // profile is all a vanilla default skin needs — and those carry no cape either.
            GameProfile profile = switch (previewSubject) {
                case YOU -> minecraft.player.getGameProfile();
                case STEVE -> new GameProfile(defaultSkinUuid(false), "Steve");
                case ALEX -> new GameProfile(defaultSkinUuid(true), "Alex");
            };
            // the cape toggle is applied by overriding the very check the renderer runs to fill
            // AvatarRenderState.showCape, so it works the same on every version
            dummy = new RemotePlayer(minecraft.level, profile) {
                @Override
                public boolean isModelPartShown(PlayerModelPart part) {
                    if (part == PlayerModelPart.CAPE && !showCape) return false;
                    // Read the enabled skin layers off the real player directly. Copying their synced data
                    // wholesale was the old way and it dragged in everything else stored there too — pose
                    // and entity flags included, so an invisibility potion made the doll disappear.
                    if (minecraft != null && minecraft.player != null) {
                        return minecraft.player.isModelPartShown(part);
                    }
                    return super.isModelPartShown(part);
                }

                @Override
                public boolean isSpectator() {
                    // The stand-in borrows the real player's profile, so in spectator mode it inherited
                    // their game type — and the renderer answers that by hiding every body part except the
                    // head. This is a preview doll, never a real participant, so it is never a spectator.
                    return false;
                }
            };
            // 26.x extractEntity → ItemModelResolver.updateForLiving reads getId(), which throws if the id
            // was never assigned; give it the real player's id (dummy is never added to the level, so no clash)
            dummy.setId(minecraft.player.getId());
            // lets the back-tool layer tell this stand-in apart from the real player, whose uuid it shares
            TOMBClient.PREVIEW_DUMMY = dummy;
            dummy.setCustomNameVisible(false);

            // A NAME TAG has to be kept off the doll, and only the old arm needs the work.
            //
            // From 1.21.9 the doll goes through extractEntity + a picture-in-picture element, and that path
            // never draws a name tag — nothing to do. Below that, EntityRenderer.render ends with
            // `if (shouldShowName(entity)) renderNameTag(...)`, and every escape route is closed for us:
            // Player.shouldShowName() is a hardcoded `return true`, so setCustomNameVisible(false) above
            // does nothing for a player, and LivingEntityRenderer.shouldShowName's own get-out is
            // `entity == minecraft.player` — which is exactly how vanilla's inventory player escapes, and
            // exactly what a separate stand-in cannot be. What is left is the distance test at the top of
            // that method: `distanceToSqr(entity) >= (isDiscrete() ? 32 : 64)²` returns false straight away.
            //
            // So park it past that radius. NAME_TAG_RANGE is the 64 from the check itself, doubled for
            // slack; the old code used 4096, which was that threshold's SQUARE pasted in as a distance.
            //? if >=1.21.9 {
            //?} else {
            /*dummy.setPos(minecraft.player.getX() + NAME_TAG_RANGE * 2.0,
                    minecraft.player.getY(), minecraft.player.getZ());
            *///?}
            //
            // The cape pose is derived from (cloak anchor − previous-tick position):
            //   dY = cloakY − lerp(partial, yo, getY());  capeFlap = clamp(dY * 10, -6, 32)
            //   capeLean = (dX * sin(bodyRot) + dZ * cos(bodyRot)) * 100
            // Both halves must agree or each term explodes: capeFlap pins to its +32 cap (the cape sticks
            // straight out over the shoulders) and capeLean goes wild as the model turns. Only the DELTAS
            // matter, never the absolute position, so seeding them at the origin is as good as anywhere.
            // xo/yo/zo stay 0 until something ticks, hence setOldPosAndRot().
            dummy.setOldPosAndRot();
            //? if >=1.21.11 {
            // 1.21.11+ moved the cloak anchor into ClientAvatarState; tick it twice at the body position
            // with zero movement so the anchor and its previous value both settle onto the body.
            Vec3 cloakPos = dummy.position();
            dummy.avatarState().tick(cloakPos, Vec3.ZERO);
            dummy.avatarState().tick(cloakPos, Vec3.ZERO);
            //?} else {
            /*// pre-1.21.11 the anchor lives on Player itself (CapeLayer reads it with the same formula)
            dummy.xCloak = dummy.xCloakO = dummy.getX();
            dummy.yCloak = dummy.yCloakO = dummy.getY();
            dummy.zCloak = dummy.zCloakO = dummy.getZ();
            *///?}
        }
        return dummy;
    }

    //? if >=26.1 {
    private void renderPreviewEntity(GuiGraphicsExtractor g, float partialTick) {
        //?} else {
        /*private void renderPreviewEntity(GuiGraphics g, float partialTick) {
         *///?}
        if (minecraft == null || minecraft.player == null) return;

        LivingEntity entity = dummy();
        if (entity == null) return;

        int cx = (pvx1 + pvx2) / 2;
        int cy = (pvy1 + mvy2) / 2;
        int boxW = pvx2 - pvx1;
        int boxH = mvy2 - pvy1;
        // Same formula on every version: in the box-based path the model is scaled by guiScale * scale into
        // a box-sized texture, so `scale` is absolute GUI px exactly like the old renderEntityInInventory
        // took. The height ratio is still vanilla's own inventory preview (a 49x70 box at scale 30); the
        // WIDTH ratio was raised 3/5 → 4/5 and the cap 80 → 100, which is what makes the doll taller. The
        // panel is far wider than a player needs — at scale 96 the model is ~58px across a 134px box — so
        // 3/5 was pinning the size on a limit that was not really there, and height now decides.
        float fitted = Mth.clamp(Math.min(boxH * 3f / 7f, boxW * 4f / 5f), 45f, 100f);
        float scale = fitted * previewZoom;

        if (previewPose == PreviewPose.SNEAKING) {
            entity.setPose(Pose.CROUCHING);
            entity.setShiftKeyDown(true);
        } else {
            entity.setPose(Pose.STANDING);
            entity.setShiftKeyDown(false);
        }

        // yaw turns the body (0 = back to camera), pitch tilts via xRot + the camera quaternion
        entity.yBodyRot = previewYaw;
        entity.yBodyRotO = previewYaw;
        entity.setYRot(previewYaw);
        entity.setXRot(-previewPitch);
        entity.yHeadRot = previewYaw;
        entity.yHeadRotO = previewYaw;
        // Every rotation the renderer reads is interpolated between its previous-tick value and its current
        // one, and partialTick sweeps 0→1 within every tick. The yaws above set both halves by hand, but
        // xRot has no public "O" setter and setXRot writes only the current value — so xRotO stayed at the 0
        // recorded when the dummy was built, and state.xRot = getXRot(partialTick) swung between 0 and the
        // set pitch sixty times a second. That is the head twitching, and why tilting up or down made it
        // obvious. setOldPosAndRot() copies pos and BOTH rotations onto their previous-tick fields, so every
        // interpolation is a no-op; it costs nothing here and re-seeds xo/yo/zo for the cape into the
        // bargain. Present unbranched on all five targets (javap-checked).
        entity.setOldPosAndRot();

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cam = new Quaternionf().rotateX(previewPitch * ((float) Math.PI / 180f));
        pose.mul(cam);

        Vector3f translate = new Vector3f(0.0F, entity.getBbHeight() / 2.0F, 0.0F);

        // The old arm needs two things the box-based paths get for free from rendering into a texture of
        // their own. Both were reported on 1.21.1 against 26.2:
        //
        //  * DEPTH. renderEntityInInventory places the model at z=50 and then scales its depth by the same
        //    factor as its width — `scale(scale, scale, -scale)`. At the sizes this screen uses (up to 100,
        //    times a 2.5 zoom) a doll tilted by the drag reaches far behind the GUI plane, and everything
        //    past it loses the depth test to the panel drawn at z≈0. That is the body disappearing when the
        //    mouse is dragged downwards. Vanilla's 50 is sized for the inventory's scale 30; push forward
        //    by one scale's worth instead, which covers the ±25° pitch and still sits under the tooltip
        //    layer at 400.
        //  * CLIPPING. Without it a zoomed doll paints across the whole screen; the box-based paths simply
        //    cannot, because their texture is the box. Scissor to the same box so all versions agree.
        //? if >=26.1 {
        net.minecraft.client.renderer.entity.state.EntityRenderState state =
                minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
        applyCrouchState(state);
        g.entity(state, scale, translate, pose, cam, pvx1, pvy1, pvx2, mvy2);
        //?} else if >=1.21.9 {
        /*net.minecraft.client.renderer.entity.state.EntityRenderState state =
                minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
        applyCrouchState(state);
        // Light the doll ourselves on this arm only. The extracted state carries the light of whatever
        // block the entity is standing on, and a stand-in that is never in the world has no business
        // taking one — it came out night-dark. From 26.1 GuiGraphicsExtractor.entity() overwrites
        // lightCoords with full bright itself (checked on 26.1 and 26.2), and below 1.21.9
        // renderEntityInInventory passes full bright to the dispatcher, so only 1.21.9..1.21.11 is left
        // holding it, and vanilla's own InventoryScreen sets it right here too.
        state.lightCoords = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        g.submitEntityRenderState(state, scale, translate, pose, cam, pvx1, pvy1, pvx2, mvy2);
        *///?} else {
        /*g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, scale);
        g.enableScissor(pvx1 + 1, pvy1 + 1, pvx2 - 1, mvy2);
        InventoryScreen.renderEntityInInventory(g, cx, cy, (int) scale, translate, pose, cam, entity);
        g.disableScissor();
        g.pose().popPose();
        *///?}
    }

    //? if >=1.21.9 {

    /**
     * Forces the crouch flag onto the extracted state. HumanoidModel keys the whole sneak pose off
     * {@code HumanoidRenderState.isCrouching} (body xRot 0.5, both arms xRot +0.4, legs/head/body offsets),
     * and that flag is filled from {@code entity.isCrouching()} → {@code hasPose(CROUCHING)} during extract —
     * which does not survive on a dummy that never ticks, leaving the arms hanging straight. Vanilla's own
     * {@code InventoryScreen.extractEntityInInventoryFollowsMouse} post-edits the extracted state the same
     * way (bodyRot/yRot/xRot/scale), so setting it here is the idiomatic fix rather than a hack.
     */
    private void applyCrouchState(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        if (state instanceof net.minecraft.client.renderer.entity.state.HumanoidRenderState humanoidRenderState) {
            humanoidRenderState.isCrouching = previewPose == PreviewPose.SNEAKING;
        }
    }
    //?}

    /**
     * Finds a UUID whose vanilla default skin is actually Steve or Alex. The default set holds every name in
     * BOTH arm models (slim/alex … slim/zuri, wide/alex … wide/zuri) and the hash that picks one differs
     * across versions, so probing by arm model alone is not enough — it happily returns wide/alex for
     * "Steve". Probe the texture path instead — both eras contain the {@code model/name} tail. Falls back to
     * an arm-model match if those asset names ever change.
     */
    private static UUID defaultSkinUuid(boolean alex) {
        UUID cached = alex ? ALEX_UUID : STEVE_UUID;
        if (cached != null) return cached;
        String wanted = alex ? "slim/alex" : "wide/steve";
        UUID fallback = null;
        for (int i = 0; i < 512; i++) {
            UUID candidate = new UUID(0L, i);
            var skin = DefaultPlayerSkin.get(candidate);
            // 1.21.11 moved PlayerSkin (client.resources → world.entity.player) and replaced the plain
            // texture id with a ClientAsset. ClientAsset.Texture is an interface, so its toString() is
            // whatever the implementation prints — go through texturePath() rather than trusting it.
            //? if >=1.21.11 {
            String path = skin.body().texturePath().toString();
            //?} else {
            /*String path = skin.texture().toString();
             *///?}
            if (path.contains(wanted)) {
                if (alex) ALEX_UUID = candidate;
                else STEVE_UUID = candidate;
                return candidate;
            }
            // the arm-model enum is PlayerSkin.Model before 1.21.11 and PlayerModelType from it on, so it is
            // compared by constant name — those stayed SLIM/WIDE — rather than by importing either type
            if (fallback == null && skin.model().name().equals("SLIM") == alex) fallback = candidate;
        }
        return fallback != null ? fallback : new UUID(0L, alex ? 1L : 0L);
    }

    private static UUID STEVE_UUID;
    private static UUID ALEX_UUID;

    private enum PreviewSubject {
        YOU("editor.subject.you"),
        STEVE("editor.subject.steve"),
        ALEX("editor.subject.alex");

        private final String key;

        PreviewSubject(String key) {
            this.key = key;
        }
    }

    private enum PreviewPose {
        STANDING,
        SNEAKING
    }

    @Override
    public void onClose() {
        // Esc leaves without committing: whatever was not saved goes back to the last saved state.
        config.setEnableMod(savedEnableMod);
        config.setRenderWithCape(savedRenderWithCape);
        config.setRenderWithElytra(savedRenderWithElytra);
        config.setTools(new ArrayList<>(snapshot));
        if (minecraft != null) TOMBClient.setScreen(parent);
        else super.onClose();
    }

    @Override
    public void removed() {
        TOMBClient.PREVIEW_PLAYER = null;
        TOMBClient.PREVIEW_STACK = ItemStack.EMPTY;
        TOMBClient.PREVIEW_ENTRY = null;
        TOMBClient.PREVIEW_DUMMY = null;
        dummy = null;
    }

    private static List<ToolEntry> deepCopy(List<ToolEntry> src) {
        List<ToolEntry> out = new ArrayList<>();
        for (ToolEntry e : src) {
            out.add(new ToolEntry(e.getItemId(), e.getComponents(),
                    e.isMatchName(), e.isMatchModelData(), e.isMatchItemModel(),
                    e.isApplyToSimilar(), e.isEnabled(), e.isFlip(),
                    e.getDisplayContext(), e.getScale(), e.getAngleX(), e.getAngleY(), e.getAngleZ(),
                    e.getOffsetX(), e.getOffsetY(), e.getOffsetZ()));
        }
        return out;
    }

    private record ResetTarget(AbstractWidget widget, Runnable restoreSaved, Runnable restoreDefaults,
                               boolean inResetEntry) {
    }

    /**
     * A heading drawn above a group of a column; centred inside the given width, or left-aligned at x.
     */
    private record Section(Component label, int x, int y, int w, boolean centered) {
    }

    /**
     * The 1px border of a panel, and whether it carries a backdrop behind it.
     */
    private record Frame(int x1, int y1, int x2, int y2, boolean filled) {
    }

    /**
     * A 1px horizontal divider.
     */
    private record Rule(int x, int y, int w) {
    }

    private static final class FloatSlider extends AbstractSliderButton {
        private final float min;
        private final float max;
        private final Component label;
        private final boolean intFmt;
        private final Supplier<Float> getter;
        private final Consumer<Float> setter;

        FloatSlider(int x, int y, int w, int h, Component label, float min, float max, boolean intFmt,
                    Supplier<Float> getter, Consumer<Float> setter) {
            super(x, y, w, h, Component.empty(), clamp01((getter.get() - min) / (max - min)));
            this.min = min;
            this.max = max;
            this.label = label;
            this.intFmt = intFmt;
            this.getter = getter;
            this.setter = setter;
            updateMessage();
        }

        private static double clamp01(double v) {
            return v < 0 ? 0 : Math.min(v, 1);
        }

        /**
         * One scroll notch = 1 unit for angles, 0.01 for offsets/scale (the least-significant shown digit).
         */
        void nudge(double scrollY) {
            if (scrollY == 0) return;
            float stepVal = intFmt ? 1f : 0.01f;
            double delta = (scrollY > 0 ? stepVal : -stepVal) / (max - min);
            this.value = Mth.clamp(this.value + delta, 0.0, 1.0);
            applyValue();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String v = intFmt ? String.format("%.0f", getter.get()) : String.format("%.2f", getter.get());
            setMessage(label.copy().append(Component.literal(" " + v)));
        }

        @Override
        protected void applyValue() {
            // Store the two decimals the slider shows, and that a shared profile carries. Otherwise the
            // config keeps 2.3360806 where the same setting imported from a profile reads 2.34, and the
            // two never compare equal for all they are the same setting.
            float raw = min + (float) this.value * (max - min);
            setter.accept(Math.round(raw * 100f) / 100f);
        }
    }
}
