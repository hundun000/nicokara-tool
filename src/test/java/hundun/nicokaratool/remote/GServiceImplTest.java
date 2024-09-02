package hundun.nicokaratool.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import hundun.nicokaratool.japanese.JapaneseService;
import org.junit.Test;

public class GServiceImplTest {


    GoogleServiceImpl googleService = new GoogleServiceImpl();

    @Test
    public void test() throws JsonProcessingException {
        var response = googleService.translateJaToZh("お寿司が食べたい");
        System.out.println(JapaneseService.objectMapper.writeValueAsString(response));
    }
}
