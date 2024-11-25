package hundun.nicokaratool.layout;

import hundun.nicokaratool.japanese.JapaneseService;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.TagTokenizer.TagToken;
import hundun.nicokaratool.japanese.TagTokenizer.TagTokenType;

public class NicokaraLyricsRender implements ILyricsRender<JapaneseLine> {
    public static NicokaraLyricsRender INSTANCE = new NicokaraLyricsRender();

    @Override
    public String toLyricsLine(JapaneseLine japaneseLine) {
        StringBuilder result = new StringBuilder();
        var tagTokenIterator = japaneseLine.getTagTokens().iterator();
        var subTokenIterator = japaneseLine.getParsedTokens().stream()
                .flatMap(it -> it.getSubTokens().stream())
                .flatMap(it -> it.getSurface().chars().mapToObj(itt -> (char) itt))
                .iterator();
        while (tagTokenIterator.hasNext()) {
            TagToken tagToken = tagTokenIterator.next();
            if (tagToken.getType() == TagTokenType.TEXT) {
                StringBuilder collectingText = new StringBuilder();
                while (!collectingText.toString().equals(tagToken.getText())) {
                    if (subTokenIterator.hasNext()) {
                        collectingText.append(subTokenIterator.next());
                    } else {
                        throw new RuntimeException("collectingText = " + collectingText + ", target = " + tagToken.getText() + ", not found.");
                    }
                }
                result.append(collectingText);
            } else {
                result.append(tagToken.toLyricsTime());
            }
        }
        return result.toString() + "\n";
    }
}
