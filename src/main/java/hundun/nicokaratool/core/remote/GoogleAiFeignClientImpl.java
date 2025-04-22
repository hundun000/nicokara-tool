package hundun.nicokaratool.core.remote;

import hundun.nicokaratool.core.base.SecretConfig;
import hundun.nicokaratool.core.remote.IGoogleAiFeignClient.GenerateContentRequest;
import hundun.nicokaratool.core.remote.IGoogleAiFeignClient.GenerateContentRequest.Content;
import hundun.nicokaratool.core.remote.IGoogleAiFeignClient.GenerateContentRequest.Content.Part;
import hundun.nicokaratool.core.remote.IGoogleAiFeignClient.GenerateContentRequest.GenerationConfig;
import hundun.nicokaratool.core.remote.IGoogleAiFeignClient.GenerateContentResponse;

import java.util.List;

public class GoogleAiFeignClientImpl {

    IGoogleAiFeignClient googleAiFeignClient;

    String key;
    //String model = "gemini-2.0-flash";
    String model = "gemini-2.5-flash-preview-04-17";
    float temperature = 0.2f;
    public GoogleAiFeignClientImpl() {
        googleAiFeignClient = IGoogleAiFeignClient.instance();
        this.key = SecretConfig.googleKey;
    }


    public GenerateContentResponse singleAsk(String ask) throws Exception {
        GenerateContentRequest requestModel = GenerateContentRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .parts(List.of(
                                        Part.builder()
                                                .text(ask)
                                                .build()
                                ))
                                .build()
                ))
                .generationConfig(GenerationConfig.builder()
                        .temperature(temperature)
                        .build())
                .build();

        // start conversation with model
        return googleAiFeignClient.generateContent(model, key, requestModel);

    }
}
