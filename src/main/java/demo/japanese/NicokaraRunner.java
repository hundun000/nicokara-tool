package demo.japanese;

import demo.japanese.IMojiHelper.SimpleMojiHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import demo.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author hundun
 * Created on 2023/03/08
 */
public class NicokaraRunner {
    

    static IMojiHelper mojiHelper = new SimpleMojiHelper();


    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
        System.out.println("Enter name: ");
        String name = br.readLine();
        File kanjiHintsFile = new File("data/" + name + ".kanjiHints.json");
        List<String> list = Utils.readAllLines("data/" + name + ".txt");
        List<JapaneseToken> MyTokens = toMyTokenList(list);
        var rubyCollector = new JapaneseRubyCollector();
        String ruby = rubyCollector.collectRuby(MyTokens, kanjiHintsFile);
        System.out.println("Ruby: ");
        System.out.println(ruby);
    }


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
    public static class JapaneseToken {
        List<JapaneseSubToken> nodes;
        boolean asNewLine;
        
        public static String toLyric(List<JapaneseToken> MyTokens) {
            return MyTokens.stream()
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
    
    private static JapaneseToken toMyToken(Token token) {
        JapaneseToken result = JapaneseToken.builder()
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
    
    public static List<JapaneseToken> toMyTokenList(String text, String splict) {
        String[] lines = text.split(splict);
        return toMyTokenList(List.of(lines));
    }
    
    public static List<JapaneseToken> toMyTokenList(List<String> list) {
        List<JapaneseToken> result = new ArrayList<>();
        Tokenizer tokenizer = new Tokenizer() ;
        for (String line : list) {
            List<Token> tokens = tokenizer.tokenize(line);
            for (Token token : tokens) {
                //boolean hasKanji = !KuromojiTool.isAllKana(token.getSurface()) && !token.getReading().equals("*") && !token.getReading().equals(token.getSurface());
                boolean hasKanji = mojiHelper.hasKanji(token.getSurface());
                if (hasKanji) {
                    result.add(toMyToken(token));
                } else {
                    result.add(JapaneseToken.builder()
                            .nodes(List.of(
                                    JapaneseSubToken.builder()
                                            .kana(token.getSurface())
                                            .build()
                                    ))
                            .build());
                }
            }
            result.add(JapaneseToken.builder()
                    .asNewLine(true)
                    .build());
        }
        return result;
    }
}
