package hundun.nicokaratool.core.japanese;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class JapaneseExtraHint {
    Map<Integer, TranslationResultItem> parsedTokensIndexToMojiHintMap;

    @AllArgsConstructor
    @Builder
    @Data
    public static class TranslationResultItem {
        String jaSearch;
        String jaOrigin;
        List<String> jaWordTags;
        String zhDetail;
    }
}
