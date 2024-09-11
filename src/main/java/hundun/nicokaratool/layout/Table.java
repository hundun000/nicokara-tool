package hundun.nicokaratool.layout;

import hundun.nicokaratool.japanese.MainService.JapaneseExtraHint;
import hundun.nicokaratool.layout.Cell.DrawContext;
import io.github.humbleui.skija.*;
import lombok.Data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@Data
public class Table {

    Cell dummyRootCell;
    int depthBound;
    int rightBound;
    int align = Align.center;
    int xPreferredSpace = 5;
    int yPreferredSpace = 5;
    boolean debug;

    Map<Integer, Integer> layerCellsMaxPreferredHeightMap = new HashMap<>();


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
