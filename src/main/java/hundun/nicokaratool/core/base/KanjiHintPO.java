package hundun.nicokaratool.core.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class KanjiHintPO {
    String kanji;
    List<PronounceHint> pronounceHints;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class PronounceHint {
        String pronounce;
        List<String> rubyLines;
    }

}
