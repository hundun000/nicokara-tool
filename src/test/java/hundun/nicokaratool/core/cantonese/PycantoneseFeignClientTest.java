package hundun.nicokaratool.core.cantonese;

import hundun.nicokaratool.core.cantonese.PycantoneseFeignClient.YaleRequest;
import hundun.nicokaratool.core.cantonese.PycantoneseFeignClient.YaleResponse;
import org.junit.Test;

public class PycantoneseFeignClientTest {

    @Test
    public void test() {
        PycantoneseFeignClient client = PycantoneseFeignClient.instance();
        YaleRequest request = YaleRequest.builder()
                .text("你好世界")
                .build();
        YaleResponse response = client.characters_to_jyutping(request);
        System.out.println(response);
    }

}
