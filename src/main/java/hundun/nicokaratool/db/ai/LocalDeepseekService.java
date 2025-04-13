package hundun.nicokaratool.db.ai;

import hundun.nicokaratool.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.db.OllamaService;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import hundun.nicokaratool.util.JsonUtils;
import io.github.ollama4j.models.chat.OllamaChatResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class LocalDeepseekService implements IAiService {



    @Override
    @Nullable
    public List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplateFile) throws Exception {
        String ask = step2AskTemplateFile + "\n" + askLines.stream().collect(Collectors.joining("\n\n"));
        try {
            OllamaChatResult chatResult = OllamaService.singleAsk(ask);
            String content = chatResult.getResponseModel().getMessage().getContent();
            content = content.split("</think>")[1].trim();
            content = content.replace("```json", "").replace("```", "");
            List<LyricLineDTO> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, LyricLineDTO.class));
            List<String> resultLines = nodes.stream().map(node -> node.getLyric()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                throw new Exception("resultLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad aiStep1Group: ", e);
        }
        return null;
    }

    @Override
    @Nullable
    public List<AiStep1ResultNode> aiStep1Group(List<String> lyricTaskGroup, String step1AskTemplate) {
        String ask = step1AskTemplate + lyricTaskGroup.stream().collect(Collectors.joining("\n"));
        try {
            OllamaChatResult chatResult = OllamaService.singleAsk(ask);
            String content = chatResult.getResponseModel().getMessage().getContent();
            content = content.split("</think>")[1].trim();
            List<AiStep1ResultNode> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, AiStep1ResultNode.class));
            List<String> resultLines = nodes.stream().flatMap(node -> node.getLyrics().stream()).collect(Collectors.toList());
            if (!lyricTaskGroup.equals(resultLines)) {
                throw new Exception("lyricLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad aiStep1Group: ", e);
        }
        return null;
    }
}
