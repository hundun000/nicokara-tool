package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import hundun.nicokaratool.japanese.TagTokenizer.TagToken;
import hundun.nicokaratool.japanese.TagTokenizer.TagTokenType;
import hundun.nicokaratool.remote.GoogleServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * @author hundun
 * Created on 2023/03/08
 */
@Slf4j
public class JapaneseService extends BaseService<JapaneseLine> {

    public static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    static IMojiHelper mojiHelper = new SimpleMojiHelper();
    static final Tokenizer tokenizer = new Tokenizer();

    TagTokenizer tagTokenizer = new TagTokenizer();
    GoogleServiceImpl googleService = new GoogleServiceImpl();

    protected JapaneseService() {
        super(NicokaraLyricsRender.INSTANCE);
    }

    /**
     * 编程实现所需的更细分的token，例如"思","い"和"出"分别对应一个实例; "大切"对应一个实例;
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
        String surface;
        /**
         * 既其所属的上一层的surface
         */
        String source;
        
        public boolean typeKanji() {
            return kanji != null;
        }

        public static JapaneseSubToken createTypeKanji(String kanji, String source) {
            return JapaneseSubToken.builder()
                    .kanji(kanji)
                    .surface(kanji)
                    .source(source)
                    .build();
        }

        public static JapaneseSubToken createTypeKana(String kana, String surface, String source) {
            return JapaneseSubToken.builder()
                    .kana(kana)
                    .surface(surface)
                    .source(source)
                    .build();
        }

    }
    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseLine {
        List<TagToken> tagTokens;
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
                        if (it.typeKanji()) {
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
            StringBuilder result = new StringBuilder();
            var tagTokenIterator = japaneseLine.getTagTokens().iterator();
            var subTokenIterator = japaneseLine.getParsedTokens().stream()
                    .flatMap(it -> it.getSubTokens().stream())
                    .flatMap(it -> it.getSurface().chars().mapToObj(itt -> (char)itt))
                    .iterator();
            while (tagTokenIterator.hasNext()) {
                TagToken tagToken = tagTokenIterator.next();
                if (tagToken.getType() == TagTokenType.TEXT) {
                    StringBuilder collectingText = new StringBuilder();
                    while (!collectingText.toString().equals(tagToken.text)) {
                        if (subTokenIterator.hasNext()) {
                            collectingText.append(subTokenIterator.next());
                        } else {
                            throw new RuntimeException("collectingText = " + collectingText + ", target = " + tagToken.text + ", not found.");
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


    /**
     * 日语语法上的一个分词，例如"思い出"对应一个实例； "大切"对应一个实例;
     */
    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseParsedToken {
        int index;
        String surface;
        String partOfSpeechLevel1;
        List<JapaneseSubToken> subTokens;

        public boolean typeKanji() {
            return subTokens.stream().anyMatch(it -> it.typeKanji());
        }
    }
    
    private static List<JapaneseSubToken> parseSubTokens(Token token) {
        List<JapaneseSubToken> subTokens = new ArrayList<>();
        String surface = token.getSurface();
        String rawPronunciation = token.isKnown() ? token.getReading() : token.getSurface();

        {
            JapaneseSubToken currentSubToken = null;
            for (int i = 0; i < surface.length(); i++) {
                String surfaceChar = String.valueOf(surface.charAt(i));
                if (mojiHelper.hasKanji(surfaceChar)) {
                    if (currentSubToken == null) {
                        currentSubToken = JapaneseSubToken.createTypeKanji(surfaceChar, token.getSurface());
                    } else if (currentSubToken.typeKanji()) {
                        currentSubToken.kanji += surfaceChar;
                        currentSubToken.surface += surfaceChar;
                    } else {
                        subTokens.add(currentSubToken);
                        currentSubToken = JapaneseSubToken.createTypeKanji(surfaceChar, token.getSurface());
                    }
                } else {
                    String hiragana = mojiHelper.katakanaToHiragana(surfaceChar);
                    if (currentSubToken == null) {
                        currentSubToken = JapaneseSubToken.createTypeKana(hiragana, surfaceChar, token.getSurface());
                    } else if (currentSubToken.kana != null) {
                        currentSubToken.kana += hiragana;
                        currentSubToken.surface += surfaceChar;
                    } else {
                        subTokens.add(currentSubToken);
                        currentSubToken = JapaneseSubToken.createTypeKana(hiragana, surfaceChar, token.getSurface());
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
        return lines.stream()
                .filter(it -> !it.isEmpty())
                .map(it -> {
                    List<TagToken> tagTokens = tagTokenizer.parse(it);
                    String noTagText = tagTokens.stream()
                            .filter(itt -> itt.getType() == TagTokenType.TEXT)
                            .map(itt -> itt.getText())
                            .collect(Collectors.joining());
                    var result = toParsedNoTagLine(noTagText);
                    result.setTagTokens(tagTokens);
                    if (rootHint != null && rootHint.getTranslationCacheMap() != null) {
                        result.setChinese(rootHint.getTranslationCacheMap().get(noTagText));
                    }
                    if (result.getChinese() == null) {
                        try {
                            String googleServiceResult = googleService.translateJaToZh(noTagText);
                            result.setChinese(googleServiceResult);
                            log.info("googleService.translateJaToZh: {} -> {}", noTagText, googleServiceResult);
                        } catch (Exception e) {
                            log.error("bad googleService.translateJaToZh: ", e);
                            result.setChinese("Error: " + e.getMessage());
                        }
                    }

                    return result;
                })
                .collect(Collectors.toList());
    }



    public static JapaneseLine toParsedNoTagLine(String line) {
        List<Token> tokens = tokenizer.tokenize(line);
        List<JapaneseParsedToken> parsedTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            var token = tokens.get(i);
            List<JapaneseSubToken> subTokens = parseSubTokens(token);
            var result = JapaneseParsedToken.builder()
                    .index(i)
                    .surface(token.getSurface())
                    .partOfSpeechLevel1(token.getPartOfSpeechLevel1())
                    .subTokens(subTokens)
                    .build();
            parsedTokens.add(result);
        }
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
                            if (subToken.typeKanji()) {
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
