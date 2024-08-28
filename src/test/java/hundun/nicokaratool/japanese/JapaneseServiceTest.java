package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.BaseService.ServiceResult;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.layout.Table;
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
    public void testTable2() throws JsonProcessingException {
        String text = "お寿司が食べたい";
        JapaneseLine line = service.toParsedLines(List.of(text), null).get(0);
        line.setChinese("我想吃寿司。一段长长长长长长长长长长长长长长长长长长长长长长长长长长文本！");
        Table table = mainService.fromLineWithHint(line);
        System.out.println(objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER +this.getClass().getSimpleName() + "_output.png");
    }
}
