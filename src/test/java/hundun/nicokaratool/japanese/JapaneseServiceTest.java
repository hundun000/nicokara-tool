package hundun.nicokaratool.japanese;

import hundun.nicokaratool.base.BaseService.ServiceResult;
import org.junit.Test;

import java.io.IOException;

public class JapaneseServiceTest {

    static JapaneseService service = new JapaneseService();

    @Test
    public void test() throws IOException {
        String name = "example-japanese";

        ServiceResult serviceResult = service.work(name);

        System.out.println("Ruby: ");
        System.out.println(serviceResult.getRuby());
    }
}
