package hundun.nicokaratool.server.db.repository;



import hundun.nicokaratool.server.db.po.WordNotePO;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface WordNoteRepository  extends MongoRepository<WordNotePO, String> {



}
