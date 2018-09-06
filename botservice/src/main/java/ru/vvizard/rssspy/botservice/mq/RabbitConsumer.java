package ru.vvizard.rssspy.botservice.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndImage;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import org.apache.http.client.ClientProtocolException;
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
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import ru.vvizard.rssspy.botservice.commands.Postman;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.domain.Feed;
import ru.vvizard.rssspy.botservice.domain.MessageMq;
import ru.vvizard.rssspy.botservice.service.ChatService;
import ru.vvizard.rssspy.botservice.service.FeedService;
import ru.vvizard.rssspy.botservice.utils.General;
import ru.vvizard.rssspy.botservice.utils.XmlReaderFilter;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by igormazevich
 * Компонент со слушателями очередей MQ
 */

@Component
public class RabbitConsumer {
    private static final Logger logger= LogManager.getLogger(RabbitConsumer.class);

    @Autowired
    private FeedService feedService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private Postman postman;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String err_Json="ERROR: to JSON: ";

    @RabbitListener(queues = RabbitConfiguration.QUEUE_ADD_CHAT_TO_FEED)
    private void addChatToFeed(String strJson){
        Map<String, Long> message;
        try {
            message = General.mapper.readValue(strJson, new TypeReference<Map<String, Long>>(){});
        } catch (IOException e) {
            logger.error(err_Json+strJson);
            return;
        }
        String idStr=message.keySet().iterator().next();
        ObjectId feedId=new ObjectId(idStr);
        Long chatId=message.get(idStr);

        ChatVV chatVV=chatService.findOne(chatId);
        Feed feed=feedService.findOne(feedId);
        feed.addChat(chatVV);
        try{
            feedService.save(feed);
        }catch (OptimisticLockingFailureException e){
            logger.error("ERROR: ");
            addChatToFeed(strJson);
        }

    }

