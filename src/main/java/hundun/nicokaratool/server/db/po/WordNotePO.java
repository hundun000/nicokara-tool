package hundun.nicokaratool.server.db.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("wordNotes")
public class WordNotePO {
    /**
     * 来自toId
     */
    @Id
    private String id;
    @Indexed
    private String songId;
    private int groupIndex;
    private int lineIndex;
    int wordIndex;
    public static String toId(String songId, int groupIndex, int lineIndex, int wordIndex) {
        return songId + "_" + groupIndex + "_" + lineIndex + "_" + wordIndex;
    }

    /*
    ------ 以下字段通过反序列化赋值，应和 WordNoteDTO 一致
     */
    private String text;
    private String hurikana;
    private String origin;
    private String translation;
    private String contextualFunction;


}