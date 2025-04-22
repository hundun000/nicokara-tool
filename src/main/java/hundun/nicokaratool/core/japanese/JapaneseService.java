package hundun.nicokaratool.core.japanese;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.core.MainRunner;
import hundun.nicokaratool.core.base.KanjiHintPO;
import hundun.nicokaratool.core.base.KanjiHintPO.PronounceHint;
import hundun.nicokaratool.core.base.KanjiPronunciationPackage;
import hundun.nicokaratool.core.base.KanjiPronunciationPackage.SourceInfo;
import hundun.nicokaratool.core.base.RootHint;
import hundun.nicokaratool.core.japanese.IMojiHelper.SimpleMojiHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import hundun.nicokaratool.core.japanese.TagTokenizer.TagToken;
import hundun.nicokaratool.core.japanese.TagTokenizer.TagTokenType;
import hundun.nicokaratool.core.japanese.TagTokenizer.Timestamp;
import hundun.nicokaratool.core.layout.ImageRender;
import hundun.nicokaratool.core.layout.VideoRender;
import hundun.nicokaratool.core.layout.VideoRender.KeyFrame;
import hundun.nicokaratool.core.layout.text.ILyricsRender;
import hundun.nicokaratool.core.layout.text.NicokaraLyricsRender;
import hundun.nicokaratool.core.layout.table.Table;
import hundun.nicokaratool.core.layout.table.TableBuilder;
import hundun.nicokaratool.core.remote.GoogleServiceImpl;
import hundun.nicokaratool.core.util.Utils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * @author hundun
 * Created on 2023/03/08
 */
@Slf4j
public class JapaneseService {
    protected ObjectMapper normalObjectMapper = new ObjectMapper();
    protected ObjectMapper fileObjectMapper = new ObjectMapper();
    public static ObjectMapper objectMapper = new ObjectMapper();
    ILyricsRender<JapaneseLine> lyricsRender;

    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    IMojiHelper mojiHelper = new SimpleMojiHelper();
    final Tokenizer tokenizer = new Tokenizer();

    public MojiService mojiService = new MojiService();
    TagTokenizer tagTokenizer = new TagTokenizer();
    GoogleServiceImpl googleService = new GoogleServiceImpl();

