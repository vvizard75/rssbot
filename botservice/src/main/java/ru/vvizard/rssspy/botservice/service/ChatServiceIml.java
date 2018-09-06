package ru.vvizard.rssspy.botservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.objects.Chat;
import ru.vvizard.rssspy.botservice.domain.ChatVV;
import ru.vvizard.rssspy.botservice.domain.TypeChat;
import ru.vvizard.rssspy.botservice.repository.ChatRepository;

/**
 * Created by igormazevich
 */
@Service
@Qualifier("chatservice")
public class ChatServiceIml implements ChatService {

    @Autowired
    private ChatRepository chatRepository;


    @Override
    public ChatVV create(Chat chat) {
        ChatVV existing =chatRepository.findOne(chat.getId());
        if (existing!=null) return existing;

        ChatVV chatVV=new ChatVV();
        chatVV.setId(chat.getId());
        chatVV.setFirstName(chat.getFirstName());
        chatVV.setTitle(chat.getTitle());
        chatVV.setLastName(chat.getLastName());
        chatVV.setUserName(chat.getUserName());
        if (chat.isChannelChat()){
            chatVV.setTypeChat(TypeChat.CHANEL);
        }else if (chat.isGroupChat()){
            chatVV.setTypeChat(TypeChat.GROUP);
        }else if (chat.isSuperGroupChat()){
            chatVV.setTypeChat(TypeChat.SGROUP);
        }else if (chat.isUserChat()){
            chatVV.setTypeChat(TypeChat.USER);
        }
        chatRepository.save(chatVV);
        return chatVV;
    }

    @Override
    public ChatVV findOne(Long id) {
        return chatRepository.findOne(id);
    }

    @Override
    public void save(ChatVV chatVV) {
         chatRepository.save(chatVV);
    }

    @Override
    public void delete(ChatVV chatVV) {
        chatRepository.delete(chatVV);
    }
}
