package hundun.nicokaratool.cantonese;

import hundun.nicokaratool.base.BaseService.ServiceResult;

import java.io.IOException;

public class CantoneseRunner {


    static CantoneseService service = new CantoneseService();

    public static void main(String[] args) throws IOException {
        String name = "example-cantonese";
        ServiceResult serviceResult = service.work(name);
        System.out.println("Ruby: ");
        System.out.println(serviceResult.getRuby());
    }




}
