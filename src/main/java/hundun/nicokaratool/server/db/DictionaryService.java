package hundun.nicokaratool.server.db;

import hundun.nicokaratool.server.db.po.SongWordPO;
import hundun.nicokaratool.server.db.po.StandardDictionaryWordPO;
import hundun.nicokaratool.server.db.repository.SongRepository;
import hundun.nicokaratool.server.db.repository.SongWordRepositoryCustomImpl;
import hundun.nicokaratool.server.db.repository.StandardDictionaryWordRepository;
import hundun.nicokaratool.server.db.repository.SongWordRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DictionaryService {

    @Autowired
    SongRepository songRepository;
    @Autowired
    SongWordRepository songWordRepository;
    @Autowired
    StandardDictionaryWordRepository standardDictionaryWordRepository;
    @Autowired
    SongWordRepositoryCustomImpl songWordRepositoryCustom;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class QueryRequest {
        List<String> wordIds;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class QueryResult {
        List<WordResult> wordResults;
        List<String> notFoundWordIds;
        List<String> badWordIds;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class WordResult {
        private StandardDictionaryWordPO word;
        List<SongWordMatchDetail> allMatchedMatchDetails;
        List<SongWordMatchDetail> subMatchedMatchDetails;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SongWordMatchDetail {
        SongWordPO songWord;
        String title;
        String lyric;
        int matchScore;
    }
    public List<StandardDictionaryWordPO> findTag(String tag, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return standardDictionaryWordRepository.findAllByStandardTagsContaining(tag, pageable);
    }

    public String renderResult(QueryResult queryResult) {
        StringBuilder allResult = new StringBuilder();
        queryResult.wordResults.forEach(it -> {
            StringBuilder result = new StringBuilder();
            var word = it.getWord().getVocabKanji() == null
                    ? it.getWord().getVocabFurigana()
                    : (it.getWord().getVocabKanji() + "(" + it.getWord().getVocabFurigana() + ")");
            word = "[" + it.getWord().getVocabPoS() + "] "  + word;
            result.append(word).append("\n");
            String des;
            List<SongWordMatchDetail> target;
            if (!it.getAllMatchedMatchDetails().isEmpty()) {
                des = "完全匹配：";
                target = it.getAllMatchedMatchDetails();
            } else {
                return;
            }
            result.append(des).append("\n");
            target.forEach(itt -> {
                var songWord = itt.getSongWord();
                String transformed = (songWord.getOrigin() != null && !songWord.getText().equals(songWord.getOrigin()))
                        ? songWord.getOrigin() + "->" + songWord.getText() + " "
                        : "";
                result.append(String.format("%s《%s》 %s", transformed, itt.getTitle(), itt.getLyric())).append("\n");
            });
            result.append("--------------------------").append("\n");
            allResult.append(result);
        });
        return allResult.toString();
    }
    public QueryResult work(QueryRequest request) {

        List<StandardDictionaryWordPO> dictionaryWords = new ArrayList<>();
        List<String> notFoundWordIds = new ArrayList<>();
        List<String> badWordIds = new ArrayList<>();
        for (String wordId : request.getWordIds()) {
            var it = standardDictionaryWordRepository.findById(wordId);
            if (it.isPresent()) {
                dictionaryWords.add(it.get());
            } else {
                badWordIds.add(wordId);
            }
        }

        List<WordResult> wordResults = new ArrayList<>();
        for (StandardDictionaryWordPO dictionaryWord : dictionaryWords) {
            String vocabFurigana = Optional.of(dictionaryWord.getVocabFurigana())
                    .map(it -> it.replace("~", ""))
                    .map(it -> it.replace(" ", ""))
                    .orElseThrow();
            String vocabKanji = Optional.ofNullable(dictionaryWord.getVocabKanji())
                    .map(it -> it.replace("~", ""))
                    .map(it -> it.replace(" ", ""))
                    .orElse(null);
            List<SongWordPO> findResults = songWordRepositoryCustom.findAllByTextOrHurikanaOrOrigin(vocabFurigana, vocabKanji);
            List<SongWordMatchDetail> details = findResults.stream()
                    .map(it -> {
                        int matchScore = 0;
                        if (Objects.equals(it.getText(), vocabFurigana) || Objects.equals(it.getHurikana(), vocabFurigana) || Objects.equals(it.getOrigin(), vocabFurigana)) {
                            matchScore++;
                        }
                        if (vocabKanji == null || Objects.equals(it.getText(), vocabKanji) || Objects.equals(it.getOrigin(), vocabKanji)) {
                            matchScore++;
                        }
                        var song = songRepository.findById(it.getSongId()).orElseThrow();
                        String lyric = song.getGroups().get(it.getGroupIndex()).getLines().get(it.getLineIndex()).getLyric();
                        return SongWordMatchDetail.builder()
                                .songWord(it)
                                .title(song.getTitle())
                                .lyric(lyric)
                                .matchScore(matchScore)
                                .build();
                    })
                    .sorted((o1, o2) -> Integer.compare(o1.getMatchScore(), o2.getMatchScore()))
                    .collect(Collectors.toList());
            List<SongWordMatchDetail> distinctDetails = details.stream()
                    .collect(Collectors.toMap(
                            item -> item.getTitle() + item.getLyric(),   // Key: 以 id 去重
                            item -> item,        // Value: 对象本身
                            (first, second) -> first,  // 冲突时保留第一个
                            LinkedHashMap::new          // 使用 LinkedHashMap 保持顺序
                    ))
                    .values()               // 获取去重后的值
                    .stream()
                    .limit(3)
                    .collect(Collectors.toList()); // 转回 List



            if (distinctDetails.isEmpty()) {
                notFoundWordIds.add(dictionaryWord.getWordId());
            } else {
                List<SongWordMatchDetail> allMatchedMatchDetails = new ArrayList<>();
                List<SongWordMatchDetail> subMatchedMatchDetails = new ArrayList<>();
                distinctDetails.forEach(itt -> {
                    if (itt.matchScore == 2) {
                        allMatchedMatchDetails.add(itt);
                    } else {
                        subMatchedMatchDetails.add(itt);
                    }
                });
                wordResults.add(
                        WordResult.builder()
                                .word(dictionaryWord)
                                .allMatchedMatchDetails(allMatchedMatchDetails)
                                .subMatchedMatchDetails(subMatchedMatchDetails)
                                .build()
                );
            }
        }


        return QueryResult.builder()
                .wordResults(wordResults)
                .badWordIds(badWordIds)
                .notFoundWordIds(notFoundWordIds)
                .build();
    }

}
