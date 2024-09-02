package hundun.nicokaratool.util;


import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Client;
import feign.Feign;
import feign.Feign.Builder;
import feign.Logger.Level;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author hundun
 * Created on 2021/07/01
 */
public class FeignClientFactory {

    static Level feignLogLevel = Level.BASIC;
    static Encoder feignEncoder = new JacksonEncoder();
    static Decoder feignDecoder= new JacksonDecoder();

    public static Client getProxiedClient(@Nullable JsonNode proxy) {
        Client client;
        if (proxy != null) {
            client = new Client.Proxied((SSLSocketFactory) null, (HostnameVerifier) null, new Proxy(Type.HTTP, new InetSocketAddress(
                    proxy.get("host").asText(),
                    proxy.get("port").asInt()
            )));
        } else {
            client = new Client.Default((SSLSocketFactory) null, (HostnameVerifier) null);
        }
        return client;
    }

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
