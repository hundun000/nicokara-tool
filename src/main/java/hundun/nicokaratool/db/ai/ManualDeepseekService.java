package hundun.nicokaratool.db.ai;

import hundun.nicokaratool.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import hundun.nicokaratool.util.JsonUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ManualDeepseekService extends AiService {



    public ManualDeepseekService() {
        this.step1TaskSplitMinSize = 20;
        this.step1TaskSplitMaxSize = 25;
        this.step2TaskSplitMinSize = 5;
        this.step2TaskSplitMaxSize = 10;
    }

    boolean lastTurnFail;

    @Override
    public @Nullable List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate) {
        try {
            String askLinesText = askLines.stream().collect(Collectors.joining("\n"));
            String content = readBigStringIn(lastTurnFail, step2AskTemplate + askLinesText, askLinesText);
            content = formatAiResult(content);
            List<LyricLineDTO> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, LyricLineDTO.class));
            List<String> resultLines = nodes.stream().map(node -> node.getLyric()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                logNotEquals(askLines, resultLines);
                throw new Exception("resultLines not equals.");
            }
            lastTurnFail = false;
            return nodes;
        } catch (Exception e) {
            lastTurnFail = true;
            return null;
        }
    }

    @Override
    public @Nullable List<AiStep1ResultNode> aiStep1Group(List<String> askLines, String step1AskTemplate) {
        try {
            String askLinesText = askLines.stream().collect(Collectors.joining("\n"));
            String content = readBigStringIn(lastTurnFail, step1AskTemplate + askLinesText, askLinesText);
            content = formatAiResult(content);
            List<AiStep1ResultNode> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, AiStep1ResultNode.class));
            List<String> resultLines = nodes.stream().flatMap(node -> node.getLyrics().stream()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                logNotEquals(askLines, resultLines);
                throw new Exception("resultLines not equals.");
            }
            lastTurnFail = false;
            return nodes;
        } catch (Exception e) {
            lastTurnFail = true;
            return null;
        }
    }

    public String readBigStringIn(boolean lastFail, String... hints) throws IOException {

        // 创建一个面板，将提示标签和文本区域添加到面板中
        JPanel panel = new JPanel();
        String inputText;

        ManualDeepseekTempPanel tempPanel = new ManualDeepseekTempPanel(lastFail, hints);
        inputText = tempPanel.getInputText();



        return inputText;
    }

}
