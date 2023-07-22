package demo.base;

import lombok.*;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class KanjiInfo {
    String kanji;
    Map<String, List<String>> pronunciationMap;


}
