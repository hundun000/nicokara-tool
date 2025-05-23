package hundun.nicokaratool.core.japanese;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.core.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.core.japanese.JapaneseService.ServiceContext;
import hundun.nicokaratool.core.japanese.JapaneseService.WorkArgPackage;
import hundun.nicokaratool.core.layout.text.DebugLyricsRender;
import hundun.nicokaratool.core.layout.ImageRender;
import hundun.nicokaratool.core.layout.table.Table;
import hundun.nicokaratool.core.layout.table.TableBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static hundun.nicokaratool.Constants.TEST_OUTPUT_FOLDER;

public class JapaneseServiceTest {
    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    static JapaneseService japaneseService = new JapaneseService();
    MojiService mojiService = new MojiService();

    @Test
    public void testAllFeaturesLong() throws IOException {
        String name = "example-japanese-long";

        japaneseService.argPackage = WorkArgPackage.getAllFeatures();
        ServiceContext serviceResult = japaneseService.quickStep1(name);

        japaneseService.workStep2(serviceResult);
    }

    @Test
    public void testAllFeaturesShort() throws IOException {
        String name = "example-japanese-short";

        japaneseService.argPackage = WorkArgPackage.getAllFeatures();
        ServiceContext serviceResult = japaneseService.quickStep1(name);

        japaneseService.workStep2(serviceResult);
    }


    @Test
    public void testTable3() throws JsonProcessingException {
        int space = 5;
        String text = "大切な思い出を";
        JapaneseLine line = japaneseService.toParsedLines(List.of(text), null).get(0);
        JapaneseExtraHint japaneseExtraHint = JapaneseExtraHint.builder()
                .parsedTokensIndexToMojiHintMap(mojiService.getMojiHintMap(line))
                .build();
        TableBuilder tableBuilder = TableBuilder.fromJapaneseLine(line, japaneseExtraHint);
        tableBuilder.setXPreferredSpace(space);
        tableBuilder.setYPreferredSpace(space);
        Table table = tableBuilder.build(ImageRender.face);
        System.out.println(objectMapper.writeValueAsString(table));
        ImageRender.multiDraw(TEST_OUTPUT_FOLDER + this.getClass().getSimpleName() + "_" + text + "_output.png", List.of(table), space, true);
    }

    @Test
    public void testTagTokenizer() throws JsonProcessingException {
        String text = "[00:22:90]大切な[00:24:03]思い[00:24:94][00:24:95]出を[00:25:51]";
        JapaneseLine line = japaneseService.toParsedLines(List.of(text), null).get(0);
        System.out.println(text);
        System.out.println(DebugLyricsRender.INSTANCE.toLyricsLine(line));
        System.out.println(objectMapper.writeValueAsString(line));
    }

    @Test
    public void testTagTokenizer2() throws JsonProcessingException {
        String text = "まだまだ言い足りないでしょ";
        JapaneseLine line = japaneseService.toParsedLines(List.of(text), null).get(0);
        System.out.println(text);
        System.out.println(DebugLyricsRender.INSTANCE.toLyricsLine(line));
        System.out.println(objectMapper.writeValueAsString(line));
    }
}
