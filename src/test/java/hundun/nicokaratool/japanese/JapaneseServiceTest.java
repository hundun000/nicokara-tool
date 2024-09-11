package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.BaseService.ServiceResult;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.MainService.JapaneseExtraHint;
import hundun.nicokaratool.layout.Table;
import hundun.nicokaratool.layout.TableBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static hundun.nicokaratool.TableTest.TEST_OUTPUT_FOLDER;

public class JapaneseServiceTest {
    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    static JapaneseService service = new JapaneseService();
    MainService mainService = new MainService();

    @Test
    public void test() throws IOException {
        String name = "example-japanese";

        ServiceResult serviceResult = service.work(name);

        System.out.println("Ruby: ");
        System.out.println(serviceResult.getRuby());
    }


    @Test
    public void testTable3() throws JsonProcessingException {
        testTableCore(5);
    }

    private void testTableCore(int space) throws JsonProcessingException {
        String text = "大切な思い出を";
        JapaneseLine line = service.toParsedLines(List.of(text), null).get(0);
        JapaneseExtraHint japaneseExtraHint = JapaneseExtraHint.builder()
                .parsedTokensIndexToMojiHintMap(mainService.getMojiHintMap(line))
                .build();
        TableBuilder tableBuilder = TableBuilder.fromJapaneseLine(line, japaneseExtraHint);
        tableBuilder.setXPreferredSpace(space);
        tableBuilder.setYPreferredSpace(space);
        Table table = tableBuilder.build();
        System.out.println(objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER +this.getClass().getSimpleName() + "_space" + space + "_output.png");
    }
}
