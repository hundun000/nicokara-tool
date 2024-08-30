package hundun.nicokaratool.gpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.*;
import hundun.nicokaratool.japanese.MojiDictFeignClient;
import hundun.nicokaratool.util.FeignClientFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface IGptChatFeignClient {

    public static IGptChatFeignClient instance(@Nullable JsonNode proxy) {
        Client client;
        if (proxy != null) {
            client = new Client.Proxied((SSLSocketFactory)null, (HostnameVerifier)null, new Proxy(Type.HTTP, new InetSocketAddress(
                    proxy.get("host").asText(),
                    proxy.get("port").asInt()
            )));
        } else {
            client = new Client.Default((SSLSocketFactory)null, (HostnameVerifier)null);
        }
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
