package hundun.nicokaratool.japanese;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseParsedToken;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseSubToken;
import hundun.nicokaratool.remote.MojiDictFeignClient;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictRequest;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictResponse;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictResponse.SearchResultItem;
import hundun.nicokaratool.layout.Cell;
import hundun.nicokaratool.layout.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainService {


    MojiDictFeignClient mojiDictFeignClient = MojiDictFeignClient.instance();

    public Map<Integer, SearchResultItem> getMojiHintMap(JapaneseLine line) {
        Map<Integer, SearchResultItem> parsedTokensIndexToMojiHintMap = new HashMap<>();
        for (int i = 0; i < line.getParsedTokens().size(); i++) {
            var it = line.getParsedTokens().get(i);
            if (it.typeKanji()) {
                MojiDictResponse response = mojiDictFeignClient.union_api(MojiDictRequest.quickBuild(it.getSurface()));
                parsedTokensIndexToMojiHintMap.put(i, MojiDictResponse.findFirstSearchResultItem(response));
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return parsedTokensIndexToMojiHintMap;
    }


    @Data
    @AllArgsConstructor
    @Builder
    public static class JapaneseExtraHint {
        Map<Integer, SearchResultItem> parsedTokensIndexToMojiHintMap;
    }




}
