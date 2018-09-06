package ru.vvizard.rssspy.botservice.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.MessageEntity;

import java.util.List;

/**
 * Created by igormazevich
 * Компонент для работы с неопознанными коммандами
 */
@Component
public class Recogniser {
    @Autowired
    private CmdAddRss cmdAddRss;

    public void recognise(Message message) {
        if (message.hasEntities()){
            List<MessageEntity> entities=message.getEntities();
            if (entities==null || entities.size()==0) return;
            MessageEntity entity=entities.get(0);
            if (entity.getType().equalsIgnoreCase("url")){
                cmdAddRss.init(message);
                cmdAddRss.saveRss(entity);
                cmdAddRss.sendMessage(cmdAddRss.sendMessage);
            }
        }else{
            String text=message.getText();
            Integer idx=text.indexOf("http");
            if (idx>=0){
                String url=text.substring(idx);
                cmdAddRss.init(message);
                cmdAddRss.saveRss(url);
                cmdAddRss.sendMessage(cmdAddRss.sendMessage);
            }
        }

    }
}
