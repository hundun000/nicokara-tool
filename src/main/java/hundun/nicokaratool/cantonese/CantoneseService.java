package hundun.nicokaratool.cantonese;

import hundun.nicokaratool.base.BaseService;
import hundun.nicokaratool.base.KanjiPronunciationPackage;
import hundun.nicokaratool.base.KanjiPronunciationPackage.SourceInfo;
import hundun.nicokaratool.base.LyricLine;
import hundun.nicokaratool.base.LyricLine.LyricTimestamp;
import hundun.nicokaratool.base.LyricLine.LyricToken;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleRequest;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    ApiCache apiCache;
    File apiCacheFile;
    static final String API_CACHE_FOLDER = "cache/";
    static final String API_CACHE_FILE = API_CACHE_FOLDER + "cantoneseServiceApiCache.json";
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ApiCache {
        Map<String, YaleResponse> map;
    }

    @Override
    protected List<LyricLine> toMyTokenList(List<String> list) {
        var result = list.stream()
                .map(it -> {
                    try {
                        return parseSimpleTimestampLine(it);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        for (int i= 0; i < result.size(); i++) {
            var it = result.get(i);
            var lastNode = it.getNodes().get(it.getNodes().size() - 1);
            if (lastNode.getEnd() == null && i + 1 < result.size()) {
                var nextLine = result.get(i + 1);
                var nextLineFirstNode = nextLine.getNodes().get(0);
                lastNode.setEnd(nextLineFirstNode.getStart());
            }
        }
        return result;
    }

    @Override
    protected Map<String, KanjiPronunciationPackage> calculateKanjiPronunciationPackageMap(List<LyricLine> lines) {
        Map<String, KanjiPronunciationPackage> map = new HashMap<>();
        lines.forEach(line -> {
            if (line.getNodes() != null) {
                line.getNodes().stream()
                        .forEach(node -> {
                            if (node.getKanji() != null) {
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
            response = client.jyutping_to_yale(request);
            apiCache.getMap().put(cacheKey, response);
            fileObjectMapper.writeValue(apiCacheFile, apiCache);
        } else {
            response = apiCache.getMap().get(cacheKey);
        }
        return response;
    }

    public LyricLine parseSimpleTimestampLine(String text) throws IOException {
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
        YaleRequest request = YaleRequest.builder()
                .text(text)
                .build();
        YaleResponse response = cachedQuery(request);
        var nodes = response.yale.stream()
                .map(it -> LyricToken.builder()
                        .kanji(it.get(0))
                        .yalePronunciation(it.get(1))
                        .build())
                .collect(Collectors.toList());
        nodes.get(0).setStart(lineStart);
        nodes.get(nodes.size() - 1).setEnd(lineEnd);
        return LyricLine.builder()
                .nodes(nodes)
                .build();
    }
}
