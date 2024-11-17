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

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
            case "助動詞":
            case "名詞":
            case "連体詞":
                return true;
            default:
                return false;
        }
    }

    private TranslationResultItem postHandleToTranslationResult(JapaneseParsedToken token, SearchResultItem searchResultItem) {
        int excerptCutLength = 30;
        return Optional.ofNullable(searchResultItem)
                .map(itt -> TranslationResultItem.builder()
                        .jaText(token.getSurface())
                        .zhText(
                                Optional.of(itt.getTitle())
                                        .map(it -> {
                                            boolean match = Arrays.asList(itt.getTitle().split("\\|")).stream()
                                                    .map(part -> part.trim())
                                                    .anyMatch(part -> part.equals(token.getSurface()));
                                            if (match) {
                                                return "";
                                            } else {
                                                return itt.getTitle() + "：";
                                            }
                                        })
                                        .get()
                                +
                                Optional.of(itt.getExcerpt())
                                        .map(it -> {
                                            if (it.length() > excerptCutLength) {
                                                return it.substring(0, excerptCutLength) + "...";
                                            } else {
                                                return it;
                                            }
                                        })
                                        .get()
                        )
                        .build()
                )
                .orElse(null);
    }

    public Map<Integer, TranslationResultItem> getMojiHintMap(JapaneseLine line) {
        Map<Integer, TranslationResultItem> parsedTokensIndexToMojiHintMap = new HashMap<>();
        boolean cacheDirty = false;
        for (int i = 0; i < line.getParsedTokens().size(); i++) {
            var it = line.getParsedTokens().get(i);
            if (needTranslate(it)) {
                MojiDictResponse response;
                if (!cache.mojiDictResponseCacheMap.containsKey(it.getSurface())) {
                    log.info("moji api for: {}", it.getSurface());
                    response = mojiDictFeignClient.union_api(MojiDictRequest.quickBuild(it.getSurface()));
                } else {
                    response = cache.mojiDictResponseCacheMap.get(it.getSurface());
                }
                SearchResultItem searchResultItem = MojiDictResponse.findFirstSearchResultItem(response);
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


}
