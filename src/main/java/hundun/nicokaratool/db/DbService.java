package hundun.nicokaratool.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hundun.nicokaratool.MainRunner;
import hundun.nicokaratool.db.dto.LyricGroupDTO;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import hundun.nicokaratool.db.dto.SongDTO;
import hundun.nicokaratool.db.dto.WordNoteDTO;
import hundun.nicokaratool.db.po.SongPO;
import hundun.nicokaratool.db.po.SongPO.LyricGroupPO;
import hundun.nicokaratool.db.po.SongPO.LyricLinePO;
import hundun.nicokaratool.db.po.WordNotePO;
import hundun.nicokaratool.db.repository.SongRepository;
import hundun.nicokaratool.db.repository.WordNoteRepository;
import lombok.extern.slf4j.Slf4j;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class DbService {

    public ObjectMapper objectMapper = new ObjectMapper();
    SongRepository songRepository = new SongRepository();
    WordNoteRepository wordNoteRepository = new WordNoteRepository();
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
        List<LyricGroupPO> groupPOS = new ArrayList<>();
        List<WordNotePO> notePOS = new ArrayList<>();
        for (int i = 0; i < songDTO.getGroups().size(); i++) {
            LyricGroupDTO groupDTO = songDTO.getGroups().get(i);
            List<LyricLinePO> linePOS = new ArrayList<>();
            for (int j = 0; j < groupDTO.getLineNotes().size(); j++) {
                LyricLineDTO lineDTO = groupDTO.getLineNotes().get(j);
                for (int k = 0; k < lineDTO.getWordNotes().size(); k++) {
                    var wordNoteDTO = lineDTO.getWordNotes().get(k);
                    var wordNotePO = objectMapper.readValue(objectMapper.writeValueAsString(wordNoteDTO), WordNotePO.class);
                    wordNotePO.setId(WordNotePO.toId(songPO.getId(), i, j, k));
                    wordNotePO.setSongId(songPO.getId());
                    wordNotePO.setGroupIndex(i);
                    wordNotePO.setLineIndex(j);
                    wordNotePO.setWordIndex(k);
                    notePOS.add(wordNotePO);
                }
                linePOS.add(LyricLinePO.builder()
                        .lyric(lineDTO.getLyric())
                        .wordSize(lineDTO.getWordNotes().size())
                        .build()
                );
            }
            groupPOS.add(
                    LyricGroupPO.builder()
                            .translation(groupDTO.getTranslation())
                            .groupNote(groupDTO.getGroupNote())
                            .lines(linePOS)
                            .build()
            );
        }
        songPO.setGroups(groupPOS);
        songRepository.save(songPO);
        wordNoteRepository.saveAll(notePOS);
        log.info("save songPO: {}, notePOS size = {}", songPO.getId(), notePOS.size());
    }

    static final String[] TABLE_TITLES = new String[] {
            "原文/中文", "解释", "原形", "分类", "等级", "更多解释"
    };

    private File getActualJsonFile(String fileName) {
        String actualFolder = MainRunner.PRIVATE_IO_FOLDER;
        File actualInput = new File(actualFolder + fileName + ".json");
        if (!actualInput.exists()) {
            actualFolder = MainRunner.RUNTIME_IO_FOLDER;
            actualInput = new File(actualFolder + fileName + ".json");
        }
        return actualInput;
    }

    public void renderSongJson(String fileName) throws Exception {

        File actualInput = getActualJsonFile(fileName);

        SongDTO songDTO = objectMapper.readValue(actualInput, SongDTO.class);
        StringBuilder result = new StringBuilder();
        result.append(new BoldText(songDTO.getTitle())).append("\n");
        result.append(songDTO.getArtist()).append("\n");
        Table.Builder tableBuilder = new Table.Builder()
                .addRow(TABLE_TITLES);
        songDTO.getGroups().forEach(lyricGroupDTO -> {
            tableBuilder.addRow(
                    new ItalicText(Optional.ofNullable(lyricGroupDTO.getTranslation()).orElse("")),
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
                            Optional.ofNullable(wordNoteDTO.getCategory()).orElse(List.of()).stream().collect(Collectors.joining(", ")),
                            Optional.ofNullable(wordNoteDTO.getLevel()).orElse(""),
                            Optional.ofNullable(wordNoteDTO.getExtensionExplain()).orElse("")
                    );
                });
            });
            tableBuilder.addRow();
        });
        result.append(tableBuilder.build());
        FileWriter myWriter = new FileWriter(actualInput.getParent() + File.separator + fileName + ".md");
        myWriter.write(result.toString());
        myWriter.close();
    }

    public SongDTO buildDTOFromDbByTitle(String title) throws Exception {

        SongPO songPO = songRepository.findFirstByTitle(title);
        List<LyricGroupDTO> groups = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < songPO.getGroups().size(); groupIndex++) {
            LyricGroupPO groupPO = songPO.getGroups().get(groupIndex);
            List<LyricLineDTO> lineDTOS = new ArrayList<>();
            for (int lineIndex = 0; lineIndex < groupPO.getLines().size(); lineIndex++) {
                LyricLinePO linePO = groupPO.getLines().get(lineIndex);
                List<WordNoteDTO> wordNotes = new ArrayList<>();
                for (int wordIndex = 0; wordIndex < linePO.getWordSize(); wordIndex++) {
                    String id = WordNotePO.toId(songPO.getId(), groupIndex, lineIndex, wordIndex);
                    WordNotePO wordNotePO = wordNoteRepository.findById(id);
                    if (wordNotePO == null) {
                        throw new Exception("NotFound WordNotePO id = " + id);
                    }
                    WordNoteDTO wordNoteDTO = objectMapper.readValue(objectMapper.writeValueAsString(wordNotePO), WordNoteDTO.class);
                    wordNotes.add(wordNoteDTO);
                }
                LyricLineDTO lineDTO = LyricLineDTO.builder()
                        .lyric(linePO.getLyric())
                        .wordNotes(wordNotes)
                        .build();
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
