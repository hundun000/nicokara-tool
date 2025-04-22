package hundun.nicokaratool.server.db.ai;

import hundun.nicokaratool.server.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.server.db.dto.LyricLineDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AiService {

    @Getter
    protected int step1TaskSplitMinSize;
    @Getter
    protected int step1TaskSplitMaxSize;
    @Getter
    protected int step2TaskSplitMinSize;
    @Getter
    protected int step2TaskSplitMaxSize;
    public abstract @Nullable List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate);

    public abstract @Nullable List<AiStep1ResultNode> aiStep1Group(List<String> lyricTaskGroup, String step1AskTemplate);

    protected void logNotEquals(List<String> list1, List<String> list2) {
        List<String> same = new ArrayList<>(list1);
        same.retainAll(list2);
        List<String> list1Diff = new ArrayList<>(list1);
        list1Diff.removeAll(same);
        List<String> list2Diff = new ArrayList<>(list2);
        list2Diff.removeAll(same);
        log.warn("NotEquals list1Diff = {}, list2Diff = {}", list1Diff, list2Diff);
    }

    protected String formatAiResult(String content) {
        return content.replace("```json", "")
                .replace("```", "")
                .replace("　"," ");
    }

}
