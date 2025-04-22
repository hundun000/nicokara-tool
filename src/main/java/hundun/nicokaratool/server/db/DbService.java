package hundun.nicokaratool.server.db;

import hundun.nicokaratool.core.MainRunner;
import hundun.nicokaratool.server.db.ai.AiServiceLocator;
import hundun.nicokaratool.server.db.dto.LyricGroupDTO;
import hundun.nicokaratool.server.db.dto.LyricLineDTO;
import hundun.nicokaratool.server.db.dto.SongDTO;
import hundun.nicokaratool.server.db.dto.WordNoteDTO;
import hundun.nicokaratool.server.db.po.SongPO;
import hundun.nicokaratool.server.db.po.SongPO.LyricGroupPO;
import hundun.nicokaratool.server.db.po.SongPO.LyricLinePO;
import hundun.nicokaratool.server.db.po.WordNotePO;
import hundun.nicokaratool.server.db.repository.SongRepository;
import hundun.nicokaratool.server.db.repository.WordNoteRepository;
import hundun.nicokaratool.core.japanese.JapaneseCharacterTool;
import hundun.nicokaratool.core.util.JsonUtils;
import hundun.nicokaratool.core.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DbService {

    AiServiceLocator aiServiceLocator = new AiServiceLocator();
    @Autowired
    SongRepository songRepository;
    @Autowired
    WordNoteRepository wordNoteRepository;
    public DbService() {

    }

    public static String autoFindFile() {
        return autoFindFile(".lrc", ".md").stream().findFirst().orElse(null);
    }

    /**
     * 在候选文件夹中寻找首个：
     * 1. 文件名以findExtension结尾（拓展名）
     * 2. 且其余部分同名的pairExtension结尾（拓展名）文件不存在
     */
    private static List<String> autoFindFile(String findExtension, String pairExtension) {
        File folder;
        folder = new File(MainRunner.PRIVATE_IO_FOLDER);
        String[] children = null;
        if (folder.exists()) {
            children = folder.list();
        } else {
            folder = new File(MainRunner.RUNTIME_IO_FOLDER);
            if (folder.exists()) {
                children = folder.list();
            }
        }
        if (children != null) {
            List<String> childrenList = Arrays.asList(children);
            List<String> finds = childrenList.stream()
                    .filter(it -> it.endsWith(findExtension))
                    .map(it -> it.substring(0, it.length() - findExtension.length()))
                    .collect(Collectors.toList());
            List<String> pairs = childrenList.stream()
                    .filter(it -> it.endsWith(pairExtension))
                    .map(it -> it.substring(0, it.length() - pairExtension.length()))
                    .collect(Collectors.toList());
            finds.removeAll(pairs);
            return finds;
        } else {
            return new ArrayList<>();
        }
    }

    public void loadSongJson(String fileName) throws Exception {
        SongDTO songDTO = JsonUtils.objectMapper.readValue(new File(MainRunner.RUNTIME_IO_FOLDER + fileName + ".json"), SongDTO.class);
        SongPO songPO = SongPO.builder()
                .title(songDTO.getTitle())
                .artist(songDTO.getArtist())
                .build();
        if (songRepository.existsByTitle(songDTO.getTitle())) {
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
                    var wordNotePO = JsonUtils.objectMapper.readValue(JsonUtils.objectMapper.writeValueAsString(wordNoteDTO), WordNotePO.class);
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
            "原文/中文", "解释", "原形", "更多解释"
    };

    private File getActualFile(String fileName) {
        return getActualFile(List.of(fileName), List.of()).get(0);
    }

    /**
     * 在候选文件夹中检查所有 mainFileNameOptions, 第一个存在的将确定文件夹和第一个文件；
     * 确定文件夹后，otherFileNames均采用该文件夹；
     */
    private List<File> getActualFile(List<String> mainFileNameOptions, List<String> otherFileNames) {
        String actualFolder = MainRunner.PRIVATE_IO_FOLDER;
        File actualFile = mainFileNameOptions.stream()
                .map(it -> new File(MainRunner.PRIVATE_IO_FOLDER + it))
                .filter(it -> it.exists())
                .findFirst()
                .orElse(null);
        if (actualFile == null) {
            actualFolder = MainRunner.RUNTIME_IO_FOLDER;
            actualFile = mainFileNameOptions.stream()
                    .map(it -> new File(MainRunner.RUNTIME_IO_FOLDER + it))
                    .filter(it -> it.exists())
                    .findFirst()
                    .orElse(null);
        }
        if (actualFile == null) {
            throw new NoSuchElementException("候选文件夹中均找不到文件：" + String.join(", ", mainFileNameOptions));
        }
        List<File> result = new ArrayList<>();
        result.add(actualFile);
        String finalActualFolder = actualFolder;
        otherFileNames.stream()
                .map(it -> new File(finalActualFolder + it))
                .forEach(it -> result.add(it));
        return result;
    }

    public static String[] handleFileName(String fileName) {
        String artist = null;
        String title;
        if (fileName.contains(" - ")) {
            var parts = fileName.split(" - ");
            title = parts[0];
            artist = parts[1];
        } else {
            title = fileName;
        }
        return new String[]{fileName, title, artist};
    }
    private static final String[] PREFIXES_TO_REMOVE = {"作词", "作曲", "编曲", "編曲"};
    public void runAiStep1(String[] args) throws Exception {
        String fileName = args[0];
        String title = args[1];
        String artist = args[2];

        var actualFiles = getActualFile(
                List.of(fileName + ".txt", fileName + ".lrc"),
                List.of(fileName + ".step1.json")
        );
        File actualInput = actualFiles.get(0);
        File actualOutput = actualFiles.get(1);

        List<String> lyricLines = Utils.readAllLines(actualInput).stream()
                .map(line -> line.replaceAll("\\[\\d{2}:\\d{2}[.:]\\d{2,3}\\]", ""))
                .filter(line -> Arrays.stream(PREFIXES_TO_REMOVE).noneMatch(line::startsWith))
                .collect(Collectors.toList());
        List<List<String>> originGroups = lyricLines.stream()
                .map(it -> List.of(it))
                .collect(Collectors.toList());
        List<List<String>> lyricTaskGroups = aiServiceLocator.splitAiTaskGroups(
                originGroups,
                it -> it.stream().allMatch(itt -> itt.isEmpty()),
                aiServiceLocator.getStep1TaskSplitMinSize(),
                aiServiceLocator.getStep1TaskSplitMaxSize()
        );

        File actualStep1AskTemplateFile = getActualFile( "Step1AskTemplate.txt");
        String step1AskTemplate = Utils.readAllLines(actualStep1AskTemplateFile).stream().collect(Collectors.joining("\n"));
        AiStep1Result result = AiStep1Result.builder()
                .title(title)
                .artist(artist)
                .nodes(new ArrayList<>())
                .build();

        for (int i = 0; i < lyricTaskGroups.size(); i++) {
            List<String> lyricTaskGroup = lyricTaskGroups.get(i);
            log.info("start lyricTaskGroup[{}] size = {}", i, lyricTaskGroup.size());
            List<AiStep1ResultNode> groupResult = aiServiceLocator.aiStep1Group(lyricTaskGroup, step1AskTemplate);
            if (groupResult != null) {
                result.getNodes().addAll(groupResult);
            } else {
                throw new Exception("cannot handle group = " + lyricTaskGroup);
            }
        }
        JsonUtils.objectMapper.writeValue(actualOutput, result);
    }

    public void runAiStep2(String[] args) throws Exception {
        String filaName = args[0];

        var actualFiles = getActualFile(
                List.of(filaName + ".step1.json"),
                List.of(filaName + ".step2.json")
        );
        File step1ResultFile = actualFiles.get(0);
        File resultFile = actualFiles.get(1);
        File step2AskTemplateFile = getActualFile( "Step2AskTemplate.txt");
        String step2AskTemplate = Utils.readAllLines(step2AskTemplateFile).stream().collect(Collectors.joining("\n"));

        SongDTO songDTO = aiStep2PrepareResult(step1ResultFile, resultFile);
        // 只选择待处理的LyricGroupDTO
        List<List<LyricLineDTO>> targetGroups = songDTO.getGroups().stream()
                .filter(it -> it.getLineNotes().stream()
                        .anyMatch(itt -> itt.getWordNotes() == null)
                )
                .map(it -> it.getLineNotes())
                .collect(Collectors.toList());
        List<List<LyricLineDTO>> taskGroups = aiServiceLocator.splitAiTaskGroups(
                targetGroups,
                it -> false,
                aiServiceLocator.getStep2TaskSplitMinSize(),
                aiServiceLocator.getStep2TaskSplitMaxSize()
        );
        for (int i = 0; i < taskGroups.size(); i++) {
            // taskGroup的所有歌词合起来问一次
            List<LyricLineDTO> taskGroup = taskGroups.get(i);
            List<String> askLines = taskGroup.stream()
                    .map(it -> it.getLyric())
                    .collect(Collectors.toList());
            log.info("start aiStep2Group Group[{}] size = {}", i, askLines.size());
            List<LyricLineDTO> groupResult = aiServiceLocator.aiStep2Group(askLines, step2AskTemplate);
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
        JsonUtils.objectMapper.writeValue(resultFile, songDTO);
    }



    private SongDTO aiStep2PrepareResult(File step1ResultFile, File resultFile) throws Exception {
        if (resultFile.exists()) {
            return JsonUtils.objectMapper.readValue(resultFile, SongDTO.class);
        } else if (step1ResultFile.exists()) {
            AiStep1Result step1Result = JsonUtils.objectMapper.readValue(step1ResultFile, AiStep1Result.class);
            List<LyricGroupDTO> groups = step1Result.getNodes().stream()
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
                    .title(step1Result.getTitle())
                    .artist(step1Result.getArtist())
                    .groups(groups)
                    .build();
        } else {
            throw new Exception("file not exist");
        }
    }


    @Data
    public static class AiStep1ResultNode {
        private String translation;
        private String groupNote;
        private List<String> lyrics;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AiStep1Result {
        private String title;
        private String artist;
        private List<AiStep1ResultNode> nodes;
    }

    public void renderSongJson(String[] args) throws Exception {
        String fileName = args[0];
        String title = args[1];

        var actualFiles = getActualFile(
                List.of(fileName + ".json", fileName + ".step2.json"),
                List.of(fileName + ".md")
        );
        File actualInput = actualFiles.get(0);
        File actualOutput = actualFiles.get(1);

        SongDTO songDTO = JsonUtils.objectMapper.readValue(actualInput, SongDTO.class);
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
                                    .filter(it -> JapaneseCharacterTool.hasAnyKanji(wordNoteDTO.getText()))
                                    .map(it -> "(" + it + ")")
                                    .orElse(""),
                            Optional.ofNullable(wordNoteDTO.getTranslation()).orElse(""),
                            Optional.ofNullable(wordNoteDTO.getOrigin()).orElse(""),
                            //Optional.ofNullable(wordNoteDTO.getCategory()).orElse(List.of()).stream().collect(Collectors.joining(", ")),
                            //Optional.ofNullable(wordNoteDTO.getLevel()).orElse(""),
                            Optional.ofNullable(wordNoteDTO.getExplain()).orElse("")
                    );
                });
            });
            tableBuilder.addRow();
        });
        result.append(tableBuilder.build());
        FileWriter myWriter = new FileWriter(actualOutput);
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
                    WordNotePO wordNotePO = wordNoteRepository.findById(id).orElse(null);
                    if (wordNotePO == null) {
                        throw new Exception("NotFound WordNotePO id = " + id);
                    }
                    WordNoteDTO wordNoteDTO = JsonUtils.objectMapper.readValue(JsonUtils.objectMapper.writeValueAsString(wordNotePO), WordNoteDTO.class);
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
