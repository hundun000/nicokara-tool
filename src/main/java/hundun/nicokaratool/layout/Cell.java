package hundun.nicokaratool.layout;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class Cell {
    /**
     * 不含xPreferredSpace的ContextMaxWidth
     */
    public static final int defaultSingleContentMaxWidth = 100;

    int tempLayoutWidth;
    int layoutWidth;
    int preferredWidth;
    int contentWidth;
    int contentHeight;
    int preferredHeight;
    int layoutHeight;
    int fontSize;
    /**
     * cell在table的位置，均基于左上角
     */
    int xInTable;
    /**
     * cell在table的位置，均基于左上角
     */
    int yInTable;
    /**
     * Content在cell的位置，均基于左上角
     */
    int xContentInCell;
    /**
     * Content在cell的位置，均基于左上角
     */
    int yContentInCell;
    String rawText;
    List<String> wrappedText;
    List<Cell> belowCells;
    int layer;
    /**
     * 其中：
     * layoutHeight;
     * 并未被决定；
     */
    public void update(Table table, @Nullable Cell aboveCell) {
        this.layer = aboveCell == null ? 0 : aboveCell.getLayer() + 1;
        int preferredWidthFromRawText = Math.min((rawText.length() * fontSize), defaultSingleContentMaxWidth);
        if (belowCells != null) {
            belowCells.forEach(it -> it.update(table, this));
            int belowCellsSumPreferredWidth = belowCells.stream()
                    .mapToInt(it -> it.getPreferredWidth())
                    .sum();
            this.preferredWidth = Math.max(belowCellsSumPreferredWidth, table.xPreferredSpace * 2 + preferredWidthFromRawText);
        } else {
            this.preferredWidth = table.xPreferredSpace * 2 + preferredWidthFromRawText;
        }
        int charPerLineMax = rawText.length() == 0 ? 0 : (this.preferredWidth / fontSize);
        this.contentWidth = Math.min(rawText.length(), charPerLineMax) * fontSize;
        int lineCount = rawText.length() == 0 ? 1 : ((rawText.length() / charPerLineMax) + (rawText.length() % charPerLineMax != 0 ? 1 : 0));
        this.preferredHeight = table.yPreferredSpace * 2 + lineCount * fontSize;
        this.contentHeight = rawText.length() == 0 ? 0 :lineCount * fontSize;
        table.getLayerCellsMaxPreferredHeightMap().merge(this.layer, preferredHeight, (o1, o2) -> Math.max(o1, o2));
        List<String> wrappedTextBuilder = new ArrayList<>();
        StringBuilder wrappedTextLine = new StringBuilder();
        for (int i = 0; i < rawText.length(); i++) {
            wrappedTextLine.append(rawText.charAt(i));
            if (i % charPerLineMax == charPerLineMax - 1) {
                wrappedTextBuilder.add(wrappedTextLine.toString());
                wrappedTextLine.setLength(0);
            }
        }
        if (wrappedTextLine.length() > 0) {
            wrappedTextBuilder.add(wrappedTextLine.toString());
        }
        this.wrappedText = wrappedTextBuilder;
    }


    public void update2(Table table, @Nullable Cell aboveCell) {
        // tempLayoutWidth before child
        if (aboveCell == null) {
            this.tempLayoutWidth = this.preferredWidth;
        } else {
            this.tempLayoutWidth = Math.max(this.preferredWidth, aboveCell.tempLayoutWidth / aboveCell.getBelowCells().size());
        }
        if (belowCells != null) {
            belowCells.forEach(it -> it.layoutHeight = table.getLayerCellsMaxPreferredHeightMap().get(it.getLayer()));
            belowCells.forEach(it -> it.update2(table, this));
            // layoutWidth after child
            int belowCellsSumWidth = belowCells.stream()
                    .mapToInt(it -> it.getLayoutWidth())
                    .sum();
            this.layoutWidth = Math.max(belowCellsSumWidth, this.tempLayoutWidth);
        } else {
            this.layoutWidth = this.tempLayoutWidth;
        }
    }

    public void update3(Table table, @Nullable Cell aboveCell, int xInParent) {
        if (aboveCell == null) {
            this.xInTable = 0;
            this.yInTable = 0;
        } else {
            this.xInTable = aboveCell.getXInTable() + xInParent;
            this.yInTable = aboveCell.getYInTable() + aboveCell.getLayoutHeight();
        }
        if (Align.isLeft(table.getAlign())) {
            this.xContentInCell = 0;
        } else if (Align.isCenterHorizontal(table.getAlign())) {
            this.xContentInCell = (this.layoutWidth - this.contentWidth) / 2;
        } else {
            this.xContentInCell = this.layoutWidth - this.contentWidth;
        }
        if (Align.isTop(table.getAlign())) {
            this.yContentInCell = 0;
        } else if (Align.isCenterVertical(table.getAlign())) {
            this.yContentInCell = (this.layoutHeight - this.contentHeight) / 2;
        } else {
            this.yContentInCell = this.layoutHeight - this.contentHeight;
        }
        if (belowCells != null) {
            int childXInCurrent = 0;
            for (var belowCell : belowCells) {
                belowCell.update3(table, this, childXInCurrent);
                childXInCurrent += belowCell.getLayoutWidth();
            }
        }
        int myDepth = this.yInTable + this.getLayoutHeight();
        if (table.getDepthBound() < myDepth) {
            table.setDepthBound(myDepth);
        }
        int myRight = this.xInTable + this.getLayoutWidth();
        if (table.getRightBound() < myRight) {
            table.setRightBound(myRight);
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class DrawContext {
        Canvas canvas;
        Typeface face;
    }

    public void draw(DrawContext drawContext) {
        Paint fill = new Paint().setColor(0xFF000000);
        Paint debugFill = new Paint().setColor(0xFF8B0000);
        if (wrappedText.size() > 0) {
            Font font = new Font(drawContext.getFace(), fontSize);
            for (int i = 0; i < wrappedText.size(); i++) {
                drawContext.getCanvas().drawString(
                        wrappedText.get(i),
                        xInTable + xContentInCell,
                        yInTable + yContentInCell + fontSize * (i + 1),
                        font,
                        fill);
            }
            Point[] contentCoords = new Point[] {
                    // up
                    new Point(xInTable + xContentInCell, yInTable + yContentInCell),
                    new Point(xInTable + xContentInCell + contentWidth, yInTable + yContentInCell),
                    // right
                    new Point(xInTable + xContentInCell + contentWidth, yInTable + yContentInCell),
                    new Point(xInTable + xContentInCell + contentWidth, yInTable + yContentInCell + contentHeight),
                    // down
                    new Point(xInTable + xContentInCell + contentWidth, yInTable + yContentInCell + contentHeight),
                    new Point(xInTable + xContentInCell, yInTable + yContentInCell + contentHeight),
                    // left
                    new Point(xInTable + xContentInCell, yInTable + yContentInCell + contentHeight),
                    new Point(xInTable + xContentInCell, yInTable + yContentInCell),
            };
            drawContext.getCanvas().drawLines(contentCoords, debugFill);
        }
        Point[] cellCoords = new Point[] {
                // up
                new Point(xInTable, yInTable),
                new Point(xInTable + layoutWidth, yInTable),
                // right
                new Point(xInTable + layoutWidth, yInTable),
                new Point(xInTable + layoutWidth, yInTable + layoutHeight),
                // down
                new Point(xInTable + layoutWidth, yInTable + layoutHeight),
                new Point(xInTable, yInTable + layoutHeight),
                // left
                new Point(xInTable, yInTable + layoutHeight),
                new Point(xInTable, yInTable),
        };
        drawContext.getCanvas().drawLines(cellCoords, fill);
        if (belowCells != null) {
            for (var belowCell : belowCells) {
                belowCell.draw(drawContext);
            }
        }
    }
}
