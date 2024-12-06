package hundun.nicokaratool.layout.text;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.SubtitleTimeSourceType;
import hundun.nicokaratool.japanese.TagTokenizer.TagToken;

import java.util.stream.Collectors;

public class DebugLyricsRender implements ILyricsRender<JapaneseLine> {
    public static DebugLyricsRender INSTANCE = new DebugLyricsRender();

    @Override
    public String toLyricsLine(JapaneseLine japaneseLine) {
        return japaneseLine.getParsedTokens().stream()
                .map(parsedToken -> {
                    return parsedToken.getSubTokens().stream()
                            .map(subToken -> {
                                return  (subToken.isSpecifiedStart() ? subToken.getStart().toLyricsTime(true) : "")
                                            + subToken.getSurface()
                                            + (subToken.isSpecifiedEnd() ? subToken.getEnd().toLyricsTime(false) : "")
                                            ;
                            })
                            .collect(Collectors.joining());
                })
                .collect(Collectors.joining()) + "\n";
    }
}
