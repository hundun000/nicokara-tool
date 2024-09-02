package hundun.nicokaratool.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import hundun.nicokaratool.japanese.JapaneseService;
import hundun.nicokaratool.remote.MojiDictFeignClient.MojiDictRequest;
import org.junit.Test;

public class MojiDictFeignClientTest {


    @Test
    public void test() throws JsonProcessingException {
        MojiDictFeignClient client = MojiDictFeignClient.instance();
        var response = client.union_api(MojiDictRequest.quickBuild("食べる"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
    }

}
