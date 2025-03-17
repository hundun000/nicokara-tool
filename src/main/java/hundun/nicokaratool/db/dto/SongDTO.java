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
public class SongDTO {
    private String title;
    private String artist;
    private List<LyricGroupDTO> groups;
}