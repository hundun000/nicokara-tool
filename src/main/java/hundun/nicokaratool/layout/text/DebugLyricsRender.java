package hundun.nicokaratool.layout.text;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.SubtitleTimeSourceType;

import java.util.stream.Collectors;

public class DebugLyricsRender implements ILyricsRender<JapaneseLine> {
    public static DebugLyricsRender INSTANCE = new DebugLyricsRender();

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
                                    if (time.getTimeSourceType() == SubtitleTimeSourceType.SPECIFIED_COPY) {
                                        collecting.append("~");
                                    }
                                    collecting.append(time.toLyricsTime());
                                });
                            }
                        }
                        collecting.append(c);
                    }
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
