package me.zipestudio.tomb.client.screen.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
 *///?}
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class EditorPainter {

    private static final int COLOUR_BORDER = 0xFF555555;
    private static final int COLOUR_BACKDROP = 0x66000000;
    private static final int COLOUR_HEADING = 0xFFB0B0B0;

    private final Minecraft minecraft;
    private final Font font;

    private final List<Panel> panels = new ArrayList<>();
    private final List<Rule> rules = new ArrayList<>();
    private final List<Heading> headings = new ArrayList<>();

    //? if >=26.1 {
    private GuiGraphicsExtractor graphics;

    public void target(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }
    //?} else {
    /*private GuiGraphics graphics;

    public void target(GuiGraphics graphics) {
        this.graphics = graphics;
    }
    *///?}

    public EditorPainter(Minecraft minecraft, Font font) {
        this.minecraft = minecraft;
        this.font = font;
    }

    public void clear() {
        panels.clear();
        rules.clear();
        headings.clear();
    }

    public void panel(int left, int top, int right, int bottom) {
        panels.add(new Panel(left, top, right, bottom, true));
    }

    public void frame(int left, int top, int right, int bottom) {
        panels.add(new Panel(left, top, right, bottom, false));
    }

    public void rule(int x, int y, int width) {
        rules.add(new Rule(x, y, width));
    }

    public void heading(Component label, int x, int y, int width, boolean centered) {
        headings.add(new Heading(label, x, y, width, centered));
    }

    public void drawBackdrops() {
        for (Panel panel : panels) {
            if (panel.filled()) fill(panel.left(), panel.top(), panel.right(), panel.bottom(), COLOUR_BACKDROP);
        }
    }

    public void drawRules() {
        for (Rule rule : rules) fill(rule.x(), rule.y(), rule.x() + rule.width(), rule.y() + 1, COLOUR_BORDER);
    }

    public void drawHeadings() {
        for (Heading heading : headings) {
            if (heading.centered()) {
                centeredText(heading.label(), heading.x() + heading.width() / 2, heading.y(), COLOUR_HEADING);
            } else {
                text(heading.label(), heading.x(), heading.y(), COLOUR_HEADING);
            }
        }
    }

    public void drawFrames() {
        for (Panel panel : panels) {
            drawBorder(panel.left(), panel.top(), panel.right(), panel.bottom(), COLOUR_BORDER);
        }
    }

    public void drawBorder(int left, int top, int right, int bottom, int colour) {
        overlayFill(left, top, right, top + 1, colour);
        overlayFill(left, bottom - 1, right, bottom, colour);
        overlayFill(left, top, left + 1, bottom, colour);
        overlayFill(right - 1, top, right, bottom, colour);
    }

    public void fill(int left, int top, int right, int bottom, int colour) {
        graphics.fill(left, top, right, bottom, colour);
    }

    //? if >=1.21.5 {
    public void overlayFill(int left, int top, int right, int bottom, int colour) {
        graphics.fill(left, top, right, bottom, colour);
    }
    //?} else {
    /*public void overlayFill(int left, int top, int right, int bottom, int colour) {
        graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), left, top, right, bottom, colour);
    }
    *///?}

    //? if >=26.1 {
    public void text(Component label, int x, int y, int colour) {
        graphics.text(font, label, x, y, colour);
    }

    public void centeredText(Component label, int centerX, int y, int colour) {
        graphics.centeredText(font, label, centerX, y, colour);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
    }

    public void tooltip(Component text, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, font.split(text, EditorLayout.TOOLTIP_WIDTH), mouseX, mouseY);
    }
    //?} else {
    /*public void text(Component label, int x, int y, int colour) {
        graphics.drawString(font, label, x, y, colour);
    }

    public void centeredText(Component label, int centerX, int y, int colour) {
        graphics.drawCenteredString(font, label, centerX, y, colour);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }

    public void tooltip(Component text, int mouseX, int mouseY) {
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(font, font.split(text, EditorLayout.TOOLTIP_WIDTH), mouseX, mouseY);
        //?} else {
        /^graphics.renderTooltip(font, font.split(text, EditorLayout.TOOLTIP_WIDTH), mouseX, mouseY);
        ^///?}
    }
    *///?}

    public void scrollingText(Component label, Font font, int left, int top, int right, int bottom,
                              int textY, int colour) {
        int overflow = font.width(label) - (right - left);
        if (overflow <= 0) {
            text(label, left, textY, colour);
            return;
        }
        double seconds = System.currentTimeMillis() / 1000.0D;
        double period = Math.max(overflow * 0.5D, 3.0D);
        double eased = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * seconds / period)) / 2.0D + 0.5D;
        scissorOn(left, top, right, bottom);
        text(label, left - (int) (eased * overflow), textY, colour);
        scissorOff();
    }

    public void scissorOn(int left, int top, int right, int bottom) {
        graphics.enableScissor(left, top, right, bottom);
    }

    public void scissorOff() {
        graphics.disableScissor();
    }

    //? if >=26.1 {
    public void entity(LivingEntity entity, float partialTick, boolean crouching, float scale,
                       Vector3f translate, Quaternionf pose, Quaternionf camera,
                       int left, int top, int right, int bottom) {
        net.minecraft.client.renderer.entity.state.EntityRenderState state =
                minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
        applyCrouch(state, crouching);
        graphics.entity(state, scale, translate, pose, camera, left, top, right, bottom);
    }
    //?} else if >=1.21.9 {
    /*public void entity(LivingEntity entity, float partialTick, boolean crouching, float scale,
                       Vector3f translate, Quaternionf pose, Quaternionf camera,
                       int left, int top, int right, int bottom) {
        net.minecraft.client.renderer.entity.state.EntityRenderState state =
                minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
        applyCrouch(state, crouching);
        state.lightCoords = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        graphics.submitEntityRenderState(state, scale, translate, pose, camera, left, top, right, bottom);
    }
    *///?} else if >=1.21.6 {
    /*public void entity(LivingEntity entity, float partialTick, boolean crouching, float scale,
                       Vector3f translate, Quaternionf pose, Quaternionf camera,
                       int left, int top, int right, int bottom) {
        InventoryScreen.renderEntityInInventory(graphics, left, top, right, bottom, scale, translate, pose,
                camera, entity);
    }
    *///?} else {
    /*public void entity(LivingEntity entity, float partialTick, boolean crouching, float scale,
                       Vector3f translate, Quaternionf pose, Quaternionf camera,
                       int left, int top, int right, int bottom) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, scale);
        graphics.enableScissor(left + 1, top + 1, right - 1, bottom);
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        //? if >=1.21 {
        InventoryScreen.renderEntityInInventory(graphics, centerX, centerY, (int) scale,
                translate, pose, camera, entity);
        //?} else {
        /^InventoryScreen.renderEntityInInventory(graphics, centerX,
                centerY + Math.round(translate.y() * scale), (int) scale, pose, camera, entity);
        ^///?}
        graphics.disableScissor();
        graphics.pose().popPose();
    }
    *///?}

    //? if >=1.21.9 {
    private static void applyCrouch(net.minecraft.client.renderer.entity.state.EntityRenderState state,
                                    boolean crouching) {
        if (state instanceof net.minecraft.client.renderer.entity.state.HumanoidRenderState humanoid) {
            humanoid.isCrouching = crouching;
        }
    }
    //?}

    private record Panel(int left, int top, int right, int bottom, boolean filled) {
    }

    private record Rule(int x, int y, int width) {
    }

    private record Heading(Component label, int x, int y, int width, boolean centered) {
    }
}
