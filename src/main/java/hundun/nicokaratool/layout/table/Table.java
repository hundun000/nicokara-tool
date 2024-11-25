package hundun.nicokaratool.layout.table;


import lombok.Data;

import java.util.HashMap;
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
