package hundun.nicokaratool.db.repository;



import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import hundun.nicokaratool.db.MongoConfig;
import hundun.nicokaratool.db.po.SongPO;

import java.util.List;


public class SongRepository {


    public SongRepository() {

    }


    public SongPO save(SongPO songPO) {
        return MongoConfig.datastore.save(songPO);
    }
    public boolean existTitle(String title) {
        Query<SongPO> query = MongoConfig.datastore.find(SongPO.class)
                .filter(Filters.eq("title", title));
        return query.count() > 0;
    }

    public SongPO findById(String id) {
        return MongoConfig.datastore.find(SongPO.class)
                .filter(Filters.eq("_id", id))
                .first();
    }

    public List<SongPO> findAll() {
        return MongoConfig.datastore.find(SongPO.class).iterator().toList();
    }

    public SongPO findFirstByTitle(String title) {
        Query<SongPO> query = MongoConfig.datastore.find(SongPO.class)
                .filter(Filters.eq("title", title));
        return query.iterator().next();
    }

}
