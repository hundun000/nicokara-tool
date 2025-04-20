package hundun.nicokaratool.db.ai;

import hundun.nicokaratool.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import hundun.nicokaratool.remote.GoogleAiFeignClientImpl;
import hundun.nicokaratool.remote.IGoogleAiFeignClient.GenerateContentResponse;
import hundun.nicokaratool.remote.OllamaService;
import hundun.nicokaratool.util.JsonUtils;
import io.github.ollama4j.models.chat.OllamaChatResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GoogleAiService  extends AiService{

    GoogleAiFeignClientImpl feignClient;


    public GoogleAiService() {
        feignClient = new GoogleAiFeignClientImpl();
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
    public @Nullable List<AiStep1ResultNode> aiStep1Group(List<String> lyricTaskGroup, String step1AskTemplate) {
        return List.of();
    }
}
