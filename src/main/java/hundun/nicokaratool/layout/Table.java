package hundun.nicokaratool.layout;


import hundun.nicokaratool.layout.ImageRender.DrawContext;
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


}
