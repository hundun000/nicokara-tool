package hundun.nicokaratool.japanese;

import hundun.nicokaratool.japanese.JapaneseService.SubtitleTimeSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagTokenizer {


    public List<TagToken> parse(String text) {
        List<String> absoluteTimeTagParts = new ArrayList<>();
        Pattern pattern1 = Pattern.compile("\\[[0-9]{2}:[0-9]{2}(:|.)[0-9]{2,3}]");
        Matcher matcher1 = pattern1.matcher(text);
        while (matcher1.find()) {
            absoluteTimeTagParts.add(matcher1.group());
        }

        // 提取 {\k00} 格式的文本
        Pattern pattern2 = Pattern.compile("\\{\\\\k\\d+}");
        Matcher matcher2 = pattern2.matcher(text);
        List<String> offsetTimeTagParts = new ArrayList<>();
        while (matcher2.find()) {
            offsetTimeTagParts.add(matcher2.group());
        }

        String unusedText = text;
        List<TagToken> results = new ArrayList<>();
        int absoluteTimeTagPartCurrent = 0;
        int offsetTimeTagPartCurrent = 0;
        StringBuilder textTypeCurrent = new StringBuilder();
        while (!unusedText.isEmpty()) {
            boolean matchAbsoluteTimeTag = absoluteTimeTagPartCurrent < absoluteTimeTagParts.size() && unusedText.startsWith(absoluteTimeTagParts.get(absoluteTimeTagPartCurrent));
            boolean matchOffsetTimeTag = offsetTimeTagPartCurrent < offsetTimeTagParts.size() && unusedText.startsWith(offsetTimeTagParts.get(offsetTimeTagPartCurrent));
            if ((matchAbsoluteTimeTag || matchOffsetTimeTag) && (textTypeCurrent.length() > 0)) {
                results.add(TagToken.create(textTypeCurrent.toString()));
                textTypeCurrent.setLength(0);
            }

            if (matchAbsoluteTimeTag) {
                results.add(TagToken.create(absoluteTimeTagParts.get(absoluteTimeTagPartCurrent)));
                unusedText = unusedText.substring(absoluteTimeTagParts.get(absoluteTimeTagPartCurrent).length());
                absoluteTimeTagPartCurrent++;
                continue;
            }

            if (matchOffsetTimeTag) {
                results.add(TagToken.create(offsetTimeTagParts.get(offsetTimeTagPartCurrent)));
                unusedText = unusedText.substring(offsetTimeTagParts.get(offsetTimeTagPartCurrent).length());
                offsetTimeTagPartCurrent++;
                continue;
            }
            textTypeCurrent.append(unusedText.charAt(0));
            unusedText = unusedText.substring(1);
        }

        if (textTypeCurrent.length() > 0) {
            results.add(TagToken.create(textTypeCurrent.toString()));
            textTypeCurrent.setLength(0);
        }

        return results;

    }



    public enum TagTokenType {
        TEXT,
        TIME_TAG,
        BAD
    }

    @AllArgsConstructor
    @Builder
    @Data
    public static class Timestamp {
        int minute;
        int second;
        int millisecond;

        public String toLyricsTime() {
            return String.format(
                    "[%02d:%02d:%02d]",
                    minute,
                    second,
                    millisecond / 10
            );
        }

        public String toLyricsTime(boolean start) {
            return String.format(
                    start ? "([%02d:%02d:%02d]" : "[%02d:%02d:%02d])",
                    minute,
                    second,
                    millisecond / 10
            );
        }

        public String toFfmpegTime() {
            return String.format(
                    "%02d:%02d:%02d.%03d",
                    0,
                    minute,
                    second,
                    millisecond / 10
            );
        }

        public static String unknownLyricsTime() {
            return "[??:??:??]";
        }

        public long totalMs() {
            return (minute * 60 + second) * 1000 + millisecond;
        }

        public long totalSec() {
            return (minute * 60 + second);
        }

        public static Timestamp parse(String text) {
            if (text.startsWith("[")) {
                int i = 1;
                int timeMinute = Integer.parseInt(text.substring(i, i + 2));
                i += 3;
                int timeSecond = Integer.parseInt(text.substring(i, i + 2));
                i += 3;
                int timeMillisecond;
                if (text.charAt(i + 2) == ']') {
                    timeMillisecond = Integer.parseInt(text.substring(i, i + 2)) * 10;
                } else {
                    timeMillisecond = Integer.parseInt(text.substring(i, i + 3));
                }
                return Timestamp.builder()
                                .minute(timeMinute)
                                .second(timeSecond)
                                .millisecond(timeMillisecond)
                                .build();
            }

            return null;
        }
    }

    @AllArgsConstructor
    @Builder
    @Data
    public static class TagToken {
        String text;

        int timeOffset;
        Timestamp timestamp;
        TagTokenType type;
        SubtitleTimeSourceType timeSourceType;

        public static TagToken create(String text) {
            return create(text, SubtitleTimeSourceType.SPECIFIED);
        }
        public static TagToken create(String text, SubtitleTimeSourceType timeSourceType) {
            if (text.startsWith("[")) {
                return TagToken.builder()
                        .timestamp(Timestamp.parse(text))
                        .text(text)
                        .type(TagTokenType.TIME_TAG)
                        .timeSourceType(timeSourceType)
                        .build();
            } else if (text.startsWith("{\\k")) {
                int timeOffset = Integer.parseInt(text.substring("{\\k".length(), text.length() - 1));
                return TagToken.builder()
                        .timeOffset(timeOffset)
                        .text(text)
                        .type(TagTokenType.TIME_TAG)
                        .timeSourceType(timeSourceType)
                        .build();
            }


            return TagToken.builder()
                    .text(text)
                    .type(TagTokenType.TEXT)
                    .build();
        }

        public String toLyricsTime() {
            return timestamp.toLyricsTime();
        }


    }


}
