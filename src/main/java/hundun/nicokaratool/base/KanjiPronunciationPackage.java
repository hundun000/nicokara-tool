package hundun.nicokaratool.base;

import hundun.nicokaratool.base.lyrics.LyricLine.LyricTimestamp;
import hundun.nicokaratool.base.lyrics.LyricLine.LyricToken;
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
        LyricTimestamp start;
        LyricTimestamp end;
        boolean fromUnknownTimestamp;

        public static SourceInfo fromUnknownTimestamp(String text) {
            return SourceInfo.builder()
                    .sourceLyricLineText(text)
                    .start(LyricTimestamp.parseType2("[00:00:00]"))
                    .end(LyricTimestamp.parseType2("[99:99:99]"))
                    .fromUnknownTimestamp(true)
                    .build();
        }

        public static SourceInfo fromLyricToken(LyricToken token) {
            return SourceInfo.builder()
                    .sourceLyricLineText("UNKNOWN")
                    .start(token.getStart())
                    .end(token.getEnd())
                    .build();
        }
    }



}
