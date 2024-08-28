package hundun.nicokaratool.japanese;

import hundun.nicokaratool.base.BaseService;
import hundun.nicokaratool.base.KanjiPronunciationPackage;
import hundun.nicokaratool.base.KanjiPronunciationPackage.SourceInfo;
import hundun.nicokaratool.base.lyrics.ILyricsRender;
import hundun.nicokaratool.base.RootHint;
import hundun.nicokaratool.japanese.IMojiHelper.SimpleMojiHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * @author hundun
 * Created on 2023/03/08
 */
public class JapaneseService extends BaseService<JapaneseLine> {
    

    static IMojiHelper mojiHelper = new SimpleMojiHelper();
    static final Tokenizer tokenizer = new Tokenizer();

    protected JapaneseService() {
        super(NicokaraLyricsRender.INSTANCE);
    }

    /**
     * type 1: kanji + furigana;
     * type 2: kana;
     */
    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseSubToken {
        String kanji;
        String furigana;
        String kana;
        String source;
    }
    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseLine {
        List<JapaneseParsedToken> parsedTokens;
        String chinese;
    }

    public static class SimpleLyricsRender implements ILyricsRender<JapaneseLine> {
        public static SimpleLyricsRender INSTANCE = new SimpleLyricsRender();
        @Override
        public String toLyricsLine(JapaneseLine japaneseLine) {
            return japaneseLine.parsedTokens.stream()
                    .flatMap(it -> it.getSubTokens().stream())
                    .map(it -> {
                        if (it.kanji != null) {
                            return String.format("%s(%s)",
                                    it.kanji,
                                    it.furigana
                            );
                        } else {
                            return it.kana;
                        }
                    })
                    .collect(Collectors.joining());
        }
    }

    public static class NicokaraLyricsRender implements ILyricsRender<JapaneseLine> {
        public static NicokaraLyricsRender INSTANCE = new NicokaraLyricsRender();

        @Override
        public String toLyricsLine(JapaneseLine japaneseLine) {
            return japaneseLine.parsedTokens.stream()
                    .flatMap(parsedToken -> parsedToken.getSubTokens().stream())
                    .map(subToken -> {
                        StringBuilder stringBuilder = new StringBuilder();
                        if (subToken.kanji != null) {
                            stringBuilder.append(subToken.kanji);
                        } else {
                            stringBuilder.append(subToken.kana);
                        }
                        return stringBuilder.toString();
                    })
                    .collect(Collectors.joining()) + "\n";
        }
    }


    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseParsedToken {
        String des;
        List<JapaneseSubToken> subTokens;
    }
    
    private static List<JapaneseSubToken> parseKanjiToken(Token token) {
        List<JapaneseSubToken> subTokens = new ArrayList<>();
        String surface = token.getSurface();
        String rawPronunciation = token.getReading();

        {
            JapaneseSubToken currentSubToken = null;
            for (int i = 0; i < surface.length(); i++) {
                String c = String.valueOf(surface.charAt(i));
                if (mojiHelper.hasKanji(c)) {
                    if (currentSubToken == null) {
                        currentSubToken = JapaneseSubToken.builder()
                                .kanji(c)
                                .source(token.getSurface())
                                .build();
                    } else if (currentSubToken.kanji != null) {
                        currentSubToken.kanji += c;
                    } else {
                        subTokens.add(currentSubToken);
                        currentSubToken = JapaneseSubToken.builder()
                                .kanji(c)
                                .source(token.getSurface())
                                .build();
                    }
                } else {
                    String hiragana = mojiHelper.katakanaToHiragana(c);
                    if (currentSubToken == null) {
                        currentSubToken = JapaneseSubToken.builder()
                                .kana(hiragana)
                                .source(token.getSurface())
                                .build();
                    } else if (currentSubToken.kana != null) {
                        currentSubToken.kana += hiragana;
                    } else {
                        subTokens.add(currentSubToken);
                        currentSubToken = JapaneseSubToken.builder()
                                .kana(hiragana)
                                .source(token.getSurface())
                                .build();
                    }
                }
            }
            subTokens.add(currentSubToken);
        }

        // 逐步截取分配到各个furigana上；
        String unusedPronunciation = mojiHelper.katakanaToHiragana(rawPronunciation);
        JapaneseSubToken handlingKanjiNode = null;
        for (int i = subTokens.size() - 1; i >= 0; i--) {
            JapaneseSubToken node = subTokens.get(i);
            if (node.kana != null) {
                int kanaIndex = unusedPronunciation.lastIndexOf(node.kana);
                if (handlingKanjiNode != null) {
                    if (kanaIndex < 0) {
                        throw new RuntimeException(node.kana + " not in " + unusedPronunciation + ", result.nodes = " + subTokens + ", rawPronunciation = " + rawPronunciation);
                    }
                    handlingKanjiNode.furigana = unusedPronunciation.substring(
                            kanaIndex + node.kana.length()
                    );
                    handlingKanjiNode = null;
                }
                unusedPronunciation = unusedPronunciation.substring(0, kanaIndex);
            } else {
                handlingKanjiNode = node;
            }
        }
        if (handlingKanjiNode != null) {
            handlingKanjiNode.furigana = unusedPronunciation;
        }
        return subTokens;
    }


    @Override
    protected List<JapaneseLine> toParsedLines(List<String> lines, @Nullable RootHint rootHint) {
        return lines.stream().map(it -> toParsedLinesCore(it)).collect(Collectors.toList());
    }



    public static JapaneseLine toParsedLinesCore(String line) {
        List<Token> tokens = tokenizer.tokenize(line);
        List<JapaneseParsedToken> parsedTokens = tokens.stream()
                .map(token -> {
                    boolean hasKanji = mojiHelper.hasKanji(token.getSurface());
                    List<JapaneseSubToken> subTokens;
                    if (hasKanji) {
                        subTokens = parseKanjiToken(token);
                    } else {
                        subTokens = List.of(
                                JapaneseSubToken.builder()
                                        .kana(token.getSurface())
                                        .build()
                        );
                    }
                    return JapaneseParsedToken.builder()
                            .des(token.getPartOfSpeechLevel1())
                            .subTokens(subTokens)
                            .build();
                })
                .collect(Collectors.toList());
        return JapaneseLine.builder()
                .parsedTokens(parsedTokens)
                .build();

    }



    @Override
    protected Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<JapaneseLine> lines) {
        Map<String, KanjiPronunciationPackage> map = new HashMap<>();
        lines.forEach(line -> {
            line.getParsedTokens().stream()
                    .forEach(parsedToken -> {
                        parsedToken.subTokens.forEach(subToken -> {
                            if (subToken.kanji != null) {
                                if (!map.containsKey(subToken.kanji)) {
                                    map.put(subToken.kanji, KanjiPronunciationPackage.builder()
                                            .kanji(subToken.kanji)
                                            .pronunciationMap(new HashMap<>())
                                            .build());
                                }
                                mergeNeedHintMap(map.get(subToken.kanji), subToken);
                            }
                        });
                    });
        });
        return map;
    }

    private void mergeNeedHintMap(KanjiPronunciationPackage thiz, JapaneseSubToken node) {
        if (!thiz.getPronunciationMap().containsKey(node.getFurigana())) {
            thiz.getPronunciationMap().put(node.getFurigana(), new ArrayList<>());
        }
        thiz.getPronunciationMap().get(node.getFurigana()).add(
                SourceInfo.fromUnknownTimestamp(node.getSource())
        );
    }


}
