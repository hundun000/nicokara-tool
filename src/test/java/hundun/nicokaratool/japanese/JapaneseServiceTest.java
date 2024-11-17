package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.BaseService.ServiceResult;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.NicokaraLyricsRender;
import hundun.nicokaratool.layout.Table;
import hundun.nicokaratool.layout.TableBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static hundun.nicokaratool.TableTest.TEST_OUTPUT_FOLDER;

public class JapaneseServiceTest {
    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    static JapaneseService japaneseService = new JapaneseService();
    MojiService mojiService = new MojiService();

    @Test
    public void testAll() throws IOException {
        String name = "example-japanese";

        ServiceResult<JapaneseLine> serviceResult = japaneseService.workStep1(name);

        System.out.println("Lines: ");
        System.out.println(objectMapper.writeValueAsString(serviceResult.getLines()));
        System.out.println("Ruby: ");
        System.out.println(serviceResult.getRuby());
        System.out.println("LyricsText: ");
        System.out.println(serviceResult.getLyricsText());

        japaneseService.workStep2(serviceResult, name);
    }


    @Test
    public void testTable3() throws JsonProcessingException {
        testTableCore(5);
    }

    private void testTableCore(int space) throws JsonProcessingException {
        String text = "大切な思い出を";
        JapaneseLine line = japaneseService.toParsedLines(List.of(text), null).get(0);
        JapaneseExtraHint japaneseExtraHint = JapaneseExtraHint.builder()
                .parsedTokensIndexToMojiHintMap(mojiService.getMojiHintMap(line))
                .build();
        TableBuilder tableBuilder = TableBuilder.fromJapaneseLine(line, japaneseExtraHint);
        tableBuilder.setXPreferredSpace(space);
        tableBuilder.setYPreferredSpace(space);
        Table table = tableBuilder.build();
        System.out.println(objectMapper.writeValueAsString(table));
        table.draw(TEST_OUTPUT_FOLDER +this.getClass().getSimpleName() + "_space" + space + "_output.png");
    }

    @Test
    public void testTagTokenizer() throws JsonProcessingException {
        String text = "[00:22:90]大切な[00:24:03]思い[00:24:94]出を[00:25:51]";
        JapaneseLine line = japaneseService.toParsedLines(List.of(text), null).get(0);
        System.out.println(objectMapper.writeValueAsString(line));
        System.out.println(NicokaraLyricsRender.INSTANCE.toLyricsLine(line));
    }

    @Test
    public void testTagTokenizer2() throws JsonProcessingException {
        String text = "まだまだ言い足りないでしょ";
        JapaneseLine line = japaneseService.toParsedLines(List.of(text), null).get(0);
        System.out.println(objectMapper.writeValueAsString(line));
        System.out.println(NicokaraLyricsRender.INSTANCE.toLyricsLine(line));
    }
}
