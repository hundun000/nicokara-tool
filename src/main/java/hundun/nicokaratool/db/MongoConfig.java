package hundun.nicokaratool.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;

public class MongoConfig {
    public static final Datastore datastore;
    static {
        // 初始化 Morphia 和 Datastore
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        datastore = Morphia.createDatastore(mongoClient, "nicokaratool-db");
        datastore.getMapper().mapPackage("hundun.nicokaratool.db");
        datastore.ensureIndexes();
    }

}
