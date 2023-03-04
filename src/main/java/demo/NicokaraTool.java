package demo;

import demo.IMojiHelper.SimpleMojiHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author hundun
 * Created on 2023/03/08
 */
public class NicokaraTool {
    

    
    static IMojiHelper mojiHelper = new SimpleMojiHelper();

    
    public static void main(String[] args) {
        List<String> list = readAllLines("data/input.txt");
        List<MyToken> myTokens = toMyTokenList(list);
        String ruby = MyToken.collectRuby(myTokens);
        System.out.println(ruby);
    }


    
    public static List<String> readAllLines(String path) {
        try {
            return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
    }
    
    @AllArgsConstructor
    @Builder
    @Data
    public static class MyTokenNode {
        String kanji;
        String kanjiPronunciationKana;
        String kana;
    }
    
    @AllArgsConstructor
    @Builder
    @Data
    public static class MyToken {
        List<MyTokenNode> nodes;
        boolean asNewLine;
        
        
        
        public static String collectRuby(List<MyToken> myTokens) {
            Map<String, String> kanjiPronunciationMap = new HashMap<>();
            myTokens.stream()
                    .filter(it -> it.nodes != null)
                    .forEach(it -> {
                        it.nodes.stream()
                                .forEach(node -> {
                                    if (node.kanji != null) {
                                        kanjiPronunciationMap.put(node.kanji, node.kanjiPronunciationKana);
                                    }
                                });
                    });
            List<String> lines = new ArrayList<>();
            kanjiPronunciationMap.forEach((k, v) -> {
                lines.add(String.format("@Ruby%d=%s,%s", 
                        lines.size(),
                        k,
                        v
                        ));
            });
            
            return lines.stream()
                    .collect(Collectors.joining("\n"))
                    ;
        }
        
        public static String toLyric(List<MyToken> myTokens) {
            return myTokens.stream()
                    .map(it -> {
                        if (it.asNewLine) {
                            return "\n";
                        } else {
                            return it.nodes.stream()
                                    .map(node -> {
                                        if (node.kanji != null) {
                                            return String.format("%s(%s)", 
                                                    node.kanji,
                                                    node.kanjiPronunciationKana
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
    
    private static MyToken toMyToken(Token token) {
        MyToken result = MyToken.builder()
                .nodes(new ArrayList<>())
                .build();
        String surface = token.getSurface();
        String rawPronunciation = token.getReading();
        String pronunciation = mojiHelper.katakanaToHiragana(rawPronunciation);

        {
            MyTokenNode currentNode = null;
            for (int i = 0; i < surface.length(); i++) {
                String c = String.valueOf(surface.charAt(i));
                if (mojiHelper.hasKanji(c)) {
                    if (currentNode == null) {
                        currentNode = MyTokenNode.builder()
                                .kanji(c)
                                .build();
                    } else if (currentNode.kanji != null) {
                        currentNode.kanji += c;
                    } else {
                        result.nodes.add(currentNode);
                        currentNode = MyTokenNode.builder()
                                .kanji(c)
                                .build();
                    }
                } else {
                    String hiragana = mojiHelper.katakanaToHiragana(c);
                    if (currentNode == null) {
                        currentNode = MyTokenNode.builder()
                                .kana(hiragana)
                                .build();
                    } else if (currentNode.kana != null) {
                        currentNode.kana += hiragana;
                    } else {
                        result.nodes.add(currentNode);
                        currentNode = MyTokenNode.builder()
                                .kana(hiragana)
                                .build();
                    }
                }
            }
            result.nodes.add(currentNode);
        }
        
        MyTokenNode handlingKanjiNode = null;
        for (int i = 0; i < result.nodes.size(); i++) {
            MyTokenNode node = result.nodes.get(i);
            if (node.kana != null) {
                int kanaIndex = pronunciation.indexOf(node.kana);
                if (handlingKanjiNode != null) {
                    if (kanaIndex < 0) {
                        throw new RuntimeException(node.kana + " not in " + pronunciation + ", result.nodes = " + result.nodes + ", rawPronunciation = " + rawPronunciation);
                    }
                    handlingKanjiNode.kanjiPronunciationKana = pronunciation.substring(
                            0,
                            kanaIndex
                            );
                    handlingKanjiNode = null;
                }
                pronunciation = pronunciation.substring(kanaIndex + node.kana.length());
            } else {
                handlingKanjiNode = node;
            }
        }
        if (handlingKanjiNode != null) {
            handlingKanjiNode.kanjiPronunciationKana = pronunciation.substring(
                    0,
                    pronunciation.length()
                    );
        }

        return result;
    }
    
    public static List<MyToken> toMyTokenList(String text, String splict) {
        String[] lines = text.split(splict);
        return toMyTokenList(List.of(lines));
    }
    
    public static List<MyToken> toMyTokenList(List<String> list) {
        List<MyToken> result = new ArrayList<>();
        Tokenizer tokenizer = new Tokenizer() ;
        for (String line : list) {
            List<Token> tokens = tokenizer.tokenize(line);
            for (Token token : tokens) {
                //boolean hasKanji = !KuromojiTool.isAllKana(token.getSurface()) && !token.getReading().equals("*") && !token.getReading().equals(token.getSurface());
                boolean hasKanji = mojiHelper.hasKanji(token.getSurface());
                if (hasKanji) {
                    result.add(toMyToken(token));
                } else {
                    result.add(MyToken.builder()
                            .nodes(List.of(
                                    MyTokenNode.builder()
                                            .kana(token.getSurface())
                                            .build()
                                    ))
                            .build());
                }
            }
            result.add(MyToken.builder()
                    .asNewLine(true)
                    .build());
        }
        return result;
    }
}
