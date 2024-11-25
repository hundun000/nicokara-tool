package hundun.nicokaratool.layout;

import hundun.nicokaratool.japanese.JapaneseService;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.SubtitleTimeSourceType;
import hundun.nicokaratool.japanese.TagTokenizer.TagToken;
import hundun.nicokaratool.japanese.TagTokenizer.TagTokenType;

import java.util.stream.Collectors;

public class NicokaraLyricsRender implements ILyricsRender<JapaneseLine> {
    public static NicokaraLyricsRender INSTANCE = new NicokaraLyricsRender();

    @Override
    public String toLyricsLine(JapaneseLine japaneseLine) {
        return japaneseLine.getParsedTokens().stream()
                .flatMap(it -> it.getSubTokens().stream())
                .map(it -> it.getSurface())
                .collect(Collectors.joining());
    }
}
