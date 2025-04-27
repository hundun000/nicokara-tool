package hundun.nicokaratool.server.db.repository;

import hundun.nicokaratool.server.db.po.SongWordPO;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SongWordRepositoryCustomImpl {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<SongWordPO> findAllByTextOrHurikanaOrOrigin(String vocabFurigana, @Nullable String vocabKanji) {
        List<Criteria> orCriteriaList = new ArrayList<>();

        // 用 vocabFurigana 的查询
        Criteria criteria;

        // 如果 vocabKanji 不为空，再加上对应的查询
        if (vocabKanji != null) {
            orCriteriaList.add(Criteria.where("text").regex(vocabKanji, "i"));
            orCriteriaList.add(Criteria.where("origin").regex(vocabKanji, "i"));
        } else {
            orCriteriaList.add(Criteria.where("text").regex(vocabFurigana, "i"));
            orCriteriaList.add(Criteria.where("hurikana").regex(vocabFurigana, "i"));
            orCriteriaList.add(Criteria.where("origin").regex(vocabFurigana, "i"));
        }

        criteria = new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0]));
        Query query = new Query(criteria).limit(3);
        return mongoTemplate.find(query, SongWordPO.class);
    }

}
