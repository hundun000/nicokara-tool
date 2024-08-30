package hundun.nicokaratool.gpt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Body;
import feign.HeaderMap;
import feign.RequestLine;
import hundun.nicokaratool.japanese.MojiDictFeignClient;
import hundun.nicokaratool.util.FeignClientFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public interface IGptChatFeignClient {

    public static IGptChatFeignClient instance() {
        return FeignClientFactory.get(IGptChatFeignClient.class, "https://api.openai-proxy.com");
    }

    @RequestLine("POST /v1/chat/completions")
    GptAskResponse ask(
            @HeaderMap Map<String, String> headers,
            GptAskRequest request
    );

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GptAskResponse {
        ObjectNode usage;
        List<GptChoice> choices;
        boolean fallBackCatchBadRequest;
        boolean fallBackCatchTimeout;
        boolean fallBackCatchOtherException;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GptAskRequest {
        String model;
        List<GptChatHistory> messages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GptChatHistory {
        /**
         * "system" / "user" / "assistant"
         */
        String role;
        String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GptChoice {
        GptChatHistory message;
        String finish_reason;
    }
}
