package hundun.nicokaratool.core.remote;

import feign.*;
import hundun.nicokaratool.core.util.FeignClientFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface IGoogleAiFeignClient {

    public static IGoogleAiFeignClient instance() {
        return FeignClientFactory
                .getBaseBuilder()
                .options(new Request.Options(1, TimeUnit.SECONDS, 300, TimeUnit.SECONDS, true))
                .target(IGoogleAiFeignClient.class, "https://generativelanguage.googleapis.com");
    }


    @RequestLine("POST " + "/v1beta/models/{model}:generateContent?key={api_key}")
    @Headers("Content-Type: application/json")
    GenerateContentResponse generateContent(
            @Param("model") String model, // 模型名称，例如 "gemini-1.0-pro"
            @Param("api_key") String apiKey,
            GenerateContentRequest requestBody);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public class GenerateContentRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Content {
            private String role;
            private List<Part> parts;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            public static class Part {
                private String text;
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class GenerationConfig {
            private Float temperature;
            private Integer topP;
            private Integer topK;
            private Integer maxOutputTokens;
        }
    }

    // 定义响应对象
    @Data
    public class GenerateContentResponse {
        private Candidate[] candidates;
        private PromptFeedback promptFeedback;

        @Data
        public static class Candidate {
            private Content content;
            private String finishReason;
            private SafetyRating[] safetyRatings;
            private CitationMetadata citationMetadata;
            private Integer index;
        }

        @Data
        public static class Content {
            private Part[] parts;
            private String role;
        }

        @Data
        public static class Part {
            private String text;
        }

        @Data
        public static class SafetyRating {
            private String category;
            private String probability;
            private boolean blocked;
        }

        @Data
        public static class CitationMetadata {
            private Citation[] citations;
        }

        @Data
        public static class Citation {
            private Integer startIndex;
            private Integer endIndex;
            private String uri;
            private String title;
        }

        @Data
        public static class PromptFeedback {
            private String safetyRatings;
            private BlockReason blockReason;
        }
        public enum BlockReason {
            UNKNOWN,
            UNSPECIFIED,
            SAFETY,
            OTHER
        }
    }
}
