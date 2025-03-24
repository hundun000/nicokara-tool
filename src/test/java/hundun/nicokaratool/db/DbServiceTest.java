package hundun.nicokaratool.db;

import org.junit.Test;

import static org.junit.Assert.*;

public class DbServiceTest {

    DbService dbService = new DbService();


    @Test
    public void renderSongJson() throws Exception {
        dbService.renderSongJson("example-japanese-short.db");
    }
}