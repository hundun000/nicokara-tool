package hundun.nicokaratool.remote;

import hundun.nicokaratool.base.SecretConfig;

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
