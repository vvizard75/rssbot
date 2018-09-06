package ru.vvizard.rssspy.botservice.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.commands.CmdAddRss;
import ru.vvizard.rssspy.botservice.commands.Postman;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.domain.Feed;
import ru.vvizard.rssspy.botservice.service.ChatService;
import ru.vvizard.rssspy.botservice.service.FeedService;
import ru.vvizard.rssspy.botservice.utils.Emoji;
import ru.vvizard.rssspy.botservice.utils.General;
import ru.vvizard.rssspy.botservice.utils.XmlReaderFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by igormazevich
 */

@Component
public class RabbitCheckUrlConsumer {
    private static final Logger logger= LogManager.getLogger(RabbitCheckUrlConsumer.class);

    @Autowired
    private FeedService feedService;

    @Autowired
    private Postman postman;

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String err_Json="ERROR: to JSON: ";

    @RabbitListener(queues = RabbitConfiguration.QUEUE_CHECK_FEED)
    public void checkFeed(String strJson){

        Map<URL, Long> message= null;
        try {
            message = General.mapper.readValue(strJson, new TypeReference<Map<URL, Long>>(){});
        } catch (IOException|IllegalArgumentException e) {
            logger.error(err_Json+strJson);
        }
        if (message==null){
            return;
        }

        URL url=message.keySet().iterator().next();
        Long chatId=message.get(url);

        ChatVV chatVV=chatService.findOne(chatId);

        if (chatVV==null) return;

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


        String url_str = url.toString();





        try (CloseableHttpClient client = HttpClientBuilder.create().setSSLSocketFactory(sslsf).setUserAgent("MagpieRSS/0.7 ( http://magpierss.sf.net)").build()) {
            HttpUriRequest method = new HttpGet(url_str);

            //Check . This rss
            SyndFeedInput input=new SyndFeedInput();
            try (CloseableHttpResponse response = client.execute(method))
                 {
                     BufferedHttpEntity httpEntity=new BufferedHttpEntity(response.getEntity());
                     InputStream stream = httpEntity.getContent();
                     stream.mark(10240);

                     try{
                    SyndFeed syndFeed = input.build(new XmlReaderFilter(stream));
                    String title = syndFeed.getTitle();

                         Feed feed=feedService.create(url);
                    feed.setTitle(title);


                        chatVV.addFeed(feed);
                        chatService.save(chatVV);


                        Map<String, Long> msgChatToFeed=new HashMap<>();
                        msgChatToFeed.put(feed.getId().toString(), chatVV.getId());
                        rabbitTemplate.convertAndSend(RabbitConfiguration.QUEUE_ADD_CHAT_TO_FEED,
                                General.mapper.writeValueAsString(msgChatToFeed));

                         SendMessage sendMessage=new SendMessage();
                         sendMessage.setChatId(chatVV.getId());
                         sendMessage.setText(messageSource.getMessage("FeedAdded", null, new Locale(chatVV.getLang())));
                         try {
                             postman.sendMessage(sendMessage);
                         } catch (TelegramApiException e) {
                             e.printStackTrace();
                         }
                         return;
                }catch (FeedException|IllegalArgumentException e) {
                    logger.error("Error in read XML from: "+ url_str);
                    logger.error(e.getLocalizedMessage());
                }


                if (!stream.markSupported()){
                    return;
                }
                     stream.reset();

                String html=EntityUtils.toString(httpEntity);
                Document document= Jsoup.parse(html);
                Elements links = document.select("link[type=application/rss+xml]");
                     if (links.size()>0){
                         sendFeedsMenu(links,  chatVV);

                 }



            }

            //Check application/rss+xml in html


        } catch (IOException|IllegalArgumentException e) {
            logger.error("ERROR to connect "+url);
            //TODO отправить сообщение о том, что адрес недоступен
        }




    }

    private void sendFeedsMenu(Elements links, ChatVV chatVV){

        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup=new InlineKeyboardMarkup();
        String str="";
        Integer i=1;
        List<InlineKeyboardButton> row=new ArrayList<>();
        for (Element element: links){
            str=str+i+" - "+ element.attr("title") +"\n";
            InlineKeyboardButton button=new InlineKeyboardButton();
            button.setText(Emoji.HEAVY_PLUS_SIGN+" "+i++);
            button.setCallbackData(CmdAddRss.addrssCommand+" "+element.attr("abs:href"));
            row.add(button);

        }

        rows.add(row);

        SendMessage sendMessage=new SendMessage();
        sendMessage.setChatId(chatVV.getId());
        sendMessage.setText(messageSource.getMessage("ChooseFoundRss", null, new Locale(chatVV.getLang()))+"\n"+str);
        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        try {
            postman.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


}
