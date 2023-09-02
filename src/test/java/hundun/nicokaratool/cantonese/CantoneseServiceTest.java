package hundun.nicokaratool.cantonese;

import hundun.nicokaratool.base.BaseService.ServiceResult;
import hundun.nicokaratool.japanese.JapaneseService;
import org.junit.Test;

import java.io.IOException;

public class CantoneseServiceTest {

    static CantoneseService service = new CantoneseService();

    @Test
    public void test() throws IOException {
        String name = "example-cantonese";

        ServiceResult serviceResult = service.work(name);

        System.out.println("Ruby: ");
        System.out.println(serviceResult.getRuby());
    }
}
