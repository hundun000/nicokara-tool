package hundun.nicokaratool.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

public class MongoConfig {
    public static final Datastore datastore;
    static {
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                .codecRegistry(pojoCodecRegistry)
                .build();
        // 初始化 Morphia 和 Datastore
        MongoClient mongoClient = MongoClients.create(settings);
        datastore = Morphia.createDatastore(mongoClient, "nicokaratool-db");
        datastore.getMapper().mapPackage("hundun.nicokaratool.db");
        datastore.ensureIndexes();
    }

}
