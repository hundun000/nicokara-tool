package hundun.nicokaratool.cantonese;

import hundun.nicokaratool.base.BaseService;
import hundun.nicokaratool.base.KanjiPronunciationPackage;
import hundun.nicokaratool.base.KanjiPronunciationPackage.SourceInfo;
import hundun.nicokaratool.base.lyrics.LyricLine;
import hundun.nicokaratool.base.lyrics.LyricLine.LyricTimestamp;
import hundun.nicokaratool.base.lyrics.LyricLine.LyricToken;
import hundun.nicokaratool.base.RootHint;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleRequest;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleResponse;
import hundun.nicokaratool.layout.StandardLyricsRender;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CantoneseService extends BaseService<LyricLine> {
    PycantoneseFeignClient client = PycantoneseFeignClient.instance();
    @Setter
    CantonesePronounceType type = CantonesePronounceType.JYUT_PING;
    ApiCache apiCache;
    File apiCacheFile;
    static final String API_CACHE_FOLDER = "cache/";
    static final String API_CACHE_FILE = API_CACHE_FOLDER + "cantoneseServiceApiCache.json";

    protected CantoneseService() {
        super(StandardLyricsRender.INSTANCE);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ApiCache {
        Map<String, YaleResponse> map;
    }

    public enum CantonesePronounceType {
        JYUT_PING,
        YALE,
    }

    @Override
    protected List<LyricLine> toParsedLines(List<String> list, @Nullable RootHint rootHint) {
        var result = list.stream()
                .map(it -> {
                    try {
                        return parseSimpleTimestampLine(it, rootHint);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
/*        for (int i= 0; i < result.size(); i++) {
            var it = result.get(i);
            var lastNode = it.getNodes().get(it.getNodes().size() - 1);
            if (lastNode.getEnd() == null && i + 1 < result.size()) {
                var nextLine = result.get(i + 1);
                var nextLineFirstNode = nextLine.getNodes().get(0);
                lastNode.setEnd(nextLineFirstNode.getStart());
            }
        }*/
        return result;
    }

    @Override
    protected Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<LyricLine> lines) {
        Map<String, KanjiPronunciationPackage> map = new HashMap<>();
        lines.forEach(line -> {
            if (line.getNodes() != null) {
                line.getNodes().stream()
                        .forEach(node -> {
                            if (node.getKanji() != null && !node.getKanji().equals(" ")) {
                                if (!map.containsKey(node.getKanji())) {
                                    map.put(node.getKanji(), KanjiPronunciationPackage.builder()
                                            .kanji(node.getKanji())
                                            .pronunciationMap(new HashMap<>())
                                            .build());
                                }
                                mergeNeedHintMap(map.get(node.getKanji()), node);
                            }
                        });
            }
        });
        return map;
    }

    private void mergeNeedHintMap(KanjiPronunciationPackage thiz, LyricToken node) {
        if (!thiz.getPronunciationMap().containsKey(node.getYalePronunciation())) {
            thiz.getPronunciationMap().put(node.getYalePronunciation(), new ArrayList<>());
        }
        thiz.getPronunciationMap().get(node.getYalePronunciation()).add(
                SourceInfo.fromLyricToken(node)
        );
    }

    private YaleResponse cachedQuery(YaleRequest request) throws IOException {
        if (apiCacheFile == null) {
            Files.createDirectories(Paths.get(API_CACHE_FOLDER));
            apiCacheFile = new File(API_CACHE_FILE);
        }
        if (apiCache == null) {
            if (apiCacheFile.exists()) {
                apiCache = fileObjectMapper.readValue(apiCacheFile, ApiCache.class);
            } else {
                apiCache = ApiCache.builder()
                        .map(new HashMap<>())
                        .build();
            }
        }
        String cacheKey = normalObjectMapper.writeValueAsString(request);
        YaleResponse response;
        if (!apiCache.getMap().containsKey(cacheKey)) {
            if (type == CantonesePronounceType.YALE) {
                response = client.characters_to_yale(request);
            } else {
                response = client.characters_to_jyutping(request);
            }
            apiCache.getMap().put(cacheKey, response);
            fileObjectMapper.writeValue(apiCacheFile, apiCache);
        } else {
            response = apiCache.getMap().get(cacheKey);
        }
        return response;
    }

    public LyricLine parseSimpleTimestampLine(String text, @Nullable RootHint rootHint) throws IOException {
        LyricTimestamp lineStart = null;
        LyricTimestamp lineEnd = null;
        if (text.startsWith("[")) {
            lineStart = (LyricTimestamp.parseType1(text.substring(0, LyricTimestamp.TYPE_1_LENGTH)));
            text = text.substring(LyricTimestamp.TYPE_1_LENGTH);
        }
        if (text.endsWith("]")) {
            lineEnd = (LyricTimestamp.parseType1(text.substring(text.length() - LyricTimestamp.TYPE_1_LENGTH)));
            text = text.substring(0, text.length() - LyricTimestamp.TYPE_1_LENGTH);
        }
        String[] parts = text.split(" ");
        List<LyricToken> lyricTokens = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            YaleRequest request = YaleRequest.builder()
                    .text(part)
                    .build();
            if (rootHint != null) {
                request.setDisallow(rootHint.getNluDisallowHints());
            }
            YaleResponse response = cachedQuery(request);
            var nodes = response.getResult().stream()
                    .map(it -> LyricToken.builder()
                            .kanji(it.get(0))
                            .yalePronunciation(it.get(1))
                            .build())
                    .collect(Collectors.toList());
            lyricTokens.addAll(nodes);

            if (i + 1 < parts.length) {
                lyricTokens.add(LyricToken.space());
            }
        }

        lyricTokens.get(0).setStart(lineStart);
        lyricTokens.get(lyricTokens.size() - 1).setEnd(lineEnd);
        return LyricLine.builder()
                .nodes(lyricTokens)
                .build();
    }
}
