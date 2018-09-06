package ru.vvizard.rssspy.botservice.service;

import org.bson.types.ObjectId;
import ru.vvizard.rssspy.botservice.domain.Feed;

import java.net.URL;

/**
 * Created by igormazevich
 */
public interface FeedService {
    Feed create(URL url);
    Feed findOne(ObjectId id);
    Iterable<Feed> findAll();
    Iterable<Feed> findAllNoError();
    Iterable<Feed> findAllYesError();
        void save(Feed feed);
    void delete(Feed feed);
}
