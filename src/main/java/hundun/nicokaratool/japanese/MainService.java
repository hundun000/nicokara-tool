package hundun.nicokaratool.japanese;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.japanese.MojiDictFeignClient.MojiDictRequest;
import hundun.nicokaratool.japanese.MojiDictFeignClient.MojiDictResponse;
import hundun.nicokaratool.japanese.MojiDictFeignClient.MojiDictResponse.SearchResultItem;
import hundun.nicokaratool.layout.Cell;
import hundun.nicokaratool.layout.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainService {
    static final int KANJI_FONT_SIZE = 20;
    static final int KANA_FONT_SIZE = 10;

    MojiDictFeignClient mojiDictFeignClient = MojiDictFeignClient.instance();

    public Table fromLineWithHint(JapaneseLine line) {
        Map<Integer, SearchResultItem> parsedTokensIndexToMojiHintMap = new HashMap<>();
        for (int i = 0; i < line.getParsedTokens().size(); i++) {
            var it = line.getParsedTokens().get(i);
            if (it.typeKanji()) {
                MojiDictResponse response = mojiDictFeignClient.union_api(MojiDictRequest.quickBuild(it.getSurface()));
                parsedTokensIndexToMojiHintMap.put(i, MojiDictResponse.findFirstSearchResultItem(response));
            }
        }

        TableHint tableHint = TableHint.builder()
                .parsedTokensIndexToMojiHintMap(parsedTokensIndexToMojiHintMap)
                .build();
        return fromLine(line, tableHint);
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class TableHint {
        Map<Integer, SearchResultItem> parsedTokensIndexToMojiHintMap;
    }

    public static Table fromLine(JapaneseLine line) {
        TableHint tableHint = TableHint.builder()
                .parsedTokensIndexToMojiHintMap(new HashMap<>())
                .build();
        return fromLine(line, tableHint);
    }

    private static Table fromLine(JapaneseLine line, TableHint tableHint) {
        Cell chineseRootCell = Cell.builder()
                .rawText(line.getChinese())
                .fontSize(KANJI_FONT_SIZE)
                .belowCells(
                        line.getParsedTokens().stream()
                                .map(it -> cellFromToken(it, tableHint))
                                .collect(Collectors.toList())
                )
                .build();
        Cell dummyRootCell = Cell.builder()
                .rawText("")
                .fontSize(0)
                .belowCells(List.of(chineseRootCell))
                .build();
        Table table = new Table();
        table.setTableHint(tableHint);
        table.setDummyRootCell(dummyRootCell);

        table.getDummyRootCell().update(table, null);
        table.getDummyRootCell().update2(table, null);
        table.getDummyRootCell().update3(table, null, 0);
        return table;
    }

    public static Cell cellFromToken(JapaneseParsedToken parsedToken, TableHint tableHint) {
        Cell current;
        current = Cell.builder()
                .rawText(parsedToken.getPartOfSpeechLevel1())
                .fontSize(20)
                .belowCells(
                        parsedToken.getSubTokens().stream()
                                .map(it -> cellFromSubToken(it))
                                .collect(Collectors.toList())
                )
                .build();
        current = Cell.builder()
                .rawText(
                        Optional.ofNullable(tableHint.getParsedTokensIndexToMojiHintMap().get(parsedToken.getIndex()))
                                .map(it -> it.getExcerpt())
                                .orElse("")
                )
                .fontSize(20)
                .belowCells(
                        List.of(current)
                )
                .build();
        return current;
    }

    public static Cell cellFromSubToken(JapaneseSubToken subToken) {
        if (subToken.getKanji() != null) {
            Cell upCell = Cell.builder()
                    .rawText(subToken.getFurigana())
                    .fontSize(KANA_FONT_SIZE)
                    .build();
            Cell downCell = Cell.builder()
                    .rawText(subToken.getKanji())
                    .fontSize(KANJI_FONT_SIZE)
                    .build();
            upCell.setBelowCells(List.of(downCell));
            return upCell;
        } else {
            Cell upCell = Cell.builder()
                    .rawText("")
                    .fontSize(0)
                    .build();
            Cell downCell = Cell.builder()
                    .rawText(subToken.getKana())
                    .fontSize(KANJI_FONT_SIZE)
                    .build();
            upCell.setBelowCells(List.of(downCell));
            return upCell;
        }
    }
}
