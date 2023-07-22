package demo.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class KanjiHint {
    String kanji;
    Map<String, List<String>> hintMap;


    public void appendAsRuby(List<String> lines) {
        hintMap.forEach((pronunciation, hints) -> {
            hints.forEach(hint -> {
                lines.add(String.format("@Ruby%d=%s,%s",
                        lines.size() + 1,
                        kanji,
                        hint
                ));
            });
        });
    }
}
