package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.nicokaratool.base.BaseService;
import hundun.nicokaratool.japanese.JapaneseExtraHint.TranslationResultItem;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.remote.MojiDictFeignClient;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictRequest;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictResponse;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictResponse.SearchResultItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MojiService {

    protected ObjectMapper fileObjectMapper = new ObjectMapper();

    protected MojiService() {
        fileObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    MojiDictFeignClient mojiDictFeignClient = MojiDictFeignClient.instance();

    MojiServiceCache cache = new MojiServiceCache();

    @Data
    public static class MojiServiceCache {
        Map<String, MojiDictResponse> mojiDictResponseCacheMap = new HashMap<>();
    }

    public void loadCache() {
        File file = new File(BaseService.CACHE_FOLDER + MojiServiceCache.class.getSimpleName() + ".json");
        if (file.exists()) {
            try {
                cache = fileObjectMapper.readValue(file, MojiServiceCache.class);
            } catch (IOException e) {
                log.error("bad readValue MojiServiceCache:", e);
            }
        }
    }

    Map<String, Set<String>> wordTypeMapping = Map.of(
            "動詞", Set.of("动", "自动", "他动"),
            "名詞", Set.of("名")
            );

    private boolean needTranslate(JapaneseParsedToken token) {
        if (token.getPartOfSpeechLevel1() == null) {
            return false;
        }
        if (token.getSurface().matches("[a-zA-Z]+")) {
            return false;
        }
        switch (token.getPartOfSpeechLevel1()) {
            case "副詞":
            case "動詞":
            case "感動詞":
            case "名詞":
            case "連体詞":
                return true;
            default:
                return false;
        }
    }
    static Pattern jaWordTypePattern = Pattern.compile("^(\\[.*?])");
    private TranslationResultItem postHandleToTranslationResult(JapaneseParsedToken search, SearchResultItem searchResultItem) {
        int excerptCutLength = 30;
        return Optional.ofNullable(searchResultItem)
                .map(itt -> {
                    Matcher matcher = jaWordTypePattern.matcher(itt.getExcerpt());
                    String jaWordTagsText = matcher.find() ? matcher.group(1) : "";
                    return TranslationResultItem.builder()
                            .jaSearch(search.getSurface())
                            .jaWordTags(
                                    Optional.of(itt.getExcerpt())
                                            .map(it -> {
                                                    if (!jaWordTagsText.isEmpty()) {
                                                        var parts = jaWordTagsText
                                                                .replace("[", "")
                                                                .replace("]", "")
                                                                .split("\\|");
                                                        return Arrays.asList(parts);
                                                    } else {
                                                        return List.of(it);
                                                    }
                                            })
                                            .get()
                            )
                            .jaOrigin(
                                    Optional.ofNullable(itt.getTitle())
                                            .map(it -> {
                                                it = it.replace("①", "").replace("②", "");
                                                var parts = it.split("\\|");
                                                boolean match = Arrays.asList(parts).stream()
                                                        .map(part -> part.trim())
                                                        .anyMatch(part -> part.equals(search.getSurface()));
                                                if (match) {
                                                    return null;
                                                } else {
                                                    return parts[0];
                                                }
                                            })
                                            .orElse("")
                            )
                            .zhDetail(
                                    Optional.of(itt.getExcerpt())
                                            .map(it -> jaWordTagsText.isEmpty() ? it : it.replace(jaWordTagsText, ""))
                                            .map(it -> {
                                                if (it.length() > excerptCutLength) {
                                                    return it.substring(0, excerptCutLength) + "...";
                                                } else {
                                                    return it;
                                                }
                                            })
                                            .get()
                            )
                            .build();
                })
                .orElse(null);
    }

    public Map<Integer, TranslationResultItem> getMojiHintMap(JapaneseLine line) {
        Map<Integer, TranslationResultItem> parsedTokensIndexToMojiHintMap = new HashMap<>();
        boolean cacheDirty = false;
        for (int i = 0; i < line.getParsedTokens().size(); i++) {
            var it = line.getParsedTokens().get(i);
            if (needTranslate(it)) {
                MojiDictResponse response;
                String search = it.getSurface();
                if (!cache.mojiDictResponseCacheMap.containsKey(search)) {
                    log.info("moji api for: {}", search);
                    response = mojiDictFeignClient.union_api(MojiDictRequest.quickBuild(search));
                } else {
                    response = cache.mojiDictResponseCacheMap.get(search);
                }
                SearchResultItem searchResultItem = findFirstSearchResultItem(it, response);
                TranslationResultItem translationResultItem = postHandleToTranslationResult(it, searchResultItem);
                cache.mojiDictResponseCacheMap.put(it.getSurface(), response);
                parsedTokensIndexToMojiHintMap.put(i, translationResultItem);
                cacheDirty = true;
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (cacheDirty) {
            File file = new File(BaseService.CACHE_FOLDER + MojiServiceCache.class.getSimpleName() + ".json");
            try {
                fileObjectMapper.writeValue(file, cache);
            } catch (IOException e) {
                log.error("bad writeValue MojiServiceCache:", e);
            }
        }
        return parsedTokensIndexToMojiHintMap;
    }

    @Nullable
    public SearchResultItem findFirstSearchResultItem(JapaneseParsedToken search, @Nullable MojiDictResponse response) {

        return Optional.ofNullable(response)
                .map(it -> it.getResult())
                .map(it -> it.getResults())
                .map(it -> it.getSearchAll())
                .map(it -> it.getResult())
                .map(it -> it.getWord())
                .map(it -> it.getSearchResult())
                .flatMap(it -> it.stream()
/*                        .filter(itt -> {
                            // 试图比较search的词性和搜索结果的词性是否匹配
                            if (!wordTypeMapping.containsKey(search.getPartOfSpeechLevel1())) {
                                return true;
                            } else {
                                Matcher matcher = pattern.matcher(itt.getExcerpt());
                                if (matcher.find()) {
                                    var parts = matcher.group(1).split("\\|");
                                    boolean match = Arrays.asList(parts).stream()
                                            .map(part -> part.trim())
                                            .anyMatch(part -> wordTypeMapping.get(search.getPartOfSpeechLevel1()).contains(part));
                                    // 当SearchResult有词性，但是和搜索目标不符合时，放弃之
                                    return match;
                                } else {
                                    return true;
                                }
                            }
                        })*/
                        .findFirst()
                )
                .orElse(null);
    }

}
