package ru.vvizard.rssspy.botservice.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import ru.vvizard.rssspy.botservice.commands.*;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.service.ChatService;

import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Created by igormazevich
 */
@RestController
public class BotController extends TelegramWebhookBot{

    private static final Logger logger= LogManager.getLogger(BotController.class);

    @Autowired
    private CmdStart cmdStart;

    @Autowired
    private ChatService chatService;

    @Autowired
    private CmdAddRss cmdAddRss;

    @Autowired
    private CmdListRss cmdListRss;

    @Autowired
    private CmdDelRss cmdDelRss;

    @Autowired
    private CmdMenu cmdMenu;

    @Autowired
    private CmdLang cmdLang;

    @Autowired
    private CmdHelp cmdHelp;

    @Autowired
    private Recogniser recogniseCommand;

    @Value("${FEED_TOKEN}")
    private String token;




    @RequestMapping(path = "/8EYQy7SH3ivYk4EYA6rra3nD8fNzcBVFGVzBCn33333333", method = POST)
    public void getCommand(@Valid @RequestBody Update update){
        Message message=update.getMessage();
        if (message==null){
            message=update.getChannelPost();
        }
        CallbackQuery callbackQuery=update.getCallbackQuery();
        if ((message==null) && (callbackQuery==null)) {
            return;
        }
        if (message!=null && message.getText()!=null){
            if (cmdStart.thisCommand(message.getText())){
                onStartCommand(message);
            }else if (cmdMenu.thisCommand(message.getText())){
                onMenuCommand(message);
            }else if (cmdAddRss.thisCommand(message.getText())) {
                onAddrssCommand(message);
            }else if (cmdListRss.thisCommand(message.getText())){
                onListRssCommand(message);
            }else if (cmdDelRss.thisCommand(message.getText())){
                onDelRssCommand(message);
            }else if (cmdLang.thisCommand(message.getText())){
                onLangCommand(message);
            }else if (cmdHelp.thisCommand(message.getText())){
                onHelpCommand(message);
            }else{
                logger.warn("Command not recognized: "+message.toString());
                onRecogniseCommand(message);
            }
        }else if (callbackQuery!=null){
            if (cmdDelRss.thisCommand(callbackQuery.getData())){
                onDelRssCommand(callbackQuery);
            }if (cmdAddRss.thisCommand(callbackQuery.getData())){
                onAddrssCommand(callbackQuery);
            }else if (cmdLang.thisCommand(callbackQuery.getData())){
                onLangCommand(callbackQuery);
            }

        }else{
            ChatVV chatVV=chatService.findOne(message.getChatId());
            if (chatVV==null){
                chatService.create(message.getChat());
            }
        }

    }

    private void onHelpCommand(Message message) {
        cmdHelp.handleCommand(message);
    }

    private void onLangCommand(Message message) {
        cmdLang.handleCommand(message);
    }

    private void onLangCommand(CallbackQuery callbackQuery) {
        cmdLang.handleCommand(callbackQuery);
    }

    private void onDelRssCommand(CallbackQuery callbackQuery) {
        cmdDelRss.handleCommand(callbackQuery);
    }


    private void onDelRssCommand(Message message) {
        cmdDelRss.handleCommand(message);
    }

    private void onListRssCommand(Message message) {
        cmdListRss.handleCommand(message);
    }

    private void onRecogniseCommand(Message message) {
        recogniseCommand.recognise(message);
    }

    private void onAddrssCommand(Message message){
        cmdAddRss.handleCommand(message);

    }
    private void onAddrssCommand(CallbackQuery callbackQuery){
        cmdAddRss.handleCommand(callbackQuery);

    }

    private void onStartCommand(Message message){
        cmdStart.handleCommand(message);


    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        return null;
    }

    @Override
    public String getBotUsername() {
        return null;
    }

    @Override
    public String getBotToken() {
        return this.token;
    }

    @Override
    public String getBotPath() {
        return null;
    }

    private void onMenuCommand(Message message){
        cmdMenu.handleCommand(message);
    }


}
