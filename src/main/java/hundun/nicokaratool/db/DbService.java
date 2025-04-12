package hundun.nicokaratool.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import hundun.nicokaratool.util.Utils;
import io.github.ollama4j.models.chat.OllamaChatResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DbService {

    public ObjectMapper objectMapper = new ObjectMapper();
    SongRepository songRepository = new SongRepository();
    WordNoteRepository wordNoteRepository = new WordNoteRepository();
    public DbService() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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

    static final Object[] TABLE_TITLES = new String[] {
            "原文/中文", "解释", "原形", "分类", "等级", "更多解释"
    };

    private File getActualFile(String fileName) {
        String actualFolder = MainRunner.PRIVATE_IO_FOLDER;
        File actualFolderFile = new File(actualFolder);
        if (!actualFolderFile.exists()) {
            actualFolder = MainRunner.RUNTIME_IO_FOLDER;
        }
        return new File(actualFolder + fileName);
    }

    public void runAiStep1(String fileName) throws Exception {
        File actualInput = getActualFile(fileName + ".txt");
        File actualOutput = getActualFile(fileName + ".step1.json");

        List<String> lyricLines = Utils.readAllLines(actualInput);
        List<List<String>> originGroups = lyricLines.stream()
                .map(it -> List.of(it))
                .collect(Collectors.toList());
        List<List<String>> lyricTaskGroups = aiTaskGroups(originGroups, it -> it.stream().allMatch(itt -> itt.isEmpty()), 10, 25);

        File actualStep1AskTemplateFile = getActualFile( "Step1AskTemplate.txt");
        String step1AskTemplate = Utils.readAllLines(actualStep1AskTemplateFile).stream().collect(Collectors.joining("\n"));
        List<AiStep1ResultNode> result = new ArrayList<>();

        for (int i = 0; i < lyricTaskGroups.size(); i++) {
            List<String> lyricTaskGroup = lyricTaskGroups.get(i);
            List<AiStep1ResultNode> groupResult = null;
            int retry = 0;
            while (groupResult == null && retry < 3) {
                log.info("start lyricTaskGroup[{}] size = {}, retry = {}", i, lyricTaskGroup.size(), retry);
                groupResult = aiStep1Group(lyricTaskGroup, step1AskTemplate);
                retry++;
            }
            if (groupResult != null) {
                result.addAll(groupResult);
            } else {
                throw new Exception("cannot handle group = " + lyricTaskGroup);
            }
        }
        objectMapper.writeValue(actualOutput, result);
    }

    public void runAiStep2(String title, String artist) throws Exception {
        File step1ResultFile = getActualFile(title + ".step1.json");
        File resultFile = getActualFile(title + ".step2.json");
        File step2AskTemplateFile = getActualFile( "Step2AskTemplate.txt");
        String step2AskTemplate = Utils.readAllLines(step2AskTemplateFile).stream().collect(Collectors.joining("\n"));

        SongDTO songDTO = aiStep2PrepareResult(step1ResultFile, resultFile, title, artist);
        // 只选择待处理的LyricGroupDTO
        List<List<LyricLineDTO>> targetGroups = songDTO.getGroups().stream()
                .filter(it -> it.getLineNotes().stream()
                        .anyMatch(itt -> itt.getWordNotes() == null)
                )
                .map(it -> it.getLineNotes())
                .collect(Collectors.toList());
        List<List<LyricLineDTO>> taskGroups = aiTaskGroups(targetGroups, it -> false, 2, 4);
        for (int i = 0; i < taskGroups.size(); i++) {
            // taskGroup的所有歌词合起来问一次
            List<LyricLineDTO> taskGroup = taskGroups.get(i);
            List<String> askLines = taskGroup.stream()
                    .map(it -> it.getLyric())
                    .collect(Collectors.toList());
            List<LyricLineDTO> groupResult = null;
            int retry = 0;
            while (groupResult == null && retry < 3) {
                log.info("start aiStep2Group Group[{}] size = {}, retry = {}", i, askLines.size(), retry);
                groupResult = aiStep2Group(askLines, step2AskTemplate);
                retry++;
            }
            if (groupResult != null) {
                // 将结果分配回taskGroup
                for (LyricLineDTO lineDTO : taskGroup) {
                    LyricLineDTO resultPop = groupResult.remove(0);
                    lineDTO.setWordNotes(resultPop.getWordNotes());
                }
            } else {
                log.error("cannot handle askLines = {}", askLines);
            }
        }
        objectMapper.writeValue(resultFile, songDTO);
    }

    private List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplateFile) throws Exception {
        String ask = step2AskTemplateFile + "\n" + askLines.stream().collect(Collectors.joining("\n\n"));
        try {
            OllamaChatResult chatResult = OllamaService.singleAsk(ask);
            String content = chatResult.getResponseModel().getMessage().getContent();
            content = content.split("</think>")[1].trim();
            content = content.replace("```json", "").replace("```", "");
            List<LyricLineDTO> nodes = objectMapper.readValue(content, objectMapper.getTypeFactory().constructCollectionType(List.class, LyricLineDTO.class));
            List<String> resultLines = nodes.stream().map(node -> node.getLyric()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                throw new Exception("resultLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad aiStep1Group: ", e);
        }
        return null;
    }

    private SongDTO aiStep2PrepareResult(File step1ResultFile, File resultFile, String title, String artist) throws Exception {
        if (resultFile.exists()) {
            return objectMapper.readValue(resultFile, SongDTO.class);
        } else if (step1ResultFile.exists()) {
            List<AiStep1ResultNode> step1ResultNodes = objectMapper.readValue(step1ResultFile, objectMapper.getTypeFactory().constructCollectionType(List.class, AiStep1ResultNode.class));
            List<LyricGroupDTO> groups = step1ResultNodes.stream()
                    .map(it -> LyricGroupDTO.builder()
                            .translation(it.getTranslation())
                            .groupNote(it.getGroupNote())
                            .lineNotes(it.getLyrics().stream()
                                    .map(itt -> LyricLineDTO.builder()
                                            .lyric(itt)
                                            .build())
                                    .collect(Collectors.toList())
                            )
                            .build())
                    .collect(Collectors.toList());
            return SongDTO.builder()
                    .title(title)
                    .artist(artist)
                    .groups(groups)
                    .build();
        } else {
            throw new Exception("file not exist");
        }
    }


    private List<AiStep1ResultNode> aiStep1Group(List<String> lyricLines, String actualStep1AskTemplate) {
        String ask = actualStep1AskTemplate + lyricLines.stream().collect(Collectors.joining("\n"));
        try {
            OllamaChatResult chatResult = OllamaService.singleAsk(ask);
            String content = chatResult.getResponseModel().getMessage().getContent();
            content = content.split("</think>")[1].trim();
            List<AiStep1ResultNode> nodes = objectMapper.readValue(content, objectMapper.getTypeFactory().constructCollectionType(List.class, AiStep1ResultNode.class));
            List<String> resultLines = nodes.stream().flatMap(node -> node.getLyrics().stream()).collect(Collectors.toList());
            if (!lyricLines.equals(resultLines)) {
                throw new Exception("lyricLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad aiStep1Group: ", e);
        }
        return null;
    }

    @Data
    public static class AiStep1ResultNode {
        private String translation;
        private String groupNote;
        private List<String> lyrics;
    }

    private List<List<String>> aiStep1TaskGroups(List<String> lyricLines) {
        int MIN_LINE = 10;
        int MAX_LINE = 25;
        List<List<String>> lyricTaskGroups = new ArrayList<>();
        List<String> currentGroup = null;
        while (!lyricLines.isEmpty()) {
            if (currentGroup == null) {
                currentGroup = new ArrayList<>();
                lyricTaskGroups.add(currentGroup);
            }
            String line = lyricLines.remove(0);
            boolean currentGroupContinue;
            if (currentGroup.size() < MIN_LINE) {
                currentGroupContinue = true;
            } else if (currentGroup.size() < MAX_LINE) {
                if (line.isEmpty()) {
                    currentGroupContinue = false;
                } else {
                    currentGroupContinue = true;
                }
            } else {
                currentGroupContinue = false;
            }
            if (!line.isEmpty()) {
                currentGroup.add(line);
            }
            if (!currentGroupContinue) {
                currentGroup = null;
            }
        }
        return lyricTaskGroups;
    }


    /**
     *将List<T>分为每组大小介于[MIN_SIZE, MAX_SIZE]的List<List<T>> (仅最后一组个数可能不足)；
     * 分组期间，如果某组已满足大于MIN_SIZE时，emptyChecker触发，则提前结束这一组；否则继续增加直到MAX_SIZE；
     */
    private <T> List<List<T>> aiTaskGroups(List<List<T>> originGroups, Function<List<T>, Boolean> emptyChecker, int MIN_SIZE, int MAX_SIZE) {
        originGroups = new ArrayList<>(originGroups);
        List<List<T>> resultGroups = new ArrayList<>();
        List<T> currentGroup = null;
        while (!originGroups.isEmpty()) {
            if (currentGroup == null) {
                currentGroup = new ArrayList<>();
                resultGroups.add(currentGroup);
            }
            List<T> originGroup = originGroups.remove(0);
            boolean currentGroupContinue;
            if (currentGroup.size() + originGroup.size() <= MIN_SIZE) {
                currentGroupContinue = true;
            } else if (currentGroup.size() + originGroup.size() < MAX_SIZE) {
                if (emptyChecker.apply(originGroup)) {
                    currentGroupContinue = false;
                } else {
                    currentGroupContinue = true;
                }
            } else {
                currentGroupContinue = false;
            }
            if (!emptyChecker.apply(originGroup)) {
                currentGroup.addAll(originGroup);
            }
            if (!currentGroupContinue) {
                currentGroup = null;
            }
        }
        return resultGroups;
    }

    public void renderSongJson(String fileName) throws Exception {

        File actualInput = getActualFile(fileName + ".json");

        SongDTO songDTO = objectMapper.readValue(actualInput, SongDTO.class);
        StringBuilder result = new StringBuilder();
        result.append(new BoldText(songDTO.getTitle())).append("\n  ");
        result.append(songDTO.getArtist()).append("\n  ");
        result.append("<!-- preventPageBreak -->\n");
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
                                    + Optional.ofNullable(wordNoteDTO.getHurikana())
                                    .filter(it -> !it.equals(wordNoteDTO.getText()))
                                    .map(it -> "(" + it + ")")
                                    .orElse(""),
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
