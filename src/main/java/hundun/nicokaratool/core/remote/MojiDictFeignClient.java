package hundun.nicokaratool.core.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Headers;
import feign.RequestLine;
import hundun.nicokaratool.core.remote.MojiDictFeignClient.MojiDictRequest.Function.Params;
import hundun.nicokaratool.core.util.FeignClientFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public interface MojiDictFeignClient {

    public static MojiDictFeignClient instance() {
        return FeignClientFactory.get(MojiDictFeignClient.class, "https://api.mojidict.com");
    }

    @RequestLine("POST /parse/functions/union-api")
    @Headers("Content-Type: application/json")
    MojiDictResponse union_api(MojiDictRequest request);


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MojiDictRequest {
        String text;
        List<Function> functions;
        String _ClientVersion;
        String _ApplicationId;
        String g_os;
        String g_ver;
        String _InstallationId;
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Function {
            String name;
            Params params;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            public static class Params {
                String text;
                List<Integer> types;
            }
        }

        public static MojiDictRequest quickBuild(String text) {
            return MojiDictRequest.builder()
                    .functions(List.of(
                            Function.builder()
                                    .name("search-all")
                                    .params(Params.builder()
                                            .text(text)
                                            .types(List.of(102, 106, 103))
                                            .build()
                                    )
                                    .build()
                    ))
                    ._ClientVersion("js3.4.1")
                    ._ApplicationId("E62VyFVLMiW7kvbtVq3p")
                    .g_os("PCWeb")
                    .g_ver("v4.8.7.20240716")
                    ._InstallationId("8fb5c3e2-f433-4678-a3f4-1606c471abf7")
                    .build();
        }
    }

    @Data
    public static class MojiDictResponse {
        Result1 result;

        @Data
        public static class Result1 {
            int code;
            Result2 results;


        }

        @Data
        public static class Result2 {
            @JsonProperty("search-all")
            SearchAll searchAll;
        }

        @Data
        public static class SearchAll {
            int code;
            Result3 result;
        }

        @Data
        public static class Result3 {
            Word word;
        }

        @Data
        public static class Word {
            List<SearchResultItem> searchResult;
        }

        /**
         * {
         * "targetId" : "198970381",
         * "targetType" : 102,
         * "title" : "食べる | たべる ②",
         * "excerpt" : "[他动·一段] 吃；生活",
         * "excerptB" : "[他动·二类] 吃；生活"
         * }
         */
        @Data
        public static class SearchResultItem {
            String targetId;
            int targetType;
            String title;
            String excerpt;
            String excerptB;
        }
    }
}
