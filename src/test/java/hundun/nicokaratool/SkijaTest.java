package hundun.nicokaratool;

import hundun.nicokaratool.japanese.KuromojiTool;
import io.github.humbleui.skija.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class SkijaTest {
    @Test
    public void testFun() {
        Surface surface = Surface.makeRasterN32Premul(100, 100);

        Canvas canvas = surface.getCanvas();
        Paint paint = new Paint();
        paint.setColor(0xFFFF0000);
        canvas.drawCircle(50, 50, 30, paint);

        Typeface face = Typeface.makeFromFile("data/Fonts/MiSans-Normal.ttf");
        //Typeface face = FontMgr.getDefault().matchFamilyStyle("Arial", FontStyle.NORMAL);
        Font font = new Font(face, 13);
        Paint fill = new Paint().setColor(0xFF000000);
        canvas.drawString("你好世界", 0, 50, font, fill);

        Image image = surface.makeImageSnapshot();
        Data pngData = image.encodeToData(EncodedImageFormat.PNG);
        ByteBuffer pngBytes = pngData.toByteBuffer();
        try
        {
            java.nio.file.Path path =  java.nio.file.Path.of("output.png");
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
