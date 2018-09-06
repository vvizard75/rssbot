package ru.vvizard.rssspy.botservice.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.BotserviceApplication;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.mq.RabbitConfiguration;
import ru.vvizard.rssspy.botservice.service.ChatService;
import ru.vvizard.rssspy.botservice.service.FeedService;
import ru.vvizard.rssspy.botservice.utils.General;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by igormazevich
 */
@Component
@Scope("prototype")
public class CmdAddRss extends Commands{
    private static final Logger logger= LogManager.getLogger(CmdAddRss.class);

    /// Add rss command
    public static final String addrssCommand =  "addfeed";

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FeedService feedService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private Postman postman;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    protected void sendMessage(SendMessage sendMessage) {
        sendMessage.setChatId(message.getChatId().toString());
        try {
            postman.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handle() {

        if (callbackQuery!=null){
            String[] dataStrings=callbackQuery.getData().split(" ");
            if (dataStrings.length==2){
                saveRss(dataStrings[1]);
            }
        }else{
            if (message.getText().isEmpty()){
                return;
            }
            String text=EmojiParser.removeAllEmojis(message.getText());

            if ((message.hasEntities())){
                List<MessageEntity> entities=message.getEntities();
                String arg1= text.substring(entities.get(0).getLength());
                if (entities.size()==1){

                    sendMessage.setText(messageSource.getMessage("PleaseSendFeedUrl", null, new Locale(chatVV.getLang())));
                }else if (entities.size()==2){
                    MessageEntity entity=entities.get(1);
                    saveRss(entity);
                }else if (arg1.length()>0){
                    getUrl(text);
                }

            }else{


                getUrl(text);


            }
        }







    }

    private void getUrl(String text) {
        Integer idx=text.indexOf("http");
        if (idx>=0){
            String url=text.substring(idx);
            saveRss(url);
        }else{
            sendMessage.setText(messageSource.getMessage("PleaseSendFeedUrl", null, new Locale(chatVV.getLang())));
        }
    }


    void saveRss(MessageEntity entity) {
        if (entity.getType().equalsIgnoreCase("url")){
            String msg=message.getText();
            String param=msg.substring(entity.getOffset(), entity.getOffset()+entity.getLength());
            if (!param.startsWith("http")){
                param="http://"+param;
            }
            saveRss(param);


        }else {
            sendMessage.setText(messageSource.getMessage("WrongFeedURL", null, new Locale(chatVV.getLang())));
        }
    }

    void saveRss(String url_str){
        try {



            URL url=new URL(url_str);
            //Отправка на проверку
            checkUrl(url, chatVV);
            sendMessage.setText(messageSource.getMessage("checkedUrl", null, new Locale(chatVV.getLang())));



        } catch (MalformedURLException e) {
            sendMessage.setText(messageSource.getMessage("WrongFeedURL", null, new Locale(chatVV.getLang())));
            e.printStackTrace();
        }
    }

    @Override
    @PostConstruct
    void fillListCommands() {
        if (ListCommands.addRssCommand ==null){
            ListCommands.addRssCommand= new ArrayList<>();
            ListCommands.addRssCommand.add(addrssCommand);
            ListCommands.addRssCommand.add(commandInitChar + addrssCommand);

            for (String s : BotserviceApplication.availableLocales) {
                String cmd=this.messageSource.getMessage("AddFeed", null, new Locale(s));
                ListCommands.addRssCommand.add(cmd);
            }
        }
        this.listCommand=ListCommands.addRssCommand;
    }


    //Отправка URL на проверку в очередь chekurl, и отправка сообщения в чат о начале проверки
    private void checkUrl(URL url, ChatVV chatVV){
        Map<URL, Long> msgFeedToCheck=new HashMap<>();
        msgFeedToCheck.put(url, chatVV.getId());
        try {
            rabbitTemplate.convertAndSend(RabbitConfiguration.QUEUE_CHECK_FEED,
                    General.mapper.writeValueAsString(msgFeedToCheck));
        } catch (JsonProcessingException e) {
            logger.error("ERROR: To JSON - "+url.toString());
        }
    }
}




