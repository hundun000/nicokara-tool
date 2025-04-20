package hundun.nicokaratool.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

@Slf4j
public class SecretConfig {
    static ObjectMapper objectMapper = new ObjectMapper();
    public static String gptAuthorization;
    public static String googleKey;
    @Nullable
    public static JsonNode proxyConfig;
    @Nullable
    public static JsonNode ffmpegConfig;

    static {
        try {
            JsonNode secretFile = objectMapper.readTree(new File("data/Secret/secret.json"));
            gptAuthorization = secretFile.get("gptKey").asText();
            googleKey = secretFile.get("googleKey").asText();
            proxyConfig = secretFile.get("proxyConfig");
            ffmpegConfig = secretFile.get("ffmpegConfig");
        } catch (IOException e) {
            log.error("bad secretFile read:", e);
        }
    }

}
