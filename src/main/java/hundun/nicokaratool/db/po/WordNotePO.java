package hundun.nicokaratool.db.po;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity("wordNotes")
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

    private String text; //文本类型，学习点对应歌词文本
    private String hurikana; //文本类型，汉字的振假名，若无可省略。
    private String origin; //文本类型，动词原形或形容原形。若已是原形可省略。
    private List<String> category; //文本类型数组，日语学习点分类，严格按照后文的category值域。如果一个词的学习点同时符合多个category，则均列出在数组里。
    private String explain; //文本类型，侧重于说明当前句子。
    private String extensionExplain; //文本类型，侧重于说明一般性的语法，不需要代入当前句子。如果explain已经足够则可省略。
    private String level; //文本类型，JLPT参考语法等级，值域为; //N5~N1，或"其他"


    public static String toId(String songId, int groupIndex, int lineIndex, int wordIndex) {
        return songId + "_" + groupIndex + "_" + lineIndex + "_" + wordIndex;
    }
}