package hundun.nicokaratool.core.layout.table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class Cell {

    @JsonIgnore
    @ToString.Exclude
    Table table;
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
    public void update(Typeface typeface, Table table, @Nullable Cell aboveCell) {
        Font font = new Font(typeface, fontSize);
        this.table = table;
        this.layer = aboveCell == null ? 0 : aboveCell.getLayer() + 1;
        int preferredWidthFromRawText = Math.min((rawText.length() * fontSize), table.getSingleContentMaxWidth());
        if (belowCells != null) {
            belowCells.forEach(it -> it.update(typeface, table, this));
            int belowCellsSumPreferredWidth = belowCells.stream()
                    .mapToInt(it -> it.getPreferredWidth())
                    .sum();
            this.preferredWidth = Math.max(belowCellsSumPreferredWidth, table.xPreferredSpace * 2 + preferredWidthFromRawText);
        } else {
            this.preferredWidth = table.xPreferredSpace * 2 + preferredWidthFromRawText;
        }

        this.contentWidth = Math.min((int) font.measureTextWidth(rawText), this.preferredWidth);

        List<String> wrappedTextBuilderList = new ArrayList<>();
        StringBuilder wrappedTextLine = new StringBuilder();
        for (int i = 0; i < rawText.length(); i++) {
            wrappedTextLine.append(rawText.charAt(i));
            String checkingNextChar = (i + 1 < rawText.length()) ? (wrappedTextLine.toString() + rawText.charAt(i)) : "";
            if (font.measureTextWidth(checkingNextChar) > this.preferredWidth) {
                wrappedTextBuilderList.add(wrappedTextLine.toString());
                wrappedTextLine.setLength(0);
            }
        }
        if (wrappedTextLine.length() > 0) {
            wrappedTextBuilderList.add(wrappedTextLine.toString());
        }
        this.wrappedText = wrappedTextBuilderList;
        int lineCount = wrappedTextBuilderList.size();
        this.preferredHeight = table.yPreferredSpace * 2 + lineCount * fontSize;
        this.contentHeight = rawText.isEmpty() ? 0 :lineCount * fontSize;
        table.getLayerCellsMaxPreferredHeightMap().merge(this.layer, preferredHeight, (o1, o2) -> Math.max(o1, o2));
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


}
