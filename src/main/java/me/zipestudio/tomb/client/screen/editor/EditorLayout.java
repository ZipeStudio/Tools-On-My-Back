package me.zipestudio.tomb.client.screen.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class EditorLayout {

    public static final int LEFT_X = 10;
    public static final int LEFT_WIDTH = 110;
    public static final int RIGHT_WIDTH = 190;
    public static final int ROW_HEIGHT = 18;
    public static final int GAP = 4;
    public static final int ROW_GAP = 2;
    public static final int GROUP_GAP = 5;
    public static final int HEADING_HEIGHT = 11;
    public static final int FRAME_PAD = 5;
    public static final int SPLIT_GAP = 7;
    public static final int CELL_SIZE = 18;
    public static final int GRID_GUTTER = 6;
    public static final int DROPDOWN_ROW_HEIGHT = 12;
    public static final int TOOLTIP_WIDTH = 200;
    public static final int OVERLAY_HEIGHT = 12;
    public static final int OVERLAY_WIDTH = 66;
    public static final int OVERLAY_PAD = 1;

    public static final int MAX_WIDTH = 560;
    public static final int MAX_HEIGHT = 280;
    public static final int PREVIEW_GUTTER = 8;

    public static final int MIN_LEFT_WIDTH = 84;
    public static final int MIN_RIGHT_WIDTH = 150;
    public static final int MIN_PREVIEW_WIDTH = 48;

    public int guiLeft;
    public int guiTop;
    public int contentWidth;
    public int contentHeight;

    public int leftWidth;
    public int rightWidth;
    public boolean previewVisible;

    public int leftColumnX;
    public int rightColumnX;
    public int frameTop;
    public int frameBottom;
    public int bottomRowY;
    public int leftFrameLeft;
    public int leftFrameRight;
    public int rightFrameLeft;
    public int rightFrameRight;

    public int previewLeft;
    public int previewTop;
    public int previewRight;
    public int previewBottom;

    public int thirdWidth;
    public int thirdWidthLast;

    public void measure(int screenWidth, int screenHeight, EditorPainter painter) {
        this.contentWidth = Math.min(screenWidth, MAX_WIDTH);
        this.contentHeight = Math.min(screenHeight, MAX_HEIGHT);
        this.guiLeft = (screenWidth - contentWidth) / 2;
        this.guiTop = (screenHeight - contentHeight) / 2;

        int span = contentWidth - 2 * (LEFT_X - FRAME_PAD);
        shareWidth(span);

        this.leftColumnX = guiLeft + LEFT_X;
        this.leftFrameLeft = leftColumnX - FRAME_PAD;
        this.leftFrameRight = leftColumnX + leftWidth + FRAME_PAD;

        this.rightFrameRight = guiLeft + contentWidth - LEFT_X + FRAME_PAD;
        this.rightFrameLeft = rightFrameRight - rightWidth - 2 * FRAME_PAD;
        this.rightColumnX = rightFrameLeft + FRAME_PAD;

        this.thirdWidth = (rightWidth - 2 * GAP) / 3;
        this.thirdWidthLast = rightWidth - 2 * (thirdWidth + GAP);

        this.bottomRowY = guiTop + contentHeight - 20;
        this.frameTop = guiTop + 20;
        this.frameBottom = bottomRowY - 5;

        painter.panel(leftFrameLeft, frameTop, leftFrameRight, frameBottom);
        painter.panel(rightFrameLeft, frameTop, rightFrameRight, frameBottom);

        this.previewTop = frameTop;
        this.previewBottom = frameBottom;
        this.previewLeft = leftFrameRight + PREVIEW_GUTTER;
        this.previewRight = Math.max(previewLeft, rightFrameLeft - PREVIEW_GUTTER);
        if (previewVisible) painter.frame(previewLeft, previewTop, previewRight, previewBottom);
    }

    private void shareWidth(int span) {
        this.previewVisible = true;
        this.leftWidth = LEFT_WIDTH;
        this.rightWidth = RIGHT_WIDTH;

        int forColumns = span - 4 * FRAME_PAD - 2 * PREVIEW_GUTTER;
        int deficit = MIN_PREVIEW_WIDTH - (forColumns - leftWidth - rightWidth);
        if (deficit <= 0) return;

        int leftSlack = LEFT_WIDTH - MIN_LEFT_WIDTH;
        int rightSlack = RIGHT_WIDTH - MIN_RIGHT_WIDTH;
        if (deficit <= leftSlack + rightSlack) {
            int fromRight = Math.min(rightSlack, (deficit * rightSlack + leftSlack + rightSlack - 1)
                    / (leftSlack + rightSlack));
            this.rightWidth -= fromRight;
            this.leftWidth -= Math.min(leftSlack, deficit - fromRight);
            return;
        }

        this.previewVisible = false;
        this.leftWidth = LEFT_WIDTH;
        this.rightWidth = RIGHT_WIDTH;
        int overflow = leftWidth + rightWidth - (span - 4 * FRAME_PAD - GAP);
        if (overflow > 0) {
            this.leftWidth = Math.max(1, leftWidth - (overflow + 1) / 2);
            this.rightWidth = Math.max(1, rightWidth - overflow / 2);
        }
    }

    public void titleRules(EditorPainter painter, Font font, Component title) {
        int middle = guiLeft + contentWidth / 2;
        int ruleY = guiTop + 12;
        int halfTitle = font.width(title) / 2 + 8;
        int left = guiLeft + LEFT_X - FRAME_PAD;
        int right = guiLeft + contentWidth - LEFT_X + FRAME_PAD;
        if (middle - halfTitle > left) painter.rule(left, ruleY, middle - halfTitle - left);
        if (right > middle + halfTitle) painter.rule(middle + halfTitle, ruleY, right - middle - halfTitle);
    }

    public int titleY() {
        return guiTop + 8;
    }

    public int titleCenterX() {
        return guiLeft + contentWidth / 2;
    }

    public Column leftColumn(EditorPainter painter) {
        return new Column(painter, leftColumnX, leftWidth, frameTop + FRAME_PAD, this);
    }

    public Column rightColumn(EditorPainter painter) {
        return new Column(painter, rightColumnX, rightWidth, frameTop + FRAME_PAD, this);
    }

    public record Slot(int x, int y, int width, int height) {
    }

    public static final class Column {

        private final EditorPainter painter;
        private final EditorLayout layout;
        private final int x;
        private final int width;
        private int cursorY;
        private boolean afterRow;

        private Column(EditorPainter painter, int x, int width, int top, EditorLayout layout) {
            this.painter = painter;
            this.layout = layout;
            this.x = x;
            this.width = width;
            this.cursorY = top;
        }

        public int x() {
            return x;
        }

        public int width() {
            return width;
        }

        public int cursorY() {
            return cursorY;
        }

        public void moveTo(int y) {
            this.cursorY = y;
            this.afterRow = false;
        }

        public void heading(Component label) {
            heading(label, false);
        }

        public void heading(Component label, boolean centered) {
            breakGroup(GROUP_GAP);
            painter.heading(label, x, cursorY, width, centered);
            cursorY += HEADING_HEIGHT;
        }

        public void divider() {
            breakGroup(GROUP_GAP);
            painter.rule(x, cursorY, width);
            cursorY += GROUP_GAP + 1;
        }

        public void splitRule(int left, int right) {
            breakGroup(SPLIT_GAP);
            painter.rule(left, cursorY, right - left);
            cursorY += 1 + SPLIT_GAP;
        }

        public Slot full() {
            return full(ROW_HEIGHT);
        }

        public Slot full(int height) {
            Slot slot = new Slot(x, cursorY, width, height);
            advance(height);
            return slot;
        }

        public Slot[] thirds() {
            int third = layout.thirdWidth;
            Slot[] slots = {
                    new Slot(x, cursorY, third, ROW_HEIGHT),
                    new Slot(x + third + GAP, cursorY, third, ROW_HEIGHT),
                    new Slot(x + 2 * (third + GAP), cursorY, layout.thirdWidthLast, ROW_HEIGHT)
            };
            advance(ROW_HEIGHT);
            return slots;
        }

        public Slot[] thirds(int tenths) {
            int narrow = (width - 2 * GAP) * tenths / 10;
            int wide = width - 2 * GAP - 2 * narrow;
            Slot[] slots = {
                    new Slot(x, cursorY, narrow, ROW_HEIGHT),
                    new Slot(x + narrow + GAP, cursorY, narrow, ROW_HEIGHT),
                    new Slot(x + 2 * (narrow + GAP), cursorY, wide, ROW_HEIGHT)
            };
            advance(ROW_HEIGHT);
            return slots;
        }

        public Slot[] split(int firstWidth) {
            Slot[] slots = {
                    new Slot(x, cursorY, firstWidth, ROW_HEIGHT),
                    new Slot(x + firstWidth + GAP, cursorY, width - GAP - firstWidth, ROW_HEIGHT)
            };
            advance(ROW_HEIGHT);
            return slots;
        }

        private void advance(int height) {
            cursorY += height + ROW_GAP;
            afterRow = true;
        }

        private void breakGroup(int air) {
            if (afterRow) cursorY += air - ROW_GAP;
            afterRow = false;
        }
    }
}
