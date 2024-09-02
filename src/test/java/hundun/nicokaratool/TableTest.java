package hundun.nicokaratool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.japanese.MainService;
import hundun.nicokaratool.japanese.MainService.TableHint;
import hundun.nicokaratool.layout.Align;
import hundun.nicokaratool.layout.Table;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class TableTest {

    public static final String TEST_OUTPUT_FOLDER = "test-output/";

    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testTable1() throws JsonProcessingException {
        JapaneseLine line = JapaneseLine.builder()
                .chinese("一段中文翻译。")
                .parsedTokens(List.of(
                        JapaneseParsedToken.builder()
                                .partOfSpeechLevel1("一阶语法分析")
                                .subTokens(List.of(
                                        JapaneseSubToken.builder()
                                                .kana("ふう")
                                                .build(),
                                        JapaneseSubToken.builder()
                                                .kanji("汉字长长长长长长长长长长长长长长长长长长长长文本")
                                                .furigana("ふりがな")
                                                .build(),
                                        JapaneseSubToken.builder()
                                                .kana("ばる")
                                                .build()
                                ))
                                .build()
                ))
                .build();
        testCore(line, "testTable1");
    }

    @Test
    public void testTable2() throws JsonProcessingException {
        JapaneseLine line = JapaneseLine.builder()
                .chinese("一段中文翻译，长长长长长长长长长长长长长长长长长长长长文本。")
                .parsedTokens(List.of(
                        JapaneseParsedToken.builder()
                                .partOfSpeechLevel1("一阶语法分析")
                                .subTokens(List.of(
                                        JapaneseSubToken.builder()
                                                .kana("ふう")
                                                .build(),
                                        JapaneseSubToken.builder()
                                                .kanji("汉字")
                                                .furigana("ふりがな")
                                                .build(),
                                        JapaneseSubToken.builder()
                                                .kana("ばる")
                                                .build()
                                ))
                                .build()
                ))
                .build();
        testCore(line, "testTable2");
    }

    private void testCore(JapaneseLine line, String subName) throws JsonProcessingException {
        Table table;
        TableHint tableHint = TableHint.builder()
                .parsedTokensIndexToMojiHintMap(new HashMap<>())
                .build();
        int i = 0;
        table = MainService.fromLine(line, tableHint);
        System.out.println("table" + (i++) + ": " + objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER + subName + "_default_output.png");

        tableHint.setAlign(Align.right);
        table = MainService.fromLine(line, tableHint);
        System.out.println("table" + (i++) + ": " + objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER + subName + "_right_output.png");
    }


}
