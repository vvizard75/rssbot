package ru.vvizard.rssspy.botservice.repository;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.vvizard.rssspy.botservice.domain.Feed;

import java.net.URL;
import java.util.List;

/**
 * Created by igormazevich
 */
@Repository
public interface FeedRepository extends CrudRepository<Feed, ObjectId>{
    Feed findByUrl(URL url);
    List<Feed> findByError(Boolean error);
}
