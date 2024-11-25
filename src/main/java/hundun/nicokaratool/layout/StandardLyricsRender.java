package hundun.nicokaratool.layout;

import hundun.nicokaratool.base.lyrics.LyricLine;

import java.util.stream.Collectors;

public class StandardLyricsRender implements ILyricsRender<LyricLine> {
    public static StandardLyricsRender INSTANCE = new StandardLyricsRender();

    @Override
    public String toLyricsLine(LyricLine standardLine) {
        return standardLine.getNodes().stream()
                .map(it -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    if (it.getStart() != null) {
                        stringBuilder.append(it.getStart().toStringTypeNicoKara());
                    }
                    stringBuilder.append(it.getKanji());
                    if (it.getEnd() != null) {
                        stringBuilder.append(it.getEnd().toStringTypeNicoKara());
                    }
                    return stringBuilder.toString();
                })
                .collect(Collectors.joining());
    }
}
