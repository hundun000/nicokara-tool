package hundun.nicokaratool.base.lyrics;

import hundun.nicokaratool.cantonese.PycantoneseFeignClient;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleRequest;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LyricLine {
    List<LyricToken> nodes;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LyricToken {
        LyricTimestamp start;
        String kanji;
        String yalePronunciation;
        LyricTimestamp end;

        public static LyricToken space() {
            return LyricToken.builder()
                    .kanji(" ")
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LyricTimestamp {
        int minute;
        int second;
        int micro;

        public String toStringType1() {
            return String.format("[%s:%s.%s]",
                    String.format("%02d", minute),
                    String.format("%02d", second),
                    String.format("%03d", micro)
                    );
        }

        public String toStringTypeNicoKara() {
            return String.format("[%s:%s:%s]",
                    String.format("%02d", minute),
                    String.format("%02d", second),
                    String.format("%02d", micro / 10)
            );
        }

        public static int parseInt(String text) {
            while (text.startsWith("0") && text.length() > 1) {
                text = text.substring(1);
            }
            return Integer.parseInt(text);
        }
        public static int TYPE_1_LENGTH = 11;

        /**
         * [xx:xx.xxx]
         */
        public static LyricTimestamp parseType1(String text) {
            String part1 = text.substring(1, 3);
            String part2 = text.substring(4, 6);
            String part3 = text.substring(7, 10);
            return LyricTimestamp.builder()
                    .minute(parseInt(part1))
                    .second(parseInt(part2))
                    .micro(parseInt(part3))
                    .build();
        }

        /**
         * [xx:xx:xx]
         */
        public static LyricTimestamp parseType2(String text) {
            String part1 = text.substring(1, 3);
            String part2 = text.substring(4, 6);
            String part3 = text.substring(7, 9);
            return LyricTimestamp.builder()
                    .minute(parseInt(part1))
                    .second(parseInt(part2))
                    .micro(parseInt(part3))
                    .build();
        }
    }




}
