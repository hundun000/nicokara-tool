package hundun.nicokaratool.server.db.po;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document("songs")
public class SongPO {
    @Id
    private String id;
    private String title;
    private String artist;
    private List<LyricGroupPO> groups;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LyricGroupPO {
        private String translation;
        private String groupNote;
        List<LyricLinePO> lines;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LyricLinePO {
        private String lyric;
        int wordSize;
    }
}