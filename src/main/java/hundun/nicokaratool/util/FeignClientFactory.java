package hundun.nicokaratool.util;


import java.util.concurrent.TimeUnit;

import feign.Feign;
import feign.Feign.Builder;
import feign.Logger.Level;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 * @author hundun
 * Created on 2021/07/01
 */
public class FeignClientFactory {

    static Level feignLogLevel = Level.BASIC;
    static Encoder feignEncoder = new JacksonEncoder();
    static Decoder feignDecoder= new JacksonDecoder();


    public static <T> T get(Class<T> clazz, String url) {
        return getBaseBuilder()
                .options(new Request.Options(1, TimeUnit.SECONDS, 2, TimeUnit.SECONDS, true))
                .target(clazz, url)
                ;
    }

    public static <T> Builder getBaseBuilder() {
        return Feign.builder()
                .encoder(feignEncoder)
                .decoder(feignDecoder)
                .logLevel(feignLogLevel)
                ;
    }
    
}
