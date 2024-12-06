package hundun.nicokaratool.base;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.japanese.TagTokenizer.Timestamp;
import lombok.*;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class KanjiPronunciationPackage {
    String kanji;
    Map<String, List<SourceInfo>> pronunciationMap;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class SourceInfo {
        String sourceLyricLineText;
        Timestamp start;
        Timestamp end;
        boolean fromUnknownTimestamp;

        public static SourceInfo fromUnknownTimestamp(String text) {
            return SourceInfo.builder()
                    .sourceLyricLineText(text)
                    .start(null)
                    .end(null)
                    .fromUnknownTimestamp(true)
                    .build();
        }

        public static SourceInfo fromSubToken(JapaneseSubToken subToken) {
            return SourceInfo.builder()
                    .sourceLyricLineText(subToken.getSource())
                    .start(subToken.getStart())
                    .end(subToken.getEnd())
                    .fromUnknownTimestamp(false)
                    .build();
        }
    }



}
