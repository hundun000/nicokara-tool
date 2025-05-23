package hundun.nicokaratool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.core.layout.table.Align;
import hundun.nicokaratool.core.layout.ImageRender;
import hundun.nicokaratool.core.layout.table.Table;
import hundun.nicokaratool.core.layout.table.TableBuilder;
import hundun.nicokaratool.core.layout.table.TableBuilder.CellBuilder;
import org.junit.Test;

import java.util.List;

public class TableTest {

    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testTable1() throws JsonProcessingException {
        String testName = "testTable";
        TableBuilder tableBuilder = new TableBuilder()
                .addFirstLayerCell(
                        CellBuilder.get("文本一", 20)
                                .addBelowCell(
                                        CellBuilder.get("文本一一", 20)
                                                .addBelowCell(
                                                        CellBuilder.get("文本一一", 20)
                                                                .addBelowCell(CellBuilder.get("文本一一一", 10))
                                                                .addBelowCell(CellBuilder.get("文本一一二", 10))
                                                )
                                                .addBelowCell(
                                                        CellBuilder.get("文本一二", 20)
                                                                .addBelowCell(CellBuilder.get("文本一二一", 10))
                                                )
                                )
                );

        Table table;
        int i = 0;
        table = tableBuilder.build(ImageRender.face);
        table.setDebug(true);
        System.out.println("table" + (i++) + ": " + objectMapper.writeValueAsString(table));
        ImageRender.multiDraw(Constants.TEST_OUTPUT_FOLDER + testName + "_default_output.png", List.of(table), 5, true);

        tableBuilder.setAlign(Align.right);
        table = tableBuilder.build(ImageRender.face);
        table.setDebug(true);
        System.out.println("table" + (i++) + ": " + objectMapper.writeValueAsString(table));
        ImageRender.multiDraw(Constants.TEST_OUTPUT_FOLDER + testName + "_right_output.png", List.of(table), 5, true);
    }


}
