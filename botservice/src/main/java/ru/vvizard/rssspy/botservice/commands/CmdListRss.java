package ru.vvizard.rssspy.botservice.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.logging.BotLogger;
import ru.vvizard.rssspy.botservice.BotserviceApplication;
import ru.vvizard.rssspy.botservice.domain.Feed;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by igormazevich
 */

//TODO Отображать признак ошибки на каналах с ошибкой
@Component
@Scope("prototype")
public class CmdListRss extends Commands {

    @Autowired
    private Postman postman;

    /// List rss command
    private static final String listrssCommand = "listfeeds";


    private static final String LOGTAG = "LISTRSSCOMMAND";
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
        Integer i=0;
        String txt="";
        HashSet<Feed> feeds=chatVV.getFeeds();
        if (feeds==null || feeds.isEmpty()){
            txt=messageSource.getMessage("RssListEmpty", null, new Locale(chatVV.getLang()));
        }else{
            for (Feed feed: feeds){
                if (feed.getTitle()!=null){
                    txt=txt+(++i)+". "+feed.getTitle()+"\n";
                }

            }
        }

        sendMessage.setText(txt);
        sendMessage.disableWebPagePreview();

        BotLogger.debug(LOGTAG, message.toString());

    }


    @Override
    @PostConstruct
    void fillListCommands(){
        if (ListCommands.listRssCommand ==null){
            ListCommands.listRssCommand= new ArrayList<>();
            ListCommands.listRssCommand.add(listrssCommand);
            ListCommands.listRssCommand.add(commandInitChar + listrssCommand);

            for (String s : BotserviceApplication.availableLocales) {
                String cmd=this.messageSource.getMessage("ListFeeds", null, new Locale(s));
                ListCommands.listRssCommand.add(cmd);
            }
        }
        this.listCommand=ListCommands.listRssCommand;

    }
}
