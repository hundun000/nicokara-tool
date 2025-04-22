package hundun.nicokaratool.core.remote;

import hundun.nicokaratool.core.base.SecretConfig;

public class GoogleServiceImpl {


    IGoogleTranslationFeignClient googleTranslationFeignClient;

    public GoogleServiceImpl() {
        googleTranslationFeignClient = IGoogleTranslationFeignClient.instance(SecretConfig.proxyConfig);
    }

    public String translateJaToZh(
            String q
    ) {
        var response = googleTranslationFeignClient.translate("dict-chrome-ex", "ja", "zh", q);
        return response.stream()
                .findFirst()
                .orElse("");
    };


}
