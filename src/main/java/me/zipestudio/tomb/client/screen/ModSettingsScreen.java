package me.zipestudio.tomb.client.screen;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.client.screen.editor.EditorLayout.Slot;
import me.zipestudio.tomb.client.screen.editor.EditorWidgets;
import me.zipestudio.tomb.config.LeafyConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;

public class ModSettingsScreen extends Screen {

    private static final int ROW_WIDTH = 220;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 24;

    private final Screen parent;
    private final Runnable onLeave;
    private final LeafyConfig config;

    public ModSettingsScreen(Screen parent, Runnable onLeave) {
        super(TOMBServer.text("settings.title"));
        this.parent = parent;
        this.onLeave = onLeave;
        this.config = LeafyConfig.getInstance();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - ROW_WIDTH / 2;
        int y = this.height / 2 - 58;

        addRenderableWidget(new StringWidget(left, y, ROW_WIDTH, 18, this.title, this.font));

        y += ROW_STEP + 4;
        addRenderableWidget(EditorWidgets.plainFlag(row(left, y), "editor.mod_enabled",
                config.isEnableMod(), config::setEnableMod));

        y += ROW_STEP;
        addRenderableWidget(EditorWidgets.plainFlag(row(left, y), "editor.under_cape",
                config.isRenderWithCape(), config::setRenderWithCape));

        y += ROW_STEP;
        addRenderableWidget(EditorWidgets.plainFlag(row(left, y), "editor.under_elytra",
                config.isRenderWithElytra(), config::setRenderWithElytra));

        y += ROW_STEP + 8;
        addRenderableWidget(Button.builder(TOMBServer.text("editor.done"), ignored -> onClose())
                .bounds(left, y, ROW_WIDTH, ROW_HEIGHT).build());
    }

    private static Slot row(int left, int y) {
        return new Slot(left, y, ROW_WIDTH, ROW_HEIGHT);
    }

    @Override
    public void onClose() {
        onLeave.run();
        TOMBClient.setScreen(parent);
    }
}
