package ru.vvizard.rssspy.botservice.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.service.ChatService;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by igormazevich
 */
@Component
@Scope("prototype")
public class CmdStart extends Commands {
    private static final Logger logger= LogManager.getLogger(CmdStart.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private CmdLang cmdLang;


    @Autowired
    private Postman postman;

    /// List rss command
    private static final String startCommand = "start";

    @Override
    protected void sendMessage(SendMessage sendMessage) {
        logger.info("Start send message");
        sendMessage.setChatId(message.getChatId().toString());
        try {
            postman.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error in sender message \n"+e.getLocalizedMessage());
        }
        logger.info("End send message");
    }

    @Override
    protected void handle() {
        chatVV=chatService.create(message.getChat());
        sendMessage.setText("Hello "+chatVV.getFirstName()+"\n"
        +messageSource.getMessage("chooseLang", null, new Locale(chatVV.getLang())));
        sendMessage.setReplyMarkup(cmdLang.getInlineKeyboardMarkup());

    }



    @Override
    @PostConstruct
    void fillListCommands(){
        if (ListCommands.startCommand ==null){
            ListCommands.startCommand= new ArrayList<>();
            ListCommands.startCommand.add(startCommand);
            ListCommands.startCommand.add(commandInitChar + startCommand);

        }
        logger.debug("START list command loaded: "+ ListCommands.startCommand);
        this.listCommand=ListCommands.startCommand;

    }

}
