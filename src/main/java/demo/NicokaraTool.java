package demo;

import demo.IMojiHelper.SimpleMojiHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author hundun
 * Created on 2023/03/08
 */
public class NicokaraTool {
    
    static ObjectMapper objectMapper;
    
    static IMojiHelper mojiHelper = new SimpleMojiHelper();
    static {
        objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                ;
    }
    
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
        System.out.println("Enter name: ");
        String name = br.readLine();
        File kanjiHintsFile = new File("data/" + name + ".kanjiHints.json");
        List<String> list = readAllLines("data/" + name + ".txt");
        List<MyToken> MyTokens = toMyTokenList(list);
        String ruby = RubyCollector.collectRuby(MyTokens, kanjiHintsFile);
        System.out.println("Ruby: ");
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
    
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class KanjiInfo {
        String kanji;
        Map<String, List<String>> pronunciationMap;
        
        
        public void merge(MySubToken node) {
            if (!pronunciationMap.containsKey(node.kanjiPronunciation)) {
                pronunciationMap.put(node.kanjiPronunciation, new ArrayList<>());
            }
            pronunciationMap.get(node.kanjiPronunciation).add(node.source);
        }
        
        public void appendAsRuby(List<String> lines) {
            if (pronunciationMap.size() == 1) {
                lines.add(String.format("@Ruby%d=%s,%s", 
                        lines.size() + 1,
                        kanji,
                        pronunciationMap.entrySet().iterator().next().getKey()
                        ));
            } else {
                pronunciationMap.forEach((pronunciation, sources) -> {
                    sources.forEach(source -> {
                        lines.add(String.format("@Ruby%d=%s,%s,[00:00:00],[99:99:99] // from %s", 
                                lines.size() + 1,
                                kanji,
                                pronunciation,
                                source
                                ));
                    });
                });
            }
        }
        
        public KanjiHint toKanjiHint() {
            var hintMap = pronunciationMap.entrySet().stream()
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
                    .kanji(kanji)
                    .hintMap(hintMap)
                    .build();
        }
    }
    
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class KanjiHint {
        String kanji;
        Map<String, List<String>> hintMap;
        
        
        public void appendAsRuby(List<String> lines) {
            hintMap.forEach((pronunciation, hints) -> {
                hints.forEach(hint -> {
                    lines.add(String.format("@Ruby%d=%s,%s", 
                            lines.size() + 1,
                            kanji,
                            hint
                            ));
                });
            });
        }
    }
    
    public static class RubyCollector {
        
        public static void newKanjiHintsFile(File kanjiHintsFile, Map<String, KanjiInfo> kanjiInfoMap) {
            
            List<KanjiHint> list = kanjiInfoMap.values().stream()
                    .filter(it -> it.getPronunciationMap().size() > 1)
                    .map(it -> it.toKanjiHint())
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
                return (List<KanjiHint>)objectMapper.readValue(kanjiHintsFile, objectMapper.getTypeFactory().constructCollectionType(List.class, KanjiHint.class));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        public static String collectRuby(List<MyToken> MyTokens, File kanjiHintsFile) {
            Map<String, KanjiInfo> kanjiInfoMap = new HashMap<>();
            MyTokens.stream()
                    .filter(it -> it.nodes != null)
                    .forEach(it -> {
                        it.nodes.stream()
                                .forEach(node -> {
                                    if (node.kanji != null) {
                                        if (!kanjiInfoMap.containsKey(node.kanji)) {
                                            kanjiInfoMap.put(node.kanji, KanjiInfo.builder()
                                                    .kanji(node.kanji)
                                                    .pronunciationMap(new HashMap<>())
                                                    .build());
                                        }
                                        kanjiInfoMap.get(node.kanji).merge(node);
                                    }
                                });
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
                    kanjiInfo.appendAsRuby(lines);
                }
            });
            
            return lines.stream()
                    .collect(Collectors.joining("\n"))
                    ;
        }
    }
    
    @AllArgsConstructor
    @Builder
    @Data
    public static class MySubToken {
        String kanji;
        String kanjiPronunciation;
        String kana;
        String source;
    }
     
    @AllArgsConstructor
    @Builder
    @Data
    public static class MyToken {
        List<MySubToken> nodes;
        boolean asNewLine;
        
        public static String toLyric(List<MyToken> MyTokens) {
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
    
    private static MyToken toMyToken(Token token) {
        MyToken result = MyToken.builder()
                .nodes(new ArrayList<>())
                .build();
        String surface = token.getSurface();
        String rawPronunciation = token.getReading();
        String pronunciation = mojiHelper.katakanaToHiragana(rawPronunciation);

        {
            MySubToken currentNode = null;
            for (int i = 0; i < surface.length(); i++) {
                String c = String.valueOf(surface.charAt(i));
                if (mojiHelper.hasKanji(c)) {
                    if (currentNode == null) {
                        currentNode = MySubToken.builder()
                                .kanji(c)
                                .source(token.getSurface())
                                .build();
                    } else if (currentNode.kanji != null) {
                        currentNode.kanji += c;
                    } else {
                        result.nodes.add(currentNode);
                        currentNode = MySubToken.builder()
                                .kanji(c)
                                .source(token.getSurface())
                                .build();
                    }
                } else {
                    String hiragana = mojiHelper.katakanaToHiragana(c);
                    if (currentNode == null) {
                        currentNode = MySubToken.builder()
                                .kana(hiragana)
                                .source(token.getSurface())
                                .build();
                    } else if (currentNode.kana != null) {
                        currentNode.kana += hiragana;
                    } else {
                        result.nodes.add(currentNode);
                        currentNode = MySubToken.builder()
                                .kana(hiragana)
                                .source(token.getSurface())
                                .build();
                    }
                }
            }
            result.nodes.add(currentNode);
        }
        
        MySubToken handlingKanjiNode = null;
        for (int i = result.nodes.size() - 1; i >= 0; i--) {
            MySubToken node = result.nodes.get(i);
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
                                    MySubToken.builder()
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
