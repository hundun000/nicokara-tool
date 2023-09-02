package hundun.nicokaratool.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RootHint {
    List<KanjiHintPO> kanjiHints;
    List<String> nluDisallowHints;
}
