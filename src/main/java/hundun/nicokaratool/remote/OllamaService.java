package hundun.nicokaratool.remote;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.types.OllamaModelType;

public class OllamaService {

    static OllamaAPI ollamaAPI;
    static String STEP1_ASK_TEMPLATE = "";
    static {
        ollamaAPI = new OllamaAPI("http://localhost:11434/");
        ollamaAPI.setRequestTimeoutSeconds(60);
    }

    public static OllamaChatResult singleAsk(String ask) throws Exception {


        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(OllamaModelType.DEEPSEEK_R1 + ":14b");

        // create first user question
        OllamaChatRequest requestModel = builder.withMessage(OllamaChatMessageRole.USER, ask)
                .build();

        // start conversation with model
        return ollamaAPI.chat(requestModel);

    }

}
