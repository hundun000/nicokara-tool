package hundun.nicokaratool.db;



import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import hundun.nicokaratool.db.po.LyricGroupPO;
import hundun.nicokaratool.db.po.LyricLinePO;
import hundun.nicokaratool.db.po.SongPO;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class LineRepository {


    public LineRepository() {

    }

    public void saveAll(List<LyricLinePO> lines) {
        MongoConfig.datastore.insert(lines);
    }

    public LyricLinePO findById(String id) {
        return MongoConfig.datastore.find(LyricLinePO.class)
                .filter(Filters.eq("_id", id))
                .first();
    }

}
