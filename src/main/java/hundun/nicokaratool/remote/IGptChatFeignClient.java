package hundun.nicokaratool.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.*;
import hundun.nicokaratool.util.FeignClientFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface IGptChatFeignClient {

    public static IGptChatFeignClient instance(@Nullable JsonNode proxy) {
        Client client = FeignClientFactory.getProxiedClient(proxy);
        var result = FeignClientFactory.getBaseBuilder()
                .client(client)
                .options(new Request.Options(1, TimeUnit.SECONDS, 2, TimeUnit.SECONDS, true))
                .target(IGptChatFeignClient.class, "https://api.openai.com");
        return result;
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
