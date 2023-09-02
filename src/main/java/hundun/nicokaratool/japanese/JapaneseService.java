package hundun.nicokaratool.japanese;

import hundun.nicokaratool.base.BaseService;
import hundun.nicokaratool.base.KanjiPronunciationPackage;
import hundun.nicokaratool.base.KanjiPronunciationPackage.SourceInfo;
import hundun.nicokaratool.base.KanjiHintPO;
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

/**
 * @author hundun
 * Created on 2023/03/08
 */
public class JapaneseService extends BaseService<JapaneseLine> {
    

    static IMojiHelper mojiHelper = new SimpleMojiHelper();

    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseSubToken {
        String kanji;
        String kanjiPronunciation;
        String kana;
        String source;
    }
     
    @AllArgsConstructor
    @Builder
    @Data
    public static class JapaneseLine {
        List<JapaneseSubToken> nodes;
        boolean asNewLine;
        
        public static String toLyric(List<JapaneseLine> lines) {
            return lines.stream()
                    .map(it -> {
                        if (it.asNewLine) {
                            return "\n";
                        } else {
                            return it.nodes.stream()
                                    .map(node -> {
                                        if (node.kanji != null) {
                                            return String.format("%s(%s)", 
                                                    node.kanji,
                                                    node.kanjiPronunciation
                                                    );
                                        } else {
                                            return node.kana;
                                        }
                                    })
                                    .collect(Collectors.joining())
                                    ;
                        }
                    })
                    .collect(Collectors.joining())
                    ;
        }
    }
    
//    private static String katakanaToHiragana(String text) {
//        //text = text.replace("ッ", "っ");
//        String romaji = converter.convertKanaToRomaji(text);
//        String hiragana = converter.convertRomajiToHiragana(romaji);
//        return hiragana;
//    }
    
    private static JapaneseLine _toMyToken(Token token) {
        JapaneseLine result = JapaneseLine.builder()
                .nodes(new ArrayList<>())
                .build();
        String surface = token.getSurface();
        String rawPronunciation = token.getReading();
        String pronunciation = mojiHelper.katakanaToHiragana(rawPronunciation);

        {
            JapaneseSubToken currentNode = null;
            for (int i = 0; i < surface.length(); i++) {
                String c = String.valueOf(surface.charAt(i));
                if (mojiHelper.hasKanji(c)) {
                    if (currentNode == null) {
                        currentNode = JapaneseSubToken.builder()
                                .kanji(c)
                                .source(token.getSurface())
                                .build();
                    } else if (currentNode.kanji != null) {
                        currentNode.kanji += c;
                    } else {
                        result.nodes.add(currentNode);
                        currentNode = JapaneseSubToken.builder()
                                .kanji(c)
                                .source(token.getSurface())
                                .build();
                    }
                } else {
                    String hiragana = mojiHelper.katakanaToHiragana(c);
                    if (currentNode == null) {
                        currentNode = JapaneseSubToken.builder()
                                .kana(hiragana)
                                .source(token.getSurface())
                                .build();
                    } else if (currentNode.kana != null) {
                        currentNode.kana += hiragana;
                    } else {
                        result.nodes.add(currentNode);
                        currentNode = JapaneseSubToken.builder()
                                .kana(hiragana)
                                .source(token.getSurface())
                                .build();
                    }
                }
            }
            result.nodes.add(currentNode);
        }
        
        JapaneseSubToken handlingKanjiNode = null;
        for (int i = result.nodes.size() - 1; i >= 0; i--) {
            JapaneseSubToken node = result.nodes.get(i);
            if (node.kana != null) {
                int kanaIndex = pronunciation.lastIndexOf(node.kana);
                if (handlingKanjiNode != null) {
                    if (kanaIndex < 0) {
                        throw new RuntimeException(node.kana + " not in " + pronunciation + ", result.nodes = " + result.nodes + ", rawPronunciation = " + rawPronunciation);
                    }
                    handlingKanjiNode.kanjiPronunciation = pronunciation.substring(
                            kanaIndex + node.kana.length(),
                            pronunciation.length()
                            );
                    handlingKanjiNode = null;
                }
                pronunciation = pronunciation.substring(0, kanaIndex);
            } else {
                handlingKanjiNode = node;
            }
        }
        if (handlingKanjiNode != null) {
            handlingKanjiNode.kanjiPronunciation = pronunciation.substring(
                    0,
                    pronunciation.length()
                    );
        }

        return result;
    }
    
    public static List<JapaneseLine> toMyTokenList(String text, String splict) {
        String[] lines = text.split(splict);
        return toMyTokenListCore(List.of(lines));
    }

    @Override
    protected List<JapaneseLine> toMyTokenList(List<String> list) {
        return toMyTokenListCore(list);
    }

    private static List<JapaneseLine> toMyTokenListCore(List<String> list) {
        List<JapaneseLine> result = new ArrayList<>();
        Tokenizer tokenizer = new Tokenizer() ;
        for (String line : list) {
            List<Token> tokens = tokenizer.tokenize(line);
            for (Token token : tokens) {
                //boolean hasKanji = !KuromojiTool.isAllKana(token.getSurface()) && !token.getReading().equals("*") && !token.getReading().equals(token.getSurface());
                boolean hasKanji = mojiHelper.hasKanji(token.getSurface());
                if (hasKanji) {
                    result.add(_toMyToken(token));
                } else {
                    result.add(JapaneseLine.builder()
                            .nodes(List.of(
                                    JapaneseSubToken.builder()
                                            .kana(token.getSurface())
                                            .build()
                                    ))
                            .build());
                }
            }
            result.add(JapaneseLine.builder()
                    .asNewLine(true)
                    .build());
        }
        return result;
    }



    @Override
    protected Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<JapaneseLine> lines) {
        Map<String, KanjiPronunciationPackage> map = new HashMap<>();
        lines.forEach(line -> {
            if (line.getNodes() != null) {
                line.getNodes().stream()
                        .forEach(node -> {
                            if (node.kanji != null) {
                                if (!map.containsKey(node.kanji)) {
                                    map.put(node.kanji, KanjiPronunciationPackage.builder()
                                            .kanji(node.kanji)
                                            .pronunciationMap(new HashMap<>())
                                            .build());
                                }
                                mergeNeedHintMap(map.get(node.kanji), node);
                            }
                        });
            }
        });
        return map;
    }

    private void mergeNeedHintMap(KanjiPronunciationPackage thiz, JapaneseSubToken node) {
        if (!thiz.getPronunciationMap().containsKey(node.getKanjiPronunciation())) {
            thiz.getPronunciationMap().put(node.getKanjiPronunciation(), new ArrayList<>());
        }
        thiz.getPronunciationMap().get(node.getKanjiPronunciation()).add(
                SourceInfo.fromUnknownTimestamp(node.getSource())
        );
    }


}
