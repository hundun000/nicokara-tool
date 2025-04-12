package hundun.nicokaratool.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class Utils {

    public static List<String> readAllLines(String path) {
        try {
            return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("bad readAllLines: ", e);
            throw new RuntimeException();
        }
    }

    public static List<String> readAllLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("bad readAllLines: ", e);
            throw new RuntimeException();
        }
    }

    public static void writeAllLines(String path, String lines) {
        try {
            Files.writeString(Paths.get(path), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("bad readAllLines: ", e);
        }
    }
}
