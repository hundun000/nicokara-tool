package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.BaseService;
import hundun.nicokaratool.base.KanjiPronunciationPackage;
import hundun.nicokaratool.base.KanjiPronunciationPackage.SourceInfo;
import hundun.nicokaratool.base.RootHint;
import hundun.nicokaratool.japanese.IMojiHelper.SimpleMojiHelper;

import java.util.*;
import java.util.stream.Collectors;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.TagTokenizer.TagToken;
import hundun.nicokaratool.japanese.TagTokenizer.TagTokenType;
import hundun.nicokaratool.layout.ImageRender;
import hundun.nicokaratool.layout.NicokaraLyricsRender;
import hundun.nicokaratool.layout.table.Table;
import hundun.nicokaratool.layout.table.TableBuilder;
import hundun.nicokaratool.remote.GoogleServiceImpl;
import hundun.nicokaratool.util.Utils;
import lombok.*;
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
    IMojiHelper mojiHelper = new SimpleMojiHelper();
    final Tokenizer tokenizer = new Tokenizer();

    public MojiService mojiService = new MojiService();
    TagTokenizer tagTokenizer = new TagTokenizer();
    GoogleServiceImpl googleService = new GoogleServiceImpl();

    protected JapaneseService() {
        super(NicokaraLyricsRender.INSTANCE);
        mojiService.loadCache();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class WorkArgPackage {
        boolean withTranslation;
        boolean outputImage;
        boolean debugImage;
        boolean outputNicokara;
        boolean outputVideo;

        public static WorkArgPackage getDefault() {
            return WorkArgPackage.builder()
                    .withTranslation(false)
                    .outputNicokara(false)
                    .build();
        }

        public static WorkArgPackage getAllFeatures() {
            return WorkArgPackage.builder()
                    .withTranslation(true)
                    .outputNicokara(true)
                    .outputImage(true)
                    .debugImage(true)
                    .outputVideo(true)
                    .build();
        }
    }

    WorkArgPackage argPackage = WorkArgPackage.getDefault();

    /**
     * 编程实现所需的更细分的token，例如"思","い"和"出"分别对应一个实例，因为他们分别属于汉字和假名; "大切"对应一个实例，因为他是连续的汉字;
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
        String surface;
        TagToken startTime;
        TagToken endTime;
    }

    public enum SubtitleTimeSourceType {
        SPECIFIED,
        SPECIFIED_COPY,
        GUESSED
        ;
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
        /**
         * 在第i个字符后，额外插入一对时间点TagToken，分别对应此处的左侧和右侧时间
         */
        Map<Integer, List<TagToken>> detailedTimeMap;

        public boolean typeKanji() {
            return subTokens.stream().anyMatch(it -> it.typeKanji());
        }
    }

    /**
     * Token("思い出") -> [JapaneseSubToken("思"), JapaneseSubToken("い"), JapaneseSubToken("出")]
     */
    private List<JapaneseSubToken> parseSubTokens(Token token) {
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

    private JapaneseLine handleOneLineStep0(String it, @Nullable RootHint rootHint) {
        List<TagToken> tagTokens = tagTokenizer.parse(it);
        String noTagText = tagTokens.stream()
                .filter(itt -> itt.getType() == TagTokenType.TEXT)
                .map(itt -> itt.getText())
                .collect(Collectors.joining());
        JapaneseLine result = toParsedNoTagLine(noTagText);
        result.setTagTokens(tagTokens);

        if (argPackage.isWithTranslation()) {
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
        } else {
            result.setChinese("");
        }

        return result;
    }

    private void handleOneLineStep12(JapaneseLine result) {

        // step 1.1 以JapaneseParsedToken的text为区间基准，将横跨多个区间的TagToken切割。
        StringBuilder currentJapaneseText = new StringBuilder();
        StringBuilder currentTagText = new StringBuilder();
        var handlingJapaneseIterator = result.getParsedTokens().iterator();
        List<TagToken> unusedTagTokens = new ArrayList<>(result.getTagTokens());
        List<TagToken> tempTagTokens = new ArrayList<>();
        while (currentJapaneseText.length() < result.getSurface().length()) {
            JapaneseParsedToken handlingJapanese = handlingJapaneseIterator.next();
            currentJapaneseText.append(handlingJapanese.getSurface());
            while (currentTagText.length() < currentJapaneseText.length()) {
                TagToken handlingTag = unusedTagTokens.remove(0);
                if (handlingTag.type == TagTokenType.TEXT) {
                    currentTagText.append(handlingTag.text);
                }
                if (currentTagText.length() <= currentJapaneseText.length()) {
                    tempTagTokens.add(handlingTag);
                } else {
                    /*
                    例：
                    handlingTag = (出を)
                    currentTagText = "思い出を"
                    currentJapaneseText = "思い出"
                    -->
                    delta = 1;
                    newLeft = (出)
                    newRight = (を)
                     */
                    int delta = currentTagText.length() - currentJapaneseText.length();
                    int same = handlingTag.text.length() - delta;
                    TagToken newLeft = TagToken.create(handlingTag.text.substring(0, same));
                    TagToken newRight = TagToken.create(handlingTag.text.substring(same));
                    tempTagTokens.add(newLeft);
                    currentTagText.setLength(currentTagText.length() - newRight.getText().length());
                    unusedTagTokens.add(0, newRight);
                }
            }
        }
        if (!unusedTagTokens.isEmpty()) {
            tempTagTokens.add(unusedTagTokens.get(0));
        }

        // step 1.2 若两个文本间只有一个TagToken，则分裂为两个，分别作为左右两侧的首尾。
        var newTagTokensCopy= new ArrayList<>(tempTagTokens);
        tempTagTokens.clear();
        for (int i = 1; i < newTagTokensCopy.size() - 1; i++) {
            var it = newTagTokensCopy.get(i);
            var last = newTagTokensCopy.get(i - 1);
            var next = newTagTokensCopy.get(i + 1);
            if (i == 1) {
                tempTagTokens.add(last);
            }
            if (it.getType() == TagTokenType.TIME_TAG && last.getType() == TagTokenType.TEXT && next.getType() == TagTokenType.TEXT) {
                tempTagTokens.add(it);
                tempTagTokens.add(TagToken.create(it.getText(), SubtitleTimeSourceType.SPECIFIED_COPY));
            } else {
                tempTagTokens.add(it);
            }
            if (i == newTagTokensCopy.size() - 2) {
                tempTagTokens.add(next);
            }
        }

        // step 2 每个JapaneseParsedToken找到属于自己的所有TagToken
        currentJapaneseText = new StringBuilder();
        currentTagText = new StringBuilder();
        handlingJapaneseIterator = result.getParsedTokens().iterator();
        unusedTagTokens = new ArrayList<>(tempTagTokens);
        while (currentJapaneseText.length() < result.getSurface().length()) {
            JapaneseParsedToken handlingJapanese = handlingJapaneseIterator.next();
            handlingJapanese.setDetailedTimeMap(new HashMap<>());
            currentJapaneseText.append(handlingJapanese.getSurface());
            // 小于时，一直拿
            while (currentTagText.length() < currentJapaneseText.length() && !unusedTagTokens.isEmpty()) {
                TagToken handlingTag = unusedTagTokens.get(0);
                if (handlingTag.type == TagTokenType.TEXT) {
                    currentTagText.append(handlingTag.text);
                } else if (handlingTag.type == TagTokenType.TIME_TAG) {
                    int delta = currentJapaneseText.length() - currentTagText.length();
                    int pos = handlingJapanese.getSurface().length() - delta;
                    if (!handlingJapanese.getDetailedTimeMap().containsKey(pos)) {
                        handlingJapanese.getDetailedTimeMap().put(pos, new ArrayList<>());
                    }
                    handlingJapanese.getDetailedTimeMap().get(pos).add(handlingTag);
                }
                unusedTagTokens.remove(0);
            }
            // 等于时，尝试再拿一次
            if (currentTagText.length() == currentJapaneseText.length() && !unusedTagTokens.isEmpty()) {
                TagToken handlingTag = unusedTagTokens.get(0);
                if (handlingTag.type == TagTokenType.TIME_TAG) {
                    int pos = handlingJapanese.getSurface().length();
                    if (!handlingJapanese.getDetailedTimeMap().containsKey(pos)) {
                        handlingJapanese.getDetailedTimeMap().put(pos, new ArrayList<>());
                    }
                    handlingJapanese.getDetailedTimeMap().get(pos).add(handlingTag);
                    unusedTagTokens.remove(0);
                }
            }
        }
    }


    @Override
    protected List<JapaneseLine> toParsedLines(List<String> lines, @Nullable RootHint rootHint) {

        /*
        text = "[00:22:90]大切な[00:24:03]思い[00:24:94][00:24:95]出を[00:25:51]"

        ==>
        step0:
        TagTokens = [(00:22:90), (大切な), (00:24:03), (思い), (00:24:94), (00:24:95), (出を), (00:25:51)]
        JapaneseLine = (parsedTokens = [(大切), (な), (思い出), (を)])

        step1:
        tempTagTokens = [(00:22:90), (大切), (な), (00:24:03), (00:24:03), (思い), (00:24:94), (00:24:95), (出), (を), (00:25:51)]

        step2:
        updated parsedTokens = (parsedTokens = [
                (大切, timeMap = {0 = [00:22:90]}),
                (な, timeMap = {1 = [00:24:03]}),
                (思い出, timeMap = {0 = [00:24:03], 2 = [00:24:031, 00:24:95]}),
                (を, timeMap = {1 = [00:24:031, 00:24:95]})
                ])
         ...

         */


        List<JapaneseLine> results = lines.stream()
                .filter(it -> !it.isEmpty())
                .map(it -> handleOneLineStep0(it, rootHint))
                .collect(Collectors.toList());

        results.forEach(it -> handleOneLineStep12(it));

        return results;
    }



    public JapaneseLine toParsedNoTagLine(String noTagText) {
        List<Token> tokens = tokenizer.tokenize(noTagText);
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
                .surface(noTagText)
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

    private void mergeNeedHintMap(KanjiPronunciationPackage thiz, JapaneseSubToken subToken) {
        String pronunciation = subToken.getFurigana();
        if (!thiz.getPronunciationMap().containsKey(pronunciation)) {
            thiz.getPronunciationMap().put(pronunciation, new ArrayList<>());
        }
        thiz.getPronunciationMap().get(pronunciation).add(
                SourceInfo.fromSubToken(subToken)
        );
    }

    public void workStep2(ServiceResult<JapaneseLine> serviceResult, String name) {
        serviceResult.getLines().forEach(it -> {
            if (!it.getTagTokens().isEmpty()) {
                it.setStartTime(
                        Optional.ofNullable(it.getTagTokens().get(0))
                                .filter(itt -> itt.getType() == TagTokenType.TIME_TAG)
                                .orElse(null)
                );
                it.setEndTime(
                        Optional.ofNullable(it.getTagTokens().get(it.getTagTokens().size() - 1))
                                .filter(itt -> itt.getType() == TagTokenType.TIME_TAG)
                                .orElse(null)
                );
            }
        });
        if (argPackage.outputImage) {
            int space = 5;
            ImageRender.multiDraw(
                    RUNTIME_IO_FOLDER + name + "_all_output.png",
                    serviceResult.getLines().stream()
                            .map(line -> {
                                JapaneseExtraHint japaneseExtraHint = JapaneseExtraHint.builder()
                                        .parsedTokensIndexToMojiHintMap(mojiService.getMojiHintMap(line))
                                        .build();
                                TableBuilder tableBuilder = TableBuilder.fromJapaneseLine(line, japaneseExtraHint);
                                tableBuilder.setXPreferredSpace(space);
                                tableBuilder.setYPreferredSpace(space);
                                Table table = tableBuilder.build(ImageRender.face);
                                return table;
                            })
                            .collect(Collectors.toList()),
                    space,
                    argPackage.debugImage
            );
        }
        if (argPackage.outputNicokara) {
            String kraFileText = serviceResult.getLyricsText() + "\n" + serviceResult.getRuby();
            Utils.writeAllLines(RUNTIME_IO_FOLDER + name + ".kra", kraFileText);
        }
        if (argPackage.outputVideo) {


        }
    }
}
