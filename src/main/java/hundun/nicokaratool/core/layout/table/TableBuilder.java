package hundun.nicokaratool.core.layout.table;

import hundun.nicokaratool.core.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.core.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.core.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.core.japanese.JapaneseExtraHint;
import hundun.nicokaratool.core.japanese.TagTokenizer.Timestamp;
import io.github.humbleui.skija.Typeface;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TableBuilder {

    int kanjiFontSize;
    int kanaFontSize;
    @Setter
    Integer align;
    @Setter
    Integer xPreferredSpace;
    @Setter
    Integer yPreferredSpace;
    @Getter
    final CellBuilder dummyRootCell;
    @Setter
    Integer singleContentMaxWidth;
    public TableBuilder() {
        this.dummyRootCell =  CellBuilder.builder()
                .rawText("")
                .fontSize(0)
                .belowCells(new ArrayList<>())
                .build();
    }

    public TableBuilder addFirstLayerCell(CellBuilder cellBuilder) {
        dummyRootCell.belowCells.add(cellBuilder);
        return this;
    }

    public static TableBuilder fromJapaneseLine(JapaneseLine line, JapaneseExtraHint japaneseExtraHint) {
        return fromJapaneseLine(line, japaneseExtraHint, 20, 10);
    }

    public static TableBuilder fromJapaneseLine(JapaneseLine line, JapaneseExtraHint japaneseExtraHint, int kanjiFontSize, int kanaFontSize) {
        TableBuilder table = new TableBuilder();
        table.kanjiFontSize = kanjiFontSize;
        table.kanaFontSize = kanaFontSize;

        CellBuilder chineseRootCell = CellBuilder.builder()
                .rawText(line.getChinese())
                .fontSize(kanjiFontSize)
                .belowCells(
                        line.getParsedTokens().stream()
                                .map(it -> table.cellFromToken(it, japaneseExtraHint))
                                .collect(Collectors.toList())
                )
                .build();
        String timeText =
                Optional.ofNullable(line.getStartTime())
                        .map(it -> it.toLyricsTime())
                        .orElse(Timestamp.unknownLyricsTime())
                        + " ~ "
                        + Optional.ofNullable(line.getEndTime())
                        .map(it -> it.toLyricsTime())
                        .orElse(Timestamp.unknownLyricsTime())
                ;
        CellBuilder lineTimeCell = CellBuilder.builder()
                .rawText(timeText)
                .fontSize(kanjiFontSize)
                .belowCells(
                        List.of(chineseRootCell)
                )
                .build();
        table.getDummyRootCell().getBelowCells().add(lineTimeCell);
        return table;
    }

    private CellBuilder cellFromToken(JapaneseParsedToken parsedToken, JapaneseExtraHint japaneseExtraHint) {
        CellBuilder current;
        // 实际图片位置：按代码构造顺序，从下至上
        // layer cellFromSubToken
        current = CellBuilder.builder()
                .rawText(parsedToken.getPartOfSpeechLevel1())
                .fontSize(kanjiFontSize)
                .belowCells(
                        parsedToken.getSubTokens().stream()
                                .map(it -> cellFromSubToken(it))
                                .collect(Collectors.toList())
                )
                .build();
        // layer ZhDetail
        current = CellBuilder.builder()
                .rawText(
                        Optional.ofNullable(japaneseExtraHint.getParsedTokensIndexToMojiHintMap().get(parsedToken.getIndex()))
                                .map(it -> "[" + it.getJaWordTags().stream().collect(Collectors.joining("|")) + "]" + it.getJaOrigin()
                                )
                                .orElse("")
                )
                .fontSize(kanjiFontSize)
                .belowCells(
                        List.of(current)
                )
                .build();
        // layer ZhDetail
        current = CellBuilder.builder()
                .rawText(
                        Optional.ofNullable(japaneseExtraHint.getParsedTokensIndexToMojiHintMap().get(parsedToken.getIndex()))
                                .map(it -> it.getZhDetail())
                                .orElse("")
                )
                .fontSize(kanjiFontSize)
                .belowCells(
                        List.of(current)
                )
                .build();
        return current;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CellBuilder {
        String rawText;
        int fontSize;
        List<CellBuilder> belowCells;

        public CellBuilder addBelowCell(CellBuilder cellBuilder) {
            belowCells.add(cellBuilder);
            return this;
        }

        public static CellBuilder get(String rawText, int fontSize) {
            return CellBuilder.builder()
                    .rawText(rawText)
                    .fontSize(fontSize)
                    .belowCells(new ArrayList<>())
                    .build();
        }

        public Cell build() {
            Cell result = Cell.builder()
                    .rawText(this.rawText)
                    .fontSize(this.fontSize)
                    .belowCells(belowCells == null ? null : belowCells.stream()
                            .map(it -> it.build())
                            .collect(Collectors.toList())
                    )
                    .build();
            return result;
        }

    }

    private CellBuilder cellFromSubToken(JapaneseSubToken subToken) {
        if (subToken.getKanji() != null) {
            CellBuilder upCell = CellBuilder.builder()
                    .rawText(subToken.getFurigana())
                    .fontSize(kanaFontSize)
                    .build();
            CellBuilder downCell = CellBuilder.builder()
                    .rawText(subToken.getKanji())
                    .fontSize(kanjiFontSize)
                    .build();
            upCell.setBelowCells(List.of(downCell));
            return upCell;
        } else {
            CellBuilder upCell = CellBuilder.builder()
                    .rawText("")
                    .fontSize(0)
                    .build();
            CellBuilder downCell = CellBuilder.builder()
                    .rawText(subToken.getSurface())
                    .fontSize(kanjiFontSize)
                    .build();
            upCell.setBelowCells(List.of(downCell));
            return upCell;
        }
    }

    public Table build(Typeface face) {
        Cell dummyRootCell = this.dummyRootCell.build();
        Table table = new Table();
        if (this.align != null) {
            table.setAlign(this.align);
        }
        if (this.xPreferredSpace != null) {
            table.setXPreferredSpace(this.xPreferredSpace);
        }
        if (this.yPreferredSpace != null) {
            table.setYPreferredSpace(this.yPreferredSpace);
        }
        if (this.singleContentMaxWidth != null) {
            table.setSingleContentMaxWidth(this.singleContentMaxWidth);
        }
        table.setDummyRootCell(dummyRootCell);

        table.getDummyRootCell().update(face, table, null);
        table.getDummyRootCell().update2(table, null);
        table.getDummyRootCell().update3(table, null, 0);
        return table;
    }
}
