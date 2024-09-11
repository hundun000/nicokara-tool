package hundun.nicokaratool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.japanese.MainService;
import hundun.nicokaratool.japanese.MainService.JapaneseExtraHint;
import hundun.nicokaratool.layout.Align;
import hundun.nicokaratool.layout.Table;
import hundun.nicokaratool.layout.TableBuilder;
import hundun.nicokaratool.layout.TableBuilder.CellBuilder;
import org.junit.Test;

import java.util.*;

public class TableTest {

    public static final String TEST_OUTPUT_FOLDER = "test-output/";

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
        table = tableBuilder.build();
        table.setDebug(true);
        System.out.println("table" + (i++) + ": " + objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER + testName + "_default_output.png");

        tableBuilder.setAlign(Align.right);
        table = tableBuilder.build();
        table.setDebug(true);
        System.out.println("table" + (i++) + ": " + objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER + testName + "_right_output.png");
    }


}