    @RabbitListener(queues = RabbitConfiguration.QUEUE_UPDATE_FEED)
    private void updateFeed(String strJson){
        ObjectId feedId=new ObjectId(strJson);

        logger.info("Get message from MQ for feedId: "+ strJson);
        Feed feed=feedService.findOne(feedId);

        if (feed==null) return;

        SyndFeedInput input=new SyndFeedInput();

        javax.net.ssl.SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException |KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
            return;
        }

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);

        String url = feed.getUrl().toString();
        try (CloseableHttpClient client = HttpClientBuilder.create().setSSLSocketFactory(sslsf).setUserAgent("MagpieRSS/0.7 ( http://magpierss.sf.net)").build()) {
            HttpUriRequest method = new HttpGet(url);
            //Read rss-data from URL

            //Make thread for timeout for execution
            ExecutorService executor = Executors.newFixedThreadPool(1);
            final CompletableFuture<BigDecimal> future = CompletableFuture.supplyAsync(() ->{

                try (CloseableHttpResponse response = client.execute(method);
                     InputStream stream = response.getEntity().getContent()) {
                    SyndFeed syndFeed = input.build(new XmlReaderFilter(stream));
                    String title = syndFeed.getTitle();

                    SyndImage image=syndFeed.getImage();


                    if (image!=null){
                        feed.setImageUrl(image.getUrl());
                    }
//                    feed=feedService.findOne(feedId);
                    feed.setTitle(title);
                    feedService.save(feed);
                } catch (OptimisticLockingFailureException e){
                    logger.error("Error optimistic locking");
                    logger.error(e.getStackTrace());
                    updateFeed(strJson);
                }catch (FeedException e) {
                    logger.error("Error in read XML from: "+ feed.getId().toString());
                    logger.error(e.getLocalizedMessage());

                    feedService.save(feed);
                    //TODO Отправлять сообщение админу
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new BigDecimal(0);
            }, executor);
            future.get(60, TimeUnit.SECONDS);
            executor.shutdown();


        } catch (IOException|IllegalArgumentException e) {
            logger.error("ERROR to connect "+url);
            if (feed.getErrCount()>10){
                feed.setError();
            }
            feed.setErrCount(feed.getErrCount()+1);
            feedService.save(feed);
        } catch (InterruptedException e) {
            logger.error("Interrupted: "+ url);
        } catch (ExecutionException e) {
            logger.error("Exception: "+ url);
        } catch (TimeoutException e) {
            logger.error("Timeout: "+ url);
        }



    }

    @RabbitListener(queues = RabbitConfiguration.QUEUE_DEL_RSS)
    private void delRss(String strJson){
        Map<ObjectId, Long> message= null;
        try {
            message = General.mapper.readValue(strJson, new TypeReference<Map<ObjectId, Long>>(){});
        } catch (IOException e) {
            logger.error(err_Json+strJson);
        }
        if (message==null){
            logger.error("ERROR: message for delete NULL");
            return;
        }
        logger.info("Message for delete: "+message.toString());
        ObjectId feedId=message.keySet().iterator().next();
        Long chatId=message.get(feedId);
        Feed feed=feedService.findOne(feedId);
        ChatVV chatVV=chatService.findOne(chatId);
        try{
            if (chatVV!=null){
                HashSet<Feed> feeds=chatVV.getFeeds();
                feeds.remove(feed);
                chatVV.setFeeds(feeds);
                chatService.save(chatVV);
            }
            if (feed!=null){
                HashSet<ChatVV> chats=feed.getChats();
                chats.remove(chatVV);
                if (chats.isEmpty()){
                    feedService.delete(feed);
                }else{
                    feedService.save(feed);
                }
            }


        }catch (OptimisticLockingFailureException|NullPointerException e){
            e.printStackTrace();
            delRss(strJson);
        }

    }



    @SuppressWarnings("unused")
    @RabbitListener(queues = RabbitConfiguration.QUEUE_SEND_FEED_MSG)
    public void sendFeedMsg(String strJson){

        MessageMq messageMq;
        try {
            messageMq = General.mapper.readValue(strJson, MessageMq.class);
        } catch (IOException e) {
            logger.error(err_Json+strJson);
            return;
        }
        if (messageMq==null) return;
        if (messageMq.getUrl()==null) return;
        if (messageMq.getChatId()==null) return;
        if (messageMq.getChatName()==null) messageMq.setChatName("");

        String url=messageMq.getUrl().toLowerCase();
        if (!url.startsWith("http")){
            url="http://".concat(url);
        }
        SendMessage message=new SendMessage();
        message.setChatId(messageMq.getChatId().toString());
        String txt=messageMq.getChatName()+"\n"+
                messageMq.getDate()+"\n"+
                String.format("<a href=\"%s\">%s</a>", url, messageMq.getTitle());
        if (messageMq.getPodcast()!=null){
            txt=txt+"\n"+messageMq.getPodcast();
        }
        message.setText(txt);
        message.enableHtml(true);
        try {
            postman.sendMessage(message);

            //For Gogle analytics

            Map<String, String> msgGA = new HashMap<>();
            msgGA.put("t", "event");
            msgGA.put("ec", "sendMsg");
            msgGA.put("ea", "rssToClient");
            msgGA.put("cid", messageMq.getChatId().toString());
            rabbitTemplate.convertAndSend(RabbitConfiguration.QUEUE_GANALYTICS_MSG,
                    General.mapper.writeValueAsString(msgGA));


        } catch (TelegramApiException|JsonProcessingException e) {
            logger.error("Error on sender message in chat \n"+e.getLocalizedMessage());
            if (e instanceof TelegramApiRequestException){
                if (((TelegramApiRequestException) e).getErrorCode()==403){
                    logger.info("User "+messageMq.getChatId()+" leave bot");
                    ChatVV chatVV=chatService.findOne(messageMq.getChatId());
                    if (chatVV!=null){
                        logger.debug("Chat for delete found!");

                        try{
                            for (Feed feed : chatVV.getFeeds()) {
                                logger.debug("Feed for delete found!");


                                Map<ObjectId, Long> msgDelRss=new HashMap<>();
                                msgDelRss.put(feed.getId(), messageMq.getChatId());
                                try {
                                    delRss(General.mapper.writeValueAsString(msgDelRss));
                                } catch (JsonProcessingException e1) {
                                    logger.error(err_Json+feed.getId());
                                }
                            }
                        }catch (NullPointerException er){
                            logger.error("ERROR in struecture data");
                        }
                        chatService.delete(chatVV);
                        logger.debug("Chat deleted!");
                    }

                }
            }
            e.printStackTrace();
        }

    }
}
