package demo.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseRubyCollector<T_TOKEN> {
    static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
        ;
    }
    protected abstract void handleToken(T_TOKEN token, Map<String, KanjiInfo> kanjiInfoMap);

    protected KanjiHint toKanjiHint(KanjiInfo thiz) {
        var hintMap = thiz.getPronunciationMap().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> {
                            var pronunciation = entry.getKey();
                            var sources = entry.getValue();
                            var hintNode = sources.stream()
                                    .map(source -> String.format("%s,[00:00:00],[99:99:99] // from %s",
                                            pronunciation,
                                            source
                                    ))
                                    .collect(Collectors.toList());
                            return hintNode;
                        })
                );
        return KanjiHint.builder()
                .kanji(thiz.getKanji())
                .hintMap(hintMap)
                .build();
    }

    protected void appendAsRuby(KanjiInfo thiz, List<String> lines) {
        if (thiz.getPronunciationMap().size() == 1) {
            lines.add(String.format("@Ruby%d=%s,%s",
                    lines.size() + 1,
                    thiz.getKanji(),
                    thiz.getPronunciationMap().entrySet().iterator().next().getKey()
            ));
        } else {
            thiz.getPronunciationMap().forEach((pronunciation, sources) -> {
                sources.forEach(source -> {
                    lines.add(String.format("@Ruby%d=%s,%s,[00:00:00],[99:99:99] // from %s",
                            lines.size() + 1,
                            thiz.getKanji(),
                            pronunciation,
                            source
                    ));
                });
            });
        }
    }

    public void newKanjiHintsFile(File kanjiHintsFile, Map<String, KanjiInfo> kanjiInfoMap) {

        List<KanjiHint> list = kanjiInfoMap.values().stream()
                .filter(it -> it.getPronunciationMap().size() > 1)
                .map(it -> toKanjiHint(it))
                .collect(Collectors.toList());
        try {
            objectMapper.writeValue(kanjiHintsFile, list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @SuppressWarnings("unchecked")
    public static List<KanjiHint> readKanjiHintsFile(File kanjiHintsFile) {
        try {
            return (List<KanjiHint>) objectMapper.readValue(kanjiHintsFile, objectMapper.getTypeFactory().constructCollectionType(List.class, KanjiHint.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String collectRuby(List<T_TOKEN> MyTokens, File kanjiHintsFile) {
        Map<String, KanjiInfo> kanjiInfoMap = new HashMap<>();
        MyTokens.stream()
                .forEach(it -> {
                    handleToken(it, kanjiInfoMap);
                });

        if (!kanjiHintsFile.exists()) {
            newKanjiHintsFile(kanjiHintsFile, kanjiInfoMap);
        }
        var kanjiHintsMap = readKanjiHintsFile(kanjiHintsFile).stream()
                .collect(Collectors.toMap(
                        it -> it.getKanji(),
                        it -> it
                ));


        List<String> lines = new ArrayList<>();
        kanjiInfoMap.forEach((kanji, kanjiInfo) -> {
            if (kanjiHintsMap.containsKey(kanji)) {
                kanjiHintsMap.get(kanji).appendAsRuby(lines);
            } else {
                appendAsRuby(kanjiInfo, lines);
            }
        });

        return String.join("\n", lines);
    }

}
