package hundun.nicokaratool.server.db.repository;



import hundun.nicokaratool.server.db.po.StandardDictionaryWordPO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface StandardDictionaryWordRepository extends MongoRepository<StandardDictionaryWordPO, String> {

    List<StandardDictionaryWordPO> findAllByStandardTagsContaining(String tag, Pageable pageable);

}
