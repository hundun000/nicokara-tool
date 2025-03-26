package hundun.nicokaratool.db.repository;



import dev.morphia.query.filters.Filters;
import hundun.nicokaratool.db.MongoConfig;
import hundun.nicokaratool.db.po.WordNotePO;

import java.util.List;


public class WordNoteRepository {


    public WordNoteRepository() {

    }

    public void saveAll(List<WordNotePO> items) {
        MongoConfig.datastore.save(items);
    }

    public WordNotePO findById(String id) {
        return MongoConfig.datastore.find(WordNotePO.class)
                .filter(Filters.eq("_id", id))
                .first();
    }

    public List<WordNotePO> findAllByLineId(String lineId) {
        return MongoConfig.datastore.find(WordNotePO.class)
                .filter(Filters.eq("lineId", lineId))
                .iterator()
                .toList();
    }

}
