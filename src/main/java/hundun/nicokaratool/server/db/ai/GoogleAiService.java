package hundun.nicokaratool.server.db.ai;

import hundun.nicokaratool.server.db.SongService.AiStep1ResultNode;
import hundun.nicokaratool.server.db.dto.LyricLineDTO;
import hundun.nicokaratool.core.remote.GoogleAiFeignClientImpl;
import hundun.nicokaratool.core.remote.IGoogleAiFeignClient.GenerateContentResponse;
import hundun.nicokaratool.core.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GoogleAiService  extends AiService{

    GoogleAiFeignClientImpl feignClient;


    public GoogleAiService() {
        feignClient = new GoogleAiFeignClientImpl();
        this.step1TaskSplitMinSize = 20;
        this.step1TaskSplitMaxSize = 25;
        this.step2TaskSplitMinSize = 5;
        this.step2TaskSplitMaxSize = 10;
    }
    @Override
    public @Nullable List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate) {
        String ask = step2AskTemplate + "\n" + askLines.stream().collect(Collectors.joining("\n\n"));
        try {
            GenerateContentResponse chatResult = feignClient.singleAsk(ask);
            String content = chatResult.getCandidates()[0].getContent().getParts()[0].getText();
            //content = content.split("</think>")[1].trim();
            content = content.replace("```json", "").replace("```", "");
            List<LyricLineDTO> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, LyricLineDTO.class));
            List<String> resultLines = nodes.stream().map(node -> node.getLyric()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                logNotEquals(askLines, resultLines);
                throw new Exception("resultLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad aiStep2Group: ", e);
        }
        return null;
    }

    @Override
    public @Nullable List<AiStep1ResultNode> aiStep1Group(List<String> askLines, String step1AskTemplate) {
        String ask = step1AskTemplate + askLines.stream().collect(Collectors.joining("\n"));
        try {
            GenerateContentResponse chatResult = feignClient.singleAsk(ask);
            String content = chatResult.getCandidates()[0].getContent().getParts()[0].getText();
            //content = content.split("</think>")[1].trim();
            content = content.replace("```json", "").replace("```", "");
            List<AiStep1ResultNode> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, AiStep1ResultNode.class));
            List<String> resultLines = nodes.stream().flatMap(node -> node.getLyrics().stream()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                logNotEquals(askLines, resultLines);
                throw new Exception("lyricLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad aiStep1Group: ", e);
        }
        return null;
    }
}
