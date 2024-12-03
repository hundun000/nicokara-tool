package hundun.nicokaratool.remote;

import hundun.nicokaratool.base.SecretConfig;
import hundun.nicokaratool.remote.IGptChatFeignClient.GptAskRequest;
import hundun.nicokaratool.remote.IGptChatFeignClient.GptAskResponse;
import hundun.nicokaratool.remote.IGptChatFeignClient.GptChatHistory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GptServiceImpl {


    IGptChatFeignClient gptChatFeignClient;

    public GptServiceImpl() {
        gptChatFeignClient = IGptChatFeignClient.instance(SecretConfig.proxyConfig);
    }

    public enum LlmModel {
        BAIDU("USELESS"),
        BAIDU_PRO("USELESS"),
        GPT_3_5("gpt-3.5-turbo"),
        GPT_4("gpt-4"),
        GPT_4_TURBO("gpt-4-turbo-preview"),
        ;
        @Getter
        final String apiName;

        LlmModel(String apiName) {
            this.apiName = apiName;
        }
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LlmComplexAskRequest {
        LlmModel model;
        @Nullable
        String prompt;
        List<LlmChatHistory> chatHistoryList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LlmChatHistory {
        boolean assistance;
        /**
         * 某些llm支持多个userSide角色
         */
        @Nullable
        String extraRoleName;
        String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LlmAskResponse {
        /**
         * 可告知外层不应用同样的request重试
         */
        boolean badRequest;
        @Nullable
        String reply;
    }

    public String run_single_and_save(LlmModel model, String system_prompt, String user_prompt) {
        var gptResult = this.generalComplexAsk(LlmComplexAskRequest.builder()
                .model(model)
                .prompt(system_prompt)
                .chatHistoryList(List.of(
                        LlmChatHistory.builder()
                                .assistance(false)
                                .text(user_prompt)
                                .build()
                ))
                .build());
        return gptResult.getReply();
    }

    public @NotNull LlmAskResponse generalComplexAsk(LlmComplexAskRequest request) {
        var messages = request.getChatHistoryList().stream()
                .map(it -> GptChatHistory.builder()
                        .role(it.isAssistance() ? "assistant" : "user")
                        .content(it.getText())
                        .build()
                )
                .collect(Collectors.toList());
        messages.add(0, GptChatHistory.builder()
                .role("system")
                .content(request.getPrompt())
                .build());
        GptAskRequest gptAskRequest = GptAskRequest.builder()
                .model(request.getModel().getApiName())
                .messages(messages)
                .build();
        GptAskResponse gptAskResponse = gptAsk(gptAskRequest);
        String reply = gptAskResponse.getChoices() == null ? null : gptAskResponse.getChoices().stream()
                .findFirst()
                .map(it -> it.getMessage().getContent())
                .orElse(null);
        return LlmAskResponse.builder()
                .reply(reply)
                .badRequest(gptAskResponse.isFallBackCatchBadRequest())
                .build();
    }

    public GptAskResponse gptAsk(GptAskRequest request) {
        var headers = Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json",
                "Authorization", "Bearer " + SecretConfig.gptAuthorization
        );
        GptAskResponse response = gptChatFeignClient.ask(headers, request);
        log.info("response: {}", response);
        if (
                response.isFallBackCatchBadRequest()
                        || response.isFallBackCatchTimeout()
                        || response.isFallBackCatchOtherException()
        ) {
            log.warn("Fail of request: {}", request);
        }
        return response;
    }

}
