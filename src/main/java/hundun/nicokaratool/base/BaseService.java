package hundun.nicokaratool.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.KanjiHintPO.PronounceHint;
import hundun.nicokaratool.layout.ILyricsRender;
import hundun.nicokaratool.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseService<T_PARSED_LINE> {
    public static final String CACHE_FOLDER = "data/caches/";
    public static final String RUNTIME_IO_FOLDER = "runtime-io/";
    protected ObjectMapper normalObjectMapper = new ObjectMapper();
    protected ObjectMapper fileObjectMapper = new ObjectMapper();
    ILyricsRender<T_PARSED_LINE> lyricsRender;
    protected BaseService(ILyricsRender<T_PARSED_LINE> lyricsRender) {
        this.lyricsRender = lyricsRender;
        fileObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    protected abstract List<T_PARSED_LINE> toParsedLines(List<String> list, @Nullable RootHint rootHint);
    protected abstract Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<T_PARSED_LINE> lines);

    public ServiceResult<T_PARSED_LINE> workStep1(String name) throws IOException {

        boolean needCreateRootHint = false;

        List<String> lines = Utils.readAllLines(RUNTIME_IO_FOLDER + name + ".txt");
        RootHint rootHint;
        File rootHintFile = new File(RUNTIME_IO_FOLDER + name + ".rootHint.json");
        if (rootHintFile.exists()) {
            rootHint = fileObjectMapper.readValue(rootHintFile, RootHint.class);
        } else {
            needCreateRootHint = true;
            rootHint = null;
        }

        List<T_PARSED_LINE> parsedLines = toParsedLines(lines, rootHint);
        Map<String, KanjiPronunciationPackage> packageMap = calculateKanjiPronunciationPackageMap(parsedLines);

        if (needCreateRootHint) {
            List<KanjiHintPO> kanjiHintPOS = packageMap.values().stream()
                    .filter(it -> it.getPronunciationMap().size() > 1)
                    .map(it -> toKanjiHint(it))
                    .collect(Collectors.toList());
            rootHint = RootHint.builder()
                    .kanjiHints(kanjiHintPOS)
                    .nluDisallowHints(new ArrayList<>(0))
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


        String lyricsText = parsedLines.stream()
                .map(it -> lyricsRender.toLyricsLine(it))
                .collect(Collectors.joining("\n"));
        return ServiceResult.<T_PARSED_LINE>builder()
                .lines(parsedLines)
                .lyricsText(lyricsText)
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
    public static class ServiceResult<T_PARSED_LINE> {
        List<T_PARSED_LINE> lines;
        String lyricsText;
        String ruby;
    }

}
