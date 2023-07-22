package demo.japanese;

import demo.base.KanjiInfo;
import demo.base.BaseRubyCollector;
import demo.japanese.NicokaraRunner.JapaneseSubToken;
import demo.japanese.NicokaraRunner.JapaneseToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JapaneseRubyCollector extends BaseRubyCollector<JapaneseToken> {

    @Override
    protected void handleToken(JapaneseToken it, Map<String, KanjiInfo> kanjiInfoMap) {
        if (it.nodes == null) {
            return;
        }
        it.nodes.stream()
                .forEach(node -> {
                    if (node.kanji != null) {
                        if (!kanjiInfoMap.containsKey(node.kanji)) {
                            kanjiInfoMap.put(node.kanji, KanjiInfo.builder()
                                    .kanji(node.kanji)
                                    .pronunciationMap(new HashMap<>())
                                    .build());
                        }
                        merge(kanjiInfoMap.get(node.kanji), node);
                    }
                });
    }


    public void merge(KanjiInfo thiz, JapaneseSubToken node) {
        if (!thiz.getPronunciationMap().containsKey(node.getKanjiPronunciation())) {
            thiz.getPronunciationMap().put(node.getKanjiPronunciation(), new ArrayList<>());
        }
        thiz.getPronunciationMap().get(node.getKanjiPronunciation()).add(node.getSource());
    }
}
