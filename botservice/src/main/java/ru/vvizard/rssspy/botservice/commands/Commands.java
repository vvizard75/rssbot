package ru.vvizard.rssspy.botservice.commands;

import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.service.ChatService;
import ru.vvizard.rssspy.botservice.service.FeedService;

import java.util.ArrayList;

/**
 * Created by igormazevich
 */
public abstract class Commands {
    static final String commandInitChar = "/";

    /// List commands
    ArrayList<String> listCommand= new ArrayList<>();

    private static final Logger logger= LogManager.getLogger(Commands.class);











    @Autowired
    MessageSource messageSource;

    @Autowired
    private ChatService chatService;

    @Autowired
    protected FeedService feedService;

    Message message;
    CallbackQuery callbackQuery;
    SendMessage sendMessage;
    ChatVV chatVV;
    private Boolean error;

    public  final void handleCommand(Message message){
        this.callbackQuery=null;
        this.message=message;
        init();
        if (!error){
            handle();
            sendMessage(sendMessage);
        }

    }

    public  final void handleCommand(CallbackQuery callbackQuery){
        this.message=callbackQuery.getMessage();
        this.callbackQuery=callbackQuery;
        init();
        if (!error){
            handle();
            sendMessage(sendMessage);
        }

    }

    abstract void fillListCommands();

    protected abstract void sendMessage(SendMessage sendMessage);

    protected abstract void handle();

    public Boolean thisCommand(String message){
        String msg=EmojiParser.removeAllEmojis(message);
        Boolean result=false;
        logger.debug("Check command\n" +
                "message: "+message+"\n" +
                "listCommand: "+listCommand.toString());
        for (String s : listCommand) {
            if (msg.toLowerCase().startsWith(s.toLowerCase())) {
                logger.debug("This command: "+s);
                result = true;
                break;
            }
        }

        return result;
    }

    private void init(){
        this.error=false;
        this.sendMessage=new SendMessage();
        chatVV=chatService.findOne(message.getChatId());


        if (chatVV== null){
            chatVV=chatService.create(message.getChat());
        }
        if ((!chatVV.pushLastNumberMessage(message.getMessageId())) && (callbackQuery==null)){
            this.error=true;
        }else{
            chatService.save(chatVV); //Save 5 last number messages
        }



    }

    void init(Message message){
        this.message=message;
        init();
    }

}
