package hundun.nicokaratool;

import hundun.nicokaratool.db.DbService;
import hundun.nicokaratool.japanese.JapaneseService;
import hundun.nicokaratool.japanese.JapaneseService.ServiceContext;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static hundun.nicokaratool.japanese.JapaneseService.objectMapper;

public class MainRunner {
    public static final String CACHE_FOLDER = "data/caches/";
    public static final String PRIVATE_IO_FOLDER = "private-io/";
    public static final String RUNTIME_IO_FOLDER = "runtime-io/";
    static final String HANDLER_NICOKARA = "nicokara";
    static final String HANDLER_DB = "db";
    static JapaneseService nicokaraService = new JapaneseService();
    static DbService dbService = new DbService();

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter handler and args: ");
        String readLine = br.readLine();

        List<String> parts = Arrays.asList(readLine.split(" --"));
        String handler = parts.get(0);
        boolean debug = parts.stream()
                .filter(it -> it.equals("debug"))
                .findAny()
                .isPresent();
        System.out.println("using: " + handler + " ...");
        try {
            if (handler.equals(HANDLER_NICOKARA)) {
                nicokaraService.getArgPackage().setWithTranslation(
                        parts.stream()
                                .filter(it -> it.equals("withTranslation"))
                                .findAny()
                                .isPresent()
                );
                ServiceContext serviceResult = nicokaraService.quickStep1(handler);
                if (debug) {
                    System.out.println("Lines: ");
                    System.out.println(objectMapper.writeValueAsString(serviceResult.getParsedLines()));
                }
                nicokaraService.workStep2(serviceResult);
            } else if (handler.equals(HANDLER_DB)) {
                String dbOperation = parts.get(1);
                if (dbOperation.equals("loadFile")) {
                    String fileName = parts.get(2);
                    dbService.loadSongJson(fileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("done.");
    }

}
