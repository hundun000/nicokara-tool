package hundun.nicokaratool.server.db.repository;



import hundun.nicokaratool.server.db.po.SongPO;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface SongRepository extends MongoRepository<SongPO, String> {


    boolean existsByTitle(String title);

    SongPO findFirstByTitle(String title);

}
