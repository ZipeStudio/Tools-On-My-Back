package me.zipestudio.tomb.client.screen.editor;

import me.zipestudio.tomb.client.screen.editor.EntryProperty.FloatProperty;
import me.zipestudio.tomb.config.ToolEntry;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
//? if <1.20.2 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
*///?}

import java.util.function.Supplier;

public final class FloatSlider extends AbstractSliderButton {

    //? if <1.20.2 {
    /*private static final net.minecraft.resources.Identifier TEXTURE =
            new net.minecraft.resources.Identifier("textures/gui/slider.png");
    private static final int HANDLE_WIDTH = 8;
    private static final int BORDER_X = 20;
    private static final int BORDER_Y = 4;
    private static final int TEXTURE_W = 200;
    private static final int TEXTURE_H = 20;
    private static final int TEXT_MARGIN = 2;
    *///?}

    private final FloatProperty property;
    private final Supplier<ToolEntry> selection;
    private final Runnable onChanged;

    FloatSlider(EditorLayout.Slot slot, FloatProperty property, Supplier<ToolEntry> selection,
                Runnable onChanged) {
        super(slot.x(), slot.y(), slot.width(), slot.height(), Component.empty(),
                normalise(property, selection.get()));
        this.property = property;
        this.selection = selection;
        this.onChanged = onChanged;
        updateMessage();
    }

    private static double normalise(FloatProperty property, ToolEntry entry) {
        if (entry == null) return 0.0;
        double fraction = (property.getter().get(entry) - property.min()) / (property.max() - property.min());
        return Mth.clamp(fraction, 0.0, 1.0);
    }

    void nudge(double scrollY) {
        if (scrollY == 0) return;
        float step = property.wholeNumbers() ? 1f : 0.01f;
        double delta = (scrollY > 0 ? step : -step) / (property.max() - property.min());
        this.value = Mth.clamp(this.value + delta, 0.0, 1.0);
        applyValue();
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        ToolEntry entry = selection.get();
        float current = entry == null ? property.min() : property.getter().get(entry);
        String shown = property.wholeNumbers()
                ? String.format("%.0f", current)
                : String.format("%.2f", current);
        setMessage(property.label().append(Component.literal(" " + shown)));
    }

    //? if <1.20.2 {
    /*@Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.setColor(1f, 1f, 1f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        graphics.blitNineSliced(TEXTURE, getX(), getY(), getWidth(), getHeight(),
                BORDER_X, BORDER_Y, TEXTURE_W, TEXTURE_H, 0, getTextureY());
        graphics.blitNineSliced(TEXTURE, getX() + (int) (this.value * (getWidth() - HANDLE_WIDTH)),
                getY(), HANDLE_WIDTH, getHeight(),
                BORDER_X, BORDER_Y, TEXTURE_W, TEXTURE_H, 0, getHandleTextureY());
        graphics.setColor(1f, 1f, 1f, 1f);
        int colour = this.active ? 0xFFFFFF : 0xA0A0A0;
        renderScrollingString(graphics, Minecraft.getInstance().font, TEXT_MARGIN,
                colour | Mth.ceil(this.alpha * 255f) << 24);
    }
    *///?}

    @Override
    protected void applyValue() {
        ToolEntry entry = selection.get();
        if (entry == null) return;
        float raw = property.min() + (float) this.value * (property.max() - property.min());
        property.setter().set(entry, property.wholeNumbers() ? Math.round(raw) : Math.round(raw * 100f) / 100f);
        onChanged.run();
    }
}
