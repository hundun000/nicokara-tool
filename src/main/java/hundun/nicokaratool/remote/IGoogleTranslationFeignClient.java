package hundun.nicokaratool.remote;

import com.fasterxml.jackson.databind.JsonNode;
import feign.*;
import hundun.nicokaratool.util.FeignClientFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface IGoogleTranslationFeignClient {

    public static IGoogleTranslationFeignClient instance(@Nullable JsonNode proxy) {
        Client client = FeignClientFactory.getProxiedClient(proxy);
        var result = FeignClientFactory.getBaseBuilder()
                .client(client)
                .options(new Request.Options(1, TimeUnit.SECONDS, 2, TimeUnit.SECONDS, true))
                .target(IGoogleTranslationFeignClient.class, "https://translate.googleapis.com");
        return result;
    }

    /*
    有可能被重定向到： www.google.com/sorry/index 我们的系统检测到您的计算机网络中存在异常流量。请稍后重新发送请求。
     */

    @RequestLine("GET /translate_a/t?client={client}&sl={sl}&tl={tl}&q={q}")
    //@Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36")
    List<String> translate(
            @Param("client") String client,
            @Param("sl") String sl,
            @Param("tl") String tl,
            @Param("q") String q
    );
}
