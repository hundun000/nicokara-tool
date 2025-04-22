package hundun.nicokaratool.server.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LyricGroupDTO {
    private String translation;
    private String groupNote;
    private List<LyricLineDTO> lineNotes;
}