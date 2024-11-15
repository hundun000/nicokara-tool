package hundun.nicokaratool.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RootHint {
    Map<String, String> translationCacheMap;
    List<KanjiHintPO> kanjiHints;
    List<String> nluDisallowHints;
}
