package hundun.nicokaratool.core.remote;

import hundun.nicokaratool.core.remote.GptServiceImpl.LlmModel;
import org.junit.Test;

public class GptServiceImplTest {

    GptServiceImpl gptService = new GptServiceImpl();
    @Test
    public void testFun() {
        var result = gptService.run_single_and_save(
                LlmModel.GPT_3_5,
                "Translate the text to Simplified Chinese. Don't output anything other than translation results.",
                "お寿司が食べたい"
        );
        System.out.println(result);
    }
}
