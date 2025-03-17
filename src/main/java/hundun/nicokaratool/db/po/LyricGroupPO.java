package hundun.nicokaratool.db.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LyricGroupPO {
    private String translation;
    private String groupNote;
    int lineSize;
}