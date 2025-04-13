package hundun.nicokaratool.db.ai;

import hundun.nicokaratool.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Slf4j
public class AiServiceLocator implements IAiService {

    static final int STEP1_RETRY = 3;


    IAiService aiService = new LocalDeepseekService();

    @Override
    @Nullable
    public List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate) throws Exception {
        List<LyricLineDTO> groupResult = null;
        int retry = 0;
        while (groupResult == null && retry < STEP1_RETRY) {
            log.info("start aiStep2Group, retry = {}", retry);
            groupResult = aiService.aiStep2Group(askLines, step2AskTemplate);
            retry++;
        }
        return groupResult;
    }

    @Override
    @Nullable
    public List<AiStep1ResultNode> aiStep1Group(List<String> lyricTaskGroup, String step1AskTemplate) {
        List<AiStep1ResultNode> groupResult = null;
        int retry = 0;
        while (groupResult == null && retry < 3) {
            log.info("start aiStep1Group, retry = {}", retry);
            groupResult = aiService.aiStep1Group(lyricTaskGroup, step1AskTemplate);
            retry++;
        }
        return groupResult;
    }
}
