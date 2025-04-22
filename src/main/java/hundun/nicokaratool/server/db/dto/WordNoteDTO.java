package hundun.nicokaratool.server.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WordNoteDTO {
    private String text; //文本类型，学习点对应歌词文本
    private String hurikana; //文本类型，汉字的振假名，若无可省略。
    private String origin; //文本类型，动词原形或形容原形。若已是原形可省略。
    private String translation; //这个词的中文翻译。如果该词不需要翻译，则省略此字段。
    private String explain; //描述这个词在整个句子中的成分，以及和其他词的关系。
}