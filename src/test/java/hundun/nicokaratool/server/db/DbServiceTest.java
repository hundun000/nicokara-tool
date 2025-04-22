package hundun.nicokaratool.server.db;

import hundun.nicokaratool.core.util.JsonUtils;
import hundun.nicokaratool.server.db.DbService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DbServiceTest {

    @Autowired
    DbService dbService;

    static String fileName = "きらきら星 - unknown";
    static String[] args = DbService.handleFileName(fileName);
    static String title = args[1];

    @Test
    public void runAiStep1() throws Exception {
        dbService.runAiStep1(args);
    }

    @Test
    public void runAiStep2() throws Exception {
        dbService.runAiStep2(args);
    }

    @Test
    public void renderSongJson() throws Exception {
        dbService.renderSongJson(args);
    }

    @Test
    public void loadSongJson() throws Exception {
        dbService.loadSongJson(title);
    }

    @Test
    public void buildDTOFromDbByTitle() throws Exception {
        var result = dbService.buildDTOFromDbByTitle(title);
        System.out.println(JsonUtils.objectMapper.writeValueAsString(result));
    }

}