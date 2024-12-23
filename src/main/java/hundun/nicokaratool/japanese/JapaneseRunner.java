package hundun.nicokaratool.japanese;

import hundun.nicokaratool.japanese.JapaneseService.ServiceContext;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static hundun.nicokaratool.japanese.JapaneseService.objectMapper;

public class JapaneseRunner {

    static JapaneseService service = new JapaneseService();

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter name and args: ");
        String readLine = br.readLine();

        List<String> parts = Arrays.asList(readLine.split(" --"));
        String name = parts.get(0);
        service.argPackage.withTranslation = parts.stream()
                .filter(it -> it.equals("withTranslation"))
                .findAny()
                .isPresent();
        boolean debug = parts.stream()
                .filter(it -> it.equals("debug"))
                .findAny()
                .isPresent();
        System.out.println("using: " + name + " ...");
        ServiceContext serviceResult = service.quickStep1(name);
        if (debug) {
            System.out.println("Lines: ");
            System.out.println(objectMapper.writeValueAsString(serviceResult.getParsedLines()));
        }

        service.workStep2(serviceResult);

        System.out.println("done.");
    }

}
