package hundun.nicokaratool.db.ai;

import hundun.nicokaratool.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IAiService {
    @Nullable
    List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate) throws Exception;

    @Nullable
    List<AiStep1ResultNode> aiStep1Group(List<String> lyricTaskGroup, String step1AskTemplate);
}
