package hundun.nicokaratool.layout;


import hundun.nicokaratool.layout.table.Cell;
import hundun.nicokaratool.layout.table.Table;
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
        if (!cell.getWrappedText().isEmpty()) {
            Font font = new Font(drawContext.getFace(), cell.getFontSize());
            for (int i = 0; i < cell.getWrappedText().size(); i++) {
                drawContext.getCanvas().drawString(
                        cell.getWrappedText().get(i),
                        cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell(),
                        cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell() + cell.getFontSize() * (i + 1),
                        font,
                        fill);
            }
            if (cell.getTable().isDebug()) {
                Point[] contentCoords = new Point[] {
                        // up
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell()),
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell() + cell.getContentWidth(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell()),
                        // right
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell() + cell.getContentWidth(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell()),
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell() + cell.getContentWidth(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell() + cell.getContentHeight()),
                        // down
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell() + cell.getContentWidth(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell() + cell.getContentHeight()),
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell() + cell.getContentHeight()),
                        // left
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell() + cell.getContentHeight()),
                        new Point(cell.getTable().getX() + cell.getXInTable() + cell.getXContentInCell(), cell.getTable().getY() + cell.getYInTable() + cell.getYContentInCell()),
                };
                drawContext.getCanvas().drawLines(contentCoords, debugFill);
            }
        }
        Point[] cellCoords = new Point[] {
                // up
                new Point(cell.getTable().getX() + cell.getXInTable(), cell.getTable().getY() + cell.getYInTable()),
                new Point(cell.getTable().getX() + cell.getXInTable() + cell.getLayoutWidth(), cell.getTable().getY() + cell.getYInTable()),
                // right
                new Point(cell.getTable().getX() + cell.getXInTable() + cell.getLayoutWidth(), cell.getTable().getY() + cell.getYInTable()),
                new Point(cell.getTable().getX() + cell.getXInTable() + cell.getLayoutWidth(), cell.getTable().getY() + cell.getYInTable() + cell.getLayoutHeight()),
                // down
                new Point(cell.getTable().getX() + cell.getXInTable() + cell.getLayoutWidth(), cell.getTable().getY() + cell.getYInTable() + cell.getLayoutHeight()),
                new Point(cell.getTable().getX() + cell.getXInTable(), cell.getTable().getY() + cell.getYInTable() + cell.getLayoutHeight()),
                // left
                new Point(cell.getTable().getX() + cell.getXInTable(), cell.getTable().getY() + cell.getYInTable() + cell.getLayoutHeight()),
                new Point(cell.getTable().getX() + cell.getXInTable(), cell.getTable().getY() + cell.getYInTable()),
        };
        drawContext.getCanvas().drawLines(cellCoords, fill);
        if (cell.getBelowCells() != null) {
            for (var belowCell : cell.getBelowCells()) {
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
                .mapToInt(it -> it.getDepthBound() + space)
                .sum();
        Surface surface = Surface.makeRasterN32Premul(surfaceWidth, surfaceHeight);

        Canvas canvas = surface.getCanvas();

        DrawContext drawContext = new DrawContext(canvas, face);

        int yOffset = 0;
        for (var table : tableList) {
            table.setY(yOffset);
            ImageRender.draw(table.getDummyRootCell(), drawContext);
            yOffset += (table.getDepthBound() + space);
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