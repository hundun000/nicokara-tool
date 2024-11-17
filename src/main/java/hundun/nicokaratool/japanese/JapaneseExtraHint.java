package hundun.nicokaratool.japanese;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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
        String jaText;
        String zhText;
    }
}
