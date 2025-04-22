package hundun.nicokaratool.server.db.ai;

import hundun.nicokaratool.server.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.server.db.dto.LyricLineDTO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class AiServiceLocator extends AiService {

    static final int STEP1_RETRY = 3;

    ManualDeepseekService manualDeepseekService = new ManualDeepseekService();
    LocalDeepseekService localDeepseekService = new LocalDeepseekService();
    GoogleAiService googleAiService = new GoogleAiService();
    AiService aiService = googleAiService;

    @Override
    public @Nullable List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate) {
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
    public @Nullable List<AiStep1ResultNode> aiStep1Group(List<String> lyricTaskGroup, String step1AskTemplate) {
        List<AiStep1ResultNode> groupResult = null;
        int retry = 0;
        while (groupResult == null && retry < 3) {
            log.info("start aiStep1Group, retry = {}", retry);
            groupResult = aiService.aiStep1Group(lyricTaskGroup, step1AskTemplate);
            retry++;
        }
        return groupResult;
    }

    /**
     *将List<T>分为每组大小介于[MIN_SIZE, MAX_SIZE]的List<List<T>> (仅最后一组个数可能不足)；
     * 分组期间，如果某组已满足大于MIN_SIZE时，emptyChecker触发，则提前结束这一组；否则继续增加直到MAX_SIZE；
     * 若MIN_SIZE和MAX_SIZE为负数，效果为不变；
     */
    public <T> List<List<T>> splitAiTaskGroups(List<List<T>> originGroups, Function<List<T>, Boolean> emptyChecker, int MIN_SIZE, int MAX_SIZE) {
        originGroups = new ArrayList<>(originGroups);
        List<List<T>> resultGroups = new ArrayList<>();
        List<T> currentGroup = null;
        List<T> addToNextGroup = null;
        while (!originGroups.isEmpty() || addToNextGroup != null) {
            if (currentGroup == null) {
                currentGroup = new ArrayList<>();
                resultGroups.add(currentGroup);
            }
            List<T> originGroup;
            // 取一个新的，或是用上一个循环未使用的
            if (addToNextGroup == null) {
                originGroup = originGroups.remove(0);
            } else {
                originGroup = addToNextGroup;
                addToNextGroup = null;
            }
            if (currentGroup.size() + originGroup.size() <= MIN_SIZE) {
            } else if (currentGroup.size() + originGroup.size() < MAX_SIZE) {
                if (emptyChecker.apply(originGroup)) {
                    addToNextGroup = originGroup;
                }
            } else if (currentGroup.isEmpty()) {
            } else {
                addToNextGroup = originGroup;
            }
            if (addToNextGroup != null) {
                currentGroup = null;
            } else {
                if (!emptyChecker.apply(originGroup)) {
                    currentGroup.addAll(originGroup);
                }
            }
        }
        return resultGroups;
    }

    @Override
    public int getStep1TaskSplitMaxSize() {
        return aiService.getStep1TaskSplitMaxSize();
    }

    @Override
    public int getStep1TaskSplitMinSize() {
        return aiService.getStep1TaskSplitMinSize();
    }

    @Override
    public int getStep2TaskSplitMaxSize() {
        return aiService.getStep2TaskSplitMaxSize();
    }

    @Override
    public int getStep2TaskSplitMinSize() {
        return aiService.getStep2TaskSplitMinSize();
    }
}
