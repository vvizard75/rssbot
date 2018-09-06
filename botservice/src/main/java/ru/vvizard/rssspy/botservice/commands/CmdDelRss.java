package ru.vvizard.rssspy.botservice.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.BotserviceApplication;
import ru.vvizard.rssspy.botservice.domain.Feed;
import ru.vvizard.rssspy.botservice.mq.RabbitConfiguration;
import ru.vvizard.rssspy.botservice.utils.Emoji;
import ru.vvizard.rssspy.botservice.utils.General;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by igormazevich
 */
@Component
@Scope("prototype")
public class CmdDelRss extends Commands {

    private static final Logger logger= LogManager.getLogger(CmdDelRss.class);

    /// Del rss command
    private static final String delrssCommand = commandInitChar + "delfeed";

    @Autowired
    private Postman postman;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String LOGTAG = "DELRSSCOMMAND";


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


        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup=new InlineKeyboardMarkup();
        if (callbackQuery!=null){
            String[] dataStrings=callbackQuery.getData().split(":");
            if (dataStrings.length==2){
                Feed feed=feedService.findOne(new ObjectId(dataStrings[1]));
                List<InlineKeyboardButton> row=new ArrayList<>();
                InlineKeyboardButton buttonY=new InlineKeyboardButton();
                buttonY.setText(messageSource.getMessage("Yes", null, new Locale(chatVV.getLang())));
                buttonY.setCallbackData(delrssCommand+":"+dataStrings[1]+":Y");
                row.add(buttonY);
                InlineKeyboardButton buttonN=new InlineKeyboardButton();
                buttonN.setText(messageSource.getMessage("Cancel", null, new Locale(chatVV.getLang())));
                buttonN.setCallbackData(delrssCommand+":"+dataStrings[1]+":N");
                row.add(buttonN);
                rows.add(row);
                sendMessage.setText(messageSource.getMessage("ConfirmDel", null, new Locale(chatVV.getLang()))+" \n"+feed.getTitle());
                inlineKeyboardMarkup.setKeyboard(rows);
                sendMessage.setReplyMarkup(inlineKeyboardMarkup);
            }else if (dataStrings.length==3){
                if (dataStrings[2].equalsIgnoreCase("Y")){
                    Map<ObjectId, Long> msgDelRss=new HashMap<>();
                    msgDelRss.put(new ObjectId(dataStrings[1]), chatVV.getId());
                    sendMessage.setText(messageSource.getMessage("FeedDeleted", null, new Locale(chatVV.getLang())));
                    try {
                        rabbitTemplate.convertAndSend(RabbitConfiguration.QUEUE_DEL_RSS,
                                General.mapper.writeValueAsString(msgDelRss));
                    } catch (JsonProcessingException e) {
                        logger.error("ERROR: To JSON - "+ chatVV.getId());
                    }
                }else if(dataStrings[2].equalsIgnoreCase("N")){
                    sendMessage.setText(messageSource.getMessage("CancelDelFeed", null, new Locale(chatVV.getLang())));
                }
            }else if (dataStrings.length==1){
                sendFeedsMenu();
            }



        }else{
            sendFeedsMenu();
        }




    }

    private void sendFeedsMenu(){
        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup=new InlineKeyboardMarkup();
        HashSet<Feed> feeds=chatVV.getFeeds();
        for (Feed feed: feeds){
            List<InlineKeyboardButton> row=new ArrayList<>();
            InlineKeyboardButton button=new InlineKeyboardButton();
            button.setText(Emoji.TOILET.toString()+" "+feed.getTitle());
            button.setCallbackData(delrssCommand+":"+feed.getId());
            row.add(button);
            rows.add(row);
        }
        sendMessage.setText(messageSource.getMessage("ChooseFeedDel", null, new Locale(chatVV.getLang())));
        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
    }


    @Override
    @PostConstruct
    void fillListCommands() {
        if (ListCommands.delRssCommand ==null){
            ListCommands.delRssCommand= new ArrayList<>();
            ListCommands.delRssCommand.add(delrssCommand);
            ListCommands.delRssCommand.add(commandInitChar + delrssCommand);

            for (String s : BotserviceApplication.availableLocales) {
                String cmd=this.messageSource.getMessage("DelFeed", null, new Locale(s));
                ListCommands.delRssCommand.add(cmd);
            }
        }
        this.listCommand=ListCommands.delRssCommand;
    }
}
