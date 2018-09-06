package ru.vvizard.rssspy.botservice.commands;

import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.EntityType;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.vvizard.rssspy.botservice.BotserviceApplication;
import ru.vvizard.rssspy.botservice.service.ChatService;
import ru.vvizard.rssspy.botservice.utils.Lang;
import ru.vvizard.rssspy.botservice.utils.ListCommands;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Created by igormazevich
 */
@Component
@Scope("prototype")
public class CmdLang extends Commands {


    /// Lang command
    private static final String langCommand = "lang";

    @Autowired
    private ChatService chatService;

    @Autowired
    private Postman postman;

    @Autowired
    private CmdMenu cmdMenu;

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
        if ((message.hasEntities())){
            List<MessageEntity> entities=message.getEntities();
            if ((entities!=null)&&(entities.get(0).getType().equalsIgnoreCase(EntityType.BOTCOMMAND))){
                String[] cmd=message.getText().split(" ");
                if (cmd.length==2){
                    setLang(cmd[1]);
                }else{
                    showMenu();
                }
            }
        }else if (this.callbackQuery!=null){
            String[] ls=this.callbackQuery.getData().split(":");
            if (ls.length==2){
                setLang(ls[1]);
            }
        }
    }

    private void setLang(String lang) {
        changeLang(lang);
        sendMessage.setText(messageSource.getMessage("LanguageIsSetting", null, new Locale(chatVV.getLang())));
        Locale locale=new Locale(chatVV.getLang());
        ReplyKeyboardMarkup inlineKeyboardMarkup = cmdMenu.getReplyKeyboardMarkup(locale);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
    }

    @PostConstruct
    @Override
    void fillListCommands() {
        if (ListCommands.langRssCommand ==null){
            ListCommands.langRssCommand= new ArrayList<>();
            ListCommands.langRssCommand.add(langCommand);
            ListCommands.langRssCommand.add(commandInitChar + langCommand);
            ListCommands.langRssCommand.addAll(BotserviceApplication.availableLocales.stream().collect(Collectors.toList()));
        }
        this.listCommand=ListCommands.langRssCommand;

    }

    private void changeLang(String lang){
        Lang l;
        try {
            l=Lang.valueOf(lang.toUpperCase());
        }catch (IllegalArgumentException e){
            l=Lang.GB;
        }

        this.chatVV.setLang(l);
        chatService.save(chatVV);

    }

    private void showMenu(){
        InlineKeyboardMarkup inlineKeyboardMarkup = getInlineKeyboardMarkup();
        sendMessage.setText(messageSource.getMessage("chooseLang", null, new Locale(chatVV.getLang())));
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);



    }

    InlineKeyboardMarkup getInlineKeyboardMarkup() {
        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup=new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row=new ArrayList<>();
        for (Lang l: Lang.values()) {
            InlineKeyboardButton button=new InlineKeyboardButton();
            button.setText(EmojiParser.parseToUnicode(":"+l.toString().toLowerCase()+": "+l.toString()));
            button.setCallbackData(langCommand+":"+l.toString());
            row.add(button);
        }
        rows.add(row);


        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }
}
