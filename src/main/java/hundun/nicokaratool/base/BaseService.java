package hundun.nicokaratool.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.KanjiHintPO.PronounceHint;
import hundun.nicokaratool.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseService<T_TOKEN> {
    protected ObjectMapper normalObjectMapper = new ObjectMapper();
    protected ObjectMapper fileObjectMapper = new ObjectMapper();

    protected BaseService() {
        fileObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    protected abstract List<T_TOKEN> toMyTokenList(List<String> list);

    protected abstract Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<T_TOKEN> lines);

        public ServiceResult work(String name) throws IOException {


        List<String> list = Utils.readAllLines("data/" + name + ".txt");
        List<T_TOKEN> myTokens = toMyTokenList(list);
        Map<String, KanjiPronunciationPackage> packageMap = calculateKanjiPronunciationPackageMap(myTokens);


        RootHint rootHint;
        File rootHintFile = new File("data/" + name + ".rootHint.json");
        if (rootHintFile.exists()) {
            rootHint = fileObjectMapper.readValue(rootHintFile, RootHint.class);
        } else {
            List<KanjiHintPO> kanjiHintPOS = packageMap.values().stream()
                    .filter(it -> it.getPronunciationMap().size() > 1)
                    .map(it -> toKanjiHint(it))
                    .collect(Collectors.toList());
            rootHint = RootHint.builder()
                    .kanjiHints(kanjiHintPOS)
                    .build();
            fileObjectMapper.writeValue(rootHintFile, rootHint);
        }

        Map<String, KanjiHintPO> kanjiHintsMap = rootHint.getKanjiHints().stream()
                .collect(Collectors.toMap(
                        it -> it.getKanji(),
                        it -> it));
        List<String> rubyList = new ArrayList<>();
        packageMap.forEach((kanji, kanjiInfo) -> {
            if (kanjiHintsMap.containsKey(kanji)) {
                KanjiHintPO po = kanjiHintsMap.get(kanji);
                appendToRubyLines(po, rubyList);
            } else {
                appendToRubyLines(kanjiInfo, rubyList);
            }
        });
        String ruby = String.join("\n", rubyList);

        return ServiceResult.builder()
                .list(list)
                .ruby(ruby)
                .build();
    }

    protected KanjiHintPO toKanjiHint(KanjiPronunciationPackage thiz) {
        var pronounceHints = thiz.getPronunciationMap().entrySet().stream()
                .map(entry -> {
                    String pronunciation = entry.getKey();
                    var rubyLines = entry.getValue().stream()
                            .map(source -> String.format("%s,[00:00:00],[99:99:99] // from %s",
                                    pronunciation,
                                    source.getSourceLyricLineText()
                            ))
                            .collect(Collectors.toList());
                    return PronounceHint.builder()
                            .pronounce(pronunciation)
                            .rubyLines(rubyLines)
                            .build();
                })
                .collect(Collectors.toList());
        return KanjiHintPO.builder()
                .kanji(thiz.getKanji())
                .pronounceHints(pronounceHints)
                .build();
    }

    protected void appendToRubyLines(KanjiPronunciationPackage bo, List<String> lines) {

        if (bo.getPronunciationMap().size() < 2) {
            String line = String.format("@Ruby%d=%s,%s",
                    lines.size() + 1,
                    bo.getKanji(),
                    bo.getPronunciationMap().entrySet().iterator().next().getKey()
            );
            lines.add(line);
        } else {
            bo.getPronunciationMap().forEach((pronunciation, sources) -> {
                sources.forEach(source -> {
                    String line = String.format("@Ruby%d=%s,%s",
                            lines.size() + 1,
                            bo.getKanji(),
                            pronunciation
                    );
                    if (bo.getPronunciationMap().size() > 1) {
                        line += String.format(",%s,%s",
                                source.getStart().toStringTypeNicoKara(),
                                source.getEnd().toStringTypeNicoKara()
                        );
                        if (source.isFromUnknownTimestamp()) {
                            line += " // from " + source.getSourceLyricLineText();
                        }
                    }
                    lines.add(line);
                });
            });
        }
    }

    protected void appendToRubyLines(KanjiHintPO po, List<String> lines) {
        po.getPronounceHints().forEach(pronounceHint -> {
            pronounceHint.getRubyLines().forEach(rubyLine -> {
                lines.add(String.format("@Ruby%d=%s,%s",
                        lines.size() + 1,
                        po.kanji,
                        rubyLine
                ));
            });
        });
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ServiceResult {
        List<String> list;
        String ruby;
    }

}
