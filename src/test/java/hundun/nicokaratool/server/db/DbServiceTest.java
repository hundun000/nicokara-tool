package hundun.nicokaratool.server.db;

import hundun.nicokaratool.core.util.JsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DbServiceTest {

    @Autowired
    SongService songService;

    static String fileName = "きらきら星 - unknown";
    static String[] args = SongService.handleFileName(fileName);
    static String title = args[1];

    @Test
    public void runAiStep1() throws Exception {
        songService.runAiStep1(args);
    }

    @Test
    public void runAiStep2() throws Exception {
        songService.runAiStep2(args);
    }

    @Test
    public void renderSongJson() throws Exception {
        songService.renderSongJson(args);
    }

    @Test
    public void loadSongJson() throws Exception {
        songService.saveSongJsonToDB(args);
    }

    @Test
    public void buildDTOFromDbByTitle() throws Exception {
        var result = songService.buildDTOFromDbByTitle(title);
        System.out.println(JsonUtils.objectMapper.writeValueAsString(result));
    }

}