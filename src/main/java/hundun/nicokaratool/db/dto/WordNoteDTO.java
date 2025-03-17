package hundun.nicokaratool.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WordNoteDTO {
    private String text; //文本类型，学习点对应歌词文本
    private String hurikana; //文本类型，汉字的振假名，若无可省略。
    private String origin; //文本类型，动词原形或形容原形。若已是原形可省略。
    private List<String> category; //文本类型数组，日语学习点分类，严格按照后文的category值域。如果一个词的学习点同时符合多个category，则均列出在数组里。
    private String explain; //文本类型，侧重于说明当前句子。
    private String generalExplain; //文本类型，侧重于说明一般性的语法，不需要代入当前句子。如果explain已经足够则可省略。
    private String level; //文本类型，JLPT参考语法等级，值域为; //N5~N1，或"其他"
}