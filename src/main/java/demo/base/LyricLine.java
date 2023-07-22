package demo.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LyricLine {
    List<LyricLineNode> nodes;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LyricLineNode {
        LyricTimestamp start;
        String content;
        LyricTimestamp end;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LyricTimestamp {
        int minute;
        int second;
        int micro;

        public static int parseInt(String text) {
            while (text.startsWith("0")) {
                text = text.substring(1);
            }
            return Integer.parseInt(text);
        }
        static int TYPE_1_LENGTH = 11;
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
    }

    public static LyricLine parseOneNodeLine(String text) {
        LyricLineNode node = new LyricLineNode();
        if (text.startsWith("[")) {
            node.setStart(LyricTimestamp.parseType1(text.substring(0, LyricTimestamp.TYPE_1_LENGTH)));
            text = text.substring(LyricTimestamp.TYPE_1_LENGTH);
        }
        if (text.endsWith("]")) {
            node.setEnd(LyricTimestamp.parseType1(text.substring(text.length() - LyricTimestamp.TYPE_1_LENGTH)));
            text = text.substring(0, text.length() - LyricTimestamp.TYPE_1_LENGTH);
        }
        node.setContent(text);
        return LyricLine.builder()
                .nodes(List.of(node))
                .build();
    }


}
