package hundun.nicokaratool.layout;

import hundun.nicokaratool.layout.Cell.DrawContext;
import io.github.humbleui.skija.*;
import lombok.Data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Table {

    int x;
    int y;
    Cell dummyRootCell;
    int depthBound;
    int rightBound;
    int align = Align.center;
    int xPreferredSpace = 5;
    int yPreferredSpace = 5;
    boolean debug;

    Map<Integer, Integer> layerCellsMaxPreferredHeightMap = new HashMap<>();

    public static void multiDraw(String outputFilePathName, List<Table> tableList, int space) {
        Typeface face = Typeface.makeFromFile("data/Fonts/MiSans-Normal.ttf");

        int surfaceWidth = tableList.stream()
                .mapToInt(it -> it.getRightBound() + space)
                .max()
                .orElse(1);
        int surfaceHeight = tableList.stream()
                .mapToInt(it -> it.depthBound + space)
                .sum();
        Surface surface = Surface.makeRasterN32Premul(surfaceWidth, surfaceHeight);

        Canvas canvas = surface.getCanvas();

        DrawContext drawContext = new DrawContext(canvas, face);

        int yOffset = 0;
        for (var table : tableList) {
            table.y = yOffset;
            table.dummyRootCell.draw(drawContext);
            yOffset += (table.depthBound + space);
        }

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


    public void draw(String outputFilePathName) {
        multiDraw(outputFilePathName, List.of(this), 5);
    }
}
