package hundun.nicokaratool.core;

import hundun.nicokaratool.server.db.DbService;
import hundun.nicokaratool.core.japanese.JapaneseService;
import hundun.nicokaratool.core.japanese.JapaneseService.ServiceContext;
import lombok.extern.slf4j.Slf4j;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static hundun.nicokaratool.core.japanese.JapaneseService.objectMapper;

@Slf4j
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
                String dbOperation = parts.stream()
                        .filter(it -> it.startsWith("operation="))
                        .map(it -> it.substring("operation=".length()))
                        .findFirst()
                        .orElse("");
                String fileName;
                boolean autoFindFile = parts.stream()
                        .filter(it -> it.equals("autoFindFile"))
                        .findAny()
                        .isPresent();
                if (autoFindFile) {
                    fileName = DbService.autoFindFile();
                    log.info("autoFindFile result = {}", fileName);
                } else {
                    fileName = parts.stream()
                            .filter(it -> it.startsWith("file="))
                            .map(it -> it.substring("file=".length()))
                            .findFirst()
                            .orElse(null);
                }
                if (fileName == null) {
                    throw new Exception("args fileName null");
                }
                if (dbOperation.startsWith("runAi")) {
                    String[] aiArgs = DbService.handleFileName(fileName);
                    boolean step1 = false;
                    boolean step2 = false;
                    boolean stepMd = false;
                    if (dbOperation.equals("runAi")) {
                        step1 = true;
                        step2 = true;
                        stepMd = true;
                    } else if (dbOperation.equals("runAiStep1")) {
                        step1 = true;
                    } else if (dbOperation.equals("runAiStep2")) {
                        step2 = true;
                    } else if (dbOperation.equals("runAiStepMd")) {
                        stepMd = true;
                    }
                    if (step1) {
                        dbService.runAiStep1(aiArgs);
                    }
                    if (step2) {
                        dbService.runAiStep2(aiArgs);
                    }
                    if (stepMd) {
                        dbService.renderSongJson(aiArgs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("bad command handle: ", e);
        }

        System.out.println("done.");
    }

}
