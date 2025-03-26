package hundun.nicokaratool.db.po;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity("songs")
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