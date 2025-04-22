package hundun.nicokaratool.core.layout.text;

import hundun.nicokaratool.core.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.core.japanese.JapaneseService.SubtitleTimeSourceType;

import java.util.stream.Collectors;

public class NicokaraLyricsRender implements ILyricsRender<JapaneseLine> {
    public static NicokaraLyricsRender INSTANCE = new NicokaraLyricsRender();

    @Override
    public String toLyricsLine(JapaneseLine japaneseLine) {
        return japaneseLine.getParsedTokens().stream()
                .map(it -> {
                    StringBuilder collecting = new StringBuilder();
                    for (int i = 0; i < it.getSurface().length(); i++) {
                        char c = it.getSurface().charAt(i);
                        if (it.getDetailedTimeMap() != null) {
                            if (it.getDetailedTimeMap().containsKey(i)) {
                                it.getDetailedTimeMap().get(i).forEach(time -> {
                                    if (time.getTimeSourceType() == SubtitleTimeSourceType.SPECIFIED) {
                                        collecting.append(time.toLyricsTime());
                                    }
                                });
                            }
                        }
                        collecting.append(c);
                    }
                    // 额外检查index=it.getSurface().length()
                    if (it.getDetailedTimeMap() != null) {
                        if (it.getDetailedTimeMap().containsKey(it.getSurface().length())) {
                            it.getDetailedTimeMap().get(it.getSurface().length()).forEach(time -> {
                                collecting.append(time.toLyricsTime());
                            });
                        }
                    }
                    return collecting;
                })
                .collect(Collectors.joining()) + "\n";
    }
}
