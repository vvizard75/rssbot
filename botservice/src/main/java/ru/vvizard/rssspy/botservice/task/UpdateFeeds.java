package ru.vvizard.rssspy.botservice.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.domain.Feed;
import ru.vvizard.rssspy.botservice.domain.MessageMq;
import ru.vvizard.rssspy.botservice.mq.RabbitConfiguration;
import ru.vvizard.rssspy.botservice.service.FeedService;
import ru.vvizard.rssspy.botservice.utils.General;
import ru.vvizard.rssspy.botservice.utils.XmlReaderFilter;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by igormazevich
 * Сервис для переодического обновления feeds
 */
@Component
public class UpdateFeeds {

    private static final Logger logger= LogManager.getLogger(UpdateFeeds.class);
    @Autowired
    private FeedService feedService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 180000)
    public void updateFeeds(){
        logger.info("Select all feed from db");
        feedService.findAllNoError().forEach(this::updateFeedTout);
    }

    private void updateFeedTout(Feed feed){
        ExecutorService executor = Executors.newFixedThreadPool(1);

        final CompletableFuture<BigDecimal> future = CompletableFuture.supplyAsync(() ->{
            updateFeed(feed);
            return new BigDecimal(0);
        }, executor);
        try {
            future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted: "+ feed.getUrl());
        } catch (ExecutionException e) {
            logger.error("Exception: "+ feed.getUrl());
        } catch (TimeoutException e) {
            logger.error("Timeout: "+ feed.getUrl());
        }
        executor.shutdown();

    }

    private void updateFeed(Feed feed) {
        //Check feed for not empty chat list
        if (feed.getChats().size()==0){
            feedService.delete(feed);
            return;
        }
        SyndFeedInput input=new SyndFeedInput();
        SyndFeed syndFeed;


        javax.net.ssl.SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        if (sslContext==null){
            logger.error("ERROR: Error to create sslContext");
            return;
        }
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);


        String url = feed.getUrl().toString();
        try (CloseableHttpClient client = HttpClientBuilder.create().setSSLSocketFactory(sslsf)
                .setUserAgent("MagpieRSS/0.7 ( http://magpierss.sf.net)").setRetryHandler((exception, executionCount, context) -> {
                    if (executionCount > 3) {
                        logger.warn("Maximum tries reached for client http pool ");
                        return false;
                    }
                    if (exception instanceof org.apache.http.NoHttpResponseException) {
                        logger.warn("No response from server on " + executionCount + " call");
                        return true;
                    }
                    return false;
                }).build()) {
            HttpGet httpGet=new HttpGet(url);
            httpGet.setConfig(General.requestConfig);
            HttpUriRequest method = new HttpGet(url);

            try (CloseableHttpResponse response = client.execute(method);
                 InputStream stream = response.getEntity().getContent()) {
                syndFeed = input.build(new XmlReaderFilter(stream));
            } catch (IllegalArgumentException | FeedException e) {
                logger.error("Error in read XML from: "+ feed.getId().toString());
                logger.error(e.getLocalizedMessage());
                if (feed.getErrCount()>10){
                    feed.setError();
                }
                feed.setErrCount(feed.getErrCount()+1);
                feedService.save(feed);
                //TODO Отправлять сообщение админу

                return;
            }
        } catch (IOException|IllegalArgumentException e) {
            logger.error("ERROR to connect "+url);
            if (feed.getErrCount()>10){
                feed.setError();
            }
            feed.setErrCount(feed.getErrCount()+1);
            feedService.save(feed);
            return;
        }



        logger.info("Download feed "+ feed.getUrl()+" \n and select items where items.time > feed.lastTimeUpdate");

        List<SyndEntry> syndEntryList = syndFeed.getEntries().stream()
                .filter(f -> {
                    try{
                        Date date;
                        date=f.getPublishedDate();
                        if (date==null){
                            date=f.getUpdatedDate();
                            f.setPublishedDate(date);
                        }
                        LocalDateTime dateFeed=feed.getDateUpdate();
                        if ((date == null) || (dateFeed==null)) {
                            return false;
                        }

                        LocalDateTime dateMsg=LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("GMT"));

                        return dateMsg.compareTo(dateFeed) > 0;
                    }catch (NullPointerException e){
                        logger.error("ERROR in "+feed.getId().toString());
                        logger.error("NullPointerException::f:"+f.toString());

                        return false;
                    }

                }).sorted((d1, d2)->d1.getPublishedDate().compareTo(d2.getPublishedDate())).collect(Collectors.toList());

        if (syndEntryList.isEmpty()){
            return;
        }
        feed.setDateUpdate(LocalDateTime.ofInstant(
                syndEntryList.get(syndEntryList.size()-1).getPublishedDate().toInstant(), ZoneId.of("GMT")));

        feed.setErrCount(0);
        saveFeed(feed);
        syndEntryList.forEach((SyndEntry syndEntry) -> {
            MessageMq messageMq = new MessageMq();
            messageMq.setTitle(syndEntry.getTitle());
            messageMq.setUrl(syndEntry.getLink());
            messageMq.setDate(syndEntry.getPublishedDate());
            messageMq.setChatName(feed.getTitle());
            if (!syndEntry.getEnclosures().isEmpty()) {
                SyndEnclosure enclosure = syndEntry.getEnclosures().get(0);
                if ((enclosure!=null) &&
                        (enclosure.getType().equalsIgnoreCase("audio/mpeg"))) {
                    messageMq.setPodcast(enclosure.getUrl());
                }
            }
            feed.getChats().forEach((ChatVV chat) -> {
                try {
                    messageMq.setChatId(chat.getId());

                    rabbitTemplate.convertAndSend(RabbitConfiguration.QUEUE_SEND_FEED_MSG,
                            General.mapper.writeValueAsString(messageMq));
                }catch (NullPointerException e){

                    logger.error("ERROR: NULL - "+  feed.getId().toString());
                } catch (JsonProcessingException e) {
                    logger.error("ERROR: To JSON - "+  feed.getId().toString());
                }


            });
        });



    }

    private void saveFeed(Feed feed){
        try{
            feedService.save(feed);
        }catch (OptimisticLockingFailureException e){
            logger.error("ERROR Record Locking: "+feed.getId());
            saveFeed(feed);
        }

    }


}


