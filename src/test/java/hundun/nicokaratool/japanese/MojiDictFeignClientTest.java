package hundun.nicokaratool.japanese;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleRequest;
import hundun.nicokaratool.cantonese.PycantoneseFeignClient.YaleResponse;
import hundun.nicokaratool.japanese.MojiDictFeignClient.MojiDictRequest;
import org.junit.Test;

public class MojiDictFeignClientTest {


    @Test
    public void test() throws JsonProcessingException {
        MojiDictFeignClient client = MojiDictFeignClient.instance();
        var response = client.union_api(MojiDictRequest.quickBuild("食べる"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
    }

}
