package hundun.nicokaratool.layout;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.layout.Cell.DrawContext;
import io.github.humbleui.skija.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class Table {

    static final int KANJI_FONT_SIZE = 20;
    static final int KANA_FONT_SIZE = 10;

    Cell dummyRootCell;
    int depthBound;
    int rightBound;

    Map<Integer, Integer> layerCellsMaxPreferredHeightMap;

    public static Table fromLine(JapaneseLine line) {
        Cell chineseRootCell = Cell.builder()
                .rawText(line.getChinese())
                .fontSize(KANJI_FONT_SIZE)
                .belowCells(
                        line.getParsedTokens().stream()
                                .map(it -> cellFromToken(it))
                                .collect(Collectors.toList())
                )
                .build();
        Cell dummyRootCell = Cell.builder()
                .rawText("")
                .fontSize(0)
                .belowCells(List.of(chineseRootCell))
                .build();
        Table table = Table.builder()
                .dummyRootCell(dummyRootCell)
                .layerCellsMaxPreferredHeightMap(new HashMap<>())
                .build();
        table.getDummyRootCell().update(table, null);
        table.getDummyRootCell().update2(table, null);
        table.getDummyRootCell().update3(table, null, 0);
        return table;
    }

    public static Cell cellFromToken(JapaneseParsedToken parsedToken) {
        List<Cell> belowCells = parsedToken.getSubTokens().stream()
                .map(it -> cellFromSubToken(it))
                .collect(Collectors.toList());
        Cell upCell = Cell.builder()
                .rawText(parsedToken.getDes())
                .fontSize(20)
                .build();
        upCell.setBelowCells(belowCells);
        return upCell;
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

    public void draw(String outputFilePathName) {
        Typeface face = Typeface.makeFromFile("data/Fonts/MiSans-Normal.ttf");

        Surface surface = Surface.makeRasterN32Premul(this.getRightBound() + 5, this.depthBound + 5);

        Canvas canvas = surface.getCanvas();

        DrawContext drawContext = new DrawContext(canvas, face);

        this.dummyRootCell.draw(drawContext);

        Image image = surface.makeImageSnapshot();
        io.github.humbleui.skija.Data pngData = image.encodeToData(EncodedImageFormat.PNG);
        ByteBuffer pngBytes = pngData.toByteBuffer();
        try
        {
            java.nio.file.Path path =  java.nio.file.Path.of(outputFilePathName);
            ByteChannel channel = Files.newByteChannel(
                    path,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            channel.write(pngBytes);
            channel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
