package ru.vvizard.rssspy.botservice.commands;

import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.BotserviceApplication;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by igormazevich
 */
@Component
@Scope("prototype")
public class CmdHelp extends Commands{

    /// Help command
    private static final String helpCommand = "help";

    @Autowired
    private Postman postman;


    @Override
    @PostConstruct
    void fillListCommands() {
        if (ListCommands.helpCommand ==null){
            ListCommands.helpCommand= new ArrayList<>();
            ListCommands.helpCommand.add(helpCommand);
            ListCommands.helpCommand.add(commandInitChar + helpCommand);

            for (String s : BotserviceApplication.availableLocales) {
                String cmd=this.messageSource.getMessage("Help", null, new Locale(s));
                ListCommands.helpCommand.add(cmd);
            }
        }
        this.listCommand=ListCommands.helpCommand;
    }

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
        sendMessage.setText(EmojiParser.parseToUnicode(this.messageSource.getMessage("helpMsg", null, new Locale(chatVV.getLang()))));
        sendMessage.enableMarkdown(true);
        ReplyKeyboardRemove menuHide=new ReplyKeyboardRemove();
        sendMessage.setReplyMarkup(menuHide);
    }
}
