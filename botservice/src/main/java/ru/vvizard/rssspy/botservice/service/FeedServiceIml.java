package ru.vvizard.rssspy.botservice.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import ru.vvizard.rssspy.botservice.domain.Feed;
import ru.vvizard.rssspy.botservice.mq.RabbitConfiguration;
import ru.vvizard.rssspy.botservice.repository.FeedRepository;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Created by igormazevich
 * Имплементация для POJO для Feed
 */
@Service
public class FeedServiceIml implements FeedService {

    private static final Logger logger= LogManager.getLogger(FeedServiceIml.class);

    @Autowired
    FeedRepository feedRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Feed create(URL url) {
        Feed existing =feedRepository.findByUrl(url);
        if (existing!=null) return existing;

        Feed feed=new Feed();
        feed.setId(new ObjectId());
        feed.setUrl(url);
        feed.setDateUpdate(LocalDateTime.now(ZoneId.of("GMT")));
        feed=feedRepository.save(feed);
        rabbitTemplate.convertAndSend(RabbitConfiguration.QUEUE_UPDATE_FEED,
                    feed.getId().toString());

        return feed;
    }

    @Override
    public Feed findOne(ObjectId id) {
        return feedRepository.findOne(id);
    }

    @Override
    public Iterable<Feed> findAll() {
        return feedRepository.findAll();
    }

    @Override
    public void save(Feed feed) {
        try{
            feedRepository.save(feed);
        } catch (OptimisticLockingFailureException e){
            save(feed);
        }

    }

    @Override
    public void delete(Feed feed) {
        feedRepository.delete(feed);
    }

    @Override
    public Iterable<Feed> findAllNoError() {
        return feedRepository.findByError(false);
    }

    @Override
    public Iterable<Feed> findAllYesError() {
        return feedRepository.findByError(true);
    }


}