    public JapaneseService() {
        this.lyricsRender = new NicokaraLyricsRender();
        fileObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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
        boolean outputLogFile;
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
                    .outputLogFile(true)
                    .build();
        }
    }

    @Getter
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
        Timestamp start;
        boolean specifiedStart;
        Timestamp end;
        boolean specifiedEnd;
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

        results.forEach(line -> {
            for (int i = 0; i  < line.getParsedTokens().size(); i ++) {
                var parsedToken = line.getParsedTokens().get(i);
                for (int j = 0; j < parsedToken.subTokens.size(); j++) {
                    var subToken = parsedToken.subTokens.get(j);
                    var findSubTokenTimestampStart = findSubTokenTimestamp(line, parsedToken, subToken, i, j, true, true);
                    subToken.start = findSubTokenTimestampStart.getLeft();
                    subToken.specifiedStart = findSubTokenTimestampStart.getRight();
                    var findSubTokenTimestampEnd = findSubTokenTimestamp(line, parsedToken, subToken, i, j, false, true);
                    subToken.end = findSubTokenTimestampEnd.getLeft();
                    subToken.specifiedEnd = findSubTokenTimestampEnd.getRight();
                }
            }
        });
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



    protected Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<JapaneseLine> lines) {
        Map<String, KanjiPronunciationPackage> map = new HashMap<>();
        lines.forEach(line -> {
            for (int i = 0; i  < line.getParsedTokens().size(); i ++) {
                var parsedToken = line.getParsedTokens().get(i);
                for (int j = 0; j < parsedToken.subTokens.size(); j++) {
                    var subToken = parsedToken.subTokens.get(j);
                    if (subToken.typeKanji()) {
                        if (!map.containsKey(subToken.kanji)) {
                            map.put(subToken.kanji, KanjiPronunciationPackage.builder()
                                    .kanji(subToken.kanji)
                                    .pronunciationMap(new HashMap<>())
                                    .build());
                        }
                        mergeNeedHintMap(map.get(subToken.kanji), line, parsedToken, subToken, i, j);
                    }
                }
            }
        });
        return map;
    }

    private void mergeNeedHintMap(
            KanjiPronunciationPackage thiz,
            JapaneseLine line,
            JapaneseParsedToken parsedToken,
            JapaneseSubToken subToken,
            int parsedTokenIndex,
            int subTokenIndex
    ) {
        String pronunciation = subToken.getFurigana();
        if (!thiz.getPronunciationMap().containsKey(pronunciation)) {
            thiz.getPronunciationMap().put(pronunciation, new ArrayList<>());
        }
        thiz.getPronunciationMap().get(pronunciation).add(
                SourceInfo.fromSubToken(subToken)
        );
    }


    /**
     * 起点为line中第parsedTokenIndex（不含）个JapaneseParsedToken的（第subTokenIndex（不含）个subToken/不限）向（左/右）寻找第一个遇到的Timestamp
     */
    private Pair<Timestamp, Boolean> findSubTokenTimestamp(
            JapaneseLine line,
            JapaneseParsedToken parsedToken,
            JapaneseSubToken subToken,
            int parsedTokenIndex,
            Integer subTokenIndex,
            boolean findLeft,
            boolean firstTimeRecursion
    ) {
        if (parsedTokenIndex < 0 || parsedTokenIndex >= line.getParsedTokens().size()) {
            throw new RuntimeException("bad find: " + subToken);
        }

        if (findLeft) {
            Integer beforePartLength = Optional.ofNullable(subTokenIndex)
                    .map(subTokenIndexIt -> {
                        String beforePart = parsedToken.getSubTokens().stream()
                                .limit(subTokenIndexIt)
                                .map(it -> it.getSurface())
                                .collect(Collectors.joining());
                        return beforePart.length();
                    })
                    .orElse(null);
            // 排序后取最后一个
            var entryOptional = parsedToken.getDetailedTimeMap().entrySet().stream()
                    .filter(it -> beforePartLength == null || it.getKey() <= beforePartLength)
                    .sorted((o1, o2) -> - Integer.compare(o1.getKey(), o2.getKey()))
                    .findFirst();
            if (entryOptional.isPresent()) {
                var value = entryOptional.get().getValue();
                boolean perfectMatch = firstTimeRecursion && Objects.equals(entryOptional.get().getKey(), beforePartLength);
                return Pair.of(value.get(value.size() - 1).getTimestamp(), perfectMatch);
            } else {
                return findSubTokenTimestamp(line, line.getParsedTokens().get(parsedTokenIndex - 1), subToken, parsedTokenIndex - 1, null, findLeft, false);
            }
        } else {
            Integer beforePartLength = Optional.ofNullable(subTokenIndex)
                    .map(subTokenIndexIt -> {
                        String beforePart = parsedToken.getSubTokens().stream()
                                .limit(subTokenIndexIt + 1)
                                .map(it -> it.getSurface())
                                .collect(Collectors.joining());
                        return beforePart.length();
                    })
                    .orElse(null);
            // 排序后取最前一个
            var entryOptional = parsedToken.getDetailedTimeMap().entrySet().stream()
                    .filter(it -> beforePartLength == null || it.getKey() >= beforePartLength)
                    .sorted((o1, o2) -> Integer.compare(o1.getKey(), o2.getKey()))
                    .findFirst();
            if (entryOptional.isPresent()) {
                var value = entryOptional.get().getValue();
                boolean perfectMatch = firstTimeRecursion && Objects.equals(entryOptional.get().getKey(), beforePartLength);
                return Pair.of(value.get(0).getTimestamp(), perfectMatch);
            } else {
                return findSubTokenTimestamp(line, line.getParsedTokens().get(parsedTokenIndex + 1), subToken, parsedTokenIndex + 1, null, findLeft, false);
            }
        }
    }

    public void workJapaneseLearningVideo(String name) {
        File tempFolderFile = new File(MainRunner.RUNTIME_IO_FOLDER + name + "_temp" + File.separator);

    }

    @AllArgsConstructor
    @Builder
    @Data
    public static class ServiceContext {
        // ---- step 0 ----
        String title;
        File rootHintFile;
        List<String> plainLines;
        RootHint rootHint;
        boolean needNewRootHint;
        // ---- step 1 ----
        List<JapaneseLine> parsedLines;
        Map<String, KanjiPronunciationPackage> packageMap;
        String lyricsText;
        String ruby;
    }

    private ServiceContext serviceContext(String name) throws IOException {

        boolean saveRootHint = false;

        List<String> lines = Utils.readAllLines(MainRunner.RUNTIME_IO_FOLDER + name + ".txt");
        RootHint rootHint;
        File rootHintFile = new File(MainRunner.RUNTIME_IO_FOLDER + name + ".rootHint.json");
        if (rootHintFile.exists()) {
            rootHint = fileObjectMapper.readValue(rootHintFile, RootHint.class);
        } else {
            saveRootHint = true;
            rootHint = null;
        }

        return ServiceContext.builder()
                .title(name)
                .needNewRootHint(saveRootHint)
                .plainLines(lines)
                .rootHint(rootHint)
                .rootHintFile(rootHintFile)
                .build();
    }


    public ServiceContext quickStep1(String name) throws IOException {
        ServiceContext serviceContext = serviceContext(name);
        workStep1Core(serviceContext);
        return serviceContext;
    }

    public void workStep1Core(ServiceContext serviceContext) throws IOException {

        List<JapaneseLine> parsedLines = toParsedLines(serviceContext.plainLines, serviceContext.rootHint);
        serviceContext.parsedLines = parsedLines;
        serviceContext.packageMap = calculateKanjiPronunciationPackageMap(parsedLines);

        if (serviceContext.needNewRootHint) {
            List<KanjiHintPO> kanjiHintPOS = serviceContext.packageMap.values().stream()
                    .filter(it -> it.getPronunciationMap().size() > 1)
                    .map(it -> toKanjiHint(it))
                    .collect(Collectors.toList());
            serviceContext.rootHint = RootHint.builder()
                    .kanjiHints(kanjiHintPOS)
                    .nluDisallowHints(new ArrayList<>(0))
                    .build();
            fileObjectMapper.writeValue(serviceContext.rootHintFile, serviceContext.rootHint);
        }

        parsedLines.forEach(it -> {
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

        Map<String, KanjiHintPO> kanjiHintsMap = serviceContext.rootHint.getKanjiHints().stream()
                .collect(Collectors.toMap(
                        it -> it.getKanji(),
                        it -> it));
        List<String> rubyList = new ArrayList<>();
        serviceContext.packageMap.forEach((kanji, kanjiInfo) -> {
            if (kanjiHintsMap.containsKey(kanji)) {
                KanjiHintPO po = kanjiHintsMap.get(kanji);
                appendToRubyLines(po, rubyList);
            } else {
                appendToRubyLines(kanjiInfo, rubyList);
            }
        });
        serviceContext.ruby = String.join("\n", rubyList);

        serviceContext.lyricsText = serviceContext.parsedLines.stream()
                .map(it -> lyricsRender.toLyricsLine(it))
                .collect(Collectors.joining("\n"));
    }

    public void workStep2(ServiceContext serviceContext) {
        if (argPackage.outputLogFile) {
            try {
                String logText = fileObjectMapper.writeValueAsString(serviceContext.getParsedLines());
                Utils.writeAllLines(MainRunner.RUNTIME_IO_FOLDER + serviceContext.getTitle() + ".log.json", logText);
            } catch (JsonProcessingException e) {
                log.error("bad outputLogFile", e);
            }
        }

        if (argPackage.outputImage) {
            int space = 5;
            ImageRender.multiDraw(
                    MainRunner.RUNTIME_IO_FOLDER + serviceContext.getTitle() + "_all_output.png",
                    serviceContext.getParsedLines().stream()
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
            String kraFileText = serviceContext.getLyricsText() + "\n" + serviceContext.getRuby();
            Utils.writeAllLines(MainRunner.RUNTIME_IO_FOLDER + serviceContext.getTitle() + ".kra", kraFileText);
        }
        if (argPackage.outputVideo) {
            int space = 10;
            int kanjiFontSize = 100;
            int kanaFontSize = 50;
            String prepareFolder = MainRunner.RUNTIME_IO_FOLDER + serviceContext.getTitle() + "_videoTemp/";
            List<Path> paths = ImageRender.multiDrawToFolder(
                    prepareFolder,
                    "{i}.png",
                    serviceContext.getParsedLines().stream()
                            .map(line -> {
                                JapaneseExtraHint japaneseExtraHint = JapaneseExtraHint.builder()
                                        .parsedTokensIndexToMojiHintMap(mojiService.getMojiHintMap(line))
                                        .build();
                                TableBuilder tableBuilder = TableBuilder.fromJapaneseLine(line, japaneseExtraHint, kanjiFontSize, kanaFontSize);
                                tableBuilder.setXPreferredSpace(space);
                                tableBuilder.setYPreferredSpace(space);
                                tableBuilder.setSingleContentMaxWidth(kanjiFontSize * 10);
                                Table table = tableBuilder.build(ImageRender.face);
                                return table;
                            })
                            .collect(Collectors.toList()),
                    space,
                    argPackage.debugImage
            );
            log.info("outputVideo paths done.");
            List<KeyFrame> frames = new ArrayList<>();
            for (int i = 0; i < serviceContext.getParsedLines().size(); i++) {
                var path = paths.get(i);
                var line = serviceContext.getParsedLines().get(i);
                var res = KeyFrame.builder()
                        .imagePath(path.toString())
                        .inpoint(line.getStartTime().getTimestamp())
                        .outpoint(line.getEndTime().getTimestamp())
                        .build();
                frames.add(res);
            }
            for (int i = 0; i < frames.size(); i++) {
                var frame = frames.get(i);
                if (i - 1 >= 0) {
                    var last = frames.get(i - 1);
                    frame.setDurationLast(frame.getInpoint().totalMs() - last.getOutpoint().totalMs());
                }
                if (i + 1 < frames.size()) {
                    var next = frames.get(i + 1);
                    frame.setDurationNext(next.getInpoint().totalMs() - frame.getOutpoint().totalMs());
                }
            }
            VideoRender.prepare(prepareFolder, frames);
            log.info("outputVideo prepare done.");
            VideoRender.concat(
                    prepareFolder,
                    frames
            );
            log.info("outputVideo concat done.");
            String outFile = MainRunner.RUNTIME_IO_FOLDER + serviceContext.getTitle() + ".mp4";
            String audioPath = MainRunner.RUNTIME_IO_FOLDER + serviceContext.getTitle() + ".mp3";
            VideoRender.addAudio(prepareFolder, outFile, audioPath);
            log.info("outputVideo addAudio done.");
        }
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

        bo.getPronunciationMap().forEach((pronunciation, sources) -> {
            sources.forEach(source -> {
/*                    String line = String.format("@Ruby%d=%s,%s",
                            lines.size() + 1,
                            bo.getKanji(),
                            pronunciation
                    );
                    if (bo.getPronunciationMap().size() > 1) {
                        line += String.format(",%s,%s",
                                source.getStart() != null ? source.getStart().toLyricsTime() : Timestamp.unknownLyricsTime(),
                                source.getEnd() != null ? source.getEnd().toLyricsTime() : Timestamp.unknownLyricsTime()
                        );
                        if (source.isFromUnknownTimestamp()) {
                            line += " // from " + source.getSourceLyricLineText();
                        }
                    }*/
                String line = String.format("@Ruby%d=%s,%s",
                        lines.size() + 1,
                        bo.getKanji(),
                        pronunciation
                );
                line += String.format(",%s,%s",
                        source.getStart() != null ? source.getStart().toLyricsTime() : Timestamp.unknownLyricsTime(),
                        source.getEnd() != null ? source.getEnd().toLyricsTime() : Timestamp.unknownLyricsTime()
                );
                lines.add(line);
            });
        });
    }

    protected void appendToRubyLines(KanjiHintPO po, List<String> lines) {
        po.getPronounceHints().forEach(pronounceHint -> {
            pronounceHint.getRubyLines().forEach(rubyLine -> {
                lines.add(String.format("@Ruby%d=%s,%s",
                        lines.size() + 1,
                        po.getKanji(),
                        rubyLine
                ));
            });
        });
    }

}
