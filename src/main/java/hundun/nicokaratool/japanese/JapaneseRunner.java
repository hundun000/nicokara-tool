package hundun.nicokaratool.japanese;

import hundun.nicokaratool.base.BaseService.ServiceResult;
import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JapaneseRunner {
    static JapaneseService service = new JapaneseService();

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter name: ");
        String name = br.readLine();

        ServiceResult<JapaneseLine> serviceResult = service.work(name);

        System.out.println("Ruby: ");
        System.out.println(serviceResult.getRuby());
    }

}
