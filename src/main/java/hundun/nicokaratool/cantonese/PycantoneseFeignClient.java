package hundun.nicokaratool.cantonese;

import feign.Headers;
import feign.RequestLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public interface PycantoneseFeignClient {

    public static PycantoneseFeignClient instance() {
        return FeignClientFactory.get(PycantoneseFeignClient.class, "http://localhost:8002");
    }

    @RequestLine("POST /jyutping_to_yale")
    @Headers("Content-Type: application/json")
    YaleResponse jyutping_to_yale(YaleRequest request);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class YaleRequest {
        String text;
        List<String> disallow;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class YaleResponse {
        List<List<String>> yale;
    }
}
