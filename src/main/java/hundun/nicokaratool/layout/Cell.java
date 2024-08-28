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
    public static final int xSpace = 5;
    public static final int ySpace = 5;
    public static final int defaultSingleCellMaxWidth = 100;

    int layoutWidth;
    int preferredWidth;
    int preferredHeight;
    int layoutHeight;
    int fontSize;
    int xInTable;
    int yInTable;
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
        int preferredWidth = xSpace * 2 + (rawText.length() * fontSize);
        if (belowCells != null) {
            belowCells.forEach(it -> it.update(table, this));
            int belowCellsSumLayoutWidth = belowCells.stream()
                    .mapToInt(it -> it.getPreferredWidth())
                    .sum();
            this.preferredWidth = Math.max(belowCellsSumLayoutWidth, Math.min(preferredWidth, belowCells.size() * defaultSingleCellMaxWidth));
        } else {
            this.preferredWidth = Math.min(preferredWidth, defaultSingleCellMaxWidth);
        }
        int charPerLine = fontSize == 0 ? 0 : (this.preferredWidth / fontSize);
        int lineCount = charPerLine == 0 ? 0 : ((rawText.length() / charPerLine) + (rawText.length() % charPerLine != 0 ? 1 : 0));
        this.preferredHeight = ySpace * 2 + lineCount * fontSize;
        table.getLayerCellsMaxPreferredHeightMap().merge(this.layer, preferredHeight, (o1, o2) -> Math.max(o1, o2));
        List<String> wrappedTextBuilder = new ArrayList<>();
        StringBuilder wrappedTextLine = new StringBuilder();
        for (int i = 0; i < rawText.length(); i++) {
            wrappedTextLine.append(rawText.charAt(i));
            if (i % charPerLine == charPerLine - 1) {
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
        if (aboveCell == null) {
            this.layoutWidth = this.preferredWidth;
        } else {
            this.layoutWidth = Math.max(this.preferredWidth, aboveCell.layoutWidth / aboveCell.getBelowCells().size());
        }
        if (belowCells != null) {
            belowCells.forEach(it -> it.layoutHeight = table.getLayerCellsMaxPreferredHeightMap().get(it.getLayer()));
            belowCells.forEach(it -> it.update2(table, this));
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
        if (wrappedText.size() > 0) {
            Font font = new Font(drawContext.getFace(), fontSize);
            for (int i = 0; i < wrappedText.size(); i++) {
                drawContext.getCanvas().drawString(wrappedText.get(i), xInTable, yInTable + fontSize * (i + 1), font, fill);
            }
        }
        Point[] coords = new Point[] {
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
        drawContext.getCanvas().drawLines(coords, fill);
        if (belowCells != null) {
            for (var belowCell : belowCells) {
                belowCell.draw(drawContext);
            }
        }
    }
}
