package hundun.nicokaratool.cantonese;

import hundun.nicokaratool.base.BaseService.ServiceResult;
import hundun.nicokaratool.base.lyrics.LyricLine;
import org.junit.Test;

import java.io.IOException;

public class CantoneseServiceTest {

    static CantoneseService service = new CantoneseService();

    @Test
    public void test() throws IOException {
        String name = "example-cantonese";

        ServiceResult<LyricLine> serviceResult = service.workStep1(name);

        System.out.println("Result: ");
        System.out.println(serviceResult.getLyricsText());
        System.out.println("\n");
        System.out.println(serviceResult.getRuby());
    }
}
