package hundun.nicokaratool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.japanese.MainService;
import hundun.nicokaratool.layout.Table;
import org.junit.Test;

import java.util.List;

public class TableTest {

    public static final String TEST_OUTPUT_FOLDER = "test-output/";

    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testTable() throws JsonProcessingException {
        JapaneseLine line = JapaneseLine.builder()
                .parsedTokens(List.of(
                        JapaneseParsedToken.builder()
                                .partOfSpeechLevel1("一段说明，长长长长长长长长长长长长长长长长长长长长文本")
                                .subTokens(List.of(
                                        JapaneseSubToken.builder()
                                                .kana("a")
                                                .build(),
                                        JapaneseSubToken.builder()
                                                .kanji("汉")
                                                .furigana("furigana")
                                                .build(),
                                        JapaneseSubToken.builder()
                                                .kana("bc")
                                                .build()
                                ))
                                .build()
                ))
                .build();
        Table table = MainService.fromLine(line);
        System.out.println(objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER +this.getClass().getSimpleName() + "_output.png");
    }


}
