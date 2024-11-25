package hundun.nicokaratool.layout;

import hundun.nicokaratool.japanese.JapaneseService;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;

import java.util.stream.Collectors;

public class SimpleLyricsRender implements ILyricsRender<JapaneseLine> {
    public static SimpleLyricsRender INSTANCE = new SimpleLyricsRender();

    @Override
    public String toLyricsLine(JapaneseLine japaneseLine) {
        return japaneseLine.getParsedTokens().stream()
                .flatMap(it -> it.getSubTokens().stream())
                .map(it -> {
                    if (it.typeKanji()) {
                        return String.format("%s(%s)",
                                it.getKanji(),
                                it.getFurigana()
                        );
                    } else {
                        return it.getKana();
                    }
                })
                .collect(Collectors.joining());
    }
}
