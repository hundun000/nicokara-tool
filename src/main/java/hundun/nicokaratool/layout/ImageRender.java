package hundun.nicokaratool.layout;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ImageRender {

    @Data
    @AllArgsConstructor
    @Builder
    public static class DrawContext {
        Canvas canvas;
        Typeface face;
    }
    static Paint fill = new Paint().setColor(0xFF000000);
    static Paint debugFill = new Paint().setColor(0xFF8B0000);
    public static void draw(Cell cell, DrawContext drawContext) {
        if (!cell.wrappedText.isEmpty()) {
            Font font = new Font(drawContext.getFace(), cell.fontSize);
            for (int i = 0; i < cell.wrappedText.size(); i++) {
                drawContext.getCanvas().drawString(
                        cell.wrappedText.get(i),
                        cell.table.getX() + cell.xInTable + cell.xContentInCell,
                        cell.table.getY() + cell.yInTable + cell.yContentInCell + cell.fontSize * (i + 1),
                        font,
                        fill);
            }
            if (cell.table.isDebug()) {
                Point[] contentCoords = new Point[] {
                        // up
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell, cell.table.getY() + cell.yInTable + cell.yContentInCell),
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell + cell.contentWidth, cell.table.getY() + cell.yInTable + cell.yContentInCell),
                        // right
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell + cell.contentWidth, cell.table.getY() + cell.yInTable + cell.yContentInCell),
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell + cell.contentWidth, cell.table.getY() + cell.yInTable + cell.yContentInCell + cell.contentHeight),
                        // down
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell + cell.contentWidth, cell.table.getY() + cell.yInTable + cell.yContentInCell + cell.contentHeight),
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell, cell.table.getY() + cell.yInTable + cell.yContentInCell + cell.contentHeight),
                        // left
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell, cell.table.getY() + cell.yInTable + cell.yContentInCell + cell.contentHeight),
                        new Point(cell.table.getX() + cell.xInTable + cell.xContentInCell, cell.table.getY() + cell.yInTable + cell.yContentInCell),
                };
                drawContext.getCanvas().drawLines(contentCoords, debugFill);
            }
        }
        Point[] cellCoords = new Point[] {
                // up
                new Point(cell.table.getX() + cell.xInTable, cell.table.getY() + cell.yInTable),
                new Point(cell.table.getX() + cell.xInTable + cell.layoutWidth, cell.table.getY() + cell.yInTable),
                // right
                new Point(cell.table.getX() + cell.xInTable + cell.layoutWidth, cell.table.getY() + cell.yInTable),
                new Point(cell.table.getX() + cell.xInTable + cell.layoutWidth, cell.table.getY() + cell.yInTable + cell.layoutHeight),
                // down
                new Point(cell.table.getX() + cell.xInTable + cell.layoutWidth, cell.table.getY() + cell.yInTable + cell.layoutHeight),
                new Point(cell.table.getX() + cell.xInTable, cell.table.getY() + cell.yInTable + cell.layoutHeight),
                // left
                new Point(cell.table.getX() + cell.xInTable, cell.table.getY() + cell.yInTable + cell.layoutHeight),
                new Point(cell.table.getX() + cell.xInTable, cell.table.getY() + cell.yInTable),
        };
        drawContext.getCanvas().drawLines(cellCoords, fill);
        if (cell.belowCells != null) {
            for (var belowCell : cell.belowCells) {
                draw(belowCell, drawContext);
            }
        }
    }

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
            ImageRender.draw(table.dummyRootCell, drawContext);
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


}
