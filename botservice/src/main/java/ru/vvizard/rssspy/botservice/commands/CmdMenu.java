package ru.vvizard.rssspy.botservice.commands;

import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.BotserviceApplication;
import ru.vvizard.rssspy.botservice.utils.Emoji;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by igormazevich
 */
@Component
@Scope("prototype")
public class CmdMenu extends Commands {
    private static final Logger logger= LogManager.getLogger(CmdMenu.class);


        /// Menu command
    private static final String menuCommand =  "menu";

    private static final List<String> hideCommands =new ArrayList<>();



    @Autowired
    private Postman postman;

    @Autowired
    private MessageSource messageSource;

    @Override
    protected void sendMessage(SendMessage sendMessage) {
        logger.info("Start send menu");
        sendMessage.setChatId(message.getChatId().toString());
        try {
            postman.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error in sender menu \n"+e.getLocalizedMessage());
        }
        logger.info("End send menu");
    }

    @Override
    protected void handle() {
        Locale locale=new Locale(chatVV.getLang());

        //If message for hide menu
        if (!message.getText().isEmpty()){
            String txt= EmojiParser.removeAllEmojis(message.getText());
            if (hideCommands.contains(txt)){
                ReplyKeyboardRemove menuHide=new ReplyKeyboardRemove();
                sendMessage.setReplyMarkup(menuHide);
                sendMessage.setText(messageSource.getMessage("CommandLineMode", null, locale));
                return;
            }


        }

        logger.info("Start make menu");

        ReplyKeyboardMarkup inlineKeyboardMarkup = getReplyKeyboardMarkup(locale);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setText(messageSource.getMessage("SelectMenu", null, locale));
        logger.info("End make menu");
    }

    ReplyKeyboardMarkup getReplyKeyboardMarkup(Locale localeLang) {
        List<KeyboardRow> rows=new ArrayList<>();
        ReplyKeyboardMarkup inlineKeyboardMarkup=new ReplyKeyboardMarkup();

        KeyboardRow row1=new KeyboardRow();

        KeyboardButton buttonAdd=new KeyboardButton();

        buttonAdd.setText(Emoji.HEAVY_PLUS_SIGN.toString()+messageSource.getMessage("AddFeed", null, localeLang));
        row1.add(buttonAdd);

        KeyboardButton buttonList=new KeyboardButton();
        buttonList.setText(Emoji.BOOKS.toString()+messageSource.getMessage("ListFeeds", null, localeLang));
        row1.add(buttonList);

        rows.add(row1);

        KeyboardRow row2=new KeyboardRow();
        KeyboardButton buttonDel=new KeyboardButton();
        buttonDel.setText(Emoji.TOILET.toString()+messageSource.getMessage("DelFeed", null, localeLang));
        row2.add(buttonDel);

        KeyboardButton buttonHelp=new KeyboardButton();
        buttonHelp.setText(Emoji.BLACK_QUESTION_MARK_ORNAMENT.toString()+messageSource.getMessage("Help", null, localeLang));
        row2.add(buttonHelp);

        rows.add(row2);


        KeyboardRow row3=new KeyboardRow();
        KeyboardButton buttonHideMenu=new KeyboardButton();
        buttonHideMenu.setText(Emoji.CROSS_MARK.toString()+messageSource.getMessage("HideMenu", null, localeLang));
        row3.add(buttonHideMenu);
        rows.add(row3);

        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }


    @Override
    @PostConstruct
    void fillListCommands() {
        if (ListCommands.menuCommand ==null){
            ListCommands.menuCommand = new ArrayList<>();
            ListCommands.menuCommand.add(menuCommand);
            ListCommands.menuCommand.add(commandInitChar + menuCommand);
            for (String s : BotserviceApplication.availableLocales) {
                String cmd=this.messageSource.getMessage("HideMenu", null, new Locale(s));
                hideCommands.add(cmd);
                ListCommands.menuCommand.add(cmd);
            }

        }
        this.listCommand=ListCommands.menuCommand;
    }
}
