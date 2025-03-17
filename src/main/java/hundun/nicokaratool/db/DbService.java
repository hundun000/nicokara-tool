package hundun.nicokaratool.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hundun.nicokaratool.MainRunner;
import hundun.nicokaratool.db.dto.LyricGroupDTO;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import hundun.nicokaratool.db.dto.SongDTO;
import hundun.nicokaratool.db.po.LyricGroupPO;
import hundun.nicokaratool.db.po.LyricLinePO;
import hundun.nicokaratool.db.po.SongPO;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.heading.Heading;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbService {

    ObjectMapper objectMapper = new ObjectMapper();
    SongRepository songRepository = new SongRepository();
    LineRepository lineRepository = new LineRepository();

    public DbService() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public void loadSongJson(String fileName) throws Exception {
        SongDTO songDTO = objectMapper.readValue(new File(MainRunner.RUNTIME_IO_FOLDER + fileName + ".json"), SongDTO.class);
        SongPO songPO = SongPO.builder()
                .title(songDTO.getTitle())
                .artist(songDTO.getArtist())
                .build();
        if (songRepository.existTitle(songDTO.getTitle())) {
            throw new Exception("existTitle: " + songDTO.getTitle());
        }
        songPO = songRepository.save(songPO);
        List<LyricLinePO> linePOS = new ArrayList<>();
        List<LyricGroupPO> groupPOS = new ArrayList<>();
        for (int i = 0; i < songDTO.getGroups().size(); i++) {
            LyricGroupDTO groupDTO = songDTO.getGroups().get(i);
            for (int j = 0; j < groupDTO.getLineNotes().size(); j++) {
                LyricLineDTO lineDTO = groupDTO.getLineNotes().get(j);
                linePOS.add(
                        LyricLinePO.builder()
                                .id(LyricLinePO.toId(songPO.getId(), i, i))
                                .groupIndex(i)
                                .lineIndex(j)
                                .songId(songPO.getId())
                                .build()
                );
            }
            groupPOS.add(
                    LyricGroupPO.builder()
                            .translation(groupDTO.getTranslation())
                            .groupNote(groupDTO.getGroupNote())
                            .lineSize(groupDTO.getLineNotes().size())
                            .build()
            );
        }
        songPO.setGroups(groupPOS);
        songRepository.save(songPO);
        lineRepository.saveAll(linePOS);
    }

    static final String[] TABLE_TITLES = new String[] {
            "原文/中文", "解释", "原形", "分类", "等级", "更多解释"
    };

    public void renderSongJson(String fileName) throws Exception {
        SongDTO songDTO = objectMapper.readValue(new File(MainRunner.RUNTIME_IO_FOLDER + fileName + ".json"), SongDTO.class);
        StringBuilder result = new StringBuilder();
        result.append(new BoldText(songDTO.getTitle())).append("\n");
        result.append(songDTO.getArtist()).append("\n");
        Table.Builder tableBuilder = new Table.Builder()
                .addRow(TABLE_TITLES);
        songDTO.getGroups().forEach(lyricGroupDTO -> {
            tableBuilder.addRow(
                    Optional.ofNullable(lyricGroupDTO.getTranslation()).orElse(""),
                    Optional.ofNullable(lyricGroupDTO.getGroupNote()).orElse("")
            );
            lyricGroupDTO.getLineNotes().forEach(lyricLineDTO -> {
                tableBuilder.addRow(
                        new BoldText(Optional.ofNullable(lyricLineDTO.getLyric()).orElse(""))
                );
                lyricLineDTO.getWordNotes().forEach(wordNoteDTO -> {
                    tableBuilder.addRow(
                            Optional.ofNullable(wordNoteDTO.getText()).orElse("")
                                    + Optional.ofNullable(wordNoteDTO.getHurikana()).map(it -> "(" + it + ")").orElse(""),
                            Optional.ofNullable(wordNoteDTO.getExplain()).orElse(""),
                            Optional.ofNullable(wordNoteDTO.getOrigin()).orElse(""),
                            Optional.ofNullable(wordNoteDTO.getCategory()).orElse(List.of()),
                            Optional.ofNullable(wordNoteDTO.getLevel()).orElse(""),
                            Optional.ofNullable(wordNoteDTO.getGeneralExplain()).orElse("")
                    );
                });
            });
            tableBuilder.addRow();
        });
        result.append(tableBuilder.build());
        FileWriter myWriter = new FileWriter(MainRunner.RUNTIME_IO_FOLDER + fileName + ".md");
        myWriter.write(result.toString());
        myWriter.close();
    }

    public SongDTO buildDTOFromDbByTitle(String title) throws Exception {

        SongPO songPO = songRepository.findFirstByTitle(title);
        List<LyricGroupDTO> groups = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < songPO.getGroups().size(); groupIndex++) {
            LyricGroupPO groupPO = songPO.getGroups().get(groupIndex);
            List<LyricLineDTO> lineDTOS = new ArrayList<>();
            for (int lineIndex = 0; lineIndex < groupPO.getLineSize(); lineIndex++) {
                String id = LyricLinePO.toId(songPO.getId(), groupIndex, lineIndex);
                LyricLinePO find = lineRepository.findById(id);
                if (find == null) {
                    throw new Exception("NotFound LyricLinePO id = " + id);
                }
                LyricLineDTO lineDTO = objectMapper.readValue(objectMapper.writeValueAsString(find), LyricLineDTO.class);
                lineDTOS.add(lineDTO);
            }
            groups.add(
                    LyricGroupDTO.builder()
                            .translation(groupPO.getTranslation())
                            .groupNote(groupPO.getGroupNote())
                            .lineNotes(lineDTOS)
                            .build()
            );
        }
        return SongDTO.builder()
                .title(songPO.getTitle())
                .artist(songPO.getArtist())
                .groups(groups)
                .build();
    }
}
