package hundun.nicokaratool.server.db.repository;



import hundun.nicokaratool.server.db.po.SongWordPO;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface SongWordRepository extends MongoRepository<SongWordPO, String> {



}
