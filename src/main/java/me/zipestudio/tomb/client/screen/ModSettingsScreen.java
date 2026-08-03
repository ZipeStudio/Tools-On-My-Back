package me.zipestudio.tomb.client.screen;

import me.zipestudio.tomb.TOMBServer;
import me.zipestudio.tomb.client.TOMBClient;
import me.zipestudio.tomb.config.LeafyConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Duration;

/**
 * The handful of settings that are about the mod rather than about one item. They lived in the editor for
 * a while and were not worth the room: three switches nobody touches twice were taking a third of the
 * panel the preview needed.
 *
 * <p>Built out of widgets alone — no render override — so it needs no branch for the retained-mode GUI
 * rewrite at 26.1. Values are written straight into the live config; the editor this returns to owns
 * saving, so its Save and Cancel cover these as well.
 */
public class ModSettingsScreen extends Screen {

    private static final int ROW_W = 220;
    private static final int ROW_H = 20;
    private static final int STEP = 24;

    private final Screen parent;
    private final LeafyConfig config;

    public ModSettingsScreen(Screen parent) {
        super(TOMBServer.text("settings.title"));
        this.parent = parent;
        this.config = LeafyConfig.getInstance();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 58;

        addRenderableWidget(new StringWidget(cx - ROW_W / 2, y, ROW_W, 18, this.title, this.font));

        y += STEP + 4;
        addRenderableWidget(tip(CycleButton.onOffBuilder(config.isEnableMod())
                        .create(cx - ROW_W / 2, y, ROW_W, ROW_H, TOMBServer.text("editor.mod_enabled"),
                                (btn, val) -> config.setEnableMod(val)),
                TOMBServer.text("editor.mod_enabled.tooltip")));

        y += STEP;
        addRenderableWidget(tip(CycleButton.onOffBuilder(config.isRenderWithCape())
                        .create(cx - ROW_W / 2, y, ROW_W, ROW_H, TOMBServer.text("editor.under_cape"),
                                (btn, val) -> config.setRenderWithCape(val)),
                TOMBServer.text("editor.under_cape.tooltip")));

        y += STEP;
        addRenderableWidget(tip(CycleButton.onOffBuilder(config.isRenderWithElytra())
                        .create(cx - ROW_W / 2, y, ROW_W, ROW_H, TOMBServer.text("editor.under_elytra"),
                                (btn, val) -> config.setRenderWithElytra(val)),
                TOMBServer.text("editor.under_elytra.tooltip")));

        y += STEP + 8;
        addRenderableWidget(Button.builder(TOMBServer.text("editor.done"), b -> onClose())
                .bounds(cx - ROW_W / 2, y, ROW_W, ROW_H).build());
    }

    private static <T extends AbstractWidget> T tip(T widget, Component text) {
        widget.setTooltip(Tooltip.create(text));
        widget.setTooltipDelay(Duration.ofMillis(500L));
        return widget;
    }

    @Override
    public void onClose() {
        TOMBClient.setScreen(parent);
    }
}
