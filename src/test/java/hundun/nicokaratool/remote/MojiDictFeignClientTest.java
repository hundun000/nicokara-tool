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

    @Test
    public void test2() throws JsonProcessingException {
        MojiDictFeignClient client = MojiDictFeignClient.instance();
        var response = client.union_api(MojiDictRequest.quickBuild("抱いて"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
        response = client.union_api(MojiDictRequest.quickBuild("抱い"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
        response = client.union_api(MojiDictRequest.quickBuild("進もう"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
        response = client.union_api(MojiDictRequest.quickBuild("進も"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
        response = client.union_api(MojiDictRequest.quickBuild("大切な"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
        response = client.union_api(MojiDictRequest.quickBuild("大切"));
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
    }

}
