package me.zipestudio.tomb.client.screen.editor;

import com.mojang.authlib.GameProfile;
import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PreviewPanel {

    private static final float ZOOM_MIN = 0.6f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.1f;
    private static final float PITCH_LIMIT = 25f;
    private static final int COLOUR_GLASS = 0x66000000;
    private static final int COLOUR_NOTICE = 0xFFAAAAAA;

    public enum Subject {
        YOU("editor.subject.you"),
        STEVE("editor.subject.steve"),
        ALEX("editor.subject.alex");

        private final String key;

        Subject(String key) {
            this.key = key;
        }

        public Component label() {
            return TOMBServer.text(key);
        }
    }

    public enum CameraPose {
        STANDING,
        SNEAKING
    }

    private final Minecraft minecraft;

    private int left;
    private int top;
    private int right;
    private int bottom;
    private boolean visible = true;

    private float yaw;
    private float pitch;
    private float zoom = 1f;
    private long hoverSince;

    private CameraPose pose = CameraPose.STANDING;
    private Subject subject = Subject.YOU;
    private boolean showCape = true;

    private RemotePlayer dummy;

    private final List<AbstractWidget> overlayControls = new ArrayList<>();

    public PreviewPanel(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void layout(EditorLayout layout) {
        this.left = layout.previewLeft;
        this.top = layout.previewTop;
        this.right = layout.previewRight;
        this.bottom = layout.previewBottom;
        this.visible = layout.previewVisible;
        this.overlayControls.clear();
    }

    public void buildControls(Consumer<AbstractWidget> sink, Runnable onChanged) {
        if (!visible) return;
        int x = left + EditorLayout.OVERLAY_PAD;
        int y = bottom - EditorLayout.OVERLAY_PAD - (3 * EditorLayout.OVERLAY_HEIGHT + 4);

        EditorLayout.Slot poseSlot = slot(x, y);
        Button poseButton = Button.builder(TOMBServer.text("editor.pose"), ignored -> {
            CameraPose[] poses = CameraPose.values();
            pose = poses[(pose.ordinal() + 1) % poses.length];
            dummy = null;
            onChanged.run();
        }).bounds(poseSlot.x(), poseSlot.y(), poseSlot.width(), poseSlot.height()).build();
        sink.accept(overlay(EditorWidgets.tip(poseButton, TOMBServer.text("editor.pose.tooltip"))));

        y += EditorLayout.OVERLAY_HEIGHT + 2;
        CycleButton<Subject> subjectButton = EditorWidgets.cycle(slot(x, y), "editor.subject",
                Subject.values(), subject, Subject::label, value -> {
                    subject = value;
                    dummy = null;
                    onChanged.run();
                });
        sink.accept(overlay(EditorWidgets.tip(subjectButton, TOMBServer.text("editor.subject.tooltip"))));

        y += EditorLayout.OVERLAY_HEIGHT + 2;
        AbstractWidget capeButton = EditorWidgets.plainFlag(slot(x, y), "editor.cape", showCape, value -> {
            showCape = value;
            dummy = null;
        });
        sink.accept(overlay(capeButton));
    }

    private static EditorLayout.Slot slot(int x, int y) {
        return new EditorLayout.Slot(x, y, EditorLayout.OVERLAY_WIDTH, EditorLayout.OVERLAY_HEIGHT);
    }

    private <T extends AbstractWidget> T overlay(T widget) {
        overlayControls.add(widget);
        return widget;
    }

    public boolean over(double mouseX, double mouseY) {
        return visible && mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private boolean overOverlayControls(double mouseX, double mouseY) {
        for (AbstractWidget widget : overlayControls) {
            if (widget.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    public boolean overModel(double mouseX, double mouseY) {
        return over(mouseX, mouseY) && !overOverlayControls(mouseX, mouseY);
    }

    public void resetCamera() {
        yaw = 0f;
        pitch = 0f;
        zoom = 1f;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!over(mouseX, mouseY)) return false;
        zoom = Mth.clamp(zoom + (float) Math.signum(scrollY) * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, double dragX, double dragY) {
        if (!over(mouseX, mouseY)) return false;
        yaw -= (float) dragX;
        pitch = Mth.clamp(pitch - (float) dragY, -PITCH_LIMIT, PITCH_LIMIT);
        return true;
    }

    public boolean hintDue(int mouseX, int mouseY) {
        if (!overModel(mouseX, mouseY)) {
            hoverSince = 0L;
            return false;
        }
        if (hoverSince == 0L) hoverSince = System.currentTimeMillis();
        return System.currentTimeMillis() - hoverSince >= EditorWidgets.HOVER_DELAY_MS;
    }

    public boolean unavailable() {
        return minecraft == null || minecraft.level == null || minecraft.player == null;
    }

    public void forget() {
        dummy = null;
    }

    public java.util.UUID previewUuid() {
        RemotePlayer shown = dummyPlayer();
        return (shown != null ? shown : minecraft.player).getUUID();
    }

    private RemotePlayer dummyPlayer() {
        if (dummy != null || unavailable()) return dummy;

        GameProfile profile = switch (subject) {
            case YOU -> minecraft.player.getGameProfile();
            case STEVE -> new GameProfile(PreviewCompat.defaultSkinUuid(false), "Steve");
            case ALEX -> new GameProfile(PreviewCompat.defaultSkinUuid(true), "Alex");
        };
        dummy = new PreviewDummy(minecraft.level, profile);
        dummy.setId(minecraft.player.getId());
        TOMBClient.PREVIEW_DUMMY = dummy;
        dummy.setCustomNameVisible(false);
        PreviewCompat.hideNameTag(dummy, minecraft.player);
        PreviewCompat.seedCape(dummy);
        return dummy;
    }

    private final class PreviewDummy extends RemotePlayer {

        private PreviewDummy(ClientLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isModelPartShown(PlayerModelPart part) {
            if (part == PlayerModelPart.CAPE && !showCape) return false;
            if (minecraft.player != null) return minecraft.player.isModelPartShown(part);
            return super.isModelPartShown(part);
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

    }

    public void drawGlass(EditorPainter painter) {
        if (!visible) return;
        painter.fill(left, top, right, bottom, COLOUR_GLASS);
    }

    public void drawNotice(EditorPainter painter) {
        if (!visible) return;
        painter.centeredText(TOMBServer.text("editor.hint.no_world"), (left + right) / 2,
                (top + bottom) / 2 - 4, COLOUR_NOTICE);
    }

    public void render(EditorPainter painter, float partialTick) {
        if (!visible || unavailable()) return;
        LivingEntity entity = dummyPlayer();
        if (entity == null) return;

        int boxWidth = right - left;
        int boxHeight = bottom - top;
        float fitted = Mth.clamp(Math.min(boxHeight * 3f / 7f, boxWidth * 4f / 5f), 45f, 100f);
        float scale = fitted * zoom;

        boolean crouching = pose == CameraPose.SNEAKING;
        entity.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
        entity.setShiftKeyDown(crouching);

        entity.yBodyRot = yaw;
        entity.yBodyRotO = yaw;
        entity.setYRot(yaw);
        entity.setXRot(-pitch);
        entity.yHeadRot = yaw;
        entity.yHeadRotO = yaw;
        entity.setOldPosAndRot();

        Quaternionf modelPose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * ((float) Math.PI / 180f));
        modelPose.mul(camera);
        Vector3f translate = new Vector3f(0.0F, entity.getBbHeight() / 2.0F, 0.0F);

        painter.entity(entity, partialTick, crouching, scale, translate, modelPose, camera,
                left, top, right, bottom);
    }
}
