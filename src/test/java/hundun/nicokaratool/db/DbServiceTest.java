package hundun.nicokaratool.db;

import org.junit.Test;

import static org.junit.Assert.*;

public class DbServiceTest {

    DbService dbService = new DbService();


    @Test
    public void renderSongJson() throws Exception {
        dbService.renderSongJson("example-japanese-short.db");
    }

    @Test
    public void loadSongJson() throws Exception {
        dbService.loadSongJson("example-japanese-short.db");
    }

    @Test
    public void buildDTOFromDbByTitle() throws Exception {
        var result = dbService.buildDTOFromDbByTitle("きらきら星");
        System.out.println(dbService.objectMapper.writeValueAsString(result));
    }
}